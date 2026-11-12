package com.clearscreen.prototype.backend

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Build
import android.os.SystemClock
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import org.json.JSONObject
import java.io.Closeable
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.TimeoutException
import kotlin.math.abs

/** Learning persists control identity and local context, never screen coordinates or raw labels. */
internal class ClearScreenAdLearning(
  private val service: AccessibilityService,
  private val handler: Handler,
  private val store: ClearScreenStore,
  private val safeRoot: () -> AccessibilityNodeInfo?,
  private val canTouch: (Rect) -> Boolean,
  private val resumeScan: () -> Unit,
) {
  private val wm = service.getSystemService(WindowManager::class.java)
  private var sessionStarted = 0L
  @Volatile private var learningActive = false
  @Volatile private var generation = 0L
  @Volatile private var scrollVersion = 0L
  private var overlay: View? = null
  private var toolbarSelectButton: TextView? = null
  private var currentToast: Toast? = null
  private var selecting = false
  @Volatile internal var selectionWindowId: Int? = null
    private set
  @Volatile internal var preparingSelection = false
    private set
  private val backgroundReadBusy = AtomicBoolean(false)
  private val selectionReadBusy = AtomicBoolean(false)
  private var previous: Page? = null
  private var pending = false
  private var trackedNode: AccessibilityNodeInfo? = null
  private var replayCooldown = 0L
  private val attempted = mutableMapOf<String, Long>()
  var message = ""
    private set
  val active get() = learningActive

  private data class Control(
    val path: String, val id: String, val kind: String, val label: String, val hasLabel: Boolean,
    val bounds: Rect, val visible: Boolean, val clickable: Boolean, val enabled: Boolean,
    val close: Boolean, val unsafe: Boolean, val scrollable: Boolean, val readySkip: Boolean,
  )
  private data class Page(
    val pkg: String, val window: Int, val bounds: Rect, val shape: String,
    val controls: List<Control>, val at: Long, val updated: Long, val scroll: Long,
  )
  private data class SelectionWindow(val pkg: String, val window: Int, val updated: Long)
  private data class Selection(val page: Page, val control: Control, val selector: JSONObject)
  private enum class PickIssue { UNAVAILABLE, WINDOW_CHANGED, BLOCKED, NO_TARGET, AMBIGUOUS, UNIDENTIFIABLE }
  private data class PickResult(val selection: Selection? = null, val issue: PickIssue? = null)
  private data class Clicked(val page: Page, val node: AccessibilityNodeInfo) : Closeable {
    @Suppress("DEPRECATION") override fun close() { node.recycle() }
  }
  private class Frame(val page: Page, val nodes: Map<String, AccessibilityNodeInfo>) : Closeable {
    @Suppress("DEPRECATION") override fun close() { nodes.values.forEach { it.recycle() } }
  }

  private fun control(node: AccessibilityNodeInfo, path: String, pkg: String, insideReadySkip: Boolean = false): Control {
    val bounds = Rect().also(node::getBoundsInScreen)
    // Never copy an input's contents into a learning fingerprint.
    val labels = if (node.isPassword || node.isEditable) emptyList()
      else listOfNotNull(node.text?.toString(), node.contentDescription?.toString()).mapNotNull { readable(it) }
    val text = labels.joinToString(" ").take(240).replace(Regex("\\s+"), " ").trim()
    val id = node.viewIdResourceName.orEmpty()
    val clickable = node.isClickable && node.actionList.any { it.id == AccessibilityNodeInfo.ACTION_CLICK }
    val action = AdSemanticRules.controlLabel(labels)
    val readySkip = clickable && node.isEnabled &&
      (action?.kind == AdSemanticRules.ControlKind.SKIP || AdSemanticRules.isSkipResource(id)) &&
      !AdSemanticRules.hasPendingSkipHint(labels)
    // Only a ready skip and its own counter children may ignore countdown changes.
    val identity = if (readySkip || insideReadySkip) labels.joinToString(" ") {
      AdSemanticRules.ocrEvidenceLabel(it, insideReadySkip)
    }.take(240).replace(Regex("\\s+"), " ").trim() else text
    val unsafe = node.isPassword || node.isEditable || node.isCheckable || node.isScrollable ||
      ProtectedUiPolicy.isProtectedResourceId(id) || AdSemanticRules.isBusinessControlResource(id) ||
      AdSemanticRules.isBlockedControlLabel(text) ||
      AdCreativeRules.isUnsafeCaiyunInterstitialClickId(pkg, id) ||
      Regex("购买|支付|下载|安装|领取|同意|允许|授权|登录|buy|purchase|install|download|accept|allow",
        RegexOption.IGNORE_CASE).containsMatchIn(text)
    return Control(path, id, node.className.toString(), digest(identity), text.isNotBlank(), bounds,
      node.isVisibleToUser && !bounds.isEmpty,
      clickable, node.isEnabled,
      action != null || readySkip ||
        Regex("(?:^|[_/])(close|skip|dismiss)(?:$|[_])", RegexOption.IGNORE_CASE).containsMatchIn(id),
      unsafe, node.isScrollable, readySkip)
  }

  private fun frame(nodeLimit: Int = 500, childLimit: Int = 150, depthLimit: Int = 30,
    keepReading: () -> Boolean = { true }): Frame? {
    if (!keepReading()) return null
    val root = runCatching(safeRoot).getOrNull() ?: return null
    val nodes = linkedMapOf<String, AccessibilityNodeInfo>()
    val controls = mutableListOf<Control>()
    var truncated = false
    fun visit(node: AccessibilityNodeInfo, path: String, depth: Int, pkg: String, insideReadySkip: Boolean = false) {
      nodes[path] = node
      if (!keepReading()) { truncated = true; return }
      val captured = control(node, path, pkg, insideReadySkip)
      controls.add(captured)
      // Input controls are never candidates; do not read their descendants either.
      if (node.isPassword || node.isEditable) return
      if (depth >= depthLimit && node.childCount > 0 || node.childCount > childLimit) { truncated = true; return }
      for (index in 0 until node.childCount) {
        if (!keepReading() || nodes.size >= nodeLimit) { truncated = true; break }
        node.getChild(index)?.let { visit(it, "$path.$index", depth + 1, pkg, insideReadySkip || captured.readySkip) }
      }
    }
    return try {
      val pkg = root.packageName.toString()
      visit(root, "0", 0, pkg)
      if (truncated || !keepReading()) {
        if (truncated && selecting) Log.i("ClearScreenLearning", "frame-truncated")
        nodes.values.forEach { @Suppress("DEPRECATION") it.recycle() }
        return null
      }
      val bounds = Rect().also(root::getBoundsInScreen)
      val shape = digest(controls.filter { it.visible }.joinToString("|") {
        "${it.path}:${it.id}:${it.kind}:${it.label}:${it.bounds.flattenToString()}"
      })
      @Suppress("DEPRECATION")
      val updated = service.packageManager.getPackageInfo(pkg, 0).lastUpdateTime
      Frame(Page(pkg, root.windowId, bounds, shape, controls, SystemClock.uptimeMillis(), updated, scrollVersion), nodes)
    } catch (_: Exception) {
      nodes.values.forEach { @Suppress("DEPRECATION") it.recycle() }
      if (nodes.isEmpty()) { @Suppress("DEPRECATION") root.recycle() }
      null
    }
  }

  /** Binder/tree reads never run on the overlay's input thread and never queue behind a stuck read. */
  private fun <T> readAsync(
    work: (() -> Boolean) -> T,
    discard: (T) -> Unit = {},
    release: () -> Unit = {},
    requireLearning: Boolean = true,
    busy: AtomicBoolean = selectionReadBusy,
    timeoutMs: Long = 2_500L,
    finish: (Result<T>) -> Unit,
  ): Boolean {
    if (!busy.compareAndSet(false, true)) return false
    val session = generation
    val deadline = SystemClock.uptimeMillis() + timeoutMs
    val sessionCurrent = { generation == session && (!requireLearning || active) }
    val current = { sessionCurrent() && SystemClock.uptimeMillis() < deadline }
    var delivered = false // Only accessed on the handler thread.
    val timeout = Runnable {
      if (!delivered && sessionCurrent()) {
        delivered = true
        Log.i("ClearScreenLearning", "read-timeout")
        finish(Result.failure(TimeoutException()))
      }
    }
    handler.postDelayed(timeout, timeoutMs)
    val started = runCatching {
      Thread({
        val result = runCatching { try { work(current) } finally { release() } }
        busy.set(false)
        handler.post {
          handler.removeCallbacks(timeout)
          if (!delivered && current()) {
            delivered = true
            finish(result)
          } else {
            result.onSuccess(discard)
            if (!delivered && sessionCurrent()) {
              delivered = true
              finish(Result.failure(TimeoutException()))
            }
          }
        }
      }, "ClearScreenLearningReader").apply { isDaemon = true; start() }
    }
    if (started.isFailure) {
      release()
      busy.set(false)
      handler.removeCallbacks(timeout)
      delivered = true
      finish(Result.failure(started.exceptionOrNull()!!))
    }
    return true
  }

  /** Capture the window identity after checking the current window and safety state. */
  private fun selectionWindow(current: () -> Boolean): SelectionWindow? {
    if (!current()) return null
    val root = runCatching(safeRoot).getOrNull() ?: return null
    return try {
      val pkg = root.packageName?.toString()?.takeIf { it.isNotBlank() } ?: return null
      val window = root.windowId
      @Suppress("DEPRECATION")
      val updated = service.packageManager.getPackageInfo(pkg, 0).lastUpdateTime
      if (current()) SelectionWindow(pkg, window, updated) else null
    } finally { @Suppress("DEPRECATION") root.recycle() }
  }

  private fun eligible(page: Page, target: Control, allowUnclickable: Boolean = false) =
    target.visible && target.enabled && !target.unsafe && small(target.bounds, page.bounds) &&
      page.bounds.contains(target.bounds) &&
      (target.clickable || allowUnclickable && tappable(target)) &&
      page.controls.none { it.visible && it.unsafe && descendant(it.path, target.path) }

  /**
   * Some SDKs draw their dismiss icon without exposing ACTION_CLICK, so the
   * node is only reachable by a coordinate tap. Limit that fallback to
   * icon-sized close or unlabeled nodes; labelled content is never tapped.
   */
  private fun tappable(target: Control) = target.bounds.width() in 1..dp(72) &&
    target.bounds.height() in 1..dp(72) && (target.close || !target.hasLabel)

  /** The route starts at a card/dialog, never at a RecyclerView index or screen position. */
  private fun selectorFor(page: Page, target: Control): JSONObject? {
    var path = parent(target.path)
    repeat(6) {
      val anchor = page.controls.firstOrNull { it.path == path } ?: return null
      if (anchor.scrollable) return null
      val scope = page.controls.filter { it.path == path || descendant(it.path, path) }
      if (scope.size > 100 || scope.any { it.scrollable }) return null
      val context = scope.filter { it.path != path && it.path != target.path &&
        !descendant(it.path, target.path) && !descendant(target.path, it.path) }
      if (context.any { it.hasLabel } || target.id.isNotBlank() && anchor.id.isNotBlank() && context.any { it.id.isNotBlank() }) {
        val fingerprint = digest(scope.joinToString("|") {
          "${it.path.removePrefix(path)}:${it.id}:${it.kind}:${it.label}:${it.clickable}"
        })
        return JSONObject().put("version", 2).put("id", target.id).put("kind", target.kind)
          .put("label", target.label).put("anchorId", anchor.id).put("anchorKind", anchor.kind)
          .put("route", target.path.removePrefix(path)).put("context", fingerprint)
      }
      if (path == "0") return null
      path = parent(path)
    }
    return null
  }

  private fun matches(page: Page, selector: JSONObject, cache: MutableMap<String, JSONObject?> = mutableMapOf()): List<Control> {
    if (selector.optInt("version") != 2) return emptyList()
    val candidates = page.controls.filter {
      eligible(page, it, allowUnclickable = true) && it.id == selector.optString("id") &&
        it.kind == selector.optString("kind") && it.label == selector.optString("label")
    }
    if (candidates.size > 20) return emptyList()
    val stableSelectorFields = listOf("version", "id", "kind", "label", "anchorId", "anchorKind", "route")
    // Ad creatives commonly change the text of sibling nodes between impressions.
    // Keep the selected control, local anchor, relative route and uniqueness exact;
    // only relax the volatile whole-card text hash when the target itself has a
    // stable resource id, or a stable label under a resource-id anchor.
    val canIgnoreCreativeText = selector.optString("id").isNotBlank() ||
      selector.optString("label").isNotBlank() && selector.optString("anchorId").isNotBlank()
    return candidates.filter { candidate ->
      val live = if (cache.containsKey(candidate.path)) cache[candidate.path]
        else selectorFor(page, candidate).also { cache[candidate.path] = it }
      live != null && stableSelectorFields.all { live.optString(it) == selector.optString(it) } &&
        (live.optString("context") == selector.optString("context") ||
          canIgnoreCreativeText && candidates.size == 1)
    }
  }

  /** Share retry protection across duplicate rules for the same selected control. */
  private fun replayKey(rule: JSONObject, selector: JSONObject): String = listOf(
    rule.optString("pkg"), rule.optLong("updated").toString(), selector.optString("version"),
    selector.optString("id"), selector.optString("kind"), selector.optString("label"),
    selector.optString("anchorId"), selector.optString("anchorKind"), selector.optString("route"),
  ).joinToString("\u001f")

  /** Expand the finger hit area only; the saved rule and eventual action still use the real node. */
  private fun nearCompactControl(target: Control, x: Int, y: Int): Boolean {
    val bounds = target.bounds
    if (bounds.width() !in 1..dp(56) || bounds.height() !in 1..dp(56)) return false
    val horizontal = minOf(dp(28), maxOf(0, (dp(72) - bounds.width() + 1) / 2))
    val vertical = minOf(dp(28), maxOf(0, (dp(72) - bounds.height() + 1) / 2))
    return Rect(bounds).apply { inset(-horizontal, -vertical) }.contains(x, y)
  }

  private fun area(control: Control) =
    control.bounds.width().toLong() * control.bounds.height()

  /** 0 at the bounds edge, 1 at the most centered point on the tighter axis. */
  private fun centrality(control: Control, x: Int, y: Int): Float {
    val margin = minOf(x - control.bounds.left, control.bounds.right - x,
      y - control.bounds.top, control.bounds.bottom - y)
    val half = minOf(control.bounds.width(), control.bounds.height()) / 2f
    return if (half <= 0f) 0f else (margin / half).coerceIn(0f, 1f)
  }

  /** Tap-to-bounds distance; 0 when the tap is inside the bounds. */
  private fun gap(control: Control, x: Int, y: Int): Int {
    val dx = maxOf(control.bounds.left - x, x - control.bounds.right, 0)
    val dy = maxOf(control.bounds.top - y, y - control.bounds.bottom, 0)
    return maxOf(dx, dy)
  }

  private fun iconLike(control: Control) = control.close || !control.hasLabel

  /**
   * Whether the tap itself points at [target] rather than the overlapping
   * [other]. Duplicated nodes at one position, a close icon not larger or
   * aimed at least as well as its sibling, a much smaller control hit in its
   * central half, a decisively deeper hit, an icon cluster's ranked winner,
   * or a clearly nearer control are not ties. Comparable size and aim with
   * different roles stay ambiguous.
   */
  private fun tapPrefers(target: Control, other: Control, x: Int, y: Int): Boolean {
    if (other.bounds == target.bounds) return true
    val ta = area(target)
    val oa = area(other)
    val tc = centrality(target, x, y)
    val oc = centrality(other, x, y)
    if (target.close && !other.close && (ta <= oa || tc > oc || tc >= 0.5f)) return true
    if (ta * 2 <= oa && tc >= 0.5f) return true
    if (ta <= oa && tc >= 0.75f && tc >= oc + 0.2f) return true
    // Close/unlabeled icon pairs at one tap are one visual affordance (the
    // duplicated X nodes of ad trees); the ranked target stands.
    if (iconLike(target) && iconLike(other)) return true
    // A deep hit on an icon survives a labelled sibling that only clips the tap.
    if (iconLike(target) && tc >= 0.75f && tc >= oc + 0.2f) return true
    return gap(target, x, y) + dp(8) <= gap(other, x, y)
  }

  private fun pick(page: Page, x: Int, y: Int): PickResult {
    if (!page.bounds.contains(x, y)) return PickResult(issue = PickIssue.NO_TARGET)
    if (page.controls.any { it.visible && it.unsafe && small(it.bounds, page.bounds) && it.bounds.contains(x, y) })
      return PickResult(issue = PickIssue.BLOCKED)
    val exact = page.controls.filter { eligible(page, it, allowUnclickable = true) && it.bounds.contains(x, y) }
    // An unlabeled compact image/button may be a drawn X. Clickable nodes are
    // preferred; icon-sized unclickable ones stay eligible for a coordinate tap.
    val nearby = if (exact.isEmpty()) page.controls.filter {
      it.visible && it.enabled && nearCompactControl(it, x, y) && (it.clickable || tappable(it))
    } else emptyList()
    if (nearby.any { it.unsafe }) return PickResult(issue = PickIssue.BLOCKED)
    val candidates = (if (exact.isNotEmpty()) exact else nearby.filter {
      eligible(page, it, allowUnclickable = true) && (it.close || !it.hasLabel)
    })
      .sortedWith(compareByDescending<Control> { it.close }
        .thenBy { it.bounds.width().toLong() * it.bounds.height() }.thenByDescending { it.path.length })
    val target = candidates.firstOrNull() ?: return PickResult(issue = PickIssue.NO_TARGET)
    val competing = if (exact.isNotEmpty()) candidates else nearby
    // Overlapping siblings alone are not ambiguity: a precise tap on the
    // smaller/close control survives the ad creative or duplicated X nodes
    // behind it. Only a tie the tap point cannot decide stays AMBIGUOUS.
    if (competing.any {
        it.path != target.path && !descendant(target.path, it.path) && !descendant(it.path, target.path) &&
          !tapPrefers(target, it, x, y)
      })
      return PickResult(issue = PickIssue.AMBIGUOUS)
    val selector = selectorFor(page, target) ?: return PickResult(issue = PickIssue.UNIDENTIFIABLE)
    if (matches(page, selector).size != 1) return PickResult(issue = PickIssue.UNIDENTIFIABLE)
    return PickResult(selection = Selection(page, target, selector))
  }

  fun onRuleDeleted() {
    generation++
    preparingSelection = false
    updateToolbarSelectionState()
    clearPending()
    previous = null
    if (selecting) {
      selecting = false; selectionWindowId = null; removeOverlay()
      if (active) showToolbar()
    }
  }
  fun start() {
    stop()
    sessionStarted = SystemClock.uptimeMillis()
    learningActive = true
    message = "学习中"
    handler.post(tick)
  }
  fun stop(result: String = "") {
    generation++
    learningActive = false
    sessionStarted = 0L
    clearPending()
    selecting = false
    selectionWindowId = null
    preparingSelection = false
    previous = null
    handler.removeCallbacks(tick)
    removeOverlay()
    currentToast?.cancel()
    if (result.isNotBlank()) { message = result; toast(result) }
  }
  @Suppress("DEPRECATION")
  private fun clearPending() {
    pending = false
    trackedNode?.recycle()
    trackedNode = null
  }
  private fun beginAttempt(): Long {
    clearPending()
    val token = ++generation
    pending = true
    handler.postDelayed({
      if (generation == token && pending) {
        generation++
        clearPending()
        if (active) toast("处理超时，请重试") else resumeScan()
      }
    }, 10_000L)
    return token
  }
  private val tick = object : Runnable {
    override fun run() {
      if (!active) return
      if (!allowed()) { stop(); return }
      runCatching {
        if (!pending && !selecting && !preparingSelection) {
          readAsync(work = { current -> frame(keepReading = current)?.use { it.page } }, busy = backgroundReadBusy) { result ->
            if (!pending && !selecting && !preparingSelection) previous = result.getOrNull()
          }
        }
        if (!selecting && overlay == null) showToolbar()
      }.onFailure { stop("学习暂不可用") }
      if (active) handler.postDelayed(this, if (selecting) 700L else 400L)
    }
  }

  fun onEvent(event: AccessibilityEvent) {
    // A scroll cannot be mistaken for successfully dismissing the selected card.
    if (event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED) scrollVersion++
    if (!active || !allowed() || pending || selecting || event.eventType != AccessibilityEvent.TYPE_VIEW_CLICKED) return
    val page = previous ?: return
    if (event.packageName?.toString() != page.pkg || event.windowId != page.window ||
      SystemClock.uptimeMillis() - page.at > 650L || event.eventTime < sessionStarted) return
    @Suppress("DEPRECATION") val copy = AccessibilityEvent.obtain(event)
    if (!readAsync(work = { current ->
        val source = if (current() && allowed()) copy.source else null
        try {
          if (source == null || !current()) null else {
            val bounds = Rect().also(source::getBoundsInScreen)
            val target = page.controls.filter { eligible(page, it) && it.close && it.bounds == bounds &&
              it.id == source.viewIdResourceName.orEmpty() && it.kind == source.className.toString() }.singleOrNull()
            val selector = target?.let { selectorFor(page, it) }
            if (selector == null || matches(page, selector).size != 1) null else {
              @Suppress("DEPRECATION") val tracked = AccessibilityNodeInfo.obtain(source)
              selector to Clicked(page, tracked)
            }
          }
        } finally { @Suppress("DEPRECATION") source?.recycle() }
      }, discard = { it?.second?.close() }, release = { @Suppress("DEPRECATION") copy.recycle() },
      busy = backgroundReadBusy) { result ->
        val learned = result.getOrNull() ?: return@readAsync
        if (pending || selecting || preparingSelection) { learned.second.close(); return@readAsync }
        beginAttempt()
        trackedNode = learned.second.node
        verifyLearning(page, learned.first)
      }) { @Suppress("DEPRECATION") copy.recycle() }
  }

  private fun showToolbar() {
    val select = button("选择控件") { selectControl() }
    val row = LinearLayout(service).apply {
      orientation = LinearLayout.HORIZONTAL
      background = background()
      elevation = dp(4).toFloat()
      addView(select)
      addView(button("取消") {
        Log.i("ClearScreenLearning", "toolbar-cancel")
        stop("已取消学习")
      })
    }
    addOverlay(row, WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT,
      WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
      WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT).apply {
      gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL; y = dp(90)
    })
    if (overlay === row) toolbarSelectButton = select
    updateToolbarSelectionState()
  }
  private fun button(title: String, action: () -> Unit) = TextView(service).apply {
    text = title; textSize = 14f; setTextColor(Color.WHITE); gravity = Gravity.CENTER
    setPadding(dp(14), dp(12), dp(14), dp(12)); minHeight = dp(48)
    contentDescription = title
    setOnClickListener { runCatching(action).onFailure { stop("学习暂不可用") } }
  }

  private fun selectControl() {
    Log.i("ClearScreenLearning", "toolbar-select")
    if (!active) return
    if (pending) { toast("正在处理，请稍候"); return }
    if (preparingSelection) return
    preparingSelection = true
    updateToolbarSelectionState()
    previous = null
    if (!readAsync(work = ::selectionWindow) { result ->
      preparingSelection = false
      updateToolbarSelectionState()
      val before = result.getOrNull()
      if (before == null) {
        Log.i("ClearScreenLearning", if (result.isFailure) "entry-failed" else "entry-unavailable")
        toast(if (result.isFailure) readFailure(result) else "当前界面暂不可读取")
      } else showControlSelector(before)
    }) {
      preparingSelection = false
      updateToolbarSelectionState()
      Log.i("ClearScreenLearning", "entry-busy")
      toast("控件读取尚未完成")
    }
  }

  private fun updateToolbarSelectionState() {
    toolbarSelectButton?.apply {
      text = if (preparingSelection) "读取中" else "选择控件"
      contentDescription = text
      isEnabled = !preparingSelection
      alpha = if (preparingSelection) 0.7f else 1f
    }
  }

  private fun showControlSelector(before: SelectionWindow) {
    if (!active || pending || selecting || !allowed()) return
    Log.i("ClearScreenLearning", "selector-open")
    removeOverlay()
    selecting = true
    selectionWindowId = before.window
    var selected: Selection? = null
    val confirm = button("确认选取") {
      val chosen = selected ?: return@button
      if (!active || !selecting) return@button
      val token = beginAttempt()
      removeOverlay(); selecting = false; selectionWindowId = null
      showToolbar()
      handler.postDelayed({
        if (token == generation && pending) {
          runCatching { confirmControl(chosen) }.onFailure {
            clearPending(); toast("控件不可用，请重新选择"); reopenControlSelection()
          }
        }
      }, 100L)
    }.apply { visibility = View.GONE }
    val actions = LinearLayout(service).apply {
      orientation = LinearLayout.HORIZONTAL; background = background()
      addView(confirm); addView(button("取消") {
        if (active && selecting && !pending) {
          Log.i("ClearScreenLearning", "selector-cancel")
          generation++; selecting = false; selectionWindowId = null
          removeOverlay(); showToolbar()
        }
      })
    }
    val status = TextView(service).apply {
      textSize = 13f; setTextColor(Color.WHITE); gravity = Gravity.CENTER
      setPadding(dp(12), dp(6), dp(12), dp(6))
      background = GradientDrawable().apply {
        setColor(Color.argb(225, 13, 39, 71)); cornerRadius = dp(14).toFloat()
      }
      visibility = View.GONE
    }
    fun showStatus(message: String) { status.text = message; status.visibility = View.VISIBLE }
    // Own the entire gesture at the window root. Never forward a selection tap to the app.
    val container = object : FrameLayout(service) {
      var downX = 0f; var downY = 0f
      var validTap = false
      var ignoringGesture = false
      var readingSelection = false
      var selectionRequest = 0L
      val marker = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(12, 119, 224); style = Paint.Style.STROKE; strokeWidth = dp(2).toFloat()
      }
      override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        val bounds = selected?.control?.bounds ?: return
        val location = IntArray(2).also(this::getLocationOnScreen)
        val box = RectF(bounds).apply { offset(-location[0].toFloat(), -location[1].toFloat()) }
        canvas.drawRoundRect(box, dp(4).toFloat(), dp(4).toFloat(), marker)
      }
      override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!active || !selecting || pending) return true
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
          if (readingSelection) {
            ignoringGesture = true
            showStatus("正在识别，请稍候")
            return true
          }
          ignoringGesture = false
          selectionRequest++
          validTap = event.pointerCount == 1
          downX = event.rawX; downY = event.rawY
          selected = null; confirm.visibility = View.GONE; invalidate()
          return true
        }
        if (ignoringGesture) {
          if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL)
            ignoringGesture = false
          return true
        }
        if (event.pointerCount != 1 || event.actionMasked == MotionEvent.ACTION_CANCEL ||
          abs(event.rawX - downX) > dp(12) || abs(event.rawY - downY) > dp(12)) validTap = false
        if (event.actionMasked == MotionEvent.ACTION_UP) {
          if (!validTap) { showStatus("请轻点关闭控件"); return true }
          validTap = false
          Log.i("ClearScreenLearning", "selector-tap")
          val x = event.rawX.toInt(); val y = event.rawY.toInt()
          val request = selectionRequest
          readingSelection = true
          showStatus("正在识别控件")
          if (!readAsync(work = { live ->
              frame(3000, 1000, 30, live)?.use { current ->
                if (sameWindow(before, current.page)) pick(current.page, x, y)
                else PickResult(issue = PickIssue.WINDOW_CHANGED)
              } ?: PickResult(issue = PickIssue.UNAVAILABLE)
            }, timeoutMs = 4_500L) { result ->
              if (!selecting || pending || this@ClearScreenAdLearning.overlay !== this || selectionRequest != request) return@readAsync
              readingSelection = false
              val outcome = result.getOrNull()
              selected = outcome?.selection
              Log.i("ClearScreenLearning", "selector-result=" +
                (if (result.isFailure) "failed" else outcome?.issue?.name?.lowercase() ?: "matched"))
              confirm.visibility = if (selected != null) View.VISIBLE else View.GONE
              invalidate()
              showStatus(if (result.isFailure) readFailure(result) else when (outcome?.issue) {
                PickIssue.UNAVAILABLE -> "无法读取当前页面"
                PickIssue.WINDOW_CHANGED -> "页面已变化，请重新选择"
                PickIssue.BLOCKED -> "此处的控件不可选"
                PickIssue.NO_TARGET -> "未找到可点击控件"
                PickIssue.AMBIGUOUS -> "附近有多个控件，请点准目标"
                PickIssue.UNIDENTIFIABLE -> "控件无法稳定识别"
                null -> "已选中，请确认"
              })
            }) {
            readingSelection = false
            showStatus("上一轮读取尚未结束")
          }
          performClick()
        }
        return true
      }
      override fun performClick(): Boolean { super.performClick(); return true }
      override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_BACK || event.keyCode == KeyEvent.KEYCODE_ESCAPE) {
          if (event.action == KeyEvent.ACTION_UP && active && selecting && !pending) {
            generation++; selecting = false; selectionWindowId = null
            removeOverlay(); showToolbar()
          }
          return true
        }
        return super.dispatchKeyEvent(event)
      }
    }.apply {
      contentDescription = "选择关闭控件"
      isClickable = true; isFocusableInTouchMode = true
      setBackgroundColor(Color.argb(18, 12, 119, 224))
      addView(actions, FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply {
        bottomMargin = dp(90)
      })
      addView(status, FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply {
        bottomMargin = dp(150)
        leftMargin = dp(16); rightMargin = dp(16)
      })
    }
    // Confirmation re-reads and validates the target; avoid repeated large tree scans on the UI thread.
    addOverlay(container, WindowManager.LayoutParams(-1, -1, WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
      WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
      PixelFormat.TRANSLUCENT).apply {
      // Unlike the small toolbar, selection is touch-modal, including transparent areas.
      gravity = Gravity.TOP or Gravity.START
      softInputMode = WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN or
        WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
      setTitle("净屏控件选择")
      if (Build.VERSION.SDK_INT >= 28) layoutInDisplayCutoutMode =
        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
      if (Build.VERSION.SDK_INT >= 30) setFitInsetsTypes(0)
    })
    container.requestFocus()
    toast("点选关闭控件")
  }

  private fun confirmControl(chosen: Selection) {
    if (!readAsync(work = { current ->
      frame(3000, 1000, 30, current)?.use { captured ->
        val target = matches(captured.page, chosen.selector).singleOrNull()
        if (!sameWindow(chosen.page, captured.page) || target == null || target.bounds != chosen.control.bounds ||
          chosen.page.scroll != captured.page.scroll) null else {
          var tracked: AccessibilityNodeInfo? = null
          val sent = click(captured, target, current) { tracked = it }
          if (sent && tracked != null) Clicked(captured.page, tracked!!) else {
            @Suppress("DEPRECATION") tracked?.recycle()
            null
          }
        }
      }
    }, discard = { it?.close() }) { result ->
      val clicked = result.getOrNull()
      if (clicked == null) {
        clearPending()
        toast(if (result.isFailure) readFailure(result) else "控件已变化或不可点击，请重新选择")
        reopenControlSelection()
      } else {
        trackedNode = clicked.node
        verifyLearning(clicked.page, chosen.selector, nodeLimit = 3000, childLimit = 1000, depthLimit = 30)
      }
    }) {
      clearPending(); toast("正在读取控件，请稍候"); reopenControlSelection()
    }
  }

  private fun click(frame: Frame, target: Control, current: () -> Boolean = { true },
    track: (AccessibilityNodeInfo) -> Unit = { trackedNode = it }): Boolean = runCatching {
    if (!current() || !allowed() || !eligible(frame.page, target, allowUnclickable = true) ||
      !canTouch(target.bounds)) return@runCatching false
    val live = frame.nodes[target.path] ?: return@runCatching false
    if (!current() || !live.refresh() || live.packageName?.toString() != frame.page.pkg || live.windowId != frame.page.window) return@runCatching false
    val insideReadySkip = frame.page.controls.any { it.readySkip && descendant(target.path, it.path) }
    val refreshed = control(live, target.path, frame.page.pkg, insideReadySkip)
    if (!current() || !allowed() || !eligible(frame.page, refreshed, allowUnclickable = true) ||
      refreshed.id != target.id || refreshed.kind != target.kind ||
      refreshed.label != target.label || refreshed.bounds != target.bounds) return@runCatching false
    @Suppress("DEPRECATION")
    val tracked = AccessibilityNodeInfo.obtain(live)
    track(tracked)
    // Do not hold the consent lock during Binder reads (especially reads of our own overlay).
    synchronized(ClearScreenPrivacyConsentStore.operationLock) {
      if (!current() || !allowed()) false
      else if (live.isClickable && live.actionList.any { it.id == AccessibilityNodeInfo.ACTION_CLICK })
        live.performAction(AccessibilityNodeInfo.ACTION_CLICK)
      else touch(live, current)
    }
  }.getOrDefault(false)

  /** A node the SDK hides from accessibility still responds to a real touch at its center. */
  private fun touch(node: AccessibilityNodeInfo, current: () -> Boolean): Boolean {
    val bounds = Rect().also(node::getBoundsInScreen)
    if (bounds.isEmpty || !current() || !allowed() || !canTouch(bounds)) return false
    val path = Path().apply { moveTo(bounds.exactCenterX(), bounds.exactCenterY()) }
    val gesture = GestureDescription.Builder()
      .addStroke(GestureDescription.StrokeDescription(path, 0L, 60L)).build()
    return runCatching { service.dispatchGesture(gesture, null, null) }.getOrDefault(false)
  }

  private fun verifyLearning(page: Page, selector: JSONObject, nodeLimit: Int = 500, childLimit: Int = 150,
    depthLimit: Int = 30) {
    val rule = JSONObject().put("key", UUID.randomUUID().toString()).put("pkg", page.pkg)
      .put("updated", page.updated).put("selector", selector).put("created", System.currentTimeMillis())
    checkClosure(page, nodeLimit, childLimit, depthLimit) { closed ->
      if (closed) {
        previous = null
        message = if (runCatching { store.saveLearnedAdRule(rule) }.getOrDefault(false)) "已学会关闭" else "学习规则保存失败"
        toast(message)
      } else {
        toast("未确认控件关闭，请重新选择")
        reopenControlSelection()
      }
    }
  }

  private fun reopenControlSelection() {
    if (!active || pending || selecting) return
    val token = generation
    handler.postDelayed({
      if (generation == token && active && allowed() && !pending && !selecting) {
        runCatching { selectControl() }.onFailure { toast("控件读取失败") }
      }
    }, 120L)
  }

  /** Require two disappearance samples; the attempt deadline also bounds slow Binder reads. */
  private fun checkClosure(page: Page, nodeLimit: Int = 500, childLimit: Int = 150, depthLimit: Int = 30,
    finish: (Boolean) -> Unit) {
    val token = generation
    var goneFrames = 0
    fun verify(attempt: Int) {
      handler.postDelayed({
        if (generation != token || !pending || !allowed()) return@postDelayed
        @Suppress("DEPRECATION") val tracked = trackedNode?.let { AccessibilityNodeInfo.obtain(it) }
        if (!readAsync(work = { current ->
            val after = frame(nodeLimit, childLimit, depthLimit, current)?.use { it.page }
          val gone = current() && tracked?.let { !it.refresh() || !it.isVisibleToUser } == true
          after != null && after.pkg == page.pkg && after.updated == page.updated &&
            after.scroll == page.scroll && after.shape != page.shape && gone
        }, release = { @Suppress("DEPRECATION") tracked?.recycle() }, requireLearning = false,
          timeoutMs = 4_500L) { result ->
          goneFrames = if (result.getOrDefault(false)) goneFrames + 1 else 0
          if (result.isFailure || goneFrames >= 2 || attempt >= 2) {
            clearPending(); runCatching { finish(goneFrames >= 2) }
            if (!active) handler.post { if (generation == token && allowed() && !pending) resumeScan() }
          } else verify(attempt + 1)
        }) {
          @Suppress("DEPRECATION") tracked?.recycle()
          clearPending(); runCatching { finish(false) }
          if (!active) handler.post { if (generation == token && allowed() && !pending) resumeScan() }
        }
      }, if (attempt == 0) 300L else 500L)
    }
    verify(0)
  }

  /** Resolve identity in the current tree; scrolling may change bounds and the outer list index. */
  fun tryReplay(pkg: String): Boolean = synchronized(ClearScreenPrivacyConsentStore.operationLock) {
    if (active || selecting || pending) return@synchronized true
    if (!allowed() || SystemClock.uptimeMillis() < replayCooldown) return@synchronized false
    val rules = store.getLearnedAdRules()
    if (rules.length() == 0) return@synchronized false
    val current = frame() ?: return@synchronized false
    current.use { captured ->
      val page = captured.page
      if (page.pkg != pkg) return@synchronized false
      val retainedKeys = (0 until rules.length()).mapNotNull { index ->
        rules.optJSONObject(index)?.let { rule ->
          rule.optJSONObject("selector")?.let { selector -> replayKey(rule, selector) }
        }
      }.toSet()
      attempted.keys.retainAll(retainedKeys)
      val now = SystemClock.uptimeMillis()
      val selectors = mutableMapOf<String, JSONObject?>()
      for (index in rules.length() - 1 downTo 0) {
        val rule = rules.optJSONObject(index) ?: continue
        val selector = rule.optJSONObject("selector") ?: continue // Legacy coordinate rules remain deletable, never replay.
        if (rule.optString("pkg") != pkg || rule.optLong("updated") != page.updated || selector.optInt("version") != 2) continue
        if (matches(page, selector, selectors).size != 1) continue
        val key = rule.optString("key")
        if (key.isBlank()) continue
        val attemptKey = replayKey(rule, selector)
        val lastAttempt = attempted[attemptKey]
        if (lastAttempt != null && now - lastAttempt < 30_000L) continue
        attempted.remove(attemptKey)
        val token = beginAttempt()
        handler.postDelayed({ synchronized(ClearScreenPrivacyConsentStore.operationLock) {
          if (generation != token || !pending || active || !allowed()) return@synchronized
          val sent = runCatching { frame()?.use {
            val live = matches(it.page, selector).singleOrNull()
            if (sameWindow(page, it.page) && page.scroll == it.page.scroll && live != null) {
              attempted[attemptKey] = SystemClock.uptimeMillis()
              val sent = click(it, live)
              if (sent) checkClosure(it.page) { closed ->
                synchronized(ClearScreenPrivacyConsentStore.operationLock) {
                  if (closed) attempted.remove(attemptKey)
                  store.appendEvent("skip", pkg, "learned-ad-close", if (closed) "success" else "unconfirmed",
                    if (closed) "已执行跳过" else "处理记录")
                }
              } else {
                attempted.remove(attemptKey)
              }
              sent
            } else false
          } ?: false }.getOrDefault(false)
          if (!sent) {
            clearPending(); replayCooldown = SystemClock.uptimeMillis() + 250L
            handler.post { if (generation == token && allowed() && !active && !pending) resumeScan() }
          }
        } }, 180L)
        return@synchronized true
      }
      false
    }
  }

  private fun sameWindow(a: Page, b: Page) = a.pkg == b.pkg && a.window == b.window && a.updated == b.updated
  private fun sameWindow(a: SelectionWindow, b: Page) = a.pkg == b.pkg && a.window == b.window && a.updated == b.updated
  private fun allowed() = ClearScreenPrivacyConsentStore(service).hasCurrentConsent() && store.isMasterEnabled()
  private fun small(bounds: Rect, root: Rect) = bounds.width() in 1..dp(150) && bounds.height() in 1..dp(100) &&
    bounds.width().toLong() * bounds.height() < root.width().toLong() * root.height() / 10
  private fun dp(value: Int) = (value * service.resources.displayMetrics.density).toInt()
  private fun toast(text: String) {
    currentToast?.cancel()
    currentToast = Toast.makeText(service, text, Toast.LENGTH_SHORT).also { it.show() }
  }
  private fun readFailure(result: Result<*>) =
    if (result.exceptionOrNull() is TimeoutException) "控件读取超时，请重试" else "控件读取失败，请重试"
  private fun background() = GradientDrawable().apply { setColor(Color.rgb(12, 119, 224)); cornerRadius = dp(24).toFloat() }
  private fun addOverlay(view: View, params: WindowManager.LayoutParams) {
    removeOverlay()
    try { wm.addView(view, params); overlay = view } catch (_: Exception) { stop("学习浮层不可用") }
  }
  private fun removeOverlay() {
    overlay?.let { runCatching { wm.removeViewImmediate(it) } }
    overlay = null
    toolbarSelectButton = null
  }
  companion object {
    private fun digest(text: String) = MessageDigest.getInstance("SHA-256").digest(text.toByteArray())
      .joinToString("") { "%02x".format(it.toInt() and 255) }
    /**
     * Ad SDKs expose opaque per-render tokens (for example "18d5b4") as
     * content descriptions. They are not user-facing labels and must not
     * become part of a learned fingerprint.
     */
    private fun readable(value: String): String? {
      val text = value.trim()
      if (text.isEmpty() || text == "virtualNode") return null
      val opaque = text.length in 6..12 &&
        text.all { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' } &&
        text.any { it in 'a'..'f' || it in 'A'..'F' }
      return if (opaque) null else text
    }
    private fun parent(path: String) = path.substringBeforeLast('.', "")
    private fun descendant(path: String, ancestor: String) = path.startsWith("$ancestor.")
  }
}

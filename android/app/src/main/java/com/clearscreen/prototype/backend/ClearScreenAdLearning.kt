package com.clearscreen.prototype.backend

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
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
  private val validatePage: (AccessibilityNodeInfo, PageSafety) -> Boolean,
  private val canTouch: (String, Int, Rect) -> Boolean,
  private val resumeScan: () -> Unit,
  private val resumeAfterReplayMiss: () -> Unit,
  private val sendGesture: (GestureDescription, () -> Boolean) -> Boolean,
) {
  private val wm = service.getSystemService(WindowManager::class.java)
  private var sessionStarted = 0L
  @Volatile private var learningActive = false
  @Volatile private var generation = 0L
  @Volatile private var selectionGeneration = 0L
  @Volatile private var scrollVersion = 0L
  private var overlay: View? = null
  @Volatile private var selectorOverlayId: Int? = null
  private var selectionRefresh: ((SelectionWindow) -> Unit)? = null
  @Volatile private var selectionDirty = false
  @Volatile private var ignoreObservedClicksUntil = 0L
  private var toolbarSelectButton: TextView? = null
  private var currentToast: Toast? = null
  @Volatile private var selecting = false
  @Volatile internal var selectionWindowId: Int? = null
    private set
  @Volatile internal var preparingSelection = false
    private set
  private val backgroundReadBusy = AtomicBoolean(false)
  private val selectionReadBusy = AtomicBoolean(false)
  private val replayReadBusy = AtomicBoolean(false)
  private var previous: Page? = null
  @Volatile private var pending = false
  @Volatile private var replaying = false
  private var trackedNode: AccessibilityNodeInfo? = null
  private var replayCooldown = 0L
  private val attempted = mutableMapOf<String, Long>()
  private val attemptedControls = mutableMapOf<String, Long>()
  var message = ""
    private set
  val active get() = learningActive
  internal val replayPending get() = pending && replaying
  internal val blocksReplay get() = pending && !replaying
  internal fun ownsSelectionOverlay(windowId: Int): Boolean =
    selecting && selectorOverlayId == windowId
  internal fun onAutomaticAction() {
    ignoreObservedClicksUntil = SystemClock.uptimeMillis() + 1_000L
    if (selecting) selectionDirty = true
  }
  private var replayStatus = "idle"
  private var replayStatusAt = 0L
  private var replayLookupMs = 0L
  private var replayLookupReadMs = 0L
  private var replayLookupMatchMs = 0L
  private var replayDispatchMs = 0L
  internal fun replayDiagnostics(): String {
    val rules = store.getLearnedAdRules()
    return "active=$active selecting=$selecting preparing=$preparingSelection pending=$pending " +
      "replaying=$replaying rules=${rules.length()} status=$replayStatus " +
      "lookupMs=$replayLookupMs lookupReadMs=$replayLookupReadMs lookupMatchMs=$replayLookupMatchMs " +
      "dispatchMs=$replayDispatchMs " +
      "ageMs=${if (replayStatusAt == 0L) -1L else SystemClock.uptimeMillis() - replayStatusAt}"
  }
  private fun recordReplay(status: String) {
    replayStatus = status
    replayStatusAt = SystemClock.uptimeMillis()
  }

  /** Ephemeral safety evidence, discarded before returning a learning frame. */
  internal class PageSafety {
    val visibleLabels = mutableListOf<String>()
    val visibleResourceIds = mutableListOf<String>()
    var hasVisiblePassword = false
    var hasVisibleFocusedInput = false
  }

  private data class Control(
    val path: String, val id: String, val kind: String, val label: String, val hasLabel: Boolean,
    val bounds: Rect, val visible: Boolean, val clickable: Boolean, val enabled: Boolean,
    val close: Boolean, val unsafe: Boolean, val scrollable: Boolean, val readySkip: Boolean, val adBadge: Boolean,
  )
  private data class Page(
    val pkg: String, val window: Int, val bounds: Rect, val shape: String,
    val controls: List<Control>, val at: Long, val updated: Long, val scroll: Long,
  ) {
    val unsafePaths = controls.filter { it.visible && it.unsafe }.map { it.path }
  }
  private data class SelectionMarker(val hitBounds: Rect, val drawBounds: Rect, val target: Control, val selectable: Boolean)
  private data class SelectionWindow(val pkg: String, val window: Int, val updated: Long,
    val markers: List<SelectionMarker>, val page: Page)
  private data class Selection(val page: Page, val control: Control, val selector: JSONObject)
  private data class ReplayCandidate(val page: Page, val control: Control, val selector: JSONObject, val attemptKey: String)
  private data class ReplayRead(val candidate: ReplayCandidate? = null, val status: String,
    val readMs: Long = 0L, val matchMs: Long = 0L)
  private data class ReplayAttempt(val clicked: Clicked? = null, val actionAttempted: Boolean = false)
  private enum class PickIssue { UNAVAILABLE, WINDOW_CHANGED, TARGET_MOVED, BLOCKED, NO_TARGET, AMBIGUOUS, UNIDENTIFIABLE }
  private data class PickResult(val selection: Selection? = null, val issue: PickIssue? = null,
    val freshWindow: SelectionWindow? = null)
  private data class Clicked(val page: Page, val node: AccessibilityNodeInfo) : Closeable {
    @Suppress("DEPRECATION") override fun close() { node.recycle() }
  }
  private class Frame(val page: Page, val nodes: Map<String, AccessibilityNodeInfo>) : Closeable {
    @Suppress("DEPRECATION") override fun close() { nodes.values.forEach { it.recycle() } }
  }

  private fun control(node: AccessibilityNodeInfo, path: String, pkg: String, insideReadySkip: Boolean = false,
    safety: PageSafety? = null): Control {
    val bounds = Rect().also(node::getBoundsInScreen)
    // Never copy an input's contents into a learning fingerprint.
    val rawLabels = if (node.isPassword || node.isEditable) emptyList()
      else listOfNotNull(node.text?.toString(), node.contentDescription?.toString())
    val labels = rawLabels.mapNotNull { readable(it) }
    val text = labels.joinToString(" ").take(240).replace(WHITESPACE, " ").trim()
    val id = node.viewIdResourceName.orEmpty()
    val visible = node.isVisibleToUser && !bounds.isEmpty
    if (safety != null && visible) {
      // Protection uses the complete label, not the truncated/readable-only
      // fingerprint. Input text and descendants are never collected.
      safety.visibleLabels.add(rawLabels.mapNotNull { it.trim().takeIf(String::isNotBlank) }.distinct().joinToString(" "))
      safety.visibleResourceIds.add(id)
      if (node.isPassword) safety.hasVisiblePassword = true
      if (node.isEditable && node.isFocused) safety.hasVisibleFocusedInput = true
    }
    val clickable = node.isClickable && node.actionList.any { it.id == AccessibilityNodeInfo.ACTION_CLICK }
    val action = AdSemanticRules.controlLabel(labels)
    val readySkip = clickable && node.isEnabled &&
      (action?.kind == AdSemanticRules.ControlKind.SKIP || AdSemanticRules.isSkipResource(id)) &&
      !AdSemanticRules.hasPendingSkipHint(labels)
    // Only a ready skip and its own counter children may ignore countdown changes.
    val identity = if (readySkip || insideReadySkip) labels.joinToString(" ") {
      AdSemanticRules.ocrEvidenceLabel(it, insideReadySkip)
    }.take(240).replace(WHITESPACE, " ").trim() else text
    val unsafe = node.isPassword || node.isEditable || node.isCheckable || node.isScrollable ||
      ProtectedUiPolicy.isProtectedResourceId(id) || AdSemanticRules.isBusinessControlResource(id) ||
      AdSemanticRules.isBlockedControlLabel(text) ||
      AdCreativeRules.isUnsafeCaiyunInterstitialClickId(pkg, id) ||
      UNSAFE_LABEL.containsMatchIn(text)
    return Control(path, id, node.className.toString(), if (identity.isEmpty()) EMPTY_LABEL_HASH else digest(identity), text.isNotBlank(), bounds,
      visible,
      clickable, node.isEnabled,
      action != null || readySkip ||
        CLOSE_RESOURCE.containsMatchIn(id),
      unsafe, node.isScrollable, readySkip, labels.any { AdSemanticRules.normalize(it) in AD_BADGES })
  }

  private fun frame(nodeLimit: Int = 500, childLimit: Int = 150, depthLimit: Int = 30,
    keepReading: () -> Boolean = { true }): Frame? {
    if (!keepReading()) return null
    val root = runCatching(safeRoot).getOrNull() ?: return null
    val nodes = linkedMapOf<String, AccessibilityNodeInfo>()
    val controls = mutableListOf<Control>()
    val safety = PageSafety()
    var truncated = false
    fun visit(node: AccessibilityNodeInfo, path: String, depth: Int, pkg: String, insideReadySkip: Boolean = false) {
      nodes[path] = node
      if (!keepReading()) { truncated = true; return }
      val captured = control(node, path, pkg, insideReadySkip, safety)
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
      if (truncated || !keepReading() || !validatePage(root, safety) || !keepReading()) {
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
    readVersion: () -> Long = { generation },
    finish: (Result<T>) -> Unit,
  ): Boolean {
    if (!busy.compareAndSet(false, true)) return false
    val session = readVersion()
    val deadline = SystemClock.uptimeMillis() + timeoutMs
    val sessionCurrent = { readVersion() == session && (!requireLearning || active) }
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

  /** Capture the current accessible controls and window identity before showing the selector. */
  private fun selectionWindow(current: () -> Boolean): SelectionWindow? {
    val captured = frame(3000, 1000, 30, current) ?: return null
    return captured.use {
      val page = it.page
      if (!current()) null else SelectionWindow(page.pkg, page.window, page.updated, selectionMarkers(page), page)
    }
  }

  private fun selectionMarkers(page: Page): List<SelectionMarker> {
    val controls = page.controls.filter { control ->
      control.visible && control.enabled && (control.clickable || tappable(control)) &&
        !control.bounds.isEmpty && page.bounds.contains(control.bounds)
    }
    val hitSize = dp(48)
    return controls.groupBy { it.bounds.centerX() to it.bounds.centerY() }.mapNotNull { (_, sameCenter) ->
      val closable = sameCenter.filter { eligible(page, it, allowUnclickable = true) && closeCandidate(it) }
      val drawControl = closable.minWithOrNull(
        compareByDescending<Control> { it.close }.thenBy { area(it) }.thenByDescending { it.path.length },
      ) ?: sameCenter.minWithOrNull(compareBy<Control> { area(it) }.thenByDescending { it.path.length })
        ?: return@mapNotNull null
      val drawBounds = Rect(drawControl.bounds)
      val hitBounds = Rect(drawBounds)
      if (hitBounds.width() < hitSize) {
        val extra = hitSize - hitBounds.width()
        hitBounds.left -= extra / 2
        hitBounds.right += extra - extra / 2
      }
      if (hitBounds.height() < hitSize) {
        val extra = hitSize - hitBounds.height()
        hitBounds.top -= extra / 2
        hitBounds.bottom += extra - extra / 2
      }
      if (!hitBounds.intersect(page.bounds)) return@mapNotNull null
      if (!drawBounds.intersect(page.bounds)) return@mapNotNull null
      SelectionMarker(hitBounds, drawBounds, drawControl.copy(bounds = Rect(drawControl.bounds)), closable.isNotEmpty())
    }
  }

  private fun markerAt(markers: List<SelectionMarker>, x: Int, y: Int): SelectionMarker? {
    val hits = markers.filter { it.hitBounds.contains(x, y) }
    if (hits.isEmpty()) return null
    fun distance(marker: SelectionMarker): Long {
      val dx = marker.drawBounds.centerX().toLong() - x
      val dy = marker.drawBounds.centerY().toLong() - y
      return dx * dx + dy * dy
    }
    // Prefer the actual small close box under the finger to a surrounding
    // parent's expanded touch area. Drawing and picking use the same node.
    val direct = hits.filter { it.selectable && it.drawBounds.contains(x, y) }
    val ranked = if (direct.isNotEmpty()) direct.sortedWith(compareBy<SelectionMarker> {
      area(it.target)
    }.thenBy(::distance)) else hits.sortedBy(::distance)
    if (ranked.size > 1 && distance(ranked[0]) == distance(ranked[1]) &&
      (direct.isEmpty() || area(ranked[0].target) == area(ranked[1].target))) return null
    return ranked.first()
  }
  private fun eligible(page: Page, target: Control, allowUnclickable: Boolean = false) =
    target.visible && target.enabled && !target.unsafe && small(target.bounds, page.bounds) &&
      page.bounds.contains(target.bounds) &&
      (target.clickable || allowUnclickable && tappable(target)) &&
      page.unsafePaths.none { descendant(it, target.path) }

  /**
   * Some SDKs draw their dismiss icon without exposing ACTION_CLICK, so the
   * node is only reachable by a coordinate tap. Limit that fallback to
   * icon-sized close or unlabeled nodes; labelled content is never tapped.
   */
  private fun tappable(target: Control) = target.bounds.width() in 1..dp(72) &&
    target.bounds.height() in 1..dp(72) && (target.close || !target.hasLabel)

  /** Describe only a bounded card/dialog, never a list index or screen position. */
  private fun selectorAt(page: Page, target: Control, anchor: Control, version: Int): JSONObject? {
    val path = anchor.path
    if (anchor.scrollable || !descendant(target.path, path)) return null
    val scope = page.controls.filter { it.path == path || descendant(it.path, path) }
    if (scope.size > 100 || scope.any { it.scrollable }) return null
    val context = scope.filter { it.path != path && it.path != target.path &&
      !descendant(it.path, target.path) && !descendant(target.path, it.path) }
    if (!(context.any { it.hasLabel } || target.id.isNotBlank() && anchor.id.isNotBlank() && context.any { it.id.isNotBlank() })) return null
    val fingerprint = digest(scope.joinToString("|") {
      "${it.path.removePrefix(path)}:${it.id}:${it.kind}:${it.label}:${it.clickable}"
    })
    val result = JSONObject().put("version", version).put("id", target.id).put("kind", target.kind)
      .put("label", target.label).put("anchorId", anchor.id).put("anchorKind", anchor.kind)
      .put("route", target.path.removePrefix(path)).put("context", fingerprint)
    if (version >= 3) {
      // Keep the target's ancestor chain and direct siblings, not volatile ad copy or creative descendants.
      val structure = scope.filter { it.path == path || descendant(target.path, it.path) || parent(it.path) == path }
      val anchorArea = anchor.bounds.width().toLong() * anchor.bounds.height()
      val pageArea = page.bounds.width().toLong() * page.bounds.height()
      val bounded = !anchor.bounds.isEmpty && page.bounds.contains(anchor.bounds) &&
        pageArea > 0 && anchorArea * 2 <= pageArea && anchor.bounds.height() * 5 <= page.bounds.height() * 3
      val localAdBadge = bounded && context.any { it.visible && it.adBadge && anchor.bounds.contains(it.bounds) }
      result.put("structure", digest(structure.joinToString("|") {
        "${it.path.removePrefix(path)}:${it.id}:${it.kind}:${it.clickable}"
      })).put("localAdBadge", localAdBadge)
      if (version >= 4) {
        // Child positions and ad copy change between impressions. Keep the
        // semantic ancestry and sibling roles, without numeric child indices.
        result.put("branch", digest(branch(page, target, anchor).joinToString("|") { role(it) }))
          .put("peers", digest(scope.filter { parent(it.path) == path }.map(::role).sorted().joinToString("|")))
      }
    }
    return result
  }

  private fun selectorFor(page: Page, target: Control): JSONObject? {
    var path = parent(target.path)
    var first: JSONObject? = null
    var identifiedAnchor: JSONObject? = null
    repeat(8) {
      val anchor = page.controls.firstOrNull { it.path == path } ?: return identifiedAnchor ?: first
      if (anchor.scrollable) return identifiedAnchor ?: first
      val scope = page.controls.filter { it.path == path || descendant(it.path, path) }
      if (scope.size > 100 || scope.any { it.scrollable }) return identifiedAnchor ?: first
      selectorAt(page, target, anchor, 4)?.let {
        if (first == null) first = it
        if (identifiedAnchor == null && anchor.id.isNotBlank()) identifiedAnchor = it
        if (it.optBoolean("localAdBadge")) return it
      }
      if (path == "0") return identifiedAnchor ?: first
      path = parent(path)
    }
    return identifiedAnchor ?: first
  }

  private fun role(control: Control) = "${control.id}:${control.kind}:${control.clickable}"

  private fun branch(page: Page, target: Control, anchor: Control): List<Control> =
    page.controls.filter { descendant(it.path, anchor.path) &&
      (it.path == target.path || descendant(target.path, it.path)) }
      .sortedBy { it.path.count { char -> char == '.' } }

  private fun hasStableIdentity(selector: JSONObject): Boolean {
    val label = selector.optString("label")
    return selector.optString("id").isNotBlank() ||
      label.isNotBlank() && label != EMPTY_LABEL_HASH && selector.optString("anchorId").isNotBlank()
  }

  private fun matches(page: Page, selector: JSONObject): List<Control> {
    val version = selector.optInt("version")
    if (version !in 2..4) return emptyList()
    val route = selector.optString("route")
    if (!route.startsWith('.')) return emptyList()
    val nodesByPath = page.controls.associateBy { it.path }
    // Narrow by the saved local anchor and route before considering how many
    // generic ImageView/X nodes the rest of the page happens to contain.
    val candidates = page.controls.mapNotNull { target ->
      if (target.id != selector.optString("id") || target.kind != selector.optString("kind") ||
        target.label != selector.optString("label") || !target.path.endsWith(route)) return@mapNotNull null
      val anchorPath = target.path.dropLast(route.length)
      val anchor = nodesByPath[anchorPath] ?: return@mapNotNull null
      if (anchor.id != selector.optString("anchorId") || anchor.kind != selector.optString("anchorKind") ||
        !eligible(page, target, allowUnclickable = true)) return@mapNotNull null
      target to anchor
    }
    if (candidates.size > 80) return emptyList()
    val label = selector.optString("label")
    val canIgnoreCreativeText = hasStableIdentity(selector)
    // Preserve the old globally unique, resource-id anchor case for unlabeled
    // controls, without treating SHA-256(empty) as a meaningful text label.
    val uniqueUnderKnownAnchor = selector.optString("id").isBlank() && label == EMPTY_LABEL_HASH &&
      selector.optString("anchorId").isNotBlank() && page.controls.asSequence().filter {
        it.id.isBlank() && it.kind == selector.optString("kind") && it.label == label &&
          eligible(page, it, allowUnclickable = true)
      }.take(2).count() == 1
    val unchangedContext = mutableListOf<Control>()
    val exact = candidates.mapNotNull { (target, anchor) ->
      val live = selectorAt(page, target, anchor, version) ?: return@mapNotNull null
      val exactContext = live.optString("context") == selector.optString("context")
      if (exactContext) unchangedContext.add(target)
      val stableIdentity = (canIgnoreCreativeText || uniqueUnderKnownAnchor) && candidates.size == 1
      val stableAdStructure = version >= 3 && candidates.size == 1 && selector.optBoolean("localAdBadge") &&
        live.optBoolean("localAdBadge") && selector.optString("structure").isNotBlank() &&
        live.optString("structure") == selector.optString("structure")
      // Older rules did not save a text-independent structure. Reuse their
      // original anchor/route only when a single target still belongs to a
      // bounded local card with an explicit ad badge in the current tree.
      val legacyAdContext = version == 2 && candidates.size == 1 && !exactContext && !stableIdentity &&
        selectorFor(page, target)?.optBoolean("localAdBadge") == true
      if (exactContext || stableIdentity || stableAdStructure || legacyAdContext) target else null
    }
    // The complete saved local tree still identifies anonymous sibling icons
    // when nothing changed. Relocation needs stronger, unambiguous identity.
    if (unchangedContext.isNotEmpty()) return unchangedContext
    val relocated = relocatedMatches(page, selector)
    // A saved numeric route must not conceal another equally identified
    // control that moved to a different child position in the same page.
    return (exact + relocated).distinctBy { it.path }
  }

  /** Resolve the learned control under its local anchor even if sibling indices changed. */
  private fun relocatedMatches(page: Page, selector: JSONObject): List<Control> {
    val version = selector.optInt("version")
    val stableIdentity = hasStableIdentity(selector)
    // Old anonymous controls did not store enough information to infer a new
    // path safely. They retain their original matching path above.
    if (version < 4 && !stableIdentity) return emptyList()
    val targets = page.controls.asSequence().filter {
      it.id == selector.optString("id") && it.kind == selector.optString("kind") &&
        it.label == selector.optString("label") && eligible(page, it, allowUnclickable = true) && closeCandidate(it)
    }
    val nodesByPath = page.controls.associateBy { it.path }
    return targets.filter { target ->
      var path = parent(target.path)
      var matched = false
      for (depth in 1..8) {
        val anchor = nodesByPath[path] ?: break
        if (anchor.scrollable) break
        if (anchor.id == selector.optString("anchorId") && anchor.kind == selector.optString("anchorKind")) {
          val live = selectorAt(page, target, anchor, version)
          if (live != null) {
            val sameBranch = version >= 4 && selector.optString("branch").isNotBlank() &&
              live.optString("branch") == selector.optString("branch")
            val sameLocalRoles = sameBranch && selector.optString("peers").isNotBlank() &&
              live.optString("peers") == selector.optString("peers") &&
              (anchor.id.isNotBlank() || selector.optBoolean("localAdBadge") && live.optBoolean("localAdBadge"))
            val knownAnchor = anchor.id.isNotBlank() || sameBranch ||
              depth == selector.optString("route").count { it == '.' }
            if (stableIdentity && knownAnchor || sameLocalRoles) { matched = true; break }
          }
        }
        if (path == "0") break
        path = parent(path)
      }
      matched
    }.take(2).toList() // Two distinct targets already prove ambiguity.
  }

  /** Share retry protection across duplicate rules for the same selected control. */
  private fun replayKey(rule: JSONObject, selector: JSONObject): String = listOf(
    rule.optString("pkg"), rule.optLong("updated").toString(),
    selector.optString("id"), selector.optString("kind"), selector.optString("label"),
    selector.optString("anchorId"), selector.optString("anchorKind"),
    selector.optString("branch").ifBlank { selector.optString("route") },
  ).joinToString("\u001f")

  /** Different saved rules can still point to the very same visible node. */
  private fun replayControlKey(page: Page, target: Control): String = listOf(
    page.pkg, page.updated.toString(), page.window.toString(), target.path, target.id, target.kind, target.label,
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

  private fun closeCandidate(target: Control) = target.close || !target.hasLabel && tappable(target)
  private fun pick(page: Page, x: Int, y: Int, markedPage: Page? = null, marker: SelectionMarker? = null): PickResult {
    if (!page.bounds.contains(x, y)) return PickResult(issue = PickIssue.NO_TARGET)
    if (page.controls.any { it.visible && it.unsafe && small(it.bounds, page.bounds) && it.bounds.contains(x, y) })
      return PickResult(issue = PickIssue.BLOCKED)
    if (markedPage != null && marker != null) return pickMarked(page, markedPage, marker)
    val exact = page.controls.filter { eligible(page, it, allowUnclickable = true) && closeCandidate(it) && it.bounds.contains(x, y) }
    // An unlabeled compact image/button may be a drawn X. Clickable nodes are
    // preferred; icon-sized unclickable ones stay eligible for a coordinate tap.
    val nearby = if (exact.isEmpty()) page.controls.filter {
      it.visible && it.enabled && nearCompactControl(it, x, y) && (it.clickable || tappable(it))
    } else emptyList()
    if (nearby.any { it.unsafe }) return PickResult(issue = PickIssue.BLOCKED)
    val candidates = (if (exact.isNotEmpty()) exact else nearby.filter {
      eligible(page, it, allowUnclickable = true) && closeCandidate(it)
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

  /** Re-identify the node whose box was selected, not a new page-wide tap winner. */
  private fun pickMarked(page: Page, markedPage: Page, marker: SelectionMarker): PickResult {
    if (!sameWindow(markedPage, page)) return PickResult(issue = PickIssue.WINDOW_CHANGED)
    val expected = marker.target
    val originalSelector = selectorFor(markedPage, expected)
    val identified = originalSelector?.let { matches(page, it) }.orEmpty()
    val atMarker = identified.filter { sameBoundsWithinTolerance(it.bounds, expected.bounds) }
    val candidates = if (atMarker.isNotEmpty()) atMarker else page.controls.filter {
      sameControlIdentity(expected, it) && sameBoundsWithinTolerance(it.bounds, expected.bounds) &&
        eligible(page, it, allowUnclickable = true) && closeCandidate(it)
    }
    if (candidates.size > 1) return PickResult(issue = PickIssue.AMBIGUOUS)
    val target = candidates.singleOrNull() ?: return PickResult(issue =
      if (identified.isNotEmpty()) PickIssue.TARGET_MOVED else PickIssue.NO_TARGET)
    // A newly exposed/reparented node may have the same visible identity and
    // bounds. Build its selector from the current local context, then require
    // that selector to identify this exact current node uniquely before acting.
    val selector = selectorFor(page, target) ?: return PickResult(issue = PickIssue.UNIDENTIFIABLE)
    val confirmed = matches(page, selector)
    if (confirmed.size > 1) return PickResult(issue = PickIssue.AMBIGUOUS)
    if (confirmed.singleOrNull()?.path != target.path) return PickResult(issue = PickIssue.UNIDENTIFIABLE)
    return PickResult(selection = Selection(page, target, selector))
  }

  fun onRuleDeleted() {
    generation++
    selectionGeneration++
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
  fun stop(result: String = "", resumeProcessing: Boolean = false) {
    generation++
    selectionGeneration++
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
    if (resumeProcessing) {
      val token = generation
      handler.post { if (generation == token && !active && !pending && allowed()) resumeScan() }
    }
  }
  @Suppress("DEPRECATION")
  private fun clearPending() {
    pending = false
    replaying = false
    trackedNode?.recycle()
    trackedNode = null
  }
  private fun beginAttempt(timeoutMs: Long = 10_000L, replay: Boolean = false): Long {
    clearPending()
    val token = ++generation
    replaying = replay
    pending = true
    handler.postDelayed({
      if (generation == token && pending) {
        val wasReplay = replaying
        generation++
        clearPending()
        if (wasReplay || !active) resumeScan() else toast("处理超时，请重试")
      }
    }, timeoutMs)
    return token
  }
  private val tick = object : Runnable {
    override fun run() {
      if (!active) return
      if (!allowed()) { stop(); return }
      runCatching {
        if (selecting && selectionDirty && !blocksReplay) {
          selectionDirty = false
          if (!readAsync(work = ::selectionWindow, busy = backgroundReadBusy, timeoutMs = 4_500L,
              readVersion = { selectionGeneration }) { result ->
              if (active && selecting) result.getOrNull()?.let { selectionRefresh?.invoke(it) }
            }) selectionDirty = true
        } else if (!pending && !selecting && !preparingSelection) {
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
    if (active && selecting && event.packageName?.toString() != service.packageName) selectionDirty = true
    if (!active || !allowed() || pending || selecting || event.eventType != AccessibilityEvent.TYPE_VIEW_CLICKED ||
      SystemClock.uptimeMillis() < ignoreObservedClicksUntil) return
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
    val select = toolbarButton("选择控件", primary = true) { selectControl() }
    val row = LinearLayout(service).apply {
      orientation = LinearLayout.HORIZONTAL
      gravity = Gravity.CENTER_VERTICAL
      background = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(
        Color.rgb(11, 130, 239), Color.rgb(0, 104, 214),
      )).apply {
        cornerRadius = dp(28).toFloat()
        setStroke(maxOf(1, dp(1)), Color.argb(55, 255, 255, 255))
      }
      elevation = dp(6).toFloat()
      addView(select)
      addView(View(service).apply {
        setBackgroundColor(Color.argb(100, 255, 255, 255))
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
      }, LinearLayout.LayoutParams(maxOf(1, dp(1)), dp(24)))
      addView(toolbarButton("取消", primary = false) {
        Log.i("ClearScreenLearning", "toolbar-cancel")
        stop("已取消学习", resumeProcessing = true)
      })
    }
    addOverlay(row, WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT,
      WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
      WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT).apply {
      gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL; y = dp(90)
    })
    if (overlay === row) toolbarSelectButton = select
    updateToolbarSelectionState()
    handler.post { if (active && allowed() && !blocksReplay) resumeScan() }
  }
  private fun toolbarButton(title: String, primary: Boolean, action: () -> Unit) = button(title, action).apply {
    textSize = 15f
    typeface = Typeface.create(if (primary) "sans-serif-medium" else "sans-serif", Typeface.NORMAL)
    setTextColor(if (primary) Color.WHITE else Color.rgb(233, 244, 255))
    setPadding(dp(18), dp(12), dp(18), dp(12))
    minWidth = dp(if (primary) 120 else 76)
    minHeight = dp(52)
    foreground = RippleDrawable(ColorStateList.valueOf(Color.argb(45, 255, 255, 255)), null,
      GradientDrawable().apply { setColor(Color.WHITE); cornerRadius = dp(26).toFloat() })
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
    if (blocksReplay) { toast("正在处理，请稍候"); return }
    if (preparingSelection) return
    preparingSelection = true
    updateToolbarSelectionState()
    previous = null
    if (!readAsync(work = ::selectionWindow, timeoutMs = 4_500L,
        readVersion = { selectionGeneration }) { result ->
      preparingSelection = false
      updateToolbarSelectionState()
      val before = result.getOrNull()
      if (before == null) {
        Log.i("ClearScreenLearning", if (result.isFailure) "entry-failed" else "entry-unavailable")
        toast(if (result.isFailure) readFailure(result) else "当前界面暂不可读取")
      } else if (before.markers.isEmpty()) {
        Log.i("ClearScreenLearning", "selector-no-markers")
        toast("当前页面没有可识别的控件")
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

  private fun finishSelection(chosen: Selection) {
    if (!active || blocksReplay || !selecting) return
    if (replayPending) { generation++; clearPending() }
    val token = beginAttempt()
    removeOverlay(); selecting = false; selectionWindowId = null
    showToolbar()
    handler.postDelayed({
      if (token == generation && pending) {
        runCatching { confirmControl(chosen) }.onFailure {
          clearPending(); toast("控件不可用，请重新选择")
        }
      }
    }, 100L)
  }

  private fun showControlSelector(before: SelectionWindow) {
    if (!active || blocksReplay || selecting || !allowed()) return
    Log.i("ClearScreenLearning", "selector-open")
    removeOverlay()
    selecting = true
    selectionWindowId = before.window
    var displayed = before
    val actions = LinearLayout(service).apply {
      orientation = LinearLayout.HORIZONTAL; background = background()
      addView(button("取消") {
        if (active && selecting && !blocksReplay) {
          Log.i("ClearScreenLearning", "selector-cancel")
          selectionGeneration++; selecting = false; selectionWindowId = null
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
    val markerOutline = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.rgb(12, 119, 224); style = Paint.Style.STROKE; strokeWidth = dp(2).toFloat()
    }
    val markerDisabledOutline = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.argb(150, 12, 119, 224); style = Paint.Style.STROKE; strokeWidth = dp(2).toFloat()
    }
    // Own the entire gesture at the window root. Never forward a selection tap to the app.
    val container = object : FrameLayout(service) {
      var downX = 0f; var downY = 0f
      var validTap = false
      var ignoringGesture = false
      var readingSelection = false
      var selectionRequest = 0L
      fun refreshMarkers(next: SelectionWindow) {
        if (readingSelection) { selectionDirty = true; return }
        if (next.pkg != displayed.pkg || next.window != displayed.window || next.updated != displayed.updated) {
          selectionGeneration++; selecting = false; selectionWindowId = null
          removeOverlay(); showToolbar()
          return
        }
        displayed = next
        invalidate()
      }
      override fun dispatchDraw(canvas: Canvas) {
        val location = IntArray(2).also(this::getLocationOnScreen)
        displayed.markers.forEach { marker ->
          val box = RectF(marker.drawBounds).apply { offset(-location[0].toFloat(), -location[1].toFloat()) }
          canvas.drawRoundRect(box, dp(4).toFloat(), dp(4).toFloat(), if (marker.selectable) markerOutline else markerDisabledOutline)
        }
        super.dispatchDraw(canvas)
      }
      override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!active || !selecting || blocksReplay) return true
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
          val marker = markerAt(displayed.markers, x, y)
          if (marker == null) {
            showStatus("请点击控件方框")
            performClick()
            return true
          }
          if (!marker.selectable) {
            showStatus("请选择可安全关闭的控件方框")
            performClick()
            return true
          }
          val request = selectionRequest
          val selectedWindow = displayed
          readingSelection = true
          showStatus("正在识别控件")
          if (!readAsync(work = { live ->
              frame(3000, 1000, 30, live)?.use { current ->
                val page = current.page
                val picked = if (sameWindow(selectedWindow, page)) pick(page, x, y, selectedWindow.page, marker)
                  else PickResult(issue = PickIssue.WINDOW_CHANGED)
                if (picked.issue == null) picked else picked.copy(freshWindow =
                  SelectionWindow(page.pkg, page.window, page.updated, selectionMarkers(page), page))
              } ?: PickResult(issue = PickIssue.UNAVAILABLE)
            }, timeoutMs = 4_500L, readVersion = { selectionGeneration }) { result ->
              if (!selecting || blocksReplay || this@ClearScreenAdLearning.overlay !== this || selectionRequest != request) return@readAsync
              readingSelection = false
              val outcome = result.getOrNull()
              Log.i("ClearScreenLearning", "selector-result=" +
                (if (result.isFailure) "failed" else if (outcome?.selection != null) "matched" else outcome?.issue?.name?.lowercase() ?: "no-target"))
              val chosen = outcome?.selection
              if (chosen != null) {
                finishSelection(chosen)
                return@readAsync
              }
              outcome?.freshWindow?.let { refreshMarkers(it) }
              if (this@ClearScreenAdLearning.overlay !== this) return@readAsync
              showStatus(if (result.isFailure) readFailure(result) else when (outcome?.issue) {
                PickIssue.UNAVAILABLE -> "无法读取当前页面"
                PickIssue.WINDOW_CHANGED -> "已切换页面，请重新选择"
                PickIssue.TARGET_MOVED -> "控件位置已更新，请重新点选"
                PickIssue.BLOCKED -> "此处的控件不可选"
                PickIssue.NO_TARGET -> "此处控件不可选"
                PickIssue.AMBIGUOUS -> "附近有多个控件，请点准目标"
                PickIssue.UNIDENTIFIABLE -> "控件无法稳定识别"
                null -> "未找到可点击控件"
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
          if (event.action == KeyEvent.ACTION_UP && active && selecting && !blocksReplay) {
            selectionGeneration++; selecting = false; selectionWindowId = null
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
    // Automatic confirmation re-reads and validates the target before the click.
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
    if (overlay === container) selectionRefresh = { next -> container.refreshMarkers(next) }
    handler.post { if (active && selecting && allowed() && !blocksReplay) resumeScan() }
    container.requestFocus()
    toast("点击蓝色关闭控件方框")
  }

  private fun confirmControl(chosen: Selection) {
    if (!readAsync(work = { current ->
      frame(3000, 1000, 30, current)?.use { captured ->
        val target = matches(captured.page, chosen.selector).singleOrNull()
        if (!sameWindow(chosen.page, captured.page) || target == null ||
          !sameBoundsWithinTolerance(target.bounds, chosen.control.bounds) ||
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
      } else {
        trackedNode = clicked.node
        verifyLearning(clicked.page, chosen.selector, nodeLimit = 3000, childLimit = 1000, depthLimit = 30,
          reopenOnFailure = false)
      }
    }) {
      clearPending(); toast("正在读取控件，请稍候")
    }
  }

  private fun click(frame: Frame, target: Control, current: () -> Boolean = { true },
    track: (AccessibilityNodeInfo) -> Unit = { trackedNode = it }): Boolean = runCatching {
    if (!current() || !allowed() || !eligible(frame.page, target, allowUnclickable = true) ||
      !canTouch(frame.page.pkg, frame.page.window, target.bounds)) return@runCatching false
    val live = frame.nodes[target.path] ?: return@runCatching false
    if (!current() || !live.refresh() || live.packageName?.toString() != frame.page.pkg || live.windowId != frame.page.window) return@runCatching false
    val insideReadySkip = frame.page.controls.any { it.readySkip && descendant(target.path, it.path) }
    val refreshed = control(live, target.path, frame.page.pkg, insideReadySkip)
    if (!current() || !allowed() || !eligible(frame.page, refreshed, allowUnclickable = true) ||
      refreshed.id != target.id || refreshed.kind != target.kind ||
      refreshed.label != target.label || !sameBoundsWithinTolerance(refreshed.bounds, target.bounds) ||
      !canTouch(frame.page.pkg, frame.page.window, refreshed.bounds)) return@runCatching false
    @Suppress("DEPRECATION")
    val tracked = AccessibilityNodeInfo.obtain(live)
    track(tracked)
    // Do not hold the consent lock during Binder reads (especially reads of our own overlay).
    if (live.isClickable && live.actionList.any { it.id == AccessibilityNodeInfo.ACTION_CLICK }) {
      synchronized(ClearScreenPrivacyConsentStore.operationLock) {
        val appRule = store.getAppRule(frame.page.pkg)
        if (!current() || !allowed() || (!active || replaying) && (!appRule.skipEnabled || appRule.whitelist)) false
        else {
          if (replaying) onAutomaticAction()
          live.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        }
      }
    } else {
      // UI-thread gesture dispatch must never wait while holding this lock.
      // The posted action re-checks consent, rules, window and generation there.
      touch(live, current)
    }
  }.getOrDefault(false)

  /** A node the SDK hides from accessibility still responds to a real touch at its center. */
  private fun touch(node: AccessibilityNodeInfo, current: () -> Boolean): Boolean {
    val bounds = Rect().also(node::getBoundsInScreen)
    if (bounds.isEmpty || !current() || !allowed() ||
      !canTouch(node.packageName?.toString().orEmpty(), node.windowId, bounds)) return false
    val path = Path().apply { moveTo(bounds.exactCenterX(), bounds.exactCenterY()) }
    val gesture = GestureDescription.Builder()
      .addStroke(GestureDescription.StrokeDescription(path, 0L, 60L)).build()
    val pkg = node.packageName?.toString().orEmpty()
    val window = node.windowId
    return runCatching {
      sendGesture(gesture) {
        val rule = store.getAppRule(pkg)
        current() && allowed() && canTouch(pkg, window, bounds) &&
          (active && !replaying || rule.skipEnabled && !rule.whitelist)
      }
    }.getOrDefault(false)
  }

  private fun verifyLearning(page: Page, selector: JSONObject, nodeLimit: Int = 500, childLimit: Int = 150,
    depthLimit: Int = 30, reopenOnFailure: Boolean = true) {
    val rule = JSONObject().put("key", UUID.randomUUID().toString()).put("pkg", page.pkg)
      .put("updated", page.updated).put("selector", selector).put("created", System.currentTimeMillis())
    checkClosure(page, nodeLimit, childLimit, depthLimit) { closed ->
      if (closed) {
        previous = null
        if (runCatching { store.saveLearnedAdRule(rule) }.getOrDefault(false)) {
          // Learning pauses normal scanning. Resume it once this rule has been
          // confirmed and saved so that the next encounter can use the rule.
          stop("已学会关闭")
          handler.post { if (!active && allowed() && !pending) resumeScan() }
        } else {
          message = "学习规则保存失败"
          toast(message)
        }
      } else {
        toast("未确认控件关闭，请重新选择")
        if (reopenOnFailure) reopenControlSelection()
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
    val wasReplay = replaying
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
          busy = if (wasReplay) replayReadBusy else selectionReadBusy,
          timeoutMs = 4_500L) { result ->
          goneFrames = if (result.getOrDefault(false)) goneFrames + 1 else 0
          if (result.isFailure || goneFrames >= 2 || attempt >= if (wasReplay) 3 else 2) {
            clearPending(); runCatching { finish(goneFrames >= 2) }
            if (wasReplay || !active) handler.post {
              if (generation == token && allowed() && !pending && !blocksReplay) resumeScan()
            }
          } else verify(attempt + 1)
        }) {
          @Suppress("DEPRECATION") tracked?.recycle()
          clearPending(); runCatching { finish(false) }
          if (wasReplay || !active) handler.post {
            if (generation == token && allowed() && !pending && !blocksReplay) resumeScan()
          }
        }
      }, if (wasReplay) { if (attempt == 0) 120L else if (attempt == 1) 180L else 500L }
        else if (attempt == 0) 300L else 500L)
    }
    verify(0)
  }

  /** Resolve identity in the current tree; scrolling may change bounds and the outer list index. */
  fun tryReplay(pkg: String): Boolean = synchronized(ClearScreenPrivacyConsentStore.operationLock) {
    if (blocksReplay || pending) return@synchronized true
    if (!allowed() || replayReadBusy.get() || SystemClock.uptimeMillis() < replayCooldown) return@synchronized false
    val rules = store.getLearnedAdRules()
    if (rules.length() == 0) return@synchronized false
    val retainedKeys = (0 until rules.length()).mapNotNull { index ->
      rules.optJSONObject(index)?.let { rule ->
        rule.optJSONObject("selector")?.let { selector -> replayKey(rule, selector) }
      }
    }.toSet()
    attempted.keys.retainAll(retainedKeys)
    val now = SystemClock.uptimeMillis()
    attemptedControls.entries.removeAll { now - it.value >= 30_000L }
    val blockedControls = attemptedControls.keys.toSet()
    val eligibleRules = (rules.length() - 1 downTo 0).mapNotNull { index ->
      val rule = rules.optJSONObject(index) ?: return@mapNotNull null
      val selector = rule.optJSONObject("selector") ?: return@mapNotNull null
      val attemptKey = replayKey(rule, selector)
      val lastAttempt = attempted[attemptKey]
      if (rule.optString("pkg") != pkg || rule.optString("key").isBlank() ||
        selector.optInt("version") !in 2..4 || lastAttempt != null && now - lastAttempt < 30_000L) null
      else rule
    }
    if (eligibleRules.isEmpty()) return@synchronized false

    // Selection accepts large trees; replay must use the same limits. Binder
    // reads stay off the service thread and are bounded by readAsync's deadline.
    replayLookupMs = 0L
    replayLookupReadMs = 0L
    replayLookupMatchMs = 0L
    replayDispatchMs = 0L
    recordReplay("reading")
    val replayStartedAt = SystemClock.uptimeMillis()
    val token = beginAttempt(25_000L, replay = true)
    if (!readAsync(work = { current ->
        val readStartedAt = SystemClock.uptimeMillis()
        val currentFrame = frame(3000, 1000, 30, current)
        val readMs = SystemClock.uptimeMillis() - readStartedAt
        val matchStartedAt = SystemClock.uptimeMillis()
        val read = currentFrame?.use { captured ->
          val page = captured.page
          if (!current() || page.pkg != pkg) ReplayRead(status = "foreground-changed") else {
            var versionMatches = false
            var ambiguous = false
            var coolingDown = false
            val candidate = eligibleRules.firstNotNullOfOrNull { rule ->
              val selector = rule.optJSONObject("selector") ?: return@firstNotNullOfOrNull null
              if (rule.optLong("updated") != page.updated) return@firstNotNullOfOrNull null
              versionMatches = true
              val found = matches(page, selector)
              if (found.size > 1) ambiguous = true
              found.singleOrNull()?.let { target ->
                if (replayControlKey(page, target) in blockedControls) {
                  coolingDown = true
                  null
                } else ReplayCandidate(page, target, selector, replayKey(rule, selector))
              }
            }
            ReplayRead(candidate, when {
              candidate != null -> "matched"
              !versionMatches -> "app-updated"
              coolingDown -> "cooldown"
              ambiguous -> "ambiguous"
              else -> "no-match"
            })
          }
        } ?: ReplayRead(status = "page-unavailable")
        read.copy(readMs = readMs, matchMs = SystemClock.uptimeMillis() - matchStartedAt)
      }, requireLearning = false, busy = replayReadBusy, timeoutMs = 4_500L) { result ->
        replayLookupMs = SystemClock.uptimeMillis() - replayStartedAt
        replayLookupReadMs = result.getOrNull()?.readMs ?: 0L
        replayLookupMatchMs = result.getOrNull()?.matchMs ?: 0L
        val candidate = result.getOrNull()?.candidate
        if (candidate == null) {
          recordReplay(if (result.isFailure) "read-failed" else result.getOrNull()?.status ?: "no-match")
          finishReplayWithoutClick(token)
        } else handler.postDelayed({
          if (generation != token || !replayPending || blocksReplay || !allowed()) return@postDelayed
          // Reserve the attempt before the background read; even a timeout after
          // a sent gesture must not repeatedly click the same visible control.
          val controlKey = replayControlKey(candidate.page, candidate.control)
          attempted[candidate.attemptKey] = SystemClock.uptimeMillis()
          attemptedControls[controlKey] = SystemClock.uptimeMillis()
          while (attemptedControls.size > 80) attemptedControls.remove(attemptedControls.keys.first())
          if (!readAsync(work = { current ->
              frame(3000, 1000, 30, current)?.use { captured ->
                val live = matches(captured.page, candidate.selector).singleOrNull()
                if (!current() || !sameWindow(candidate.page, captured.page) ||
                  candidate.page.scroll != captured.page.scroll || live == null ||
                  candidate.control.id != live.id || candidate.control.kind != live.kind ||
                  candidate.control.label != live.label || candidate.control.bounds != live.bounds) ReplayAttempt()
                else {
                  var tracked: AccessibilityNodeInfo? = null
                  val sent = click(captured, live, current) { tracked = it }
                  val clicked = if (sent && tracked != null) Clicked(captured.page, tracked!!) else {
                    @Suppress("DEPRECATION") tracked?.recycle()
                    null
                  }
                  ReplayAttempt(clicked, actionAttempted = tracked != null)
                }
              } ?: ReplayAttempt()
            }, discard = { it.clicked?.close() }, requireLearning = false, busy = replayReadBusy,
            timeoutMs = 4_500L) { clickResult ->
              val attempt = clickResult.getOrNull()
              if (attempt != null && !attempt.actionAttempted) {
                attempted.remove(candidate.attemptKey)
                attemptedControls.remove(controlKey)
              }
              val clicked = attempt?.clicked
              if (clicked == null) {
                recordReplay(if (clickResult.isFailure) "click-read-failed" else if (attempt?.actionAttempted == true)
                  "action-rejected" else "target-changed")
                finishReplayWithoutClick(token)
              }
              else {
                replayDispatchMs = SystemClock.uptimeMillis() - replayStartedAt
                recordReplay("verifying")
                trackedNode = clicked.node
                checkClosure(clicked.page, 3000, 1000, 30) { closed ->
                  synchronized(ClearScreenPrivacyConsentStore.operationLock) {
                    recordReplay(if (closed) "closed" else "unconfirmed")
                    if (closed) {
                      attempted.remove(candidate.attemptKey)
                      attemptedControls.remove(controlKey)
                    }
                    store.appendEvent("skip", pkg, "learned-ad-close", if (closed) "success" else "unconfirmed",
                      if (closed) "已执行跳过" else "处理记录")
                  }
                }
              }
            }) {
            attempted.remove(candidate.attemptKey)
            attemptedControls.remove(controlKey)
            finishReplayWithoutClick(token)
          }
        }, 60L)
      }) {
      clearPending()
      return@synchronized false
    }
    true
  }

  private fun finishReplayWithoutClick(token: Long) {
    if (generation != token || !pending) return
    clearPending()
    replayCooldown = SystemClock.uptimeMillis() + if (replayStatus == "no-match") 200L else 1_000L
    handler.post { if (generation == token && allowed() && !pending && !blocksReplay) resumeAfterReplayMiss() }
  }

  private fun sameControlIdentity(a: Control, b: Control) = a.id == b.id && a.kind == b.kind && a.label == b.label
  /** Allow only screen-pixel rounding, never relocation to another UI area. */
  private fun sameBoundsWithinTolerance(a: Rect, b: Rect): Boolean {
    val tolerance = maxOf(1, dp(2))
    return abs(a.left - b.left) <= tolerance && abs(a.top - b.top) <= tolerance &&
      abs(a.right - b.right) <= tolerance && abs(a.bottom - b.bottom) <= tolerance
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
  /** Called on the service thread so an injected ad tap reaches the application. */
  internal fun passThroughSelectionGesture(): () -> Unit {
    val view = overlay?.takeIf { selecting } ?: return {}
    val params = view.layoutParams as? WindowManager.LayoutParams ?: return {}
    val originalFlags = params.flags
    params.flags = originalFlags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
    wm.updateViewLayout(view, params)
    return {
      if (selecting && overlay === view) {
        params.flags = originalFlags
        runCatching { wm.updateViewLayout(view, params) }
        selectionDirty = true
      }
    }
  }
  private fun addOverlay(view: View, params: WindowManager.LayoutParams) {
    removeOverlay()
    try {
      wm.addView(view, params)
      overlay = view
      captureSelectorOverlayId(view)
      handler.post { if (overlay === view) captureSelectorOverlayId(view) }
    } catch (_: Exception) { stop("学习浮层不可用") }
  }
  private fun captureSelectorOverlayId(view: View) {
    if (!selecting || overlay !== view) return
    val info = view.createAccessibilityNodeInfo() ?: return
    try { selectorOverlayId = info.windowId.takeIf { it >= 0 } }
    finally { @Suppress("DEPRECATION") info.recycle() }
  }
  private fun removeOverlay() {
    overlay?.let { runCatching { wm.removeViewImmediate(it) } }
    overlay = null
    selectorOverlayId = null
    selectionRefresh = null
    selectionDirty = false
    toolbarSelectButton = null
  }
  companion object {
    private val AD_BADGES = setOf("广告", "推广", "赞助", "广告推广", "sponsored", "advertisement")
    private val WHITESPACE = Regex("\\s+")
    private val UNSAFE_LABEL = Regex("购买|支付|下载|安装|领取|同意|允许|授权|登录|buy|purchase|install|download|accept|allow", RegexOption.IGNORE_CASE)
    private val CLOSE_RESOURCE = Regex("(?:^|[_/])(close|skip|dismiss)(?:$|[_])", RegexOption.IGNORE_CASE)
    private const val HEX = "0123456789abcdef"
    private val EMPTY_LABEL_HASH = digest("")
    private fun digest(text: String): String {
      val bytes = MessageDigest.getInstance("SHA-256").digest(text.toByteArray())
      val output = CharArray(bytes.size * 2)
      bytes.forEachIndexed { index, value ->
        val unsigned = value.toInt() and 255
        output[index * 2] = HEX[unsigned ushr 4]
        output[index * 2 + 1] = HEX[unsigned and 15]
      }
      return String(output)
    }
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

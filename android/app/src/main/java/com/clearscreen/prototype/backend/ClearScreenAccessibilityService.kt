package com.clearscreen.prototype.backend

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.content.ComponentName
import android.content.Context
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.clearscreen.prototype.BuildConfig
import java.util.ArrayDeque
import java.util.LinkedHashMap
import java.util.Locale

private fun Rect.toAdBox(): AdGeometryRules.Box =
  AdGeometryRules.Box(left, top, right, bottom)

/**
 * Accessibility-based ad action detector.
 *
 * The detector deliberately uses a layered approach: explicit action labels and
 * resource ids first, then the surrounding screen/ancestor context, and finally
 * the target geometry. This borrows the useful ideas behind open-source helpers
 * without copying a third-party rules engine or its licensed rule data.
 */
class ClearScreenAccessibilityService : AccessibilityService() {
  private lateinit var store: ClearScreenStore
  private val handler = Handler(Looper.getMainLooper())
  private val recentActions = LinkedHashMap<String, Long>()
  private val recentAttempts = LinkedHashMap<String, ArrayDeque<Long>>()
  private var lastActionAt = 0L
  private var lastScanAt = 0L
  private var lastWindowProbeAt = 0L
  private var lastWindowProbePackage: String? = null
  private var actionInFlight = false
  private val pendingProbes = mutableListOf<Runnable>()

  override fun onServiceConnected() {
    super.onServiceConnected()
    store = ClearScreenStore(this)
    running = true
  }

  override fun onAccessibilityEvent(event: AccessibilityEvent?) {
    if (event == null || !::store.isInitialized || !store.isMasterEnabled()) return
    if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
      event.eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
      event.eventType != AccessibilityEvent.TYPE_WINDOWS_CHANGED
    ) return
    val packageName = event.packageName?.toString()?.takeIf { it.isNotBlank() }
      ?: rootInActiveWindow?.packageName?.toString()?.takeIf { it.isNotBlank() }
      ?: return
    if (packageName == applicationContext.packageName || isProtectedPackage(packageName)) return

    val rule = store.getAppRule(packageName)
    if (!rule.skipEnabled || rule.whitelist) return

    val now = System.currentTimeMillis()
    // Many splash ads appear one or two frames AFTER the window state event. Do
    // bounded follow-up scans instead of assuming the first node tree is final.
    if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
      (lastWindowProbePackage != packageName || now - lastWindowProbeAt > WINDOW_PROBE_RESET_MS)
    ) {
      lastWindowProbePackage = packageName
      lastWindowProbeAt = now
      scheduleWindowProbes(packageName)
    }

    // A content-changing WebView may emit hundreds of events per second.
    if (now - lastScanAt < MIN_SCAN_INTERVAL_MS && event.eventType ==
      AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
    ) return
    scanForAd(packageName)
  }

  private fun scheduleWindowProbes(packageName: String) {
    pendingProbes.forEach(handler::removeCallbacks)
    pendingProbes.clear()
    for (delay in WINDOW_PROBE_DELAYS_MS) {
      val task = Runnable {
        // Android can move to a different app before a delayed scan fires.
        // findRootForPackage() below checks the package again.
        scanForAd(packageName)
      }
      pendingProbes += task
      handler.postDelayed(task, delay)
    }
  }

  private fun scanForAd(packageName: String) {
    if (!::store.isInitialized || !store.isMasterEnabled() || actionInFlight) return
    if (packageName == applicationContext.packageName || isProtectedPackage(packageName)) return
    val rule = store.getAppRule(packageName)
    if (!rule.skipEnabled || rule.whitelist) return

    val now = System.currentTimeMillis()
    if (now - lastActionAt < ACTION_COOLDOWN_MS) return
    lastScanAt = now
    val root = findRootForPackage(packageName) ?: return
    val action = findAdAction(root) ?: return
    val fingerprint = action.targetFingerprint(packageName)
    if (wasRecentlyAttempted(fingerprint, now)) return
    if (!tryReserveAttempt(fingerprint, now)) return

    actionInFlight = true
    lastActionAt = now
    // An OEM may accept dispatchGesture() without returning a completion
    // callback. Release this guard eventually so detection never stalls.
    handler.postDelayed({
      if (actionInFlight && lastActionAt == now) {
        actionInFlight = false
        if (BuildConfig.DEBUG) Log.w(TAG, "Ad action callback timed out: $packageName")
      }
    }, ACTION_IN_FLIGHT_TIMEOUT_MS)
    val clicked = runCatching {
      action.target.performAction(AccessibilityNodeInfo.ACTION_CLICK) ||
        (action.allowDismiss && action.target.performAction(AccessibilityNodeInfo.ACTION_DISMISS))
    }.getOrDefault(false)

    if (clicked) {
      finalizeAction(packageName, action, fingerprint, accepted = true, viaGesture = false)
      return
    }

    // Some ad SDKs expose their skip text/icon as an accessibility node but
    // mark neither it nor any ancestor clickable. A tightly bounded tap is
    // permitted ONLY for an explicit ad action with strong geometry/context.
    if (action.allowGestureFallback) {
      performBoundedTap(packageName, action, fingerprint)
    } else {
      finalizeAction(packageName, action, fingerprint, accepted = false, viaGesture = false)
    }
  }

  private fun performBoundedTap(
    packageName: String,
    action: DetectionAction,
    fingerprint: String,
  ) {
    val x = action.targetBounds.exactCenterX()
    val y = action.targetBounds.exactCenterY()
    val path = Path().apply { moveTo(x, y) }
    val gesture = GestureDescription.Builder()
      .addStroke(GestureDescription.StrokeDescription(path, 0, 56L))
      .build()
    val callback = object : AccessibilityService.GestureResultCallback() {
      override fun onCompleted(gestureDescription: GestureDescription?) {
        finalizeAction(packageName, action, fingerprint, accepted = true, viaGesture = true)
      }
      override fun onCancelled(gestureDescription: GestureDescription?) {
        finalizeAction(packageName, action, fingerprint, accepted = false, viaGesture = true)
      }
    }
    val dispatched = runCatching { dispatchGesture(gesture, callback, handler) }.getOrDefault(false)
    if (!dispatched) {
      finalizeAction(packageName, action, fingerprint, accepted = false, viaGesture = true)
    }
  }

  private fun finalizeAction(
    packageName: String,
    action: DetectionAction,
    fingerprint: String,
    accepted: Boolean,
    viaGesture: Boolean,
  ) {
    if (BuildConfig.DEBUG) {
      Log.d(TAG, "action=${action.ruleId} score=${action.score} package=$packageName " +
        "label=${action.label} accepted=$accepted gesture=$viaGesture")
    }
    if (!accepted) {
      actionInFlight = false
      appendActionEvent(packageName, action, "failed", "处理失败")
      return
    }

    rememberAction(fingerprint, System.currentTimeMillis())
    handler.postDelayed({
      actionInFlight = false
      if (!::store.isInitialized || !store.isMasterEnabled()) return@postDelayed
      val rule = store.getAppRule(packageName)
      if (!rule.skipEnabled || rule.whitelist) return@postDelayed

      // Do NOT mark success merely because the event's window vanished. This
      // also avoids false success when the user switches to another app.
      val root = findRootForPackage(packageName)
      if (root == null) {
        appendActionEvent(packageName, action, "attempted", "已执行，结果待确认")
        return@postDelayed
      }

      // Countdown digits may change between frames (3跳过 -> 2跳过).
      // Fingerprints intentionally ignore the changing number.
      val stillPresent = findAdAction(root)?.targetFingerprint(packageName) == fingerprint
      if (stillPresent) {
        appendActionEvent(packageName, action, "failed", "动作后仍显示广告控件")
      } else {
        appendActionEvent(packageName, action, "success", action.successText)
      }
    }, ACTION_VERIFY_DELAY_MS)
  }

  override fun onInterrupt() = Unit

  override fun onDestroy() {
    handler.removeCallbacksAndMessages(null)
    pendingProbes.clear()
    actionInFlight = false
    recentActions.clear()
    recentAttempts.clear()
    running = false
    super.onDestroy()
  }

  private fun findAdAction(root: AccessibilityNodeInfo): DetectionAction? {
    val snapshot = AccessibilitySnapshot.capture(root)
    if (snapshot.nodes.isEmpty()) return null

    val candidates = snapshot.nodes.mapNotNull { node ->
      findSkipCandidate(node, snapshot)
    } + snapshot.nodes.mapNotNull { node ->
      findPopupCloseCandidate(node, snapshot)
    }

    return candidates
      .filter { it.score >= it.minimumScore }
      .maxWithOrNull(
        compareBy<DetectionAction> { it.score }
          .thenBy { it.actionPriority }
          .thenBy { -it.targetBounds.top },
      )
  }

  private fun findSkipCandidate(
    node: ScreenNode,
    snapshot: AccessibilitySnapshot,
  ): DetectionAction? {
    if (!node.isEnabled) return null
    val target = preferredActionTarget(node, snapshot.rootBounds)
    if (!target.isVisibleToUser || !target.isEnabled) return null

    val compact = node.compactLabel
    val resourceId = node.resourceId
    // text and contentDescription are often duplicates or contain different
    // strings. Score them independently instead of requiring a joined match.
    val strongLabel = node.labelVariants.any(::isStrongSkipLabel)
    val countdownLabel = node.labelVariants.any(::isCountdownSkipLabel)
    val plainLabel = node.labelVariants.any { it == "跳过" || it == "skip" }
    val explicitResource = hasSkipResourceId(resourceId)
    // An ordinary onboarding or consent dialog may also say "跳过".
    // Never infer an advertisement from a sensitive ancestor context.
    if (node.ancestorLabels.any(AdSemanticRules::isSensitiveUiContext)) return null
    if (!strongLabel && !countdownLabel && !plainLabel && !explicitResource) return null

    val targetBounds = Rect().also(target::getBoundsInScreen)
    if (!hasUsableBounds(targetBounds)) return null
    val nearTopRight = isNearTopRight(targetBounds, snapshot.rootBounds)
    val compactControl = isCompactControl(targetBounds, snapshot.rootBounds)
    if (!compactControl) return null
    val ancestorHasAdSignal = node.ancestorLabels.any(::hasStrongAdSignal)

    var score = when {
      countdownLabel -> 116
      strongLabel -> 112
      explicitResource -> 96
      else -> 82
    }
    var ruleId = when {
      countdownLabel -> "builtin.skip.countdown"
      strongLabel -> "builtin.skip.strong-label"
      explicitResource -> "builtin.skip.resource-id"
      else -> "builtin.skip.short-label"
    }

    if (explicitResource) score += 28
    if (nearTopRight) score += 20
    if (compactControl) score += 8
    if (ancestorHasAdSignal) score += 18
    if (snapshot.context.hasAdSignal) score += 24
    if (snapshot.context.hasStrongAdSignal) score += 12

    // A bare "跳过/Skip" is intentionally accepted only when its position or
    // surrounding screen makes it look like an ad action. This prevents ordinary
    // tutorial/settings controls from being clicked just because they contain a
    // common verb.
    if (plainLabel && !countdownLabel && !explicitResource &&
      !nearTopRight && !ancestorHasAdSignal
    ) return null
    if (plainLabel && !compactControl && !snapshot.context.hasAdSignal && !explicitResource) return null
    if ((strongLabel || countdownLabel) &&
      !nearTopRight && !ancestorHasAdSignal && !snapshot.context.hasStrongAdSignal
    ) return null
    // Resource names such as skip_button also occur in normal onboarding UIs.
    // A resource-id-only match needs both ad context AND a small corner control.
    if (explicitResource && !strongLabel && !countdownLabel && !plainLabel &&
      !(nearTopRight && compactControl && snapshot.context.hasAdSignal)
    ) return null

    if (snapshot.context.hasAdSignal || ancestorHasAdSignal) {
      ruleId += ".ad-context"
    }
    return DetectionAction(
      target = target,
      targetBounds = targetBounds,
      score = score,
      minimumScore = if (plainLabel) 102 else 96,
      actionPriority = 2,
      allowDismiss = false,
      allowGestureFallback = nearTopRight && isTightTapTarget(targetBounds, snapshot.rootBounds) &&
        (countdownLabel || strongLabel || (explicitResource && snapshot.context.hasStrongAdSignal)),
      ruleId = ruleId,
      label = node.label.take(MAX_LOG_LABEL_LENGTH),
      successText = "已跳过开屏广告",
    )
  }

  private fun findPopupCloseCandidate(
    node: ScreenNode,
    snapshot: AccessibilitySnapshot,
  ): DetectionAction? {
    if (!node.isEnabled) return null
    val target = preferredActionTarget(node, snapshot.rootBounds)
    if (!target.isVisibleToUser || !target.isEnabled) return null

    val compact = node.compactLabel
    val resourceId = node.resourceId
    val closeLabel = node.labelVariants.any(::isPopupCloseLabel)
    val explicitAdResource = hasAdCloseResourceId(resourceId)
    if (node.ancestorLabels.any(AdSemanticRules::isSensitiveUiContext)) return null
    if (!closeLabel && !explicitAdResource) return null

    val targetBounds = Rect().also(target::getBoundsInScreen)
    if (!hasUsableBounds(targetBounds)) return null
    val nearTopRight = isNearTopRight(targetBounds, snapshot.rootBounds)
    val compactControl = isCompactControl(targetBounds, snapshot.rootBounds)
    val ancestorHasAdSignal = node.ancestorLabels.any(::hasStrongAdSignal)
    val hasAdContext = snapshot.context.hasAdSignal || ancestorHasAdSignal
    val explicitCloseLabel = node.labelVariants.any(AdSemanticRules::isExplicitAdClose)
    // An unrelated background banner may contain "广告", but this must not
    // authorize clicking every generic X on a normal page.
    val nearAdModalCorner = snapshot.hasAdModalPanel(targetBounds)
    val localAdContext = ancestorHasAdSignal ||
      snapshot.hasNearbyAdSignal(targetBounds) ||
      nearAdModalCorner
    if (!explicitCloseLabel && !explicitAdResource && !localAdContext) return null

    // A generic close icon is too dangerous to click without ad context. A
    // resource id explicitly mentioning an interstitial/splash/ad is stronger,
    // but it still must be a small control near the edge of the screen.
    if ((!nearTopRight && !nearAdModalCorner) || !compactControl) return null
    if (!hasAdContext && !explicitAdResource && !explicitCloseLabel) return null

    var score = when {
      explicitAdResource -> 92
      compact == "关闭广告" || compact == "关闭弹窗" -> 88
      compact == "关闭" || compact == "close" -> 76
      else -> 62
    }
    if (explicitAdResource) score += 30
    if (nearTopRight || nearAdModalCorner) score += 18
    if (compactControl) score += 8
    if (hasAdContext) score += 26

    return DetectionAction(
      target = target,
      targetBounds = targetBounds,
      score = score,
      minimumScore = 106,
      actionPriority = 1,
      allowDismiss = true,
      allowGestureFallback = (
        isTightTapTarget(targetBounds, snapshot.rootBounds) ||
          (nearAdModalCorner && isSmallModalClose(targetBounds, snapshot.rootBounds))
        ) && (explicitAdResource || explicitCloseLabel || localAdContext),
      ruleId = if (explicitAdResource) "builtin.popup.close.resource-id" else "builtin.popup.close.context",
      label = node.label.take(MAX_LOG_LABEL_LENGTH),
      successText = "已关闭弹窗广告",
    )
  }

  private fun findRootForPackage(packageName: String): AccessibilityNodeInfo? {
    val active = rootInActiveWindow
    if (active != null) {
      val activePackage = active.packageName?.toString()
      if (activePackage.isNullOrBlank() || activePackage == packageName) return active
      // Never tap an old app's window while another app or a permission dialog
      // is in the foreground. Delayed probe callbacks must obey this too.
      return null
    }
    return runCatching {
      windows.asSequence()
        .filter { it.isActive }
        .mapNotNull { window -> runCatching { window.root }.getOrNull() }
        .firstOrNull { root -> root.packageName?.toString() == packageName }
    }.getOrNull()
  }

  /**
   * Some SDKs make the entire splash layout clickable. Never promote a tiny
   * skip label into a full-screen ACTION_CLICK target; prefer the tiny leaf and
   * apply the guarded gesture fallback instead.
   */
  private fun preferredActionTarget(
    node: ScreenNode,
    rootBounds: Rect,
  ): AccessibilityNodeInfo {
    val clickable = clickableTarget(node.node) ?: return node.node
    val box = Rect().also(clickable::getBoundsInScreen)
    return if (isCompactControl(box, rootBounds)) clickable else node.node
  }

  private fun clickableTarget(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
    var current: AccessibilityNodeInfo? = node
    repeat(MAX_CLICKABLE_ANCESTORS) {
      if (current?.isVisibleToUser == true && current.isEnabled && current.isClickable) {
        return current
      }
      current = current?.parent
    }
    return null
  }

  private fun tryReserveAttempt(key: String, now: Long): Boolean {
    val history = recentAttempts.getOrPut(key) { ArrayDeque() }
    while (history.isNotEmpty() && now - history.first() > ATTEMPT_BUDGET_WINDOW_MS) {
      history.removeFirst()
    }
    if (history.size >= MAX_ATTEMPTS_PER_TARGET) return false
    history.addLast(now)
    while (recentAttempts.size > MAX_REMEMBERED_ACTIONS) {
      recentAttempts.remove(recentAttempts.keys.first())
    }
    return true
  }

  private fun isProtectedPackage(packageName: String): Boolean =
    packageName == "com.android.settings" ||
      packageName == "com.android.systemui" ||
      packageName == "com.google.android.permissioncontroller" ||
      packageName == "com.android.permissioncontroller" ||
      packageName == "com.vivo.permissionmanager"

  private fun wasRecentlyAttempted(key: String, now: Long): Boolean =
    recentActions[key]?.let { now - it < ACTION_REPEAT_GUARD_MS } == true

  private fun rememberAction(key: String, now: Long) {
    recentActions[key] = now
    while (recentActions.size > MAX_REMEMBERED_ACTIONS) {
      recentActions.remove(recentActions.entries.first().key)
    }
  }

  private fun appendActionEvent(
    packageName: String,
    action: DetectionAction,
    result: String,
    text: String,
  ) {
    store.appendEvent(
      type = "skip",
      packageName = packageName,
      ruleId = action.ruleId,
      result = result,
      text = text,
    )
  }

  private data class PendingNode(
    val node: AccessibilityNodeInfo,
    val depth: Int,
    val ancestorLabels: List<String>,
  )

  private data class AccessibilitySnapshot(
    val nodes: List<ScreenNode>,
    val rootBounds: Rect,
    val context: AdContext,
  ) {
    fun hasNearbyAdSignal(bounds: Rect): Boolean {
      val width = rootBounds.width().coerceAtLeast(1)
      val height = rootBounds.height().coerceAtLeast(1)
      return nodes.any { node ->
        node.isVisible &&
          hasStrongAdSignal(node.label, node.resourceId) &&
          kotlin.math.abs(node.bounds.centerX() - bounds.centerX()) < width * 0.34f &&
          kotlin.math.abs(node.bounds.centerY() - bounds.centerY()) < height * 0.22f
      }
    }

    /**
     * SDK dialogs often put the "广告" badge at the BOTTOM of a modal and the
     * X icon at its upper-right corner. Their nodes are too far apart for a
     * simple nearest-neighbour check. Accept the badge as evidence only when
     * both belong to the same bounded, centered panel; never use an ad label
     * from an unrelated background card as authorization to click a random X.
     */
    fun hasAdModalPanel(close: Rect): Boolean {
      val screen = rootBounds.toAdBox()
      return nodes.any { panel ->
        val box = panel.bounds
        panel.isVisible &&
          AdGeometryRules.isModalCorner(close.toAdBox(), box.toAdBox(), screen) &&
          nodes.any { signal ->
            signal.isVisible && signal !== panel &&
              hasStrongAdSignal(signal.label, signal.resourceId) &&
              box.contains(signal.bounds.centerX(), signal.bounds.centerY())
          }
      }
    }

    companion object {
      fun capture(root: AccessibilityNodeInfo): AccessibilitySnapshot {
        val nodes = mutableListOf<ScreenNode>()
        val pending = ArrayDeque<PendingNode>()
        pending.add(PendingNode(root, 0, emptyList()))
        val rootBounds = Rect().also(root::getBoundsInScreen)

        while (pending.isNotEmpty() && nodes.size < MAX_NODES_PER_SNAPSHOT) {
          val current = pending.removeFirst()
          val node = current.node
          val variants = readNodeLabelVariants(node)
          val label = variants.joinToString(" ")
          val resourceId = node.viewIdResourceName.orEmpty().lowercase(Locale.ROOT)
          val bounds = Rect().also(node::getBoundsInScreen)
          val visible = node.isVisibleToUser && hasUsableBounds(bounds)
          nodes += ScreenNode(
            node = node,
            label = label,
            compactLabel = normalizeLabel(label),
            labelVariants = variants.map(::normalizeLabel),
            resourceId = resourceId,
            ancestorLabels = current.ancestorLabels,
            bounds = bounds,
            isVisible = visible,
            isEnabled = node.isEnabled,
            depth = current.depth,
          )

          val nextAncestors = if (label.isBlank()) {
            current.ancestorLabels
          } else {
            (current.ancestorLabels + label).takeLast(MAX_ANCESTOR_LABELS)
          }
          if (current.depth >= MAX_TREE_DEPTH) continue
          for (index in 0 until node.childCount) {
            node.getChild(index)?.let { child ->
              pending.add(PendingNode(child, current.depth + 1, nextAncestors))
            }
          }
        }

        return AccessibilitySnapshot(
          nodes = nodes,
          rootBounds = rootBounds,
          context = AdContext.from(nodes),
        )
      }
    }
  }

  private data class ScreenNode(
    val node: AccessibilityNodeInfo,
    val label: String,
    val compactLabel: String,
    val labelVariants: List<String>,
    val resourceId: String,
    val ancestorLabels: List<String>,
    val bounds: Rect,
    val isVisible: Boolean,
    val isEnabled: Boolean,
    val depth: Int,
  )

  private data class AdContext(
    val strongSignals: Int,
    val mediumSignals: Int,
  ) {
    val hasStrongAdSignal: Boolean get() = strongSignals > 0
    val hasAdSignal: Boolean get() = hasStrongAdSignal || mediumSignals >= 2

    companion object {
      fun from(nodes: List<ScreenNode>): AdContext {
        var strong = 0
        var medium = 0
        nodes.forEach { node ->
          if (!node.isVisible) return@forEach
          if (hasStrongAdSignal(node.label, node.resourceId)) strong++
          if (hasMediumAdSignal(node.compactLabel)) medium++
        }
        return AdContext(
          strongSignals = strong.coerceAtMost(3),
          mediumSignals = medium.coerceAtMost(4),
        )
      }
    }
  }

  private data class DetectionAction(
    val target: AccessibilityNodeInfo,
    val targetBounds: Rect,
    val score: Int,
    val minimumScore: Int,
    val actionPriority: Int,
    val allowDismiss: Boolean,
    val allowGestureFallback: Boolean,
    val ruleId: String,
    val label: String,
    val successText: String,
  ) {
    fun targetFingerprint(packageName: String): String = buildString {
      append(packageName)
      append('|')
      // Ignore the changing countdown number and small layout jitter.
      append(if (ruleId.startsWith("builtin.skip.countdown")) "countdown" else label)
      append('|')
      append(targetBounds.left / FINGERPRINT_BUCKET_PX)
      append(',')
      append(targetBounds.top / FINGERPRINT_BUCKET_PX)
      append(',')
      append(targetBounds.right / FINGERPRINT_BUCKET_PX)
      append(',')
      append(targetBounds.bottom / FINGERPRINT_BUCKET_PX)
    }
  }

  companion object {
    @Volatile
    var running: Boolean = false

    private const val TAG = "ClearScreenA11y"
    private const val ACTION_COOLDOWN_MS = 480L
    private const val ACTION_VERIFY_DELAY_MS = 700L
    private const val ACTION_IN_FLIGHT_TIMEOUT_MS = 3_000L
    private const val MIN_SCAN_INTERVAL_MS = 100L
    private const val WINDOW_PROBE_RESET_MS = 2_500L
    private val WINDOW_PROBE_DELAYS_MS = longArrayOf(220L, 600L, 1_200L)
    private const val FINGERPRINT_BUCKET_PX = 20
    private const val ACTION_REPEAT_GUARD_MS = 1_500L
    private const val ATTEMPT_BUDGET_WINDOW_MS = 8_000L
    private const val MAX_ATTEMPTS_PER_TARGET = 2
    private const val MAX_CLICKABLE_ANCESTORS = 6
    private const val MAX_NODES_PER_SNAPSHOT = 1_200
    private const val MAX_TREE_DEPTH = 32
    private const val MAX_ANCESTOR_LABELS = 8
    private const val MAX_REMEMBERED_ACTIONS = 80
    private const val MAX_LOG_LABEL_LENGTH = 80

    fun isEnabled(context: Context): Boolean {
      val enabled = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
      ) ?: return false
      val component = ComponentName(context, ClearScreenAccessibilityService::class.java)
        .flattenToString()
      return enabled.split(':').any { it.equals(component, ignoreCase = true) }
    }

    private fun readNodeLabelVariants(node: AccessibilityNodeInfo): List<String> =
      listOfNotNull(
        node.text?.toString()?.trim()?.takeIf { it.isNotBlank() },
        node.contentDescription?.toString()?.trim()?.takeIf { it.isNotBlank() },
      ).distinct()

    private fun normalizeLabel(value: String): String = AdSemanticRules.normalize(value)

    private fun isStrongSkipLabel(compact: String): Boolean =
      AdSemanticRules.isExplicitSkip(compact)

    private fun isCountdownSkipLabel(compact: String): Boolean =
      AdSemanticRules.isCountdownSkip(compact)

    private fun hasSkipResourceId(resourceId: String): Boolean =
      AdSemanticRules.isSkipResource(resourceId)

    private fun hasAdCloseResourceId(resourceId: String): Boolean =
      AdSemanticRules.isAdCloseResource(resourceId)

    private fun isPopupCloseLabel(compact: String): Boolean =
      AdSemanticRules.isClose(compact)

    private fun hasStrongAdSignal(label: String): Boolean =
      AdSemanticRules.hasStrongAdSignal(label)

    private fun hasStrongAdSignal(label: String, resourceId: String): Boolean =
      AdSemanticRules.hasStrongAdSignal(label, resourceId)

    private fun hasMediumAdSignal(compact: String): Boolean =
      AdSemanticRules.hasMediumAdSignal(compact)

    private fun hasUsableBounds(bounds: Rect): Boolean =
      bounds.width() > 0 && bounds.height() > 0

    private fun isNearTopRight(bounds: Rect, rootBounds: Rect): Boolean =
      AdGeometryRules.isNearTopRight(bounds.toAdBox(), rootBounds.toAdBox())

    private fun isTightTapTarget(bounds: Rect, rootBounds: Rect): Boolean =
      AdGeometryRules.isTightTapTarget(bounds.toAdBox(), rootBounds.toAdBox())

    private fun isSmallModalClose(bounds: Rect, rootBounds: Rect): Boolean =
      AdGeometryRules.isSmallModalClose(bounds.toAdBox(), rootBounds.toAdBox())

    private fun isCompactControl(bounds: Rect, rootBounds: Rect): Boolean =
      AdGeometryRules.isCompactControl(bounds.toAdBox(), rootBounds.toAdBox())

  }
}

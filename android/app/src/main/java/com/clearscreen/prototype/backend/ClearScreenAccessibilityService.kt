package com.clearscreen.prototype.backend

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.clearscreen.prototype.BuildConfig
import java.util.ArrayDeque
import java.util.LinkedHashMap
import java.util.Locale

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
  private val scheduledAdRetries = HashSet<String>()
  private var lastActionAt = 0L

  override fun onServiceConnected() {
    super.onServiceConnected()
    store = ClearScreenStore(this)
    running = true
    Log.i(
      TAG,
      "onServiceConnected component=${ComponentName(this, ClearScreenAccessibilityService::class.java).flattenToString()} " +
        "pid=${Process.myPid()}",
    )
  }

  override fun onAccessibilityEvent(event: AccessibilityEvent?) {
    if (event == null || !::store.isInitialized || !store.isMasterEnabled()) return
    val packageName = event.packageName?.toString()?.takeIf { it.isNotBlank() } ?: return

    // Never inspect or click our own UI. The app intentionally contains labels such as
    // "今日自动跳过" and "自动跳过权限"; treating those as ad controls would navigate
    // the user around the app while they are configuring it.
    if (packageName == applicationContext.packageName) return

    val rule = store.getAppRule(packageName)
    if (!rule.skipEnabled || rule.whitelist) return

    val now = System.currentTimeMillis()
    if (now - lastActionAt < ACTION_COOLDOWN_MS) return

    val handled = tryHandlePackage(packageName)
    if (handled) {
      scheduledAdRetries.remove(packageName)
    } else if (isDynamicAdEvent(event.eventType)) {
      scheduleDynamicAdRetries(packageName)
    }
  }

  override fun onInterrupt() = Unit

  override fun onUnbind(intent: Intent?): Boolean {
    running = false
    Log.w(TAG, "onUnbind pid=${Process.myPid()}")
    return super.onUnbind(intent)
  }

  override fun onDestroy() {
    Log.w(TAG, "onDestroy pid=${Process.myPid()}")
    handler.removeCallbacksAndMessages(null)
    recentActions.clear()
    scheduledAdRetries.clear()
    running = false
    super.onDestroy()
  }

  private fun tryHandlePackage(packageName: String): Boolean {
    if (!::store.isInitialized || !store.isMasterEnabled()) return false
    val rule = store.getAppRule(packageName)
    if (!rule.skipEnabled || rule.whitelist) return false

    val now = System.currentTimeMillis()
    if (now - lastActionAt < ACTION_COOLDOWN_MS) return false
    val root = findRootForPackage(packageName) ?: return false
    val action = findAdAction(root) ?: return false
    val fingerprint = action.targetFingerprint(packageName)
    if (wasRecentlyAttempted(fingerprint, now)) return false

    lastActionAt = now
    // Only click a deliberately identified control. ACTION_DISMISS is exposed by
    // many ordinary dialogs and would close user-required popups even when no ad
    // close button was found.
    val clicked = action.target.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    if (clicked) rememberAction(fingerprint, now)

    if (BuildConfig.DEBUG) {
      Log.d(
        TAG,
        "action=${action.ruleId} score=${action.score} package=$packageName " +
          "label=${action.label} clicked=$clicked",
      )
    }

    if (!clicked) {
      appendActionEvent(packageName, action, "failed", "处理失败")
      return false
    }

    // performAction(true) only means that Android accepted the request. Verify
    // the same target shortly afterwards so a stale/non-operational node does
    // not inflate the success counter.
    handler.postDelayed({
      if (!::store.isInitialized || !store.isMasterEnabled()) return@postDelayed
      val currentRoot = findRootForPackage(packageName)
      val currentAction = currentRoot?.let(::findAdAction)
      val stillPresent = currentAction?.targetFingerprint(packageName) == fingerprint
      if (stillPresent) {
        appendActionEvent(packageName, action, "failed", "动作未生效")
      } else {
        appendActionEvent(packageName, action, "success", action.successText)
      }
    }, ACTION_VERIFY_DELAY_MS)
    return true
  }

  private fun scheduleDynamicAdRetries(packageName: String) {
    if (!scheduledAdRetries.add(packageName)) return
    handler.postDelayed(
      { runDynamicAdRetry(packageName, 0) },
      DYNAMIC_AD_RETRY_DELAYS_MS.first(),
    )
  }

  private fun runDynamicAdRetry(packageName: String, attempt: Int) {
    if (!scheduledAdRetries.contains(packageName)) return
    if (tryHandlePackage(packageName) || attempt >= DYNAMIC_AD_RETRY_DELAYS_MS.lastIndex) {
      scheduledAdRetries.remove(packageName)
      return
    }

    val nextAttempt = attempt + 1
    val delay = DYNAMIC_AD_RETRY_DELAYS_MS[nextAttempt] - DYNAMIC_AD_RETRY_DELAYS_MS[attempt]
    handler.postDelayed(
      { runDynamicAdRetry(packageName, nextAttempt) },
      delay,
    )
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
          .thenByDescending { it.actionPriority }
          .thenBy { it.targetBounds.top },
      )
  }

  private fun findSkipCandidate(
    node: ScreenNode,
    snapshot: AccessibilitySnapshot,
  ): DetectionAction? {
    if (!node.isEnabled) return null
    val target = clickableTarget(node.node) ?: return null
    if (!target.isVisibleToUser || !target.isEnabled) return null

    val compact = node.compactLabel
    val resourceId = node.resourceId
    val strongLabel = isStrongSkipLabel(compact)
    val countdownLabel = isCountdownSkipLabel(compact)
    val plainLabel = compact == "跳过" || compact == "skip"
    val explicitResource = hasSkipResourceId(resourceId)
    if (!strongLabel && !countdownLabel && !plainLabel && !explicitResource) return null

    val labelBounds = node.bounds
    if (!hasUsableBounds(labelBounds)) return null
    val targetBounds = Rect().also(target::getBoundsInScreen)
    if (!hasUsableBounds(targetBounds)) return null
    val labelNearTopRight = isNearTopRight(labelBounds, snapshot.rootBounds)
    val targetNearTopRight = isNearTopRight(targetBounds, snapshot.rootBounds)
    val nearTopRight = labelNearTopRight || targetNearTopRight
    val labelCompactControl = isCompactControl(labelBounds, snapshot.rootBounds)
    val targetCompactControl = isCompactControl(targetBounds, snapshot.rootBounds)
    val compactControl = labelCompactControl || targetCompactControl
    val ancestorHasAdSignal = node.ancestorLabels.any(::hasStrongAdSignal)
    val localStrongAdContext = hasStrongAdSignal(node.label, resourceId) || ancestorHasAdSignal
    val videoAdContext = node.ancestorLabels.any(::hasVideoAdSignal) ||
      hasVideoAdSignal(node.label) || snapshot.context.hasVideoAdSignal
    val edgeSkip = plainLabel && labelNearTopRight && labelCompactControl
    // A resource id is the weakest signal on its own: ordinary navigation
    // containers are commonly named "xxx_jump", "skip_*" or "countdown_*".
    // Treat the id as a supporting marker that still needs geometry or ad context.
    val resourceOnly = explicitResource && !strongLabel && !countdownLabel && !plainLabel

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
    if (localStrongAdContext) score += 24
    if (videoAdContext) score += 18

    // A bare "跳过/Skip" is intentionally accepted only when its position or
    // surrounding screen makes it look like an ad action. This prevents ordinary
    // tutorial/settings controls from being clicked just because they contain a
    // common verb.
    if (plainLabel && !countdownLabel && !explicitResource &&
      !labelNearTopRight && !ancestorHasAdSignal
    ) return null
    if (plainLabel && !edgeSkip && !videoAdContext &&
      !ancestorHasAdSignal && !explicitResource
    ) return null
    // The same protection a bare "跳过" gets must apply when only the resource id
    // matched, otherwise any enabled view named "*_jump" inside a clickable
    // ancestor would be clicked on ordinary screens such as a ride-hailing order.
    if (resourceOnly &&
      !(nearTopRight && compactControl) &&
      !ancestorHasAdSignal && !localStrongAdContext && !videoAdContext
    ) return null

    if (localStrongAdContext || videoAdContext) {
      ruleId += ".ad-context"
    }
    if (edgeSkip && videoAdContext) {
      ruleId = "builtin.skip.video-ad.edge"
    }
    return DetectionAction(
      target = target,
      targetBounds = targetBounds,
      score = score,
      minimumScore = if (plainLabel) 102 else 96,
      actionPriority = 2,
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
    val target = clickableTarget(node.node) ?: return null
    if (!target.isVisibleToUser || !target.isEnabled) return null

    val compact = node.compactLabel
    val resourceId = node.resourceId
    val closeLabel = isPopupCloseLabel(compact)
    val targetResourceId = target.viewIdResourceName.orEmpty().lowercase(Locale.ROOT)
    val explicitAdResource = hasAdCloseResourceId(resourceId) ||
      hasAdCloseResourceId(targetResourceId)

    val targetBounds = Rect().also(target::getBoundsInScreen)
    if (!hasUsableBounds(targetBounds)) return null
    val nearTopRight = isNearTopRight(targetBounds, snapshot.rootBounds)
    val nearPopupEdge = isNearPopupEdge(targetBounds, snapshot.rootBounds)
    val compactControl = isCompactControl(targetBounds, snapshot.rootBounds)
    val localStrongAdContext = explicitAdResource ||
      hasStrongAdSignal(node.label, resourceId) ||
      node.ancestorLabels.any(::hasStrongAdSignal)
    val fullScreenAdContext = snapshot.context.hasFullScreenAdEvidence
    val unlabeledEdgeClose = isLikelyUnlabeledEdgeClose(
      node = node,
      target = target,
      targetResourceId = targetResourceId,
      nearTopRight = nearTopRight,
      compactControl = compactControl,
    )
    if (!closeLabel && !explicitAdResource && !unlabeledEdgeClose) return null
    val genericCloseLabel = isGenericPopupCloseLabel(compact)

    // Do not use the whole screen's commercial/benefit text as evidence. A
    // normal dialog can contain words such as "会员" or "福利" elsewhere.
    // Require an ad marker on this control, its ancestors, or a separately
    // verified full-screen ad context. The latter is deliberately stricter:
    // it needs a large overlay/panel plus a cluster of ad/commercial labels.
    if (!localStrongAdContext && !fullScreenAdContext) return null
    if (!nearPopupEdge || !compactControl) return null

    // Generic "关闭"/X controls are only safe in the conventional ad position.
    // Ad-specific labels such as "关闭广告" may also appear on the popup edge.
    if (genericCloseLabel && !nearTopRight && !explicitAdResource) return null
    if (unlabeledEdgeClose && !nearTopRight) return null

    var score = when {
      explicitAdResource -> 124
      isAdSpecificCloseLabel(compact) -> 114
      fullScreenAdContext -> 108
      else -> 100
    }
    if (explicitAdResource) score += 30
    if (nearTopRight) score += 18
    if (nearPopupEdge) score += 18
    if (compactControl) score += 8
    if (localStrongAdContext) score += 26
    if (fullScreenAdContext) score += 24

    return DetectionAction(
      target = target,
      targetBounds = targetBounds,
      score = score,
      minimumScore = if (explicitAdResource) 120 else 112,
      actionPriority = 1,
      ruleId = when {
        explicitAdResource -> "builtin.popup.close.resource-id"
        fullScreenAdContext -> "builtin.popup.close.fullscreen-ad"
        else -> "builtin.popup.close.context"
      },
      label = node.label.ifBlank { node.className }.take(MAX_LOG_LABEL_LENGTH),
      successText = "已关闭弹窗广告",
    )
  }

  private fun findRootForPackage(packageName: String): AccessibilityNodeInfo? {
    val active = rootInActiveWindow
    if (active != null) {
      val activePackage = active.packageName?.toString()
      if (activePackage.isNullOrBlank() || activePackage == packageName) return active
    }
    return runCatching {
      windows.asSequence()
        .mapNotNull { window -> runCatching { window.root }.getOrNull() }
        .firstOrNull { root -> root.packageName?.toString() == packageName }
    }.getOrNull()
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
    companion object {
      fun capture(root: AccessibilityNodeInfo): AccessibilitySnapshot {
        val nodes = mutableListOf<ScreenNode>()
        val pending = ArrayDeque<PendingNode>()
        pending.add(PendingNode(root, 0, emptyList()))
        val rootBounds = Rect().also(root::getBoundsInScreen)

        while (pending.isNotEmpty() && nodes.size < MAX_NODES_PER_SNAPSHOT) {
          val current = pending.removeFirst()
          val node = current.node
          val label = readNodeLabel(node)
          val resourceId = node.viewIdResourceName.orEmpty().lowercase(Locale.ROOT)
          val className = node.className?.toString().orEmpty().lowercase(Locale.ROOT)
          val bounds = Rect().also(node::getBoundsInScreen)
          val visible = node.isVisibleToUser && hasUsableBounds(bounds)
          nodes += ScreenNode(
            node = node,
            label = label,
            compactLabel = normalizeLabel(label),
            resourceId = resourceId,
            className = className,
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
          context = AdContext.from(nodes, rootBounds),
        )
      }
    }
  }

  private data class ScreenNode(
    val node: AccessibilityNodeInfo,
    val label: String,
    val compactLabel: String,
    val resourceId: String,
    val className: String,
    val ancestorLabels: List<String>,
    val bounds: Rect,
    val isVisible: Boolean,
    val isEnabled: Boolean,
    val depth: Int,
  )

  private data class AdContext(
    val strongSignals: Int,
    val mediumSignals: Int,
    val commercialSignals: Int,
    val popupSignals: Int,
    val largeOverlaySignals: Int,
    val videoSignals: Int,
  ) {
    val hasStrongAdSignal: Boolean get() = strongSignals > 0
    val hasCommercialAdSignal: Boolean get() =
      commercialSignals >= 2 || (commercialSignals > 0 && popupSignals > 0)
    val hasAdSignal: Boolean get() =
      hasStrongAdSignal || mediumSignals >= 2 || hasCommercialAdSignal
    val hasFullScreenAdEvidence: Boolean
      get() {
        // Some ad SDKs expose the marketing copy but omit the literal "广告"
        // node. Four independent commercial markers are a safe fallback only
        // when the same screen also has a medium ad action and a large overlay.
        val hasAdTextCluster =
          (strongSignals > 0 && commercialSignals >= 2) || commercialSignals >= 4
        return hasAdTextCluster && mediumSignals > 0 && largeOverlaySignals > 0
      }
    val hasVideoAdSignal: Boolean get() = videoSignals > 0

    companion object {
      fun from(nodes: List<ScreenNode>, rootBounds: Rect): AdContext {
        var strong = 0
        var medium = 0
        var commercial = 0
        var popup = 0
        var largeOverlay = 0
        var video = 0
        val rootArea = rootBounds.width().toLong().coerceAtLeast(1L) *
          rootBounds.height().toLong().coerceAtLeast(1L)
        nodes.forEach { node ->
          if (!node.isVisible) return@forEach
          if (hasStrongAdSignal(node.label, node.resourceId)) strong++
          medium += countMatchingMarkers(node.compactLabel, MEDIUM_AD_MARKERS)
          commercial += countMatchingMarkers(node.compactLabel, COMMERCIAL_AD_MARKERS)
          video += countMatchingMarkers(node.compactLabel, VIDEO_AD_CONTEXT_MARKERS)
          if (hasPopupSignal(node.compactLabel, node.resourceId, node.className)) popup++
          val nodeArea = node.bounds.width().toLong().coerceAtLeast(0L) *
            node.bounds.height().toLong().coerceAtLeast(0L)
          if (node.depth > 0 &&
            nodeArea >= rootArea * LARGE_OVERLAY_MIN_AREA_RATIO &&
            node.bounds.width() >= rootBounds.width() * LARGE_OVERLAY_MIN_WIDTH_RATIO &&
            node.bounds.height() >= rootBounds.height() * LARGE_OVERLAY_MIN_HEIGHT_RATIO
          ) {
            largeOverlay++
          }
        }
        return AdContext(
          strongSignals = strong.coerceAtMost(3),
          mediumSignals = medium.coerceAtMost(8),
          commercialSignals = commercial.coerceAtMost(8),
          popupSignals = popup.coerceAtMost(4),
          largeOverlaySignals = largeOverlay.coerceAtMost(4),
          videoSignals = video.coerceAtMost(6),
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
    val ruleId: String,
    val label: String,
    val successText: String,
  ) {
    fun targetFingerprint(packageName: String): String = buildString {
      append(packageName)
      append('|')
      append(label)
      append('|')
      append(targetBounds.left)
      append(',')
      append(targetBounds.top)
      append(',')
      append(targetBounds.right)
      append(',')
      append(targetBounds.bottom)
    }
  }

  companion object {
    @Volatile
    var running: Boolean = false

    private const val TAG = "ClearScreenA11y"
    private const val ACTION_COOLDOWN_MS = 480L
    private const val ACTION_VERIFY_DELAY_MS = 650L
    private const val ACTION_REPEAT_GUARD_MS = 1_500L
    private const val MAX_CLICKABLE_ANCESTORS = 6
    private const val MAX_NODES_PER_SNAPSHOT = 1_200
    private const val MAX_TREE_DEPTH = 32
    private const val MAX_ANCESTOR_LABELS = 8
    private const val MAX_REMEMBERED_ACTIONS = 80
    private const val MAX_LOG_LABEL_LENGTH = 80
    private const val LARGE_OVERLAY_MIN_AREA_RATIO = 0.24
    private const val LARGE_OVERLAY_MIN_WIDTH_RATIO = 0.58f
    private const val LARGE_OVERLAY_MIN_HEIGHT_RATIO = 0.32f
    private val DYNAMIC_AD_RETRY_DELAYS_MS = longArrayOf(240L, 600L, 1_000L)

    private val COUNTDOWN_SKIP_PATTERN = Regex(
      "^(?:\\d{1,3}(?:秒|s)?跳过(?:广告|视频|此广告)?|跳过(?:广告|视频|此广告)?\\d{1,3}(?:秒|s)?|\\d{1,3}(?:秒|s)?skip(?:ad|thisad|video)?)$",
    )
    private val STRONG_SKIP_LABELS = setOf(
      "跳过广告",
      "跳过此广告",
      "跳过视频",
      "skipad",
      "skipthisad",
      "skipvideo",
    )
    private val POPUP_CLOSE_LABELS = setOf(
      "关闭",
      "关闭广告",
      "关闭弹窗",
      "close",
      "closead",
      "dismiss",
      "×",
      "✕",
      "✖",
      "✗",
      "╳",
      "⨯",
      "x",
    )
    private val STRONG_AD_MARKERS = listOf(
      "广告",
      "广告位",
      "开屏",
      "推广",
      "赞助",
      "sponsored",
      "advertisement",
      "interstitial",
      "splashad",
    )
    private val VIDEO_AD_CONTEXT_MARKERS = listOf(
      "互动广告",
      "视频广告",
      "广告视频",
      "预加载",
      "wifi预加载",
      "腾讯视频",
      "videoad",
      "preload",
    )
    private val MEDIUM_AD_MARKERS = listOf(
      "立即打开",
      "立即下载",
      "点击跳转",
      "详情页面",
      "免费领取",
      "立即办理",
      "办理",
      "福利",
      "优惠",
      "促销",
      "特惠",
      "红包",
      "svip",
    )
    private val COMMERCIAL_AD_MARKERS = listOf(
      "vip",
      "会员",
      "年卡",
      "权益",
      "特权",
      "商城",
      "折扣",
      "促销",
      "特惠",
      "优惠",
      "礼包",
      "开通",
      "充值",
      "流量",
      "云盘",
      "套餐",
      "5g",
      "gb",
      "tb",
      "立即购买",
      "购买",
    )
    private val POPUP_MARKERS = listOf(
      "落地页",
      "第三方app",
      "第三方应用",
      "弹窗",
      "dialog",
      "popup",
      "modal",
      "interstitial",
    )
    private val SKIP_RESOURCE_MARKERS = listOf(
      "skip",
      "jump",
      "countdown",
      "ad_skip",
      "skip_ad",
      "skipad",
    )
    private val AD_CLOSE_RESOURCE_MARKERS = listOf(
      "ad_close",
      "close_ad",
      "closebtn_ad",
      "ad_dialog",
      "adclose",
      "interstitial_close",
      "interstitialclose",
      "splash_ad",
      "splashad",
      "splash_close",
    )
    private val GENERIC_CLOSE_RESOURCE_MARKERS = listOf(
      "close",
      "dismiss",
      "cancel",
      "exit",
      "guanbi",
    )

    fun isEnabled(context: Context): Boolean {
      val enabled = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
      ) ?: return false
      val component = ComponentName(context, ClearScreenAccessibilityService::class.java)
      val fullName = component.flattenToString()
      val shortName = component.flattenToShortString()
      return enabled.split(':').any { rawName ->
        val name = rawName.trim()
        if (name.equals(fullName, ignoreCase = true) ||
          name.equals(shortName, ignoreCase = true)
        ) {
          true
        } else {
          // vivo may persist the short form. Let Android expand it before
          // comparing so the app and system agree on the same service.
          ComponentName.unflattenFromString(name)?.let { savedComponent ->
            savedComponent.packageName.equals(component.packageName, ignoreCase = true) &&
              savedComponent.className.equals(component.className, ignoreCase = true)
          } == true
        }
      }
    }

    private fun readNodeLabel(node: AccessibilityNodeInfo): String =
      listOfNotNull(
        node.text?.toString()?.trim()?.takeIf { it.isNotBlank() },
        node.contentDescription?.toString()?.trim()?.takeIf { it.isNotBlank() },
      ).distinct().joinToString(" ")

    private fun normalizeLabel(value: String): String = value
      .lowercase(Locale.ROOT)
      .replace("\u3000", "")
      .replace("[\\s\\-_:：，,。.!！？?()（）\\[\\]{}<>《》【】'\"`~·|/\\\\]".toRegex(), "")

    private fun isStrongSkipLabel(compact: String): Boolean =
      compact in STRONG_SKIP_LABELS ||
        (compact.startsWith("skipad") && compact.length <= 20) ||
        (compact.startsWith("跳过") && compact.length <= 10 &&
          (compact.contains("广告") || compact.contains("视频")))

    private fun isCountdownSkipLabel(compact: String): Boolean =
      COUNTDOWN_SKIP_PATTERN.matches(compact)

    private fun hasSkipResourceId(resourceId: String): Boolean =
      SKIP_RESOURCE_MARKERS.any(resourceId::contains)

    private fun hasAdCloseResourceId(resourceId: String): Boolean =
      AD_CLOSE_RESOURCE_MARKERS.any(resourceId::contains)

    private fun isPopupCloseLabel(compact: String): Boolean =
      compact in POPUP_CLOSE_LABELS ||
        (compact.startsWith("关闭") && compact.length <= 8) ||
        (compact.startsWith("close") && compact.length <= 12)

    private fun isAdSpecificCloseLabel(compact: String): Boolean =
      compact == "关闭广告" ||
        compact == "closead" ||
        compact.contains("advertisement")

    private fun isGenericPopupCloseLabel(compact: String): Boolean =
      compact == "关闭" ||
        compact == "close" ||
        compact == "dismiss" ||
        compact in setOf("×", "✕", "✖", "✗", "╳", "⨯", "x")

    private fun hasStrongAdSignal(label: String): Boolean =
      hasStrongAdSignal(label, "")

    private fun hasStrongAdSignal(label: String, resourceId: String): Boolean {
      val compact = normalizeLabel(label)
      return STRONG_AD_MARKERS.any(compact::contains) ||
        resourceId.contains("ad_") ||
        resourceId.contains("_ad") ||
        resourceId.contains("adview") ||
        resourceId.contains("interstitial") ||
        resourceId.contains("splash")
    }

    private fun hasVideoAdSignal(label: String): Boolean {
      val compact = normalizeLabel(label)
      return VIDEO_AD_CONTEXT_MARKERS.any(compact::contains)
    }

    private fun isDynamicAdEvent(eventType: Int): Boolean =
      eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
        eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED ||
        eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED ||
        eventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED

    private fun hasMediumAdSignal(compactLabel: String): Boolean =
      MEDIUM_AD_MARKERS.any(compactLabel::contains)

    private fun hasCommercialAdSignal(compactLabel: String): Boolean =
      COMMERCIAL_AD_MARKERS.any(compactLabel::contains)

    private fun countMatchingMarkers(compactLabel: String, markers: List<String>): Int =
      markers.count(compactLabel::contains)

    private fun isLikelyUnlabeledEdgeClose(
      node: ScreenNode,
      target: AccessibilityNodeInfo,
      targetResourceId: String,
      nearTopRight: Boolean,
      compactControl: Boolean,
    ): Boolean {
      if (node.label.isNotBlank() || !nearTopRight || !compactControl) return false
      val className = (node.className + " " + target.className?.toString().orEmpty())
        .lowercase(Locale.ROOT)
      val closeResource = hasGenericCloseResourceId(node.resourceId) ||
        hasGenericCloseResourceId(targetResourceId)
      val iconClass = className.contains("imageview") ||
        className.contains("imagebutton") ||
        className.endsWith("button")
      return closeResource || iconClass
    }

    private fun hasGenericCloseResourceId(resourceId: String): Boolean =
      GENERIC_CLOSE_RESOURCE_MARKERS.any(resourceId::contains)

    private fun hasPopupSignal(
      compactLabel: String,
      resourceId: String,
      className: String,
    ): Boolean =
      POPUP_MARKERS.any(compactLabel::contains) ||
        POPUP_MARKERS.any(resourceId::contains) ||
        POPUP_MARKERS.any(className::contains)

    private fun hasUsableBounds(bounds: Rect): Boolean =
      bounds.width() > 0 && bounds.height() > 0

    private fun isNearTopRight(bounds: Rect, rootBounds: Rect): Boolean {
      val width = rootBounds.width().coerceAtLeast(1)
      val height = rootBounds.height().coerceAtLeast(1)
      return bounds.centerX() >= rootBounds.left + width * 0.55f &&
        bounds.top <= rootBounds.top + height * 0.48f &&
        bounds.right >= rootBounds.left + width * 0.70f
    }

    private fun isNearPopupEdge(bounds: Rect, rootBounds: Rect): Boolean {
      val width = rootBounds.width().coerceAtLeast(1)
      val height = rootBounds.height().coerceAtLeast(1)
      val centerX = bounds.centerX()
      val centerY = bounds.centerY()
      val nearBottomCenter = centerY >= rootBounds.top + height * 0.62f &&
        centerX >= rootBounds.left + width * 0.28f &&
        centerX <= rootBounds.left + width * 0.72f
      val nearSide = (centerX <= rootBounds.left + width * 0.18f ||
        centerX >= rootBounds.left + width * 0.82f) &&
        centerY >= rootBounds.top + height * 0.08f &&
        centerY <= rootBounds.top + height * 0.92f
      return isNearTopRight(bounds, rootBounds) || nearBottomCenter || nearSide
    }

    private fun isCompactControl(bounds: Rect, rootBounds: Rect): Boolean {
      val width = rootBounds.width().coerceAtLeast(1)
      val height = rootBounds.height().coerceAtLeast(1)
      return bounds.width() <= width * 0.36f && bounds.height() <= height * 0.22f
    }
  }
}

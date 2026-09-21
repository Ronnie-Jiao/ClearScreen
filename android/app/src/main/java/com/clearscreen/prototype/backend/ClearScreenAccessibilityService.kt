package com.clearscreen.prototype.backend

import android.accessibilityservice.AccessibilityService
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.clearscreen.prototype.BuildConfig
import com.clearscreen.prototype.MainActivity
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
  private var lastActionAt = 0L

  override fun onServiceConnected() {
    super.onServiceConnected()
    store = ClearScreenStore(this)
    running = true
    startPersistentForegroundNotification()
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

    val root = findRootForPackage(packageName) ?: return
    val action = findAdAction(root) ?: return
    val fingerprint = action.targetFingerprint(packageName)
    if (wasRecentlyAttempted(fingerprint, now)) return

    lastActionAt = now
    val clicked = action.target.performAction(AccessibilityNodeInfo.ACTION_CLICK) ||
      (action.allowDismiss && action.target.performAction(AccessibilityNodeInfo.ACTION_DISMISS))
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
      return
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
  }

  override fun onInterrupt() = Unit

  override fun onDestroy() {
    handler.removeCallbacksAndMessages(null)
    recentActions.clear()
    running = false
    stopPersistentForegroundNotification()
    super.onDestroy()
  }

  /**
   * The system owns the accessibility binding, but an ongoing notification
   * gives Android and the user a clear, visible lifetime for the user-enabled
   * rule service. OEM startup protection is still required on devices that
   * aggressively stop background packages; this is a resilience layer, not a
   * bypass for a user's system setting.
   */
  private fun startPersistentForegroundNotification() {
    try {
      val manager = getSystemService(NotificationManager::class.java)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        manager.createNotificationChannel(
          NotificationChannel(
            FOREGROUND_CHANNEL_ID,
            "净屏后台服务",
            NotificationManager.IMPORTANCE_LOW,
          ).apply {
            description = "净屏在用户开启服务后保持可用"
            setShowBadge(false)
          },
        )
      }
      val openAppIntent = Intent(this, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
      }
      val pendingIntent = PendingIntent.getActivity(
        this,
        0,
        openAppIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )
      val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Notification.Builder(this, FOREGROUND_CHANNEL_ID)
      } else {
        @Suppress("DEPRECATION")
        Notification.Builder(this)
      }
      val notification = builder
        .setSmallIcon(android.R.drawable.ic_menu_info_details)
        .setContentTitle("净屏正在运行")
        .setContentText("自动跳过服务已由你开启")
        .setContentIntent(pendingIntent)
        .setOngoing(true)
        .setShowWhen(false)
        .build()
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        startForeground(
          FOREGROUND_NOTIFICATION_ID,
          notification,
          ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
        )
      } else {
        startForeground(FOREGROUND_NOTIFICATION_ID, notification)
      }
    } catch (error: Exception) {
      // Foreground mode must never crash or self-disable the accessibility
      // service. The native status page will still guide the user to the
      // device's startup protection if an OEM refuses background execution.
      Log.w(TAG, "Unable to promote accessibility service to foreground", error)
    }
  }

  private fun stopPersistentForegroundNotification() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      stopForeground(STOP_FOREGROUND_REMOVE)
    } else {
      @Suppress("DEPRECATION")
      stopForeground(true)
    }
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

    val targetBounds = Rect().also(target::getBoundsInScreen)
    if (!hasUsableBounds(targetBounds)) return null
    val nearTopRight = isNearTopRight(targetBounds, snapshot.rootBounds)
    val compactControl = isCompactControl(targetBounds, snapshot.rootBounds)
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
    if (snapshot.context.hasCommercialAdSignal) score += 12

    // A bare "跳过/Skip" is intentionally accepted only when its position or
    // surrounding screen makes it look like an ad action. This prevents ordinary
    // tutorial/settings controls from being clicked just because they contain a
    // common verb.
    if (plainLabel && !countdownLabel && !explicitResource &&
      !nearTopRight && !ancestorHasAdSignal && !snapshot.context.hasAdSignal
    ) return null
    if (plainLabel && !compactControl &&
      !snapshot.context.hasAdSignal && !explicitResource
    ) return null

    if (snapshot.context.hasAdSignal || ancestorHasAdSignal) {
      ruleId += ".ad-context"
    }
    return DetectionAction(
      target = target,
      targetBounds = targetBounds,
      score = score,
      minimumScore = if (plainLabel && snapshot.context.hasAdSignal) 86 else if (plainLabel) 102 else 96,
      actionPriority = 2,
      allowDismiss = false,
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
    val dismissTarget = node.node.takeIf { target ->
      target.isVisibleToUser && target.actionList.any { action ->
        action.id == AccessibilityNodeInfo.ACTION_DISMISS
      }
    }
    val target = dismissTarget ?: clickableTarget(node.node) ?: return null
    if (!target.isVisibleToUser || !target.isEnabled) return null

    val compact = node.compactLabel
    val resourceId = node.resourceId
    val closeLabel = isPopupCloseLabel(compact)
    val explicitAdResource = hasAdCloseResourceId(resourceId)
    val dismissAction = dismissTarget != null
    if (!closeLabel && !explicitAdResource && !dismissAction) return null

    val targetBounds = Rect().also(target::getBoundsInScreen)
    if (!hasUsableBounds(targetBounds)) return null
    val nearTopRight = isNearTopRight(targetBounds, snapshot.rootBounds)
    val nearPopupEdge = isNearPopupEdge(targetBounds, snapshot.rootBounds)
    val compactControl = isCompactControl(targetBounds, snapshot.rootBounds)
    val ancestorHasAdSignal = node.ancestorLabels.any(::hasStrongAdSignal)
    val hasAdContext = snapshot.context.hasAdSignal ||
      snapshot.context.hasCommercialAdSignal || ancestorHasAdSignal

    // A generic close icon is too dangerous to click without ad context. The
    // close button can be in the top-right, on a modal's bottom edge, or be
    // exposed as ACTION_DISMISS by a native Dialog.
    if (!hasAdContext && !explicitAdResource) return null
    if (!dismissAction && !nearPopupEdge && !explicitAdResource) return null
    if (!dismissAction && !compactControl && !explicitAdResource) return null

    var score = when {
      dismissAction -> 106
      explicitAdResource -> 92
      compact == "关闭广告" || compact == "关闭弹窗" -> 88
      compact == "关闭" || compact == "close" -> 76
      else -> 62
    }
    if (explicitAdResource) score += 30
    if (nearTopRight) score += 18
    if (nearPopupEdge) score += 18
    if (compactControl) score += 8
    if (hasAdContext) score += 26

    return DetectionAction(
      target = target,
      targetBounds = targetBounds,
      score = score,
      minimumScore = if (dismissAction) 96 else 106,
      actionPriority = 1,
      allowDismiss = dismissAction,
      ruleId = if (explicitAdResource) "builtin.popup.close.resource-id" else "builtin.popup.close.context",
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
          context = AdContext.from(nodes),
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
  ) {
    val hasStrongAdSignal: Boolean get() = strongSignals > 0
    val hasCommercialAdSignal: Boolean get() =
      commercialSignals >= 2 || (commercialSignals > 0 && popupSignals > 0)
    val hasAdSignal: Boolean get() =
      hasStrongAdSignal || mediumSignals >= 2 || hasCommercialAdSignal

    companion object {
      fun from(nodes: List<ScreenNode>): AdContext {
        var strong = 0
        var medium = 0
        var commercial = 0
        var popup = 0
        nodes.forEach { node ->
          if (!node.isVisible) return@forEach
          if (hasStrongAdSignal(node.label, node.resourceId)) strong++
          if (hasMediumAdSignal(node.compactLabel)) medium++
          if (hasCommercialAdSignal(node.compactLabel)) commercial++
          if (hasPopupSignal(node.compactLabel, node.resourceId, node.className)) popup++
        }
        return AdContext(
          strongSignals = strong.coerceAtMost(3),
          mediumSignals = medium.coerceAtMost(4),
          commercialSignals = commercial.coerceAtMost(6),
          popupSignals = popup.coerceAtMost(4),
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
    private const val FOREGROUND_CHANNEL_ID = "clearscreen_accessibility"
    private const val FOREGROUND_NOTIFICATION_ID = 301
    private const val ACTION_COOLDOWN_MS = 480L
    private const val ACTION_VERIFY_DELAY_MS = 650L
    private const val ACTION_REPEAT_GUARD_MS = 1_500L
    private const val MAX_CLICKABLE_ANCESTORS = 6
    private const val MAX_NODES_PER_SNAPSHOT = 1_200
    private const val MAX_TREE_DEPTH = 32
    private const val MAX_ANCESTOR_LABELS = 8
    private const val MAX_REMEMBERED_ACTIONS = 80
    private const val MAX_LOG_LABEL_LENGTH = 80

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
    private val MEDIUM_AD_MARKERS = listOf(
      "立即打开",
      "立即下载",
      "点击跳转",
      "详情页面",
      "免费领取",
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
      "close",
      "dismiss",
      "ad_close",
      "close_ad",
      "closebtn_ad",
      "interstitial",
      "splash",
      "ad_dialog",
      "adclose",
    )

    fun isEnabled(context: Context): Boolean {
      val enabled = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
      ) ?: return false
      val component = ComponentName(context, ClearScreenAccessibilityService::class.java)
        .flattenToString()
      return enabled.split(':').any { it.equals(component, ignoreCase = true) }
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

    private fun hasMediumAdSignal(compactLabel: String): Boolean =
      MEDIUM_AD_MARKERS.any(compactLabel::contains)

    private fun hasCommercialAdSignal(compactLabel: String): Boolean =
      COMMERCIAL_AD_MARKERS.any(compactLabel::contains)

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

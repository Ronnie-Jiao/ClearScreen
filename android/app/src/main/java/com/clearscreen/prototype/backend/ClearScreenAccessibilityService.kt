package com.clearscreen.prototype.backend

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.graphics.Rect
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.util.Locale

class ClearScreenAccessibilityService : AccessibilityService() {
  private lateinit var store: ClearScreenStore
  private var lastActionAt = 0L

  override fun onServiceConnected() {
    super.onServiceConnected()
    store = ClearScreenStore(this)
    running = true
  }

  override fun onAccessibilityEvent(event: AccessibilityEvent?) {
    if (event == null || !::store.isInitialized || !store.isMasterEnabled()) return
    val packageName = event.packageName?.toString()?.takeIf { it.isNotBlank() } ?: return
    // Never inspect or click our own UI. The app intentionally contains labels such as
    // "今日自动跳过" and "自动跳过权限"; treating those as ad controls would trigger
    // the matching Pressable and send the user to the records page.
    if (packageName == applicationContext.packageName) return
    val rule = store.getAppRule(packageName)
    if (!rule.skipEnabled || rule.whitelist) return

    val now = System.currentTimeMillis()
    if (now - lastActionAt < 700L) return
    val root = findRootForPackage(packageName) ?: return
    val action = findAdAction(root) ?: return
    lastActionAt = now
    val clicked = action.target.performAction(AccessibilityNodeInfo.ACTION_CLICK) ||
      action.target.performAction(AccessibilityNodeInfo.ACTION_DISMISS)
    store.appendEvent(
      type = "skip",
      packageName = packageName,
      ruleId = action.ruleId,
      result = if (clicked) "success" else "failed",
      text = if (clicked) action.successText else "处理失败",
    )
  }

  override fun onInterrupt() = Unit

  override fun onDestroy() {
    running = false
    super.onDestroy()
  }

  private fun findAdAction(node: AccessibilityNodeInfo): AccessibilityAction? {
    findSkipTarget(node)?.let { target ->
      return AccessibilityAction(target, "builtin.skip.text", "已跳过开屏广告")
    }

    if (!containsAdMarker(node)) return null
    val rootBounds = Rect().also(node::getBoundsInScreen)
    findPopupCloseTarget(node, rootBounds)?.let { target ->
      return AccessibilityAction(target, "builtin.popup.close", "已关闭弹窗广告")
    }
    return null
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

  private fun findSkipTarget(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
    val label = listOfNotNull(
      node.text?.toString(),
      node.contentDescription?.toString(),
    ).joinToString(" ").trim()
    val matches = isSkipLabel(label)
    if (matches && node.isVisibleToUser) {
      clickableTarget(node)?.let { return it }
    }
    for (index in 0 until node.childCount) {
      val child = node.getChild(index) ?: continue
      val result = findSkipTarget(child)
      if (result != null) return result
    }
    return null
  }

  private fun findPopupCloseTarget(
    node: AccessibilityNodeInfo,
    rootBounds: Rect,
  ): AccessibilityNodeInfo? {
    val label = listOfNotNull(
      node.text?.toString(),
      node.contentDescription?.toString(),
    ).joinToString(" ").trim()
    val resourceId = node.viewIdResourceName.orEmpty().lowercase(Locale.ROOT)
    val isExplicitAdResource = resourceId.contains("close_btn") ||
      resourceId.contains("ad_close") ||
      resourceId.contains("skip") ||
      resourceId.contains("countdown") ||
      resourceId.contains("interstitial") ||
      resourceId.contains("splash")
    if ((isPopupCloseLabel(label) || isExplicitAdResource) && node.isVisibleToUser) {
      val target = clickableTarget(node)
      if (target != null) {
        val bounds = Rect().also(target::getBoundsInScreen)
        val rootWidth = rootBounds.width().coerceAtLeast(1)
        val rootHeight = rootBounds.height().coerceAtLeast(1)
        val isNearTopRight = bounds.centerX() >= rootBounds.left + rootWidth * 0.58f &&
          bounds.top <= rootBounds.top + rootHeight * 0.55f
        val isCompact = bounds.width() <= rootWidth * 0.35f &&
          bounds.height() <= rootHeight * 0.22f
        val isRightEdge = bounds.right >= rootBounds.left + rootWidth * 0.78f
        if (isCompact && (isNearTopRight || label.trim() == "关闭" ||
            (isExplicitAdResource && isRightEdge))) return target
      }
    }
    for (index in 0 until node.childCount) {
      val child = node.getChild(index) ?: continue
      val result = findPopupCloseTarget(child, rootBounds)
      if (result != null) return result
    }
    return null
  }

  private fun clickableTarget(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
    var current: AccessibilityNodeInfo? = node
    repeat(5) {
      if (current?.isVisibleToUser == true && current.isClickable) return current
      current = current?.parent
    }
    return null
  }

  private fun isSkipLabel(label: String): Boolean {
    val normalized = label.trim().lowercase(Locale.ROOT)
    if (normalized.isBlank()) return false
    if (SKIP_LABELS.any { normalized == it || (it != "跳过" && normalized.contains(it)) }) return true
    return normalized.matches(Regex("^\\d{1,3}\\s*(跳过|skip)(广告|视频|此广告|this ad)?$"))
  }

  private fun containsAdMarker(node: AccessibilityNodeInfo): Boolean {
    val label = listOfNotNull(
      node.text?.toString(),
      node.contentDescription?.toString(),
    ).joinToString(" ").trim().lowercase(Locale.ROOT)
    if (AD_MARKERS.any { marker ->
        if (marker == "ad") label == marker || label.startsWith("ad ") || label.endsWith(" ad")
        else label.contains(marker)
      }) return true
    for (index in 0 until node.childCount) {
      val child = node.getChild(index) ?: continue
      if (containsAdMarker(child)) return true
    }
    return false
  }

  private fun isPopupCloseLabel(label: String): Boolean {
    val normalized = label.trim().lowercase(Locale.ROOT)
    return normalized in POPUP_CLOSE_LABELS || normalized.startsWith("关闭") || normalized == "close"
  }

  private data class AccessibilityAction(
    val target: AccessibilityNodeInfo,
    val ruleId: String,
    val successText: String,
  )

  companion object {
    @Volatile
    var running: Boolean = false

    private val SKIP_LABELS = listOf("跳过广告", "跳过", "Skip Ad", "Skip")
    private val POPUP_CLOSE_LABELS = setOf("关闭", "close", "×", "✕", "✖", "✗", "╳", "⨯", "x")
    private val AD_MARKERS = listOf(
      "广告",
      "广告位",
      "推广",
      "赞助",
      "ad",
      "sponsored",
      "advertisement",
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
      "会员",
      "开通",
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
  }
}

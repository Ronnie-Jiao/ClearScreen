package com.clearscreen.prototype.backend

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class ClearScreenAccessibilityService : AccessibilityService() {
  private lateinit var store: ClearScreenStore
  private var lastActionAt = 0L

  override fun onServiceConnected() {
    super.onServiceConnected()
    store = ClearScreenStore(this)
    serviceInfo = serviceInfo.apply {
      eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
      feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
      notificationTimeout = 100
    }
    running = true
  }

  override fun onAccessibilityEvent(event: AccessibilityEvent?) {
    if (event == null || !::store.isInitialized || !store.isMasterEnabled()) return
    val packageName = event.packageName?.toString()?.takeIf { it.isNotBlank() } ?: return
    val rule = store.getAppRule(packageName)
    if (!rule.skipEnabled || rule.whitelist) return

    val now = System.currentTimeMillis()
    if (now - lastActionAt < 700L) return
    val root = rootInActiveWindow ?: return
    val target = findSkipTarget(root) ?: return
    lastActionAt = now
    val clicked = target.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    store.appendEvent(
      type = "skip",
      packageName = packageName,
      ruleId = "builtin.skip.text",
      result = if (clicked) "success" else "failed",
      text = if (clicked) "已跳过开屏广告" else "处理失败",
    )
  }

  override fun onInterrupt() = Unit

  override fun onDestroy() {
    running = false
    super.onDestroy()
  }

  private fun findSkipTarget(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
    val label = listOfNotNull(
      node.text?.toString(),
      node.contentDescription?.toString(),
    ).joinToString(" ").trim()
    val matches = SKIP_LABELS.any { candidate ->
      label.equals(candidate, ignoreCase = true) || label.contains(candidate, ignoreCase = true)
    }
    if (matches && node.isVisibleToUser) {
      if (node.isClickable) return node
      node.parent?.let { parent -> if (parent.isClickable) return parent }
    }
    for (index in 0 until node.childCount) {
      val child = node.getChild(index) ?: continue
      val result = findSkipTarget(child)
      if (result != null) return result
    }
    return null
  }

  companion object {
    @Volatile
    var running: Boolean = false

    private val SKIP_LABELS = listOf("跳过广告", "跳过", "Skip Ad", "Skip")

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

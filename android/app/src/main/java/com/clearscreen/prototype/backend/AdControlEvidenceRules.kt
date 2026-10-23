package com.clearscreen.prototype.backend

import kotlin.math.max
import kotlin.math.min

/** Position is a hint, never an authorization or a veto in this policy. */
internal object AdControlEvidenceRules {
  data class Evidence(
    val visual: Boolean,
    val badge: Boolean,
    val redirect: Boolean,
    val callToAction: Boolean,
    val adContainer: Boolean,
    val fullScreen: Boolean,
    val readyCountdown: Boolean = false,
  ) {
    fun permits(label: AdSemanticRules.ControlLabel): Boolean {
      if (!visual) return false
      if (label.kind == AdSemanticRules.ControlKind.GLYPH) {
        // An ordinary player X has the same shape. Require a bounded ad subtree.
        return !fullScreen && badge && (redirect || callToAction || adContainer)
      }
      if (label.explicitAd) return badge || adContainer || redirect
      if (!fullScreen) return badge && (redirect || callToAction || adContainer)
      return (badge && (redirect || adContainer)) || (adContainer && redirect) ||
        (label.kind == AdSemanticRules.ControlKind.SKIP && readyCountdown && redirect)
    }

    fun permitsVisualTouch(label: AdSemanticRules.ControlLabel): Boolean =
      label.kind != AdSemanticRules.ControlKind.GLYPH && fullScreen && permits(label) &&
        ((badge && redirect) || (adContainer && (badge || redirect)))
  }

  fun isAdBadge(value: String): Boolean {
    val text = AdSemanticRules.normalize(value)
    if (listOf("免广告", "去广告", "无广告", "广告设置", "关闭广告", "跳过广告").any(text::contains)) return false
    return text in setOf("广告", "推荐广告", "互动广告", "推广", "赞助", "ad", "ads", "sponsored", "advertisement") ||
      (text.startsWith("互动广告") && text.length <= 18)
  }

  fun isAdContainer(resourceId: String): Boolean = Regex(
    """(?:^|[/_.-])(?:adview|adcontainer|ad_container|ad_layout|ad_content|ad_root|ad_view|splashad|splash_ad|interstitial|gdt_ad|ksad|tt_ad)(?:$|[/_.\d-])"""
  ).containsMatchIn(resourceId.lowercase())

  fun hasRedirectDisclosure(value: String): Boolean {
    val text = AdSemanticRules.normalize(value)
    val destination = listOf("详情页", "详情页面", "第三方应用", "第三方app", "第三方页面").any(text::contains)
    if (destination && listOf("跳转", "开启", "点击", "滑动", "互动").any(text::contains)) return true
    return listOf("翻转手机", "扭转手机", "扭动手机", "摇一摇", "扭一扭").any(text::contains) &&
      (text.contains("前往淘宝") || text.contains("开启详情"))
  }

  fun hasCallToAction(value: String): Boolean {
    val text = AdSemanticRules.normalize(value)
    return listOf("立即打开", "立即下载", "安全下载", "查看详情", "了解更多", "立即前往").any(text::contains)
  }

  /** Independent touch area, adapted to density and its own scope; no corner checks. */
  fun isIndependentControl(
    target: AdGeometryRules.Box,
    label: AdGeometryRules.Box,
    scope: AdGeometryRules.Box,
    screen: AdGeometryRules.Box,
    density: Double,
    glyph: Boolean,
  ): Boolean {
    if (density <= 0 || screen.width <= 0 || screen.height <= 0 ||
      target.width <= 0 || target.height <= 0 || label.width <= 0 || label.height <= 0 ||
      !contains(screen, target) || !contains(scope, target) || !contains(target, label)
    ) return false
    val screenArea = screen.width.toLong() * screen.height
    val targetArea = target.width.toLong() * target.height
    val maxWidth = min(screen.width * 0.52, max(180 * density, screen.width * 0.34))
    val maxHeight = min(screen.height * 0.20, max(64 * density, screen.height * 0.11))
    if (target.width > maxWidth || target.height > maxHeight || targetArea > screenArea * 0.065 ||
      target.width > scope.width * 0.80 || label.height > max(64 * density, screen.height * 0.07)
    ) return false
    if (glyph && (target.width > max(64 * density, screen.width * 0.16) ||
        label.width > max(40 * density, screen.width * 0.08))) return false
    // A padded touch area can occupy most of a thin ad row's height.
    return target.width <= scope.width * 0.24 ||
      targetArea <= scope.width.toLong() * scope.height * 0.30
  }

  fun contains(outer: AdGeometryRules.Box, inner: AdGeometryRules.Box): Boolean =
    inner.width > 0 && inner.height > 0 && inner.left >= outer.left && inner.top >= outer.top &&
      inner.right <= outer.right && inner.bottom <= outer.bottom
}

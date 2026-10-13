package com.clearscreen.prototype.backend

import java.util.Locale

/**
 * Package-independent, testable semantics for advertising UI controls.
 *
 * This layer ONLY recognizes words / resource-id patterns. It never authorizes
 * a click by itself: the AccessibilityService separately checks geometry,
 * local advertising context, foreground package and user settings.
 *
 * Keep this module Android-free so it can be tested without a phone.
 */
internal object AdSemanticRules {
  private val ignoredPunctuation = Regex("""[\s\p{Z}\-_:：，,。.!！？?()（）\[\]{}<>《》【】'"\x60~·|/\\]+""")
  private val numericSkip = listOf(
    Regex("""^\d{1,3}(?:秒|s)?(?:后)?(?:跳过|skip)(?:广告|此广告|视频|ad|thisad|video)?$"""),
    Regex("""^(?:跳过|skip)(?:广告|此广告|视频|ad|thisad|video)?\d{1,3}(?:秒|s)?$"""),
    Regex("""^(?:skipin|跳过倒计时)\d{1,3}(?:秒|s)?$"""),
  )
  private val explicitSkip = setOf(
    "跳过广告", "跳过此广告", "跳过视频",
    "跳过推广", "跳过开屏", "跳过开屏广告",
    "skipad", "skipthisad", "skipvideo",
    "skipadvertisement", "skipnow",
  )
  private val close = setOf(
    "关闭", "关掉", "关闭弹窗", "关闭广告", "关闭此广告",
    "关闭推广", "关闭开屏广告",
    "close", "dismiss", "closead", "closeadvertisement",
    "×", "✕", "✖", "✗", "╳", "⨯", "x",
  )
  private val explicitAdClose = setOf(
    "关闭广告", "关闭此广告", "关闭推广", "关闭开屏广告",
    "closead", "closeadvertisement", "dismissad",
  )
  private val strongAdSignals = listOf(
    "广告", "广告位", "开屏广告", "推广", "赞助",
    "sponsored", "advertisement", "interstitial", "splashad",
  )
  private val mediumAdSignals = listOf(
    "立即下载", "立即打开", "立即办理", "点击跳转",
    "详情页面", "免费领取", "福利", "优惠", "促销",
    "特惠", "红包", "svip",
  )
  private val sensitiveSignals = listOf(
    "隐私政策", "用户协议", "个人信息", "权限申请", "权限管理",
    "系统权限", "支付密码", "支付确认", "验证码",
    "实名认证", "账户安全", "身份验证", "新手引导", "操作教程",
    "删除账号", "退出登录",
  )
  // Token boundaries matter: "jump" and "skip" within unrelated long ids
  // should not automatically mark a button as a skip.
  private val skipId = Regex(
    """(?:^|[/_.])(?:skip|skipbtn|skipbutton|skipview|skiptext|skipad|adskip|splashskip|countdownskip|timerskip)(?:$|[/_.\d])"""
  )
  private val adCloseId = Regex(
    """(?:^|[/_.])(?:ad[_-]?close|close[_-]?ad|adclosebtn|adclosebutton|closebtn[_-]?ad|splash[_-]?close|interstitial[_-]?close|closeinterstitial|addismiss)(?:$|[/_.\d])"""
  )

  fun normalize(value: String): String =
    ignoredPunctuation.replace(value.lowercase(Locale.ROOT), "")

  fun isPlainSkip(compact: String): Boolean = compact == "跳过" || compact == "skip"

  fun isCountdownSkip(compact: String): Boolean =
    numericSkip.any { it.matches(compact) }

  fun isExplicitSkip(compact: String): Boolean =
    compact in explicitSkip ||
      (compact.startsWith("跳过") && compact.length <= 12 &&
        (compact.contains("广告") || compact.contains("推广") || compact.contains("开屏"))) ||
      (compact.startsWith("skipad") && compact.length <= 20)

  fun isClose(compact: String): Boolean = compact in close

  fun isExplicitAdClose(compact: String): Boolean = compact in explicitAdClose

  fun isSkipResource(id: String): Boolean = skipId.containsMatchIn(id)

  fun isAdCloseResource(id: String): Boolean = adCloseId.containsMatchIn(id)

  fun hasStrongAdSignal(text: String, resourceId: String = ""): Boolean {
    val compact = normalize(text)
    return strongAdSignals.any(compact::contains) ||
      listOf("ad_", "_ad", "adview", "interstitial", "splashad")
        .any(resourceId::contains)
  }

  fun hasMediumAdSignal(compact: String): Boolean =
    mediumAdSignals.any(compact::contains)

  fun isSensitiveUiContext(text: String): Boolean {
    val compact = normalize(text)
    return sensitiveSignals.any(compact::contains)
  }
}

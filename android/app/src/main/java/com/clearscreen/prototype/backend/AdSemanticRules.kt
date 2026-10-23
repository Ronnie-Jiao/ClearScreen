package com.clearscreen.prototype.backend

import java.util.Locale
import java.text.Normalizer

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
  private val ignoredPunctuation = Regex("""[\s\p{Z}\p{Cf}\-_:：，,。.!！？?()（）\[\]{}<>《》【】'"\x60~·|/\\]+""")
  private val numericSkip = listOf(
    Regex("""^\d{1,3}(?:秒|s)?(?:后)?(?:跳过|skip)(?:广告|此广告|视频|ad|thisad|video)?$"""),
    Regex("""^(?:跳过|skip)(?:广告|此广告|视频|ad|thisad|video)?\d{1,3}(?:秒|s)?$"""),
    Regex("""^(?:skipin|跳过倒计时)\d{1,3}(?:秒|s)?$"""),
  )
  private val explicitSkip = setOf(
    "跳过广告", "跳过此广告", "跳过这条广告", "立即跳过广告", "广告跳过", "跳过视频",
    "跳过视频广告", "跳过贴片广告", "跳过片前广告", "跳过中插广告",
    "跳过推广", "跳过开屏", "跳过开屏广告",
    "skipad", "skipads", "skipthisad", "skipvideoad", "skipvideo",
    "skipadvertisement", "skipadvertisements", "skipnow", "跳过此推广",
  )
  private val close = setOf(
    "关闭", "关掉", "关闭弹窗", "关闭广告", "关闭此广告",
    "关闭推广", "关闭开屏广告", "关闭这条广告", "关闭视频广告", "关闭此推广",
    "close", "dismiss", "closead", "closeadvertisement", "dismissad", "closethisad",
    "×", "✕", "✖", "✗", "╳", "⨯", "x",
  )
  private val explicitAdClose = setOf(
    "关闭广告", "关闭此广告", "关闭推广", "关闭开屏广告",
    "关闭这条广告", "关闭视频广告", "关闭此推广",
    "closead", "closeadvertisement", "dismissad", "closethisad",
  )
  private val strongAdSignals = listOf(
    "广告", "广告位", "开屏广告", "推广", "赞助",
    "sponsored", "advertisement", "interstitial", "splashad",
  )
  private val mediumAdSignals = listOf(
    "立即下载", "安全下载", "立即打开", "立即办理", "点击跳转",
    "详情页面", "免费领取", "福利", "优惠", "促销",
    "特惠", "红包", "svip", "低至", "限时", "领券", "折扣",
  )
  private val sensitiveSignals = listOf(
    "隐私政策", "用户协议", "个人信息", "权限申请", "权限管理",
    "系统权限", "支付密码", "支付确认", "验证码",
    "实名认证", "账户安全", "身份验证", "新手引导", "操作教程",
    "删除账号", "退出登录",
  )
  private val unsafeSkipLabels = listOf(
    "跳过片头", "跳过片尾", "跳过前情", "跳过回顾", "跳过教程", "跳过引导",
    "跳过验证", "跳过绑定", "跳过登录", "跳过支付", "跳过授权", "跳过这个步骤",
    "skipintro", "skipoutro", "skiprecap", "skiptutorial", "skiplogin", "skipverification",
  )
  private val pendingSkip = Regex("""(?:\d{1,3}(?:秒|s)?后(?:可|可以)?跳过|后可(?:以)?跳过|skipin\d|跳过倒计时)""")
  private val businessControls = listOf(
    "跳过剧情", "关闭视频", "关闭播放器", "退出播放", "退出全屏", "关闭自动续费",
    "关闭账户", "关闭账号", "关闭权限", "关闭提醒", "取消订阅", "支付确认", "验证码",
    "实名认证", "新手引导", "操作教程", "跳过本步骤", "跳过当前步骤", "skipguide", "skiponboard", "skipwelcome",
    "skipauth", "skippermission", "skipprivacy", "closeplayer", "exitfullscreen",
  )
  private val glyphs = setOf("×", "✕", "✖", "✗", "╳", "⨯", "x")
  private val businessResourceMarkers = listOf(
    "skip_intro", "skip_tutorial", "skip_guide", "skip_onboard", "skip_welcome",
    "skip_login", "skip_auth", "skip_verify", "skip_payment", "skip_permission", "skip_privacy",
    "close_player", "closeplayer", "exit_fullscreen", "exitfullscreen", "close_settings",
  )

  enum class ControlKind { SKIP, CLOSE, GLYPH }
  data class ControlLabel(val value: String, val kind: ControlKind, val explicitAd: Boolean)

  /** Only whole action labels qualify; a matching word inside business copy does not. */
  fun controlLabel(labels: List<String>): ControlLabel? {
    if (labels.any(::isBlockedControlLabel)) return null
    val actions = labels.map { value ->
      val compact = normalize(value)
      val action = compact.removeSuffix("按钮").removeSuffix("button")
      if (isPlainSkip(action) || isExplicitSkip(action) || isClose(action)) action else compact
    }
    selectSkipLabel(actions)?.let { return ControlLabel(it, ControlKind.SKIP, isExplicitSkip(it)) }
    selectCloseLabel(actions)?.let {
      return ControlLabel(it, if (it in glyphs) ControlKind.GLYPH else ControlKind.CLOSE, isExplicitAdClose(it))
    }
    return null
  }

  fun isBlockedControlLabel(value: String): Boolean {
    val compact = normalize(value)
    if (compact in explicitAdClose) return false
    return isBlockedSkipLabel(value) || businessControls.any(compact::contains)
  }

  fun isControlCounter(value: String): Boolean = Regex("""^\d{1,3}(?:秒|s)?$""").matches(normalize(value))

  fun isBusinessControlResource(resourceId: String): Boolean =
    businessResourceMarkers.any(resourceId.lowercase(Locale.ROOT).substringAfter(":id/")::contains)
  // Token boundaries matter: "jump" and "skip" within unrelated long ids
  // should not automatically mark a button as a skip.
  private val skipId = Regex(
    """(?:^|[/_.])(?:skip|skipbtn|skipbutton|skipview|skiptext|skipad|adskip|splashskip|countdownskip|timerskip)(?:$|[/_.\d])"""
  )
  private val adCloseId = Regex(
    """(?:^|[/_.])(?:ad[_-]?close|close[_-]?ad|adclosebtn|adclosebutton|closebtn[_-]?ad|splash[_-]?close|interstitial[_-]?close|closeinterstitial|addismiss)(?:$|[/_.\d])"""
  )

  fun normalize(value: String): String =
    ignoredPunctuation.replace(Normalizer.normalize(value, Normalizer.Form.NFKC).lowercase(Locale.ROOT), "")

  /** A ready skip remains the same control as its displayed countdown ticks. */
  fun ocrEvidenceLabel(value: String): String {
    val compact = normalize(value)
    return if (!isBlockedSkipLabel(value) && (isPlainSkip(compact) || isCountdownSkip(compact))) {
      "<ready-ad-skip>"
    } else compact
  }

  /** Some SDKs expose the number and the ready skip label as sibling nodes. */
  fun ocrEvidenceLabel(value: String, insideReadySkipControl: Boolean): String =
    if (insideReadySkipControl && Regex("""^\d{1,3}(?:秒|s)?$""").matches(normalize(value))) {
      "<ready-ad-skip-counter>"
    } else ocrEvidenceLabel(value)

  fun hasPendingSkipHint(labels: List<String>): Boolean = labels.any { label ->
    val compact = normalize(label)
    isBlockedSkipLabel(label) || compact.contains("后可") || compact.contains("秒后") || compact.contains("skipin")
  }

  fun isBlockedSkipLabel(label: String): Boolean {
    val compact = normalize(label)
    return unsafeSkipLabels.any(compact::contains) || pendingSkip.containsMatchIn(compact)
  }

  /** Text and accessibility description are alternatives, not one invented label. */
  fun selectSkipLabel(labels: List<String>): String? {
    if (labels.any(::isBlockedSkipLabel)) return null
    val variants = labels.map(::normalize).distinct()
    return variants.firstOrNull(::isExplicitSkip) ?: variants.firstOrNull(::isCountdownSkip) ?:
      variants.firstOrNull(::isPlainSkip)
  }

  fun selectCloseLabel(labels: List<String>): String? {
    if (labels.any(::isBlockedControlLabel)) return null
    val variants = labels.map(::normalize).distinct()
    return variants.firstOrNull(::isExplicitAdClose) ?: variants.firstOrNull(::isClose)
  }

  fun isNeutralControlLabel(value: String): Boolean = normalize(value) in setOf(
    "", "按钮", "button", "图片", "image", "imagebutton", "关闭按钮", "closebutton",
  )

  fun isPlainSkip(compact: String): Boolean = compact == "跳过" || compact == "skip"

  fun isCountdownSkip(compact: String): Boolean =
    numericSkip.any { it.matches(compact) }

  fun isExplicitSkip(compact: String): Boolean =
    compact in explicitSkip

  fun isClose(compact: String): Boolean = compact in close

  fun isExplicitAdClose(compact: String): Boolean = compact in explicitAdClose

  fun isSkipResource(id: String): Boolean =
    !isBusinessControlResource(id) && skipId.containsMatchIn(id)

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

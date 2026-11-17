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
  private val readyNumericSkip = listOf(
    Regex("""^(\d{1,3})(?:秒|s)?(?:后)?(?:跳过|skip)(?:广告|此广告|视频|ad|thisad|video)?$"""),
    Regex("""^(?:跳过|skip)(?:广告|此广告|视频|ad|thisad|video)?(\d{1,3})(?:秒|s)?$"""),
  )
  private val numericSkip = readyNumericSkip + listOf(
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
    "关闭这条广告", "关闭视频广告", "关闭贴片广告", "关闭中插广告", "关闭此推广",
    "closead", "closeadvertisement", "dismissad", "closethisad",
  )
  private val adTarget = "(?:广告(?:内容|页面|弹窗|活动)?|视频广告|商业推广|品牌推广|推广(?:内容)?|赞助内容|开屏广告|贴片广告|片前广告|中插广告|插屏广告|激励视频|促销(?:广告|活动)?|广告推广|ads?|advertisements?|advertising|promotion|sponsoredcontent|sponsoredpost|promotedpost|promotedcontent|preroll|midroll|interstitial)"
  private val targetPrefix = "(?:(?:本次|此次|这次|本条|这条|当前(?:这条)?|这个|该|此|这|this|that|the|current))?"
  private val composedExplicitSkip = Regex(
    """^(?:(?:先|立即|现在|马上|直接|果断|毅然|一键|狠心|忍痛)?(?:跳过|略过)${targetPrefix}${adTarget}|(?:now|immediately|quickly)?skip(?:this|that|the|current)?(?:ad|ads|video|advertisement|promotion))$"""
  )
  private val composedClose = Regex(
    """^(?:(?:请|立即|现在|马上|直接|点此|点击|一键)?(?:关闭|关掉|关上|关闭掉|收起|隐藏|移除|退出|离开|dismiss|close|hide|remove)(?:${targetPrefix}(?:弹窗|窗口|页面|提示|广告|推广|开屏广告|视频广告|贴片广告|中插广告|促销|活动|优惠|福利|奖励|礼包|内容|offer|promotion|advertisement))?|(?:广告|推广|弹窗|窗口|促销|活动|优惠|福利|奖励|礼包|promotion|offer)(?:关闭|关掉|关闭掉|dismiss|close))$"""
  )
  private val explicitAdClosePhrase = Regex(
    """^(?:(?:请|立即|现在|马上|直接|点此|点击|一键)?(?:关闭|关掉|关上|关闭掉|跳过|略过|退出|离开|拒绝|放弃|不看|dismiss|close|skip|leave|decline|reject|exit)${targetPrefix}${adTarget}|${adTarget}(?:关闭|关掉|关闭掉|跳过|略过|退出|离开|关闭按钮|close|skip|dismiss))$"""
  )
  private val dramaticDismissal = Regex(
    """^(?:残忍|狠心|忍痛|不舍|舍不得|忍心|果断|毅然|痛快|无情)(?:地|的)?(?:离开|退出|关闭|关掉|关闭掉|放弃|拒绝|不看|跳过|略过)(?:(?:本次|此次|这次|当前|这个|此|这)?(?:广告|推广|开屏|视频广告|贴片广告|中插广告|优惠|活动|福利|奖励|礼包|会员|内容))?$"""
  )
  private val offerDecline = Regex(
    """^(?:我)?(?:放弃|拒绝|不要|不需要|不想要|暂不|暂时不|稍后|以后|算了|不了|不想看|暂时不看|不再看|不再参与|不继续)(?:(?:本次|此次|这次|当前|这个|该|此|这)?(?:广告|推广|优惠|福利|活动|奖励|礼包|会员|特权|参与|领取|观看|开通|订购|购买|安装|下载|使用|权益|内容|继续|浏览|逛逛|再说))$"""
  )
  private val deferredDismissals = setOf(
    "以后再说", "稍后再说", "下次再说", "暂不", "暂时不", "先不了", "不用了",
    "不需要", "不用", "不想要", "不要", "不想看", "不再看", "不继续", "暂时不看", "稍后", "以后", "不需要了", "不要了", "不想要了", "不看了", "不想看了", "不继续了", "不了谢谢",
    "不谢谢", "以后再说吧", "稍后再说吧", "不想参与", "暂不参与", "放弃领取",
    "nothanks", "nothankyou", "notnow", "maybelater", "nevermind",
    "declineoffer", "rejectoffer", "leaveoffer", "leavead", "exitad",
  )
  private val plainSkipPhrase = Regex(
    """^(?:(?:请|先|立即|现在|马上|直接|继续)?(?:跳过|略过)(?:吧|掉)?|skip(?:this)?)$"""
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
  private val promotionalBenefitSignals = listOf(
    "大奖", "奖品", "奖金", "奖励", "好运", "福利", "优惠券", "优惠", "折扣", "特惠",
    "免单", "红包", "礼包", "礼品", "返现", "返利", "立减", "满减", "补贴", "赠品",
    "免费", "专享", "权益", "低至", "限时", "prize", "reward", "bonus", "gift",
    "deal", "offer", "discount", "coupon", "cashback", "free",
  )
  private val promotionalActionSignals = listOf(
    "赢", "中奖", "抽奖", "领取", "领券", "免费领", "抢购", "抢红包", "马上抢", "参与",
    "兑换", "点击", "立即", "马上", "购买", "前往", "下载", "安装", "体验", "开通",
    "订购", "等你拿", "等你来拿", "win", "enter", "claim", "redeem", "shop", "buy",
    "join", "download", "install", "playnow", "signup", "subscribe",
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
      if (isPlainSkip(action) || isExplicitSkip(action) || isCountdownSkip(action) || isClose(action)) action else compact
    }
    selectSkipLabel(actions)?.let { return ControlLabel(it, ControlKind.SKIP, isExplicitSkip(it)) }
    selectCloseLabel(actions)?.let {
      return ControlLabel(it, if (it in glyphs) ControlKind.GLYPH else ControlKind.CLOSE, isExplicitAdClose(it))
    }
    return null
  }

  fun isBlockedControlLabel(value: String): Boolean {
    val compact = normalize(value)
    if (isExplicitAdClose(compact)) return false
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

  fun isPlainSkip(compact: String): Boolean = plainSkipPhrase.matches(normalize(compact))

  fun isCountdownSkip(compact: String): Boolean =
    numericSkip.any { it.matches(compact) }

  /** Remaining display time bounds evidence when the inaccessible ad changes its background tree. */
  fun readySkipCountdownMillis(value: String): Long? {
    if (isBlockedSkipLabel(value)) return null
    val compact = normalize(value)
    return readyNumericSkip.firstNotNullOfOrNull { pattern ->
      pattern.matchEntire(compact)?.groupValues?.get(1)?.toLongOrNull()?.times(1_000L)
    }
  }

  fun isZeroCountdownSkip(value: String): Boolean = readySkipCountdownMillis(value) == 0L

  fun isExplicitSkip(compact: String): Boolean {
    val text = normalize(compact)
    return text in explicitSkip || composedExplicitSkip.matches(text)
  }

  fun isClose(compact: String): Boolean {
    val text = normalize(compact)
    return text in close || isExplicitAdClose(text) || composedClose.matches(text) ||
      dramaticDismissal.matches(text) || offerDecline.matches(text) || text in deferredDismissals
  }

  /** Ambiguous refusal/defer phrases need locally verified ad evidence. */
  fun isContextualAdDismissal(compact: String): Boolean {
    val text = normalize(compact)
    return text !in close && (
      dramaticDismissal.matches(text) || offerDecline.matches(text) || text in deferredDismissals
    )
  }

  fun isExplicitAdClose(compact: String): Boolean {
    val text = normalize(compact)
    return text in explicitAdClose || explicitAdClosePhrase.matches(text)
  }

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

  /**
   * Recognizes promotion intent from generic benefit/reward and action semantics,
   * independent of a campaign title or a single hard-coded ad sentence.
   */
  fun hasCommercialOfferIntent(text: String): Boolean {
    val compact = normalize(text)
    if (compact.isEmpty()) return false
    return promotionalBenefitSignals.any(compact::contains) &&
      promotionalActionSignals.any(compact::contains)
  }

  fun isSensitiveUiContext(text: String): Boolean {
    val compact = normalize(text)
    return sensitiveSignals.any(compact::contains)
  }
}

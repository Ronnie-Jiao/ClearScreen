package com.clearscreen.prototype.backend

import java.util.Locale

/**
 * Conservative playback-ad controls for major Chinese video platforms.
 *
 * A video player contains many ordinary "跳过", "关闭", "下一集" and VIP buttons.
 * Never treat a plain close/X or a countdown by itself as an ad dismissal.
 * Classification has no Android dependencies and cannot trigger an action.
 */
internal object VideoAdRules {
  private val platforms = mapOf(
    "com.qiyi.video" to "爱奇艺",
    "com.tencent.qqlive" to "腾讯视频",
    "com.youku.phone" to "优酷",
    "com.hunantv.imgo.activity" to "芒果TV",
  )

  enum class Action {
    NONE,
    EXPLICIT_SKIP,
    EXPLICIT_CLOSE,
    CONTEXTUAL_SKIP,
  }

  private val skipLabels = setOf(
    "跳过广告", "跳过此广告", "跳过这条广告", "跳过贴片广告",
    "跳过视频广告", "跳过片前广告", "跳过中插广告",
    "广告跳过", "立即跳过广告",
    "skipad", "skipthisad", "skipvideoad", "skipads",
    "skipadvertisement", "skipadvertisements",
  )
  private val closeLabels = setOf(
    "关闭广告", "关闭此广告", "关闭这条广告", "关闭贴片广告",
    "关闭视频广告", "关闭中插广告",
    "closead", "closethisad", "closeadvertisement",
    "dismissad",
  )
  private val genericSkipLabels = setOf("跳过", "skip")
  private val unsafePlaybackTokens = listOf(
    "跳过片头", "跳过片尾", "跳过前情", "跳过回顾",
    "跳过教程", "跳过引导", "跳过剧情",
    "下一集", "上集", "下一段", "退出播放", "退出全屏",
    "退出播放器", "关闭视频", "关闭播放器",
    "试看", "选集", "倍速", "弹幕", "章节", "重播",
    "购买", "支付", "充值", "订阅", "开通", "续费",
    "skipintro", "skipoutro", "skiprecap", "nextvideo",
    "nextepisode", "closeplayer", "exitfullscreen",
    "subscribe", "purchase", "payment",
  )
  private val notYetSkippable = listOf(
    "后可跳过", "后跳过", "跳过倒计时",
    "秒后可跳过", "秒后跳过",
    "skipin", "untilskip", "remaining",
  )
  private val adIdMarkers = setOf(
    "ad", "ads", "advert", "advertisement", "preroll",
    "midroll", "interstitial", "video_ad", "videoad",
  )
  private val actionIdMarkers = setOf(
    "skip", "skipbtn", "skipbutton", "adskip",
    "close", "closebtn", "closebutton", "dismiss",
  )
  private val splitId = Regex("[^a-z0-9]+")
  private val countdownSkip = Regex("""^\d{1,2}(?:秒|s)?跳过$""")

  fun isSupported(packageName: String): Boolean = packageName in platforms

  fun platformName(packageName: String): String? = platforms[packageName]

  fun isUnsafePlaybackControl(label: String): Boolean {
    val compact = AdSemanticRules.normalize(label)
    return unsafePlaybackTokens.any(compact::contains) ||
      notYetSkippable.any(compact::contains)
  }

  private fun isExplicitAdResource(resourceId: String, action: String): Boolean {
    val id = resourceId.lowercase(Locale.ROOT).substringAfter(":id/")
    if (id.isBlank() || isUnsafePlaybackControl(id)) return false
    val tokens = splitId.split(id).filter(String::isNotBlank)
    val hasAd = tokens.any { it in adIdMarkers } ||
      listOf("adskip", "skipad", "adclose", "closead", "videoad", "advideo")
        .any(id::contains)
    val hasAction = tokens.any { it in actionIdMarkers } ||
      listOf("adskip", "skipad", "adclose", "closead").any(id::contains)
    return hasAd && hasAction && when (action) {
      "skip" -> tokens.any { it.startsWith("skip") } ||
        id.contains("adskip") || id.contains("skipad")
      "close" -> tokens.any { it.startsWith("close") || it == "dismiss" } ||
        id.contains("adclose") || id.contains("closead")
      else -> false
    }
  }

  fun hasAdEvidence(label: String, resourceId: String = ""): Boolean {
    val compact = AdSemanticRules.normalize(label)
    // Label "广告" on a player's local overlay counts as evidence; a
    // promotion elsewhere on the page must NOT, so callers bound proximity.
    return compact.contains("广告") ||
      compact.contains("advertisement") ||
      compact == "sponsored" ||
      compact.contains("贴片") ||
      compact.contains("preroll") ||
      compact.contains("midroll") ||
      resourceId.lowercase(Locale.ROOT).let { id ->
        id.contains("ad_view") || id.contains("video_ad") ||
          id.contains("preroll") || id.contains("midroll") ||
          id.contains("advertisement")
      }
  }

  /**
   * The caller has already checked that the app is foreground, the node is
   * visible/compact and ProtectedUiPolicy did not veto the entire window.
   *
   * - Explicit ad-labelled buttons can be acted on in a player.
   * - Generic Skip / countdown needs LOCAL ad evidence AND the right-hand edge.
   * - Resource-ID-only matches still require local evidence.
   * - Generic X / close are never accepted in video players.
   */
  fun classify(
    labels: List<String>,
    resourceId: String,
    nearbyAdEvidence: Boolean,
    atRightEdge: Boolean,
  ): Action {
    val variants = labels.map(AdSemanticRules::normalize).filter(String::isNotBlank)
    if (variants.any(::isUnsafePlaybackControl) || isUnsafePlaybackControl(resourceId)) return Action.NONE

    if (variants.any { it in skipLabels }) return Action.EXPLICIT_SKIP
    if (variants.any { it in closeLabels }) return Action.EXPLICIT_CLOSE

    if (nearbyAdEvidence && isExplicitAdResource(resourceId, "skip")) return Action.EXPLICIT_SKIP
    if (nearbyAdEvidence && isExplicitAdResource(resourceId, "close")) return Action.EXPLICIT_CLOSE

    if (atRightEdge && nearbyAdEvidence &&
      variants.any { it in genericSkipLabels || countdownSkip.matches(it) }
    ) return Action.CONTEXTUAL_SKIP

    return Action.NONE
  }
}

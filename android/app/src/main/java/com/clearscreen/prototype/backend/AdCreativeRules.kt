package com.clearscreen.prototype.backend

/** Text/geometry evidence only. Callers still require a real, independent clickable node. */
internal object AdCreativeRules {
  data class Text(val value: String, val bounds: AdGeometryRules.Box)

  /** The SDK wires this visible X to its ad click-through action, not dismissal. */
  fun isUnsafeCaiyunInterstitialClickId(packageName: String, resourceId: String): Boolean =
    packageName == "com.nowcasting.activity" &&
      resourceId.equals("com.nowcasting.activity:id/cll_close_click", ignoreCase = true)

  /** Observed variants; callers must still verify the SDK subtree, badge and geometry. */
  fun isCaiyunInterstitialCloseId(packageName: String, resourceId: String): Boolean =
    packageName == "com.nowcasting.activity" && resourceId in setOf(
      "com.nowcasting.activity:id/cll_close_3",
    )

  /** Live Caiyun splash: the SDK exposes a redirect hint but rasterizes its ad badge. */
  fun hasCaiyunRedirectSplash(
    packageName: String,
    containerId: String,
    container: AdGeometryRules.Box,
    screen: AdGeometryRules.Box,
    skipText: String,
    skip: AdGeometryRules.Box,
    texts: List<Text>,
    creatives: List<AdGeometryRules.Box>,
  ): Boolean {
    val label = AdSemanticRules.normalize(skipText)
    if (packageName != "com.nowcasting.activity" ||
      containerId != "com.nowcasting.activity:id/splash_container" ||
      AdSemanticRules.isBlockedSkipLabel(skipText) ||
      (!AdSemanticRules.isPlainSkip(label) && !AdSemanticRules.isCountdownSkip(label)) ||
      !contains(screen, container) || !contains(container, skip) ||
      container.width < screen.width * 0.90 || container.height < screen.height * 0.70 ||
      skip.width > container.width * 0.30 || skip.height > container.height * 0.10 ||
      skip.centerX < container.left + container.width * 0.70 ||
      skip.top > container.top + container.height * 0.22
    ) return false
    val largeCreative = creatives.any { creative ->
      contains(container, creative) && creative.width >= container.width * 0.90 &&
        creative.height >= container.height * 0.70
    }
    val redirectHint = texts.any { text ->
      contains(container, text.bounds) &&
        text.bounds.centerY >= container.top + container.height * 0.40 &&
        text.bounds.centerY <= container.top + container.height * 0.90 &&
        AdSemanticRules.normalize(text.value) in setOf(
          "摇一摇跳转至第三方应用或详情页",
          "点击跳转详情页或第三方应用",
        )
    }
    return largeCreative && redirectHint
  }

  private val version = Regex("""版本(?:号)?\s*[:：]?\s*\d+(?:\.\d+){1,3}""")
  private val downloadActions = listOf("点击立即下载", "立即下载", "抢先下载", "安全下载")
  private val disclosureLinks = listOf("功能", "权限", "隐私", "备案")
  private val redirectActions = setOf(
    "点击跳转详情页面", "点击跳转详情页", "点击跳转第三方应用", "点击跳转第三方页面",
  )
  private val motionActions = setOf("扭动手机开启详情", "扭一扭开启详情", "摇一摇开启详情")
  private val motionRedirectHints = setOf(
    "互动跳转详情页或第三方应用", "互动跳转至详情页或第三方应用",
  )
  private val badges = setOf("广告", "推广", "赞助", "sponsored", "advertisement")

  /** The weather-page samples expose download-ad disclosures instead of an ad badge. */
  fun hasDownloadDisclosure(packageName: String, texts: List<String>): Boolean {
    if (packageName != "com.nowcasting.activity") return false
    val raw = texts.joinToString(" ")
    val compact = AdSemanticRules.normalize(raw)
    return compact.contains("应用名称") && compact.contains("开发者") &&
      version.containsMatchIn(raw) && disclosureLinks.count(compact::contains) >= 2 &&
      downloadActions.any(compact::contains)
  }

  /** The Mobile campaign carousel places its real close above the poster. */
  fun hasMobileCampaignPosterComposition(
    packageName: String,
    poster: AdGeometryRules.Box,
    close: AdGeometryRules.Box,
    screen: AdGeometryRules.Box,
    posterTexts: List<String>,
    overlayTexts: List<Text>,
  ): Boolean {
    if (packageName != "com.greenpoint.android.mc10086.activity" ||
      !contains(screen, poster) || !contains(screen, close) ||
      poster.width !in (screen.width * 0.50).toInt()..(screen.width * 0.86).toInt() ||
      poster.height !in (screen.height * 0.26).toInt()..(screen.height * 0.65).toInt() ||
      poster.centerX !in (screen.left + screen.width * 0.40).toInt()..(screen.left + screen.width * 0.60).toInt() ||
      close.width > screen.width * 0.14 || close.height > screen.height * 0.09 ||
      close.centerX !in (poster.right - screen.width * 0.10).toInt()..(poster.right + screen.width * 0.14).toInt() ||
      close.centerY !in (poster.top - screen.height * 0.14).toInt()..(poster.top + screen.height * 0.04).toInt()
    ) return false
    val copy = AdSemanticRules.normalize(posterTexts.joinToString(""))
    if (!listOf("广东移动", "视频号", "福利", "5gb", "立即前往").all(copy::contains) ||
      !(copy.contains("数量有限") || copy.contains("派完即止"))
    ) return false
    val belowPoster = overlayTexts.filter {
      it.bounds.centerY in poster.bottom..(poster.bottom + screen.height * 0.15).toInt()
    }
    val previous = belowPoster.firstOrNull { AdSemanticRules.normalize(it.value) == "上一条" } ?: return false
    val next = belowPoster.firstOrNull { AdSemanticRules.normalize(it.value) == "下一条" } ?: return false
    return previous.bounds.centerX < poster.centerX && next.bounds.centerX > poster.centerX &&
      kotlin.math.abs(previous.bounds.centerY - next.bounds.centerY) <= screen.height * 0.035
  }

  /** Explicit upper ad badge + lower redirect CTA on one full-screen splash. */
  fun hasRedirectSplashComposition(
    texts: List<Text>,
    skip: AdGeometryRules.Box,
    screen: AdGeometryRules.Box,
  ): Boolean = hasSplashComposition(texts, skip, screen, motionOnly = false)

  fun hasMotionRedirectSplashComposition(
    texts: List<Text>,
    skip: AdGeometryRules.Box,
    screen: AdGeometryRules.Box,
  ): Boolean = hasSplashComposition(texts, skip, screen, motionOnly = true)

  private fun hasSplashComposition(
    texts: List<Text>,
    skip: AdGeometryRules.Box,
    screen: AdGeometryRules.Box,
    motionOnly: Boolean,
  ): Boolean {
    if (screen.width <= 0 || screen.height <= 0 || !contains(screen, skip) ||
      skip.width <= 0 || skip.height <= 0 ||
      skip.width > screen.width * 0.34 || skip.height > screen.height * 0.12 ||
      skip.centerX < screen.left + screen.width * 0.65 ||
      skip.top > screen.top + screen.height * 0.22
    ) return false
    val local = texts.filter { contains(screen, it.bounds) }
    val badge = local.any { text ->
      AdSemanticRules.normalize(text.value) in badges &&
        text.bounds.width <= screen.width * 0.20 &&
        text.bounds.height <= screen.height * 0.06 &&
        text.bounds.centerY <= screen.top + screen.height * 0.30 &&
        (text.bounds.centerX <= screen.left + screen.width * 0.40 ||
          (text.bounds.centerX >= screen.left + screen.width * 0.50 &&
            text.bounds.right <= skip.left + screen.width * 0.02 &&
            kotlin.math.abs(text.bounds.centerY - skip.centerY) <= screen.height * 0.06))
    }
    val redirect = local.any { text ->
      AdSemanticRules.normalize(text.value).trimEnd('›', '〉', '→', '❯') in redirectActions &&
        text.bounds.centerY >= screen.top + screen.height * 0.50 &&
        text.bounds.centerY <= screen.top + screen.height * 0.88
    }
    // A motion instruction alone also occurs in normal product tutorials.
    // Require the separate lower third-party redirect disclosure as well.
    val motionAction = local.any { text ->
      AdSemanticRules.normalize(text.value).trimEnd('›', '〉', '→', '❯') in motionActions &&
        text.bounds.centerY >= screen.top + screen.height * 0.60 &&
        text.bounds.centerY <= screen.top + screen.height * 0.92
    }
    val motionRedirect = local.any { text ->
      AdSemanticRules.normalize(text.value) in motionRedirectHints &&
        text.bounds.centerY >= screen.top + screen.height * 0.82 &&
        text.bounds.centerY <= screen.top + screen.height * 0.97
    }
    return badge && ((motionAction && motionRedirect) || (!motionOnly && redirect))
  }

  /** A specific China Mobile traffic-card splash whose artwork may be a non-image view. */
  fun hasMobileTrafficCardSplashComposition(
    packageName: String,
    texts: List<Text>,
    skip: AdGeometryRules.Box,
    screen: AdGeometryRules.Box,
  ): Boolean {
    if (packageName != "com.greenpoint.android.mc10086.activity" ||
      screen.width <= 0 || screen.height <= 0 || !contains(screen, skip) ||
      skip.width > screen.width * 0.34 || skip.height > screen.height * 0.12 ||
      skip.centerX < screen.left + screen.width * 0.65 ||
      skip.top > screen.top + screen.height * 0.22
    ) return false

    val local = texts.filter { contains(screen, it.bounds) }
    val offer = AdSemanticRules.normalize(local
      .sortedWith(compareBy<Text> { it.bounds.centerY }.thenBy { it.bounds.left })
      .joinToString("") { it.value })
    val exactTrafficOffer = listOf("55gb", "大流量卡", "39元", "40gb", "云盘").all(offer::contains)
    val redirect = local.any { text ->
      text.bounds.centerY >= screen.top + screen.height * 0.50 &&
        text.bounds.centerY <= screen.top + screen.height * 0.90 &&
        redirectActions.any { AdSemanticRules.normalize(text.value).contains(it) }
    }
    return exactTrafficOffer && redirect
  }

  private fun contains(outer: AdGeometryRules.Box, inner: AdGeometryRules.Box): Boolean =
    inner.width > 0 && inner.height > 0 && inner.left >= outer.left && inner.top >= outer.top &&
      inner.right <= outer.right && inner.bottom <= outer.bottom
}

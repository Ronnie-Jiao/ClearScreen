package com.clearscreen.prototype.backend

/** Text/geometry evidence only. Callers still validate a fresh window and an independent target. */
internal object AdCreativeRules {
  data class Text(val value: String, val bounds: AdGeometryRules.Box)

  data class OcrFloatingCloseEvidence(
    val creative: AdGeometryRules.Box,
    val semanticStamp: Int,
  )

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
      skip.width > container.width * 0.30 || skip.height > container.height * 0.10
    ) return false
    val largeCreative = creatives.any { creative ->
      contains(container, creative) && creative.width >= container.width * 0.90 &&
        creative.height >= container.height * 0.70
    }
    val redirectHint = texts.any { text ->
      contains(container, text.bounds) &&
        AdSemanticRules.normalize(text.value) in setOf(
          "摇一摇跳转至第三方应用或详情页",
          "点击跳转详情页或第三方应用",
        )
    }
    return largeCreative && redirectHint
  }

  /** Caiyun's inline swipe-to-redirect video card; callers still require its real close node and OCR X. */
  fun hasCaiyunSwipeRedirectVideoCard(
    packageName: String,
    texts: List<Text>,
    close: AdGeometryRules.Box,
    screen: AdGeometryRules.Box,
  ): Boolean {
    val width = screen.width.coerceAtLeast(1)
    val height = screen.height.coerceAtLeast(1)
    val screenArea = width.toLong() * height
    val closeArea = close.width.toLong().coerceAtLeast(0L) * close.height.toLong().coerceAtLeast(0L)
    if (packageName != "com.nowcasting.activity" ||
      screen.width <= 0 || screen.height <= 0 || !contains(screen, close) ||
      close.width > width * 0.13f || close.height > height * 0.09f ||
      closeArea.toDouble() / screenArea > 0.015 ||
      close.centerX !in (screen.left + width * 0.78f).toInt()..(screen.left + width * 0.97f).toInt() ||
      close.centerY !in (screen.top + height * 0.20f).toInt()..(screen.top + height * 0.39f).toInt()
    ) return false

    val local = texts.filter { contains(screen, it.bounds) }
    val adBadge = local.firstOrNull { text ->
      AdSemanticRules.normalize(text.value) == "广告" &&
        text.bounds.centerX in (screen.left + width * 0.20f).toInt()..(screen.left + width * 0.58f).toInt() &&
        text.bounds.centerY in (screen.top + height * 0.43f).toInt()..(screen.top + height * 0.58f).toInt() &&
        text.bounds.width <= width * 0.20f && text.bounds.height <= height * 0.07f
    } ?: return false
    val caption = local.firstOrNull { text ->
      val value = AdSemanticRules.normalize(text.value)
      (value.contains("滴滴") || value.contains("该省")) &&
        text.bounds.centerX in (screen.left + width * 0.04f).toInt()..(screen.left + width * 0.96f).toInt() &&
        text.bounds.centerY in (screen.top + height * 0.22f).toInt()..(screen.top + height * 0.39f).toInt() &&
        text.bounds.height <= height * 0.10f
    } ?: return false
    val taxiAction = local.firstOrNull { text ->
      AdSemanticRules.normalize(text.value).contains("打车立省") &&
        text.bounds.centerX in (screen.left + width * 0.72f).toInt()..(screen.left + width * 0.99f).toInt() &&
        text.bounds.centerY in (screen.top + height * 0.43f).toInt()..(screen.top + height * 0.59f).toInt()
    } ?: return false
    val footer = local.filter { text ->
      text.bounds.centerX in (screen.left + width * 0.08f).toInt()..(screen.left + width * 0.96f).toInt() &&
        text.bounds.centerY in (screen.top + height * 0.43f).toInt()..(screen.top + height * 0.59f).toInt()
    }
    val footerCopy = AdSemanticRules.normalize(footer
      .sortedWith(compareBy<Text> { it.bounds.centerY }.thenBy { it.bounds.left })
      .joinToString("") { it.value })
    val swipeDisclosure = footerCopy.contains("滑动跳转") && footerCopy.contains("详情页") &&
      footerCopy.contains("第三方应用")
    val footerAligned = kotlin.math.abs(adBadge.bounds.centerY - taxiAction.bounds.centerY) <= height * 0.06f &&
      kotlin.math.abs(close.centerY - caption.bounds.centerY) <= height * 0.10f &&
      close.centerY + height * 0.10f < taxiAction.bounds.centerY
    return swipeDisclosure && footerAligned
  }

  private val version = Regex("""版本(?:号)?\s*[:：]?\s*\d+(?:\.\d+){1,3}""")
  private val downloadActions = listOf("点击立即下载", "立即下载", "抢先下载", "安全下载")
  private val disclosureLinks = listOf("功能", "权限", "隐私", "备案")
  private val redirectActions = setOf(
    "点击跳转详情页面", "点击跳转详情页", "点击跳转第三方应用", "点击跳转第三方页面",
  )
  private val motionActions = setOf(
    "扭动手机开启详情", "扭一扭开启详情", "摇一摇开启详情", "翻转手机",
    "转动手机或点击图标", "转动手机点击图标", "转动手机", "扭动手机或点击图标",
  )
  private val motionRedirectHints = setOf(
    "互动跳转详情页或第三方应用", "互动跳转至详情页或第三方应用",
    "跳转详情页或第三方应用",
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

  /** Identifies the observed OPPEIN reward modal from its same-screen copy. */
  fun hasOppeinRecommendedPopupCopy(
    texts: List<Text>,
    screen: AdGeometryRules.Box,
  ): Boolean = oppeinPopupCopy(texts, screen) != null

  /** Only the labeled refusal button inside that popup can use this exception. */
  fun hasOppeinRecommendedPopupDismiss(
    label: String,
    texts: List<Text>,
    button: AdGeometryRules.Box,
    screen: AdGeometryRules.Box,
  ): Boolean {
    if (AdSemanticRules.normalize(label) != "残忍离开" || screen.width <= 0 || screen.height <= 0 ||
      !contains(screen, button)
    ) return false
    val width = screen.width
    val height = screen.height
    val buttonArea = button.width.toLong().coerceAtLeast(0L) * button.height.toLong().coerceAtLeast(0L)
    if (button.width > width * 0.42f || button.height > height * 0.10f ||
      buttonArea.toDouble() / (width.toLong() * height) > 0.045 ||
      button.centerX !in (screen.left + width * 0.32f).toInt()..(screen.left + width * 0.68f).toInt()
    ) return false

    val copy = oppeinPopupCopy(texts, screen) ?: return false
    val action = copy[2]
    val disclosure = copy[3]
    return button.centerY > action.bounds.centerY + height * 0.01f &&
      button.centerY < disclosure.bounds.centerY &&
      kotlin.math.abs(button.centerX - action.bounds.centerX) <= width * 0.14f
  }

  private fun oppeinPopupCopy(
    texts: List<Text>,
    screen: AdGeometryRules.Box,
  ): List<Text>? {
    if (screen.width <= 0 || screen.height <= 0) return null
    val width = screen.width
    val height = screen.height
    val centered = texts.filter { text ->
      contains(screen, text.bounds) &&
        text.bounds.centerX in (screen.left + width * 0.18f).toInt()..(screen.left + width * 0.82f).toInt()
    }
    fun match(marker: String): Text? = centered.firstOrNull {
      AdSemanticRules.normalize(it.value).contains(marker)
    }
    val title = match("今日专属推荐") ?: return null
    val brand = match("欧派整装官方") ?: return null
    val action = match("去微信看看") ?: return null
    val disclosure = match("实际内容以广告内容为主") ?: return null
    val rows = listOf(title, brand, action, disclosure)
    if (rows.any { it.bounds.height > height * 0.12f } ||
      rows.any { it.bounds.centerY !in (screen.top + height * 0.10f).toInt()..(screen.top + height * 0.92f).toInt() } ||
      rows.zipWithNext().any { (upper, lower) -> upper.bounds.centerY >= lower.bounds.centerY } ||
      rows.any { kotlin.math.abs(it.bounds.centerX - (screen.left + width / 2)) > width * 0.24f } ||
      disclosure.bounds.centerY - title.bounds.centerY > height * 0.72f
    ) return null
    return rows
  }

  /**
   * The observed KPL finals poster has no readable ad badge and places its X
   * above the central campaign copy. Require the full campaign signature and
   * keep the X tied geometrically to that same OCR text cluster.
   */
  fun hasKplFinalsPosterClose(
    texts: List<Text>,
    close: AdGeometryRules.Box,
    screen: AdGeometryRules.Box,
  ): Boolean {
    val width = screen.width.coerceAtLeast(1)
    val height = screen.height.coerceAtLeast(1)
    val screenArea = width.toLong() * height
    val closeArea = close.width.toLong().coerceAtLeast(0L) * close.height.toLong().coerceAtLeast(0L)
    if (screen.width <= 0 || screen.height <= 0 || !contains(screen, close) ||
      close.width > width * 0.14f || close.height > height * 0.09f ||
      closeArea.toDouble() / screenArea > 0.015 ||
      close.centerX !in (screen.left + width * 0.70f).toInt()..(screen.left + width * 0.98f).toInt() ||
      close.centerY !in (screen.top + height * 0.16f).toInt()..(screen.top + height * 0.40f).toInt()
    ) return false

    val centralCopy = texts.filter { text ->
      contains(screen, text.bounds) &&
        text.bounds.centerX in (screen.left + width * 0.10f).toInt()..(screen.left + width * 0.90f).toInt() &&
        text.bounds.centerY in (screen.top + height * 0.28f).toInt()..(screen.top + height * 0.79f).toInt()
    }
    val normalized = centralCopy.map { AdSemanticRules.normalize(it.value) }
    val requiredCopy = listOf("kpl", "年度总决赛", "纪念皮肤", "韩信", "限时秒杀", "立即前往")
    if (!requiredCopy.all { marker -> normalized.any { it.contains(marker) } } ||
      normalized.none { it.contains("2026") || it.contains("十周年") }
    ) return false

    val signatureMarkers = requiredCopy + listOf("2026", "十周年")
    val signatureLines = centralCopy.filter { text ->
      val value = AdSemanticRules.normalize(text.value)
      signatureMarkers.any(value::contains)
    }
    if (signatureLines.isEmpty()) return false
    val cluster = AdGeometryRules.Box(
      left = signatureLines.minOf { it.bounds.left },
      top = signatureLines.minOf { it.bounds.top },
      right = signatureLines.maxOf { it.bounds.right },
      bottom = signatureLines.maxOf { it.bounds.bottom },
    )
    return cluster.width in (width * 0.18f).toInt()..(width * 0.88f).toInt() &&
      cluster.height in (height * 0.025f).toInt()..(height * 0.40f).toInt() &&
      cluster.centerX in (screen.left + width * 0.34f).toInt()..(screen.left + width * 0.66f).toInt() &&
      cluster.centerY in (screen.top + height * 0.36f).toInt()..(screen.top + height * 0.76f).toInt() &&
      close.centerX >= cluster.centerX + width * 0.14f &&
      close.centerY < cluster.top + height * 0.08f &&
      close.centerY >= cluster.top - height * 0.34f
  }

  /** Tencent Video's lower-right "免 / 免费看" floating promo with its own X. */
  fun hasTencentFreeViewingPromoClose(
    packageName: String,
    texts: List<Text>,
    close: AdGeometryRules.Box,
    screen: AdGeometryRules.Box,
  ): Boolean {
    if (packageName != "com.tencent.qqlive" || screen.width <= 0 || screen.height <= 0 ||
      !contains(screen, close)
    ) return false
    val width = screen.width
    val height = screen.height
    val screenArea = width.toLong() * height
    val closeArea = close.width.toLong().coerceAtLeast(0L) * close.height.toLong().coerceAtLeast(0L)
    if (close.width > width * 0.12f || close.height > height * 0.08f ||
      closeArea.toDouble() / screenArea > 0.012 ||
      close.centerX < screen.left + width * 0.82f ||
      close.centerY !in (screen.top + height * 0.70f).toInt()..(screen.top + height * 0.94f).toInt()
    ) return false

    val local = texts.filter { text ->
      contains(screen, text.bounds) &&
        text.bounds.centerX >= screen.left + width * 0.70f &&
        text.bounds.centerY in (screen.top + height * 0.70f).toInt()..(screen.top + height * 0.94f).toInt()
    }
    val icon = local.firstOrNull { text ->
      AdSemanticRules.normalize(text.value) == "免" &&
        text.bounds.width <= width * 0.22f && text.bounds.height <= height * 0.14f
    } ?: return false
    val action = local.firstOrNull { text ->
      AdSemanticRules.normalize(text.value).contains("免费看") &&
        text.bounds.width <= width * 0.30f && text.bounds.height <= height * 0.09f
    } ?: return false
    return kotlin.math.abs(icon.bounds.centerX - action.bounds.centerX) <= width * 0.14f &&
      action.bounds.centerY >= icon.bounds.centerY - height * 0.04f &&
      action.bounds.centerY <= icon.bounds.centerY + height * 0.14f &&
      close.centerX >= action.bounds.centerX + width * 0.035f &&
      close.centerY >= icon.bounds.top - height * 0.10f &&
      close.centerY < action.bounds.centerY
  }

  /** Same-window ad disclosure and redirect semantics; neither has a required position. */
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
      skip.width > screen.width * 0.34 || skip.height > screen.height * 0.12
    ) return false
    val local = texts.filter { contains(screen, it.bounds) }
    val badge = local.any { text ->
      AdControlEvidenceRules.isAdBadge(text.value) &&
        text.bounds.width <= screen.width * 0.20 &&
        text.bounds.height <= screen.height * 0.06
    }
    val redirect = local.any { text ->
      AdControlEvidenceRules.hasRedirectDisclosure(text.value)
    }
    // A motion instruction alone also occurs in normal product tutorials.
    // Require the separate third-party redirect disclosure as well.
    val motionAction = local.any { text ->
      AdSemanticRules.normalize(text.value).trimEnd('›', '〉', '→', '❯') in motionActions
    }
    val motionRedirect = local.any { text ->
      AdSemanticRules.normalize(text.value) in motionRedirectHints ||
        AdControlEvidenceRules.hasRedirectDisclosure(text.value)
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
      skip.width > screen.width * 0.34 || skip.height > screen.height * 0.12
    ) return false

    val local = texts.filter { contains(screen, it.bounds) }
    val offer = AdSemanticRules.normalize(local
      .sortedWith(compareBy<Text> { it.bounds.centerY }.thenBy { it.bounds.left })
      .joinToString("") { it.value })
    val exactTrafficOffer = listOf("55gb", "大流量卡", "39元", "40gb", "云盘").all(offer::contains)
    val redirect = local.any { text ->
      AdControlEvidenceRules.hasRedirectDisclosure(text.value)
    }
    return exactTrafficOffer && redirect
  }

  /** A specific China Mobile broadband splash; callers still require a real skip control or fresh OCR target. */
  fun hasMobileBroadbandSplashComposition(
    packageName: String,
    texts: List<Text>,
    skip: AdGeometryRules.Box,
    screen: AdGeometryRules.Box,
  ): Boolean {
    if (packageName != "com.greenpoint.android.mc10086.activity" ||
      screen.width <= 0 || screen.height <= 0 || !contains(screen, skip) ||
      skip.width > screen.width * 0.34 || skip.height > screen.height * 0.12
    ) return false

    val local = texts.filter { contains(screen, it.bounds) }
    val badge = local.any { text ->
      AdSemanticRules.normalize(text.value) == "广告" &&
        text.bounds.width <= screen.width * 0.20 &&
        text.bounds.height <= screen.height * 0.06
    }
    if (!badge) return false

    val offer = AdSemanticRules.normalize(local
      .sortedWith(compareBy<Text> { it.bounds.centerY }.thenBy { it.bounds.left })
      .joinToString("") { it.value })
    val exactBroadbandOffer = offer.contains("千兆宽带") &&
      offer.contains("秋季特惠") && offer.contains("1元天起") && offer.contains("全家畅享")
    val redirect = local.any { text ->
      AdControlEvidenceRules.hasRedirectDisclosure(text.value)
    }
    return exactBroadbandOffer && redirect
  }

  /** Known host branding supports a ready skip; the control and copy may move. */
  fun hasChinaTelecomBrandedSplash(
    packageName: String,
    skipText: String,
    skip: AdGeometryRules.Box,
    screen: AdGeometryRules.Box,
    texts: List<Text>,
  ): Boolean {
    val width = screen.width.coerceAtLeast(1)
    val height = screen.height.coerceAtLeast(1)
    val skipArea = skip.width.toLong().coerceAtLeast(0L) * skip.height.toLong().coerceAtLeast(0L)
    val screenArea = width.toLong() * height
    if (packageName != "com.ct.client" ||
      AdSemanticRules.selectSkipLabel(listOf(skipText)) == null ||
      !contains(screen, skip) || skip.width > width * 0.28f || skip.height > height * 0.10f ||
      skipArea.toDouble() / screenArea > 0.03
    ) return false

    val local = texts.filter { contains(screen, it.bounds) }
    val telecomLogo = local.any { text ->
      AdSemanticRules.normalize(text.value).contains("中国电信") &&
        text.bounds.width <= width * 0.55f && text.bounds.height <= height * 0.08f
    }
    if (!telecomLogo) return false

    if (local.any { AdControlEvidenceRules.hasRedirectDisclosure(it.value) }) return true
    val copy = local.joinToString("") { AdSemanticRules.normalize(it.value) }
    return copy.contains("用户") && copy.contains("至上") &&
      copy.contains("用心") && copy.contains("服务")
  }

  /** China Mobile's centered plan-upgrade popup with its own lower-center close button. */
  fun hasMobilePlanUpgradePopup(
    packageName: String,
    texts: List<Text>,
    close: AdGeometryRules.Box,
    screen: AdGeometryRules.Box,
  ): Boolean {
    val width = screen.width.coerceAtLeast(1)
    val height = screen.height.coerceAtLeast(1)
    val screenArea = width.toLong() * height
    val closeArea = close.width.toLong().coerceAtLeast(0L) * close.height.toLong().coerceAtLeast(0L)
    if (packageName != "com.greenpoint.android.mc10086.activity" ||
      screen.width <= 0 || screen.height <= 0 || !contains(screen, close) ||
      close.width > width * 0.12f || close.height > height * 0.09f ||
      closeArea.toDouble() / screenArea > 0.015 ||
      close.centerX !in (screen.left + width * 0.44f).toInt()..(screen.left + width * 0.56f).toInt() ||
      close.centerY !in (screen.top + height * 0.70f).toInt()..(screen.top + height * 0.84f).toInt()
    ) return false

    val local = texts.filter { contains(screen, it.bounds) }
    val adBadge = local.firstOrNull { text ->
      AdSemanticRules.normalize(text.value) == "广告" &&
        text.bounds.centerX in (screen.left + width * 0.78f).toInt()..(screen.left + width * 0.96f).toInt() &&
        text.bounds.centerY in (screen.top + height * 0.22f).toInt()..(screen.top + height * 0.38f).toInt() &&
        text.bounds.width <= width * 0.14f && text.bounds.height <= height * 0.06f
    } ?: return false
    val title = local.firstOrNull { text ->
      val value = AdSemanticRules.normalize(text.value)
      value.contains("套餐升级") && value.contains("享") &&
        text.bounds.centerX in (screen.left + width * 0.28f).toInt()..(screen.left + width * 0.72f).toInt() &&
        text.bounds.centerY in (screen.top + height * 0.22f).toInt()..(screen.top + height * 0.40f).toInt()
    } ?: return false
    val offer = local.firstOrNull { text ->
      val value = AdSemanticRules.normalize(text.value)
      value.contains("30gb") && value.contains("200分钟") &&
        text.bounds.centerX in (screen.left + width * 0.20f).toInt()..(screen.left + width * 0.80f).toInt() &&
        text.bounds.centerY in (screen.top + height * 0.29f).toInt()..(screen.top + height * 0.52f).toInt()
    } ?: return false
    val price = local.firstOrNull { text ->
      AdSemanticRules.normalize(text.value).contains("19元月") &&
        text.bounds.centerX in (screen.left + width * 0.30f).toInt()..(screen.left + width * 0.70f).toInt() &&
        text.bounds.centerY in (screen.top + height * 0.55f).toInt()..(screen.top + height * 0.70f).toInt()
    } ?: return false

    val badgeAligned = kotlin.math.abs(adBadge.bounds.centerY - title.bounds.centerY) <= height * 0.09f &&
      adBadge.bounds.centerX >= title.bounds.centerX + width * 0.18f
    val closeBelowPrice = close.top >= price.bounds.bottom &&
      close.top - price.bounds.bottom in (height * 0.03f).toInt()..(height * 0.20f).toInt() &&
      kotlin.math.abs(close.centerX - price.bounds.centerX) <= width * 0.14f &&
      close.centerY >= price.bounds.centerY + height * 0.08f
    return badgeAligned && closeBelowPrice &&
      offer.bounds.centerY > title.bounds.centerY && price.bounds.centerY > offer.bounds.centerY
  }

  /** China Mobile home floating gift ads: known offer copy and an X at that creative's upper-right edge. */
  fun hasMobileGiftPromoFloatingClose(
    packageName: String,
    texts: List<Text>,
    close: AdGeometryRules.Box,
    screen: AdGeometryRules.Box,
  ): Boolean {
    if (packageName != "com.greenpoint.android.mc10086.activity" ||
      screen.width <= 0 || screen.height <= 0 || !contains(screen, close) ||
      close.width > screen.width * 0.12 || close.height > screen.height * 0.08 ||
      close.centerX < screen.left + screen.width * 0.82f
    ) return false

    val local = texts.filter { contains(screen, it.bounds) }
    val offerPairs = listOf(
      local.filter { AdSemanticRules.normalize(it.value).contains("20gb") } to
        local.filter { AdSemanticRules.normalize(it.value).contains("特惠包") },
      local.filter { AdSemanticRules.normalize(it.value).contains("59元") } to
        local.filter { AdSemanticRules.normalize(it.value).contains("全家享") },
    )
    val offers = mutableListOf<AdGeometryRules.Box>()
    val width = screen.width.coerceAtLeast(1)
    val height = screen.height.coerceAtLeast(1)
    for ((firstTerms, secondTerms) in offerPairs) {
      for (first in firstTerms) for (second in secondTerms) {
        if (kotlin.math.abs(first.bounds.centerX - second.bounds.centerX) > width * 0.12f ||
          kotlin.math.abs(first.bounds.centerY - second.bounds.centerY) > height * 0.10f
        ) continue
        offers += AdGeometryRules.Box(
          minOf(first.bounds.left, second.bounds.left),
          minOf(first.bounds.top, second.bounds.top),
          maxOf(first.bounds.right, second.bounds.right),
          maxOf(first.bounds.bottom, second.bounds.bottom),
        )
      }
    }
    if (offers.isEmpty()) return false

    for (offer in offers.distinct()) {
      if (close.centerX < offer.centerX || close.centerY >= offer.centerY ||
        close.centerY < offer.top - height * 0.12f
      ) continue

      val creative = AdGeometryRules.Box(
        maxOf(screen.left, offer.left - (width * 0.05f).toInt()),
        maxOf(screen.top, offer.top - (height * 0.06f).toInt()),
        minOf(screen.right, offer.right + (width * 0.04f).toInt()),
        minOf(screen.bottom, offer.bottom + (height * 0.03f).toInt()),
      )
      if (AdGeometryRules.isOcrFloatingCreative(creative, screen) &&
        AdGeometryRules.isCompactFloatingCreativeClose(close, creative, screen)
      ) return true
    }
    return false
  }

  /**
   * Finds compact OCR-backed ad creatives associated with a small X glyph.
   * Generic copy must either disclose an ad or combine offer and action intent;
   * the known China Mobile gift tile has an additional local offer-pattern rule.
   */
  fun findOcrFloatingCloseEvidence(
    packageName: String,
    texts: List<Text>,
    close: AdGeometryRules.Box,
    screen: AdGeometryRules.Box,
  ): List<OcrFloatingCloseEvidence> {
    val width = screen.width.coerceAtLeast(1)
    val height = screen.height.coerceAtLeast(1)
    if (!contains(screen, close) || close.width <= 0 || close.height <= 0 ||
      close.width > width * 0.12f || close.height > height * 0.08f ||
      (close.width.toLong() * close.height.toLong()).toDouble() /
        (width.toLong() * height.toLong()) > 0.012
    ) return emptyList()

    val local = texts.filter { text ->
      contains(screen, text.bounds) &&
        kotlin.math.abs(text.bounds.centerX - close.centerX) <= width * 0.46f &&
        kotlin.math.abs(text.bounds.centerY - close.centerY) <= height * 0.36f
    }
    if (local.isEmpty()) return emptyList()

    return local.mapNotNull { seed ->
      val group = local.filter { text ->
        kotlin.math.abs(text.bounds.centerX - seed.bounds.centerX) <= width * 0.32f &&
          kotlin.math.abs(text.bounds.centerY - seed.bounds.centerY) <= height * 0.22f
      }.distinctBy { it.value to it.bounds }
      if (group.none { text ->
          kotlin.math.abs(text.bounds.centerX - close.centerX) <= width * 0.22f &&
            kotlin.math.abs(text.bounds.centerY - close.centerY) <= height * 0.22f
        }
      ) return@mapNotNull null
      val combined = AdSemanticRules.normalize(group.joinToString(" ") { it.value })
      val hasAdDisclosure = AdSemanticRules.hasStrongAdSignal(combined)
      val hasCommercialIntent = AdSemanticRules.hasCommercialOfferIntent(combined)
      val isKnownMobileGiftPromo = hasMobileGiftPromoFloatingClose(packageName, group, close, screen)
      if (!hasAdDisclosure && !hasCommercialIntent && !isKnownMobileGiftPromo) return@mapNotNull null

      val contentLeft = minOf(close.left, group.minOf { it.bounds.left })
      val contentTop = minOf(close.top, group.minOf { it.bounds.top })
      val contentRight = maxOf(close.right, group.maxOf { it.bounds.right })
      val contentBottom = maxOf(close.bottom, group.maxOf { it.bounds.bottom })
      val padX = (width * 0.025f).toInt()
      val padY = (height * 0.025f).toInt()
      val creative = AdGeometryRules.Box(
        maxOf(screen.left, contentLeft - padX),
        maxOf(screen.top, contentTop - padY),
        minOf(screen.right, contentRight + padX),
        minOf(screen.bottom, contentBottom + padY),
      )
      if (!AdGeometryRules.isOcrFloatingCreative(creative, screen) ||
        !AdGeometryRules.isCompactFloatingCreativeClose(close, creative, screen)
      ) return@mapNotNull null

      val semanticStamp = (if (hasAdDisclosure) 1 else 0) or
        (if (hasCommercialIntent) 2 else 0) or (if (isKnownMobileGiftPromo) 4 else 0)
      OcrFloatingCloseEvidence(creative, semanticStamp)
    }.distinctBy { it.creative to it.semanticStamp }
  }

  fun hasOcrFloatingPromoEvidence(packageName: String, texts: List<Text>): Boolean {
    val combined = AdSemanticRules.normalize(texts.joinToString(" ") { it.value })
    if (AdSemanticRules.hasStrongAdSignal(combined) || AdSemanticRules.hasCommercialOfferIntent(combined)) {
      return true
    }
    if (packageName != "com.greenpoint.android.mc10086.activity") return false
    return (combined.contains("20gb") && combined.contains("特惠包")) ||
      (combined.contains("59元") && combined.contains("全家享"))
  }

  private fun contains(outer: AdGeometryRules.Box, inner: AdGeometryRules.Box): Boolean =
    inner.width > 0 && inner.height > 0 && inner.left >= outer.left && inner.top >= outer.top &&
      inner.right <= outer.right && inner.bottom <= outer.bottom
}

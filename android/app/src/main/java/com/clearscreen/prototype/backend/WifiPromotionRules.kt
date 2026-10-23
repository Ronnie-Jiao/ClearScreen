package com.clearscreen.prototype.backend

/** Exact observed SDK promotions; host and local composition both remain restricted. */
internal object WifiPromotionRules {
  const val PACKAGE_NAME = "com.snda.wifilocating"
  const val CALENDAR_PACKAGE_NAME = "cn.etouch.ecalendar"

  fun isSupportedHost(packageName: String): Boolean =
    packageName == PACKAGE_NAME || packageName == CALENDAR_PACKAGE_NAME

  enum class Kind(val ruleId: String) {
    MEMBERSHIP_OFFER("builtin.popup.close.wifi-membership-offer"),
    EXTERNAL_FLOATING_BANNER("builtin.popup.close.sdk-external-floating-banner"),
    FLOATING_GAME("builtin.popup.close.wifi-floating-lingzhu"),
    VIDEO_POSTER("builtin.popup.close.wifi-video-poster"),
    CALENDAR_DOWNLOAD_CARD("builtin.feed.close.calendar-download-footer"),
  }

  /**
   * The caller supplies one stable, non-scrolling subtree containing a real X
   * and its own visual creative. Screen-wide/background copy is not evidence.
   */
  fun classify(
    packageName: String,
    screen: AdGeometryRules.Box,
    region: AdGeometryRules.Box,
    creative: AdGeometryRules.Box,
    close: AdGeometryRules.Box,
    texts: List<AdCreativeRules.Text>,
  ): Kind? {
    if (!isSupportedHost(packageName) || screen.width <= 0 || screen.height <= 0 ||
      !contains(screen, region) || !contains(region, creative) || !contains(region, close) ||
      !AdGeometryRules.isSmallModalClose(close, screen) ||
      close.width > screen.width * 0.12 || close.height > screen.height * 0.08 ||
      creative.width < screen.width * 0.13 || creative.height < screen.height * 0.06
    ) return null

    val localTexts = texts.filter { contains(region, it.bounds) }
    val creativeTexts = localTexts.filter { contains(creative, it.bounds) }
    val body = normalizedText(creativeTexts)
    val width = screen.width
    val height = screen.height
    val closeAtRight = close.centerX >= screen.left + width * 0.70

    // This SDK puts the X above its video, with a separate third-party CTA
    // below. All three pieces must belong to the same stable overlay subtree.
    if (packageName == PACKAGE_NAME && AdGeometryRules.isDetachedPosterClose(close, creative, screen)) {
      val badge = creativeTexts.any {
        AdSemanticRules.normalize(it.value) == "广告" &&
          it.bounds.width <= width * 0.16 && it.bounds.height <= height * 0.05 &&
          it.bounds.centerX >= creative.left + creative.width * 0.65 &&
          it.bounds.centerY <= creative.top + creative.height * 0.12
      }
      val footer = normalizedText(localTexts.filter {
        it.bounds.centerX in creative.left..creative.right &&
          it.bounds.centerY >= creative.bottom &&
          it.bounds.centerY <= creative.bottom + height * 0.11
      })
      if (badge && listOf("点击图片", "下载或跳转第三方应用", "立即打开").all(footer::contains)) {
        return Kind.VIDEO_POSTER
      }
    }

    // The close is well above this campaign's poster. Its dedicated subtree,
    // exact congratulatory title and price/claim composition are all required.
    val offerTitle = localTexts.any {
      AdSemanticRules.normalize(it.value) == "恭喜红包到账" &&
        it.bounds.centerY <= creative.top &&
        it.bounds.centerY >= creative.top - height * 0.12
    }
    val memberOffer = creative.width in (width * 0.48).toInt()..(width * 0.90).toInt() &&
      creative.height in (height * 0.24).toInt()..(height * 0.65).toInt() &&
      creative.centerX in (screen.left + width * 0.38).toInt()..(screen.left + width * 0.62).toInt() &&
      closeAtRight &&
      close.centerX in (creative.right - width * 0.12).toInt()..(creative.right + width * 0.12).toInt() &&
      close.centerY in (creative.top - height * 0.22).toInt()..(creative.top + height * 0.03).toInt() &&
      offerTitle && body.contains("svip月卡专属红包") && body.contains("领取并使用") &&
      Regex("优惠后[0-9]+元").containsMatchIn(body) &&
      Regex("原价[0-9]+元").containsMatchIn(body)
    if (packageName == PACKAGE_NAME && memberOffer) return Kind.MEMBERSHIP_OFFER

    val externalBanner = creative.width >= width * 0.45 && creative.height <= height * 0.20 &&
      creative.width.toDouble() / creative.height >= 2.4 && closeAtRight &&
      AdGeometryRules.isCompactFloatingCreativeClose(close, creative, screen) &&
      body.contains("惊喜等你拿") &&
      (body.contains("点击到落地页或第三方app") || body.contains("点击到落地页或第三方应用")) &&
      texts.any {
        AdSemanticRules.normalize(it.value) == "关闭悬浮球可继续浏览应用" &&
          contains(screen, it.bounds) &&
          it.bounds.centerX in creative.left..creative.right &&
          it.bounds.width <= creative.width * 1.1 &&
          it.bounds.centerY >= creative.bottom &&
          it.bounds.centerY <= creative.bottom + height * 0.07
      }
    if (externalBanner) return Kind.EXTERNAL_FLOATING_BANNER

    if (packageName == CALENDAR_PACKAGE_NAME) {
      // This card places the real X in its download/ad footer, below the video.
      // Do not treat a page-level reminder, video control or membership CTA as its close.
      val footerTexts = localTexts.filter { it.bounds.centerY >= creative.bottom }
      val footer = normalizedText(footerTexts)
      val disclosure = normalizedText(creativeTexts.filter {
        it.bounds.centerY <= creative.top + creative.height * 0.20
      })
      val downloadFooter = region.width >= width * 0.65 && region.height <= height * 0.55 &&
        creative.width >= region.width * 0.75 && creative.height >= region.height * 0.45 &&
        close.centerX >= region.right - width * 0.14 &&
        close.centerY >= creative.bottom &&
        close.centerY <= creative.bottom + height * 0.06 &&
        close.height <= region.height * 0.20 &&
        footer.contains("为您推荐") && footer.contains("下载") && footer.contains("广告") &&
        !footer.contains("免广告") && !footer.contains("无广告") &&
        disclosure.contains("滴滴") &&
        disclosure.contains("滴滴出行北京网络平台技术有限公司") &&
        (disclosure.contains("隐私") || disclosure.contains("权限")) &&
        creativeTexts.any { Regex("[0-9]+\\.[0-9]+\\.[0-9]+").containsMatchIn(it.value) }
      return if (downloadFooter) Kind.CALENDAR_DOWNLOAD_CARD else null
    }

    // An explicit requested title in a tight edge-mounted creative/close pair,
    // not a normal game page, search result, list card, or unrelated page X.
    val floatingGame = region.width <= width * 0.40 && region.height <= height * 0.30 &&
      creative.width <= width * 0.36 && creative.height <= height * 0.18 &&
      creative.right >= screen.right - width * 0.04 &&
      creative.centerX >= screen.left + width * 0.70 &&
      creative.centerY in (screen.top + height * 0.45).toInt()..(screen.top + height * 0.88).toInt() &&
      closeAtRight &&
      close.centerX in (creative.right - width * 0.10).toInt()..(creative.right + width * 0.04).toInt() &&
      close.centerY in (creative.top - height * 0.06).toInt()..(creative.top + height * 0.04).toInt() &&
      creativeTexts.any {
        AdSemanticRules.normalize(it.value) == "玩转灵珠" &&
          it.bounds.centerY >= creative.top + creative.height * 0.50
      }
    return if (floatingGame) Kind.FLOATING_GAME else null
  }

  private fun normalizedText(texts: List<AdCreativeRules.Text>): String = texts
    .sortedWith(compareBy<AdCreativeRules.Text> { it.bounds.centerY }.thenBy { it.bounds.left })
    .joinToString("") { AdSemanticRules.normalize(it.value) }

  private fun contains(outer: AdGeometryRules.Box, inner: AdGeometryRules.Box): Boolean =
    inner.width > 0 && inner.height > 0 && outer.left <= inner.left && outer.top <= inner.top &&
      outer.right >= inner.right && outer.bottom >= inner.bottom
}

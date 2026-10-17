package com.clearscreen.prototype.backend

import org.junit.Assert.*
import org.junit.Test

class AdCreativeRulesTest {
  @Test fun caiyunInterstitialIdsAreAnExactPackageBoundAllowlist() {
    for (id in listOf("cll_close_3")) {
      assertTrue(AdCreativeRules.isCaiyunInterstitialCloseId(
        "com.nowcasting.activity", "com.nowcasting.activity:id/$id"))
      assertFalse(AdCreativeRules.isCaiyunInterstitialCloseId(
        "com.example.app", "com.nowcasting.activity:id/$id"))
    }
    for (id in listOf("close", "cll_close_click", "cll_close_1", "cll_close_30", "cll_ad_container")) {
      assertFalse(AdCreativeRules.isCaiyunInterstitialCloseId(
        "com.nowcasting.activity", "com.nowcasting.activity:id/$id"))
    }
  }

  @Test fun knownCaiyunAdClickThroughXIsDeniedEvenWithOtherwiseValidEvidence() {
    assertTrue(AdCreativeRules.isUnsafeCaiyunInterstitialClickId(
      "com.nowcasting.activity", "com.nowcasting.activity:id/cll_close_click"))
    assertTrue(AdCreativeRules.isCaiyunInterstitialCloseId(
      "com.nowcasting.activity", "com.nowcasting.activity:id/cll_close_3"))
    assertFalse(AdCreativeRules.isUnsafeCaiyunInterstitialClickId(
      "com.example.app", "com.nowcasting.activity:id/cll_close_click"))
    assertFalse(AdCreativeRules.isUnsafeCaiyunInterstitialClickId(
      "com.nowcasting.activity", "com.nowcasting.activity:id/cll_close_3"))
  }

  private val screen = AdGeometryRules.Box(0, 0, 1260, 2800)
  private val skip = AdGeometryRules.Box(990, 120, 1210, 220)
  private val badge = AdCreativeRules.Text("广告", AdGeometryRules.Box(110, 250, 215, 310))
  private val redirect = AdCreativeRules.Text("点击跳转详情页面 ›", AdGeometryRules.Box(390, 1990, 860, 2060))

  // Captured from the USB Xiaomi's live splash, not inferred from a screenshot alone.
  private val xiaomiScreen = AdGeometryRules.Box(0, 0, 1080, 2400)
  private val xiaomiSplash = AdGeometryRules.Box(0, 0, 1080, 2049)
  private val xiaomiSkip = AdGeometryRules.Box(877, 123, 1039, 190)
  private val xiaomiHint = AdCreativeRules.Text("摇一摇跳转至 第三方应用或详情页",
    AdGeometryRules.Box(391, 1433, 688, 1521))

  private fun hasXiaomiSplash(
    label: String = "跳过 5",
    packageName: String = "com.nowcasting.activity",
    containerId: String = "com.nowcasting.activity:id/splash_container",
    skipBox: AdGeometryRules.Box = xiaomiSkip,
    texts: List<AdCreativeRules.Text> = listOf(xiaomiHint),
    creatives: List<AdGeometryRules.Box> = listOf(xiaomiSplash),
  ): Boolean = AdCreativeRules.hasCaiyunRedirectSplash(
    packageName, containerId, xiaomiSplash, xiaomiScreen, label, skipBox, texts, creatives)

  @Test fun liveCaiyunCountdownUsesTheSameSplashRedirectSubtree() {
    for (label in listOf("跳过 5", "跳过 1", "跳过")) assertTrue(label, hasXiaomiSplash(label))
  }

  @Test fun ordinaryNavigationOrFutureSkipIsNeverAuthorizedBySplashGeometry() {
    for (label in listOf("立即进入", "反馈", "跳过引导", "5秒后可跳过", "Skip in 5")) {
      assertFalse(label, hasXiaomiSplash(label))
    }
    assertFalse(hasXiaomiSplash(packageName = "com.example.app"))
    assertFalse(hasXiaomiSplash(containerId = "com.nowcasting.activity:id/weather_card"))
  }

  @Test fun splashHintAndLargeCreativeAreBothRequired() {
    assertFalse(hasXiaomiSplash(texts = emptyList()))
    assertFalse(hasXiaomiSplash(texts = listOf(xiaomiHint.copy(value = "查看天气详情"))))
    assertFalse(hasXiaomiSplash(creatives = emptyList()))
    assertFalse(hasXiaomiSplash(creatives = listOf(AdGeometryRules.Box(0, 0, 500, 500))))
  }

  @Test fun splashCannotBorrowHintsOutsideItsContainerOrClickTheCreative() {
    assertFalse(hasXiaomiSplash(texts = listOf(xiaomiHint.copy(bounds =
      AdGeometryRules.Box(391, 2150, 688, 2240)))))
    assertFalse(hasXiaomiSplash(skipBox = xiaomiSplash))
    assertFalse(hasXiaomiSplash(skipBox = AdGeometryRules.Box(400, 123, 560, 190)))
  }

  // Manually transcribed from the supplied 19:01:52 / 19:01:56 screenshots.
  // These are text/geometry regressions, not claims of on-device OCR accuracy.
  private val downloadCard = listOf(
    "应用名称：滴滴", "版本号：8.0.9", "功能 | 权限 | 隐私 | 备案",
    "开发者：滴滴出行（北京）网络平台技术有限公司",
    "要想租车，就选滴滴租车，专业车型，一站式", "点击立即下载",
  )

  private fun hasDisclosure(texts: List<String>, packageName: String = "com.nowcasting.activity"): Boolean =
    AdCreativeRules.hasDownloadDisclosure(packageName, texts)

  @Test fun recognizesBothObservedDownloadDisclosureCards() {
    assertTrue(hasDisclosure(downloadCard))
    assertTrue(hasDisclosure(downloadCard.dropLast(2) +
      listOf("服务，省心省力！暑假7折起优惠", "点击立即下载")))
  }

  @Test fun noSingleDisclosureOrDownloadActionCanAuthorizeTheCard() {
    for (index in listOf(0, 1, 2, 3, 5)) {
      assertFalse("missing field $index", hasDisclosure(
        downloadCard.filterIndexed { at, _ -> at != index }))
    }
    assertFalse(hasDisclosure(listOf("点击立即下载")))
    assertFalse(hasDisclosure(listOf("应用名称：滴滴", "开发者：滴滴", "版本号：未知", "权限 隐私", "立即下载")))
  }

  @Test fun ordinaryWeatherMembershipAndPrivacyPanelsAreNotDownloadAds() {
    for (labels in listOf(
      listOf("40天趋势", "SVIP", "开通会员解锁全部", "查看详情"),
      listOf("历史天气", "温度", "降水量", "降水天数", "查看更多"),
      listOf("隐私政策", "权限管理", "版本号：7.63.1", "关闭"),
      listOf("应用名称：净屏", "版本号：1.0.32", "开发者：净屏", "权限 隐私", "检查更新"),
    )) assertFalse(labels.toString(), hasDisclosure(labels))
  }

  @Test fun theDisclosureExceptionDoesNotApplyToStoresOrOtherApps() {
    for (packageName in listOf("com.android.vending", "com.vivo.appstore", "com.example.app", "android")) {
      assertFalse(packageName, hasDisclosure(downloadCard, packageName))
    }
  }

  @Test fun recognizesTheTwoChinaMobileSplashCompositions() {
    for (offer in listOf("发口令 抢红包 至高领9元充值券", "11.9元/日起 境外流量")) {
      assertTrue(offer, AdCreativeRules.hasRedirectSplashComposition(
        listOf(badge, redirect, AdCreativeRules.Text(offer, AdGeometryRules.Box(150, 400, 1100, 900))), skip, screen))
    }
  }

  @Test fun aSkipCornerOrUnrelatedAdBadgeIsInsufficient() {
    assertFalse(AdCreativeRules.hasRedirectSplashComposition(listOf(badge), skip, screen))
    assertFalse(AdCreativeRules.hasRedirectSplashComposition(listOf(redirect), skip, screen))
    assertFalse(AdCreativeRules.hasRedirectSplashComposition(
      listOf(badge.copy(value = "免广告"), redirect), skip, screen))
    assertFalse(AdCreativeRules.hasRedirectSplashComposition(
      listOf(badge.copy(bounds = AdGeometryRules.Box(110, 1500, 215, 1560)), redirect), skip, screen))
    assertFalse(AdCreativeRules.hasRedirectSplashComposition(
      listOf(badge, redirect.copy(value = "允许并继续")), skip, screen))
  }

  @Test fun centeredOrLargeControlsAndTextOutsideTheWindowAreRejected() {
    assertFalse(AdCreativeRules.hasRedirectSplashComposition(listOf(badge, redirect),
      AdGeometryRules.Box(400, 120, 620, 220), screen))
    assertFalse(AdCreativeRules.hasRedirectSplashComposition(listOf(badge, redirect),
      AdGeometryRules.Box(0, 0, 1260, 2800), screen))
    assertFalse(AdCreativeRules.hasRedirectSplashComposition(listOf(badge, redirect),
      skip, AdGeometryRules.Box(700, 0, 1260, 2800)))
  }
}

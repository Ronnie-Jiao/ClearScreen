package com.clearscreen.prototype.backend

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WifiPromotionRulesTest {
  private val screen = AdGeometryRules.Box(0, 0, 1_000, 2_200)
  private val memberRegion = AdGeometryRules.Box(120, 300, 950, 1_650)
  private val memberCreative = AdGeometryRules.Box(190, 800, 810, 1_560)
  private val memberClose = AdGeometryRules.Box(820, 380, 890, 450)
  private val memberTexts = listOf(
    text("恭喜！红包到账", 300, 725, 700, 770),
    text("SVIP月卡专属红包", 300, 950, 700, 995),
    text("优惠后 1.99元 原价9元", 290, 1_140, 740, 1_185),
    text("领取并使用", 290, 1_330, 710, 1_380),
    text("免广告·专属热点优先连", 260, 1_470, 760, 1_510),
  )

  private val bannerRegion = AdGeometryRules.Box(235, 350, 1_000, 710)
  private val bannerCreative = AdGeometryRules.Box(250, 380, 1_000, 620)
  private val bannerClose = AdGeometryRules.Box(927, 440, 990, 515)
  private val bannerTexts = listOf(
    text("滴滴 惊喜等你拿", 320, 400, 650, 450),
    text("点击到落地页或第三方APP", 315, 550, 860, 590),
    text("关闭悬浮球可继续浏览应用", 400, 648, 900, 688),
  )

  private val gameRegion = AdGeometryRules.Box(760, 1_470, 995, 1_805)
  private val gameCreative = AdGeometryRules.Box(770, 1_550, 990, 1_800)
  private val gameClose = AdGeometryRules.Box(940, 1_480, 985, 1_525)
  private val gameTexts = listOf(text("玩转灵珠", 780, 1_720, 980, 1_770))

  @Test fun requestedMembershipCampaignAllowsItsDetachedCloseButNotAPageButton() {
    assertEquals(WifiPromotionRules.Kind.MEMBERSHIP_OFFER, classifyMember())
    assertNull(classifyMember(close = AdGeometryRules.Box(800, 1_820, 870, 1_890)))
    assertNull(classifyMember(close = AdGeometryRules.Box(40, 380, 110, 450)))
    assertNull(classifyMember(close = AdGeometryRules.Box(750, 380, 990, 600)))
  }

  @Test fun ordinaryVipPagesOrBackgroundCampaignTextCannotAuthorizeAnX() {
    assertNull(classifyMember(texts = memberTexts.filterNot { it.value.startsWith("恭喜") }))
    assertNull(classifyMember(texts = memberTexts.map {
      if (it.value.startsWith("恭喜")) text(it.value, 300, 100, 700, 145) else it
    }))
    assertNull(classifyMember(texts = memberTexts.filterNot { it.value.contains("原价") }))
    assertNull(classifyMember(texts = memberTexts.filterNot { it.value == "领取并使用" }))
    assertNull(WifiPromotionRules.classify("example.shop", screen, memberRegion,
      memberCreative, memberClose, memberTexts))
  }

  @Test fun externalFloatingBannerNeedsItsOwnCreativeAndDismissalHint() {
    assertEquals(WifiPromotionRules.Kind.EXTERNAL_FLOATING_BANNER, classifyBanner())
    assertNull(classifyBanner(texts = bannerTexts.filterNot { it.value.startsWith("关闭悬浮球") }))
    assertNull(classifyBanner(texts = bannerTexts.filterNot { it.value.contains("第三方") }))
    assertNull(classifyBanner(close = AdGeometryRules.Box(1_010, 440, 1_070, 515)))
    assertNull(classifyBanner(texts = bannerTexts.map {
      if (it.value.contains("惊喜")) text(it.value, 20, 900, 200, 950) else it
    }))
  }

  @Test fun requestedGameMustBeATightEdgeMountedCreativeWithItsOwnClose() {
    assertEquals(WifiPromotionRules.Kind.FLOATING_GAME, classifyGame())
    assertNull(classifyGame(region = screen))
    assertNull(classifyGame(texts = listOf(text("游戏", 780, 1_720, 980, 1_770))))
    assertNull(classifyGame(texts = listOf(text("玩转灵珠", 100, 1_720, 300, 1_770))))
    assertNull(classifyGame(close = AdGeometryRules.Box(700, 1_480, 745, 1_525)))
    assertNull(classifyGame(close = AdGeometryRules.Box(940, 1_690, 985, 1_735)))
  }

  @Test fun observedSdkBannerAlsoWorksInCalendarWithAnAdjacentSiblingCaption() {
    val creative = AdGeometryRules.Box(300, 380, 1_000, 650)
    val region = AdGeometryRules.Box(280, 350, 1_000, 660)
    val texts = bannerTexts.filterNot { it.value.startsWith("关闭悬浮球") } +
      text("关闭悬浮球可继续浏览应用", 400, 675, 900, 710)
    assertEquals(WifiPromotionRules.Kind.EXTERNAL_FLOATING_BANNER,
      WifiPromotionRules.classify(WifiPromotionRules.CALENDAR_PACKAGE_NAME, screen, region,
        creative, bannerClose, texts))
    assertNull(WifiPromotionRules.classify("example.calendar", screen, region,
      creative, bannerClose, texts))
    assertNull(WifiPromotionRules.classify(WifiPromotionRules.CALENDAR_PACKAGE_NAME, screen, region,
      creative, bannerClose, texts.filterNot { it.value.startsWith("关闭悬浮球") }))
  }

  @Test fun calendarOnlySharesTheObservedBannerNotWifiMembershipOrGames() {
    assertNull(WifiPromotionRules.classify(WifiPromotionRules.CALENDAR_PACKAGE_NAME,
      screen, memberRegion, memberCreative, memberClose, memberTexts))
    assertNull(WifiPromotionRules.classify(WifiPromotionRules.CALENDAR_PACKAGE_NAME,
      screen, gameRegion, gameCreative, gameClose, gameTexts))
  }

  private val calendarCard = AdGeometryRules.Box(30, 400, 970, 1_130)
  private val calendarVideo = AdGeometryRules.Box(60, 480, 940, 1_030)
  private val calendarClose = AdGeometryRules.Box(890, 1_050, 940, 1_090)
  private val calendarTexts = listOf(
    text("滴滴 | 8.0.16 | 隐私 | 权限 | 应用介绍 | 滴滴出行（北京）网络平台技术有限公司", 75, 500, 925, 530),
    text("为您推荐 下载 广告 滴滴", 60, 1_055, 650, 1_100),
  )

  @Test fun calendarVideoCardRequiresItsOwnDownloadFooterDisclosureAndLowerRightX() {
    assertEquals(WifiPromotionRules.Kind.CALENDAR_DOWNLOAD_CARD, classifyCalendar())
    assertNull(classifyCalendar(close = AdGeometryRules.Box(890, 490, 940, 530)))
    assertNull(classifyCalendar(close = AdGeometryRules.Box(300, 760, 580, 830)))
    assertNull(classifyCalendar(texts = calendarTexts.take(1)))
    assertNull(classifyCalendar(texts = calendarTexts.drop(1)))
    assertNull(classifyCalendar(texts = calendarTexts.map {
      if (it.value.startsWith("为您推荐")) it.copy(value = "为您推荐 下载 免广告") else it
    }))
    assertNull(WifiPromotionRules.classify(WifiPromotionRules.PACKAGE_NAME, screen,
      calendarCard, calendarVideo, calendarClose, calendarTexts))
  }

  private fun classifyCalendar(
    close: AdGeometryRules.Box = calendarClose,
    texts: List<AdCreativeRules.Text> = calendarTexts,
  ) = WifiPromotionRules.classify(WifiPromotionRules.CALENDAR_PACKAGE_NAME, screen,
    calendarCard, calendarVideo, close, texts)

  private fun classifyMember(
    close: AdGeometryRules.Box = memberClose,
    texts: List<AdCreativeRules.Text> = memberTexts,
  ) = WifiPromotionRules.classify(WifiPromotionRules.PACKAGE_NAME, screen, memberRegion,
    memberCreative, close, texts)

  private fun classifyBanner(
    close: AdGeometryRules.Box = bannerClose,
    texts: List<AdCreativeRules.Text> = bannerTexts,
  ) = WifiPromotionRules.classify(WifiPromotionRules.PACKAGE_NAME, screen, bannerRegion,
    bannerCreative, close, texts)

  private fun classifyGame(
    region: AdGeometryRules.Box = gameRegion,
    close: AdGeometryRules.Box = gameClose,
    texts: List<AdCreativeRules.Text> = gameTexts,
  ) = WifiPromotionRules.classify(WifiPromotionRules.PACKAGE_NAME, screen, region,
    gameCreative, close, texts)

  private fun text(value: String, left: Int, top: Int, right: Int, bottom: Int) =
    AdCreativeRules.Text(value, AdGeometryRules.Box(left, top, right, bottom))
}

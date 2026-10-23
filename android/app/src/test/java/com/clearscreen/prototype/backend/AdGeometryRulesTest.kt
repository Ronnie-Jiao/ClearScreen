package com.clearscreen.prototype.backend

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Screen ratios are based on real UI layouts, not absolute click coordinates.
 * Passing a geometry test is NEVER enough to authorize an ad dismissal:
 * semantic evidence, foreground package and privacy checks are mandatory.
 */
class AdGeometryRulesTest {
  private val screen = AdGeometryRules.Box(0, 0, 852, 1846)

  @Test fun screenshotCoordinatesStayOnTheDisplayDespiteInsetAppWindows() {
    val screenshotClose = AdGeometryRules.Box(1160, 976, 1208, 1027)
    val mapped = AdGeometryRules.mapScreenshotBox(screenshotClose, 1260, 2800, 1260, 2800)
    assertEquals(screenshotClose, mapped)
    // The app starts below a status bar. Its inset must not shift the X by 70px.
    assertTrue(AdGeometryRules.Box(0, 130, 1260, 2758).contains(mapped!!.centerX, mapped.centerY))
    assertTrue(screenshotClose.contains(mapped.centerX, mapped.centerY))
  }

  @Test fun resizedScreenshotsScaleToTheDisplayRatherThanTheAppBounds() {
    assertEquals(AdGeometryRules.Box(1160, 976, 1208, 1026),
      AdGeometryRules.mapScreenshotBox(AdGeometryRules.Box(580, 488, 604, 513), 630, 1400, 1260, 2800))
  }

  @Test fun invalidOcrCoordinatesCannotBecomeAClickTarget() {
    assertNull(AdGeometryRules.mapScreenshotBox(AdGeometryRules.Box(-1, 20, 30, 40), 630, 1400, 1260, 2800))
    assertNull(AdGeometryRules.mapScreenshotBox(AdGeometryRules.Box(600, 20, 650, 40), 630, 1400, 1260, 2800))
    assertNull(AdGeometryRules.mapScreenshotBox(AdGeometryRules.Box(10, 20, 30, 40), 0, 1400, 1260, 2800))
  }

  @Test fun countdownInTopRightIsRecognizable() {
    val countdown = AdGeometryRules.Box(659, 81, 807, 142)
    assertTrue(AdGeometryRules.isNearTopRight(countdown, screen))
    assertTrue(AdGeometryRules.isTightTapTarget(countdown, screen))
  }

  @Test fun lowerLeftSkipMustBeSmallAndInTheSplashActionArea() {
    val lowerLeftSkip = AdGeometryRules.Box(30, 1380, 120, 1445)
    val centerAction = AdGeometryRules.Box(330, 1380, 500, 1445)
    val oversizedAction = AdGeometryRules.Box(0, 1100, 400, 1500)

    assertTrue(AdGeometryRules.isBottomLeftSplashSkip(lowerLeftSkip, screen))
    assertFalse(AdGeometryRules.isBottomLeftSplashSkip(centerAction, screen))
    assertFalse(AdGeometryRules.isBottomLeftSplashSkip(oversizedAction, screen))
  }

  @Test fun largeClickableBackgroundIsNotAnAcceptableTarget() {
    val fullscreen = AdGeometryRules.Box(0, 0, 852, 1800)
    assertFalse(AdGeometryRules.isCompactControl(fullscreen, screen))
    assertFalse(AdGeometryRules.isTightTapTarget(fullscreen, screen))
  }

  @Test fun floatingCloseAboveModalCanBeRecognized() {
    // China Mobile: X sits ~150 pixels above the promotional card.
    val panel = AdGeometryRules.Box(123, 504, 710, 1289)
    val close = AdGeometryRules.Box(711, 317, 786, 389)
    assertTrue(AdGeometryRules.isModalCorner(close, panel, screen))
    assertTrue(AdGeometryRules.isSmallModalClose(close, screen))
  }

  @Test fun closeAtTopEdgeOfModalCanBeRecognized() {
    // Third-party interstitial (e.g. rental ad) with an "广告" badge.
    val panel = AdGeometryRules.Box(82, 621, 746, 1280)
    val close = AdGeometryRules.Box(678, 626, 735, 689)
    assertTrue(AdGeometryRules.isModalCorner(close, panel, screen))
  }

  @Test fun genericToolbarCloseIsNotAnAdModalClose() {
    val panel = AdGeometryRules.Box(82, 621, 746, 1280)
    val toolbarClose = AdGeometryRules.Box(710, 48, 780, 112)
    assertFalse(AdGeometryRules.isModalCorner(toolbarClose, panel, screen))
  }

  @Test fun fullscreenContainerIsNotAnAdModal() {
    val fullWindow = AdGeometryRules.Box(0, 0, 850, 1810)
    val close = AdGeometryRules.Box(750, 10, 800, 90)
    assertFalse(AdGeometryRules.isModalCorner(close, fullWindow, screen))
  }

  @Test fun labelledVideoAdCanUseTightLowerRightGestureOnLandscapeScreen() {
    val landscape = AdGeometryRules.Box(0, 0, 1846, 852)
    val bottomRightSkip = AdGeometryRules.Box(1570, 737, 1785, 817)
    assertTrue(AdGeometryRules.isVideoAdGestureTarget(bottomRightSkip, landscape))
    val centerPurchase = AdGeometryRules.Box(700, 360, 1150, 490)
    assertFalse(AdGeometryRules.isVideoAdGestureTarget(centerPurchase, landscape))
    val bigPlayerSurface = AdGeometryRules.Box(0, 0, 1846, 852)
    assertFalse(AdGeometryRules.isVideoAdGestureTarget(bigPlayerSurface, landscape))
  }

  @Test fun nonTopRightControlNeedsIndependentModalEvidence() {
    val leftClose = AdGeometryRules.Box(40, 800, 90, 850)
    assertFalse(AdGeometryRules.isNearTopRight(leftClose, screen))
    assertFalse(AdGeometryRules.isTightTapTarget(leftClose, screen))
  }

  @Test fun thinAdBannerCanBeConsideredWithoutRelaxingSemanticChecks() {
    val display = AdGeometryRules.Box(0, 0, 1260, 2800)
    val banner = AdGeometryRules.Box(30, 180, 1100, 300)
    val tallContentCard = AdGeometryRules.Box(30, 300, 900, 1100)

    assertTrue(AdGeometryRules.isHorizontalAdBanner(banner, display))
    assertFalse(AdGeometryRules.isHorizontalAdBanner(tallContentCard, display))
  }

  @Test fun bannerCloseMustBeSmallAndOverlapTheRecognizedX() {
    val display = AdGeometryRules.Box(0, 0, 1260, 2800)
    val banner = AdGeometryRules.Box(30, 180, 1100, 300)
    val close = AdGeometryRules.Box(1010, 200, 1085, 275)
    val xGlyph = AdGeometryRules.Box(1033, 221, 1062, 250)
    val adjacentPlus = AdGeometryRules.Box(1115, 180, 1235, 320)
    val broadBannerHitArea = AdGeometryRules.Box(931, 170, 1210, 269)

    assertTrue(AdGeometryRules.isBannerCloseTarget(close, xGlyph, banner, display))
    assertFalse(AdGeometryRules.isBannerCloseTarget(adjacentPlus, xGlyph, banner, display))
    assertFalse(AdGeometryRules.isBannerCloseTarget(broadBannerHitArea, xGlyph, banner, display))
  }

  @Test fun fullScreenCreativeMayExposeALabelledClosePillButNotABroadButton() {
    val display = AdGeometryRules.Box(0, 0, 922, 2048)
    val closePill = AdGeometryRules.Box(665, 52, 872, 144)
    val broadButton = AdGeometryRules.Box(500, 50, 920, 260)

    assertTrue(AdGeometryRules.isCompactFullScreenAdClose(closePill, display))
    assertFalse(AdGeometryRules.isCompactFullScreenAdClose(broadButton, display))
  }

  @Test fun floatingCreativeCloseMustBeSmallAndAttachedToItsOwnRightEdge() {
    val creative = AdGeometryRules.Box(600, 1000, 820, 1300)
    val close = AdGeometryRules.Box(790, 1015, 822, 1047)
    val detached = AdGeometryRules.Box(790, 1410, 822, 1442)
    val broadTarget = AdGeometryRules.Box(700, 980, 850, 1090)
    val fullPage = AdGeometryRules.Box(0, 0, 852, 1846)

    assertTrue(AdGeometryRules.isOcrFloatingCreative(creative, screen))
    assertTrue(AdGeometryRules.isCompactFloatingCreativeClose(close, creative, screen))
    assertFalse(AdGeometryRules.isCompactFloatingCreativeClose(detached, creative, screen))
    assertFalse(AdGeometryRules.isCompactFloatingCreativeClose(broadTarget, creative, screen))
    assertFalse(AdGeometryRules.isOcrFloatingCreative(fullPage, screen))
  }
}

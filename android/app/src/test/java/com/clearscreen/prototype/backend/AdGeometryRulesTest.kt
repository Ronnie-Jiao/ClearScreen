package com.clearscreen.prototype.backend

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Screen ratios are based on real UI layouts, not absolute click coordinates.
 * Passing a geometry test is NEVER enough to authorize an ad dismissal:
 * semantic evidence, foreground package and privacy checks are mandatory.
 */
class AdGeometryRulesTest {
  private val screen = AdGeometryRules.Box(0, 0, 852, 1846)

  @Test fun countdownInTopRightIsRecognizable() {
    val countdown = AdGeometryRules.Box(659, 81, 807, 142)
    assertTrue(AdGeometryRules.isNearTopRight(countdown, screen))
    assertTrue(AdGeometryRules.isTightTapTarget(countdown, screen))
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
}

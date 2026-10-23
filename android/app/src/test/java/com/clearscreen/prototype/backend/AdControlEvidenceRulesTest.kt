package com.clearscreen.prototype.backend

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Acceptance corpus for the new policy; compiling this file does not execute it. */
class AdControlEvidenceRulesTest {
  private val screen = AdGeometryRules.Box(0, 0, 1260, 2800)
  private val close = AdSemanticRules.controlLabel(listOf("关闭"))!!
  private val skip = AdSemanticRules.controlLabel(listOf("跳过3"))!!
  private val glyph = AdSemanticRules.controlLabel(listOf("×"))!!
  private val fullAd = AdControlEvidenceRules.Evidence(true, true, true, true, false, true)

  @Test fun buttonPositionIsNotAnAuthorizationOrAVeto() {
    for (x in listOf(20, 480, 940)) for (y in listOf(120, 1300, 2500)) {
      val box = AdGeometryRules.Box(x, y, x + 260, y + 100)
      assertTrue(AdControlEvidenceRules.isIndependentControl(box, box, screen, screen, 3.5, false))
      assertTrue(fullAd.permits(close))
      assertFalse(fullAd.copy(badge = false, redirect = false).permits(close))
    }
  }

  @Test fun densityAndButtonSizeVaryWithoutAcceptingAnEntireCreative() {
    for (width in listOf(80, 160, 320, 540)) {
      val target = AdGeometryRules.Box(300, 800, 300 + width, 960)
      val text = AdGeometryRules.Box(320, 840, 360, 900)
      assertTrue(AdControlEvidenceRules.isIndependentControl(target, text, screen, screen, 3.5, false))
    }
    assertFalse(AdControlEvidenceRules.isIndependentControl(
      AdGeometryRules.Box(0, 0, 1260, 2600), AdGeometryRules.Box(1100, 200, 1200, 250),
      screen, screen, 3.5, false))
    assertFalse(AdControlEvidenceRules.isIndependentControl(
      AdGeometryRules.Box(100, 100, 400, 200), AdGeometryRules.Box(500, 100, 550, 140),
      screen, screen, 3.5, false))
  }

  @Test fun aPaddedTargetCanOccupyMostOfAnAdRowsHeight() {
    val row = AdGeometryRules.Box(60, 1600, 1200, 1740)
    val target = AdGeometryRules.Box(1050, 1610, 1180, 1730)
    assertTrue(AdControlEvidenceRules.isIndependentControl(target,
      AdGeometryRules.Box(1090, 1640, 1140, 1690), row, screen, 3.5, true))
  }

  @Test fun countdownAndMarketingCopyDoNotAuthorizeVisualCoordinates() {
    val countdown = fullAd.copy(badge = false, readyCountdown = true)
    assertTrue(countdown.permits(skip))
    assertFalse(countdown.permitsVisualTouch(skip))
    assertFalse(countdown.permits(close))
    assertFalse(fullAd.copy(badge = false, redirect = false, callToAction = true).permits(skip))
    assertFalse(fullAd.copy(visual = false).permits(close))
  }

  @Test fun xNeedsABoundedAdAndNeverGetsAGenericVisualTouch() {
    assertFalse(fullAd.permits(glyph))
    val card = fullAd.copy(fullScreen = false)
    assertTrue(card.permits(glyph))
    assertFalse(card.copy(badge = false).permits(glyph))
    assertFalse(card.permitsVisualTouch(glyph))
    assertTrue(fullAd.permitsVisualTouch(close))
  }

  @Test fun actionSynonymsStaySeparateFromBusinessAndWaitingControls() {
    for (label in listOf("关 闭", "关掉", "关闭按钮", "Close", "关闭这条广告", "跳 过", "跳过 (3s)")) {
      assertTrue(label, AdSemanticRules.controlLabel(listOf(label, "按钮")) != null)
    }
    for (label in listOf("3秒后可跳过", "跳过教程", "跳过片头", "关闭自动续费",
      "关闭播放器", "关闭权限", "Skip in 3", "跳过广告并支付")) {
      assertNull(label, AdSemanticRules.controlLabel(listOf(label)))
    }
    assertNull(AdSemanticRules.controlLabel(listOf("关闭", "关闭自动续费")))
    assertTrue(AdSemanticRules.isBusinessControlResource("com.example:id/skip_guide"))
    assertTrue(AdSemanticRules.isBusinessControlResource("com.example:id/close_player"))
    assertFalse(AdSemanticRules.isBusinessControlResource("com.example:id/ad_skip"))
    assertTrue(AdSemanticRules.controlLabel(listOf("关闭视频广告")) != null)
  }

  @Test fun adWordsInSettingsAreNotBadges() {
    assertTrue(AdControlEvidenceRules.isAdBadge("互动广告 淘宝"))
    for (value in listOf("免广告", "广告设置", "关闭广告", "广告投放设置", "无广告模式")) {
      assertFalse(value, AdControlEvidenceRules.isAdBadge(value))
    }
    assertFalse(AdControlEvidenceRules.isAdContainer("com.example:id/download_button"))
    assertTrue(AdControlEvidenceRules.isAdContainer("com.example:id/ad_container"))
    assertTrue(AdControlEvidenceRules.hasRedirectDisclosure("滑动或点击按钮跳转至详情页或第三方应用"))
  }
}

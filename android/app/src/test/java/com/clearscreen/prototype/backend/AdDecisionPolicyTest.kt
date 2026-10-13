package com.clearscreen.prototype.backend

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdDecisionPolicyTest {
  @Test fun systemConfirmationWindowsAreProtected() {
    assertTrue(AdDecisionPolicy.isProtectedPackage("com.android.permissioncontroller"))
    assertTrue(AdDecisionPolicy.isProtectedPackage("com.miui.securitycenter"))
    assertFalse(AdDecisionPolicy.isProtectedPackage("com.nowcasting.activity"))
  }

  @Test fun anExternalAppLaunchPromptCannotAuthorizeAnAdClick() {
    assertTrue(AdDecisionPolicy.isProtectedConfirmation(
      listOf("彩云天气想要打开大众点评是否允许", "拒绝", "始终允许")))
    assertFalse(AdDecisionPolicy.isProtectedConfirmation(
      listOf("广告", "立即打开", "关闭广告")))
  }

  @Test fun aBareCountdownNeedsPositionOrAdEvidence() {
    assertFalse(AdDecisionPolicy.allowCountdownSkip(false, false, false, false, false, false))
    assertFalse(AdDecisionPolicy.allowCountdownSkip(false, true, false, false, false, false))
    assertTrue(AdDecisionPolicy.allowCountdownSkip(false, false, false, false, true, false))
    assertTrue(AdDecisionPolicy.allowCountdownSkip(false, true, true, false, false, false))
  }

  @Test fun ocrSplashSkipNeedsASkipControlAndAdLikeFullScreenCreative() {
    fun allowed(
      hasSkipLabel: Boolean = true,
      compactTopRight: Boolean = true,
      hasLargeCreative: Boolean = true,
      hasExplicitAdMarker: Boolean = false,
      promotionalSignalCount: Int = 0,
      hasPromotionalAction: Boolean = false,
    ) = AdDecisionPolicy.allowOcrBackedSplashSkip(
      hasSkipLabel,
      compactTopRight,
      hasLargeCreative,
      hasExplicitAdMarker,
      promotionalSignalCount,
      hasPromotionalAction,
    )

    assertTrue(allowed(hasExplicitAdMarker = true))
    assertTrue(allowed(promotionalSignalCount = 2, hasPromotionalAction = true))
    assertFalse(allowed(hasExplicitAdMarker = true, hasLargeCreative = false))
    assertFalse(allowed(promotionalSignalCount = 1, hasPromotionalAction = true))
    assertFalse(allowed(promotionalSignalCount = 3, hasPromotionalAction = false))
    assertFalse(allowed(hasSkipLabel = false, hasExplicitAdMarker = true))
    assertFalse(allowed(compactTopRight = false, hasExplicitAdMarker = true))
  }

  @Test fun ocrPopupCloseNeedsLocalAdCopyAndAnIndependentEdgeControl() {
    fun allowed(
      hasCloseSemantic: Boolean = true,
      isBoundedLocalRegion: Boolean = true,
      hasExplicitAdMarker: Boolean = false,
      promotionalSignalCount: Int = 0,
      hasPromotionalAction: Boolean = true,
      isIndependentClose: Boolean = true,
      isAtRegionEdge: Boolean = true,
    ) = AdDecisionPolicy.allowOcrBackedPopupClose(
      hasCloseSemantic,
      isBoundedLocalRegion,
      hasExplicitAdMarker,
      promotionalSignalCount,
      hasPromotionalAction,
      isIndependentClose,
      isAtRegionEdge,
    )

    assertTrue(allowed(hasExplicitAdMarker = true))
    assertTrue(allowed(promotionalSignalCount = 3))
    assertFalse(allowed(isBoundedLocalRegion = false, hasExplicitAdMarker = true))
    assertFalse(allowed(promotionalSignalCount = 2))
    assertFalse(allowed(hasPromotionalAction = false, hasExplicitAdMarker = true))
    assertFalse(allowed(isIndependentClose = false, hasExplicitAdMarker = true))
    assertFalse(allowed(isAtRegionEdge = false, hasExplicitAdMarker = true))
    assertFalse(allowed(hasCloseSemantic = false, hasExplicitAdMarker = true))
  }

  @Test fun evidenceCannotComeFromASiblingCard() {
    assertTrue(AdDecisionPolicy.isDescendantOf("0.3.2", "0.3"))
    assertFalse(AdDecisionPolicy.isDescendantOf("0.30.2", "0.3"))
    assertFalse(AdDecisionPolicy.isDescendantOf("0.4.2", "0.3"))
  }

  @Test fun playerMonitoringNeedsAPlayerOrVideoAdCue() {
    assertFalse(AdDecisionPolicy.shouldMonitorMedia(false, true, false))
    assertFalse(AdDecisionPolicy.shouldMonitorMedia(false, false, true))
    assertTrue(AdDecisionPolicy.shouldMonitorMedia(false, true, true))
    assertTrue(AdDecisionPolicy.shouldMonitorMedia(true, false, false))
  }

  @Test fun anUnlabelledImageAloneIsNotAClosingAction() {
    assertFalse(AdDecisionPolicy.allowUnlabeledClose(false, false))
    assertTrue(AdDecisionPolicy.allowUnlabeledClose(true, false))
    assertTrue(AdDecisionPolicy.allowUnlabeledClose(false, true))
  }

  @Test fun aBottomCloseNeedsExplicitAdCloseSemantics() {
    assertTrue(AdDecisionPolicy.isVerifiedCardEdge(0.88, 0.12, false))
    assertFalse(AdDecisionPolicy.isVerifiedCardEdge(0.50, 0.90, false))
    assertTrue(AdDecisionPolicy.isVerifiedCardEdge(0.50, 0.90, true))
    assertFalse(AdDecisionPolicy.isVerifiedCardEdge(0.50, 0.50, true))
  }

  @Test fun embeddedAdCloseRequiresAnIndependentButtonInTheSameCard() {
    val safe = mapOf(
      "directClickable" to true,
      "sameSubtree" to true,
      "explicitAdBadge" to true,
      "closeSemantic" to true,
      "atVerifiedCardEdge" to true,
    )
    fun allowed(overrides: Map<String, Boolean> = emptyMap(), area: Double = 0.20,
      targetArea: Double = 0.02): Boolean {
      val values = safe + overrides
      return AdDecisionPolicy.allowEmbeddedAdClose(
        directClickable = values.getValue("directClickable"),
        sameSubtree = values.getValue("sameSubtree"),
        explicitAdBadge = values.getValue("explicitAdBadge"),
        closeSemantic = values.getValue("closeSemantic"),
        atVerifiedCardEdge = values.getValue("atVerifiedCardEdge"),
        cardAreaRatio = area,
        closeAreaRatio = targetArea,
        closeWidthRatio = 0.14,
        closeHeightRatio = 0.12,
      )
    }
    assertTrue(allowed())
    assertFalse(allowed(mapOf("directClickable" to false)))
    assertFalse(allowed(mapOf("sameSubtree" to false)))
    assertFalse(allowed(mapOf("explicitAdBadge" to false)))
    assertFalse(allowed(mapOf("closeSemantic" to false)))
    assertFalse(allowed(mapOf("atVerifiedCardEdge" to false)))
    assertFalse(allowed(area = 0.90))
    assertFalse(allowed(targetArea = 0.10))
  }
}

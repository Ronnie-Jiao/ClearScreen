package com.clearscreen.prototype.backend

import java.util.Locale

/** Small, testable gates shared by the accessibility detector's action paths. */
internal object AdDecisionPolicy {
  private val protectedPackages = setOf(
    "android",
    "com.android.systemui",
    "com.android.permissioncontroller",
    "com.google.android.permissioncontroller",
    "com.android.packageinstaller",
    "com.google.android.packageinstaller",
    "com.miui.packageinstaller",
    "com.miui.securitycenter",
  )

  fun isProtectedPackage(packageName: String): Boolean =
    packageName.lowercase(Locale.ROOT) in protectedPackages

  fun isProtectedConfirmation(labels: List<String>): Boolean {
    val hasDecision = labels.any { it == "拒绝" || it == "不允许" || it == "始终允许" ||
      it == "仅此一次" || it == "仅在使用期间允许" }
    val hasSystemQuestion = labels.any { it.contains("是否允许") || it.contains("想要打开") ||
      it.contains("权限请求") || it.contains("允许此应用") || it.contains("安装应用") }
    return hasDecision && hasSystemQuestion
  }

  fun isDescendantOf(path: String, ancestorPath: String): Boolean =
    path.startsWith("$ancestorPath.")

  fun allowCountdownSkip(
    hasAdWord: Boolean,
    nearTopRight: Boolean,
    hasSkipResource: Boolean,
    localAdContext: Boolean,
    videoAdContext: Boolean,
    verifiedTopCenterAd: Boolean,
  ): Boolean = hasAdWord || (nearTopRight && hasSkipResource) ||
    localAdContext || videoAdContext || verifiedTopCenterAd

  fun allowOcrBackedSplashSkip(
    hasSkipLabel: Boolean,
    compactTopRight: Boolean,
    hasLargeCreative: Boolean,
    hasExplicitAdMarker: Boolean,
    promotionalSignalCount: Int,
    hasPromotionalAction: Boolean,
  ): Boolean = hasSkipLabel && compactTopRight && hasLargeCreative &&
    (hasExplicitAdMarker || (promotionalSignalCount >= 2 && hasPromotionalAction))

  fun allowOcrBackedBottomLeftSplashSkip(
    hasSkipLabel: Boolean,
    compactBottomLeft: Boolean,
    hasLargeCreative: Boolean,
    currentWindowEvidence: Boolean,
    controlInsideCreative: Boolean,
    hasExplicitAdMarker: Boolean,
    hasPromotionalAction: Boolean,
  ): Boolean = hasSkipLabel && compactBottomLeft && hasLargeCreative &&
    currentWindowEvidence && controlInsideCreative && hasExplicitAdMarker && hasPromotionalAction

  fun allowOcrBackedPopupClose(
    hasCloseSemantic: Boolean,
    isBoundedLocalRegion: Boolean,
    hasExplicitAdMarker: Boolean,
    promotionalSignalCount: Int,
    hasPromotionalAction: Boolean,
    isIndependentClose: Boolean,
    isAtRegionEdge: Boolean,
  ): Boolean = hasCloseSemantic && isBoundedLocalRegion && isIndependentClose &&
    isAtRegionEdge && hasPromotionalAction &&
    (hasExplicitAdMarker || promotionalSignalCount >= 3)

  fun shouldMonitorMedia(
    hasVideoAdCue: Boolean,
    hasPlayerSurface: Boolean,
    playerEntry: Boolean,
  ): Boolean = hasVideoAdCue || (playerEntry && hasPlayerSurface)

  fun shouldEvaluateGenericAdCandidates(
    isVideoPlayer: Boolean,
    hasVideoSpecificCandidate: Boolean,
  ): Boolean = !isVideoPlayer || !hasVideoSpecificCandidate

  fun allowOcrBackedFullScreenAdClose(
    hasCloseSemantic: Boolean,
    compactTopRightControl: Boolean,
    currentWindowEvidence: Boolean,
    controlBelongsToLargeCreative: Boolean,
    hasExplicitAdMarker: Boolean,
  ): Boolean = hasCloseSemantic && compactTopRightControl && currentWindowEvidence &&
    controlBelongsToLargeCreative && hasExplicitAdMarker

  fun allowOcrBackedFloatingCreativeClose(
    hasCloseSemantic: Boolean,
    compactCreativeEdgeTarget: Boolean,
    currentWindowEvidence: Boolean,
    sameCreativeEvidence: Boolean,
    hasAdMarkerOrPromoCluster: Boolean,
  ): Boolean = hasCloseSemantic && compactCreativeEdgeTarget && currentWindowEvidence &&
    sameCreativeEvidence && hasAdMarkerOrPromoCluster

  fun allowUnlabeledClose(hasCloseId: Boolean, hasOcrGlyph: Boolean): Boolean =
    hasCloseId || hasOcrGlyph

  fun isVerifiedCardEdge(x: Double, y: Double, explicitAdClose: Boolean): Boolean =
    (x >= 0.68 && y <= 0.38) ||
      (explicitAdClose && x in 0.42..0.58 && y >= 0.80)

  fun allowEmbeddedAdClose(
    directClickable: Boolean,
    sameSubtree: Boolean,
    explicitAdBadge: Boolean,
    closeSemantic: Boolean,
    atVerifiedCardEdge: Boolean,
    cardAreaRatio: Double,
    closeAreaRatio: Double,
    closeWidthRatio: Double,
    closeHeightRatio: Double,
  ): Boolean = directClickable && sameSubtree && explicitAdBadge && closeSemantic &&
    atVerifiedCardEdge && cardAreaRatio in 0.025..0.65 &&
    closeAreaRatio in 0.0..0.05 && closeWidthRatio in 0.0..0.28 &&
    closeHeightRatio in 0.0..0.24
}

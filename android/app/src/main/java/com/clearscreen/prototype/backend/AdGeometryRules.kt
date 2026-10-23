package com.clearscreen.prototype.backend

/**
 * Pixel-independent geometry policy. Decisions require semantic evidence too;
 * no coordinate alone is ever sufficient to authorize an action.
 */
internal object AdGeometryRules {
  /** Accessibility screenshots cover the display, not the app's inset window. */
  fun mapScreenshotBox(
    box: Box,
    imageWidth: Int,
    imageHeight: Int,
    displayWidth: Int,
    displayHeight: Int,
  ): Box? {
    if (imageWidth <= 0 || imageHeight <= 0 || displayWidth <= 0 || displayHeight <= 0 ||
      box.width <= 0 || box.height <= 0 || box.left < 0 || box.top < 0 ||
      box.right > imageWidth || box.bottom > imageHeight
    ) return null
    return Box(
      (box.left.toLong() * displayWidth / imageWidth).toInt(),
      (box.top.toLong() * displayHeight / imageHeight).toInt(),
      (box.right.toLong() * displayWidth / imageWidth).toInt(),
      (box.bottom.toLong() * displayHeight / imageHeight).toInt(),
    ).takeIf { it.width > 0 && it.height > 0 }
  }

  data class Box(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
  ) {
    val width: Int get() = right - left
    val height: Int get() = bottom - top
    val centerX: Int get() = (left + right) / 2
    val centerY: Int get() = (top + bottom) / 2
    fun contains(x: Int, y: Int): Boolean =
      x >= left && x < right && y >= top && y < bottom
  }

  fun isCompactControl(target: Box, screen: Box): Boolean =
    target.width > 0 && target.height > 0 &&
      target.width <= screen.width.coerceAtLeast(1) * 0.36f &&
      target.height <= screen.height.coerceAtLeast(1) * 0.22f

  fun isNearTopRight(target: Box, screen: Box): Boolean {
    val width = screen.width.coerceAtLeast(1)
    val height = screen.height.coerceAtLeast(1)
    return target.centerX >= screen.left + width * 0.55f &&
      target.top <= screen.top + height * 0.48f &&
      target.right >= screen.left + width * 0.70f
  }

  fun isTightTapTarget(target: Box, screen: Box): Boolean {
    val width = screen.width.coerceAtLeast(1)
    val height = screen.height.coerceAtLeast(1)
    return target.width > 0 && target.height > 0 &&
      target.width <= width * 0.30f &&
      target.height <= height * 0.11f &&
      target.centerX >= screen.left + width * 0.63f &&
      target.top <= screen.top + height * 0.43f
  }

  /**
   * Video players can be landscape and place a *labelled* ad Skip at either
   * right-hand corner. This is used ONLY for explicit ad-labelled controls
   * with independent nearby evidence, never for a generic video player X.
   */
  fun isVideoAdGestureTarget(target: Box, screen: Box): Boolean {
    val width = screen.width.coerceAtLeast(1)
    val height = screen.height.coerceAtLeast(1)
    return target.width > 0 && target.height > 0 &&
      target.width <= width * 0.29f &&
      target.height <= height * 0.18f &&
      target.centerX >= screen.left + width * 0.66f &&
      (target.top <= screen.top + height * 0.36f ||
        target.bottom >= screen.top + height * 0.70f)
  }

  fun isSmallModalClose(target: Box, screen: Box): Boolean {
    val width = screen.width.coerceAtLeast(1)
    val height = screen.height.coerceAtLeast(1)
    return target.width > 0 && target.height > 0 &&
      target.width <= width * 0.18f &&
      target.height <= height * 0.10f
  }

  /** A labelled close pill on a full-screen ad may be wider than an X icon. */
  fun isCompactFullScreenAdClose(target: Box, screen: Box): Boolean {
    val width = screen.width.coerceAtLeast(1)
    val height = screen.height.coerceAtLeast(1)
    val screenArea = width.toLong() * height.toLong()
    val targetArea = target.width.toLong().coerceAtLeast(0L) * target.height.toLong().coerceAtLeast(0L)
    return target.width > 0 && target.height > 0 &&
      target.width <= width * 0.30f && target.height <= height * 0.11f &&
      targetArea.toDouble() / screenArea <= 0.035
  }

  /** A lower-left splash skip is allowed only by a separate same-screen ad-evidence policy. */
  fun isBottomLeftSplashSkip(target: Box, screen: Box): Boolean {
    val width = screen.width.coerceAtLeast(1)
    val height = screen.height.coerceAtLeast(1)
    val screenArea = width.toLong() * height.toLong()
    val targetArea = target.width.toLong().coerceAtLeast(0L) * target.height.toLong().coerceAtLeast(0L)
    return target.width > 0 && target.height > 0 &&
      target.width <= width * 0.22f && target.height <= height * 0.12f &&
      targetArea.toDouble() / screenArea <= 0.025 &&
      target.centerX <= screen.left + width * 0.32f &&
      target.centerY in (screen.top + height * 0.58f).toInt()..(screen.top + height * 0.92f).toInt()
  }

  /** Bound an OCR creative to a visible, non-full-page floating ad surface. */
  fun isOcrFloatingCreative(creative: Box, screen: Box): Boolean {
    val width = screen.width.coerceAtLeast(1)
    val height = screen.height.coerceAtLeast(1)
    val screenArea = width.toLong() * height.toLong()
    val creativeArea = creative.width.toLong().coerceAtLeast(0L) *
      creative.height.toLong().coerceAtLeast(0L)
    return creative.width >= width * 0.13f && creative.height >= height * 0.06f &&
      creativeArea.toDouble() / screenArea in 0.008..0.65
  }

  /** Match a real small close control to the upper/right edge of its own floating creative. */
  fun isCompactFloatingCreativeClose(target: Box, creative: Box, screen: Box): Boolean {
    val screenWidth = screen.width.coerceAtLeast(1)
    val screenHeight = screen.height.coerceAtLeast(1)
    val screenArea = screenWidth.toLong() * screenHeight.toLong()
    val targetArea = target.width.toLong().coerceAtLeast(0L) *
      target.height.toLong().coerceAtLeast(0L)
    val marginX = (screenWidth * 0.035f).toInt()
    val marginY = (screenHeight * 0.025f).toInt()
    if (target.width <= 0 || target.height <= 0 ||
      target.width > screenWidth * 0.12f || target.height > screenHeight * 0.08f ||
      targetArea.toDouble() / screenArea > 0.012 ||
      target.centerX !in (creative.left - marginX)..(creative.right + marginX) ||
      target.centerY !in (creative.top - marginY)..(creative.bottom + marginY)
    ) return false

    val relativeX = (target.centerX - creative.left).toDouble() / creative.width.coerceAtLeast(1)
    val relativeY = (target.centerY - creative.top).toDouble() / creative.height.coerceAtLeast(1)
    val upperRightEdge = relativeX >= 0.62 && relativeY in -0.10..0.50
    val rightEdge = relativeX >= 0.82 && relativeY in -0.05..1.05
    return upperRightEdge || rightEdge
  }

  fun isModalCorner(close: Box, panel: Box, screen: Box): Boolean {
    val width = screen.width.coerceAtLeast(1)
    val height = screen.height.coerceAtLeast(1)
    return panel.width >= width * 0.52f && panel.width <= width * 0.93f &&
      panel.height >= height * 0.22f && panel.height <= height * 0.76f &&
      panel.top >= screen.top + height * 0.10f &&
      panel.top <= screen.top + height * 0.60f &&
      close.centerX >= panel.right - width * 0.23f &&
      close.centerX <= panel.right + width * 0.10f &&
      close.centerY >= panel.top - height * 0.14f &&
      close.centerY <= panel.top + height * 0.14f
  }

  /**
   * A thin horizontal banner can be an ad card even though it is too short to
   * qualify as a feed card. This is geometry only; callers still require a
   * same-region explicit ad marker and a real accessible close target.
   */
  fun isHorizontalAdBanner(region: Box, screen: Box): Boolean {
    val width = screen.width.coerceAtLeast(1)
    val height = screen.height.coerceAtLeast(1)
    val areaRatio = region.width.toLong().coerceAtLeast(0L) *
      region.height.toLong().coerceAtLeast(0L) / (width.toLong() * height.toLong()).toDouble()
    return region.width >= width * 0.58f &&
      region.height in (height * 0.035f).toInt()..(height * 0.18f).toInt() &&
      region.width.toDouble() / region.height.coerceAtLeast(1) >= 4.0 &&
      areaRatio in 0.02..0.18
  }

  /** Match a visual X to a compact, independently clickable control at a banner's right edge. */
  fun isBannerCloseTarget(target: Box, closeGlyph: Box, banner: Box, screen: Box): Boolean {
    val screenWidth = screen.width.coerceAtLeast(1)
    val screenHeight = screen.height.coerceAtLeast(1)
    val screenArea = screenWidth.toLong() * screenHeight.toLong()
    val targetArea = target.width.toLong().coerceAtLeast(0L) * target.height.toLong().coerceAtLeast(0L)
    val targetY = target.centerY
    val verticalPadding = (screenHeight * 0.02f).toInt()
    val relativeX = (target.centerX - banner.left).toDouble() / banner.width.coerceAtLeast(1)
    return target.width > 0 && target.height > 0 && closeGlyph.width > 0 && closeGlyph.height > 0 &&
      target.contains(closeGlyph.centerX, closeGlyph.centerY) &&
      target.width <= screenWidth * 0.12f && target.height <= screenHeight * 0.08f &&
      targetArea.toDouble() / screenArea <= 0.01 &&
      relativeX >= 0.70 && target.centerX <= banner.right + screenWidth * 0.03f &&
      targetY in (banner.top - verticalPadding)..(banner.bottom + verticalPadding)
  }

  /** Requires a separate same-card ad badge and action; a right-side icon alone is insufficient. */
  fun isRightEdgeAdCardClose(target: Box, glyph: Box, card: Box, screen: Box): Boolean {
    if (screen.width <= 0 || screen.height <= 0) return false
    val width = screen.width.coerceAtLeast(1)
    val height = screen.height.coerceAtLeast(1)
    if (card.width < width * 0.50 || card.height <= 0 ||
      target.width <= 0 || target.height <= 0 || glyph.width <= 0 || glyph.height <= 0 ||
      target.width > width * 0.14 || target.height > height * 0.08 ||
      glyph.width > width * 0.08 || glyph.height > height * 0.06 ||
      !target.contains(glyph.centerX, glyph.centerY) || !card.contains(glyph.centerX, glyph.centerY)
    ) return false
    val x = (glyph.centerX - card.left).toDouble() / card.width
    val y = (glyph.centerY - card.top).toDouble() / card.height
    return x in 0.86..1.0 && y in 0.10..0.96 &&
      target.centerX >= card.left + card.width * 0.80 &&
      target.centerX <= card.right + width * 0.02
  }

  /** Only links a detached control to a large poster's immediately adjacent upper-right corner. */
  fun isDetachedPosterClose(target: Box, poster: Box, screen: Box): Boolean {
    if (screen.width <= 0 || screen.height <= 0) return false
    val width = screen.width.coerceAtLeast(1)
    val height = screen.height.coerceAtLeast(1)
    val posterAreaRatio = poster.width.toLong().coerceAtLeast(0L) * poster.height.coerceAtLeast(0) /
      (width.toLong() * height).toDouble()
    return target.width > 0 && target.height > 0 &&
      target.width <= width * 0.12 && target.height <= height * 0.08 &&
      poster.width in (width * 0.60).toInt()..(width * 0.92).toInt() &&
      poster.height in (height * 0.30).toInt()..(height * 0.76).toInt() &&
      posterAreaRatio in 0.20..0.65 &&
      target.centerX in (poster.right - width * 0.09).toInt()..(poster.right + width * 0.03).toInt() &&
      target.centerY >= poster.top - height * 0.07 &&
      target.bottom <= poster.top + height * 0.015 &&
      poster.top - target.bottom <= height * 0.05
  }
}

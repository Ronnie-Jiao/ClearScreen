package com.clearscreen.prototype.backend

/**
 * Pixel-independent geometry policy. Decisions require semantic evidence too;
 * no coordinate alone is ever sufficient to authorize an action.
 */
internal object AdGeometryRules {
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

  fun isSmallModalClose(target: Box, screen: Box): Boolean {
    val width = screen.width.coerceAtLeast(1)
    val height = screen.height.coerceAtLeast(1)
    return target.width > 0 && target.height > 0 &&
      target.width <= width * 0.18f &&
      target.height <= height * 0.10f
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
}

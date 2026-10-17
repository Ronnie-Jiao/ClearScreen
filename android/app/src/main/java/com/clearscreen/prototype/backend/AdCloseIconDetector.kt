package com.clearscreen.prototype.backend

import android.graphics.Bitmap
import android.graphics.Rect
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Recognizes the four diagonal strokes of an X inside an already exposed small
 * clickable control. This supplies visual evidence only; it never finds a tap
 * coordinate or authorizes closing a window without separate local ad evidence.
 */
internal object AdCloseIconDetector {
  fun findX(bitmap: Bitmap, target: Rect): Rect? {
    if (target.left < 0 || target.top < 0 || target.right > bitmap.width ||
      target.bottom > bitmap.height || target.width() < 14 || target.height() < 14
    ) return null
    val aspect = target.width().toDouble() / target.height()
    if (aspect !in 0.65..1.55) return null
    val width = target.width()
    val height = target.height()
    val pixels = IntArray(width * height)
    bitmap.getPixels(pixels, 0, width, target.left, target.top, width, height)
    fun luminance(x: Double, y: Double): Double {
      val color = pixels[y.roundToInt().coerceIn(0, height - 1) * width +
        x.roundToInt().coerceIn(0, width - 1)]
      return ((color shr 16 and 255) * 0.2126 + (color shr 8 and 255) * 0.7152 +
        (color and 255) * 0.0722)
    }
    fun patch(x: Double, y: Double, spread: Double): Double =
      (luminance(x, y) + luminance(x - spread, y) + luminance(x + spread, y) +
        luminance(x, y - spread) + luminance(x, y + spread)) / 5.0

    var bestScore = 0.0
    var best: Rect? = null
    val side = min(width, height).toDouble()
    for (offsetX in listOf(-0.07, 0.0, 0.07)) {
      for (offsetY in listOf(-0.07, 0.0, 0.07)) {
        val centerX = (width - 1) * (0.5 + offsetX)
        val centerY = (height - 1) * (0.5 + offsetY)
        for (fraction in listOf(0.17, 0.21, 0.25, 0.29, 0.33)) {
          val radius = side * fraction
          val spread = (radius * 0.035).coerceAtLeast(0.4)
          // Cardinal samples fall between the diagonal strokes, inside a circle
          // border. A circle alone or a plus sign must not count as an X.
          val background = listOf(
            patch(centerX + radius * 0.82, centerY, spread),
            patch(centerX - radius * 0.82, centerY, spread),
            patch(centerX, centerY + radius * 0.82, spread),
            patch(centerX, centerY - radius * 0.82, spread),
          )
          if (background.max() - background.min() > 65.0) continue
          val backgroundLevel = background.average()
          val arms = listOf(-1 to -1, -1 to 1, 1 to -1, 1 to 1).map { (x, y) ->
            listOf(0.35, 0.50, 0.65, 0.80, 0.95).map { distance ->
              patch(centerX + x * radius * distance, centerY + y * radius * distance, spread)
            }
          }
          val contrast = arms.flatten().average() - backgroundLevel
          if (abs(contrast) < 38.0) continue
          val direction = if (contrast > 0) 1.0 else -1.0
          if (arms.any { arm -> arm.count { (it - backgroundLevel) * direction >= 24.0 } < 4 }) continue
          if ((patch(centerX, centerY, spread) - backgroundLevel) * direction < 24.0) continue
          val score = arms.minOf { abs(it.average() - backgroundLevel) }
          if (score > bestScore) {
            bestScore = score
            best = Rect(
              target.left + (centerX - radius).roundToInt(),
              target.top + (centerY - radius).roundToInt(),
              target.left + (centerX + radius).roundToInt(),
              target.top + (centerY + radius).roundToInt(),
            )
          }
        }
      }
    }
    return best
  }
}

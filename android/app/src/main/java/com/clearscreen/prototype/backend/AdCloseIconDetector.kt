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
    if (aspect !in 0.55..2.0) return null
    val width = target.width()
    val height = target.height()
    val pixels = IntArray(width * height)
    bitmap.getPixels(pixels, 0, width, target.left, target.top, width, height)
    val levels = FloatArray(pixels.size) { index ->
      val color = pixels[index]
      ((color shr 16 and 255) * 0.2126 + (color shr 8 and 255) * 0.7152 +
        (color and 255) * 0.0722).toFloat()
    }
    fun luminance(x: Double, y: Double): Double {
      return levels[y.roundToInt().coerceIn(0, height - 1) * width +
        x.roundToInt().coerceIn(0, width - 1)].toDouble()
    }
    fun patch(x: Double, y: Double, spread: Double): Double =
      (luminance(x, y) + luminance(x - spread, y) + luminance(x + spread, y) +
        luminance(x, y - spread) + luminance(x, y + spread)) / 5.0

    var bestScore = 0.0
    var best: Rect? = null
    val side = min(width, height).toDouble()
    // The accessible touch area can be padded or rectangular while the drawn
    // glyph is tiny/off-centre. Search inside that real control only.
    val offsets = doubleArrayOf(-0.24, -0.12, 0.0, 0.12, 0.24)
    val fractions = doubleArrayOf(0.09, 0.12, 0.16, 0.21, 0.27, 0.33)
    val distances = doubleArrayOf(0.35, 0.50, 0.65, 0.80, 0.95)
    for (offsetX in offsets) {
      for (offsetY in offsets) {
        val centerX = (width - 1) * (0.5 + offsetX)
        val centerY = (height - 1) * (0.5 + offsetY)
        for (fraction in fractions) {
          val radius = side * fraction
          if (radius < 4.0 || centerX - radius < 0 || centerY - radius < 0 ||
            centerX + radius >= width || centerY + radius >= height
          ) continue
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
          val centerContrast = patch(centerX, centerY, spread) - backgroundLevel
          if (abs(centerContrast) < 20.0) continue
          val direction = if (centerContrast > 0) 1.0 else -1.0
          var score = Double.MAX_VALUE
          var valid = true
          for (arm in 0..3) {
            val x = if (arm < 2) -1 else 1
            val y = if (arm % 2 == 0) -1 else 1
            var contrastSum = 0.0
            var matched = 0
            for (distance in distances) {
              val contrast = (patch(centerX + x * radius * distance,
                centerY + y * radius * distance, spread) - backgroundLevel) * direction
              contrastSum += contrast
              if (contrast >= 18.0) matched++
            }
            val armContrast = contrastSum / distances.size
            if (matched < 4 || armContrast < 26.0) {
              valid = false
              break
            }
            score = min(score, armContrast)
          }
          if (!valid) continue
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

package com.clearscreen.prototype.backend

import android.graphics.Bitmap
import android.graphics.Rect
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Finds a closed, contrasting button background around an already recognized
 * action label. A text box alone, an open background or a large poster fails.
 * This supplies geometry only; the service must verify ad evidence and two frames.
 */
internal object AdVisualButtonDetector {
  /** Distributed texture is only a visual-surface hint, never ad evidence. */
  fun hasLargeCreative(bitmap: Bitmap, region: Rect): Boolean {
    if (region.width() < 16 || region.height() < 24 || region.left < 0 || region.top < 0 ||
      region.right > bitmap.width || region.bottom > bitmap.height
    ) return false
    var textured = 0
    val rows = mutableSetOf<Int>()
    val columns = mutableSetOf<Int>()
    for (row in 0 until 12) for (column in 0 until 8) {
      val colors = (0 until 9).map { sample ->
        bitmap.getPixel(
          region.left + ((column + (sample % 3 + 1) / 4.0) * region.width() / 8).toInt().coerceIn(0, region.width() - 1),
          region.top + ((row + (sample / 3 + 1) / 4.0) * region.height() / 12).toInt().coerceIn(0, region.height() - 1),
        )
      }
      if (listOf(0, 8, 16).any { shift ->
          colors.maxOf { it shr shift and 255 } - colors.minOf { it shr shift and 255 } >= 55
        }) {
        textured++
        rows += row
        columns += column
      }
    }
    return textured >= 34 && rows.size >= 7 && columns.size >= 5
  }

  fun findTextButton(bitmap: Bitmap, label: Rect): Rect? {
    if (label.width() <= 0 || label.height() < 8 || label.left < 0 || label.top < 0 ||
      label.right > bitmap.width || label.bottom > bitmap.height
    ) return null
    val h = label.height()
    val search = Rect(
      max(0, label.left - max(label.width(), h * 3)),
      max(0, label.top - h * 3),
      min(bitmap.width, label.right + max(label.width(), h * 3)),
      min(bitmap.height, label.bottom + h * 3),
    )
    val step = max(1, min(bitmap.width, bitmap.height) / 480)
    val w = search.width() / step
    val height = search.height() / step
    if (w < 5 || height < 5 || w.toLong() * height > 400_000) return null
    val pixels = IntArray(w * height)
    val scanRow = IntArray(search.width())
    for (y in 0 until height) {
      bitmap.getPixels(scanRow, 0, scanRow.size, search.left, search.top + y * step, scanRow.size, 1)
      for (x in 0 until w) pixels[y * w + x] = scanRow[x * step]
    }
    fun near(a: Int, b: Int, tolerance: Int = 34): Boolean =
      abs((a shr 16 and 255) - (b shr 16 and 255)) <= tolerance &&
        abs((a shr 8 and 255) - (b shr 8 and 255)) <= tolerance &&
        abs((a and 255) - (b and 255)) <= tolerance
    val samplePoints = listOf(
      label.left - h / 4 to label.centerY(), label.right + h / 4 to label.centerY(),
      label.centerX() to label.top - h / 4, label.centerX() to label.bottom + h / 4,
      label.left to label.top, label.right - 1 to label.top,
      label.left to label.bottom - 1, label.right - 1 to label.bottom - 1,
    )
    val seeds = samplePoints.mapNotNull { (x, y) ->
      val sx = (x - search.left) / step
      val sy = (y - search.top) / step
      if (sx !in 1 until w - 1 || sy !in 1 until height - 1) null else sy * w + sx
    }
    val seed = seeds.maxByOrNull { candidate -> seeds.count { near(pixels[candidate], pixels[it]) } } ?: return null
    if (seeds.count { near(pixels[seed], pixels[it]) } < 5) return null
    val visited = BooleanArray(pixels.size)
    val queue = IntArray(pixels.size)
    var head = 0
    var tail = 1
    queue[0] = seed
    visited[seed] = true
    var left = w
    var right = 0
    var top = height
    var bottom = 0
    while (head < tail) {
      val index = queue[head++]
      val x = index % w
      val y = index / w
      // An unbounded fill is the creative/page background, not a button.
      if (x == 0 || y == 0 || x == w - 1 || y == height - 1) return null
      left = min(left, x); right = max(right, x)
      top = min(top, y); bottom = max(bottom, y)
      for (direction in 0..3) {
        val next = when (direction) { 0 -> index - 1; 1 -> index + 1; 2 -> index - w; else -> index + w }
        if (!visited[next] && near(pixels[seed], pixels[next])) {
          visited[next] = true
          queue[tail++] = next
        }
      }
    }
    val result = Rect(
      search.left + left * step, search.top + top * step,
      search.left + (right + 1) * step, search.top + (bottom + 1) * step,
    )
    if (!result.contains(label) || result.width() < label.width() + h * 0.15 ||
      result.height() < h * 1.25 || result.height() > h * 4.5 ||
      result.width() > label.width() * 3.5 + h * 2 ||
      tail.toDouble() / ((right - left + 1) * (bottom - top + 1)) < 0.48
    ) return null
    val outer = listOf(
      result.left - step * 2 to result.centerY(), result.right + step * 2 to result.centerY(),
      result.centerX() to result.top - step * 2, result.centerX() to result.bottom + step * 2,
    )
    if (outer.count { (x, y) ->
        x in 0 until bitmap.width && y in 0 until bitmap.height && !near(pixels[seed], bitmap.getPixel(x, y), 24)
      } < 3
    ) return null
    return result
  }
}

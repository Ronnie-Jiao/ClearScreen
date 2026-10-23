package com.clearscreen.prototype.backend

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/** Retains only bounded numeric identities, never images, OCR text or UI nodes. */
internal class AdVisualAttemptGuard {
  private data class Observation(
    val key: String,
    val bounds: AdGeometryRules.Box,
    val evidence: Int,
    val frameAt: Long,
    val observedAt: Long,
  )
  private var previous: Observation? = null

  fun observe(key: String, bounds: AdGeometryRules.Box, evidence: Int, frameAt: Long, now: Long): Boolean {
    val prior = previous
    previous = Observation(key, bounds, evidence, frameAt, now)
    if (prior == null || prior.key != key || prior.evidence != evidence || frameAt <= prior.frameAt ||
      now - prior.observedAt !in 200L..3_000L
    ) return false
    val intersection = max(0, min(bounds.right, prior.bounds.right) - max(bounds.left, prior.bounds.left)).toLong() *
      max(0, min(bounds.bottom, prior.bounds.bottom) - max(bounds.top, prior.bounds.top))
    val union = bounds.width.toLong() * bounds.height + prior.bounds.width.toLong() * prior.bounds.height - intersection
    return union > 0 && intersection.toDouble() / union >= 0.78 &&
      abs(bounds.centerX - prior.bounds.centerX) <= max(2, bounds.height / 5) &&
      abs(bounds.centerY - prior.bounds.centerY) <= max(2, bounds.height / 5)
  }

  fun clear() { previous = null }
}

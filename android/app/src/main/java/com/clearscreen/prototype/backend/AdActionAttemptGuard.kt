package com.clearscreen.prototype.backend

/** One primary action and at most one verified touch fallback per observed control. */
internal class AdActionAttemptGuard(
  private val quietPeriodMs: Long = 30_000L,
  private val maxTargets: Int = 80,
) {
  private data class Attempt(
    var lastSeenAt: Long,
    var ineffective: Boolean = false,
    var touchAttempted: Boolean = false,
  )

  private val attempts = LinkedHashMap<String, Attempt>()

  /** Reserve before dispatch, including requests that Android subsequently rejects. */
  fun tryBegin(key: String, now: Long): Boolean {
    removeExpired(now)
    attempts[key]?.let {
      it.lastSeenAt = now
      return false
    }
    attempts[key] = Attempt(now)
    while (attempts.size > maxTargets) attempts.remove(attempts.entries.first().key)
    return true
  }

  fun markIneffective(key: String) {
    attempts[key]?.ineffective = true
  }

  /** The caller must independently revalidate the current window, OCR and real node. */
  fun tryBeginTouchFallback(key: String, now: Long): Boolean {
    removeExpired(now)
    val attempt = attempts[key] ?: return false
    attempt.lastSeenAt = now
    if (!attempt.ineffective || attempt.touchAttempted) return false
    attempt.touchAttempted = true
    return true
  }

  fun clear() = attempts.clear()

  private fun removeExpired(now: Long) {
    attempts.entries.removeAll { now - it.value.lastSeenAt >= quietPeriodMs }
  }
}

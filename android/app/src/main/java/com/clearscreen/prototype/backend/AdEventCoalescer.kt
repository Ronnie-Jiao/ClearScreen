package com.clearscreen.prototype.backend

/** Collapse noisy content events without delaying the already scheduled scan. */
internal class AdEventCoalescer(private val minimumIntervalMs: Long = 350L) {
  data class Event(val packageName: String, val windowChanged: Boolean)
  private var pending: Event? = null
  private var lastScanAt = -minimumIntervalMs

  fun offer(packageName: String, windowChanged: Boolean) {
    val previous = pending
    pending = Event(packageName, windowChanged ||
      (previous?.packageName == packageName && previous.windowChanged))
  }

  fun delay(now: Long): Long = (lastScanAt + minimumIntervalMs - now).coerceAtLeast(0L)

  fun take(now: Long): Event? {
    val event = pending ?: return null
    pending = null
    lastScanAt = now
    return event
  }

  fun clear() { pending = null }
}

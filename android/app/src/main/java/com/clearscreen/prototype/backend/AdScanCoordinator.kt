package com.clearscreen.prototype.backend

/** Bounded rescans; content events cannot keep extending the same window's deadline. */
internal class AdScanCoordinator {
  class Ticket internal constructor(
    val packageName: String,
    val windowId: Int,
    internal val startedAt: Long,
    internal var extended: Boolean,
  ) {
    internal var nextAttempt = 0
  }

  private var active: Ticket? = null

  fun begin(packageName: String, windowId: Int, now: Long, waitForLateControls: Boolean): Ticket? {
    val previous = active
    if (previous?.packageName == packageName && previous.windowId == windowId) {
      previous.extended = previous.extended || waitForLateControls
      return null
    }
    return Ticket(packageName, windowId, now, waitForLateControls).also { active = it }
  }

  fun isCurrent(ticket: Ticket): Boolean = active === ticket

  fun cancel(ticket: Ticket? = null) {
    if (ticket == null || active === ticket) active = null
  }

  fun nextDelay(ticket: Ticket, now: Long): Long? {
    if (!isCurrent(ticket)) return null
    val delays = if (ticket.extended) EXTENDED_DELAYS_MS else SHORT_DELAYS_MS
    val offset = delays.getOrNull(ticket.nextAttempt++) ?: run {
      cancel(ticket)
      return null
    }
    // Drop a suspended/late session rather than replaying all overdue scans.
    if (now - ticket.startedAt > delays.last() + 1_000L) {
      cancel(ticket)
      return null
    }
    return (ticket.startedAt + offset - now).coerceAtLeast(0L)
  }

  companion object {
    private val SHORT_DELAYS_MS = longArrayOf(240L, 600L, 1_000L, 1_800L)
    private val EXTENDED_DELAYS_MS = SHORT_DELAYS_MS + longArrayOf(3_000L, 4_600L, 6_500L, 8_500L, 11_000L)
  }
}

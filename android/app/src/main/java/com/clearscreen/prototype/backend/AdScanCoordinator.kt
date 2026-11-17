package com.clearscreen.prototype.backend

/** Bounded rescans; content events cannot keep extending the same window's deadline. */
internal class AdScanCoordinator {
  class Ticket internal constructor(
    val packageName: String,
    val windowId: Int,
    internal val startedAt: Long,
    internal var extended: Boolean,
    internal var startup: Boolean,
  ) {
    internal var nextAttempt = 0
  }

  private var active: Ticket? = null

  fun begin(
    packageName: String, windowId: Int, now: Long, waitForLateControls: Boolean,
    startup: Boolean = false,
  ): Ticket? {
    val previous = active
    if (previous?.packageName == packageName && previous.windowId == windowId) {
      previous.extended = previous.extended || waitForLateControls
      previous.startup = previous.startup || startup
      return null
    }
    return Ticket(packageName, windowId, now, waitForLateControls, startup).also { active = it }
  }

  /** A fixed window-entry budget; noisy content events never restart its clock. */
  fun isStartup(packageName: String, now: Long, windowId: Int? = null): Boolean = active?.let {
    it.startup && it.packageName == packageName && (windowId == null || it.windowId == windowId) &&
      now - it.startedAt in 0L..STARTUP_BUDGET_MS
  } == true

  fun isCurrent(ticket: Ticket): Boolean = active === ticket

  fun cancel(ticket: Ticket? = null) {
    if (ticket == null || active === ticket) active = null
  }

  fun nextDelay(ticket: Ticket, now: Long): Long? {
    if (!isCurrent(ticket)) return null
    val delays = when {
      ticket.startup -> if (ticket.extended) STARTUP_EXTENDED_DELAYS_MS else STARTUP_DELAYS_MS
      ticket.extended -> EXTENDED_DELAYS_MS
      else -> SHORT_DELAYS_MS
    }
    if (ticket.startup) {
      // A slow system tree read must not trigger a burst of already overdue
      // startup scans in front of the OCR result that is ready to act.
      while (ticket.nextAttempt < delays.size && delays[ticket.nextAttempt] < now - ticket.startedAt) {
        ticket.nextAttempt++
      }
    }
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
    private const val STARTUP_BUDGET_MS = 3_000L
    private val SHORT_DELAYS_MS = longArrayOf(240L, 600L, 1_000L, 1_800L)
    private val STARTUP_DELAYS_MS = longArrayOf(120L, 360L, 720L, 1_120L, 1_600L, 2_200L, 3_000L)
    private val LATE_DELAYS_MS = longArrayOf(4_600L, 6_500L, 8_500L, 11_000L)
    private val EXTENDED_DELAYS_MS = SHORT_DELAYS_MS + longArrayOf(3_000L) + LATE_DELAYS_MS
    private val STARTUP_EXTENDED_DELAYS_MS = STARTUP_DELAYS_MS + LATE_DELAYS_MS
  }
}

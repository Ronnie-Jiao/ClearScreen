package com.clearscreen.prototype.backend

import org.junit.Assert.*
import org.junit.Test

class AdEventCoalescerTest {
  @Test fun aFloodProducesOneScanAndKeepsTheWindowEntrySignal() {
    val events = AdEventCoalescer()
    events.offer("weather", true)
    repeat(200) { events.offer("weather", false) }
    assertEquals(AdEventCoalescer.Event("weather", true), events.take(0L))
    assertNull(events.take(1L))
  }

  @Test fun newEventsDoNotPostponeTheAlreadyScheduledScan() {
    val events = AdEventCoalescer()
    events.offer("weather", false)
    events.take(100L)
    events.offer("weather", false)
    assertEquals(300L, events.delay(150L))
    events.offer("weather", false)
    assertEquals(50L, events.delay(400L))
    assertEquals(0L, events.delay(450L))
  }

  @Test fun packageChangesCannotBorrowThePreviousWindowEntry() {
    val events = AdEventCoalescer()
    events.offer("weather", true)
    events.offer("other", false)
    assertEquals(AdEventCoalescer.Event("other", false), events.take(0L))
  }

  @Test fun stoppedWorkDoesNotLeaveAPendingEvent() {
    val events = AdEventCoalescer()
    events.offer("weather", true)
    events.clear()
    assertNull(events.take(0L))
  }
}

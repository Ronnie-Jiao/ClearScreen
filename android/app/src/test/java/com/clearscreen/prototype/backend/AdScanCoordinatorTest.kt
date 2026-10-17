package com.clearscreen.prototype.backend

import org.junit.Assert.*
import org.junit.Test

class AdScanCoordinatorTest {
  @Test fun waitsForAnAdControlThatAppearsAfterTheFirstSecond() {
    val coordinator = AdScanCoordinator()
    val ticket = coordinator.begin("weather", 1, 0L, true)!!
    var now = 0L
    var foundLateControl = false
    while (true) {
      val delay = coordinator.nextDelay(ticket, now) ?: break
      now += delay
      if (now >= 5_000L) foundLateControl = true
    }
    assertTrue(foundLateControl)
    assertEquals(11_000L, now)
    assertFalse(coordinator.isCurrent(ticket))
  }

  @Test fun ordinaryContentHasARescanAfterTheOcrThrottleButDoesNotKeepPolling() {
    val coordinator = AdScanCoordinator()
    val ticket = coordinator.begin("weather", 1, 0L, false)!!
    var now = 0L
    while (true) now += coordinator.nextDelay(ticket, now) ?: break
    assertTrue(now > 1_200L)
    assertTrue(now < 2_000L)
  }

  @Test fun repeatingContentEventsDoNotMoveTheOriginalDeadline() {
    val coordinator = AdScanCoordinator()
    val ticket = coordinator.begin("weather", 1, 100L, true)!!
    var now = 100L
    while (true) {
      val delay = coordinator.nextDelay(ticket, now) ?: break
      now += delay
      assertNull(coordinator.begin("weather", 1, now, true))
    }
    assertEquals(11_100L, now)
  }

  @Test fun discoveringAdEvidenceExtendsTheExistingSessionWithoutRestartingIt() {
    val coordinator = AdScanCoordinator()
    val ticket = coordinator.begin("weather", 1, 0L, false)!!
    assertEquals(240L, coordinator.nextDelay(ticket, 0L))
    assertNull(coordinator.begin("weather", 1, 240L, true))
    var now = 240L
    while (true) now += coordinator.nextDelay(ticket, now) ?: break
    assertEquals(11_000L, now)
  }

  @Test fun oldCallbacksCannotActOnAnotherPackageOrAnotherWindow() {
    val coordinator = AdScanCoordinator()
    val first = coordinator.begin("weather", 1, 0L, true)!!
    val second = coordinator.begin("mobile", 2, 300L, true)!!
    assertNull(coordinator.nextDelay(first, 600L))
    coordinator.cancel(first)
    assertTrue(coordinator.isCurrent(second))
    val third = coordinator.begin("mobile", 3, 700L, true)!!
    assertFalse(coordinator.isCurrent(second))
    assertTrue(coordinator.isCurrent(third))
  }

  @Test fun cancellationAndSuspensionDoNotLeaveAReplayOfOverdueScans() {
    val coordinator = AdScanCoordinator()
    val cancelled = coordinator.begin("weather", 1, 0L, true)!!
    coordinator.cancel()
    assertNull(coordinator.nextDelay(cancelled, 240L))
    val suspended = coordinator.begin("weather", 1, 500L, true)!!
    assertNull(coordinator.nextDelay(suspended, 20_000L))
  }
}

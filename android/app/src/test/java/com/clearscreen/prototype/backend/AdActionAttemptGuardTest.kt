package com.clearscreen.prototype.backend

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdActionAttemptGuardTest {
  @Test fun rejectedRequestsStillConsumeThePrimaryAttempt() {
    val guard = AdActionAttemptGuard()
    assertTrue(guard.tryBegin("same-window-and-control", 1_000L))
    guard.markIneffective("same-window-and-control")
    assertFalse(guard.tryBegin("same-window-and-control", 1_500L))
    assertFalse(guard.tryBegin("same-window-and-control", 3_000L))
  }

  @Test fun aTouchFallbackRequiresAnIneffectivePrimaryAndCanOnlyRunOnce() {
    val guard = AdActionAttemptGuard()
    assertFalse(guard.tryBeginTouchFallback("skip", 1_000L))
    assertTrue(guard.tryBegin("skip", 1_100L))
    assertFalse(guard.tryBeginTouchFallback("skip", 1_200L))
    guard.markIneffective("skip")
    assertTrue(guard.tryBeginTouchFallback("skip", 1_800L))
    guard.markIneffective("skip")
    assertFalse(guard.tryBeginTouchFallback("skip", 3_500L))
    assertFalse(guard.tryBegin("skip", 5_000L))
  }

  @Test fun continuingRescansDoNotRestartARepeatedClickLoop() {
    val guard = AdActionAttemptGuard(quietPeriodMs = 30_000L)
    assertTrue(guard.tryBegin("skip", 1_000L))
    assertFalse(guard.tryBegin("skip", 20_000L))
    assertFalse(guard.tryBegin("skip", 40_000L))
    assertFalse(guard.tryBegin("skip", 60_000L))
    assertTrue(guard.tryBegin("skip", 90_001L))
    assertFalse(guard.tryBeginTouchFallback("skip", 90_002L))
  }

  @Test fun aDifferentWindowOrControlHasItsOwnBudget() {
    val guard = AdActionAttemptGuard()
    assertTrue(guard.tryBegin("package|window1|skip", 1_000L))
    assertTrue(guard.tryBegin("package|window2|skip", 1_001L))
    assertTrue(guard.tryBegin("package|window1|close", 1_002L))
    assertFalse(guard.tryBegin("package|window1|skip", 1_003L))
  }

  @Test fun serviceResetAndBoundedHistoryReleaseOldEntries() {
    val guard = AdActionAttemptGuard(maxTargets = 2)
    assertTrue(guard.tryBegin("first", 1_000L))
    assertTrue(guard.tryBegin("second", 1_001L))
    assertTrue(guard.tryBegin("third", 1_002L))
    assertTrue(guard.tryBegin("first", 1_003L))
    guard.clear()
    assertTrue(guard.tryBegin("first", 1_004L))
    assertFalse(guard.tryBeginTouchFallback("first", 1_005L))
  }
}

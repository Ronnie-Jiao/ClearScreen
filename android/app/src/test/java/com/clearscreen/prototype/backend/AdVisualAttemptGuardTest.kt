package com.clearscreen.prototype.backend

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdVisualAttemptGuardTest {
  private val box = AdGeometryRules.Box(300, 600, 560, 700)

  @Test fun requiresTwoDifferentFreshFrames() {
    val guard = AdVisualAttemptGuard()
    assertFalse(guard.observe("window", box, 42, 1_000, 1_100))
    assertFalse(guard.observe("window", box, 42, 1_000, 1_400))
    assertTrue(guard.observe("window", box, 42, 1_700, 1_800))
  }

  @Test fun aMovedButtonChangedWindowOrChangedEvidenceMustBeObservedAgain() {
    for (change in listOf("position", "window", "evidence")) {
      val guard = AdVisualAttemptGuard()
      assertFalse(guard.observe("window", box, 42, 1_000, 1_100))
      assertFalse(guard.observe(if (change == "window") "other" else "window",
        if (change == "position") AdGeometryRules.Box(600, 1200, 860, 1300) else box,
        if (change == "evidence") 43 else 42, 1_700, 1_800))
    }
  }

  @Test fun staleOrClearedObservationsDoNotAuthorizeATouch() {
    val guard = AdVisualAttemptGuard()
    assertFalse(guard.observe("window", box, 42, 1_000, 1_100))
    assertFalse(guard.observe("window", box, 42, 5_000, 5_100))
    guard.clear()
    assertFalse(guard.observe("window", box, 42, 5_700, 5_800))
  }
}

package com.clearscreen.prototype.backend

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

/** Foreground-only, read-only check. Never clicks, screenshots, or reads node text. */
object ClearScreenAccessibilityHealth {
  const val READY = "ready"
  const val UNAVAILABLE = "unavailable"
  const val UNKNOWN = "unknown"
  private const val FRESH_MS = 8_000L
  private val lock = Any()
  private val main = Handler(Looper.getMainLooper())
  private val worker = Executors.newSingleThreadExecutor { task ->
    Thread(task, "clearscreen-health").apply { isDaemon = true }
  }
  private var service: ClearScreenAccessibilityService? = null
  private var revision = 0L
  private var checkedAt = 0L
  private var status = UNKNOWN
  private var pending: Future<String>? = null
  private var eventReply: CountDownLatch? = null

  fun connected(value: ClearScreenAccessibilityService) = synchronized(lock) {
    service = value
    invalidateLocked()
  }

  fun disconnected(value: ClearScreenAccessibilityService) = synchronized(lock) {
    if (service === value) {
      service = null
      invalidateLocked()
    }
  }

  fun invalidate() = synchronized(lock) { invalidateLocked() }

  private fun invalidateLocked() {
    revision++
    checkedAt = 0L
    status = UNKNOWN
    eventReply?.countDown()
    eventReply = null
  }

  fun onEvent(value: ClearScreenAccessibilityService, event: AccessibilityEvent) {
    // A fresh event from our own view exercises Android's event delivery path.
    // No event source, text, screenshot, or other application's content is used.
    if (event.eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED ||
      event.packageName?.toString() != value.packageName) return
    synchronized(lock) { if (service === value) eventReply?.countDown() }
  }

  fun current(): String = synchronized(lock) {
    if (service == null) UNAVAILABLE
    else if (checkedAt == 0L || SystemClock.elapsedRealtime() - checkedAt > FRESH_MS) UNKNOWN
    else status
  }

  /** Called off the UI thread; a stuck framework call never queues more probes. */
  fun check(activity: Activity?): String {
    if (activity == null || Looper.myLooper() == Looper.getMainLooper()) return UNKNOWN
    val task: Future<String>
    synchronized(lock) {
      val live = service ?: return UNAVAILABLE
      if (!ClearScreenPrivacyConsentStore(live).hasCurrentConsent()) return UNKNOWN
      if (pending?.isDone == false) {
        status = UNKNOWN
        checkedAt = 0L
        return UNKNOWN
      }
      val expectedRevision = revision
      val startedAt = SystemClock.elapsedRealtime()
      task = worker.submit<String> {
        val result = runCatching { probe(live, activity, expectedRevision) }.getOrDefault(UNKNOWN)
        synchronized(lock) {
          if (service !== live || revision != expectedRevision ||
            SystemClock.elapsedRealtime() - startedAt > 1_800L ||
            !ClearScreenPrivacyConsentStore(live).hasCurrentConsent()) UNKNOWN
          else {
            status = result
            checkedAt = startedAt
            result
          }
        }
      }
      pending = task
    }
    return runCatching { task.get(1_800L, TimeUnit.MILLISECONDS) }.getOrElse {
      // Do not cancel/requeue blocking Binder reads or retain an older green result.
      synchronized(lock) { status = UNKNOWN; checkedAt = 0L }
      UNKNOWN
    }
  }

  private fun probe(live: ClearScreenAccessibilityService, activity: Activity, expectedRevision: Long): String {
    if (live.isOcrStalled()) return UNKNOWN
    val info = live.serviceInfo ?: return UNAVAILABLE
    if (info.capabilities and AccessibilityServiceInfo.CAPABILITY_CAN_RETRIEVE_WINDOW_CONTENT == 0 ||
      info.capabilities and AccessibilityServiceInfo.CAPABILITY_CAN_PERFORM_GESTURES == 0 ||
      info.eventTypes and AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED == 0) return UNAVAILABLE
    val posted = CountDownLatch(1)
    val reply = CountDownLatch(1)
    var focused = false
    val ping = Runnable {
      try {
        synchronized(lock) {
          if (service !== live || revision != expectedRevision ||
            !ClearScreenPrivacyConsentStore(live).hasCurrentConsent()) return@Runnable
          val view = activity.window?.decorView
          focused = view?.hasWindowFocus() == true && view.isShown
          if (focused) {
            eventReply = reply
            view?.sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED)
          }
        }
      } finally { posted.countDown() }
    }
    main.post(ping)
    try {
      if (!posted.await(500L, TimeUnit.MILLISECONDS)) return UNKNOWN
      if (!focused) return UNKNOWN // lock screen, notification shade, or window transition
      if (!reply.await(700L, TimeUnit.MILLISECONDS)) return UNKNOWN
      synchronized(lock) { if (service !== live || revision != expectedRevision) return UNKNOWN }
      if (!ClearScreenPrivacyConsentStore(live).hasCurrentConsent()) return UNKNOWN
      val root = live.rootInActiveWindow ?: return UNKNOWN
      try {
        // refresh() goes back to the live view instead of trusting a cached node.
        // A replaced/obscured root is inconclusive, not proof that the service died.
        if (root.packageName?.toString() != live.packageName || !root.refresh() ||
          root.packageName?.toString() != live.packageName) return UNKNOWN
        return READY
      } finally {
        @Suppress("DEPRECATION")
        root.recycle()
      }
    } finally {
      main.removeCallbacks(ping)
      synchronized(lock) { if (eventReply === reply) eventReply = null }
    }
  }
}

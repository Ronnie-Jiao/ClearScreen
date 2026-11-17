package com.clearscreen.prototype.backend

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import java.util.concurrent.Executors

/** A bounded foreground repair of an existing grant, never a new authorization. */
internal object ClearScreenAccessibilityRecovery {
  private const val TAG = "ClearScreenRecovery"
  private const val BIND_GRACE_MS = 1_500L
  private const val RETRY_COOLDOWN_MS = 30_000L
  private val main = Handler(Looper.getMainLooper())
  private val worker = Executors.newSingleThreadExecutor { task ->
    Thread(task, "clearscreen-rebind").apply { isDaemon = true }
  }
  @Volatile private var foreground = false
  @Volatile private var generation = 0L
  @Volatile private var pending = false
  @Volatile private var attemptInFlight = false
  @Volatile var isRebinding = false
    private set
  private var lastAttemptAt = 0L
  private var check: Runnable? = null

  val isPending: Boolean get() = pending || attemptInFlight

  fun hasGrant(context: Context): Boolean = services(context).any { isOwnService(context, it) }

  fun onForeground(context: Context) {
    val app = context.applicationContext
    foreground = true
    val session = ++generation
    check?.let(main::removeCallbacks)
    check = null
    ClearScreenRuntimeService.sync(app)
    pending = false
    if (attemptInFlight) return
    if (!eligible(app) || ClearScreenAccessibilityService.running) return
    pending = true
    check = Runnable {
      check = null
      if (!foreground || generation != session || !eligible(app) || ClearScreenAccessibilityService.running) {
        pending = false
        return@Runnable
      }
      val now = SystemClock.elapsedRealtime()
      if ((lastAttemptAt != 0L && now - lastAttemptAt < RETRY_COOLDOWN_MS) ||
        app.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) != PackageManager.PERMISSION_GRANTED) {
        pending = false
        return@Runnable
      }
      lastAttemptAt = now
      pending = false
      attemptInFlight = true
      worker.execute {
        try {
          if (foreground && generation == session && eligible(app) && !ClearScreenAccessibilityService.running) {
            reconnectOwnGrant(app, session)
          }
        } catch (error: RuntimeException) {
          Log.w(TAG, "rebind unavailable: ${error.javaClass.simpleName}")
        } finally {
          attemptInFlight = false
          main.post {
            ClearScreenAccessibilityHealth.invalidate()
            ClearScreenRuntimeService.sync(app)
          }
        }
      }
    }.also { main.postDelayed(it, BIND_GRACE_MS) }
  }

  fun onBackground() {
    foreground = false
    generation++
    check?.let(main::removeCallbacks)
    check = null
    pending = false
    // If the provider write has begun, its finally block restores our own entry.
  }

  fun onConnected() {
    check?.let(main::removeCallbacks)
    check = null
    pending = false
  }

  private fun eligible(context: Context): Boolean = runCatching {
    ClearScreenPrivacyConsentStore(context).hasCurrentConsent() &&
      ClearScreenStore(context).isMasterEnabled() && hasGrant(context)
  }.getOrDefault(false)

  private fun services(context: Context): List<String> = Settings.Secure.getString(
    context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
  ).orEmpty().split(':').filter(String::isNotBlank)

  private fun isOwnService(context: Context, entry: String): Boolean {
    val saved = ComponentName.unflattenFromString(entry) ?: return false
    val own = ComponentName(context, ClearScreenAccessibilityService::class.java)
    return saved.packageName.equals(own.packageName, ignoreCase = true) &&
      saved.className.equals(own.className, ignoreCase = true)
  }

  private fun reconnectOwnGrant(context: Context, session: Long) {
    // WRITE_SECURE_SETTINGS is optional, already granted through the user's ADB
    // setup. Without it we leave Android's settings untouched and retain manual repair.
    // Never restore an entry remembered from an earlier launch or absent now.
    val before = services(context)
    if (before.none { isOwnService(context, it) } || ClearScreenAccessibilityService.running) return
    val withoutOwn = before.filterNot { isOwnService(context, it) }
    var removed = false
    var interrupted = false
    isRebinding = true
    try {
      if (!foreground || generation != session || !eligible(context) || services(context) != before) return
      removed = Settings.Secure.putString(context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, withoutOwn.joinToString(":"))
      if (!removed) return
      // Give AccessibilityManager's settings observer a turn to discard its dead
      // connection. Keep this short and off the UI/recognition threads.
      try { Thread.sleep(250L) } catch (_: InterruptedException) { interrupted = true }
    } finally {
      try {
        if (removed) {
          val current = services(context)
          // Another settings writer/user took over: do not overwrite that change.
          // Other accessibility components and the global switch are never toggled.
          if (current == withoutOwn) {
            val restored = Settings.Secure.putString(context.contentResolver,
              Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, before.joinToString(":"))
            Log.i(TAG, "existing grant reconnect requested=$restored")
          } else Log.i(TAG, "reconnect stopped after external settings change")
        }
      } finally {
        isRebinding = false
        if (interrupted) Thread.currentThread().interrupt()
      }
    }
  }
}

package com.clearscreen.prototype.backend

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.database.ContentObserver
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.Log
import com.clearscreen.prototype.MainActivity
import com.clearscreen.prototype.R

/** Keeps the user-enabled ad assistance alive independently of the Activity task. */
class ClearScreenRuntimeService : Service() {
  private val handler = Handler(Looper.getMainLooper())
  private var stopObservingMaster: (() -> Unit)? = null
  private var observingGrant = false
  private var foregroundStarted = false
  private val grantObserver = object : ContentObserver(handler) {
    override fun onChange(selfChange: Boolean) { refresh() }
  }

  override fun onCreate() {
    super.onCreate()
    // A sticky restart must also meet Android's foreground-notification deadline.
    // Promote before performing settings reads; a stale start is stopped below.
    foregroundStarted = runCatching {
      val manager = getSystemService(NotificationManager::class.java)
      if (Build.VERSION.SDK_INT >= 26) {
        manager.createNotificationChannel(NotificationChannel(
          CHANNEL_ID, getString(R.string.runtime_channel_name), NotificationManager.IMPORTANCE_LOW,
        ).apply { setShowBadge(false) })
      }
      if (Build.VERSION.SDK_INT >= 34) {
        startForeground(NOTIFICATION_ID, notification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
      } else startForeground(NOTIFICATION_ID, notification())
      true
    }.getOrElse {
      Log.w(TAG, "foreground unavailable: ${it.javaClass.simpleName}")
      false
    }
    if (!foregroundStarted) {
      stopSelf()
      return
    }
    stopObservingMaster = ClearScreenStore(this).observeMasterEnabled { handler.post { refresh() } }
    observingGrant = runCatching {
      contentResolver.registerContentObserver(
        Settings.Secure.getUriFor(Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES), false, grantObserver,
      )
      true
    }.getOrDefault(false)
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    if (!foregroundStarted || !shouldRun(this)) {
      stopSelf()
      return START_NOT_STICKY
    }
    refresh()
    // Android can recreate the notification host after ordinary process reclamation.
    // Only the system binds AccessibilityService; this is not a substitute for it.
    return START_STICKY
  }

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onTaskRemoved(rootIntent: Intent?) {
    // Removing the UI task must not stop the user's active background feature.
    Log.i(TAG, "UI task removed; background assistance retained")
    super.onTaskRemoved(rootIntent)
  }

  private fun refresh() {
    if (!shouldRun(this)) {
      stopSelf()
      return
    }
    if (foregroundStarted) runCatching {
      getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification())
    }
  }

  private fun notification(): Notification {
    val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java),
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    val builder = if (Build.VERSION.SDK_INT >= 26) Notification.Builder(this, CHANNEL_ID)
      else { @Suppress("DEPRECATION") Notification.Builder(this) }
    return builder.setSmallIcon(R.drawable.ic_runtime_status)
      .setContentTitle(getString(R.string.runtime_notification_title))
      .setContentText(getString(if (ClearScreenAccessibilityService.running)
        R.string.runtime_notification_active else R.string.runtime_notification_connecting))
      .setContentIntent(open)
      .setCategory(Notification.CATEGORY_SERVICE)
      .setOngoing(true)
      .setOnlyAlertOnce(true)
      .setShowWhen(false)
      .setVisibility(Notification.VISIBILITY_PUBLIC)
      .build()
  }

  override fun onDestroy() {
    stopObservingMaster?.invoke()
    stopObservingMaster = null
    if (observingGrant) runCatching { contentResolver.unregisterContentObserver(grantObserver) }
    handler.removeCallbacksAndMessages(null)
    if (foregroundStarted) stopForeground(STOP_FOREGROUND_REMOVE)
    foregroundStarted = false
    // Do not restart from here: respect pause, revoked grants, and system force-stop.
    super.onDestroy()
  }

  companion object {
    private const val TAG = "ClearScreenRuntime"
    private const val CHANNEL_ID = "clearscreen_runtime"
    private const val NOTIFICATION_ID = 1001

    private fun shouldRun(context: Context): Boolean = runCatching {
      ClearScreenPrivacyConsentStore(context).hasCurrentConsent() && ClearScreenStore(context).isMasterEnabled() &&
        (ClearScreenAccessibilityRecovery.hasGrant(context) || ClearScreenAccessibilityRecovery.isRebinding)
    }.getOrDefault(false)

    /** Called after connection, on foreground entry, and when the user changes the master switch. */
    internal fun sync(context: Context) {
      val app = context.applicationContext
      val intent = Intent(app, ClearScreenRuntimeService::class.java)
      runCatching {
        if (!shouldRun(app)) app.stopService(intent)
        else if (Build.VERSION.SDK_INT >= 26) app.startForegroundService(intent)
        else app.startService(intent)
      }.onFailure {
        // Background-start restrictions must never crash the accessibility process.
        // The next visible Activity entry retries the foreground promotion.
        Log.w(TAG, "runtime start deferred: ${it.javaClass.simpleName}")
      }
    }
  }
}

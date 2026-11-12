package com.clearscreen.prototype.backend

import android.app.AppOpsManager
import android.content.Context
import android.net.Uri
import android.os.CancellationSignal
import java.util.concurrent.Executors
import java.util.concurrent.FutureTask
import java.util.concurrent.TimeUnit

/** Read only this application's OEM state. Unknown is never treated as a grant. */
internal class ClearScreenPermissionReader(private val context: Context) {
  private val executor = Executors.newSingleThreadExecutor()
  private val profile = ClearScreenOemProfile.current()
  private val lock = Any()
  private var pending: Pair<FutureTask<Boolean?>, CancellationSignal>? = null

  fun startupAllowed(): Boolean? {
    // A just-confirmed switch from the user-guided OEM settings page reflects
    // the visible, current row. Prefer this session-bound evidence over
    // Xiaomi's broader private query if the two results disagree.
    ClearScreenSettingsObserver.value(ClearScreenSettingsObserver.Kind.STARTUP)?.let { return it }
    if (profile != ClearScreenOemProfile.VIVO && profile != ClearScreenOemProfile.XIAOMI) {
      return ClearScreenSettingsObserver.value(ClearScreenSettingsObserver.Kind.STARTUP)
    }

    // Share an in-flight read with full snapshots. A stalled OEM provider must
    // neither delay the other permission states nor spawn unbounded workers.
    val query = synchronized(lock) {
      pending ?: run {
        val cancellation = CancellationSignal()
        val task = FutureTask<Boolean?> {
          try {
            when (profile) {
              ClearScreenOemProfile.VIVO -> readVivoStartup(cancellation)
              ClearScreenOemProfile.XIAOMI -> readXiaomiStartup()
              else -> null
            }
          } finally {
            synchronized(lock) { pending = null }
          }
        }
        (task to cancellation).also {
          pending = it
          executor.execute(task)
        }
      }
    }
    val queried = try {
      query.first.get(1_200, TimeUnit.MILLISECONDS)
    } catch (_: Exception) {
      query.second.cancel()
      query.first.cancel(true)
      null
    }
    // A page observation may have completed while the private query was in
    // flight. Recheck it so the current visible switch still takes priority.
    return ClearScreenSettingsObserver.value(ClearScreenSettingsObserver.Kind.STARTUP) ?: queried
  }

  private fun readXiaomiStartup(): Boolean? = runCatching {
    // MIUI/HyperOS keeps the autostart switch in AppOps op 10008 ("MIUIOP(10008)"
    // in `appops get`), which Security Center writes. Only the numeric overload
    // reads it, and the public API exposes string ops only, so call the numeric
    // overload reflectively - that is exactly what MIUI's own
    // android.miui.AppOpsUtils.getApplicationAutoStart does; that helper class
    // itself is hiddenapi-blocked for third-party apps.
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
      ?: return@runCatching null
    val check = AppOpsManager::class.java.getMethod(
      "checkOpNoThrow", Integer.TYPE, Integer.TYPE, String::class.java,
    )
    when (check.invoke(appOps, MIUI_AUTO_START_OP, context.applicationInfo.uid, context.packageName) as? Int) {
      AppOpsManager.MODE_ALLOWED -> true
      AppOpsManager.MODE_IGNORED -> false
      else -> null
    }
  }.getOrNull()

  private fun readVivoStartup(cancellation: CancellationSignal): Boolean? = runCatching {
    // PermissionManager 15.16.0.6.4: PermissionProvider.query restricts ordinary
    // callers to their own package. BgStartUpUtil maps 0 = allowed, 1 = denied.
    // Do not use the protected appfilter provider or infer a grant from no row.
    context.contentResolver.query(
      Uri.parse("content://com.vivo.permissionmanager.provider.permission/bg_start_up_apps"),
      arrayOf("pkgname", "pkguid", "currentstate"),
      "pkgname = ?",
      arrayOf(context.packageName),
      null,
      cancellation,
    )?.use { cursor ->
      if (!cursor.moveToFirst() || cursor.count != 1) return@use null
      val name = cursor.getColumnIndexOrThrow("pkgname")
      val uid = cursor.getColumnIndexOrThrow("pkguid")
      val state = cursor.getColumnIndexOrThrow("currentstate")
      if (cursor.isNull(name) || cursor.isNull(uid) || cursor.isNull(state) ||
        cursor.getString(name) != context.packageName ||
        cursor.getInt(uid) != context.applicationInfo.uid) return@use null
      when (cursor.getInt(state)) {
        0 -> true
        1 -> false
        else -> null
      }
    }
  }.getOrNull()

  fun close() {
    synchronized(lock) {
      pending?.second?.cancel()
      pending?.first?.cancel(true)
    }
    executor.shutdownNow()
  }

  private companion object {
    /** MIUI/HyperOS autostart AppOp; the framework helper reads this same code. */
    const val MIUI_AUTO_START_OP = 10008
  }
}

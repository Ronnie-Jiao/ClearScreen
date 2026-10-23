package com.clearscreen.prototype.backend

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
    return queried ?: ClearScreenSettingsObserver.value(ClearScreenSettingsObserver.Kind.STARTUP)
  }

  private fun readXiaomiStartup(): Boolean? = runCatching {
    // Interoperability selector documented by XomaDev/MIUI-Autostart. Use no
    // HiddenApiBypass, setAccessible, default-true value, or hard-coded AppOp.
    // A restricted/missing MIUI or HyperOS interface simply remains unknown.
    val api = Class.forName("android.miui.AppOpsUtils")
      .getMethod("getApplicationAutoStart", Context::class.java, String::class.java)
    when (api.invoke(null, context, context.packageName) as? Int) {
      0 -> true
      1 -> false
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
}

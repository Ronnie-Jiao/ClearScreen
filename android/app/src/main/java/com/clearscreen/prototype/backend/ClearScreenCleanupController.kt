package com.clearscreen.prototype.backend

import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReadableArray
import com.facebook.react.bridge.WritableArray
import com.facebook.react.bridge.WritableMap
import org.json.JSONArray
import org.json.JSONObject
import rikka.shizuku.Shizuku
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** On-demand, user-authorized operations only. Backups are written before any mutation. */
internal class ClearScreenCleanupController(
  private val context: ReactApplicationContext,
  private val store: () -> ClearScreenStore,
  private val requireConsent: () -> Unit,
) {
  private val serviceArgs = Shizuku.UserServiceArgs(ComponentName(context.packageName, ClearScreenPrivilegedService::class.java.name))
    .daemon(false).processNameSuffix("system_cleanup").version(1)
  private val lock = Any()
  private var channelCandidates = emptySet<Pair<String, String>>()
  private var permissionListener: Shizuku.OnRequestPermissionResultListener? = null
  private var permissionPromise: Promise? = null

  fun accessStatus(): WritableMap = Arguments.createMap().apply {
    val running = runCatching { Shizuku.pingBinder() && !Shizuku.isPreV11() }.getOrDefault(false)
    putBoolean("running", running)
    putBoolean("granted", running && runCatching { Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED }.getOrDefault(false))
    val adb = ClearScreenAdbOperations.status(context)
    putBoolean("adbSupported", adb["supported"] as Boolean)
    putBoolean("adbConnected", adb["connected"] as Boolean)
    putString("adbState", adb["state"] as String)
  }

  fun requestAccess(promise: Promise) {
    try {
      requireConsent()
      check(Shizuku.pingBinder() && !Shizuku.isPreV11()) { "请先启动 Shizuku" }
      if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
        promise.resolve(true)
        return
      }
      check(!Shizuku.shouldShowRequestPermissionRationale()) { "请在 Shizuku 中允许净屏使用权限" }
      synchronized(lock) {
        if (permissionPromise != null) {
          promise.reject("CLEANUP_PERMISSION", "授权请求正在进行")
          return
        }
        permissionPromise = promise
        val listener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
          if (requestCode == REQUEST_CODE) {
            val pending = synchronized(lock) {
              permissionListener?.let(Shizuku::removeRequestPermissionResultListener)
              permissionListener = null
              permissionPromise.also { permissionPromise = null }
            }
            pending?.resolve(grantResult == PackageManager.PERMISSION_GRANTED)
          }
        }
        permissionListener = listener
        Shizuku.addRequestPermissionResultListener(listener)
      }
      context.runOnUiQueueThread {
        try { Shizuku.requestPermission(REQUEST_CODE) } catch (error: Exception) { failPermission(error) }
      }
    } catch (error: Exception) {
      failPermission(error, promise)
    }
  }

  private fun failPermission(error: Exception, fallback: Promise? = null) {
    val pending = synchronized(lock) {
      permissionListener?.let { runCatching { Shizuku.removeRequestPermissionResultListener(it) } }
      permissionListener = null
      permissionPromise.also { permissionPromise = null }
    }
    (pending ?: fallback)?.reject("CLEANUP_PERMISSION", error)
  }

  private fun <T> withService(block: (IClearScreenPrivilegedService) -> T): T {
    requireConsent()
    check(Shizuku.pingBinder() && !Shizuku.isPreV11()) { "请先启动 Shizuku" }
    check(Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) { "请先授予 Shizuku 权限" }
    var failure: Exception? = null
    for (attempt in 1..3) {
      val latch = CountDownLatch(1)
      var remote: IClearScreenPrivilegedService? = null
      val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
          remote = IClearScreenPrivilegedService.Stub.asInterface(binder)
          latch.countDown()
        }
        override fun onServiceDisconnected(name: ComponentName?) { remote = null; latch.countDown() }
      }
      try {
        Shizuku.bindUserService(serviceArgs, connection)
        if (!latch.await(15, TimeUnit.SECONDS)) {
          failure = IllegalStateException("连接 Shizuku 超时")
        } else {
          val service = remote
          if (service == null) {
            // The user service process can die before the handshake completes.
            failure = IllegalStateException("Shizuku 服务连接失败")
          } else {
            requireConsent()
            return block(service)
          }
        }
      } finally {
        runCatching { Shizuku.unbindUserService(serviceArgs, connection, true) }
      }
      if (attempt < 3) Thread.sleep(600)
    }
    throw failure ?: IllegalStateException("Shizuku 服务连接失败")
  }

  fun inspectTargets(): WritableArray = withOps { ops ->
    val items = JSONObject(ops.inspectTargets()).getJSONArray("items")
    Arguments.createArray().apply {
      for (index in 0 until items.length()) {
        val item = items.getJSONObject(index)
        val id = item.getString("id")
        pushMap(Arguments.createMap().apply {
          putString("id", id)
          putString("title", item.getString("title"))
          putString("category", item.getString("category"))
          putString("packageName", item.optString("packageName", ""))
          putString("state", item.getString("state"))
          putBoolean("canRestore", store().cleanupBackup(id) != null)
        })
      }
    }
  }

  fun changeTarget(id: String, restore: Boolean): WritableMap = withOps { ops ->
    // The privileged side independently enforces the manufacturer-specific allowlist.
    val state = ops.readTarget(id)
    val backup = store().cleanupBackup(id)
    val result: String
    if (restore) {
      check(backup == "active") { "没有该项目的原状态记录，无法自动恢复" }
      result = when (state) {
        "active" -> "already_restored"
        "blocked" -> { ops.changeTarget(id, "blocked", "active"); "restored" }
        else -> error("无法确认当前状态，未恢复")
      }
      store().removeCleanupBackup(id)
    } else {
      when (state) {
        "blocked" -> result = "already_blocked"
        "active" -> {
          store().saveCleanupBackup(id, "active")
          requireConsent()
          ops.changeTarget(id, "active", "blocked")
          result = "blocked"
        }
        else -> error("无法确认当前状态，未执行")
      }
    }
    Arguments.createMap().apply { putString("result", result) }
  }

  /** Semantic operations available through either privileged backend. */
  private interface PrivilegedOps {
    fun readSetting(namespace: String, key: String): String
    fun writeSetting(namespace: String, key: String, value: String): String
    fun inspectTargets(): String
    fun readTarget(id: String): String
    fun changeTarget(id: String, expected: String, desired: String): String
    fun scanChannels(): String
    fun blockedNotifications(): String
    fun setAppNotification(packageName: String, enabled: Boolean): String
  }

  private val adbOps = object : PrivilegedOps {
    private val runner = ClearScreenAdbOperations.shellRunner
    override fun readSetting(namespace: String, key: String): String =
      ClearScreenShellOps.readSetting(runner, namespace, key)
    override fun writeSetting(namespace: String, key: String, value: String): String =
      ClearScreenShellOps.writeSetting(runner, namespace, key, value)
    override fun inspectTargets(): String = ClearScreenShellOps.inspectTargets(runner)
    override fun readTarget(id: String): String = ClearScreenShellOps.readTarget(runner, id)
    override fun changeTarget(id: String, expected: String, desired: String): String =
      ClearScreenShellOps.changeTarget(runner, id, expected, desired)
    override fun scanChannels(): String = ClearScreenShellOps.scanChannels(runner)
    override fun blockedNotifications(): String = ClearScreenShellOps.blockedNotifications(runner)
    override fun setAppNotification(packageName: String, enabled: Boolean): String =
      ClearScreenShellOps.setAppNotification(runner, packageName, enabled)
  }

  private fun shizukuOps(service: IClearScreenPrivilegedService) = object : PrivilegedOps {
    override fun readSetting(namespace: String, key: String): String = service.readSetting(namespace, key)
    override fun writeSetting(namespace: String, key: String, value: String): String =
      service.writeSetting(namespace, key, value)
    override fun inspectTargets(): String = service.inspectTargets()
    override fun readTarget(id: String): String = service.readTarget(id)
    override fun changeTarget(id: String, expected: String, desired: String): String =
      service.changeTarget(id, expected, desired)
    override fun scanChannels(): String = service.scanChannels()
    override fun blockedNotifications(): String = service.blockedNotifications()
    override fun setAppNotification(packageName: String, enabled: Boolean): String =
      service.setAppNotification(packageName, enabled)
  }

  /**
   * The app's own verified ADB channel is preferred; otherwise the Shizuku user
   * service is used. Both backends share the same command logic and allowlist.
   */
  private fun <T> withOps(block: (PrivilegedOps) -> T): T {
    if (ClearScreenAdbOperations.isVerifiedConnected()) {
      requireConsent()
      return block(adbOps)
    }
    return withService { service -> block(shizukuOps(service)) }
  }

  /**
   * Rewrites one vendor switch inside the Android settings tables, then reads
   * the value back. The original value is stored before the first change so the
   * switch can be restored later. Only switches with a confirmed settings key
   * can be handled here; the rest keep the page assisted flow.
   */
  fun applyDirectUiCleanup(id: String, desired: String): String {
    check(desired == "on" || desired == "off") { "不支持的操作" }
    val target = ClearScreenUiCleanup.targets(context).singleOrNull { it.id == id } ?: error("这台手机没有这个开关")
    val spec = ClearScreenDirectSettings.spec(target) ?: error("这个开关没有可以直接改写的系统设置项")
    val backupKey = "ui:$id"
    val wanted = if (desired == "off") spec.offValue else spec.onValue
    // One connection for the whole change: rebinding between the read and the
    // write restarts the service process and loses the handshake.
    return withOps { ops ->
      val current = ops.readSetting(spec.namespace, spec.key)
      check(current != "unknown") { "系统里没有这个设置项，请到系统设置里自己关闭" }
      check(current == spec.onValue || current == spec.offValue) { "这个设置项不是开关，没有改写" }
      if (current == wanted) {
        store().saveCleanupState(backupKey, desired)
        return@withOps "already"
      }
      if (desired == "on") {
        // This is an explicit user request to enable the known switch, even if
        // it was already off before ClearScreen first observed it.
        ops.writeSetting(spec.namespace, spec.key, wanted)
        store().removeCleanupBackup(backupKey)
      } else {
        // The original value is stored before the first change so it can be restored.
        store().saveCleanupBackup(backupKey, current)
        ops.writeSetting(spec.namespace, spec.key, wanted)
      }
      store().saveCleanupState(backupKey, desired)
      "changed"
    }
  }

  fun scanChannels(): WritableArray {
    synchronized(lock) { channelCandidates = emptySet() }
    requireConsent()
    val candidates = mutableSetOf<Pair<String, String>>()
    val result = Arguments.createArray()
    val privileged = if (hasPrivilegedRead()) {
      runCatching { withOps { ops -> ops.scanChannels() } }.getOrNull()
    } else null
    if (privileged != null) {
      // 已连接授权通道：dumpsys 给出完整清单，应用级与分组级关闭也能读取。
      val items = JSONArray(privileged)
      for (index in 0 until items.length()) {
        val item = items.getJSONObject(index)
        val pkg = item.optString("packageName")
        val id = item.optString("id")
        if (!pkg.matches(APP_PACKAGE) || id.isBlank()) continue
        if (!candidates.add(pkg to id)) continue
        val state = item.optString("state").takeIf { it in setOf("on", "off", "unknown") } ?: "unknown"
        result.pushMap(Arguments.createMap().apply {
          putString("packageName", pkg)
          putString("id", id)
          putString("name", item.optString("name"))
          putString("state", state)
          putInt("importance", -1)
          putNull("group")
        })
      }
    } else {
      // 未连接：只能看到正在发通知或刚发生变更的渠道，结果不完整。
      for (channel in ClearScreenNotificationChannels.scanObserved(context)) {
        val pkg = channel.packageName
        val id = channel.id
        if (!pkg.matches(APP_PACKAGE) || id.isBlank()) continue
        if (!candidates.add(pkg to id)) continue
        val state = if (channel.importance == NotificationManager.IMPORTANCE_NONE) "off" else "unknown"
        result.pushMap(Arguments.createMap().apply {
          putString("packageName", pkg)
          putString("id", id)
          putString("name", channel.name)
          putString("state", state)
          putInt("importance", channel.importance)
          if (channel.group != null) putString("group", channel.group) else putNull("group")
        })
      }
    }
    synchronized(lock) { channelCandidates = candidates }
    return result
  }

  /** 完整渠道清单只有 dumpsys/appops 能给出，应用内的直接改设置通道或 Shizuku 均可。 */
  private fun hasPrivilegedRead(): Boolean {
    if (ClearScreenAdbOperations.isVerifiedConnected()) return true
    return runCatching {
      Shizuku.pingBinder() && !Shizuku.isPreV11() &&
        Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    }.getOrDefault(false)
  }

  /**
   * One batch read returns the real notification switch for every requested
   * package. Packages that are not in the blocked set are enabled.
   */
  fun notificationStates(packageNames: ReadableArray): WritableArray = withOps { ops ->
    val blocked = JSONArray(ops.blockedNotifications())
    val disabled = HashSet<String>()
    for (index in 0 until blocked.length()) disabled.add(blocked.getString(index))
    Arguments.createArray().apply {
      for (index in 0 until packageNames.size()) {
        val pkg = packageNames.getString(index) ?: continue
        if (!pkg.matches(APP_PACKAGE)) continue
        pushMap(Arguments.createMap().apply {
          putString("packageName", pkg)
          putBoolean("enabled", pkg !in disabled)
        })
      }
    }
  }

  /** Writes one switch and reports back the state that was read from the system. */
  fun setAppNotification(packageName: String, enabled: Boolean): WritableMap = withOps { ops ->
    Arguments.createMap().apply { putString("result", ops.setAppNotification(packageName, enabled)) }
  }

  fun openChannel(pkg: String, id: String) {
    requireConsent()
    check(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { "当前 Android 版本不支持通知渠道" }
    check(synchronized(lock) { (pkg to id) in channelCandidates }) { "请先重新扫描通知渠道" }
    val intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
      .putExtra(Settings.EXTRA_APP_PACKAGE, pkg).putExtra(Settings.EXTRA_CHANNEL_ID, id)
      .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
  }

  fun clear() {
    synchronized(lock) { channelCandidates = emptySet() }
  }

  fun close() {
    clear()
    failPermission(IllegalStateException("授权请求已结束"))
  }

  companion object {
    private const val REQUEST_CODE = 49320
    private val APP_PACKAGE = Regex("[A-Za-z0-9_]+(\\.[A-Za-z0-9_]+)+")
  }
}

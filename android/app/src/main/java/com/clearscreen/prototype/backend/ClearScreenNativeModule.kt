package com.clearscreen.prototype.backend

import android.app.Activity
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.net.VpnService
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Base64
import com.facebook.react.bridge.ActivityEventListener
import com.facebook.react.bridge.BaseActivityEventListener
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.WritableArray
import com.facebook.react.bridge.WritableMap
import com.facebook.react.bridge.Arguments
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.util.concurrent.Executors
import java.util.Locale

class ClearScreenNativeModule(
  private val reactContext: ReactApplicationContext,
) : ReactContextBaseJavaModule(reactContext) {
  private val store = ClearScreenStore(reactContext)
  private val executor = Executors.newSingleThreadExecutor()

  private val activityEventListener: ActivityEventListener = object : BaseActivityEventListener() {
    override fun onActivityResult(activity: Activity, requestCode: Int, resultCode: Int, data: Intent?) {
      if (requestCode != VPN_REQUEST_CODE || resultCode != android.app.Activity.RESULT_OK) return
      if (store.isMasterEnabled()) startVpnService()
    }
  }

  init {
    reactContext.addActivityEventListener(activityEventListener)
  }

  override fun getName(): String = "ClearScreenBackend"

  @ReactMethod
  fun getSnapshot(promise: Promise) = runAsync(promise) {
    snapshot()
  }

  @ReactMethod
  fun setOnboardingCompleted(completed: Boolean, promise: Promise) = runAsync(promise) {
    store.setOnboardingCompleted(completed)
    true
  }

  @ReactMethod
  fun setMasterEnabled(enabled: Boolean, promise: Promise) = runAsync(promise) {
    store.setMasterEnabled(enabled)
    if (!enabled) stopVpnService()
    else if (VpnService.prepare(reactContext) == null) startVpnService()
    snapshot()
  }

  @ReactMethod
  fun setAppRule(
    packageName: String,
    skipEnabled: Boolean,
    networkBlockEnabled: Boolean,
    whitelist: Boolean,
    promise: Promise,
  ) = runAsync(promise) {
    store.setAppRule(packageName, AppRuleState(skipEnabled, networkBlockEnabled, whitelist))
    true
  }

  @ReactMethod
  fun setSetting(key: String, enabled: Boolean, promise: Promise) = runAsync(promise) {
    val knownKey = when (key) {
      "startup" -> ClearScreenStore.KEY_STARTUP
      "autoUpdate" -> ClearScreenStore.KEY_AUTO_UPDATE
      "debug" -> ClearScreenStore.KEY_DEBUG
      else -> throw IllegalArgumentException("Unknown setting: $key")
    }
    store.setSettingEnabled(knownKey, enabled)
    true
  }

  @ReactMethod
  fun clearLogs(promise: Promise) = runAsync(promise) {
    store.clearEvents()
    snapshot()
  }

  @ReactMethod
  fun openAccessibilitySettings(promise: Promise) {
    openSettings(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS), promise)
  }

  @ReactMethod
  fun openAppDetailsSettings(promise: Promise) {
    openSettings(
      Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.parse("package:${reactContext.packageName}")
      },
      promise,
    )
  }

  @ReactMethod
  fun startVpn(promise: Promise) {
    val prepareIntent = VpnService.prepare(reactContext)
    if (prepareIntent != null) {
      val activity = reactContext.currentActivity
      if (activity == null) {
        promise.reject("NO_ACTIVITY", "无法打开 VPN 授权页面")
        return
      }
      activity.startActivityForResult(prepareIntent, VPN_REQUEST_CODE)
      promise.resolve("consent_required")
    } else {
      if (store.isMasterEnabled()) startVpnService()
      promise.resolve(if (store.isMasterEnabled()) "started" else "prepared")
    }
  }

  @ReactMethod
  fun stopVpn(promise: Promise) {
    stopVpnService()
    promise.resolve(true)
  }

  @ReactMethod
  fun openBatterySettings(promise: Promise) {
    // On vivo/iQOO, the documented Android battery-exemption screen does not
    // control the vendor's "自启动" gate that is responsible for terminating
    // user-enabled accessibility services. Prefer the verified OEM entrypoint
    // when it exists, and retain the standard Android fallback elsewhere.
    openSettings(
      vendorStartupSettingsIntent() ?: Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS),
      promise,
    )
  }

  @ReactMethod
  fun confirmVendorStartupGuide(promise: Promise) = runAsync(promise) {
    // This is an acknowledgement of the guided check, not a fabricated claim
    // that Android exposed the OEM switch to us.
    store.setVendorStartupGuideConfirmed(true)
    snapshot()
  }

  override fun invalidate() {
    executor.shutdownNow()
    reactContext.removeActivityEventListener(activityEventListener)
    super.invalidate()
  }

  private fun snapshot(): WritableMap {
    val map = Arguments.createMap()
    val apps = Arguments.createArray()
    installedApps().forEach { app -> apps.pushMap(app) }
    val logs = Arguments.createArray()
    // Older builds could inspect the ClearScreen UI itself and record its own
    // "今日自动跳过" labels as fake skips. Keep the stored events intact, but
    // exclude this package from user-facing history and counters so every view
    // uses the same real-data measure.
    store.getRecentEvents(excludedPackageName = reactContext.packageName).forEach { event ->
      logs.pushMap(Arguments.createMap().apply {
        putString("appId", event.packageName ?: "")
        putString("packageName", event.packageName)
        putString("time", formatTime(event.timestamp))
        putString("type", event.type)
        putString("text", event.text)
        putString("result", event.result)
        putDouble("timestamp", event.timestamp.toDouble())
      })
    }
    val settings = Arguments.createMap().apply {
      putBoolean("startup", store.isSettingEnabled(ClearScreenStore.KEY_STARTUP, true))
      putBoolean("autoUpdate", store.isSettingEnabled(ClearScreenStore.KEY_AUTO_UPDATE, true))
      putBoolean("debug", store.isSettingEnabled(ClearScreenStore.KEY_DEBUG, false))
    }
    map.putBoolean("backendReady", true)
    map.putBoolean("onboardingCompleted", store.isOnboardingCompleted())
    map.putBoolean("masterEnabled", store.isMasterEnabled())
    map.putInt("installedAppCount", apps.size())
    map.putInt("todaySkipCount", store.countToday("skip", reactContext.packageName))
    map.putInt("todayNetworkCount", store.countToday("network", reactContext.packageName))
    map.putBoolean("accessibilityEnabled", ClearScreenAccessibilityService.isEnabled(reactContext))
    // The accessibility service runs in its own process, so its in-memory running flag
    // is not visible from the React Native process. The secure setting is the durable
    // source of truth for the UI and survives activity/process recreation.
    map.putBoolean("accessibilityRunning", ClearScreenAccessibilityService.isEnabled(reactContext))
    map.putBoolean("vpnPrepared", VpnService.prepare(reactContext) == null)
    map.putBoolean("vpnRunning", ClearScreenVpnService.running)
    map.putBoolean("batteryOptimizationIgnored", isBatteryOptimizationIgnored())
    map.putBoolean("isVivoFamily", isVivoFamilyDevice())
    map.putBoolean("vendorStartupSettingsAvailable", vendorStartupSettingsIntent() != null)
    map.putBoolean("vendorStartupGuideConfirmed", store.isVendorStartupGuideConfirmed())
    map.putString("deviceManufacturer", Build.MANUFACTURER ?: "unknown")
    map.putArray("apps", apps)
    map.putArray("logs", logs)
    map.putMap("settings", settings)
    map.putMap("installSource", installSourceDiagnostics())
    return map
  }

  private fun installSourceDiagnostics(): WritableMap {
    val map = Arguments.createMap()
    val packageManager = reactContext.packageManager
    val packageInfo = runCatching {
      val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        PackageManager.GET_SIGNING_CERTIFICATES
      } else {
        @Suppress("DEPRECATION")
        PackageManager.GET_SIGNATURES
      }
      packageManager.getPackageInfo(reactContext.packageName, flags)
    }.getOrNull()
    val sourceInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      runCatching { packageManager.getInstallSourceInfo(reactContext.packageName) }.getOrNull()
    } else {
      null
    }
    val installingPackageName = sourceInfo?.installingPackageName
    val initiatingPackageName = sourceInfo?.initiatingPackageName
    val originatingPackageName = sourceInfo?.originatingPackageName
    val packageSource = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      sourceInfo?.packageSource ?: -1
    } else {
      -1
    }
    val noInstallerRecorded = installingPackageName.isNullOrBlank() &&
      initiatingPackageName.isNullOrBlank()
    // A direct `adb install` on the target vivo build can report packageSource=OTHER
    // while leaving both installer package names empty. Keep this conservative signal
    // so the first authorization attempt can explain the Restricted Settings path.
    val adbInstallLikely = installingPackageName == "com.android.shell" ||
      initiatingPackageName == "com.android.shell" ||
      (noInstallerRecorded && (packageSource == PackageInstaller.PACKAGE_SOURCE_OTHER ||
        packageSource == PackageInstaller.PACKAGE_SOURCE_UNSPECIFIED))
    val restrictedSettingsLikely = adbInstallLikely ||
      packageSource == PackageInstaller.PACKAGE_SOURCE_LOCAL_FILE ||
      packageSource == PackageInstaller.PACKAGE_SOURCE_DOWNLOADED_FILE

    putNullableString(map, "installingPackageName", installingPackageName)
    putNullableString(map, "initiatingPackageName", initiatingPackageName)
    putNullableString(map, "originatingPackageName", originatingPackageName)
    map.putInt("packageSource", packageSource)
    map.putString("packageSourceLabel", packageSourceLabel(packageSource))
    map.putBoolean("adbInstallLikely", adbInstallLikely)
    map.putBoolean("restrictedSettingsLikely", restrictedSettingsLikely)
    map.putDouble("firstInstallTime", packageInfo?.firstInstallTime?.toDouble() ?: 0.0)
    map.putDouble("lastUpdateTime", packageInfo?.lastUpdateTime?.toDouble() ?: 0.0)
    putNullableString(map, "signingCertificateSha256", packageInfo?.let(::signingCertificateSha256))
    return map
  }

  private fun signingCertificateSha256(packageInfo: android.content.pm.PackageInfo): String? {
    val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      packageInfo.signingInfo?.apkContentsSigners
    } else {
      @Suppress("DEPRECATION")
      packageInfo.signatures
    }
    val signature = signatures?.firstOrNull() ?: return null
    val digest = MessageDigest.getInstance("SHA-256").digest(signature.toByteArray())
    return digest.joinToString(":") { byte ->
      "%02X".format(Locale.ROOT, byte.toInt() and 0xFF)
    }
  }

  private fun packageSourceLabel(source: Int): String = when (source) {
    PackageInstaller.PACKAGE_SOURCE_UNSPECIFIED -> "unspecified"
    PackageInstaller.PACKAGE_SOURCE_OTHER -> "other"
    PackageInstaller.PACKAGE_SOURCE_STORE -> "store"
    PackageInstaller.PACKAGE_SOURCE_LOCAL_FILE -> "local_file"
    PackageInstaller.PACKAGE_SOURCE_DOWNLOADED_FILE -> "downloaded_file"
    else -> "unknown"
  }

  private fun putNullableString(map: WritableMap, key: String, value: String?) {
    if (value == null) map.putNull(key) else map.putString(key, value)
  }

  private fun installedApps(): List<WritableMap> {
    val packageManager = reactContext.packageManager
    val launchIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    val resolveInfos = packageManager.queryIntentActivities(launchIntent, PackageManager.MATCH_ALL)
    val packages = resolveInfos.mapNotNull { it.activityInfo?.packageName }
      .distinct()
      .filter { it != reactContext.packageName }
      .mapNotNull { packageName ->
        runCatching { packageManager.getApplicationInfo(packageName, PackageManager.MATCH_ALL) }.getOrNull()
      }
      .sortedBy { packageManager.getApplicationLabel(it).toString().lowercase() }

    return packages.map { info ->
      val packageName = info.packageName
      val rule = store.getAppRule(packageName)
      val versionName = runCatching {
        packageManager.getPackageInfo(packageName, 0).versionName ?: ""
      }.getOrDefault("")
      Arguments.createMap().apply {
        putString("id", packageName)
        putString("packageName", packageName)
        putString("name", packageManager.getApplicationLabel(info).toString())
        putString("iconUri", drawableToDataUri(packageManager.getApplicationIcon(info)))
        putBoolean("skip", rule.skipEnabled)
        putBoolean("network", rule.networkBlockEnabled)
        putBoolean("whitelist", rule.whitelist)
        putBoolean("isSystemApp", info.flags and ApplicationInfo.FLAG_SYSTEM != 0)
        putString("versionName", versionName)
        putDouble("lastUpdateTime", packageManager.getPackageInfo(packageName, 0).lastUpdateTime.toDouble())
      }
    }
  }

  private fun drawableToDataUri(drawable: android.graphics.drawable.Drawable): String {
    val size = 96
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, size, size)
    drawable.draw(canvas)
    val output = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
    bitmap.recycle()
    return "data:image/png;base64," + Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
  }

  private fun isBatteryOptimizationIgnored(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
    val power = reactContext.getSystemService(PowerManager::class.java)
    return power?.isIgnoringBatteryOptimizations(reactContext.packageName) == true
  }

  private fun isVivoFamilyDevice(): Boolean {
    val identity = listOf(Build.MANUFACTURER, Build.BRAND, Build.PRODUCT)
      .joinToString(" ")
      .lowercase(Locale.ROOT)
    return identity.contains("vivo") || identity.contains("iqoo")
  }

  private fun vendorStartupSettingsIntent(): Intent? {
    if (!isVivoFamilyDevice()) return null
    val intent = Intent(VIVO_BG_STARTUP_ACTION)
    return intent.takeIf { candidate ->
      candidate.resolveActivity(reactContext.packageManager) != null
    }
  }

  private fun startVpnService() {
    val intent = Intent(reactContext, ClearScreenVpnService::class.java)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) reactContext.startForegroundService(intent)
    else reactContext.startService(intent)
  }

  private fun stopVpnService() {
    reactContext.stopService(Intent(reactContext, ClearScreenVpnService::class.java).apply {
      action = ClearScreenVpnService.ACTION_STOP
    })
  }

  private fun openSettings(intent: Intent, promise: Promise) {
    try {
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      reactContext.startActivity(intent)
      promise.resolve(true)
    } catch (error: Exception) {
      promise.reject("SETTINGS_OPEN_FAILED", error)
    }
  }

  private fun formatTime(timestamp: Long): String =
    java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(timestamp))

  private fun <T> runAsync(promise: Promise, block: () -> T) {
    executor.execute {
      try {
        promise.resolve(block())
      } catch (error: Exception) {
        promise.reject("BACKEND_ERROR", error)
      }
    }
  }

  companion object {
    private const val VPN_REQUEST_CODE = 771
    // Resolved on the affected V2502DA device to
    // com.vivo.permissionmanager/.activity.BgStartUpManagerActivity.
    private const val VIVO_BG_STARTUP_ACTION = "com.iqoo.secure.BGSTARTUPMANAGER"
  }
}

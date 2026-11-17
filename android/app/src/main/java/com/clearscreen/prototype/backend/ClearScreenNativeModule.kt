package com.clearscreen.prototype.backend

import android.app.ActivityManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.os.SystemClock
import android.provider.Settings
import android.widget.Toast
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.ReadableArray
import com.facebook.react.bridge.WritableArray
import com.facebook.react.bridge.WritableMap
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.LifecycleEventListener
import java.security.MessageDigest
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.Locale

class ClearScreenNativeModule(
  private val reactContext: ReactApplicationContext,
) : ReactContextBaseJavaModule(reactContext), LifecycleEventListener {
  private val settingsNavigator by lazy { ClearScreenSettingsNavigator(reactContext) }
  private val storeDelegate = lazy {
    ClearScreenStore(reactContext).apply { prepareAfterConsent() }
  }
  private val store by storeDelegate
  private val privacyConsentStore by lazy { ClearScreenPrivacyConsentStore(reactContext) }
  private val executor = Executors.newSingleThreadExecutor()
  private val snapshotExecutor = ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
    ArrayBlockingQueue<Runnable>(4), { task -> Thread(task, "clearscreen-snapshot").apply { isDaemon = true } })
  private val healthExecutor = ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
    ArrayBlockingQueue<Runnable>(1), { task -> Thread(task, "clearscreen-health").apply { isDaemon = true } })
  private val permissionExecutor = Executors.newSingleThreadExecutor()
  private val adbReconnectExecutor = Executors.newSingleThreadExecutor()
  private val adbReconnectLock = Any()
  private var adbReconnectScheduled = false
  private var lastAdbReconnectAttempt = 0L
  private val adbWatchdogExecutor = Executors.newSingleThreadScheduledExecutor { task ->
    Thread(task, "clearscreen-adb-watchdog").apply { isDaemon = true }
  }
  private val appInventory = ClearScreenAppInventory(reactContext)
  private val permissionReader by lazy { ClearScreenPermissionReader(reactContext) }
  private val cleanupController by lazy { ClearScreenCleanupController(reactContext, { store }, ::requirePrivacyConsent) }
  @Volatile private var hostResumed = false

  init {
    reactContext.addLifecycleEventListener(this)
    // Keep the wireless-debugging channel alive while the app is on screen and
    // heal it after the system turns the toggle off or reaps an idle socket.
    adbWatchdogExecutor.scheduleWithFixedDelay(
      { runCatching { adbWatchdogTick() } }, 30L, 30L, TimeUnit.SECONDS,
    )
  }

  override fun onHostPause() { hostResumed = false; ClearScreenAccessibilityHealth.invalidate(); ClearScreenSettingsObserver.onHostPause(); ClearScreenUiCleanup.onHostPause() }
  override fun onHostResume() {
    hostResumed = true
    ClearScreenAccessibilityHealth.invalidate()
    ClearScreenSettingsObserver.onHostResume()
    ClearScreenUiCleanup.onHostResume()
    scheduleAdbReconnect()
  }
  override fun onHostDestroy() { hostResumed = false; ClearScreenAccessibilityHealth.invalidate(); ClearScreenSettingsObserver.clear(); ClearScreenUiCleanup.stop() }

  private fun scheduleAdbReconnect() {
    if (!hostResumed) return
    synchronized(adbReconnectLock) {
      if (adbReconnectScheduled) return
      adbReconnectScheduled = true
    }
    try {
      adbReconnectExecutor.execute {
        try {
          if (!hostResumed || !privacyConsentStore.hasCurrentConsent() ||
            !ClearScreenAdbOperations.shouldRestore(reactContext)) return@execute
          val now = SystemClock.elapsedRealtime()
          val delay = synchronized(adbReconnectLock) {
            if (lastAdbReconnectAttempt == 0L) 0L
            else (15_000L - (now - lastAdbReconnectAttempt)).coerceAtLeast(0L)
          }
          if (delay > 0L) Thread.sleep(delay)
          if (!hostResumed || !privacyConsentStore.hasCurrentConsent() ||
            !ClearScreenAdbOperations.shouldRestore(reactContext)) return@execute
          synchronized(adbReconnectLock) { lastAdbReconnectAttempt = SystemClock.elapsedRealtime() }
          ensureWirelessDebuggingEnabled()
          ClearScreenAdbOperations.restoreIfRequested(reactContext)
        } catch (_: Exception) {
          // The status remains disconnected; a later foreground visit can retry.
        } finally {
          synchronized(adbReconnectLock) { adbReconnectScheduled = false }
        }
      }
    } catch (_: Exception) {
      synchronized(adbReconnectLock) { adbReconnectScheduled = false }
    }
  }

  /** Foreground watchdog: probe a live channel, otherwise restore the connection. */
  private fun adbWatchdogTick() {
    if (!hostResumed) return
    if (ClearScreenAdbOperations.isVerifiedConnected()) {
      ClearScreenAdbOperations.keepAlive()
      return
    }
    if (!privacyConsentStore.hasCurrentConsent() ||
      !ClearScreenAdbOperations.shouldRestore(reactContext)) return
    scheduleAdbReconnect()
  }

  private fun wirelessDebuggingEnabled(): Boolean = runCatching {
    Settings.Global.getInt(reactContext.contentResolver, "adb_wifi_enabled", 1) != 0
  }.getOrDefault(true)

  /** Re-enables the wireless debugging toggle when the system/OEM turned it off. */
  private fun ensureWirelessDebuggingEnabled() {
    runCatching {
      val granted = reactContext.checkSelfPermission("android.permission.WRITE_SECURE_SETTINGS") ==
        PackageManager.PERMISSION_GRANTED
      if (granted && !wirelessDebuggingEnabled()) {
        Settings.Global.putInt(reactContext.contentResolver, "adb_wifi_enabled", 1)
      }
    }
  }

  override fun getName(): String = "ClearScreenBackend"

  @ReactMethod
  fun getAdLearningState(promise: Promise) = learningCommand(promise, "state")

  @ReactMethod
  fun startAdLearning(promise: Promise) = learningCommand(promise, "start")

  @ReactMethod
  fun stopAdLearning(promise: Promise) = learningCommand(promise, "stop")

  @ReactMethod
  fun clearAdLearning(promise: Promise) = learningCommand(promise, "clear")

  @ReactMethod
  fun deleteAdLearningRule(key: String, promise: Promise) = learningCommand(promise, "delete", key)

  private fun learningCommand(promise: Promise, action: String, key: String? = null) {
    val deadline = SystemClock.elapsedRealtime() + if (action == "state") 3_000L else 6_000L
    android.os.Handler(android.os.Looper.getMainLooper()).post {
      synchronized(ClearScreenPrivacyConsentStore.operationLock) {
        try {
          check(SystemClock.elapsedRealtime() < deadline) { "学习请求等待超时，尚未执行" }
          requirePrivacyConsent()
          val learning = ClearScreenAccessibilityService.learningService?.adLearning
          when (action) {
            "start" -> {
              check(store.isMasterEnabled()) { "请先开启净屏" }
              check(learning != null) { "请先开启并连接无障碍服务" }
              ClearScreenUiCleanup.stop()
              ClearScreenAccessibilityService.learningService?.beginAdLearning()
            }
            "stop" -> learning?.stop("已取消学习", resumeProcessing = true)
            "clear" -> {
              learning?.stop()
              check(store.clearLearnedAdRules()) { "学习规则清除失败" }
            }
            "delete" -> {
              check(store.deleteLearnedAdRule(key.orEmpty())) { "记录不存在或删除失败" }
              learning?.onRuleDeleted()
            }
          }
          val rules = store.getLearnedAdRules()
          val records = Arguments.createArray()
          for (i in rules.length() - 1 downTo 0) {
            val rule = rules.optJSONObject(i) ?: continue
            val id = rule.optString("key")
            if (id.isBlank()) continue
            val selectorVersion = rule.optJSONObject("selector")?.optInt("version") ?: 0
            records.pushMap(Arguments.createMap().apply {
              putString("id", id)
              putString("appId", rule.optString("pkg"))
              putDouble("timestamp", rule.optLong("created", 0L).toDouble())
              putBoolean("needsRelearn", selectorVersion !in 2..5)
            })
          }
          promise.resolve(Arguments.createMap().apply {
            putBoolean("active", learning?.active == true)
            putInt("count", rules.length())
            putString("message", learning?.message.orEmpty())
            putArray("records", records)
          })
        } catch (error: Exception) { promise.reject("AD_LEARNING", error.message, error) }
      }
    }
  }

  @ReactMethod
  fun getSnapshot(promise: Promise) = runAsync(promise, snapshotExecutor) {
    val current = snapshot()
    scheduleAdbReconnect()
    current
  }

  @ReactMethod
  fun getPermissionSnapshot(promise: Promise) = runAsync(promise, permissionExecutor) {
    if (!privacyConsentStore.hasCurrentConsent()) privacySnapshot()
    else {
      val map = Arguments.createMap().apply {
        putBoolean("privacyConsentAccepted", true)
        writePermissionState(this, verifyAccessibility = true)
      }
      // Consent can be withdrawn on the main worker while a provider read is
      // in flight. Never return the old authorized snapshot after withdrawal.
      if (privacyConsentStore.hasCurrentConsent()) map else privacySnapshot()
    }
  }

  /** No app inventory, logs, signing diagnostics, or OEM provider reads. */
  @ReactMethod
  fun getAccessibilityHealth(promise: Promise) = runAsync(promise, healthExecutor) {
    if (!privacyConsentStore.hasCurrentConsent()) privacySnapshot()
    else {
      val map = Arguments.createMap().apply {
        putBoolean("privacyConsentAccepted", true)
        writeAccessibilityState(this, verify = true)
      }
      if (privacyConsentStore.hasCurrentConsent()) map else privacySnapshot()
    }
  }

  @ReactMethod
  fun setOnboardingCompleted(completed: Boolean, promise: Promise) = runAsync(promise) {
    requirePrivacyConsent()
    store.setOnboardingCompleted(completed)
    true
  }

  @ReactMethod
  fun setMasterEnabled(enabled: Boolean, promise: Promise) = runAsync(promise) {
    requirePrivacyConsent()
    store.setMasterEnabled(enabled)
    ClearScreenRuntimeService.sync(reactContext)
    if (enabled && hostResumed) {
      reactContext.runOnUiQueueThread { ClearScreenAccessibilityRecovery.onForeground(reactContext) }
    }
    true
  }

  @ReactMethod
  fun acceptPrivacyPolicy(promise: Promise) = runSnapshotMutation(promise) {
    privacyConsentStore.acceptCurrentPolicy()
    store.prepareAfterConsent()
    scheduleAdbReconnect()
  }

  @ReactMethod
  fun withdrawPrivacyConsent(promise: Promise) = runAsync(promise) {
    val cleared = synchronized(ClearScreenPrivacyConsentStore.operationLock) {
      privacyConsentStore.withdrawConsent()
      if (storeDelegate.isInitialized()) store.clearUserData()
      else ClearScreenStore(reactContext).clearUserData()
    }
    ClearScreenAdbPairingHelper.stop()
    val adbStopped = runCatching { ClearScreenAdbOperations.disconnect(reactContext, forgetEndpoint = true) }.isSuccess
    ClearScreenSettingsObserver.clear()
    ClearScreenUiCleanup.clear()
    cleanupController.clear()
    check(cleared && adbStopped) { "Unable to clear local app data" }
    privacySnapshot()
  }

  @ReactMethod
  fun setAppRule(
    packageName: String,
    skipEnabled: Boolean,
    networkBlockEnabled: Boolean,
    whitelist: Boolean,
    promise: Promise,
  ) = runAsync(promise) {
    requirePrivacyConsent()
    store.setAppRule(packageName, AppRuleState(skipEnabled, networkBlockEnabled, whitelist))
    true
  }

  @ReactMethod
  fun setAppRules(updates: ReadableArray, promise: Promise) = runAsync(promise) {
    requirePrivacyConsent()
    val rules = (0 until updates.size()).associate { index ->
      val update = requireNotNull(updates.getMap(index)) { "App rule is required" }
      val packageName = requireNotNull(update.getString("packageName")) { "App package name is required" }
      val enabled = update.getBoolean("skipEnabled")
      packageName to AppRuleState(skipEnabled = enabled, whitelist = !enabled)
    }
    store.setAppRules(rules)
    true
  }

  @ReactMethod
  fun setSetting(key: String, enabled: Boolean, promise: Promise) = runAsync(promise) {
    requirePrivacyConsent()
    val knownKey = when (key) {
      "startup" -> ClearScreenStore.KEY_STARTUP
      "autoUpdate" -> ClearScreenStore.KEY_AUTO_UPDATE
      "debug" -> ClearScreenStore.KEY_DEBUG
      "skipSplashVideo" -> ClearScreenStore.KEY_SKIP_SPLASH_VIDEO
      "closePopupAds" -> ClearScreenStore.KEY_CLOSE_POPUP_ADS
      "closeFeedAds" -> ClearScreenStore.KEY_CLOSE_FEED_ADS
      else -> throw IllegalArgumentException("Unknown setting: $key")
    }
    store.setSettingEnabled(knownKey, enabled)
    true
  }

  @ReactMethod
  fun restoreDefaults(promise: Promise) = runSnapshotMutation(promise) {
    requirePrivacyConsent()
    store.restoreDefaultPreferences()
  }

  @ReactMethod
  fun setLogRetentionDays(days: Int, promise: Promise) = runAsync(promise) {
    requirePrivacyConsent()
    store.setLogRetentionDays(days)
    true
  }

  @ReactMethod
  fun clearLogs(promise: Promise) = runSnapshotMutation(promise) {
    requirePrivacyConsent()
    store.clearEvents()
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
  fun openBatterySettings(promise: Promise) {
    openSettingsPage(promise) { settingsNavigator.openBatteryOptimizationSettings() }
  }

  @ReactMethod
  fun openSystemPrivacySettings(promise: Promise) {
    openSettingsPage(promise) { settingsNavigator.openSystemPrivacySettings() }
  }

  @ReactMethod
  fun openSystemAdPrivacySettings(promise: Promise) {
    openSettingsPage(promise) { settingsNavigator.openSystemAdPrivacySettings() }
  }

  @ReactMethod
  fun openSystemHomeSettings(promise: Promise) {
    openSettingsPage(promise) { settingsNavigator.openSystemHomeSettings() }
  }

  /**
   * Opens one registered recommendation destination and reports which level was reached,
   * so the page can tell the user what to tap when only the app info page was available.
   */
  @ReactMethod
  fun openRecommendRoute(route: String, promise: Promise) {
    try {
      val landed = settingsNavigator.openRecommendRoute(route)
      // The system page is already in front when this returns, so the guidance is shown as a
      // short notice on top of that page instead of waiting for the user to come back.
      if (landed.level != ClearScreenRecommendRoutes.LEVEL_APP_DETAILS) {
        landed.hint?.let { Toast.makeText(reactContext, it, Toast.LENGTH_LONG).show() }
      }
      promise.resolve(Arguments.createMap().apply {
        putString("level", landed.level)
        putString("component", landed.component)
        landed.hint?.let { putString("hint", it) }
      })
    } catch (error: Exception) {
      promise.reject("SETTINGS_OPEN_FAILED", error)
    }
  }

  /**
   * Opens the exact system settings page that owns one recommendation switch,
   * identified by its catalog id. For assisted targets a passive watch is armed
   * first: it never taps anything, it only confirms the user's own change so the
   * row can turn into「已设置」. The jump still works without the accessibility
   * service; in that case the watch is simply skipped.
   */
  @ReactMethod
  fun openTargetSettingsPage(id: String, promise: Promise) {
    try {
      val target = ClearScreenUiCleanup.targets(reactContext).singleOrNull { it.id == id }
        ?: error("这台手机没有这个开关")
      if (target.support == "assisted" && ClearScreenDirectSettings.spec(target) == null) {
        runCatching { ClearScreenUiCleanup.begin(reactContext, target.id, false, store, manual = true) }
      }
      val landed = settingsNavigator.openTarget(target)
      if (landed.level == ClearScreenRecommendRoutes.LEVEL_APP_DETAILS) {
        runCatching { ClearScreenUiCleanup.cancel() }
      }
      landed.hint?.let { Toast.makeText(reactContext, it, Toast.LENGTH_LONG).show() }
      promise.resolve(Arguments.createMap().apply {
        putString("level", landed.level)
        putString("component", landed.component)
        landed.hint?.let { putString("hint", it) }
      })
    } catch (error: Exception) {
      promise.reject("SETTINGS_OPEN_FAILED", error)
    }
  }

  @ReactMethod
  fun openAppNotificationSettings(packageName: String, promise: Promise) {
    openSettingsPage(promise) {
      requirePrivacyConsent()
      settingsNavigator.openAppNotificationSettings(packageName)
    }
  }

  @ReactMethod
  fun getCleanupAccessStatus(promise: Promise) = runAsync(promise) {
    requirePrivacyConsent()
    cleanupController.accessStatus()
  }

  @ReactMethod
  fun requestCleanupAccess(promise: Promise) { cleanupController.requestAccess(promise) }

  @ReactMethod
  fun inspectCleanupTargets(promise: Promise) = runAsync(promise) { cleanupController.inspectTargets() }

  @ReactMethod
  fun changeCleanupTarget(id: String, restore: Boolean, promise: Promise) = runAsync(promise) {
    cleanupController.changeTarget(id, restore)
  }

  @ReactMethod
  fun readAppNotificationStates(packageNames: ReadableArray, promise: Promise) = runAsync(promise) {
    cleanupController.notificationStates(packageNames)
  }

  @ReactMethod
  fun setAppNotificationEnabled(packageName: String, enabled: Boolean, promise: Promise) = runAsync(promise) {
    cleanupController.setAppNotification(packageName, enabled)
  }

  @ReactMethod
  fun scanMarketingChannels(promise: Promise) = runAsync(promise) { cleanupController.scanChannels() }

  @ReactMethod
  fun getNotificationAccessStatus(promise: Promise) = runAsync(promise) {
    requirePrivacyConsent()
    Arguments.createMap().apply {
      putBoolean("supported", Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
      putBoolean("granted", ClearScreenNotificationChannels.isGranted(reactContext))
    }
  }

  @ReactMethod
  fun openNotificationAccessSettings(promise: Promise) {
    openSettingsPage(promise) {
      requirePrivacyConsent()
      ClearScreenNotificationChannels.openSettings(reactContext)
    }
  }

  @ReactMethod
  fun isQuickComponentsGuideSeen(promise: Promise) = runAsync(promise) {
    requirePrivacyConsent()
    store.isQuickComponentsGuideSeen()
  }

  @ReactMethod
  fun setQuickComponentsGuideSeen(promise: Promise) = runAsync(promise) {
    requirePrivacyConsent()
    store.setQuickComponentsGuideSeen()
    true
  }

  @ReactMethod
  fun readAdbGuideSeen(promise: Promise) = runAsync(promise) {
    requirePrivacyConsent()
    Arguments.createArray().apply {
      store.readAdbGuideSeen().forEach { pushString(it) }
    }
  }

  @ReactMethod
  fun markAdbGuideSeen(key: String, promise: Promise) = runAsync(promise) {
    requirePrivacyConsent()
    store.markAdbGuideSeen(key)
    true
  }

  @ReactMethod
  fun openMarketingChannel(packageName: String, channelId: String, promise: Promise) {
    openSettingsPage(promise) { cleanupController.openChannel(packageName, channelId) }
  }

  @ReactMethod
  fun openVendorStore(promise: Promise) {
    openSettingsPage(promise) {
      requirePrivacyConsent()
      settingsNavigator.openVendorStore()
    }
  }

  @ReactMethod
  fun getUiCleanupTargets(promise: Promise) = runAsync(promise) {
    requirePrivacyConsent()
    Arguments.createArray().apply {
      ClearScreenUiCleanup.targets(reactContext).forEach { target ->
        val spec = ClearScreenDirectSettings.spec(target)
        pushMap(Arguments.createMap().apply {
          putString("id", target.id); putString("title", target.title)
          putString("category", target.category); putString("page", target.page); putString("entry", target.entry)
          putBoolean("canRestore", ClearScreenUiCleanup.hasBackup(target, store))
          putBoolean("direct", spec != null)
          putBoolean("needsSystemToggleOn", target.checkedWhenFeatureDisabled)
          putString("effect", target.effect)
          putString("support", target.support)
          target.reason?.let { putString("reason", it) }
          target.navigate?.let { putString("navigate", it) }
          putString("state", when {
            spec == null -> ClearScreenUiCleanup.state(target, store) ?: "unknown"
            else -> {
              val live = spec.stateOf(ClearScreenDirectSettings.readLocal(reactContext, spec))
              if (live != "unknown") live else ClearScreenUiCleanup.state(target, store) ?: "unknown"
            }
          })
        })
      }
    }
  }

  /**
   * Rewrites one vendor switch inside the Android settings tables, then reads
   * the value back. The original value is stored before the first change so the
   * switch can be restored later. Only switches with a confirmed settings key
   * can be handled here; the rest keep the page assisted flow.
   */
  @ReactMethod
  fun applyUiCleanupDirect(id: String, desired: String, promise: Promise) = runAsync(promise) {
    requirePrivacyConsent()
    cleanupController.applyDirectUiCleanup(id, desired)
  }

  @ReactMethod
  fun getAdbAccessStatus(promise: Promise) = runAsync(promise) {
    adbResult(ClearScreenAdbOperations.status(reactContext))
  }

  @ReactMethod
  fun pairAdb(host: String, port: Double, code: String, promise: Promise) = runAsync(promise) {
    requirePrivacyConsent()
    val address = requireAdbHost(host)
    val pairingPort = requireAdbPort(port)
    val pairingCode = code.trim()
    check(pairingCode.matches(Regex("\\d{4,8}"))) { "配对码应为 4-8 位数字" }
    ClearScreenAdbOperations.requestReconnect(reactContext)
    adbResult(ClearScreenAdbOperations.pair(reactContext, address, pairingPort, pairingCode))
  }

  @ReactMethod
  fun connectAdb(host: String, port: Double, promise: Promise) = runAsync(promise) {
    requirePrivacyConsent()
    adbResult(ClearScreenAdbOperations.connect(reactContext, requireAdbHost(host), requireAdbPort(port)))
  }

  @ReactMethod
  fun autoConnectAdb(promise: Promise) = runAsync(promise) {
    requirePrivacyConsent()
    adbResult(ClearScreenAdbOperations.autoConnect(reactContext))
  }

  @ReactMethod
  fun disconnectAdb(promise: Promise) = runAsync(promise) {
    ClearScreenAdbOperations.disconnect(reactContext)
    true
  }

  @ReactMethod
  fun startAdbAutoPair(promise: Promise) = runAsync(promise) {
    requirePrivacyConsent()
    check(ClearScreenAdbOperations.isSupported()) { "Android 11 以下不支持无线调试配对" }
    check(accessibilityServiceEnabled()) { "请先开启净屏的无障碍服务，自动配对需要读取配对弹窗" }
    ClearScreenAdbOperations.requestReconnect(reactContext)
    ClearScreenAdbPairingHelper.start(reactContext)
    adbResult(ClearScreenAdbOperations.status(reactContext))
  }

  @ReactMethod
  fun stopAdbAutoPair(promise: Promise) = runAsync(promise) {
    // Only cancels the in-flight pairing assist. The user's request to keep the
    // channel connected — and any live connection itself — must survive a
    // cancelled attempt, otherwise the switch silently drops after setup.
    ClearScreenAdbPairingHelper.stop()
    adbResult(ClearScreenAdbOperations.status(reactContext))
  }

  private fun accessibilityServiceEnabled(): Boolean = runCatching {
    val enabled = Settings.Secure.getString(
      reactContext.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
    ).orEmpty()
    val target = ComponentName(reactContext, ClearScreenAccessibilityService::class.java)
    enabled.split(':').filter { it.isNotBlank() }.any { ComponentName.unflattenFromString(it) == target }
  }.getOrDefault(false)

  @ReactMethod
  fun openDeveloperOptions(promise: Promise) {
    try {
      requirePrivacyConsent()
      promise.resolve(settingsNavigator.openDeveloperOptions())
    } catch (error: Exception) {
      promise.reject("SETTINGS_OPEN_FAILED", error)
    }
  }

  private fun requireAdbHost(host: String): String {
    val address = host.trim()
    check(address.matches(Regex("\\d{1,3}(\\.\\d{1,3}){3}"))) { "只支持同一网络内的 IPv4 地址" }
    return address
  }

  private fun requireAdbPort(port: Double): Int {
    val value = port.toInt()
    check(value in 1..65535) { "端口不正确" }
    return value
  }

  private fun adbResult(values: Map<String, Any?>): WritableMap = Arguments.createMap().apply {
    for ((key, value) in values) {
      when (value) {
        null -> putNull(key)
        is Boolean -> putBoolean(key, value)
        is Int -> putInt(key, value)
        is Double -> putDouble(key, value)
        else -> putString(key, value.toString())
      }
    }
  }

  @ReactMethod
  fun takeUiCleanupResult(promise: Promise) = runAsync(promise) {
    requirePrivacyConsent()
    ClearScreenUiCleanup.takeResult()?.let { result -> Arguments.createMap().apply {
      putString("title", result.first); putString("message", result.second)
    } }
  }

  @ReactMethod
  fun startUiCleanup(id: String, restore: Boolean, promise: Promise) {
    openSettingsPage(promise) {
      synchronized(ClearScreenPrivacyConsentStore.operationLock) {
        requirePrivacyConsent()
        val target = ClearScreenUiCleanup.begin(reactContext, id, restore, store)
        try {
          val route = target.navigate
          if (route != null) {
            val landed = settingsNavigator.openRecommendRoute(route)
            if (landed.level == ClearScreenRecommendRoutes.LEVEL_APP_DETAILS) {
              ClearScreenUiCleanup.cancel()
              error(landed.hint ?: "系统没有开放这个设置页")
            }
          } else {
            when (target.category) {
              "store" -> settingsNavigator.openVendorStore()
              "news" -> when (target.entry) {
                "home" -> settingsNavigator.openHomeScreen()
                "browser" -> settingsNavigator.openVivoBrowser()
                else -> settingsNavigator.openSystemHomeSettings()
              }
              else -> settingsNavigator.openSystemAdPrivacySettings()
            }
          }
        } catch (error: Exception) {
          ClearScreenUiCleanup.cancel()
          throw error
        }
      }
    }
  }

  @ReactMethod
  fun openStartupSettings(promise: Promise) {
    if (settingsNavigator.startupManagement == "sleeping") {
      ClearScreenSettingsObserver.clear()
      openSettingsPage(promise) { settingsNavigator.openStartupSettings() }
    } else openObservedSettings(promise, ClearScreenSettingsObserver.Kind.STARTUP) { settingsNavigator.openStartupSettings() }
  }

  @ReactMethod
  fun openBackgroundSettings(promise: Promise) {
    openObservedSettings(promise, ClearScreenSettingsObserver.Kind.BACKGROUND) { settingsNavigator.openBackgroundSettings() }
  }

  @ReactMethod
  fun confirmVendorStartupGuide(promise: Promise) = runSnapshotMutation(promise) {
    requirePrivacyConsent()
    // This is an acknowledgement of the guided check, not a fabricated claim
    // that Android exposed the OEM switch to us.
    store.setVendorStartupGuideConfirmed(true)
  }

  override fun invalidate() {
    hostResumed = false
    reactContext.removeLifecycleEventListener(this)
    ClearScreenSettingsObserver.clear()
    permissionReader.close()
    cleanupController.close()
    permissionExecutor.shutdownNow()
    healthExecutor.shutdownNow()
    snapshotExecutor.shutdownNow()
    adbReconnectExecutor.shutdownNow()
    appInventory.close()
    executor.shutdownNow()
    super.invalidate()
  }

  private fun snapshot(): WritableMap {
    if (!privacyConsentStore.hasCurrentConsent()) return privacySnapshot()
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
        putString("ruleId", event.ruleId.orEmpty())
        putDouble("timestamp", event.timestamp.toDouble())
      })
    }
    val settings = Arguments.createMap().apply {
      putBoolean("startup", store.isSettingEnabled(ClearScreenStore.KEY_STARTUP, true))
      putBoolean("autoUpdate", store.isSettingEnabled(ClearScreenStore.KEY_AUTO_UPDATE, true))
      putBoolean("debug", store.isSettingEnabled(ClearScreenStore.KEY_DEBUG, false))
      putBoolean("skipSplashVideo", store.isSettingEnabled(ClearScreenStore.KEY_SKIP_SPLASH_VIDEO, true))
      putBoolean("closePopupAds", store.isSettingEnabled(ClearScreenStore.KEY_CLOSE_POPUP_ADS, true))
      putBoolean("closeFeedAds", store.isSettingEnabled(ClearScreenStore.KEY_CLOSE_FEED_ADS, true))
      putInt("logRetentionDays", store.getLogRetentionDays())
    }
    val versionName = runCatching {
      reactContext.packageManager.getPackageInfo(reactContext.packageName, 0).versionName
    }.getOrNull() ?: "unknown"
    map.putBoolean("backendReady", true)
    map.putBoolean("privacyConsentAccepted", true)
    map.putString("privacyPolicyVersion", ClearScreenPrivacyConsentStore.POLICY_VERSION)
    map.putString("versionName", versionName)
    map.putBoolean("onboardingCompleted", store.isOnboardingCompleted())
    map.putBoolean("masterEnabled", store.isMasterEnabled())
    map.putInt("installedAppCount", apps.size())
    map.putInt("todaySkipCount", store.countToday("skip", reactContext.packageName))
    map.putInt("todayNetworkCount", store.countToday("network", reactContext.packageName))
    writePermissionState(map)
    map.putArray("apps", apps)
    map.putArray("logs", logs)
    map.putMap("settings", settings)
    return map
  }

  /** Re-read permission state without loading application icons or processing history. */
  private fun writeAccessibilityState(map: WritableMap, verify: Boolean = false) {
    val accessibility = readAccessibilityState(verify)
    map.putBoolean("accessibilityEnabled", accessibility.authorized)
    map.putBoolean("accessibilityRunning", accessibility.running)
    map.putBoolean("accessibilitySettingEnabled", accessibility.settingEnabled)
    map.putBoolean("accessibilityServiceBound", accessibility.serviceBound)
    map.putBoolean("accessibilityProcessAlive", accessibility.processAlive)
    map.putString("accessibilityHealth", accessibility.health)
    map.putString("accessibilityStoredServices", accessibility.storedServices)
  }

  private fun writePermissionState(map: WritableMap, verifyAccessibility: Boolean = false) {
    writeAccessibilityState(map, verifyAccessibility)
    // ClearScreen deliberately does not request or own a VPN. Keep these
    // fields false for older JS clients that still read them.
    map.putBoolean("vpnPrepared", false)
    map.putBoolean("vpnRunning", false)
    map.putNullableBoolean("batteryOptimizationIgnored", isBatteryOptimizationIgnored())
    map.putNullableBoolean("backgroundRestricted", isBackgroundRestricted())
    map.putNullableBoolean("startupAllowed", if (settingsNavigator.startupManagement == "sleeping") null else permissionReader.startupAllowed())
    map.putString("startupManagement", settingsNavigator.startupManagement)
    map.putNullableBoolean("backgroundAllowed", ClearScreenSettingsObserver.value(ClearScreenSettingsObserver.Kind.BACKGROUND))
    map.putBoolean("isVivoFamily", isVivoFamilyDevice())
    map.putBoolean("vendorStartupSettingsAvailable", settingsNavigator.hasStartupSettings())
    map.putBoolean("vendorStartupGuideConfirmed", store.isVendorStartupGuideConfirmed())
    map.putString("deviceManufacturer", Build.MANUFACTURER ?: "unknown")
    map.putMap("installSource", installSourceDiagnostics())
  }

  /** No app inventory, old preferences, device diagnostics, or system permission state is read here. */
  private fun privacySnapshot(): WritableMap = Arguments.createMap().apply {
    putBoolean("backendReady", true)
    putBoolean("privacyConsentAccepted", false)
    putString("privacyPolicyVersion", ClearScreenPrivacyConsentStore.POLICY_VERSION)
    putString("versionName", "unknown")
    putBoolean("onboardingCompleted", false)
    putBoolean("masterEnabled", false)
    putInt("installedAppCount", 0)
    putInt("todaySkipCount", 0)
    putInt("todayNetworkCount", 0)
    putBoolean("accessibilityEnabled", false)
    putBoolean("accessibilityRunning", false)
    putBoolean("accessibilitySettingEnabled", false)
    putBoolean("accessibilityServiceBound", false)
    putBoolean("accessibilityProcessAlive", false)
    putString("accessibilityHealth", ClearScreenAccessibilityHealth.UNKNOWN)
    putString("accessibilityStoredServices", "")
    putBoolean("vpnPrepared", false)
    putBoolean("vpnRunning", false)
    putNull("batteryOptimizationIgnored")
    putNull("backgroundRestricted")
    putNull("startupAllowed")
    putNull("backgroundAllowed")
    putBoolean("isVivoFamily", false)
    putBoolean("vendorStartupSettingsAvailable", false)
    putBoolean("vendorStartupGuideConfirmed", false)
    putString("deviceManufacturer", "unknown")
    putArray("apps", Arguments.createArray())
    putArray("logs", Arguments.createArray())
    putMap("settings", Arguments.createMap().apply {
      putBoolean("startup", true)
      putBoolean("autoUpdate", true)
      putBoolean("debug", false)
      putBoolean("skipSplashVideo", true)
      putBoolean("closePopupAds", true)
      putBoolean("closeFeedAds", true)
      putInt("logRetentionDays", ClearScreenStore.DEFAULT_LOG_RETENTION_DAYS)
    })
    putMap("installSource", Arguments.createMap().apply {
      putNull("installingPackageName")
      putNull("initiatingPackageName")
      putNull("originatingPackageName")
      putInt("packageSource", -1)
      putString("packageSourceLabel", "unknown")
      putBoolean("adbInstallLikely", false)
      putBoolean("restrictedSettingsLikely", false)
      putDouble("firstInstallTime", 0.0)
      putDouble("lastUpdateTime", 0.0)
      putNull("signingCertificateSha256")
    })
  }

  private fun requirePrivacyConsent() {
    check(privacyConsentStore.hasCurrentConsent()) { "Privacy policy consent is required" }
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

  private fun installedApps(): List<WritableMap> = appInventory.entries().map { entry ->
    val rule = store.getAppRule(entry.packageName)
    Arguments.createMap().apply {
      putString("id", entry.packageName)
      putString("packageName", entry.packageName)
      putString("name", entry.name)
      putString("iconUri", entry.iconUri)
      putBoolean("skip", rule.skipEnabled)
      putBoolean("network", rule.networkBlockEnabled)
      putBoolean("whitelist", rule.whitelist)
      putBoolean("isSystemApp", entry.systemApp)
    }
  }

  private fun isBatteryOptimizationIgnored(): Boolean? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
    return runCatching {
      reactContext.getSystemService(PowerManager::class.java)
        ?.isIgnoringBatteryOptimizations(reactContext.packageName)
    }.getOrNull()
  }

  private fun isBackgroundRestricted(): Boolean? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return null
    return runCatching {
      reactContext.getSystemService(ActivityManager::class.java)?.isBackgroundRestricted
    }.getOrNull()
  }

  private fun WritableMap.putNullableBoolean(key: String, value: Boolean?) {
    if (value == null) putNull(key) else putBoolean(key, value)
  }

  private data class AccessibilityState(
    val settingEnabled: Boolean,
    val serviceBound: Boolean,
    val processAlive: Boolean,
    val storedServices: String,
    val health: String,
  ) {
    val authorized: Boolean
      get() = settingEnabled && serviceBound
    val running: Boolean
      get() = settingEnabled && serviceBound && processAlive && health == ClearScreenAccessibilityHealth.READY
  }

  private fun readAccessibilityState(verify: Boolean = false): AccessibilityState {
    val component = ComponentName(reactContext, ClearScreenAccessibilityService::class.java)
    val storedServices = Settings.Secure.getString(
      reactContext.contentResolver,
      Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
    ).orEmpty()
    val settingEnabled = storedServices.split(':').any {
      val saved = ComponentName.unflattenFromString(it)
      saved != null && saved.packageName.equals(component.packageName, ignoreCase = true) &&
        saved.className.equals(component.className, ignoreCase = true)
    }
    val serviceBound = settingEnabled && ClearScreenAccessibilityService.running
    // This module and the accessibility service use the same default process.
    // Executing this call proves that process is alive, not that the service is bound.
    val processAlive = android.os.Process.myPid() > 0
    val health = if (!serviceBound && ClearScreenAccessibilityRecovery.isPending) ClearScreenAccessibilityHealth.UNKNOWN
      else if (!serviceBound) ClearScreenAccessibilityHealth.UNAVAILABLE
      else if (verify && hostResumed) ClearScreenAccessibilityHealth.check(reactContext.currentActivity)
      else ClearScreenAccessibilityHealth.current()
    return AccessibilityState(
      settingEnabled = settingEnabled,
      serviceBound = serviceBound,
      processAlive = processAlive,
      storedServices = storedServices,
      health = health,
    )
  }

  private fun isVivoFamilyDevice(): Boolean {
    val identity = listOf(Build.MANUFACTURER, Build.BRAND, Build.PRODUCT)
      .joinToString(" ")
      .lowercase(Locale.ROOT)
    return identity.contains("vivo") || identity.contains("iqoo")
  }

  private fun openSettingsPage(promise: Promise, open: () -> Unit) {
    try {
      open()
      promise.resolve(true)
    } catch (error: Exception) {
      promise.reject("SETTINGS_OPEN_FAILED", error)
    }
  }

  private fun openObservedSettings(promise: Promise, kind: ClearScreenSettingsObserver.Kind, open: () -> Unit) {
    val deadline = SystemClock.elapsedRealtime() + 5_000L
    try {
      requirePrivacyConsent()
      ClearScreenSettingsObserver.begin(reactContext, kind)
      check(hostResumed && SystemClock.elapsedRealtime() < deadline) { "设置读取超时或应用已离开前台，请返回后重试" }
      open()
      promise.resolve(true)
    } catch (error: Exception) {
      ClearScreenSettingsObserver.clear()
      promise.reject("SETTINGS_OPEN_FAILED", error)
    }
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

  private fun <T> runAsync(promise: Promise, worker: ExecutorService = executor, block: () -> T) {
    val deadline = SystemClock.elapsedRealtime() + if (worker === snapshotExecutor) 12_000L else if (worker === healthExecutor) 2_000L else 4_000L
    try {
      worker.execute {
        if (SystemClock.elapsedRealtime() >= deadline) {
          promise.reject("BACKEND_TIMEOUT", "请求等待超时，请重试")
          return@execute
        }
        try {
          promise.resolve(block())
        } catch (error: Exception) {
          promise.reject("BACKEND_ERROR", error)
        }
      }
    } catch (error: Exception) {
      promise.reject("BACKEND_BUSY", "读取繁忙，请稍后重试", error)
    }
  }

  /** Commit on the command queue; app enumeration must not hold that queue. */
  private fun runSnapshotMutation(promise: Promise, mutation: () -> Unit) {
    val deadline = SystemClock.elapsedRealtime() + 4_000L
    try {
      executor.execute {
        if (SystemClock.elapsedRealtime() >= deadline) {
          promise.reject("BACKEND_TIMEOUT", "操作等待超时，尚未执行")
          return@execute
        }
        try {
          mutation()
          runAsync(promise, snapshotExecutor) { snapshot() }
        } catch (error: Exception) { promise.reject("BACKEND_ERROR", error) }
      }
    } catch (error: Exception) { promise.reject("BACKEND_BUSY", "操作繁忙，请稍后重试", error) }
  }

}

package com.clearscreen.prototype.backend

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class AppRuleState(
  val skipEnabled: Boolean = false,
  val networkBlockEnabled: Boolean = false,
  val whitelist: Boolean = false,
)

data class StoredEvent(
  val type: String,
  val packageName: String?,
  val ruleId: String?,
  val result: String,
  val timestamp: Long,
  val duration: Long,
  val text: String,
)

class ClearScreenStore(context: Context) {
  private val lock = Any()
  // Keep the existing preference file and additive migration path. The UI and
  // accessibility service now share the app process, while this mode remains
  // compatible with installations that are upgrading from the old service process.
  @Suppress("DEPRECATION")
  private val preferences = context.getSharedPreferences(
    PREFERENCES,
    Context.MODE_PRIVATE or Context.MODE_MULTI_PROCESS,
  )

  /**
   * SharedPreferences survives an in-place APK update as long as the package name and
   * signing key stay the same. Keep migrations additive: never call clear() and never
   * replace the preference file, so existing rules, settings, and event history remain.
   */
  private fun migrateStorageIfNeeded() = synchronized(lock) {
    val storedVersion = preferences.getInt(KEY_STORAGE_VERSION, 0)
    val hasOnboardingState = preferences.contains(KEY_ONBOARDING_COMPLETED)
    if (storedVersion >= CURRENT_STORAGE_VERSION && hasOnboardingState) return@synchronized

    val editor = preferences.edit()
    if (storedVersion < CURRENT_STORAGE_VERSION) {
      if (!preferences.contains(KEY_APP_RULES)) editor.putString(KEY_APP_RULES, "{}")
      if (!preferences.contains(KEY_EVENTS)) editor.putString(KEY_EVENTS, "[]")
      editor.putInt(KEY_STORAGE_VERSION, CURRENT_STORAGE_VERSION)
    }
    if (!hasOnboardingState) {
      val hasLegacyUserData = LEGACY_DATA_KEYS.any(preferences::contains)
      editor.putBoolean(
        KEY_ONBOARDING_COMPLETED,
        storedVersion >= CURRENT_STORAGE_VERSION || hasLegacyUserData,
      )
    }
    editor.apply()
  }

  /** Called only after the user has accepted the current privacy policy. */
  fun prepareAfterConsent() {
    migrateStorageIfNeeded()
  }

  /** Clear all app-owned local data after an explicit withdrawal request. */
  fun clearUserData(): Boolean = synchronized(lock) {
    preferences.edit().clear().commit()
  }

  fun isOnboardingCompleted(): Boolean =
    preferences.getBoolean(KEY_ONBOARDING_COMPLETED, false)

  fun setOnboardingCompleted(completed: Boolean) {
    preferences.edit().putBoolean(KEY_ONBOARDING_COMPLETED, completed).apply()
  }

  fun isMasterEnabled(): Boolean = preferences.getBoolean(KEY_MASTER_ENABLED, false)

  fun setMasterEnabled(enabled: Boolean) {
    preferences.edit().putBoolean(KEY_MASTER_ENABLED, enabled).apply()
  }

  fun getAppRule(packageName: String): AppRuleState = synchronized(lock) {
    val rules = JSONObject(preferences.getString(KEY_APP_RULES, "{}") ?: "{}")
    // New apps remain off until the user explicitly enables their rule.
    val value = rules.optJSONObject(packageName) ?: return@synchronized AppRuleState()
    AppRuleState(
      skipEnabled = value.optBoolean("skipEnabled", false),
      networkBlockEnabled = value.optBoolean("networkBlockEnabled", false),
      whitelist = value.optBoolean("whitelist", false),
    )
  }

  fun setAppRule(packageName: String, rule: AppRuleState) = synchronized(lock) {
    val rules = JSONObject(preferences.getString(KEY_APP_RULES, "{}") ?: "{}")
    val safeRule = if (rule.whitelist) {
      rule.copy(skipEnabled = false, networkBlockEnabled = false)
    } else {
      rule
    }
    rules.put(packageName, JSONObject().apply {
      put("skipEnabled", safeRule.skipEnabled)
      put("networkBlockEnabled", safeRule.networkBlockEnabled)
      put("whitelist", safeRule.whitelist)
    })
    preferences.edit().putString(KEY_APP_RULES, rules.toString()).apply()
  }

  fun isSettingEnabled(key: String, defaultValue: Boolean): Boolean =
    preferences.getBoolean(key, defaultValue)

  fun setSettingEnabled(key: String, enabled: Boolean) {
    preferences.edit().putBoolean(key, enabled).apply()
  }

  fun getLogRetentionDays(): Int =
    preferences.getInt(KEY_LOG_RETENTION_DAYS, DEFAULT_LOG_RETENTION_DAYS)
      .takeIf { it in LOG_RETENTION_OPTIONS }
      ?: DEFAULT_LOG_RETENTION_DAYS

  fun setLogRetentionDays(days: Int) = synchronized(lock) {
    require(days in LOG_RETENTION_OPTIONS) { "Unsupported log retention: $days" }
    preferences.edit().putInt(KEY_LOG_RETENTION_DAYS, days).apply()
    pruneExpiredEventsLocked(readEventsLocked())
  }

  /**
   * vivo/iQOO expose their startup protection in a vendor-owned settings page.
   * Android does not provide a public API to read that switch, so this only
   * records that the user completed the guided check. It is deliberately not
   * used as proof that the system has granted any permission.
   */
  fun isVendorStartupGuideConfirmed(): Boolean =
    preferences.getBoolean(KEY_VENDOR_STARTUP_GUIDE_CONFIRMED, false)

  fun setVendorStartupGuideConfirmed(confirmed: Boolean) {
    preferences.edit().putBoolean(KEY_VENDOR_STARTUP_GUIDE_CONFIRMED, confirmed).apply()
  }

  fun getRecentEvents(
    limit: Int = 100,
    excludedPackageName: String? = null,
  ): List<StoredEvent> = synchronized(lock) {
    val events = pruneExpiredEventsLocked(readEventsLocked())
    val result = mutableListOf<StoredEvent>()
    val start = maxOf(0, events.length() - limit)
    for (index in events.length() - 1 downTo start) {
      val event = events.optJSONObject(index) ?: continue
      if (excludedPackageName != null &&
        event.optString("packageName", "") == excludedPackageName
      ) continue
      result += StoredEvent(
        type = event.optString("type", "fail"),
        packageName = event.optString("packageName", "").ifBlank { null },
        ruleId = event.optString("ruleId", "").ifBlank { null },
        result = event.optString("result", "failed"),
        timestamp = event.optLong("timestamp", 0L),
        duration = event.optLong("duration", 0L),
        text = event.optString("text", "处理失败"),
      )
    }
    result
  }

  fun appendEvent(
    type: String,
    packageName: String?,
    ruleId: String?,
    result: String,
    text: String,
    duration: Long = 0L,
  ) = synchronized(lock) {
    if (getLogRetentionDays() == 0) return@synchronized
    val events = pruneExpiredEventsLocked(readEventsLocked())
    events.put(JSONObject().apply {
      put("type", type)
      put("packageName", packageName ?: "")
      put("ruleId", ruleId ?: "")
      put("result", result)
      put("timestamp", System.currentTimeMillis())
      put("duration", duration)
      put("text", text)
    })
    val first = maxOf(0, events.length() - MAX_EVENTS)
    val trimmed = JSONArray()
    for (index in first until events.length()) trimmed.put(events.opt(index))
    preferences.edit().putString(KEY_EVENTS, trimmed.toString()).apply()
  }

  fun clearEvents() = synchronized(lock) {
    preferences.edit().putString(KEY_EVENTS, "[]").apply()
  }

  private fun readEventsLocked(): JSONArray =
    JSONArray(preferences.getString(KEY_EVENTS, "[]") ?: "[]")

  private fun pruneExpiredEventsLocked(events: JSONArray): JSONArray {
    val retentionDays = getLogRetentionDays()
    val cutoff = if (retentionDays == 0) {
      Long.MAX_VALUE
    } else {
      System.currentTimeMillis() - retentionDays * MILLIS_PER_DAY
    }
    val retained = JSONArray()
    for (index in 0 until events.length()) {
      val event = events.optJSONObject(index) ?: continue
      if (retentionDays > 0 && event.optLong("timestamp", 0L) >= cutoff) {
        retained.put(event)
      }
    }
    if (retained.length() != events.length()) {
      preferences.edit().putString(KEY_EVENTS, retained.toString()).apply()
    }
    return retained
  }

  fun countToday(type: String, excludedPackageName: String? = null): Int {
    val day = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    return getRecentEvents(MAX_EVENTS, excludedPackageName).count { event ->
      event.type == type &&
        event.result == "success" &&
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(event.timestamp)) == day
    }
  }

  fun shouldBlockDomain(domain: String): Boolean {
    val normalized = domain.trim().lowercase(Locale.US).trimEnd('.')
    if (normalized.isBlank()) return false
    return DEFAULT_NETWORK_DOMAINS.any { normalized == it || normalized.endsWith(".$it") }
  }

  companion object {
    private const val PREFERENCES = "clearscreen_backend"
    private const val KEY_STORAGE_VERSION = "storageSchemaVersion"
    private const val KEY_ONBOARDING_COMPLETED = "onboardingCompleted"
    private const val KEY_MASTER_ENABLED = "masterEnabled"
    private const val KEY_APP_RULES = "appRules"
    private const val KEY_EVENTS = "events"
    private const val CURRENT_STORAGE_VERSION = 1
    const val KEY_STARTUP = "startupEnabled"
    const val KEY_AUTO_UPDATE = "autoUpdateEnabled"
    const val KEY_DEBUG = "debugEnabled"
    const val KEY_SKIP_SPLASH_VIDEO = "skipSplashVideoEnabled"
    const val KEY_CLOSE_POPUP_ADS = "closePopupAdsEnabled"
    const val KEY_CLOSE_FEED_ADS = "closeFeedAdsEnabled"
    private const val KEY_LOG_RETENTION_DAYS = "logRetentionDays"
    private const val KEY_VENDOR_STARTUP_GUIDE_CONFIRMED = "vendorStartupGuideConfirmed"
    const val DEFAULT_LOG_RETENTION_DAYS = 7
    private const val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L
    private const val MAX_EVENTS = 5000
    private val LOG_RETENTION_OPTIONS = setOf(0, 1, 7, 30)
    private val LEGACY_DATA_KEYS = setOf(
      KEY_MASTER_ENABLED,
      KEY_APP_RULES,
      KEY_EVENTS,
      KEY_STARTUP,
      KEY_AUTO_UPDATE,
      KEY_DEBUG,
    )

    // These are conservative seed rules for the first local PoC. They are data rules,
    // not UI decoration, and can be replaced by a local rules file later.
    private val DEFAULT_NETWORK_DOMAINS = setOf(
      "doubleclick.net",
      "googlesyndication.com",
      "adnxs.com",
      "adsrvr.org",
      "adservice.google.com",
    )
  }
}

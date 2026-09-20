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
  private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

  init {
    migrateStorageIfNeeded()
  }

  /**
   * SharedPreferences survives an in-place APK update as long as the package name and
   * signing key stay the same. Keep migrations additive: never call clear() and never
   * replace the preference file, so existing rules, settings, and event history remain.
   */
  private fun migrateStorageIfNeeded() = synchronized(lock) {
    val storedVersion = preferences.getInt(KEY_STORAGE_VERSION, 0)
    if (storedVersion >= CURRENT_STORAGE_VERSION) return@synchronized

    val editor = preferences.edit()
    if (!preferences.contains(KEY_APP_RULES)) editor.putString(KEY_APP_RULES, "{}")
    if (!preferences.contains(KEY_EVENTS)) editor.putString(KEY_EVENTS, "[]")
    editor.putInt(KEY_STORAGE_VERSION, CURRENT_STORAGE_VERSION).apply()
  }

  fun isMasterEnabled(): Boolean = preferences.getBoolean(KEY_MASTER_ENABLED, false)

  fun setMasterEnabled(enabled: Boolean) {
    preferences.edit().putBoolean(KEY_MASTER_ENABLED, enabled).apply()
  }

  fun getAppRule(packageName: String): AppRuleState = synchronized(lock) {
    val rules = JSONObject(preferences.getString(KEY_APP_RULES, "{}") ?: "{}")
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

  fun getRecentEvents(limit: Int = 100): List<StoredEvent> = synchronized(lock) {
    val events = JSONArray(preferences.getString(KEY_EVENTS, "[]") ?: "[]")
    val result = mutableListOf<StoredEvent>()
    val start = maxOf(0, events.length() - limit)
    for (index in events.length() - 1 downTo start) {
      val event = events.optJSONObject(index) ?: continue
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
    val events = JSONArray(preferences.getString(KEY_EVENTS, "[]") ?: "[]")
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

  fun clearEvents() {
    preferences.edit().putString(KEY_EVENTS, "[]").apply()
  }

  fun countToday(type: String): Int {
    val day = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    return getRecentEvents(MAX_EVENTS).count { event ->
      event.type == type && SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(event.timestamp)) == day
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
    private const val KEY_MASTER_ENABLED = "masterEnabled"
    private const val KEY_APP_RULES = "appRules"
    private const val KEY_EVENTS = "events"
    private const val CURRENT_STORAGE_VERSION = 1
    const val KEY_STARTUP = "startupEnabled"
    const val KEY_AUTO_UPDATE = "autoUpdateEnabled"
    const val KEY_DEBUG = "debugEnabled"
    const val MAX_EVENTS = 200

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

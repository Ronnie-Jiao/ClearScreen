package com.clearscreen.prototype.backend

import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClearScreenStoreTest {
  @Test fun freshInstallAndLaterInstalledAppsAreEnabledByDefault() {
    val preferences = MemoryPreferences()
    val store = ClearScreenStore(preferences)
    store.prepareAfterConsent()

    assertTrue(store.getAppRule("com.example.first").skipEnabled)
    store.setAppRule("com.example.first", AppRuleState(skipEnabled = false))
    assertTrue(store.getAppRule("com.example.installedlater").skipEnabled)
    assertFalse(store.getAppRule("com.example.installedlater").whitelist)
    assertFalse(store.isMasterEnabled())
  }

  @Test fun manualOptOutSurvivesRefreshRestartAndStoragePreparation() {
    val preferences = MemoryPreferences()
    ClearScreenStore(preferences).apply {
      prepareAfterConsent()
      setAppRule("com.example.excluded", AppRuleState(skipEnabled = false))
    }

    val restartedStore = ClearScreenStore(preferences)
    restartedStore.prepareAfterConsent()
    repeat(3) {
      assertFalse(restartedStore.getAppRule("com.example.excluded").skipEnabled)
      assertTrue(restartedStore.getAppRule("com.example.excluded").whitelist)
      assertTrue(restartedStore.getAppRule("com.example.other").skipEnabled)
    }
  }

  @Test fun existingSavedOffAndWhitelistChoicesAreNotResetOnUpgrade() {
    val preferences = MemoryPreferences()
    preferences.edit().putString("appRules", """{
      "com.example.off":{"skipEnabled":false,"whitelist":false},
      "com.example.whitelisted":{"skipEnabled":true,"whitelist":true},
      "com.example.on":{"skipEnabled":true,"whitelist":false}
    }""").putInt("storageSchemaVersion", 1).apply()

    val upgradedStore = ClearScreenStore(preferences)
    upgradedStore.prepareAfterConsent()
    assertFalse(upgradedStore.getAppRule("com.example.off").skipEnabled)
    assertTrue(upgradedStore.getAppRule("com.example.off").whitelist)
    assertFalse(upgradedStore.getAppRule("com.example.whitelisted").skipEnabled)
    assertTrue(upgradedStore.getAppRule("com.example.on").skipEnabled)
    assertTrue(upgradedStore.getAppRule("com.example.unconfigured").skipEnabled)
  }

  @Test fun enableAllClearsAllTargetExclusionsInOneDurableWrite() {
    val preferences = MemoryPreferences()
    val store = ClearScreenStore(preferences)
    val packages = (1..179).map { "com.example.app$it" }
    store.setAppRules(packages.associateWith { AppRuleState(skipEnabled = false, whitelist = true) })
    preferences.ruleCommits = 0

    store.setAppRules(packages.associateWith { AppRuleState(skipEnabled = true, whitelist = false) })

    assertEquals(1, preferences.ruleCommits)
    val restartedStore = ClearScreenStore(preferences)
    packages.forEach { packageName ->
      assertTrue(packageName, restartedStore.getAppRule(packageName).skipEnabled)
      assertFalse(packageName, restartedStore.getAppRule(packageName).whitelist)
    }
  }

  @Test fun individualChangeDoesNotOverwriteOtherAppsOrSettings() {
    val preferences = MemoryPreferences()
    val store = ClearScreenStore(preferences)
    store.prepareAfterConsent()
    store.setMasterEnabled(true)
    store.setLogRetentionDays(30)
    store.setAppRule("com.example.keepoff", AppRuleState(skipEnabled = false))
    store.setAppRules(mapOf("com.example.changed" to AppRuleState(skipEnabled = false)))
    store.setAppRules(mapOf("com.example.changed" to AppRuleState(skipEnabled = true)))

    val restartedStore = ClearScreenStore(preferences)
    assertFalse(restartedStore.getAppRule("com.example.keepoff").skipEnabled)
    assertTrue(restartedStore.getAppRule("com.example.changed").skipEnabled)
    assertTrue(restartedStore.isMasterEnabled())
    assertEquals(30, restartedStore.getLogRetentionDays())
  }

  @Test(expected = IllegalStateException::class)
  fun saveFailureReachesTheCallerInsteadOfReportingSuccess() {
    val preferences = MemoryPreferences()
    preferences.failNextCommit = true
    ClearScreenStore(preferences).setAppRules(mapOf("com.example.app" to AppRuleState()))
  }

  /** Reusing this backing store models preferences retained across app instances. */
  private class MemoryPreferences : SharedPreferences {
    private val values = mutableMapOf<String, Any?>()
    var ruleCommits = 0
    var failNextCommit = false

    override fun getAll(): MutableMap<String, *> = values.toMutableMap()
    override fun getString(key: String?, defValue: String?): String? = values[key] as? String ?: defValue
    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
      (values[key] as? Set<*>)?.filterIsInstance<String>()?.toMutableSet() ?: defValues
    override fun getInt(key: String?, defValue: Int): Int = values[key] as? Int ?: defValue
    override fun getLong(key: String?, defValue: Long): Long = values[key] as? Long ?: defValue
    override fun getFloat(key: String?, defValue: Float): Float = values[key] as? Float ?: defValue
    override fun getBoolean(key: String?, defValue: Boolean): Boolean = values[key] as? Boolean ?: defValue
    override fun contains(key: String?): Boolean = values.containsKey(key)
    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit
    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit
    override fun edit(): SharedPreferences.Editor = Editor()

    private inner class Editor : SharedPreferences.Editor {
      private val updates = mutableMapOf<String, Any?>()
      private var clearRequested = false
      override fun putString(key: String?, value: String?): SharedPreferences.Editor = apply { updates[requireNotNull(key)] = value }
      override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor = apply { updates[requireNotNull(key)] = values?.toSet() }
      override fun putInt(key: String?, value: Int): SharedPreferences.Editor = apply { updates[requireNotNull(key)] = value }
      override fun putLong(key: String?, value: Long): SharedPreferences.Editor = apply { updates[requireNotNull(key)] = value }
      override fun putFloat(key: String?, value: Float): SharedPreferences.Editor = apply { updates[requireNotNull(key)] = value }
      override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor = apply { updates[requireNotNull(key)] = value }
      override fun remove(key: String?): SharedPreferences.Editor = apply { updates[requireNotNull(key)] = null }
      override fun clear(): SharedPreferences.Editor = apply { clearRequested = true }
      override fun apply() { commit() }
      override fun commit(): Boolean {
        if (failNextCommit) {
          failNextCommit = false
          return false
        }
        if (clearRequested) values.clear()
        updates.forEach { (key, value) -> if (value == null) values.remove(key) else values[key] = value }
        if (updates.containsKey("appRules")) ruleCommits++
        return true
      }
    }
  }
}

package com.clearscreen.prototype.backend

import android.content.Context
import android.provider.Settings

/**
 * Direct access to the Android settings tables.
 *
 * A vendor recommendation switch can only be rewritten directly when its state
 * is stored in Settings.Secure, Settings.System or Settings.Global. Switches
 * that a vendor app keeps in its own private data have no key here and keep
 * the page assisted flow, because no other app can write that data.
 */
internal object ClearScreenDirectSettings {
  data class Spec(val namespace: String, val key: String, val onValue: String, val offValue: String) {
    /** "on" or "off" for a raw stored value; "unknown" for anything else. */
    fun stateOf(value: String?): String = when (value) {
      onValue -> "on"
      offValue -> "off"
      else -> "unknown"
    }
  }

  fun spec(target: ClearScreenUiCleanup.Target): Spec? {
    val key = target.settingKey ?: return null
    return Spec(target.settingNamespace, key, target.settingOn, target.settingOff)
  }

  /** Reading a settings table entry needs no special permission. */
  fun readLocal(context: Context, spec: Spec): String? = try {
    when (spec.namespace) {
      "secure" -> Settings.Secure.getString(context.contentResolver, spec.key)
      "system" -> Settings.System.getString(context.contentResolver, spec.key)
      "global" -> Settings.Global.getString(context.contentResolver, spec.key)
      else -> null
    }
  } catch (_: Exception) {
    null
  }
}

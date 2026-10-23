package com.clearscreen.prototype.backend

import android.os.Build
import java.util.Locale

/** Shared identity for navigation and state readers; no grant is inferred from a brand. */
internal enum class ClearScreenOemProfile(val settingsPackages: Set<String>) {
  VIVO(setOf("com.vivo.permissionmanager", "com.iqoo.secure")),
  XIAOMI(setOf("com.miui.securitycenter", "com.miui.powerkeeper")),
  HUAWEI(setOf("com.huawei.systemmanager")),
  HONOR(setOf("com.hihonor.systemmanager", "com.huawei.systemmanager")),
  OPPO(setOf("com.oplus.safecenter", "com.coloros.safecenter", "com.oppo.safe", "com.oplus.battery", "com.coloros.oppoguardelf")),
  ONEPLUS(setOf("com.oneplus.security", "com.oplus.safecenter", "com.coloros.safecenter", "com.oplus.battery", "com.coloros.oppoguardelf")),
  SAMSUNG(setOf("com.samsung.android.lool", "com.samsung.android.sm")),
  MEIZU(setOf("com.meizu.safe", "com.meizu.power")),
  LENOVO(setOf("com.lenovo.security", "com.lenovo.safecenter", "com.zui.safecenter")),
  MOTOROLA(emptySet()),
  ZTE(setOf("com.zte.heartyservice", "cn.nubia.security2", "cn.nubia.security")),
  ASUS(setOf("com.asus.mobilemanager")),
  NOKIA(setOf("com.evenwell.powersaving.g3")),
  SONY(emptySet()),
  GOOGLE(emptySet()),
  NOTHING(emptySet()),
  TRANSSION(setOf("com.transsion.phonemaster")),
  LETV(setOf("com.letv.android.letvsafe")),
  OTHER(emptySet());

  val readableSettingsPackages: Set<String>
    get() = settingsPackages + "com.android.settings"

  companion object {
    fun current(): ClearScreenOemProfile =
      listOf(Build.BRAND, Build.MANUFACTURER).firstNotNullOfOrNull { identity ->
        when (identity.orEmpty().lowercase(Locale.ROOT)) {
          "vivo", "iqoo" -> VIVO
          "xiaomi", "redmi", "poco" -> XIAOMI
          "huawei" -> HUAWEI
          "honor" -> HONOR
          "oppo", "realme" -> OPPO
          "oneplus" -> ONEPLUS
          "samsung" -> SAMSUNG
          "meizu" -> MEIZU
          "lenovo", "zuk", "zui" -> LENOVO
          "motorola", "moto" -> MOTOROLA
          "zte", "nubia", "redmagic", "red magic" -> ZTE
          "asus", "rog" -> ASUS
          "nokia", "hmd" -> NOKIA
          "sony" -> SONY
          "google" -> GOOGLE
          "nothing", "cmf" -> NOTHING
          "infinix", "tecno", "itel" -> TRANSSION
          "letv", "leeco" -> LETV
          else -> null
        }
      } ?: OTHER
  }
}

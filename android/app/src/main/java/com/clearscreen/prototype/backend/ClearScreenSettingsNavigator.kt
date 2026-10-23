package com.clearscreen.prototype.backend

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings

/** OEM entrypoints are optional: an available settings page is not a permission grant. */
internal class ClearScreenSettingsNavigator(private val context: Context) {
  private val family = ClearScreenOemProfile.current()

  fun hasStartupSettings(): Boolean = startupCandidates().any(::canOpen)

  fun openStartupSettings() = openFirst(startupCandidates() + appSettingsFallbacks())

  fun openBackgroundSettings() = openFirst(backgroundCandidates() + appSettingsFallbacks())

  fun openBatteryOptimizationSettings() = openFirst(
    listOf(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) + appSettingsFallbacks(),
  )

  // Component identifiers are interoperability candidates, not stable Android APIs.
  // Reference: github.com/judemanutd/AutoStarter (AutoStartPermissionHelper.kt).
  private fun startupCandidates(): List<Intent> = when (family) {
    ClearScreenOemProfile.VIVO -> listOf(
      Intent(VIVO_STARTUP_ACTION).setPackage("com.vivo.permissionmanager"),
      Intent(VIVO_STARTUP_ACTION).setPackage("com.iqoo.secure"),
      component("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"),
      component("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager"),
      component("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"),
    )
    ClearScreenOemProfile.XIAOMI -> listOf(
      component("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"),
    )
    ClearScreenOemProfile.HUAWEI -> huaweiStartupCandidates("com.huawei.systemmanager")
    ClearScreenOemProfile.HONOR -> huaweiStartupCandidates("com.hihonor.systemmanager") +
      huaweiStartupCandidates("com.huawei.systemmanager")
    ClearScreenOemProfile.OPPO -> colorOsStartupCandidates()
    ClearScreenOemProfile.ONEPLUS -> listOf(
      component("com.oneplus.security", "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity"),
    ) + colorOsStartupCandidates()
    ClearScreenOemProfile.ASUS -> listOf(
      component("com.asus.mobilemanager", "com.asus.mobilemanager.autostart.AutoStartActivity"),
      component("com.asus.mobilemanager", "com.asus.mobilemanager.powersaver.PowerSaverSettings"),
    )
    ClearScreenOemProfile.LETV -> listOf(
      component("com.letv.android.letvsafe", "com.letv.android.letvsafe.AutobootManageActivity"),
    )
    // These systems do not expose a common app-autostart page; use app information.
    else -> emptyList()
  }

  private fun huaweiStartupCandidates(packageName: String): List<Intent> = listOf(
    component(packageName, "$packageName.startupmgr.ui.StartupNormalAppListActivity"),
    component(packageName, "$packageName.optimize.process.ProtectActivity"),
  )

  private fun colorOsStartupCandidates(): List<Intent> = listOf(
    component("com.oplus.safecenter", "com.oplus.safecenter.startupapp.StartupAppListActivity"),
    component("com.oplus.safecenter", "com.oplus.safecenter.permission.startup.StartupAppListActivity"),
    component("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"),
    component("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity"),
    component("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity"),
  )

  private fun backgroundCandidates(): List<Intent> = when (family) {
    ClearScreenOemProfile.HUAWEI -> listOf(
      component("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity"),
    )
    ClearScreenOemProfile.HONOR -> listOf(
      component("com.hihonor.systemmanager", "com.hihonor.systemmanager.optimize.process.ProtectActivity"),
      component("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity"),
    )
    ClearScreenOemProfile.ONEPLUS -> listOf(
      Intent("com.android.settings.action.BACKGROUND_OPTIMIZE").setPackage("com.android.settings")
        .putExtra("packageName", context.packageName),
    )
    ClearScreenOemProfile.SAMSUNG -> listOf("com.samsung.android.lool", "com.samsung.android.sm").flatMap { packageName ->
      listOf(
        component(packageName, "com.samsung.android.sm.battery.ui.BatteryActivity"),
        component(packageName, "com.samsung.android.sm.ui.battery.BatteryActivity"),
      )
    }
    ClearScreenOemProfile.ASUS -> listOf(
      component("com.asus.mobilemanager", "com.asus.mobilemanager.powersaver.PowerSaverSettings"),
    )
    ClearScreenOemProfile.NOKIA -> listOf(
      component("com.evenwell.powersaving.g3", "com.evenwell.powersaving.g3.exception.PowerSaverExceptionActivity"),
    )
    else -> emptyList()
  }

  private fun component(packageName: String, className: String): Intent = Intent().apply {
    component = ComponentName(packageName, className)
    putExtra("packageName", context.packageName)
    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
  }

  private fun appSettingsFallbacks(): List<Intent> = listOf(
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(Uri.parse("package:${context.packageName}")),
    Intent(Settings.ACTION_APPLICATION_SETTINGS),
    Intent(Settings.ACTION_SETTINGS),
  )

  private fun canOpen(intent: Intent): Boolean = runCatching {
    val activity = intent.resolveActivityInfo(context.packageManager, 0) ?: return@runCatching false
    activity.enabled && activity.applicationInfo.enabled && activity.exported &&
      (activity.permission.isNullOrEmpty() ||
        context.checkSelfPermission(activity.permission) == PackageManager.PERMISSION_GRANTED)
  }.getOrDefault(false)

  private fun openFirst(candidates: List<Intent>) {
    var lastError: RuntimeException? = null
    for (candidate in candidates) {
      if (!canOpen(candidate)) continue
      try {
        context.startActivity(Intent(candidate).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        return
      } catch (error: RuntimeException) {
        // OEM pages may resolve but reject launching; continue to the next available destination.
        lastError = error
      }
    }
    throw lastError ?: ActivityNotFoundException("No available system settings screen")
  }

  companion object {
    private const val VIVO_STARTUP_ACTION = "com.iqoo.secure.BGSTARTUPMANAGER"
  }
}

package com.clearscreen.prototype.backend

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast

/** OEM entrypoints are optional: an available settings page is not a permission grant. */
internal class ClearScreenSettingsNavigator(private val context: Context) {
  private val family = ClearScreenOemProfile.current()

  fun hasStartupSettings(): Boolean = startupCandidates().any(::canOpen)

  fun openStartupSettings() = openFirst(startupCandidates() + appSettingsFallbacks())

  fun openBackgroundSettings() = openFirst(backgroundCandidates() + appSettingsFallbacks())

  fun openBatteryOptimizationSettings() = openFirst(
    listOf(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) + appSettingsFallbacks(),
  )

  fun openSystemPrivacySettings() = openFirst(
    listOf(Intent(Settings.ACTION_PRIVACY_SETTINGS), Intent(Settings.ACTION_SETTINGS)),
  )

  /** Where a recommendation route actually landed, so the page can tell the user what happened. */
  data class RouteResult(val level: String, val component: String, val hint: String?)

  /**
   * Opens one registered destination, degrading vendor page → standard intent → app info page.
   * Component and action identifiers are interoperability candidates, not stable Android APIs.
   */
  fun openRecommendRoute(route: String): RouteResult {
    for (entry in ClearScreenRecommendRoutes.candidates(route, context, family)) {
      if (!canOpen(entry.intent)) continue
      try {
        context.startActivity(Intent(entry.intent).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        return RouteResult(entry.level, describe(entry.intent), entry.hint)
      } catch (_: ActivityNotFoundException) {
        // The page disappeared between resolve and launch; fall through to the next level.
      } catch (_: RuntimeException) {
        // Resolved but rejected by the OEM: keep trying the rest of the list.
      }
    }
    val appDetails = ClearScreenRecommendRoutes.appDetails(context)
    if (tryOpen(listOf(appDetails))) {
      return RouteResult(ClearScreenRecommendRoutes.LEVEL_APP_DETAILS, describe(appDetails),
        appDetailsHint(route))
    }
    throw ActivityNotFoundException("No available system settings screen")
  }

  /** Tells the user exactly where to tap when only the app info page was reachable. */
  private fun appDetailsHint(route: String): String = when (route) {
    ClearScreenRecommendCatalog.ROUTE_STORE ->
      "没找到应用商店设置，只打开了净屏的应用信息页；请从桌面打开应用商店，再进「我的 → 设置」关闭推荐"
    ClearScreenRecommendCatalog.ROUTE_NEWS ->
      "系统没有开放推荐设置页，只打开了净屏的应用信息页；请在系统设置中搜索「推荐」或进入负一屏设置"
    else ->
      "系统没有开放这个设置页，只打开了净屏的应用信息页，请回到系统设置手动查找"
  }

  fun openSystemAdPrivacySettings() {
    openRecommendRoute(ClearScreenRecommendCatalog.ROUTE_AD_PRIVACY)
  }

  private fun describe(intent: Intent): String =
    intent.component?.flattenToShortString() ?: intent.action ?: "unknown"

  private fun isMiui(): Boolean = Build.MANUFACTURER.equals("Xiaomi", ignoreCase = true)

  /** Opens developer options, or the system settings home when the OEM blocks direct navigation. */
  fun openDeveloperOptions(): String {
    // MIUI/HyperOS redirects third-party launches of the developer sub-page to this app's details.
    // Open the settings home instead; Xiaomi's user path is Additional settings → Developer options.
    if (!isMiui() && tryOpen(developerCandidates())) return "developer"

    openSystemHomeSettings()
    val nextStep = if (isMiui()) {
      "已打开系统设置，请前往「更多设置 → 开发者选项 → 无线调试」"
    } else {
      "已打开系统设置，请搜索「开发者选项」并进入「无线调试」"
    }
    notifySettingsFallback(nextStep)
    return "settingsHome"
  }

  fun openSystemHomeSettings() = openFirst(listOf(Intent(Settings.ACTION_SETTINGS)))

  fun openHomeScreen() = openFirst(listOf(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)))

  fun openVivoBrowser() {
    check(family == ClearScreenOemProfile.VIVO) { "当前设备不支持此入口" }
    val intent = context.packageManager.getLaunchIntentForPackage("com.vivo.browser")
      ?: throw ActivityNotFoundException("未找到 vivo 浏览器")
    context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
  }

  fun openVendorStore() {
    val packageName = when (family) {
      ClearScreenOemProfile.VIVO -> "com.bbk.appstore"
      ClearScreenOemProfile.XIAOMI -> "com.xiaomi.market"
      ClearScreenOemProfile.OPPO, ClearScreenOemProfile.ONEPLUS -> "com.heytap.market"
      ClearScreenOemProfile.HONOR -> "com.hihonor.appmarket"
      ClearScreenOemProfile.HUAWEI -> "com.huawei.appmarket"
      else -> null
    } ?: throw ActivityNotFoundException("未找到此品牌的应用商店入口")
    val intent = context.packageManager.getLaunchIntentForPackage(packageName)
      ?: throw ActivityNotFoundException("未找到可打开的应用商店")
    context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
  }

  fun openAppNotificationSettings(packageName: String) {
    require(packageName.matches(Regex("[A-Za-z0-9_]+(\\.[A-Za-z0-9_]+)+"))) {
      "Invalid application package name"
    }
    require(packageName != context.packageName) { "The target application is unavailable" }
    context.packageManager.getApplicationInfo(packageName, PackageManager.MATCH_ALL)
    val candidates = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      appNotificationCandidates(packageName)
    } else {
      emptyList()
    }
    if (tryOpen(candidates)) return
    openAppDetailsOrThrow("已打开该应用的信息页，请点「通知」进入通知管理")
  }

  /**
   * Vendor notification pages come first, then the standard Android action. The app-details
   * page is appended by the caller so a failed jump always has a reachable destination.
   */
  private fun appNotificationCandidates(packageName: String): List<Intent> {
    val vendor = when (family) {
      ClearScreenOemProfile.XIAOMI -> listOf(
        component("com.android.settings", "com.android.settings.Settings\$AppNotificationSettingsActivity")
          .putExtra(Settings.EXTRA_APP_PACKAGE, packageName),
      )
      else -> emptyList()
    }
    return vendor + Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
      .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
  }

  private fun developerCandidates(): List<Intent> = listOf(
    component("com.android.settings", "com.android.settings.Settings\$DevelopmentSettingsDashboardActivity"),
    component("com.android.settings", "com.android.settings.development.DevelopmentSettingsActivity"),
  ) + vendorDeveloperCandidates() + Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)

  // Vendor developer pages are optional interoperability candidates, not stable APIs.
  private fun vendorDeveloperCandidates(): List<Intent> = when (family) {
    ClearScreenOemProfile.HUAWEI -> listOf(
      component("com.huawei.systemmanager", "com.huawei.systemmanager.development.DevelopmentSettingsActivity"),
    )
    ClearScreenOemProfile.HONOR -> listOf(
      component("com.hihonor.systemmanager", "com.hihonor.systemmanager.development.DevelopmentSettingsActivity"),
    )
    ClearScreenOemProfile.SAMSUNG -> listOf(
      component("com.samsung.android.settings", "com.samsung.android.settings.development.DevelopmentSettingsActivity"),
    )
    else -> emptyList()
  }

  private fun notifySettingsFallback(message: String) {
    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
  }

  private fun openAppDetailsOrThrow(notice: String) {
    val appDetails = Intent(
      Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
      Uri.parse("package:${context.packageName}"),
    )
    if (!tryOpen(listOf(appDetails))) throw ActivityNotFoundException("No available application details screen")
    notifySettingsFallback(notice)
  }

  private fun tryOpen(candidates: List<Intent>): Boolean {
    for (candidate in candidates) {
      if (!canOpen(candidate)) continue
      try {
        context.startActivity(Intent(candidate).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        return true
      } catch (_: ActivityNotFoundException) {
        // The page disappeared between resolve and launch; fall through to the next candidate.
      } catch (_: RuntimeException) {
        // Resolved but rejected by the OEM: keep trying the rest of the list.
      }
    }
    return false
  }

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
    if (tryOpen(candidates)) return
    throw ActivityNotFoundException("No available system settings screen")
  }

  companion object {
    private const val VIVO_STARTUP_ACTION = "com.iqoo.secure.BGSTARTUPMANAGER"
  }
}

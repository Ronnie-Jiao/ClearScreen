package com.clearscreen.prototype.backend

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

/**
 * Central destination table for the "manual settings" rows and for the pages the
 * accessibility click sessions need to land on. Every entry is tagged with the level it
 * represents, so the caller can degrade vendor page → standard intent → app info page and
 * tell the user honestly which level was reached.
 *
 * Component and action identifiers are interoperability candidates, not stable Android APIs.
 * Xiaomi entries were verified on HyperOS 2 (22101317C); vivo entries on OriginOS 16.
 */
internal object ClearScreenRecommendRoutes {
  const val LEVEL_VENDOR = "vendor"
  const val LEVEL_SYSTEM = "system"
  const val LEVEL_APP_DETAILS = "appDetails"

  data class Entry(val level: String, val intent: Intent, val hint: String? = null)

  fun candidates(route: String, context: Context, family: ClearScreenOemProfile): List<Entry> =
    when (route) {
      ClearScreenRecommendCatalog.ROUTE_AD_PRIVACY -> adPrivacy(context, family)
      ClearScreenRecommendCatalog.ROUTE_NEWS -> news(family)
      ClearScreenRecommendCatalog.ROUTE_STORE -> store(context, family)
      ClearScreenRecommendCatalog.ROUTE_XIAOMI_SEARCH -> xiaomiSearch()
      ClearScreenRecommendCatalog.ROUTE_XIAOMI_LAUNCHER -> xiaomiLauncher()
      ClearScreenRecommendCatalog.ROUTE_XIAOMI_BROWSER -> xiaomiBrowser()
      ClearScreenRecommendCatalog.ROUTE_HOME_SCREEN -> homeScreen()
      else -> listOf(Entry(LEVEL_SYSTEM, Intent(Settings.ACTION_SETTINGS), null))
    }

  fun appDetails(context: Context): Intent =
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))

  private fun adPrivacy(context: Context, family: ClearScreenOemProfile): List<Entry> = when (family) {
    ClearScreenOemProfile.XIAOMI -> listOf(
      // Verified on HyperOS 2 (22101317C): lands on the "广告服务" page with 个性化广告推荐.
      Entry(LEVEL_VENDOR, component(context, "com.android.settings", "com.android.settings.ad.AdServiceSettings"),
        "已打开系统的广告服务页"),
      Entry(LEVEL_SYSTEM, Intent(Settings.ACTION_PRIVACY_SETTINGS),
        "系统没有开放广告服务页，已打开隐私设置，请进「更多安全设置 → 广告服务」"),
      Entry(LEVEL_SYSTEM, Intent(Settings.ACTION_SETTINGS), null),
    )
    ClearScreenOemProfile.VIVO -> listOf(
      // vivo routes this action to VivoSubSettings, which shows the requested fragment when
      // the fragment name is registered in the system fragment registry.
      Entry(LEVEL_VENDOR, Intent(VIVO_SUB_SETTINGS_ACTION)
        .putExtra(VIVO_SUB_SETTINGS_EXTRA, "fragment:$VIVO_AD_PRIVACY_FRAGMENT"),
        "已打开系统的广告与隐私页"),
      Entry(LEVEL_VENDOR, Intent(VIVO_MORE_SECURITY_PRIVACY_ACTION), null),
      Entry(LEVEL_SYSTEM, Intent(Settings.ACTION_PRIVACY_SETTINGS), null),
      Entry(LEVEL_SYSTEM, Intent(Settings.ACTION_SETTINGS), null),
    )
    ClearScreenOemProfile.HUAWEI -> brandPrivacy(
      "已打开隐私设置，请点「广告与隐私」")
    ClearScreenOemProfile.HONOR -> brandPrivacy(
      "已打开隐私设置，请点「广告与隐私」")
    ClearScreenOemProfile.OPPO, ClearScreenOemProfile.ONEPLUS -> brandPrivacy(
      "已打开隐私设置，请找「设备标识与广告」")
    ClearScreenOemProfile.SAMSUNG -> brandPrivacy(
      "已打开隐私设置，请找广告或个人化服务开关")
    else -> brandPrivacy(null)
  }

  /** Brands without a verified vendor page stop at the standard privacy page with a next-tap hint. */
  private fun brandPrivacy(hint: String?): List<Entry> = listOf(
    Entry(LEVEL_SYSTEM, Intent(Settings.ACTION_PRIVACY_SETTINGS), hint),
    Entry(LEVEL_SYSTEM, Intent(Settings.ACTION_SETTINGS), null),
  )

  private fun news(family: ClearScreenOemProfile): List<Entry> = when (family) {
    ClearScreenOemProfile.XIAOMI -> listOf(
      // Verified on HyperOS 2 (22101317C): desktop settings with the 智能助理 (negative screen) switch.
      Entry(LEVEL_VENDOR, Intent("com.miui.home.Setting").setPackage("com.miui.home"),
        "已打开桌面设置，关掉「智能助理」可停用负一屏资讯推荐"),
      Entry(LEVEL_VENDOR, Intent().setComponent(ComponentName(
        "com.android.quicksearchbox", "com.android.quicksearchbox.preferences.SearchSettingsPreferenceActivity")),
        "已打开搜索设置"),
      Entry(LEVEL_SYSTEM, Intent(Settings.ACTION_SETTINGS),
        "系统没有开放推荐设置页，已打开设置首页，请搜索「推荐」"),
    )
    else -> listOf(
      Entry(LEVEL_SYSTEM, Intent(Settings.ACTION_SETTINGS),
        "系统没有开放推荐设置页，已打开设置首页，请在负一屏或搜索设置里关闭推荐"),
    )
  }

  private fun store(context: Context, family: ClearScreenOemProfile): List<Entry> {
    val vendor = when (family) {
      ClearScreenOemProfile.XIAOMI -> listOf(
        // Verified on HyperOS 2 (22101317C): lands on the store 隐私 page with 个性化服务.
        Entry(LEVEL_VENDOR, Intent().setComponent(ComponentName("com.xiaomi.market",
          "com.xiaomi.market.business_ui.settings.recommend.RecommendSettingsFragmentActivity")),
          "已打开应用商店的个人化推荐设置页"),
      )
      else -> emptyList()
    }
    val packageName = when (family) {
      ClearScreenOemProfile.XIAOMI -> "com.xiaomi.market"
      ClearScreenOemProfile.VIVO -> "com.bbk.appstore"
      ClearScreenOemProfile.OPPO, ClearScreenOemProfile.ONEPLUS -> "com.heytap.market"
      ClearScreenOemProfile.HONOR -> "com.hihonor.appmarket"
      ClearScreenOemProfile.HUAWEI -> "com.huawei.appmarket"
      ClearScreenOemProfile.SAMSUNG -> "com.sec.android.app.samsungapps"
      else -> null
    }
    val launch = packageName?.let { name ->
      context.packageManager.getLaunchIntentForPackage(name)?.let {
        Entry(LEVEL_SYSTEM, it, "已打开应用商店，请进「我的 → 设置」关闭推荐")
      }
    }
    return vendor + listOfNotNull(launch)
  }

  private fun xiaomiSearch(): List<Entry> = listOf(
    Entry(LEVEL_VENDOR, Intent().setComponent(ComponentName(
      "com.android.quicksearchbox", "com.android.quicksearchbox.preferences.SearchSettingsPreferenceActivity")),
      "已打开全局搜索设置，点「首页展示模块」，再关掉对应开关"),
    Entry(LEVEL_SYSTEM, Intent(Settings.ACTION_SETTINGS), null),
  )

  private fun xiaomiLauncher(): List<Entry> = listOf(
    Entry(LEVEL_VENDOR, Intent("com.miui.home.Setting").setPackage("com.miui.home"),
      "已打开桌面设置"),
    Entry(LEVEL_VENDOR, Intent("com.mi.android.globallauncher.Setting").setPackage("com.mi.android.globallauncher"),
      "已打开桌面设置"),
    Entry(LEVEL_SYSTEM, Intent(Settings.ACTION_SETTINGS), null),
  )

  private fun xiaomiBrowser(): List<Entry> = listOf(
    Entry(LEVEL_VENDOR, Intent().setComponent(ComponentName(
      "com.android.browser", "com.android.browser.BrowserSettingsActivity")),
      "已打开浏览器设置，点「主页设置」关闭「宫格位推送」"),
    Entry(LEVEL_SYSTEM, Intent(Settings.ACTION_SETTINGS), null),
  )

  private fun homeScreen() = listOf(
    Entry(LEVEL_SYSTEM, Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),
      "已回到桌面，右滑进入负一屏设置"),
  )

  private fun component(context: Context, packageName: String, className: String): Intent = Intent().apply {
    component = ComponentName(packageName, className)
    putExtra("packageName", context.packageName)
    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
  }

  private const val VIVO_SUB_SETTINGS_ACTION = "android.settings.SETTINGS.SUB_SETTINGS"
  private const val VIVO_SUB_SETTINGS_EXTRA = "extra"
  private const val VIVO_AD_PRIVACY_FRAGMENT =
    "com.vivo.settings.securityandprivacy.adprivacy.AdAndPrivacyFragmentForOsTwo"
  private const val VIVO_MORE_SECURITY_PRIVACY_ACTION = "com.android.settings.MORE_SECURITY_PRIVACY_SETTINGS"
}

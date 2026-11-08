package com.clearscreen.prototype.backend

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build

private typealias Target = ClearScreenUiCleanup.Target
private typealias Stage = ClearScreenUiCleanup.Stage

/**
 * One registration table for every "system recommendation" switch the app knows about.
 * Each vendor contributes its own entries; brands without an entry get the generic
 * manual destinations from [ClearScreenRecommendRoutes] instead of a silent empty page.
 */
internal object ClearScreenRecommendCatalog {
  const val ROUTE_AD_PRIVACY = "settings.adPrivacy"
  const val ROUTE_NEWS = "settings.news"
  const val ROUTE_STORE = "store.home"
  const val ROUTE_XIAOMI_SEARCH = "xiaomi.search"
  const val ROUTE_XIAOMI_LAUNCHER = "xiaomi.launcher"
  const val ROUTE_XIAOMI_BROWSER = "xiaomi.browser"
  const val ROUTE_HOME_SCREEN = "home.screen"

  fun targets(context: Context): List<Target> = vendorTargets() + googleAdPrivacy(context)

  private fun vendorTargets(): List<Target> = when (ClearScreenOemProfile.current()) {
    ClearScreenOemProfile.VIVO -> vivo
    ClearScreenOemProfile.XIAOMI -> xiaomi
    ClearScreenOemProfile.HUAWEI -> huawei
    ClearScreenOemProfile.HONOR -> honor
    ClearScreenOemProfile.OPPO -> oppo
    ClearScreenOemProfile.ONEPLUS -> oneplus
    ClearScreenOemProfile.SAMSUNG -> samsung
    ClearScreenOemProfile.MOTOROLA -> motorola
    else -> generic
  }

  private val vivo = listOf(
    Target("vivo.system.ad", "system", "个性化广告", "广告与隐私", "com.android.settings",
      settingKey = "vivo_recommended_status", navigate = ROUTE_AD_PRIVACY,
      effect = "关掉后，系统应用不再按你的使用习惯精准推广告"),
    Target("vivo.system.recommend", "system", "个性化推荐", "广告与隐私", "com.android.settings",
      settingKey = "vivo_personalized_recommend_status", navigate = ROUTE_AD_PRIVACY,
      effect = "关掉后，系统应用不再按你的兴趣推荐内容"),
    Target("vivo.system.smart", "system", "智能推荐服务", "广告与隐私", "com.android.settings",
      settingKey = "vivo_smart_recommend_service_status", navigate = ROUTE_AD_PRIVACY,
      effect = "关掉后，系统不再根据使用记录提供智能推荐"),
    Target("vivo.news.searchword", "news", "搜索框推荐词", "首页展示内容", "com.android.settings",
      setOf("com.vivo.globalsearch"), navigate = ROUTE_NEWS,
      effect = "关掉后，桌面搜索框不再出现推荐词"),
    Target("vivo.news.card", "news", "推荐卡片", "首页展示内容", "com.android.settings",
      setOf("com.vivo.globalsearch"), navigate = ROUTE_NEWS,
      effect = "关掉后，搜索首页不再展示推荐卡片"),
    Target("vivo.news.hot", "news", "热门搜索", "推荐热搜榜单", "com.android.settings",
      setOf("com.vivo.globalsearch"), navigate = ROUTE_NEWS,
      effect = "关掉后，搜索页不再展示热搜榜单"),
    Target("vivo.news.ad", "news", "广告个性化", "个性化内容", "com.android.settings",
      setOf("com.vivo.globalsearch"), navigate = ROUTE_NEWS,
      effect = "关掉后，搜索和浏览器不再按兴趣推广告"),
    Target("vivo.hiboard.services", "news", "服务推荐", "个性化推荐", "com.vivo.hiboard",
      entry = "home", navigate = ROUTE_HOME_SCREEN,
      effect = "关掉后，负一屏不再推荐服务卡片"),
    Target("vivo.hiboard.news", "news", "资讯推荐", "个性化推荐", "com.vivo.hiboard",
      entry = "home", navigate = ROUTE_HOME_SCREEN,
      effect = "关掉后，负一屏不再推送资讯"),
    Target("vivo.hiboard.content", "news", "内容精选", "内容精选", "com.vivo.hiboard",
      entry = "home", navigate = ROUTE_HOME_SCREEN,
      effect = "关掉后，负一屏不再展示精选内容"),
    Target("vivo.hiboard.hot", "news", "热点资讯", "热点资讯", "com.vivo.hiboard",
      entry = "home", navigate = ROUTE_HOME_SCREEN,
      effect = "关掉后，负一屏不再推送热点资讯"),
    Target("vivo.browser.feed", "news", "首页上滑展示资讯", "首页设置", "com.vivo.browser",
      entry = "browser",
      effect = "关掉后，浏览器首页上滑不再出现资讯流"),
    Target("vivo.browser.download", "news", "网页下载相关推荐", "网页浏览设置", "com.vivo.browser",
      entry = "browser",
      effect = "关掉后，下载完成页不再推荐应用"),
    Target("vivo.browser.wifi", "news", "WiFi连接跳转新闻资讯", "更多设置", "com.vivo.browser",
      entry = "browser",
      effect = "关掉后，连上 WiFi 不再自动跳转资讯页"),
    Target("vivo.store.personal", "store", "个性化推荐", "个性化推荐", "com.bbk.appstore",
      navigate = ROUTE_STORE, effect = "关掉后，应用商店首页不再按兴趣推荐应用"),
    Target("vivo.store.personalAd", "store", "个性化广告推荐", "个性化推荐", "com.bbk.appstore",
      navigate = ROUTE_STORE, effect = "关掉后，应用商店不再按兴趣推广告"),
    Target("vivo.store.sign", "store", "签到有礼", "服务管理", "com.bbk.appstore",
      navigate = ROUTE_STORE, effect = "关掉后，应用商店不再显示签到活动入口"),
    Target("vivo.store.banner", "store", "顶部轮播图", "服务管理", "com.bbk.appstore",
      navigate = ROUTE_STORE, effect = "关掉后，应用商店顶部不再展示推广轮播"),
    Target("vivo.store.column", "store", "顶部栏目", "服务管理", "com.bbk.appstore",
      navigate = ROUTE_STORE, effect = "关掉后，应用商店顶部不再展示推荐栏目"),
    Target("vivo.store.splash", "store", "开屏", "服务管理", "com.bbk.appstore",
      navigate = ROUTE_STORE, effect = "关掉后，打开应用商店不再出现开屏推广"),
    Target("vivo.store.shake", "store", "摇一摇", "服务管理", "com.bbk.appstore",
      navigate = ROUTE_STORE, effect = "关掉后，摇一摇不会再跳转推广页"),
    Target("vivo.store.float", "store", "悬浮窗", "服务管理", "com.bbk.appstore",
      navigate = ROUTE_STORE, effect = "关掉后，应用商店内不再弹出悬浮推广"),
    Target("vivo.store.promo", "store", "限时推广", "服务管理", "com.bbk.appstore",
      navigate = ROUTE_STORE, effect = "关掉后，应用商店不再弹限时推广"),
  )

  /**
   * HyperOS/MIUI exposes no readable ad-personalization keys, so every Xiaomi entry is either
   * an accessibility click on a verified page or a jump. Protected pages stay registered with a
   * reason so the list never silently shrinks.
   */
  private val xiaomi = listOf(
    Target("xiaomi.system.personalizedAds", "system", "个性化广告推荐", "广告服务", "com.android.settings",
      navigate = ROUTE_AD_PRIVACY,
      effect = "关掉后，系统广告不再按你的兴趣精准匹配"),
    Target("xiaomi.store.personal", "store", "个性化服务", "隐私", "com.xiaomi.market",
      navigate = ROUTE_STORE,
      effect = "关掉后，应用商店不再推荐个性化内容和广告"),
    Target("xiaomi.launcher.assistant", "news", "智能助理（负一屏总开关）", "桌面设置", "com.miui.home",
      entry = "home", navigate = ROUTE_XIAOMI_LAUNCHER,
      // The page label itself is just「智能助理」; the parenthetical is our own clarification.
      titleAliases = setOf("智能助理"),
      effect = "关掉后，桌面右滑不再进入负一屏资讯页"),
    Target("xiaomi.search.hint", "news", "搜索提示词", "首页展示模块", "com.android.quicksearchbox",
      stages = listOf(Stage("首页展示模块", "首页展示模块")),
      navigate = ROUTE_XIAOMI_SEARCH,
      effect = "关掉后，搜索框不再出现热门资讯提示词"),
    Target("xiaomi.search.app", "news", "应用推荐", "应用推荐", "com.android.quicksearchbox",
      titleAliases = setOf("展示应用推荐"),
      stages = listOf(Stage("首页展示模块", "首页展示模块"), Stage("应用推荐", "应用推荐")),
      navigate = ROUTE_XIAOMI_SEARCH,
      effect = "关掉后，搜索页不再推荐你常用的应用"),
    Target("xiaomi.search.history", "news", "搜索历史", "首页展示模块", "com.android.quicksearchbox",
      stages = listOf(Stage("首页展示模块", "首页展示模块")),
      navigate = ROUTE_XIAOMI_SEARCH,
      effect = "关掉后，搜索界面不再显示你的搜索历史"),
    Target("xiaomi.browser.grid", "news", "宫格位推送", "主页设置", "com.android.browser",
      entry = "browser",
      stages = listOf(Stage("主页设置", "主页设置")),
      navigate = ROUTE_XIAOMI_BROWSER,
      effect = "关掉后，浏览器主页宫格不再推送内容"),
    Target("xiaomi.assistant.news", "news", "资讯推荐", "个性化推荐", "com.miui.home",
      entry = "home", support = "unreachable",
      reason = "系统不允许其它应用打开负一屏设置，请桌面右滑进入负一屏，在右上角设置里关闭",
      effect = "关掉后，负一屏不再推送资讯"),
    Target("xiaomi.assistant.services", "news", "服务推荐", "个性化推荐", "com.miui.home",
      entry = "home", support = "unreachable",
      reason = "系统不允许其它应用打开负一屏设置，请桌面右滑进入负一屏，在右上角设置里关闭",
      effect = "关掉后，负一屏不再推荐服务卡片"),
    Target("xiaomi.assistant.content", "news", "内容精选", "内容精选", "com.miui.home",
      entry = "home", support = "unreachable",
      reason = "系统不允许其它应用打开负一屏设置，请桌面右滑进入负一屏，在右上角设置里关闭",
      effect = "关掉后，负一屏不再展示精选内容"),
    Target("xiaomi.assistant.hot", "news", "热点资讯", "热点资讯", "com.miui.home",
      entry = "home", support = "unreachable",
      reason = "系统不允许其它应用打开负一屏设置，请桌面右滑进入负一屏，在右上角设置里关闭",
      effect = "关掉后，负一屏不再推送热搜"),
    // Verified rows in the negative-screen settings page (PASettingActivity), same manual limit.
    Target("xiaomi.assistant.card", "news", "推荐卡片", "个性化推荐", "com.miui.home",
      entry = "home", support = "unreachable",
      reason = "系统不允许其它应用打开负一屏设置，请桌面右滑进入负一屏，在右上角设置里关闭",
      effect = "关掉后，负一屏不再展示推荐卡片"),
    Target("xiaomi.assistant.search", "news", "搜索框推荐词", "个性化推荐", "com.miui.home",
      entry = "home", support = "unreachable",
      reason = "系统不允许其它应用打开负一屏设置，请桌面右滑进入负一屏，在右上角设置里关闭",
      effect = "关掉后，负一屏搜索框不再出现推荐词"),
    Target("xiaomi.assistant.hotActivity", "news", "热门活动", "个性化推荐", "com.miui.home",
      entry = "home", support = "unreachable",
      reason = "系统不允许其它应用打开负一屏设置，请桌面右滑进入负一屏，在右上角设置里关闭",
      effect = "关掉后，负一屏不再推送热门活动"),
    Target("xiaomi.assistant.pin", "news", "智能置顶", "个性化推荐", "com.miui.home",
      entry = "home", support = "unreachable",
      reason = "系统不允许其它应用打开负一屏设置，请桌面右滑进入负一屏，在右上角设置里关闭",
      effect = "关掉后，负一屏不再把常看内容自动置顶"),
  )

  private val huawei = listOf(
    Target("huawei.system.restrictAdTracking", "system", "限制广告跟踪", "广告与隐私", "com.android.settings",
      checkedWhenFeatureDisabled = true, navigate = ROUTE_AD_PRIVACY,
      effect = "勾选后，应用不再用你的广告标识推送个性化广告"),
  )
  private val honor = listOf(
    Target("honor.system.allowAppTracking", "system", "允许应用跟踪", "广告与隐私", "com.android.settings",
      navigate = ROUTE_AD_PRIVACY,
      effect = "关掉后，应用不再跨应用跟踪你并推个性化广告"),
  )
  private val oppo = listOf(
    Target("oppo.system.restrictAdTracking", "system", "限制广告追踪", "设备标识与广告", "com.android.settings",
      checkedWhenFeatureDisabled = true, navigate = ROUTE_AD_PRIVACY,
      effect = "勾选后，应用不再用你的广告标识推送个性化广告"),
  )
  private val oneplus = listOf(
    Target("oneplus.system.limitAdTracking", "system", "限制广告跟踪", "设备 ID 和广告", "com.android.settings",
      checkedWhenFeatureDisabled = true,
      titleAliases = setOf("Limit ad tracking"), pageAliases = setOf("Device ID & ads", "Device ID and ads"),
      navigate = ROUTE_AD_PRIVACY,
      effect = "勾选后，应用不再用你的广告标识推送个性化广告"),
  )
  private val motorola = listOf(
    Target("motorola.system.optOutAdsPersonalization", "system", "停用广告个性化", "广告", "com.android.settings",
      checkedWhenFeatureDisabled = true,
      titleAliases = setOf("Opt out of Ads Personalization", "选择停用广告个性化"), pageAliases = setOf("Ads"),
      navigate = ROUTE_AD_PRIVACY,
      effect = "勾选后，应用不再按你的兴趣推送个性化广告"),
  )
  private val samsung = listOf(
    Target("samsung.system.customizationService", "system", "自定义服务", "自定义服务", "com.android.settings",
      titleAliases = setOf("Customisation service", "Customization service"),
      pageAliases = setOf("Customisation service", "Customization service"),
      navigate = ROUTE_AD_PRIVACY,
      effect = "关掉后，三星系统应用不再按你的兴趣推荐内容和广告"),
  )
  /** Brands without a verified page still get one honest row instead of an empty screen. */
  private val generic = listOf(
    Target("generic.system.personalizedAds", "system", "个性化广告", "广告与隐私", "com.android.settings",
      support = "manual", navigate = ROUTE_AD_PRIVACY,
      reason = "这台手机没有开放的直达页，请在系统设置里找到广告与隐私手动关闭",
      effect = "关掉后，应用不再按你的兴趣推送广告"),
  )

  /** Android's documented Privacy Sandbox controls, available on Android 13+ with Google Play services. */
  private fun googleAdPrivacy(context: Context): List<Target> {
    val hasGooglePlayServices = runCatching {
      val info = context.packageManager.getApplicationInfo("com.google.android.gms", 0)
      info.enabled && info.flags and (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
    }.getOrDefault(false)
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || !hasGooglePlayServices) return emptyList()
    return listOf(
      Target("android.adPrivacy.topics", "system", "广告主题", "广告主题", "com.android.settings",
        alternates = setOf("com.google.android.gms"),
        titleAliases = setOf("允许广告主题", "Allow ad topics"), pageAliases = setOf("Ad topics"),
        navigate = ROUTE_AD_PRIVACY,
        effect = "关掉后，系统不再按浏览兴趣生成广告主题"),
      Target("android.adPrivacy.appSuggestedAds", "system", "应用建议的广告", "应用建议的广告", "com.android.settings",
        alternates = setOf("com.google.android.gms"),
        titleAliases = setOf("允许应用建议的广告", "Allow app-suggested ads"),
        pageAliases = setOf("App-suggested ads"),
        navigate = ROUTE_AD_PRIVACY,
        effect = "关掉后，应用不能再建议跨应用广告"),
      Target("android.adPrivacy.measurement", "system", "广告衡量", "广告衡量", "com.android.settings",
        alternates = setOf("com.google.android.gms"),
        titleAliases = setOf("允许广告衡量", "Allow ad measurement"), pageAliases = setOf("Ad measurement"),
        navigate = ROUTE_AD_PRIVACY,
        effect = "关掉后，广告主无法衡量你是否看过广告"),
    )
  }
}

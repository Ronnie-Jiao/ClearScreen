package com.clearscreen.prototype.backend

/**
 * A deliberately small allowlist, adapted from Android Ad Cleaner (MIT).
 * Unknown packages and other phone brands are never passed to the shell.
 */
internal object ClearScreenCleanupRules {
  data class Target(val id: String, val title: String, val category: String, val packageName: String?)

  private fun pkg(name: String, title: String, category: String) =
    Target("$category:$name", title, category, name)

  fun targets(): List<Target> = when (ClearScreenOemProfile.current()) {
    ClearScreenOemProfile.VIVO -> listOf(
      Target("quick:vivo_service", "快应用服务", "quick", "com.vivo.hybrid"),
      pkg("com.vivo.magazine", "锁屏杂志", "ads"),
    )
    ClearScreenOemProfile.XIAOMI -> listOf(
      pkg("com.miui.systemAdSolution", "系统广告服务", "ads"),
      pkg("com.miui.hybrid", "快应用服务框架", "quick"),
      pkg("com.miui.hybrid.accessory", "快应用服务辅件", "quick"),
    )
    ClearScreenOemProfile.OPPO, ClearScreenOemProfile.ONEPLUS -> listOf(
      pkg("com.opos.ads", "系统广告服务", "ads"),
      pkg("com.heytap.pictorial", "锁屏画报", "ads"),
      pkg("com.nearme.instant.platform", "快应用平台", "quick"),
      pkg("com.oppo.instant.local.service", "快应用本地服务", "quick"),
      pkg("com.nearme.instant.card", "快应用卡片", "quick"),
    )
    ClearScreenOemProfile.HONOR -> listOf(
      pkg("com.hihonor.quickengine", "快应用引擎", "quick"),
      pkg("com.hihonor.quickgame", "快游戏", "quick"),
      pkg("com.honor.quickgamecenter", "快游戏中心", "quick"),
    )
    ClearScreenOemProfile.HUAWEI -> listOf(pkg("com.huawei.fastapp", "快应用中心", "quick"))
    else -> emptyList()
  }
}

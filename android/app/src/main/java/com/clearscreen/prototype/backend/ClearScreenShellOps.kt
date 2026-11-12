package com.clearscreen.prototype.backend

import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

/**
 * One shell identity (Shizuku user service or a local ADB connection). Every
 * caller passes an argument list; no caller ever receives a raw exec(String).
 * All command arguments are either fixed literals or values checked against the
 * patterns in this file / the cleanup allowlist.
 */
internal interface ClearScreenShellRunner {
  fun userId(): Int
  fun exec(vararg args: String): ClearScreenShellOps.Result
  fun execLines(vararg args: String, timeoutSeconds: Long, maxBytes: Long, onLine: (String) -> Unit)
}

/**
 * Command implementations shared by the Shizuku user service and the app's own
 * ADB client, so both backends behave identically.
 */
internal object ClearScreenShellOps {
  data class Result(val code: Int, val output: String)

  fun readSetting(runner: ClearScreenShellRunner, namespace: String, key: String): String {
    val user = runner.userId()
    val safeNamespace = requireNamespace(namespace)
    val safeKey = requireKey(key)
    val current = runner.exec("/system/bin/settings", "--user", user.toString(), "get", safeNamespace, safeKey)
    if (current.code != 0) return "unknown"
    val value = current.output.trim()
    return if (value.isEmpty() || value == "null") "unknown" else value
  }

  /**
   * `settings put` is silently dropped by some Android 16 vendor builds, so the
   * write goes through the settings provider instead. The stored value is
   * verified afterwards; a write that did not land is reported as a failure.
   */
  fun writeSetting(runner: ClearScreenShellRunner, namespace: String, key: String, value: String): String {
    val user = runner.userId()
    val safeNamespace = requireNamespace(namespace)
    val safeKey = requireKey(key)
    val safeValue = requireValue(value)
    val written = runner.exec(
      "/system/bin/content", "insert", "--uri", "content://settings/$safeNamespace",
      "--user", user.toString(), "--bind", "name:s:$safeKey", "--bind", "value:s:$safeValue",
    )
    check(written.code == 0) { "系统拒绝了这次改写" }
    val verified = readSetting(runner, safeNamespace, safeKey)
    check(verified == safeValue) { "改写后没有读到新值，系统没有接受这次操作" }
    return verified
  }

  fun state(runner: ClearScreenShellRunner, item: ClearScreenCleanupRules.Target, user: Int): String {
    val pkg = item.packageName ?: return "unknown"
    // No `-s` filter: vendor ad components are sometimes updated as user apps,
    // and a system-only listing would report them as absent.
    val packages = runner.exec("/system/bin/pm", "list", "packages", "--user", user.toString(), pkg)
    if (packages.code != 0) return "unknown"
    if (packages.output.lineSequence().none { it.trim() == "package:$pkg" }) return "absent"
    if (item.id == "quick:vivo_service") {
      val current = runner.exec("/system/bin/settings", "--user", user.toString(), "get", "secure", "switch_quick_app_service")
      return if (current.code != 0) "unknown" else when (current.output.trim()) {
        "1" -> "active"
        "0" -> "blocked"
        else -> "unknown"
      }
    }
    val dump = runner.exec("/system/bin/dumpsys", "package", pkg)
    if (dump.code != 0) return "unknown"
    // One package can be listed in several records (stub installs and multi-user
    // clones). Any record reporting suspended=true means the app is blocked.
    val pattern = Regex("^\\s*User\\s+$user:.*\\bsuspended=(true|false)\\b")
    val states = dump.output.lineSequence().mapNotNull { pattern.find(it)?.groupValues?.get(1) }.toList()
    if (states.isEmpty()) return "unknown"
    return if (states.contains("true")) "blocked" else "active"
  }

  fun target(id: String): ClearScreenCleanupRules.Target = ClearScreenCleanupRules.targets().singleOrNull { it.id == id }
    ?: throw IllegalArgumentException("该设备不支持此项目")

  fun inspectTargets(runner: ClearScreenShellRunner): String {
    val user = runner.userId()
    val items = JSONArray()
    for (item in ClearScreenCleanupRules.targets()) {
      val current = runCatching { state(runner, item, user) }.getOrDefault("unknown")
      items.put(JSONObject().put("id", item.id).put("title", item.title)
        .put("category", item.category).put("packageName", item.packageName).put("state", current))
    }
    return JSONObject().put("items", items).toString()
  }

  fun readTarget(runner: ClearScreenShellRunner, id: String): String = state(runner, target(id), runner.userId())

  fun changeTarget(runner: ClearScreenShellRunner, id: String, expected: String, desired: String): String {
    val user = runner.userId()
    val item = target(id)
    require(expected in setOf("active", "blocked") && desired in setOf("active", "blocked"))
    check(state(runner, item, user) == expected) { "系统状态已变化，请重新检查" }
    if (expected == desired) return expected
    if (item.id == "quick:vivo_service") {
      // `settings put` is dropped on some Android 16 vendor builds, so the
      // provider write path is used for this vendor switch as well.
      writeSetting(runner, "secure", "switch_quick_app_service", if (desired == "blocked") "0" else "1")
    } else {
      val result = runner.exec("/system/bin/pm", if (desired == "blocked") "suspend" else "unsuspend", "--user", user.toString(),
        requireNotNull(item.packageName))
      check(result.code == 0) { "系统拒绝操作，原状态备份已保留" }
    }
    check(state(runner, item, user) == desired) { "操作后未能确认状态，原状态备份已保留" }
    return desired
  }

  /** Discard all dump lines except channel metadata; never return notification bodies. */
  fun scanChannels(runner: ClearScreenShellRunner): String {
    val user = runner.userId()
    data class ChannelCandidate(
      val packageName: String,
      val id: String,
      val name: String,
      val importance: Int,
      val groupId: String?,
    )

    val channels = linkedMapOf<Pair<String, String>, ChannelCandidate>()
    val appBlocked = mutableMapOf<String, Boolean>()
    val groupBlocked = mutableMapOf<Pair<String, String>, Boolean>()
    var section = false
    var packageName: String? = null
    var appRecords = 0
    var channelRecords = 0
    val appPattern = Regex("^\\s*AppSettings:\\s+(\\S+)\\s+\\((\\d+)\\)")
    val appImportancePattern = Regex("\\bimportance=([^\\s,]+)", RegexOption.IGNORE_CASE)
    val groupIdPattern = Regex("mId='(.*?)'")
    val groupBlockedPattern = Regex("mBlocked=(true|false)", RegexOption.IGNORE_CASE)
    val channelIdPattern = Regex("mId='(.*?)'")
    val channelNamePattern = Regex("mName=(.*?),\\s*mDescription=")
    val channelImportancePattern = Regex("mImportance=(-?\\d+)")
    val channelDeletedPattern = Regex("mDeleted=(\\w+)")
    val channelGroupPattern = Regex("mGroup='(.*?)'")
    runner.execLines("/system/bin/dumpsys", "notification", "--noredact",
      timeoutSeconds = 35, maxBytes = 20_000_000) { line ->
      if (!section) {
        if (line.contains("PackagePreferences:") || line.contains("per-package config:")) section = true
      } else {
        val app = appPattern.find(line)
        if (app != null) {
          appRecords++
          val uid = app.groupValues[2].toIntOrNull()
          packageName = app.groupValues[1].takeIf { uid != null && uid / 100000 == user }
          val importance = appImportancePattern.find(line)?.groupValues?.get(1)
          packageName?.let { pkg ->
            if (importance != null) appBlocked[pkg] = importance.equals("NONE", ignoreCase = true)
          }
        } else if (line.trimStart().startsWith("AppSettings:")) {
          packageName = null
        } else {
          val pkg = packageName
          val group = if (line.contains("NotificationChannelGroup{")) {
            val id = groupIdPattern.find(line)?.groupValues?.get(1)
            val blocked = groupBlockedPattern.find(line)?.groupValues?.get(1)?.lowercase(Locale.ROOT)?.toBooleanStrictOrNull()
            if (pkg != null && id != null && blocked != null) groupBlocked[pkg to id] = blocked
            true
          } else false
          val channel = line.contains("NotificationChannel{")
          if (channel) channelRecords++
          if (channel && pkg != null && !group) {
            val id = channelIdPattern.find(line)?.groupValues?.get(1)
            val name = channelNamePattern.find(line)?.groupValues?.get(1)?.trim()
            val importance = channelImportancePattern.find(line)?.groupValues?.get(1)?.toIntOrNull() ?: -1
            val deleted = channelDeletedPattern.find(line)?.groupValues?.get(1)?.lowercase(Locale.ROOT) != "false"
            val groupId = channelGroupPattern.find(line)?.groupValues?.get(1)?.takeIf { it.isNotEmpty() }
            if (id == null || name == null) return@execLines
            val text = "$name $id".lowercase(Locale.ROOT)
            if (!deleted && id.length in 1..256 && name.length in 1..120 &&
              !id.endsWith("...") && !id.endsWith("…") && !id.any { it.isISOControl() } && !name.any { it.isISOControl() } &&
              MARKETING.containsMatchIn(text) && !PROTECTED.containsMatchIn(text)) {
              check(channels.size < 500 || (pkg to id) in channels) { "候选渠道过多，未生成完整清单" }
              channels[pkg to id] = ChannelCandidate(pkg, id, name, importance, groupId)
            }
          }
        }
      }
    }
    check(section && appRecords > 0 && channelRecords > 0) { "系统未提供可识别的通知渠道格式" }
    val result = JSONArray()
    for (channel in channels.values) {
      val state = when {
        channel.importance == 0 || appBlocked[channel.packageName] == true ||
          (channel.groupId != null && groupBlocked[channel.packageName to channel.groupId] == true) -> "off"
        channel.importance > 0 -> "on"
        else -> "unknown"
      }
      result.put(JSONObject().put("packageName", channel.packageName).put("id", channel.id)
        .put("name", channel.name).put("state", state))
    }
    return result.toString()
  }

  /**
   * One `appops` query returns every package whose app-level notification
   * switch is off. No per-app command is needed, so large lists load quickly.
   */
  fun blockedNotifications(runner: ClearScreenShellRunner): String {
    val blocked = linkedSetOf<String>()
    var answered = false
    for (mode in listOf("ignore", "deny")) {
      val query = runner.exec("/system/bin/cmd", "appops", "query-op", "POST_NOTIFICATION", mode)
      if (query.code != 0) continue
      answered = true
      for (line in query.output.lineSequence()) {
        val pkg = line.trim()
        if (pkg.matches(PACKAGE_PATTERN)) blocked.add(pkg)
      }
    }
    // Some ROMs store the user-visible switch as a UID mode only, which
    // query-op does not list. The appops dump exposes both levels.
    val dump = runner.exec("/system/bin/dumpsys", "appops", "--op", "POST_NOTIFICATION")
    if (dump.code == 0) {
      val blockedUids = linkedSetOf<Int>()
      val packageModes = linkedMapOf<String, String>()
      var uid: Int? = null
      var pkg: String? = null
      for (raw in dump.output.lineSequence()) {
        val line = raw.trim()
        when {
          line.startsWith("Uid ") -> {
            uid = uidNumber(line.removePrefix("Uid ").trim().removeSuffix(":").trim())
            pkg = null
          }
          line.startsWith("POST_NOTIFICATION: mode=") -> {
            if (isBlockedMode(line.substringAfter("mode=").trim())) uid?.let { blockedUids.add(it) }
          }
          line.startsWith("Package ") -> {
            pkg = line.removePrefix("Package ").trim().removeSuffix(":").trim()
          }
          line.startsWith("POST_NOTIFICATION (") -> {
            val name = pkg
            if (name != null && name.matches(PACKAGE_PATTERN)) {
              packageModes[name] = line.substringAfter("POST_NOTIFICATION (").substringBefore(')').trim()
            }
          }
        }
      }
      packageModes.forEach { (name, mode) -> if (isBlockedMode(mode)) blocked.add(name) }
      if (blockedUids.isNotEmpty()) {
        val list = runner.exec("/system/bin/pm", "list", "packages", "-U", "--user", runner.userId().toString())
        if (list.code == 0) {
          for (raw in list.output.lineSequence()) {
            val text = raw.trim()
            if (!text.startsWith("package:")) continue
            val name = text.removePrefix("package:").substringBefore(' ').trim()
            val id = text.substringAfter("uid:", "").trim().toIntOrNull() ?: continue
            if (id in blockedUids && name.matches(PACKAGE_PATTERN) && name !in packageModes) blocked.add(name)
          }
        }
      }
    }
    check(answered || blocked.isNotEmpty()) { "系统没有提供应用通知状态" }
    return JSONArray(blocked.toList()).toString()
  }

  /** Reads the real system notification switch of an installed package. */
  fun appNotificationState(runner: ClearScreenShellRunner, packageName: String): String {
    val pkg = requirePackage(packageName)
    val result = runner.exec("/system/bin/cmd", "appops", "get", pkg, "POST_NOTIFICATION")
    if (result.code != 0) return "unknown"
    val lines = result.output.lineSequence().map { it.trim() }.toList()
    val op = lines.firstOrNull { it.startsWith("POST_NOTIFICATION:") }
    if (op != null) return modeToState(op.substringAfter(':').trim().substringBefore(';').trim())
    val uidMode = lines.firstOrNull { it.startsWith("Uid mode:") && it.contains("POST_NOTIFICATION:") }
    if (uidMode != null) return modeToState(uidMode.substringAfter("POST_NOTIFICATION:").trim().substringBefore(';').trim())
    val fallback = lines.firstOrNull { it.startsWith("Default mode:") }
    if (fallback != null) return modeToState(fallback.substringAfter(':').trim())
    return "unknown"
  }

  /** Writes the switch and verifies it from the system again before reporting success. */
  fun setAppNotification(runner: ClearScreenShellRunner, packageName: String, enabled: Boolean): String {
    val user = runner.userId()
    val pkg = requirePackage(packageName)
    val installed = runner.exec("/system/bin/pm", "list", "packages", "--user", user.toString(), pkg)
    check(installed.code == 0 && installed.output.lineSequence().any { it.trim() == "package:$pkg" }) {
      "这台手机没有安装这个应用"
    }
    val desired = if (enabled) "enabled" else "disabled"
    when (appNotificationState(runner, pkg)) {
      desired -> return "already_$desired"
      "unknown" -> error("系统没有提供这个应用的通知状态，本次不会更改")
    }
    val mode = if (enabled) "allow" else "ignore"
    // Keep the package and uid levels in agreement: MIUI's switch writes both,
    // and query-op only reports the package level.
    val written = runner.exec("/system/bin/cmd", "appops", "set", pkg, "POST_NOTIFICATION", mode)
    check(written.code == 0) { "系统拒绝了这次修改" }
    val writtenUid = runner.exec("/system/bin/cmd", "appops", "set", "--uid", pkg, "POST_NOTIFICATION", mode)
    check(writtenUid.code == 0) { "系统拒绝了这次修改" }
    check(appNotificationState(runner, pkg) == desired) { "修改后没有读到新状态，系统没有接受这次操作" }
    return desired
  }

  private fun isBlockedMode(mode: String): Boolean =
    mode.lowercase(Locale.ROOT).let { it == "ignore" || it == "deny" }

  private fun uidNumber(token: String): Int? {
    token.toIntOrNull()?.let { return it }
    val match = Regex("u(\\d+)a(\\d+)").matchEntire(token) ?: return null
    val user = match.groupValues[1].toIntOrNull() ?: return null
    val app = match.groupValues[2].toIntOrNull() ?: return null
    return user * 100000 + 10000 + app
  }

  private fun modeToState(mode: String): String = when (mode.lowercase(Locale.ROOT)) {
    "ignore", "deny" -> "disabled"
    "allow", "default", "foreground" -> "enabled"
    else -> "unknown"
  }

  private fun requirePackage(packageName: String): String {
    require(packageName.matches(PACKAGE_PATTERN)) { "不支持的应用" }
    return packageName
  }

  private fun requireNamespace(namespace: String): String {
    require(namespace in setOf("secure", "system", "global")) { "不支持的设置项位置" }
    return namespace
  }

  private fun requireKey(key: String): String {
    require(key.matches(KEY_PATTERN)) { "不支持的设置项名称" }
    return key
  }

  private fun requireValue(value: String): String {
    require(value.matches(VALUE_PATTERN)) { "不支持的设置项取值" }
    return value
  }

  private val KEY_PATTERN = Regex("[A-Za-z0-9_.\\-]{1,80}")
  private val PACKAGE_PATTERN = Regex("[A-Za-z0-9_]+(\\.[A-Za-z0-9_]+)+")
  private val VALUE_PATTERN = Regex("[A-Za-z0-9_.\\-]{1,64}")
  private val MARKETING = Regex("营销|推广|促销|优惠|特惠|广告|热搜|资讯|新闻|福利|recommend|promo|market|deal|news|offer", RegexOption.IGNORE_CASE)
  private val PROTECTED = Regex("订单|物流|快递|交易|支付|付款|账号|安全|验证|通话|来电|消息|聊天|私信|评论|提醒|闹钟|日程|预警|系统|前台|后台|服务|播放|媒体|驾驶|权限|告警|赛事|护眼|找机位|通知总开关|order|delivery|payment|security|verification|message|emergency|alarm|warning", RegexOption.IGNORE_CASE)

  /** 通知渠道候选的同一套关键字判断，供公开通知接口的扫描复用。 */
  internal fun isMarketingChannelText(text: String): Boolean =
    MARKETING.containsMatchIn(text) && !PROTECTED.containsMatchIn(text)
}

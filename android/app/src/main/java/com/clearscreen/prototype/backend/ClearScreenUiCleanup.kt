package com.clearscreen.prototype.backend

import android.content.Context
import android.content.pm.ApplicationInfo
import android.graphics.Rect
import android.os.Build
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo

/** Explicit one-item sessions for exact vivo pages. No coordinates, OCR, or navigation clicks. */
internal object ClearScreenUiCleanup {
  /** One extra page to open before the switch appears: the row to click and the header it should land on. */
  data class Stage(val entry: String, val page: String,
    val entryAliases: Set<String> = emptySet(), val pageAliases: Set<String> = emptySet()) {
    fun matchesEntry(value: String): Boolean = value == entry || value in entryAliases
    fun matchesPage(value: String): Boolean = value == page || value in pageAliases
  }
  data class Target(val id: String, val category: String, val title: String, val page: String, val pkg: String,
    val alternates: Set<String> = emptySet(), val entry: String = "settings",
    val stages: List<Stage> = emptyList(),
    val settingKey: String? = null, val settingNamespace: String = "secure",
    val settingOn: String = "1", val settingOff: String = "0",
    val checkedWhenFeatureDisabled: Boolean = false,
    val titleAliases: Set<String> = emptySet(), val pageAliases: Set<String> = emptySet(),
    /** What closing this switch actually gives the user, in plain language. */
    val effect: String = "",
    /** assisted: accessibility tap · manual: jump only · unreachable: system blocks third-party apps. */
    val support: String = "assisted",
    /** Route key handled by ClearScreenRecommendRoutes; null keeps the legacy category path. */
    val navigate: String? = null,
    /** Why this item cannot be handled automatically; shown instead of a silent omission. */
    val reason: String? = null) {
    fun matchesTitle(value: String): Boolean = value == title || value in titleAliases
    fun matchesPage(value: String): Boolean = value == page || value in pageAliases
    fun matchesStageEntry(value: String): Boolean = stages.any { it.matchesEntry(value) }
    fun matchesStagePage(value: String): Boolean = stages.any { it.matchesPage(value) }
  }
  private data class Sample(val window: Int, val bounds: Rect, val checked: Boolean)
  private data class Session(
    val target: Target, val trustedPackages: Set<String>, val restore: Boolean, val ownPackage: String, val started: Long,
    var leftApp: Boolean = false, var seenTarget: Boolean = false, var sample: Sample? = null, var sampledAt: Long = 0,
    var clicked: Boolean = false, var stageIndex: Int = 0, var stageClickedAt: Long = 0,
    var stageAttempts: Int = 0, var stageAttemptStage: Int = -1,
  )
  private val lock = Any()
  private var session: Session? = null
  private var result: Pair<String, String>? = null
  private const val MAX_SESSION_MS = 180_000L

  fun targets(context: Context): List<Target> = ClearScreenRecommendCatalog.targets(context)
  private fun key(target: Target) = "ui:${target.id}"

  fun begin(context: Context, id: String, restore: Boolean, store: ClearScreenStore): Target = synchronized(lock) {
    val target = targets(context).singleOrNull { it.id == id } ?: error("这台手机没有这个开关")
    check(active() == null) { "上一个操作还没结束" }
    check(ClearScreenAccessibilityService.running) { "请先开启净屏无障碍服务" }
    val trusted = (target.alternates + target.pkg).filterTo(mutableSetOf()) { packageName ->
      runCatching {
        val info = context.packageManager.getApplicationInfo(packageName, 0)
        info.enabled && info.flags and (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
      }.getOrDefault(false)
    }
    check(trusted.isNotEmpty()) {
      "无法确认系统设置页面"
    }
    if (restore) check(store.cleanupBackup(key(target)) == "active") { "没有之前保存的记录，不能恢复" }
    session = Session(target, trusted, restore, context.packageName, SystemClock.elapsedRealtime())
    result = null
    target
  }

  fun hasBackup(target: Target, store: ClearScreenStore): Boolean = store.cleanupBackup(key(target)) != null

  /** Last confirmed state of the switch: "on" or "off"; null when net screen never confirmed it. */
  fun state(target: Target, store: ClearScreenStore): String? = store.cleanupState(key(target))

  fun onForeground(packageName: String, windowChanged: Boolean): Boolean = synchronized(lock) {
    val current = active() ?: return@synchronized false
    when (packageName) {
      in current.trustedPackages -> { current.leftApp = true; current.seenTarget = true; true }
      current.ownPackage -> {
        if (current.seenTarget && windowChanged) finish(current, if (current.clicked) "没能确认是否已关成功，请到设置里查看" else "没找到这个开关，没有改动")
        false
      }
      // Status bar, IME, and launcher emit background events during the jump;
      // only a real window change means the user left the target page.
      else -> { if (current.seenTarget && windowChanged) finish(current, "页面已经变了，操作已停止"); false }
    }
  }

  fun onHostResume() = synchronized(lock) {
    val current = active() ?: return@synchronized
    if (current.leftApp) finish(current, if (current.clicked) "没能确认是否已关成功，请到设置里查看" else "没找到这个开关，没有改动")
  }
  fun onHostPause() = synchronized(lock) { session?.leftApp = true }

  fun takeResult(): Pair<String, String>? = synchronized(lock) { result.also { result = null } }
  fun isActive(): Boolean = synchronized(lock) { active() != null }
  fun rejectSample() = synchronized(lock) { session?.sample = null }
  fun clear() = synchronized(lock) { session = null; result = null }
  fun cancel() = synchronized(lock) { session = null }
  fun stop() = synchronized(lock) {
    session?.let { finish(it, if (it.clicked) "净屏服务已停止，没能确认是否已关成功" else "净屏服务已停止，没有改动") }
  }

  private fun active(): Session? {
    val current = session ?: return null
    if (SystemClock.elapsedRealtime() - current.started > MAX_SESSION_MS) {
      finish(current, if (current.clicked) "操作超时，没能确认是否已关成功" else "操作超时，没有改动")
      return null
    }
    return current
  }

  private fun finish(current: Session, message: String) {
    result = current.target.title to message
    session = null
  }

  private data class Node(val ref: AccessibilityNodeInfo, val parent: Int, var end: Int, val text: String, val bounds: Rect)

  /** HyperOS toggles are CheckBox/SlidingButton nodes that report clickable=false but still checkable. */
  private fun isSwitchLike(ref: AccessibilityNodeInfo): Boolean {
    if (!ref.isCheckable || !ref.isEnabled) return false
    val cls = ref.className?.toString().orEmpty()
    val id = ref.viewIdResourceName.orEmpty()
    return cls == "android.widget.Switch" || cls.contains("SwitchCompat") || cls == "android.widget.CheckBox" ||
      cls.contains("SlidingButton") || cls.contains("Toggle") || id.contains("checkbox") || id.contains("switch")
  }

  /** Called on the service thread; all transient nodes are recycled before returning. */
  fun observe(root: AccessibilityNodeInfo, store: ClearScreenStore, unobstructed: (Rect) -> Boolean) = synchronized(lock) {
    val current = active() ?: return@synchronized
    if (root.packageName?.toString() !in current.trustedPackages) return@synchronized
    val nodes = mutableListOf<Node>()
    val owned = mutableListOf<AccessibilityNodeInfo>()
    var complete = true
    fun visit(node: AccessibilityNodeInfo, parent: Int, depth: Int) {
      if (!complete || !node.isVisibleToUser) return
      if (nodes.size >= 240 || depth > 18 || node.isPassword || (node.isEditable && node.isFocused)) { complete = false; return }
      val text = listOf(node.text, node.contentDescription).mapNotNull { it?.toString()?.trim() }
        .firstOrNull { current.target.matchesTitle(it) || current.target.matchesPage(it) ||
          current.target.matchesStageEntry(it) || current.target.matchesStagePage(it) }.orEmpty()
      val index = nodes.size
      nodes += Node(node, parent, index + 1, text, Rect().also(node::getBoundsInScreen))
      for (i in 0 until node.childCount) {
        val child = node.getChild(i)
        if (child == null) { complete = false; break }
        owned += child
        visit(child, index, depth + 1)
        if (!complete) break
      }
      nodes[index].end = nodes.size
    }
    try {
      visit(root, -1, 0)
      if (!complete || nodes.isEmpty()) { current.sample = null; return@synchronized }
      val screen = nodes[0].bounds
      if (screen.width() <= 0 || screen.height() <= 0) return@synchronized
      // Walk the page chain first: click the row that opens the next page, then look for the switch.
      val pendingStage = current.target.stages.getOrNull(current.stageIndex)
      if (pendingStage != null) {
        current.sample = null
        val stageNow = SystemClock.elapsedRealtime()
        if (stageNow - current.stageClickedAt < 1_600) return@synchronized
        val entryIndex = nodes.indexOfFirst { pendingStage.matchesEntry(it.text) }
        if (entryIndex < 0) {
          if (current.stageAttempts > 0 && stageNow - current.stageClickedAt > 12_000) {
            finish(current, "没能打开「${pendingStage.entry}」，没有改动")
          }
          return@synchronized
        }
        val entry = nodes[entryIndex]
        var pointer = entry.parent
        var row: Node? = null
        var depth = 0
        while (pointer >= 0 && depth < 5) {
          val candidate = nodes[pointer]
          if (candidate.ref.isClickable && candidate.ref.isEnabled && !candidate.bounds.isEmpty) { row = candidate; break }
          pointer = candidate.parent
          depth++
        }
        if (row == null || !unobstructed(row.bounds)) return@synchronized
        if (current.stageAttemptStage != current.stageIndex) {
          current.stageAttemptStage = current.stageIndex
          current.stageAttempts = 0
        }
        if (current.stageAttempts >= 2) { finish(current, "没能打开「${pendingStage.entry}」，没有改动"); return@synchronized }
        current.stageAttempts++
        current.stageIndex++
        current.stageClickedAt = stageNow
        val opened = row.ref.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        if (!opened) {
          current.stageIndex--
          finish(current, "系统没有接受这次操作，设置没有被改动")
        }
        return@synchronized
      }
      val stagedDone = current.target.stages.isNotEmpty() && current.stageIndex >= current.target.stages.size
      if (stagedDone && SystemClock.elapsedRealtime() - current.stageClickedAt > 20_000) {
        finish(current, if (current.clicked) "没能确认是否已关成功，请到设置里查看" else "没找到这个开关，没有改动")
        return@synchronized
      }
      // Some sub-pages repeat the entry row inside the page itself, so once the chain is done
      // the switch may be found without a top header; only allow that after the page has settled.
      val relaxed = stagedDone && SystemClock.elapsedRealtime() - current.stageClickedAt >= 1_600
      val headers = nodes.indices.filter { current.target.matchesPage(nodes[it].text) &&
        nodes[it].bounds.top < screen.top + screen.height() * .3 }
      if (headers.isEmpty() && !relaxed) { current.sample = null; return@synchronized }
      // switchIndex -> rowIndex (the clickable preference row that owns it).
      val matches = mutableMapOf<Int, Int>()
      for (labelIndex in nodes.indices.filter { current.target.matchesTitle(nodes[it].text) }) {
        val label = nodes[labelIndex]
        if (!relaxed && headers.none { it != labelIndex && nodes[it].bounds.bottom <= label.bounds.top }) continue
        var parent = label.parent
        repeat(4) {
          if (parent < 0) return@repeat
          val row = nodes[parent]
          if (row.end - parent <= 24 && row.bounds.height() in 1..(screen.height() / 4)) {
            val switches = (parent until row.end).filter { index -> isSwitchLike(nodes[index].ref) }
            val labels = (parent until row.end).count { current.target.matchesTitle(nodes[it].text) }
            if (switches.size == 1 && labels == 1) {
              val switchIndex = switches.single()
              val control = nodes[switchIndex]
              val sameControl = switchIndex == labelIndex
              // MIUI/HyperOS draws the title on the top line and the toggle vertically centered,
              // so they often share no rows; some pages let the switch cover the whole row with the
              // title inside it. Either way the single label + single switch in one row is enough.
              if (sameControl || control.bounds.left >= label.bounds.right - 4 ||
                control.bounds.top >= label.bounds.bottom || control.bounds.contains(label.bounds)) {
                matches[switchIndex] = parent
              }
            }
          }
          parent = row.parent
        }
      }
      if (matches.size != 1) { current.sample = null; return@synchronized }
      val (switchIndex, rowIndex) = matches.entries.single()
      val control = nodes[switchIndex]
      if (control.bounds.isEmpty || !Rect.intersects(screen, control.bounds)) { current.sample = null; return@synchronized }
      if (!unobstructed(control.bounds)) { current.sample = null; return@synchronized }
      val checked = if (Build.VERSION.SDK_INT >= 36) when (control.ref.getChecked()) {
        AccessibilityNodeInfo.CHECKED_STATE_TRUE -> true
        AccessibilityNodeInfo.CHECKED_STATE_FALSE -> false
        else -> null
      } else @Suppress("DEPRECATION") control.ref.isChecked
      if (checked == null) { current.sample = null; return@synchronized }
      val sample = Sample(root.windowId, Rect(control.bounds), checked)
      val now = SystemClock.elapsedRealtime()
      val age = now - current.sampledAt
      if (current.sample != sample || age < 100 || age > 1000) {
        if (current.sample != sample || age > 1000) { current.sample = sample; current.sampledAt = now }
        return@synchronized
      }
      val featureEnabled = checked != current.target.checkedWhenFeatureDisabled
      val desired = current.restore
      if (featureEnabled == desired) {
        if (current.restore) {
          store.removeCleanupBackup(key(current.target))
          store.saveCleanupState(key(current.target), "on")
        } else {
          store.saveCleanupState(key(current.target), "off")
        }
        finish(current, if (current.clicked) if (current.restore) "已恢复并确认" else "已关掉并确认" else "这个开关已经是现在这样了")
        return@synchronized
      }
      if (current.clicked) return@synchronized // Never issue a second click after an unconfirmed action.
      if (!current.restore) store.saveCleanupBackup(key(current.target), "active")
      current.clicked = true
      current.sample = null
      val clicked = control.ref.performAction(AccessibilityNodeInfo.ACTION_CLICK) ||
        // HyperOS CheckBox nodes report clickable=false; the preference row is the real click target.
        nodes.getOrNull(rowIndex)?.ref?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true
      if (!clicked) finish(current, "系统没有接受这次操作，设置没有被改动")
    } catch (_: Exception) {
      finish(current, "没能确认这个开关，操作已停止")
    } finally {
      @Suppress("DEPRECATION")
      owned.forEach { runCatching { it.recycle() } }
    }
  }
}

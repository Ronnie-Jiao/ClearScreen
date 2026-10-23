package com.clearscreen.prototype.backend

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.graphics.Rect
import android.os.Build
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import java.util.Locale

/**
 * Read-only fallback for OEMs without an accessible permission query. It is
 * armed ONLY by a user-initiated trip to our own settings, and never performs
 * an action, takes a screenshot, saves screen text, or keeps a node alive.
 */
internal object ClearScreenSettingsObserver {
  enum class Kind { STARTUP, BACKGROUND }
  private data class Session(
    val kind: Kind,
    val ownPackage: String,
    val ownLabel: String,
    val uniqueLabel: Boolean,
    val allowGlobalStartupRow: Boolean,
    val trustedPackages: Set<String>,
    val startedAt: Long,
    var candidate: Map<Kind, Boolean> = emptyMap(),
    var candidateAt: Long = 0,
    var windowId: Int = -1,
    var confirmed: Map<Kind, Boolean> = emptyMap(),
  )
  private val lock = Any()
  private var session: Session? = null
  private var returnedValues: Map<Kind, Boolean> = emptyMap()
  private var returnedAt = 0L
  private const val MAX_SESSION_MS = 180_000L
  private const val RESULT_LIFETIME_MS = 300_000L

  fun begin(context: Context, kind: Kind) {
    val pm = context.packageManager
    val label = normalize(pm.getApplicationLabel(context.applicationInfo).toString())
    val profile = ClearScreenOemProfile.current()
    val trusted = profile.readableSettingsPackages.filterTo(mutableSetOf()) { name ->
      runCatching {
        val info = pm.getApplicationInfo(name, 0)
        info.enabled && info.flags and (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
      }.getOrDefault(false)
    }
    // Identical launcher labels must not let another application's row be
    // mistaken for ours. This one-off check loads no icons or saved rules.
    val uniqueLabel = runCatching {
      val entries = pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
      entries.size <= 2048 && entries.any { it.activityInfo?.packageName == context.packageName } && entries.none {
        it.activityInfo?.packageName != context.packageName && normalize(it.loadLabel(pm).toString()) == label
      }
    }.getOrDefault(false)
    synchronized(lock) {
      returnedValues = emptyMap()
      session = Session(kind, context.packageName, label, uniqueLabel,
        profile !in setOf(ClearScreenOemProfile.HUAWEI, ClearScreenOemProfile.HONOR, ClearScreenOemProfile.SAMSUNG),
        trusted, SystemClock.elapsedRealtime())
    }
  }

  fun isWatching(packageName: String): Boolean = synchronized(lock) {
    val current = activeSession() ?: return@synchronized false
    packageName in current.trustedPackages
  }

  fun observe(root: AccessibilityNodeInfo) = synchronized(lock) {
    val current = activeSession() ?: return@synchronized
    if (root.packageName?.toString() !in current.trustedPackages) return@synchronized
    val values = readPage(root, current)
    val now = SystemClock.elapsedRealtime()
    if (current.windowId == root.windowId && current.candidate == values && now - current.candidateAt >= 100) {
      current.confirmed = values
    } else {
      current.windowId = root.windowId
      current.candidate = values
      current.candidateAt = now
      current.confirmed = emptyMap()
    }
  }

  fun onHostPause() = synchronized(lock) {
    // A new visit outside our guided settings invalidates previous UI proof.
    returnedValues = emptyMap()
  }

  fun onHostResume() = synchronized(lock) {
    val current = activeSession()
    if (current != null) {
      returnedValues = current.confirmed.toMap()
      returnedAt = SystemClock.elapsedRealtime()
      session = null
    }
  }

  fun onForegroundWindow(packageName: String) = synchronized(lock) {
    val current = activeSession() ?: return@synchronized
    if (packageName == current.ownPackage) {
      onHostResume()
    } else if (packageName !in current.trustedPackages) {
      clear()
    }
  }

  fun rejectSample() = synchronized(lock) {
    session?.let { it.candidate = emptyMap(); it.confirmed = emptyMap(); it.windowId = -1 }
  }

  fun value(kind: Kind): Boolean? = synchronized(lock) {
    if (SystemClock.elapsedRealtime() - returnedAt > RESULT_LIFETIME_MS) returnedValues = emptyMap()
    returnedValues[kind]
  }

  fun clear() = synchronized(lock) {
    session = null
    returnedValues = emptyMap()
  }

  private fun activeSession(): Session? {
    if (session?.let { SystemClock.elapsedRealtime() - it.startedAt > MAX_SESSION_MS } == true) {
      session = null
      returnedValues = emptyMap()
    }
    return session
  }

  private data class Node(
    val label: String,
    val parent: Int,
    var end: Int,
    val bounds: Rect,
    val checkable: Boolean,
    val checked: Boolean?,
    val enabled: Boolean,
    val collectionRows: Int,
  )

  private fun readPage(root: AccessibilityNodeInfo, current: Session): Map<Kind, Boolean> {
    val nodes = mutableListOf<Node>()
    val owned = mutableListOf<AccessibilityNodeInfo>()
    var complete = true
    fun visit(node: AccessibilityNodeInfo, parent: Int, depth: Int) {
      if (!complete || !node.isVisibleToUser) return
      if (nodes.size >= 240 || depth > 18 || node.isPassword || (node.isEditable && node.isFocused)) {
        complete = false
        return
      }
      val text = normalize(node.text?.toString().orEmpty())
      val description = normalize(node.contentDescription?.toString().orEmpty())
      fun relevant(value: String) = value == current.ownPackage ||
        (current.uniqueLabel && value == current.ownLabel) || value in KNOWN_LABELS
      // Discard unrelated text immediately; only exact labels needed for this
      // application's permission rows are present in the temporary projection.
      val label = when { relevant(text) -> text; relevant(description) -> description; else -> "" }
      val bounds = Rect().also(node::getBoundsInScreen)
      val index = nodes.size
      nodes += Node(label, parent, index + 1, bounds, node.isCheckable, checkedState(node),
        node.isEnabled, node.collectionInfo?.rowCount ?: 0)
      for (childIndex in 0 until node.childCount) {
        val child = node.getChild(childIndex)
        if (child == null) { complete = false; break }
        owned += child
        visit(child, index, depth + 1)
        if (!complete) break
      }
      nodes[index].end = nodes.size
    }
    try {
      visit(root, -1, 0)
      if (!complete || nodes.isEmpty()) return emptyMap()
      val packageMatches = nodes.indices.filter { nodes[it].label == current.ownPackage }
      val identities = packageMatches.ifEmpty {
        nodes.indices.filter { current.uniqueLabel && nodes[it].label == current.ownLabel }
      }
      if (identities.size != 1) return emptyMap()
      val ownIndex = identities.single()
      val own = nodes[ownIndex]
      val screen = nodes.first().bounds
      val headers = nodes.filter {
        it.label in GLOBAL_STARTUP_HEADERS && it.bounds.bottom <= own.bounds.top &&
          it.bounds.top < screen.top + screen.height() * .3
      }.map { it.label }
      if (headers.isNotEmpty()) {
        // Huawei/Honor automatic management, and Samsung sleeping-app lists,
        // are NOT direct autostart grants. Only a positive self-start list
        // heading and a single, unambiguous checkable own row are accepted.
        if (!current.allowGlobalStartupRow || headers.none { it in DIRECT_STARTUP_HEADERS } ||
          current.kind != Kind.STARTUP) return emptyMap()
        val state = rowState(nodes, ownIndex)
        return state?.let { mapOf(Kind.STARTUP to it) } ?: emptyMap()
      }
      // Detail/dialog pages must identify our app above their permission rows,
      // and must not be a long multi-application collection with an unknown title.
      if (nodes.any { it.collectionRows > 12 } ||
        (own.bounds.top > screen.top + screen.height() * .35 && nodes.none { it.label in DETAIL_HEADERS })) {
        return emptyMap()
      }
      val result = mutableMapOf<Kind, Boolean>()
      for ((kind, labels) in mapOf(Kind.STARTUP to STARTUP_LABELS, Kind.BACKGROUND to BACKGROUND_LABELS)) {
        val matches = nodes.indices.filter { nodes[it].label in labels && nodes[it].bounds.top >= own.bounds.bottom }
        // Multiple matching controls or contradictory values are ambiguous.
        if (matches.size == 1) rowState(nodes, matches.single())?.let { result[kind] = it }
      }
      if (Kind.BACKGROUND !in result && nodes.any { it.label in BATTERY_HEADERS }) {
        val options = nodes.indices.filter {
          nodes[it].label in BATTERY_CHOICES && nodes[it].bounds.top >= own.bounds.bottom
        }.map { nodes[it].label to rowState(nodes, it) }
        val checked = options.filter { it.second == true }
        if (options.none { it.second == null } && checked.size == 1) {
          result[Kind.BACKGROUND] = checked.single().first in ALLOWED_BATTERY_CHOICES
        }
      }
      return result
    } finally {
      @Suppress("DEPRECATION")
      owned.forEach { runCatching { it.recycle() } }
    }
  }

  private fun checkedState(node: AccessibilityNodeInfo): Boolean? {
    if (Build.VERSION.SDK_INT >= 36) {
      return when (node.getChecked()) {
        AccessibilityNodeInfo.CHECKED_STATE_TRUE -> true
        AccessibilityNodeInfo.CHECKED_STATE_FALSE -> false
        else -> null // Mixed/partially checked is not an explicit grant or denial.
      }
    }
    @Suppress("DEPRECATION")
    return node.isChecked
  }

  private fun rowState(nodes: List<Node>, labelIndex: Int): Boolean? {
    var index = labelIndex
    repeat(5) {
      if (index < 0) return null
      val row = nodes[index]
      if (row.end - index > 28) return null
      val local = nodes.subList(index, row.end)
      if (local.any { it.label in MANAGEMENT_LABELS }) return null
      val controls = local.filter { it.checkable }
      if (controls.size == 1 && controls.single().enabled) {
        val target = controls.single().bounds
        val label = nodes[labelIndex].bounds
        // Do not climb to a container and borrow the next application's switch.
        if (kotlin.math.abs(target.centerY() - label.centerY()) > (target.height() + label.height()) / 2) return null
        // The caller has already verified the list heading or the explicit
        // permission label; both must bind to this same small local row.
        return controls.single().checked
      }
      if (controls.size > 1) return null
      index = row.parent
    }
    return null
  }

  private fun normalize(value: String): String = value.trim().lowercase(Locale.ROOT).replace(Regex("\\s+"), " ")
  private val DIRECT_STARTUP_HEADERS = setOf("自启动", "自启动管理", "开机自启动", "后台自启动", "自动启动", "自动启动管理", "autostart", "auto-start", "auto start", "auto-launch", "auto launch", "background autostart")
  private val GLOBAL_STARTUP_HEADERS = DIRECT_STARTUP_HEADERS + setOf("应用启动管理", "应用启动", "启动管理", "app launch", "launch management")
  private val STARTUP_LABELS = setOf("允许自启动", "允许自动启动", "允许开机启动", "允许后台自启动", "自启动", "开机自启动", "开机启动", "后台自启动", "自动启动", "allow auto-launch", "allow auto launch", "allow autostart", "auto-launch", "autostart")
  private val BACKGROUND_LABELS = setOf("允许后台运行", "允许后台活动", "允许后台高耗电", "后台运行", "后台活动", "后台高耗电", "allow background activity", "allow background usage", "allow background running", "run in background")
  private val MANAGEMENT_LABELS = setOf("自动管理", "智能管理", "手动管理", "manage automatically", "manage manually", "auto manage", "automatic management")
  private val DETAIL_HEADERS = setOf("应用信息", "应用详情", "手动管理", "app info", "app details", "manual management")
  private val BATTERY_HEADERS = setOf("电池", "电池用量", "应用电池用量", "耗电管理", "后台耗电管理", "battery", "battery usage", "app battery usage", "battery optimization")
  private val ALLOWED_BATTERY_CHOICES = setOf("不受限制", "无限制", "允许后台运行", "unrestricted", "no restrictions")
  private val BATTERY_CHOICES = ALLOWED_BATTERY_CHOICES + setOf("受限", "受限制", "限制", "禁止后台运行", "restricted")
  private val KNOWN_LABELS = GLOBAL_STARTUP_HEADERS + STARTUP_LABELS + BACKGROUND_LABELS + MANAGEMENT_LABELS + DETAIL_HEADERS + BATTERY_HEADERS + BATTERY_CHOICES
}

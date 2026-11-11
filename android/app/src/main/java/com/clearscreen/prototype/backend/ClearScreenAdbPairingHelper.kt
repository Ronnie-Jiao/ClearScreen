package com.clearscreen.prototype.backend

import android.content.Context
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import java.util.ArrayDeque
import java.util.concurrent.Executors

/**
 * Reads the wireless-debugging pairing dialog while it is still on screen and
 * completes pairing from the background.
 *
 * The system keeps the pairing server alive only while the dialog is visible:
 * switching to another app closes the dialog and the port stops accepting
 * connections. The accessibility service runs in this process, so it can read
 * the code and port without taking the user away from the dialog; the pairing
 * and connection then run in the background.
 *
 * Security boundaries: the pairing code and pairing port are used once and never
 * logged or stored. A successfully verified connection endpoint may be kept locally
 * for reconnection; only the fixed pairing/connect calls above are triggered.
 */
internal object ClearScreenAdbPairingHelper {
  private const val TAG = "ClearScreenAdb"
  private const val SETTINGS_PACKAGE = "com.android.settings"
  private const val ID_PAIRING_CODE = "com.android.settings:id/pairing_code"
  private const val ID_IP_ADDR = "com.android.settings:id/ip_addr"
  // Every stage ends by itself: a missing deadline used to leave the switch
  // spinning forever when the pairing dialog was never read.
  private const val WAITING_CODE_MS = 120_000L
  private const val PAIRING_MS = 60_000L
  private const val WAITING_PORT_MS = 60_000L
  private const val CONNECTING_MS = 90_000L
  private val ENDPOINT = Regex("((?:\\d{1,3}\\.){3}\\d{1,3}):(\\d{2,5})")
  private val lock = Any()
  private val worker = Executors.newSingleThreadExecutor { task ->
    Thread(task, "clearscreen-adb-pairing").apply { isDaemon = true }
  }

  private var appContext: Context? = null
  private var active = false
  private var stageName = ""
  private var statusText = ""
  private var pairingHost = ""
  private var pairingPort = 0
  private var lastAttemptKey = ""
  private var generation = 0L
  private var stageSince = 0L

  fun isActive(): Boolean = synchronized(lock) { active }
  private fun isActiveAttempt(expected: Long): Boolean = synchronized(lock) { active && generation == expected }

  fun readStage(): String = synchronized(lock) { stageName }

  fun readMessage(): String = synchronized(lock) { statusText }

  fun start(context: Context) = synchronized(lock) {
    generation++
    appContext = context.applicationContext
    active = true
    stageName = "waiting-code"
    statusText = "请打开无线调试并点“使用配对码配对设备”，正在读取配对码"
    stageSince = SystemClock.elapsedRealtime()
    Log.i(TAG, "pair stage=waiting-code")
    pairingHost = ""
    pairingPort = 0
    lastAttemptKey = ""
  }

  fun stop() = synchronized(lock) {
    generation++
    active = false
    stageName = ""
    statusText = ""
    stageSince = 0L
    appContext = null
    pairingHost = ""
    pairingPort = 0
    lastAttemptKey = ""
  }

  /**
   * Ends the attempt when a stage cannot reach its next step. Called by the
   * accessibility service on a fixed interval so a silent dialog or a missing
   * endpoint still finishes with a readable reason instead of spinning forever.
   */
  fun tick() {
    val reason = synchronized(lock) {
      if (!active) return
      val elapsed = SystemClock.elapsedRealtime() - stageSince
      when {
        stageName == "waiting-code" && elapsed > WAITING_CODE_MS ->
          "没有读到配对码，请重新点按开关，在系统设置里打开「无线调试」并点「使用配对码配对设备」"
        stageName == "pairing" && elapsed > PAIRING_MS ->
          "配对没有完成，请重新点按开关再试一次"
        stageName == "waiting-port" && elapsed > WAITING_PORT_MS ->
          "配对成功，但没有读到无线调试的连接端口，请重新点按开关再试一次"
        stageName == "connecting" && elapsed > CONNECTING_MS ->
          "连接没有完成，请确认无线调试仍然开启后重新点按开关"
        else -> return
      }
    }
    fail(reason)
  }

  private fun fail(reason: String) {
    synchronized(lock) {
      if (stageName != "failed") Log.i(TAG, "pair stage=failed")
      stageName = "failed"
      statusText = reason
      stageSince = SystemClock.elapsedRealtime()
      active = false
      pairingHost = ""
      pairingPort = 0
      lastAttemptKey = ""
    }
  }

  private fun setStageLocked(stage: String, text: String) {
    stageName = stage
    statusText = text
    stageSince = SystemClock.elapsedRealtime()
    Log.i(TAG, "pair stage=$stage")
  }

  /** Called by the accessibility service for windows of the settings app. */
  fun onSettingsWindow(root: AccessibilityNodeInfo) {
    val context = synchronized(lock) {
      if (!active) return
      appContext
    } ?: return
    val nodes = collect(root)
    val stage = synchronized(lock) { stageName }
    if (stage == "waiting-code") {
      val code = nodes.firstOrNull { it.first == ID_PAIRING_CODE }?.second
        ?.filter { it.isDigit() }.orEmpty()
      if (code.length !in 4..8) return
      val endpoint = nodes.firstOrNull { it.first == ID_IP_ADDR }?.second.orEmpty()
      val match = ENDPOINT.find(endpoint) ?: return
      val host = match.groupValues[1]
      val port = match.groupValues[2].toInt()
      if (port !in 1..65535) return
      val key = "$host:$port:$code"
      val attempt = synchronized(lock) {
        if (!active || stageName != "waiting-code") return
        if (lastAttemptKey == key) return
        lastAttemptKey = key
        pairingHost = host
        pairingPort = port
        setStageLocked("pairing", "已读取配对码，正在配对")
        generation
      }
      worker.execute {
        if (!isActiveAttempt(attempt)) return@execute
        val result = runCatching { ClearScreenAdbOperations.pair(context, host, port, code) }.getOrNull()
        val ok = result?.get("ok") == true
        synchronized(lock) {
          if (!active || generation != attempt || stageName != "pairing") return@synchronized
          if (ok) {
            setStageLocked("waiting-port", "配对成功，正在读取连接端口")
          } else {
            setStageLocked("failed", (result?.get("message") as? String) ?: "配对失败")
            active = false
            pairingHost = ""
            pairingPort = 0
            lastAttemptKey = ""
          }
        }
      }
      return
    }
    if (stage != "waiting-port" && stage != "connecting") return
    // The pairing dialog is gone; the page now shows the connection endpoint.
    val candidate = nodes.asSequence().map { it.second }
      .mapNotNull { ENDPOINT.find(it) }
      .firstOrNull { it.groupValues[2].toInt() != pairingPort } ?: return
    val host = candidate.groupValues[1]
    val port = candidate.groupValues[2].toInt()
    if (host != pairingHost || port !in 1..65535) return
    val attempt = synchronized(lock) {
      if (stageName != "waiting-port") return
      setStageLocked("connecting", "正在连接")
      generation
    }
    worker.execute {
      if (!isActiveAttempt(attempt)) return@execute
      val result = runCatching { ClearScreenAdbOperations.connectFromPairing(context, host, port) }.getOrNull()
      val ok = result?.get("ok") == true
      synchronized(lock) {
        if (!active || generation != attempt || stageName != "connecting") return@synchronized
        if (ok) setStageLocked("connected", "已连接")
        else setStageLocked("failed", (result?.get("message") as? String) ?: "连接失败")
        active = false
        pairingHost = ""
        pairingPort = 0
        lastAttemptKey = ""
      }
    }
  }

  private fun collect(root: AccessibilityNodeInfo): List<Pair<String, String>> {
    val out = ArrayList<Pair<String, String>>(8)
    val queue = ArrayDeque<AccessibilityNodeInfo>()
    queue.add(root)
    var visited = 0
    while (queue.isNotEmpty() && visited < 400) {
      val node = queue.poll() ?: break
      visited++
      val text = node.text?.toString()?.trim().orEmpty()
      if (text.isNotEmpty()) out.add(node.viewIdResourceName.orEmpty() to text)
      for (index in 0 until node.childCount) node.getChild(index)?.let { queue.add(it) }
    }
    return out
  }
}

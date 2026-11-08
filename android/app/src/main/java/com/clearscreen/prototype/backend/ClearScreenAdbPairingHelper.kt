package com.clearscreen.prototype.backend

import android.content.Context
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
  private const val SETTINGS_PACKAGE = "com.android.settings"
  private const val ID_PAIRING_CODE = "com.android.settings:id/pairing_code"
  private const val ID_IP_ADDR = "com.android.settings:id/ip_addr"
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
    pairingHost = ""
    pairingPort = 0
    lastAttemptKey = ""
  }

  fun stop() = synchronized(lock) {
    generation++
    active = false
    stageName = ""
    statusText = ""
    appContext = null
    pairingHost = ""
    pairingPort = 0
    lastAttemptKey = ""
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
        stageName = "pairing"
        statusText = "已读取配对码，正在配对"
        generation
      }
      worker.execute {
        if (!isActiveAttempt(attempt)) return@execute
        val result = runCatching { ClearScreenAdbOperations.pair(context, host, port, code) }.getOrNull()
        val ok = result?.get("ok") == true
        synchronized(lock) {
          if (!active || generation != attempt || stageName != "pairing") return@synchronized
          stageName = if (ok) "waiting-port" else "failed"
          statusText = if (ok) "配对成功，正在读取连接端口"
          else (result?.get("message") as? String) ?: "配对失败"
          if (!ok) {
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
      stageName = "connecting"
      statusText = "正在连接"
      generation
    }
    worker.execute {
      if (!isActiveAttempt(attempt)) return@execute
      val result = runCatching { ClearScreenAdbOperations.connectFromPairing(context, host, port) }.getOrNull()
      val ok = result?.get("ok") == true
      synchronized(lock) {
        if (!active || generation != attempt || stageName != "connecting") return@synchronized
        stageName = if (ok) "connected" else "failed"
        statusText = if (ok) "已连接" else (result?.get("message") as? String) ?: "连接失败"
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

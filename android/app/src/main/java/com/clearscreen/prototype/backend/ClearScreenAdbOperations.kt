package com.clearscreen.prototype.backend

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.Keep
import io.github.muntashirakon.adb.AdbAuthenticationFailedException
import io.github.muntashirakon.adb.AdbPairingRequiredException
import java.io.File
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.concurrent.TimeUnit

/**
 * State holder for the app's own ADB channel (Android 11+ wireless debugging).
 *
 * Security boundaries:
 * - the key pair is generated and kept locally (see ClearScreenAdbKeys);
 * - pairing codes and keys are never logged;
 * - commands are argument lists validated by [SAFE_ARG], and only the fixed
 *   command set built by ClearScreenShellOps is ever executed.
 */
@Keep
internal object ClearScreenAdbOperations {
  enum class State { UNSUPPORTED, IDLE, PAIRING, CONNECTING, CONNECTED, ERROR }

  private const val MIN_API = 30
  private const val TAG = "ClearScreenAdb"
  private const val CONNECT_TIMEOUT_SECONDS = 12L
  private val SAFE_ARG = Regex("[A-Za-z0-9_./:=+-]{1,200}")
  private val lock = Any()
  private val connectionLock = Any()

  private var client: ClearScreenAdbClient? = null
  private var state: State = State.IDLE
  private var host = ""
  private var port = 0
  private var message = ""

  fun isSupported(): Boolean = Build.VERSION.SDK_INT >= MIN_API

  fun status(context: Context): Map<String, Any?> = synchronized(lock) {
    mapOf(
      "supported" to isSupported(),
      "state" to when {
        !isSupported() -> State.UNSUPPORTED.name.lowercase()
        else -> state.name.lowercase()
      },
      "connected" to (state == State.CONNECTED && runCatching { client?.isConnected() == true }.getOrDefault(false)),
      "paired" to isPaired(context),
      "reconnectRequested" to reconnectRequested(context),
      "host" to host,
      "port" to port,
      "message" to message,
      "localAddress" to localAddress(),
      "autoPairStage" to ClearScreenAdbPairingHelper.readStage(),
      "autoPairMessage" to ClearScreenAdbPairingHelper.readMessage(),
    )
  }

  fun pair(context: Context, host: String, port: Int, code: String): Map<String, Any?> =
    synchronized(connectionLock) { pairLocked(context, host, port, code) }

  private fun pairLocked(context: Context, host: String, port: Int, code: String): Map<String, Any?> {
    synchronized(lock) { state = State.PAIRING; message = "正在配对" }
    return try {
      require(isSupported()) { "Android 11 以下不支持无线调试配对" }
      if (!connectionAllowed(context)) return cancelled()
      val manager = ensureClient(context)
      val paired = synchronized(ClearScreenPrivacyConsentStore.operationLock) {
        if (!connectionAllowed(context)) false
        else manager.pair(host, port, code).also { if (it) markPaired(context) }
      }
      if (!connectionAllowed(context)) return cancelled()
      if (paired) {
        synchronized(lock) { this.host = host; state = State.IDLE; message = "配对成功，请读取无线调试页的连接端口" }
        result(true, "配对成功")
      } else {
        fail("配对失败，请核对配对端口和配对码")
      }
    } catch (error: Exception) {
      fail(readable(error, "配对失败"))
    }
  }

  fun connect(context: Context, host: String, port: Int): Map<String, Any?> {
    requestReconnect(context)
    return synchronized(connectionLock) {
      if (!connectionAllowed(context)) cancelled(synchronized(lock) { client })
      else if (isVerifiedConnected()) result(true, "已连接")
      else connectDirect(context, host, port, reportFailure = true)
    }
  }

  /** Pairing has already recorded the user's request; stopping it must not be undone. */
  fun connectFromPairing(context: Context, host: String, port: Int): Map<String, Any?> =
    synchronized(connectionLock) { connectDirect(context, host, port, reportFailure = true) }

  private fun connectDirect(context: Context, host: String, port: Int, reportFailure: Boolean): Map<String, Any?> {
    synchronized(lock) { state = State.CONNECTING; message = "正在连接" }
    var manager: ClearScreenAdbClient? = null
    return try {
      require(isSupported()) { "Android 11 以下不支持无线调试连接" }
      if (!connectionAllowed(context)) return cancelled()
      val active = ensureClient(context)
      manager = active
      // libadb refuses to connect while the old socket still reports as
      // connected, which turns every manual retry into a silent failure.
      dropConnection(active)
      if (!active.connect(host, port)) {
        dropConnection(active)
        return connectionFailure(context, "连接失败，请确认无线调试已开启且连接端口正确", reportFailure)
      }
      // A TCP handshake alone does not prove the shell identity: when the phone
      // has not yet allowed this key, the daemon accepts the socket but not the
      // command. `id` must report the adb shell uid.
      val authorized = synchronized(ClearScreenPrivacyConsentStore.operationLock) {
        connectionAllowed(context) && executeInternal(active, "id", 12L, 200_000, null).output.contains("uid=2000")
      }
      if (!connectionAllowed(context)) return cancelled(active)
      if (!authorized) {
        dropConnection(active)
        return connectionFailure(context, "已连接但未获得调试授权，请在手机上点“允许”后重试连接", reportFailure)
      }
      synchronized(ClearScreenPrivacyConsentStore.operationLock) {
        if (!connectionAllowed(context)) return cancelled(active)
        markPaired(context)
        rememberEndpoint(context, host, port)
        synchronized(lock) { this.host = host; this.port = port; state = State.CONNECTED; message = "已连接" }
      }
      result(true, "已连接")
    } catch (error: AdbAuthenticationFailedException) {
      manager?.let(::dropConnection)
      connectionFailure(context, "调试授权被拒绝，请在手机上允许后重试连接", reportFailure)
    } catch (error: AdbPairingRequiredException) {
      manager?.let(::dropConnection)
      connectionFailure(context, "需要先完成无线调试配对", reportFailure)
    } catch (error: Exception) {
      manager?.let(::dropConnection)
      connectionFailure(context, readable(error, "连接失败，请确认无线调试已开启且连接端口正确"), reportFailure)
    }
  }

  fun autoConnect(context: Context): Map<String, Any?> {
    requestReconnect(context)
    return synchronized(connectionLock) {
      if (!connectionAllowed(context)) cancelled(synchronized(lock) { client })
      else if (isVerifiedConnected()) result(true, "已连接")
      else connectKnownOrDiscover(context)
    }
  }

  /** A previous paired connection is restored only while the user still wants it. */
  fun shouldRestore(context: Context): Boolean {
    if (!isSupported() || !ClearScreenPrivacyConsentStore(context).hasCurrentConsent() ||
      !isPaired(context) || !reconnectRequested(context) || ClearScreenAdbPairingHelper.isActive()) return false
    return synchronized(lock) {
      state != State.CONNECTING && state != State.PAIRING &&
        !(state == State.CONNECTED && runCatching { client?.isConnected() == true }.getOrDefault(false))
    }
  }

  fun restoreIfRequested(context: Context) {
    synchronized(connectionLock) {
      if (shouldRestore(context)) connectKnownOrDiscover(context)
    }
  }

  private fun connectKnownOrDiscover(context: Context): Map<String, Any?> {
    if (!connectionAllowed(context)) return cancelled()
    val prefs = adbPrefs(context)
    val savedHost = prefs.getString(PREF_LAST_HOST, "").orEmpty()
    val savedPort = prefs.getInt(PREF_LAST_PORT, 0)
    // Only retry an endpoint that is still this device's current local address.
    if (savedHost.isNotEmpty() && savedHost == localAddress() && savedPort in 1..65535) {
      val previous = connectDirect(context, savedHost, savedPort, reportFailure = false)
      if (previous["ok"] == true || !connectionAllowed(context)) return previous
      forgetEndpoint(context)
    }
    return discoverConnection(context)
  }

  private fun discoverConnection(context: Context): Map<String, Any?> {
    synchronized(lock) { state = State.CONNECTING; message = "正在查找无线调试设备" }
    var manager: ClearScreenAdbClient? = null
    return try {
      require(isSupported()) { "Android 11 以下不支持无线调试连接" }
      if (!connectionAllowed(context)) return cancelled()
      val active = ensureClient(context)
      manager = active
      dropConnection(active)
      if (!active.autoConnect(context, 5000L)) {
        dropConnection(active)
        return connectionFailure(context, "同一网络下没有发现已开启的无线调试，请手动填写地址", true)
      }
      val authorized = synchronized(ClearScreenPrivacyConsentStore.operationLock) {
        connectionAllowed(context) && executeInternal(active, "id", 12L, 200_000, null).output.contains("uid=2000")
      }
      if (!connectionAllowed(context)) return cancelled(active)
      if (!authorized) {
        dropConnection(active)
        return connectionFailure(context, "已连接但未获得调试授权，请在手机上点“允许”后重试连接", true)
      }
      synchronized(ClearScreenPrivacyConsentStore.operationLock) {
        if (!connectionAllowed(context)) return cancelled(active)
        markPaired(context)
        synchronized(lock) { state = State.CONNECTED; message = "已连接" }
      }
      result(true, "已连接")
    } catch (error: Exception) {
      manager?.let(::dropConnection)
      connectionFailure(context, readable(error, "没有找到可连接的无线调试，请开启无线调试后重试"), true)
    }
  }

  fun disconnect(context: Context, forgetEndpoint: Boolean = false) {
    var persistenceError: Exception? = null
    try { setReconnectRequested(context, false, forgetEndpoint) } catch (error: Exception) { persistenceError = error }
    synchronized(connectionLock) {
      val active = synchronized(lock) { state = State.IDLE; message = "已断开"; client }
      active?.let(::dropConnection)
    }
    persistenceError?.let { throw it }
  }

  fun isVerifiedConnected(): Boolean = synchronized(lock) {
    state == State.CONNECTED && runCatching { client?.isConnected() == true }.getOrDefault(false)
  }

  /** Used by ClearScreenShellOps through the shared runner interface. */
  fun runCommand(vararg args: String): ClearScreenShellOps.Result =
    executeInternal(connection(), buildCommand(args), 20L, 2_000_000, null)

  fun runCommandLines(vararg args: String, timeoutSeconds: Long, maxBytes: Long, onLine: (String) -> Unit) {
    executeInternal(connection(), buildCommand(args), timeoutSeconds, maxBytes, onLine)
  }

  /**
   * Drop the socket but keep the manager and the locally stored key pair.
   * Closing the manager would also destroy the cached private key, and the next
   * connection would then be unable to authenticate.
   */
  private fun dropConnection(manager: ClearScreenAdbClient) {
    runCatching { manager.disconnect() }
  }

  private fun connection(): ClearScreenAdbClient = synchronized(lock) {
    check(state == State.CONNECTED) { "还没和这台手机连上" }
    val manager = client ?: error("还没和这台手机连上")
    if (!runCatching { manager.isConnected() }.getOrDefault(false)) {
      state = State.ERROR
      message = "连接已断开，请重新连一次"
      error("连接已断开，请重新连一次")
    }
    manager
  }

  /** Shell identity of the ADB daemon: commands run as the shell user in user 0. */
  val shellRunner: ClearScreenShellRunner = object : ClearScreenShellRunner {
    override fun userId(): Int = 0
    override fun exec(vararg args: String): ClearScreenShellOps.Result = runCommand(*args)
    override fun execLines(vararg args: String, timeoutSeconds: Long, maxBytes: Long, onLine: (String) -> Unit) =
      runCommandLines(*args, timeoutSeconds = timeoutSeconds, maxBytes = maxBytes, onLine = onLine)
  }

  private fun ensureClient(context: Context): ClearScreenAdbClient = synchronized(lock) {
    client ?: ClearScreenAdbClient(context).apply {
      setApi(Build.VERSION.SDK_INT)
      setTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
      setThrowOnUnauthorised(true)
    }.also { client = it }
  }

  private fun buildCommand(args: Array<out String>): String {
    require(args.isNotEmpty()) { "不支持的命令" }
    args.forEach { require(it.matches(SAFE_ARG)) { "不支持的命令参数" } }
    return args.joinToString(" ")
  }

  private fun executeInternal(
    manager: ClearScreenAdbClient,
    command: String,
    timeoutSeconds: Long,
    maxBytes: Long,
    onLine: ((String) -> Unit)?,
  ): ClearScreenShellOps.Result {
    // The trailing sentinel doubles as end-of-output: adbd does not always close
    // the stream after the command exits, so waiting for EOF deadlocks instead.
    val stream = manager.openStream("shell:$command; echo; echo __CS_EXIT:\$?")
    var failure: Exception? = null
    var exitCode = -1
    val output = StringBuilder()
    val worker = Thread({
      try {
        stream.openInputStream().bufferedReader().useLines { lines ->
          for (line in lines) {
            if (line.startsWith("__CS_EXIT:")) {
              exitCode = line.removePrefix("__CS_EXIT:").trim().toIntOrNull() ?: -1
              break
            }
            check(output.length + line.length <= maxBytes) { "系统返回内容过长" }
            output.append(line).append('\n')
            onLine?.invoke(line)
          }
        }
      } catch (error: Exception) {
        failure = error
      } finally {
        runCatching { stream.close() }
      }
    }, "clearscreen-adb-stream")
    worker.start()
    worker.join(timeoutSeconds * 1000)
    if (worker.isAlive) {
      Log.w(TAG, "exec timeout: bytes=${output.length} closed=${stream.isClosed}")
      runCatching { stream.close() }
      worker.interrupt()
      // A half-dead socket keeps the UI in "connected" while every command hangs,
      // so surface the failure and drop the socket instead of reusing it.
      synchronized(lock) { state = State.ERROR; message = "系统命令响应超时" }
      dropConnection(manager)
      throw IllegalStateException("系统命令响应超时")
    }
    // Without the sentinel the output may be truncated, so keep the failure.
    val error = failure
    if (error != null && exitCode < 0) throw error
    return ClearScreenShellOps.Result(exitCode, output.toString())
  }

  private fun localAddress(): String = runCatching {
    // Wireless debugging only listens on the real network interface, never on
    // loopback. Prefer WLAN because tethering/VPN interfaces are also non-loopback
    // and would give an address that the daemon does not listen on.
    val addresses = NetworkInterface.getNetworkInterfaces().toList()
      .filter { it.isUp && !it.isLoopback }
      .flatMap { iface ->
        iface.inetAddresses.toList()
          .filterIsInstance<Inet4Address>()
          .filter { it.isSiteLocalAddress }
          .map { iface.name.lowercase() to it }
      }
    (addresses.firstOrNull { it.first.contains("wlan") || it.first.contains("wifi") }
      ?: addresses.firstOrNull { it.first.startsWith("ap") || it.first.contains("eth") }
      ?: addresses.firstOrNull())?.second?.hostAddress ?: ""
  }.getOrDefault("")

  private const val PREF_RECONNECT = "reconnect_requested"
  private const val PREF_LAST_HOST = "last_connection_host"
  private const val PREF_LAST_PORT = "last_connection_port"

  private fun adbPrefs(context: Context) =
    context.getSharedPreferences("clearscreen_adb", Context.MODE_PRIVATE)

  fun requestReconnect(context: Context) {
    check(ClearScreenPrivacyConsentStore(context).hasCurrentConsent()) { "请先同意隐私政策" }
    setReconnectRequested(context, true)
  }

  fun cancelReconnectRequest(context: Context) = setReconnectRequested(context, false)

  private fun setReconnectRequested(context: Context, requested: Boolean, forgetEndpoint: Boolean = false) {
    val editor = adbPrefs(context).edit().putBoolean(PREF_RECONNECT, requested)
    if (forgetEndpoint) editor.remove(PREF_LAST_HOST).remove(PREF_LAST_PORT)
    check(editor.commit()) { "无法保存连接状态" }
  }

  /** Older installs had only the paired flag; keep their existing connection available. */
  private fun reconnectRequested(context: Context): Boolean = runCatching {
    val prefs = adbPrefs(context)
    if (prefs.contains(PREF_RECONNECT)) prefs.getBoolean(PREF_RECONNECT, false)
    else prefs.getBoolean("paired", false)
  }.getOrDefault(false)

  private fun connectionAllowed(context: Context): Boolean =
    ClearScreenPrivacyConsentStore(context).hasCurrentConsent() && reconnectRequested(context)

  private fun rememberEndpoint(context: Context, host: String, port: Int) {
    runCatching { adbPrefs(context).edit().putString(PREF_LAST_HOST, host).putInt(PREF_LAST_PORT, port).commit() }
  }

  private fun forgetEndpoint(context: Context) {
    runCatching { adbPrefs(context).edit().remove(PREF_LAST_HOST).remove(PREF_LAST_PORT).apply() }
  }

  private fun cancelled(manager: ClearScreenAdbClient? = null): Map<String, Any?> {
    manager?.let(::dropConnection)
    synchronized(lock) { state = State.IDLE; message = "已断开" }
    return result(false, "连接已取消")
  }

  private fun connectionFailure(context: Context, text: String, reportFailure: Boolean): Map<String, Any?> {
    if (!connectionAllowed(context)) return cancelled()
    return if (reportFailure) fail(text) else result(false, text)
  }

  /** Pairing or a verified shell connection proves authorization; a key file alone does not. */
  private fun isPaired(context: Context): Boolean = runCatching {
    adbPrefs(context).getBoolean("paired", false)
  }.getOrDefault(false)

  private fun markPaired(context: Context) {
    runCatching { adbPrefs(context).edit().putBoolean("paired", true).commit() }
  }

  private fun result(ok: Boolean, text: String): Map<String, Any?> = mapOf("ok" to ok, "message" to text)

  private fun fail(text: String): Map<String, Any?> {
    synchronized(lock) { state = State.ERROR; message = text }
    return result(false, text)
  }

  private val CJK = Regex("[\\u4e00-\\u9fff]")

  private fun readable(error: Exception, fallback: String): String {
    val text = error.message?.trim().orEmpty()
    // libadb throws English socket/IO text; only Chinese messages are readable.
    return when {
      text.isEmpty() -> fallback
      text.length > 200 -> fallback
      !CJK.containsMatchIn(text) -> fallback
      else -> text
    }
  }
}

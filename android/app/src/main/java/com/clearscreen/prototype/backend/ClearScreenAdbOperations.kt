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
      "host" to host,
      "port" to port,
      "message" to message,
      "localAddress" to localAddress(),
      "autoPairStage" to ClearScreenAdbPairingHelper.readStage(),
      "autoPairMessage" to ClearScreenAdbPairingHelper.readMessage(),
    )
  }

  fun pair(context: Context, host: String, port: Int, code: String): Map<String, Any?> {
    synchronized(lock) { state = State.PAIRING; message = "正在配对" }
    return try {
      require(isSupported()) { "Android 11 以下不支持无线调试配对" }
      val manager = ensureClient(context)
      val paired = manager.pair(host, port, code)
      if (paired) {
        markPaired(context)
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
    synchronized(lock) { state = State.CONNECTING; message = "正在连接" }
    return try {
      require(isSupported()) { "Android 11 以下不支持无线调试连接" }
      val manager = ensureClient(context)
      // libadb refuses to connect while the old socket still reports as
      // connected, which turns every manual retry into a silent failure.
      dropConnection(manager)
      val connected = manager.connect(host, port)
      if (!connected) return fail("连接失败，请确认无线调试已开启且连接端口正确")
      // A TCP handshake alone does not prove the shell identity: when the phone
      // has not yet allowed this key, the daemon accepts the socket but not the
      // command. `id` must report the adb shell uid.
      val identity = executeInternal(manager, "id", 12L, 200_000, null)
      if (!identity.output.contains("uid=2000")) {
        dropConnection(manager)
        return fail("已连接但未获得调试授权，请在手机上点“允许”后重试连接")
      }
      synchronized(lock) { this.host = host; this.port = port; state = State.CONNECTED; message = "已连接" }
      result(true, "已连接")
    } catch (error: AdbAuthenticationFailedException) {
      fail("调试授权被拒绝，请在手机上允许后重试连接")
    } catch (error: AdbPairingRequiredException) {
      fail("需要先完成无线调试配对")
    } catch (error: Exception) {
      fail(readable(error, "连接失败"))
    }
  }

  fun autoConnect(context: Context): Map<String, Any?> {
    synchronized(lock) { state = State.CONNECTING; message = "正在查找无线调试设备" }
    return try {
      require(isSupported()) { "Android 11 以下不支持无线调试连接" }
      val manager = ensureClient(context)
      dropConnection(manager)
      val connected = manager.autoConnect(context, 5000L)
      if (!connected) return fail("同一网络下没有发现已开启的无线调试，请手动填写地址")
      val identity = executeInternal(manager, "id", 12L, 200_000, null)
      if (!identity.output.contains("uid=2000")) {
        dropConnection(manager)
        return fail("已连接但未获得调试授权，请在手机上点“允许”后重试连接")
      }
      synchronized(lock) { state = State.CONNECTED; message = "已连接" }
      result(true, "已连接")
    } catch (error: Exception) {
      fail(readable(error, "连接失败"))
    }
  }

  fun disconnect() {
    synchronized(lock) {
      client?.let { dropConnection(it) }
      state = State.IDLE
      message = "已断开"
    }
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

  private fun adbPrefs(context: Context) =
    context.getSharedPreferences("clearscreen_adb", Context.MODE_PRIVATE)

  /** Pairing success is a durable fact; a key file alone only proves a key was generated. */
  private fun isPaired(context: Context): Boolean = runCatching {
    adbPrefs(context).getBoolean("paired", false)
  }.getOrDefault(false)

  private fun markPaired(context: Context) {
    runCatching { adbPrefs(context).edit().putBoolean("paired", true).apply() }
  }

  private fun result(ok: Boolean, text: String): Map<String, Any?> = mapOf("ok" to ok, "message" to text)

  private fun fail(text: String): Map<String, Any?> {
    synchronized(lock) { state = State.ERROR; message = text }
    return result(false, text)
  }

  private fun readable(error: Exception, fallback: String): String {
    val text = error.message?.trim().orEmpty()
    return when {
      text.isEmpty() -> fallback
      text.length > 200 -> fallback
      else -> text
    }
  }
}

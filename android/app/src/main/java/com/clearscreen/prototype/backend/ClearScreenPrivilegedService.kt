package com.clearscreen.prototype.backend

import android.os.Binder
import android.os.Process
import androidx.annotation.Keep
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** Shizuku UserService: no arbitrary shell command or package supplied by JS is accepted. */
@Keep
class ClearScreenPrivilegedService @Keep constructor() : IClearScreenPrivilegedService.Stub() {

  override fun destroy() { System.exit(0) }

  private val runner = object : ClearScreenShellRunner {
    override fun userId(): Int {
      check(Process.myUid() == 2000 || Process.myUid() == 0) { "Shizuku 服务权限不可用" }
      return Binder.getCallingUid() / 100000
    }

    override fun exec(vararg args: String): ClearScreenShellOps.Result = command(*args)

    override fun execLines(
      vararg args: String,
      timeoutSeconds: Long,
      maxBytes: Long,
      onLine: (String) -> Unit,
    ) {
      val process = ProcessBuilder(*args).redirectErrorStream(true).start()
      val executor = Executors.newSingleThreadExecutor()
      try {
        executor.submit {
          var seenBytes = 0L
          process.inputStream.bufferedReader().useLines { lines ->
            lines.forEach { line ->
              seenBytes += line.length
              check(seenBytes <= maxBytes) { "系统返回内容过长" }
              onLine(line)
            }
          }
        }.get(timeoutSeconds, TimeUnit.SECONDS)
        check(process.waitFor() == 0) { "系统命令执行失败" }
      } finally {
        process.destroy()
        executor.shutdownNow()
      }
    }
  }

  private fun command(vararg args: String): ClearScreenShellOps.Result {
    val process = ProcessBuilder(*args).redirectErrorStream(true).start()
    val executor = Executors.newSingleThreadExecutor()
    try {
      val result = executor.submit<String> {
        val text = StringBuilder()
        process.inputStream.bufferedReader().use { reader ->
          val buffer = CharArray(4096)
          while (true) {
            val count = reader.read(buffer)
            if (count < 0) break
            check(text.length + count <= 2_000_000) { "系统返回内容过长" }
            text.append(buffer, 0, count)
          }
        }
        text.toString()
      }.get(20, TimeUnit.SECONDS)
      return ClearScreenShellOps.Result(process.waitFor(), result)
    } finally {
      process.destroy()
      executor.shutdownNow()
    }
  }

  override fun readSetting(namespace: String, key: String): String =
    ClearScreenShellOps.readSetting(runner, namespace, key)

  override fun writeSetting(namespace: String, key: String, value: String): String =
    ClearScreenShellOps.writeSetting(runner, namespace, key, value)

  override fun inspectTargets(): String = ClearScreenShellOps.inspectTargets(runner)

  override fun readTarget(id: String): String = ClearScreenShellOps.readTarget(runner, id)

  override fun changeTarget(id: String, expected: String, desired: String): String =
    ClearScreenShellOps.changeTarget(runner, id, expected, desired)

  override fun scanChannels(): String = ClearScreenShellOps.scanChannels(runner)

  override fun blockedNotifications(): String = ClearScreenShellOps.blockedNotifications(runner)

  override fun setAppNotification(packageName: String, enabled: Boolean): String =
    ClearScreenShellOps.setAppNotification(runner, packageName, enabled)
}

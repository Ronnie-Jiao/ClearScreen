package com.clearscreen.prototype.backend

import android.content.BroadcastReceiver
import android.content.ComponentCallbacks2
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import android.os.SystemClock
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

/** Cache immutable app metadata only. User rules are always read separately. */
internal class ClearScreenAppInventory(private val context: Context) : Closeable, ComponentCallbacks2 {
  data class Entry(val packageName: String, val name: String, val iconUri: String, val systemApp: Boolean)
  private data class Cached(val generation: Long, val configuration: String, val expiresAt: Long, val entries: List<Entry>)
  private val generation = AtomicLong()
  private val pool = Executors.newFixedThreadPool(minOf(6, maxOf(2, Runtime.getRuntime().availableProcessors()))) as ThreadPoolExecutor
  @Volatile private var cached: Cached? = null
  @Volatile private var closed = false
  private val receiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) { clear() }
  }
  private val callbacksRegistered = runCatching { context.registerComponentCallbacks(this); true }.getOrDefault(false)
  private val registered = runCatching {
    val filter = IntentFilter().apply {
      addAction(Intent.ACTION_PACKAGE_ADDED); addAction(Intent.ACTION_PACKAGE_REMOVED)
      addAction(Intent.ACTION_PACKAGE_REPLACED); addAction(Intent.ACTION_PACKAGE_CHANGED)
      addDataScheme("package")
    }
    if (Build.VERSION.SDK_INT >= 33) context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
    else @Suppress("DEPRECATION") context.registerReceiver(receiver, filter)
    true
  }.getOrDefault(false)

  private fun clear() { generation.incrementAndGet(); cached = null }
  override fun onConfigurationChanged(configuration: Configuration) { clear() }
  override fun onLowMemory() { clear() }
  override fun onTrimMemory(level: Int) {
    if (level == ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW || level == ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL ||
      level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND) clear()
  }

  @Synchronized fun entries(): List<Entry> {
    check(!closed) { "应用列表读取已结束" }
    val now = SystemClock.elapsedRealtime()
    val configuration = context.resources.configuration.let {
      "${it.locales.toLanguageTags()}:${context.resources.displayMetrics.densityDpi}:${it.uiMode}"
    }
    val version = generation.get()
    cached?.takeIf { it.generation == version && it.configuration == configuration && now < it.expiresAt }
      ?.let { return it.entries }
    val manager = context.packageManager
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    val packages = manager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
      .mapNotNull { it.activityInfo?.packageName }.distinct().filter { it != context.packageName }
      .mapNotNull { runCatching { manager.getApplicationInfo(it, PackageManager.MATCH_ALL) }.getOrNull() }
    val tasks = packages.map { info -> pool.submit<Entry> {
      Entry(info.packageName, runCatching { manager.getApplicationLabel(info).toString() }.getOrDefault(info.packageName),
        runCatching { icon(manager.getApplicationIcon(info)) }.getOrDefault(""),
        info.flags and ApplicationInfo.FLAG_SYSTEM != 0)
    } }
    val deadline = SystemClock.elapsedRealtime() + 12_000L
    var complete = packages.isNotEmpty()
    val result = try {
      tasks.mapIndexed { index, task ->
        try {
          if (task.isDone) task.get() else task.get((deadline - SystemClock.elapsedRealtime()).coerceAtLeast(1L), TimeUnit.MILLISECONDS)
        } catch (error: InterruptedException) {
          Thread.currentThread().interrupt(); throw error
        } catch (_: Exception) {
          complete = false
          val info = packages[index]
          Entry(info.packageName, info.packageName, "", info.flags and ApplicationInfo.FLAG_SYSTEM != 0)
        }
      }.sortedBy { it.name.lowercase(Locale.getDefault()) }
    } finally {
      tasks.forEach { if (!it.isDone) it.cancel(true) }
      pool.purge()
    }
    if (!closed && generation.get() == version) {
      cached = Cached(version, configuration, SystemClock.elapsedRealtime() + if (complete) 60_000L else 3_000L, result)
    }
    return result
  }

  private fun icon(drawable: android.graphics.drawable.Drawable): String {
    val bitmap = Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888)
    return try {
      drawable.setBounds(0, 0, 96, 96); drawable.draw(Canvas(bitmap))
      ByteArrayOutputStream().use {
        check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it))
        "data:image/png;base64," + Base64.encodeToString(it.toByteArray(), Base64.NO_WRAP)
      }
    } finally { bitmap.recycle() }
  }

  override fun close() {
    closed = true; generation.incrementAndGet(); cached = null
    if (registered) runCatching { context.unregisterReceiver(receiver) }
    if (callbacksRegistered) runCatching { context.unregisterComponentCallbacks(this) }
    pool.shutdownNow()
  }
}

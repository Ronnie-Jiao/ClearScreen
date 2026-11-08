package com.clearscreen.prototype.backend

import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.UserHandle
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import java.util.Locale

/**
 * 「通知使用权」在这台手机上只能看到通知里出现的渠道，系统不允许第三方应用
 * 列出其他应用的完整渠道清单（getNotificationChannels 会抛 SecurityException，
 * 该接口只对系统级监听服务开放）。因此这里只做观察：记录正在显示的通知和
 * 之后新建/变更的渠道元数据，不读取通知标题或正文。
 */
class ClearScreenNotificationListenerService : NotificationListenerService() {
  override fun onListenerConnected() {
    instance = this
    ClearScreenNotificationChannels.primeFromActive(this)
  }

  override fun onListenerDisconnected() {
    if (instance === this) instance = null
  }

  override fun onDestroy() {
    if (instance === this) instance = null
    super.onDestroy()
  }

  override fun onNotificationPosted(sbn: StatusBarNotification) {
    ClearScreenNotificationChannels.rememberPosted(sbn.packageName, sbn.notification?.channelId)
  }

  override fun onNotificationChannelModified(
    pkg: String,
    user: UserHandle,
    channel: NotificationChannel,
    modificationType: Int,
  ) {
    ClearScreenNotificationChannels.rememberChannel(pkg, channel)
  }

  override fun onNotificationChannelGroupModified(
    pkg: String,
    user: UserHandle,
    group: NotificationChannelGroup,
    modificationType: Int,
  ) {
    ClearScreenNotificationChannels.rememberGroup(pkg, group)
  }

  companion object {
    @Volatile
    internal var instance: ClearScreenNotificationListenerService? = null
  }
}

internal data class ClearScreenChannel(
  val packageName: String,
  val id: String,
  val name: String,
  val importance: Int,
  val group: String?,
)

internal object ClearScreenNotificationChannels {
  private data class Observed(
    val name: String,
    val importance: Int,
    val groupId: String?,
  )

  private val lock = Any()
  private val observed = LinkedHashMap<Pair<String, String>, Observed>()
  private val groupNames = HashMap<Pair<String, String>, String>()

  fun component(context: Context): ComponentName =
    ComponentName(context, ClearScreenNotificationListenerService::class.java)

  fun isGranted(context: Context): Boolean {
    val target = component(context)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
      val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
      val granted = runCatching { manager?.isNotificationListenerAccessGranted(target) ?: false }
        .getOrDefault(false)
      if (granted) return true
    }
    val enabled = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
      .orEmpty()
    return enabled.split(':')
      .mapNotNull { ComponentName.unflattenFromString(it) }
      .any { it.packageName == target.packageName && it.className == target.className }
  }

  fun openSettings(context: Context) {
    context.startActivity(
      Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
  }

  fun primeFromActive(listener: NotificationListenerService) {
    val active = runCatching { listener.activeNotifications }.getOrNull() ?: return
    for (item in active) rememberPosted(item.packageName, item.notification?.channelId)
  }

  fun rememberPosted(packageName: String?, channelId: String?) {
    if (packageName.isNullOrBlank() || channelId.isNullOrBlank()) return
    synchronized(lock) {
      if (!observed.containsKey(packageName to channelId)) {
        observed[packageName to channelId] = Observed("", -1, null)
      }
    }
  }

  fun rememberChannel(packageName: String?, channel: NotificationChannel?) {
    if (packageName.isNullOrBlank() || channel?.id.isNullOrBlank()) return
    val name = channel.name?.toString()?.trim().orEmpty()
    synchronized(lock) {
      observed[packageName to channel.id] = Observed(
        name.take(120),
        channel.importance,
        channel.group?.takeIf { it.isNotEmpty() },
      )
    }
  }

  fun rememberGroup(packageName: String?, group: NotificationChannelGroup?) {
    if (packageName.isNullOrBlank() || group?.id.isNullOrBlank()) return
    val name = group.name?.toString()?.trim()?.takeIf { it.isNotEmpty() } ?: return
    synchronized(lock) { groupNames[packageName to group.id] = name.take(120) }
  }

  /**
   * 只返回观察到的、名称或 id 命中营销关键字的渠道。授权刚生效时系统可能还没
   * 绑定服务，这里等最多 2.5 秒再给出明确提示。观察结果不完整，完整清单要在
   * 连接应用内的直接改设置通道时用 dumpsys 读取。
   */
  fun scanObserved(context: Context): List<ClearScreenChannel> {
    check(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { "当前 Android 版本不支持通知渠道" }
    check(isGranted(context)) { "还没有通知使用权" }
    val listener = awaitListener()
      ?: throw IllegalStateException("系统还没有把通知使用权连到净屏，请稍等再试一次")
    primeFromActive(listener)
    val snapshot = synchronized(lock) {
      observed.entries.map { (key, value) ->
        ClearScreenChannel(
          key.first,
          key.second,
          value.name,
          value.importance,
          value.groupId?.let { groupNames[key.first to it] },
        )
      }
    }
    val channels = mutableListOf<ClearScreenChannel>()
    for (channel in snapshot) {
      val id = channel.id
      val name = channel.name
      if (id.length !in 1..256 || name.length > 120) continue
      if (id.endsWith("...") || id.endsWith("…")) continue
      if (id.any { it.isISOControl() } || name.any { it.isISOControl() }) continue
      if (!ClearScreenShellOps.isMarketingChannelText("$name $id".lowercase(Locale.ROOT))) continue
      if (channels.size >= 500) break
      channels += channel
    }
    return channels.sortedWith(compareBy({ it.packageName }, { it.name }, { it.id }))
  }

  private fun awaitListener(): ClearScreenNotificationListenerService? {
    val deadline = System.currentTimeMillis() + 2500
    while (System.currentTimeMillis() < deadline) {
      ClearScreenNotificationListenerService.instance?.let { return it }
      Thread.sleep(50)
    }
    return ClearScreenNotificationListenerService.instance
  }
}

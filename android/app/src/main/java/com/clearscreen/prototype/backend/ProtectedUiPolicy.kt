package com.clearscreen.prototype.backend

import java.util.Locale

/**
 * Absolute exclusion policy for real alarms, timers, stopwatch, reminders,
 * calendar alerts and other time-sensitive user tasks.
 *
 * This check runs BEFORE ad scoring. Neither high ad score nor custom app
 * preferences may override it. False negatives are safer than dismissing
 * something the user needs to notice.
 *
 * No Android dependencies: protect these cases with JVM regression tests.
 */
internal object ProtectedUiPolicy {
  private val clockPackages = setOf(
    "com.android.deskclock",
    "com.google.android.deskclock",
    "com.vivo.alarmclock",
    "com.vivo.deskclock",
    "com.bbk.alarmclock",
    "com.coloros.alarmclock",
    "com.oplus.clock",
    "com.oneplus.deskclock",
    "com.miui.clock",
    "com.sec.android.app.clockpackage",
    "com.samsung.android.app.clockpackage",
    "com.huawei.deskclock",
    "com.huawei.clock",
    "com.hihonor.deskclock",
    "com.realme.clock",
    "com.google.android.apps.tasks",
    "com.google.android.calendar",
  )
  private val clockTokens = setOf(
    "alarm", "alarms", "alarmclock", "deskclock", "clock",
    "timer", "timers", "stopwatch", "reminder", "reminders",
    "calendar", "countdown", "countdowntimer",
  )
  private val timekeepingLabels = listOf(
    "闹钟", "闹铃", "响铃", "起床铃", "计时器", "倒计时", "秒表",
    "定时提醒", "日程提醒", "事件提醒", "待办提醒", "事项提醒",
    "稍后提醒", "延后提醒", "稍后再响", "重复提醒", "停止计时",
    "时间到了", "时间到", "计时结束", "计时已结束", "暂停计时",
    "alarm", "snooze", "reminder", "stopwatch", "timer",
    "countdown", "wakeupalarm", "alarmringing",
  )
  private val protectedControlIds = listOf(
    "snooze", "alarm_stop", "alarm_dismiss", "dismiss_alarm",
    "stop_alarm", "timer_stop", "stop_timer", "timer_pause",
    "alarm_snooze", "clock_stop", "reminder_dismiss",
    "reminder_snooze", "countdown_stop", "stop_countdown",
  )

  fun isProtectedPackage(packageName: String): Boolean {
    val lower = packageName.lowercase(Locale.ROOT)
    if (lower in clockPackages) return true
    // Protect third-party alarm, timer and calendar apps as well, even if they
    // don't show a machine-readable title on their full-screen alert.
    return lower.split('.', '_', '-').any { it in clockTokens }
  }

  fun isProtectedWindowClass(className: String): Boolean {
    val lower = className.lowercase(Locale.ROOT)
    return listOf(
      "alarmalert", "alarmring", "snooze", "timeralert",
      "timerexpired", "countdown", "stopwatch", "reminderalert",
      "ringingalarm",
    ).any(lower::contains)
  }

  fun isProtectedText(text: String): Boolean {
    val lower = text.lowercase(Locale.ROOT)
    return timekeepingLabels.any(lower::contains)
  }

  fun isProtectedResourceId(resourceId: String): Boolean {
    val lower = resourceId.lowercase(Locale.ROOT)
    // Matching meaningful control names avoids blocking "countdown_skip"
    // inside legitimate advertising SDKs.
    return protectedControlIds.any(lower::contains)
  }

  fun isProtectedScreen(
    packageName: String,
    windowClassName: String = "",
    visibleLabels: Iterable<String> = emptyList(),
    visibleResourceIds: Iterable<String> = emptyList(),
  ): Boolean {
    if (isProtectedPackage(packageName) || isProtectedWindowClass(windowClassName)) return true
    if (visibleLabels.any(::isProtectedText)) return true
    if (visibleResourceIds.any(::isProtectedResourceId)) return true
    return false
  }
}

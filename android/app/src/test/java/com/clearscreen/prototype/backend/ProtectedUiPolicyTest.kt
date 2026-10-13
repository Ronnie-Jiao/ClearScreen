package com.clearscreen.prototype.backend

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProtectedUiPolicyTest {
  @Test fun protectsVendorClockAndAlarmAppsEvenIfAnAdBannerIsPresent() {
    val packages = listOf(
      "com.android.deskclock",
      "com.google.android.deskclock",
      "com.vivo.alarmclock",
      "com.coloros.alarmclock",
      "com.miui.clock",
      "com.sec.android.app.clockpackage",
      "com.huawei.deskclock",
      "com.example.timer",
      "com.example.countdown",
      "com.example.reminder",
      "com.google.android.calendar",
    )
    packages.forEach { pkg ->
      assertTrue(pkg, ProtectedUiPolicy.isProtectedScreen(
        packageName = pkg,
        visibleLabels = listOf("广告", "3 跳过", "关闭"),
      ))
    }
  }

  @Test fun neverClosesAlarmsRemindersOrCountdownScreens() {
    val protected = listOf(
      "闹钟", "正在响铃", "稍后提醒", "定时提醒", "计时器",
      "倒计时", "秒表", "计时已结束", "时间到了",
      "Snooze", "Alarm ringing", "Timer", "Reminder", "Stopwatch",
    )
    protected.forEach { label ->
      assertTrue(label, ProtectedUiPolicy.isProtectedScreen(
        packageName = "com.example.anyapp",
        visibleLabels = listOf("广告", "关闭", label),
      ))
    }
  }

  @Test fun excludesTimerAlarmControlsEvenWhenTheirLabelsAreJustAnX() {
    for (id in listOf(
      "com.foo:id/alarm_dismiss",
      "com.foo:id/snooze_button",
      "com.foo:id/timer_stop",
      "com.foo:id/reminder_dismiss",
      "com.foo:id/stop_countdown",
    )) {
      assertTrue(id, ProtectedUiPolicy.isProtectedScreen(
        packageName = "com.foo",
        visibleLabels = listOf("×", "优惠"),
        visibleResourceIds = listOf(id),
      ))
    }
  }

  @Test fun recognizesAlarmWindowClassesEvenWithNoReadableText() {
    for (clazz in listOf(
      "com.foo.AlarmAlertFullScreen",
      "com.foo.TimerExpiredActivity",
      "com.foo.ReminderAlertDialog",
      "com.foo.RingingAlarmActivity",
    )) {
      assertTrue(clazz, ProtectedUiPolicy.isProtectedScreen(
        packageName = "com.foo",
        windowClassName = clazz,
      ))
    }
  }

  @Test fun realAdSkipCountdownIsNotAutomaticallyEquatedWithClock() {
    assertFalse(ProtectedUiPolicy.isProtectedScreen(
      packageName = "com.example.shopping",
      windowClassName = "com.example.ads.SplashActivity",
      visibleLabels = listOf("广告", "3 跳过", "立即办理"),
      visibleResourceIds = listOf("com.foo:id/ad_skip"),
    ))
  }

  @Test fun promotionalCountdownIsConservativelyExcluded() {
    assertTrue(ProtectedUiPolicy.isProtectedScreen(
      packageName = "com.example.shopping",
      visibleLabels = listOf("广告", "活动倒计时", "关闭广告"),
    ))
  }

  @Test fun ordinaryAppScreensAreNotBlanketBlocked() {
    assertFalse(ProtectedUiPolicy.isProtectedScreen(
      packageName = "com.example.shopping",
      visibleLabels = listOf("广告", "立即办理", "3 跳过"),
    ))
    assertFalse(ProtectedUiPolicy.isProtectedScreen(
      packageName = "com.example.weather",
      visibleLabels = listOf("广告", "×", "滴滴租车"),
    ))
  }
}

package com.clearscreen.probe;

import android.accessibilityservice.AccessibilityService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.view.accessibility.AccessibilityEvent;

public final class ProbeAccessibilityService extends AccessibilityService {
  private static final String CHANNEL_ID = "authorization_probe";
  private static final int NOTIFICATION_ID = 4101;

  @Override
  protected void onServiceConnected() {
    super.onServiceConnected();
    // LiTiaotiao declares FOREGROUND_SERVICE and its active service is not
    // fast-frozen on this device.  Keep this behaviour confined to the probe
    // so we can distinguish a lifecycle/power-management issue from a grant
    // or manifest-declaration issue.
    startProbeForeground();
    getSharedPreferences("probe", MODE_PRIVATE).edit().putBoolean("connected", true).apply();
  }

  private void startProbeForeground() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      NotificationChannel channel = new NotificationChannel(
          CHANNEL_ID,
          "净屏授权探针",
          NotificationManager.IMPORTANCE_LOW);
      channel.setDescription("仅用于验证无障碍服务在后台的连接状态。");
      getSystemService(NotificationManager.class).createNotificationChannel(channel);
    }

    Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
        ? new Notification.Builder(this, CHANNEL_ID)
        : new Notification.Builder(this);
    Notification notification = builder
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle("净屏授权探针正在运行")
        .setContentText("此通知仅用于无障碍授权稳定性测试。")
        .setOngoing(true)
        .build();

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      startForeground(
          NOTIFICATION_ID,
          notification,
          ServiceInfo.FOREGROUND_SERVICE_TYPE_MANIFEST);
    } else {
      startForeground(NOTIFICATION_ID, notification);
    }
  }

  @Override
  public void onAccessibilityEvent(AccessibilityEvent event) {
    // Intentionally empty: this APK isolates authorization persistence from all app logic.
  }

  @Override
  public void onInterrupt() {
  }

  @Override
  public void onDestroy() {
    stopForeground(STOP_FOREGROUND_REMOVE);
    getSharedPreferences("probe", MODE_PRIVATE).edit().putBoolean("connected", false).apply();
    super.onDestroy();
  }
}

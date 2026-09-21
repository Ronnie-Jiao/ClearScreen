package com.clearscreen.probe;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;

public final class ProbeAccessibilityService extends AccessibilityService {
  @Override
  protected void onServiceConnected() {
    super.onServiceConnected();
    getSharedPreferences("probe", MODE_PRIVATE).edit().putBoolean("connected", true).apply();
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
    getSharedPreferences("probe", MODE_PRIVATE).edit().putBoolean("connected", false).apply();
    super.onDestroy();
  }
}

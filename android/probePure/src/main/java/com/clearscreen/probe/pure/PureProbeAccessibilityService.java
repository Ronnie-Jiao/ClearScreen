package com.clearscreen.probe.pure;

import android.accessibilityservice.AccessibilityService;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

public final class PureProbeAccessibilityService extends AccessibilityService {
  private static final String TAG = "ClearScreenPureProbe";
  public static volatile boolean connected;

  @Override
  protected void onServiceConnected() {
    super.onServiceConnected();
    connected = true;
    Log.i(TAG, "onServiceConnected");
  }

  @Override
  public void onAccessibilityEvent(AccessibilityEvent event) {
    // Intentionally empty. This probe only measures whether the system keeps the service bound.
  }

  @Override
  public void onInterrupt() {
  }

  @Override
  public void onDestroy() {
    connected = false;
    Log.w(TAG, "onDestroy");
    super.onDestroy();
  }
}

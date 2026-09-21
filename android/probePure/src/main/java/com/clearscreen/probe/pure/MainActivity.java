package com.clearscreen.probe.pure;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class MainActivity extends Activity {
  private TextView status;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    LinearLayout content = new LinearLayout(this);
    content.setOrientation(LinearLayout.VERTICAL);
    int padding = (int) (24 * getResources().getDisplayMetrics().density);
    content.setPadding(padding, padding, padding, padding);

    TextView title = new TextView(this);
    title.setText("净屏纯无障碍探针");
    title.setTextSize(24);
    content.addView(title, new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

    status = new TextView(this);
    status.setTextSize(15);
    content.addView(status, new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

    Button accessibility = new Button(this);
    accessibility.setText("打开无障碍设置");
    accessibility.setOnClickListener(view ->
        startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
    content.addView(accessibility, new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

    setContentView(content);
  }

  @Override
  protected void onResume() {
    super.onResume();
    refreshStatus();
  }

  private void refreshStatus() {
    String enabled = Settings.Secure.getString(
        getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
    String component = new ComponentName(this, PureProbeAccessibilityService.class).flattenToString();
    boolean isEnabled = false;
    if (enabled != null) {
      for (String item : enabled.split(":")) {
        if (component.equalsIgnoreCase(item)) {
          isEnabled = true;
          break;
        }
      }
    }
    boolean connected = PureProbeAccessibilityService.connected;
    status.setText(
        "包名：" + getPackageName() + "\n" +
        "系统开关：" + (isEnabled ? "已开启" : "已关闭") + "\n" +
        "服务连接：" + (connected ? "已连接" : "未连接") + "\n" +
        "服务组件：" + component + "\n\n" +
        "这个 APK 只有一个页面和一个无障碍服务，\n" +
        "没有前台通知、悬浮窗、电量优化、VPN 或 React Native。\n" +
        "只用于判断手机系统是否能稳定保留授权。");
  }
}

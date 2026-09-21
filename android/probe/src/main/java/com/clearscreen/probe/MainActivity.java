package com.clearscreen.probe;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

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
    title.setText("净屏授权探针");
    title.setTextSize(24);
    content.addView(title, new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

    status = new TextView(this);
    status.setTextSize(15);
    content.addView(status, new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

    Button accessibility = new Button(this);
    accessibility.setText("打开无障碍设置");
    accessibility.setOnClickListener(view -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
    content.addView(accessibility, new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

    Button appInfo = new Button(this);
    appInfo.setText("打开应用信息");
    appInfo.setOnClickListener(view -> startActivity(new Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        android.net.Uri.parse("package:" + getPackageName()))));
    content.addView(appInfo, new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

    Button refresh = new Button(this);
    refresh.setText("刷新诊断");
    refresh.setOnClickListener(view -> refreshStatus());
    content.addView(refresh, new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

    ScrollView scrollView = new ScrollView(this);
    scrollView.addView(content);
    setContentView(scrollView);
  }

  @Override
  protected void onResume() {
    super.onResume();
    refreshStatus();
  }

  private void refreshStatus() {
    String enabled = Settings.Secure.getString(
        getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
    String component = new ComponentName(this, ProbeAccessibilityService.class).flattenToString();
    boolean isEnabled = false;
    if (enabled != null) {
      for (String item : enabled.split(":")) {
        if (component.equalsIgnoreCase(item)) {
          isEnabled = true;
          break;
        }
      }
    }

    PackageManager packageManager = getPackageManager();
    PackageInfo packageInfo;
    try {
      packageInfo = packageManager.getPackageInfo(getPackageName(), PackageManager.GET_SIGNING_CERTIFICATES);
    } catch (PackageManager.NameNotFoundException error) {
      status.setText(error.toString());
      return;
    }

    String installer = "未知";
    String initiating = "未知";
    String originating = "未知";
    String packageSource = "未知";
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      try {
        installer = String.valueOf(packageManager.getInstallSourceInfo(getPackageName()).getInstallingPackageName());
        initiating = String.valueOf(packageManager.getInstallSourceInfo(getPackageName()).getInitiatingPackageName());
        originating = String.valueOf(packageManager.getInstallSourceInfo(getPackageName()).getOriginatingPackageName());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          packageSource = packageSourceLabel(packageManager.getInstallSourceInfo(getPackageName()).getPackageSource());
        }
      } catch (PackageManager.NameNotFoundException ignored) {
        // The package is already known above; leave the source fields as unknown.
      }
    }

    DateFormat format = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM, Locale.CHINA);
    status.setText(
        "包名：" + getPackageName() + "\n" +
        "版本：" + packageInfo.versionName + "\n" +
        "服务：" + (isEnabled ? "已启用" : "已关闭") + "\n" +
        "安装器：" + installer + "\n" +
        "发起者：" + initiating + "\n" +
        "来源：" + originating + " / " + packageSource + "\n" +
        "首次安装：" + format.format(new Date(packageInfo.firstInstallTime)) + "\n" +
        "最近更新：" + format.format(new Date(packageInfo.lastUpdateTime)) + "\n" +
        "服务组件：" + component);
  }

  private static String packageSourceLabel(int source) {
    if (source == PackageInstaller.PACKAGE_SOURCE_UNSPECIFIED) return "unspecified";
    if (source == PackageInstaller.PACKAGE_SOURCE_OTHER) return "other";
    if (source == PackageInstaller.PACKAGE_SOURCE_STORE) return "store";
    if (source == PackageInstaller.PACKAGE_SOURCE_LOCAL_FILE) return "local_file";
    if (source == PackageInstaller.PACKAGE_SOURCE_DOWNLOADED_FILE) return "downloaded_file";
    return "unknown(" + source + ")";
  }
}

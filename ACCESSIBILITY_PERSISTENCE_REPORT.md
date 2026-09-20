# 净屏无障碍服务“返回后自动关闭”问题报告

更新时间：2026-09-20

## 一、问题结论

在 vivo V2502DA、Android 16 / OriginOS 16 真机上，可以在“无障碍服务详情页”把“净屏”开关打开，但返回上一层的无障碍服务列表后，系统会把“净屏”显示为“已关闭”。

这不是 React Native 页面状态没有刷新，而是系统安全设置中的持久值被系统移除了。实测证据：

```text
# 打开详情页并确认允许后
com.clearscreen.prototype/com.clearscreen.prototype.backend.ClearScreenAccessibilityService:com.vivo.dr/com.vivo.dr.LocateBehaviorAnalysisService

# 从详情页返回列表后
com.vivo.dr/com.vivo.dr.LocateBehaviorAnalysisService
```

因此，应用本身无法通过刷新页面或保存一个本地开关来解决；授权状态的最终来源是 Android 的 `Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES`。

## 二、稳定复现步骤

1. 安装当前 APK，使用覆盖安装方式，不清除数据。
2. 打开“净屏” → “开启权限” → “自动跳过权限” → “去设置”。
3. 在 vivo 无障碍列表中打开“净屏”。
4. 在安全确认弹窗中选择“允许”。
5. 返回无障碍服务列表。
6. “净屏”马上显示为“已关闭”，并且上面的 secure setting 值中不再包含净屏服务组件。

## 三、设备与安装信息

- 设备：vivo V2502DA / PD2502
- Android：16
- 系统：OriginOS 16
- 屏幕：1260 × 2800
- 包名：`com.clearscreen.prototype`
- 服务组件：`com.clearscreen.prototype/.backend.ClearScreenAccessibilityService`
- 安装方式：ADB 覆盖安装，`pm install -r -d --user 0`
- 当前安装来源：`com.android.shell`，`installerPackageName=null`

## 四、已经尝试过的修复

### 1. 服务生命周期调整

- 移除了自定义 `onUnbind()` / `onRebind()` 处理。
- 保留标准的 `onServiceConnected()`、`onInterrupt()`、`onDestroy()`。

结果：仍然会在返回列表时关闭。

### 2. 服务独立进程

Manifest 中尝试使用：

```xml
android:process=":accessibility"
```

并让 `MainApplication` 在 `:accessibility` 进程中跳过 React Native / Expo 初始化，避免主界面进程影响无障碍服务。

实测服务进程确实存在：

```text
com.clearscreen.prototype
com.clearscreen.prototype:accessibility
```

结果：仍然会被系统列表移除。

### 3. 无障碍 XML 配置兼容调整

已尝试：

- 移除 `android:isAccessibilityTool`。
- 移除 `android:settingsActivity`。
- 使用完整事件类型 `typeAllMask`。
- 使用完整反馈类型 `feedbackAllMask`。
- 开启 `canRetrieveWindowContent`、`canRequestFilterKeyEvents`、`canPerformGestures`。
- 使用与手机上同类服务接近的 flags：`0x73`。

结果：仍然会被系统列表移除。

### 4. target SDK 兼容测试

参考手机上安装的同类 `com.android.skip` 服务，将实验包 target SDK 临时改为 34；返回列表后仍然显示“已关闭”。该实验已撤回，项目源码恢复使用原来的 target SDK 配置，避免留下未验证的 Android 行为降级。

### 5. 崩溃与进程检查

返回列表后检查：

- `dumpsys accessibility` 未发现 `Crashed services`。
- logcat 未发现 `FATAL EXCEPTION` 或 `AndroidRuntime` 崩溃。
- 无障碍服务进程仍然存在。

这更像是 vivo 设置应用的授权策略主动清理，而不是服务启动崩溃。

## 五、当前代码状态

### 已修复：点击任意位置跳到“记录”页

此前无障碍服务会扫描“净屏”自己的界面。首页中的“今日自动跳过”“自动跳过权限”会被误识别为广告的“跳过”按钮，服务随后点击对应的父级 Pressable，而首页这些 Pressable 中有跳转“记录”的入口。

已在 `ClearScreenAccessibilityService.kt` 中加入：

- 不处理 `com.clearscreen.prototype` 自己的窗口。
- 收紧“跳过”文本匹配，避免把“自动跳过权限”当成广告跳过按钮。

实机验证结果：无障碍服务保持启用时，首页点击“应用”“设置”“权限”都能进入正确页面，不再随机回到“记录”。

### 当前仍未解决：系统授权持久化

相关文件：

- `android/app/src/main/AndroidManifest.xml`
- `android/app/src/main/res/xml/accessibility_service_config.xml`
- `android/app/src/main/java/com/clearscreen/prototype/backend/ClearScreenAccessibilityService.kt`
- `android/app/src/main/java/com/clearscreen/prototype/MainApplication.kt`
- `android/app/src/main/java/com/clearscreen/prototype/backend/ClearScreenNativeModule.kt`

TypeScript 检查和 Release 构建均通过。

## 六、建议交给下一位 AI 的重点方向

1. 优先确认 OriginOS 对“ADB 侧载的无障碍服务”是否有受限设置 / 来源校验。当前包的安装来源是 `com.android.shell`，不是应用商店或系统可信安装源。
2. 对比 `com.android.skip` 的完整安装来源、签名、权限、应用启用状态和无障碍服务 XML，而不仅是 target SDK。
3. 检查 vivo 设置应用在返回详情页时的 logcat，重点搜索 `AccessibilityManagerService`、`AccessibilitySettings`、`vivo`、`restricted`、`prohibited`、`accessibility`。
4. 分别验证以下变量，不要同时修改多个变量：
   - 是否去掉 `android:process=":accessibility"`。
   - 是否恢复 `android:isAccessibilityTool="true"`。
   - 是否恢复 `android:settingsActivity`。
   - 是否使用与参考服务完全相同的 target / flags / event types。
5. 每次只用系统设置界面点击“允许”，不要只用 `settings put secure` 模拟授权；后者可能绕过 vivo 的真实校验流程。
6. 每次返回后同时记录以下三项：

```text
adb shell settings --user 0 get secure enabled_accessibility_services
adb shell dumpsys accessibility
adb logcat -d -v threadtime
```

## 七、交接时需要明确的边界

当前可以确认：

- APK 能正常构建和覆盖安装。
- 覆盖安装不会清除本地规则、设置和记录数据。
- “点击任意位置跳到记录页”的应用内误触问题已经修复。
- vivo 无障碍详情页返回后自动关闭的问题仍未解决，根因更可能在系统授权策略或安装来源校验。

不要把“应用页面显示已开启”当作无障碍服务真正启用；必须以系统无障碍列表和 `Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES` 的返回结果为准。

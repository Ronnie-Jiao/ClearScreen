# 净屏无障碍服务“返回后自动关闭”问题报告

更新时间：2026-09-21

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
- 当前设备 `dumpsys package` 还显示 `packageSource=1`（`OTHER`）；因此不能只用 `LOCAL_FILE` / `DOWNLOADED_FILE` 判断侧载，必须同时考虑“安装者为空 + `OTHER`/`UNSPECIFIED`”这一类 ADB 结果。

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

## 八、按 Android 16 安装来源机制补充处理

结合 Android 16 的受限设置机制，当前版本已补充两项处理：

1. 原生快照新增安装来源诊断：
   - `installingPackageName`
   - `initiatingPackageName`
   - `originatingPackageName`
   - `packageSource` / `packageSourceLabel`
   - `adbInstallLikely`
   - `restrictedSettingsLikely`
2. 权限页会根据安装来源选择引导路径：可信来源直接打开系统无障碍页；检测到 ADB/侧载风险时，先提供“打开应用信息”和“直接去无障碍”两个快捷入口。
3. 从系统无障碍页返回后，应用会自动重新读取系统授权状态。若开关没有保持开启，会提示用户打开应用信息；如果系统没有提供“允许受限设置”，则明确提示通过 vivo EasyShare 或可信应用商店重新安装。

这只是引导和诊断，不会伪造授权状态，也不会通过 `settings put secure` 写入系统设置。`EnhancedConfirmationManager` 属于系统/隐藏 API，应用不直接调用；最终授权仍以系统无障碍列表和 `Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES` 为准。

## 九、最新真机验证结果

已将版本 `1.0.9`（`versionCode=10`）覆盖安装到测试机，未清除应用数据。vivo 安装器明确显示：

- “来自未知来源”
- “外部来源应用：该应用来源于非 vivo 官方应用商店，未经 vivo 人工亲测”

安装后的系统信息仍为：

```text
versionCode=10
versionName=1.0.9
installerPackageName=null
initiatingPackageName=com.android.shell
packageSource=1
```

在无障碍详情页打开净屏后，`ENABLED_ACCESSIBILITY_SERVICES` 仍只保留 vivo 自己的服务；返回列表后净屏显示“已关闭”。同时 `dumpsys accessibility` 没有 `Crashed services`，说明这次仍是系统安装来源/受限设置策略拒绝持久化，不是净屏服务崩溃。

因此新版已经把失败状态和下一步衔接补上，但在这台 vivo 设备上，ADB 安装本身仍不具备授予该受限无障碍权限的资格。若“应用信息”里没有“允许受限设置”，应用代码不能绕过系统校验；必须改用 vivo EasyShare 或 vivo 官方应用商店等可信安装来源。

## 十、本轮参考李跳跳的兼容改动

对公开的李跳跳 2.2 APK 做了静态对比：其 `targetSdkVersion=33`，无障碍服务没有声明单独进程；仓库中公开的主要是规则文件和成品 APK，并没有可直接移植的授权实现。

净屏 `1.0.11` 已做以下改动：

- 无障碍服务移除 `android:process=":accessibility"`，回到与主界面相同的应用进程，减少 vivo 对额外服务进程的兼容变量。
- 对齐李跳跳 2.2 的服务声明兼容点：服务使用单进程、`exported=false`、`settingsActivity`、通用反馈类型和 `canPerformGestures=true`；保留净屏识别窗口所需的 `flagReportViewIds`、`flagRetrieveInteractiveWindows` 等 flags。
- 在 `res/xml-v31/accessibility_service_config.xml` 中声明 `isAccessibilityTool=true`，与李跳跳在 Android 12+ 的配置一致；API 30 及以下继续使用不含该属性的基础配置。
- 增加 `-PclearscreenTargetSdk=33` 的兼容测试参数；默认构建仍为 target 36，因此可以把 target 33 与 target 36 在同一台手机上分别验证。
- 应用信息与无障碍设置之间增加回流引导：从应用信息返回后，会提示继续打开无障碍，而不是让用户重新猜下一步。

这组改动不会绕过 Android 16 / OriginOS 的 Restricted Settings。

## 十一、1.0.11 真机复测结果（2026-09-21）

本轮已经完成 `1.0.11`（`versionCode=12`、`targetSdk=33`）的 Release 包复测。该包的无障碍服务声明已按李跳跳 2.2 的可观测配置对齐，并通过 `aapt2` 检查；安装后仍复现“打开后返回即关闭”。

复测时手机上的实际系统状态为：

```text
净屏：versionCode=12, versionName=1.0.11, targetSdk=33
installerPackageName=com.android.packageinstaller
initiatingPackageName=com.android.packageinstaller

Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES:
hello.litiaotiao.app/hello.litiaotiao.app.LttService
com.vivo.dr/com.vivo.dr.LocateBehaviorAnalysisService

dumpsys accessibility:
Enabled services: 李跳跳、vivo 自带服务
Crashed services: {}
```

这次安装是通过手机系统安装器完成的，不是 ADB，也不是 vivo EasyShare；因此“ADB 侧载导致受限设置失败”已经不能作为唯一结论。当前能确定的是：

- 净屏服务声明、单进程、`exported=false`、`targetSdk=33` 以及 API 31+ 的 `isAccessibilityTool=true` 已完成兼容调整。
- 返回后净屏组件从 `ENABLED_ACCESSIBILITY_SERVICES` 中消失，系统没有报告服务崩溃。
- 李跳跳稳定开启与“安装包不是通过 EasyShare 传输”并不矛盾：实测李跳跳的安装来源是 `com.android.packageinstaller`，而不是 `com.android.shell`。这说明“安装器来源”与“是否使用 EasyShare”是两个不同维度。
- 目前剩余差异可能在 vivo 对应用身份、签名、包历史、服务元数据或受限设置白名单的校验上，不能再仅凭 React Native、页面刷新或服务生命周期代码推断。

因此本报告的最终结论是：问题仍未修复，但已经确认不是界面显示缓存、服务进程崩溃或单纯 ADB 安装来源问题。下一步应从 vivo 系统设置返回瞬间抓取 `AccessibilityManagerService`、`AccessibilitySettings` 和 vivo 相关日志，并与李跳跳的完整签名、安装历史、服务元数据做 A/B 对比；应用代码不能强行写入或伪造无障碍授权。

## 十二、仓库合并与推送状态

此前已检查当前仓库的本地分支、远程分支和 Git worktree：当时只有 `main` 工作树，没有发现其他 Agent 或其他窗口留下的可合并分支；1.0.11 复测结论已由提交 `c702774` 推送到 `origin/main`。本轮诊断改造在独立的 `accessibility-diagnostics` 分支上进行。

## 十三、诊断分支执行结果（2026-09-21）

已按后续分析建立独立分支 `accessibility-diagnostics`，暂不覆盖普通发布分支。当前已完成：

- 正式 Release 签名配置：`android/keystore.properties.example`、`tools/New-ClearScreenSigning.ps1` 和 Gradle 的正式签名读取逻辑。实际 keystore 与密码只保存在本机并被 Git 忽略；四个诊断 APK 已验证使用同一 SHA-256 签名证书。
- 1.0.14 诊断包：通过 `-PclearscreenApplicationId=com.clearscreen.app` 生成正式测试包身份，并在 `diagnostic` 构建类型中移除 VPN、悬浮窗、后台、电池优化和旧存储等可选能力。APK 级 Manifest 校验只保留 `INTERNET` 与系统动态接收器权限。
- `lttCompat` 对照构建：包名固定为 `com.clearscreen.app.lttcompat`，不会覆盖诊断包；只在该测试变体保留 `isAccessibilityTool=true`。
- 无障碍 XML：把 `typeAllMask` 收窄为 `typeWindowContentChanged|typeWindowStateChanged`，降低无关系统事件干扰；保留净屏识别所需的窗口、资源 ID 和手势能力。
- `com.clearscreen.probe` 生命周期对照探针：保留前台通知，用来验证“前台保活”是否影响结果。
- `com.clearscreen.probe.pure` 纯无障碍探针：只有一个 Activity 和一个空的 AccessibilityService，不声明任何应用权限，不包含 React Native、Expo、VPN、悬浮窗、前台服务或后台能力。
- A/B 脚本：`tools/Build-AccessibilityDiagnostics.ps1`、`tools/Collect-AccessibilityDiagnostics.ps1`、`tools/Compare-AccessibilityPackages.ps1`，用于采集安装来源、`packageSource`、首次安装时间、最近更新时间、签名摘要、无障碍列表和返回瞬间日志。

同时用脚本对当前手机上的旧包做了只读对比：

```text
净屏 1.0.11：installerPackageName=com.android.packageinstaller
净屏 1.0.11：packageSource=3（本地文件）
净屏首次安装：2026-09-20 15:33:13
净屏当前证书 SHA-256：fac61745dc0903786fb9ede62a962b399f7348f0bb6f899b8332667591033b9c

李跳跳 2.2：installerPackageName=com.android.packageinstaller
李跳跳 2.2：packageSource=3（本地文件）
李跳跳首次安装：2026-09-21 01:00:01
李跳跳当前证书 SHA-256：7a8896226fa1239e60114cb2de411cc9fa688fd2fb0d26b4c49597d858bfac86

诊断包正式证书 SHA-256：93c410454a8263736ff60bb6e27153b19f74f4c934f444531842f5162ccfa999
```

这组数据说明：当前净屏与李跳跳的安装器和 `packageSource` 已经相同，且李跳跳并非旧系统遗留安装；两者最明显的未对齐变量确实是签名身份。当前手机上的净屏 1.0.11 仍使用旧 Debug 证书，新诊断包已经切换到固定正式证书，适合进行下一轮 A/B 授权测试。

本轮构建验证已通过：

```text
android/app/build/outputs/apk/diagnostic/app-diagnostic.apk
android/app/build/outputs/apk/lttCompat/app-lttCompat.apk
android/probe/build/outputs/apk/release/probe-release.apk
android/probePure/build/outputs/apk/release/probePure-release.apk
```

四个包的实际身份和签名已静态核验：

```text
净屏（诊断版）       com.clearscreen.app           targetSdk=33
净屏兼容测试         com.clearscreen.app.lttcompat targetSdk=33
净屏前台保活对照探针 com.clearscreen.probe        targetSdk=33
净屏纯无障碍探针     com.clearscreen.probe.pure    targetSdk=33
签名 SHA-256          93c410454a8263736ff60bb6e27153b19f74f4c934f444531842f5162ccfa999
```

本轮已在实体 vivo 手机上完成 USB A/B 授权操作。四个实验包均已安装，但纯探针仍然出现“打开后返回即关闭”，详细证据见下一节。由于纯探针本轮由 USB 调试安装，下一步仍需用手机系统安装器或 EasyShare 安装同一个纯探针，比较不同安装来源是否改变结果。

## 十四、纯无障碍探针真机结果（2026-09-22）

本轮使用实体设备 `10AG4M2KGF00527`，安装并测试了不包含 React Native、Expo、VPN、悬浮窗、前台服务、后台能力和业务逻辑的最小包：

```text
包名：com.clearscreen.probe.pure
安装方式：USB 调试安装
installerPackageName：null
initiatingPackageName：com.android.shell
packageSource：1
```

用户在系统无障碍设置中打开纯探针后返回，日志明确记录：

```text
09-22 09:35:13.109  I ClearScreenPureProbe: onServiceConnected
09-22 09:35:55.202  W ClearScreenPureProbe: onDestroy
```

也就是说，系统曾经真正启动并连接了纯探针服务；约 42 秒后服务被销毁。随后系统最终保存的 `enabled_accessibility_services` 只剩：

```text
hello.litiaotiao.app/hello.litiaotiao.app.LttService
com.vivo.dr/com.vivo.dr.LocateBehaviorAnalysisService
```

纯探针已经不在授权名单中。同时 `dumpsys accessibility` 显示：

```text
Bound services：只有李跳跳和 vivo 位置引擎
Binding services：{}
Crashed services：{}
```

这次结果排除了“净屏主界面进程太重”“React/Expo 导致服务崩溃”“广告识别代码导致服务退出”“前台保活或额外权限不足”等解释。当前最强结论是：vivo 在 USB/ADB 侧载的这个新包上，曾允许服务启动，但随后没有保留该无障碍授权，或由系统策略主动移除了它。由于本次纯探针是 ADB 安装，仍需通过手机系统安装器/EasyShare 再做一次相同测试，才能最终区分“ADB 安装来源限制”和“包名/签名等身份限制”。

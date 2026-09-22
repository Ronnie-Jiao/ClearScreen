# ClearScreen 无障碍授权返回后自动关闭：统一问题报告

更新时间：2026-09-22

## 一、结论摘要

在 vivo V2502DA（PD2502）、Android 16 / OriginOS 16 手机上，用户可以在系统无障碍设置中暂时打开“净屏”，但返回列表或返回应用后，系统又显示“已关闭”。

这不是净屏页面刷新错误，也不是广告识别代码造成的。系统的最终授权名单没有保留净屏服务，或者在服务连接后又主动将它移除。应用只能读取这个结果，不能通过页面代码强行授予无障碍权限。

目前最强的根因范围是：vivo 对应用的包名、签名、服务元数据、安装历史或安装来源进行系统级校验，并在不满足条件时解绑/移除无障碍服务。尚不能仅凭现有证据确定是哪一项单独触发。

## 二、稳定复现方式

1. 安装净屏 APK。
2. 打开“净屏” → “开启权限” → “自动跳过权限” → “去设置”。
3. 在系统“无障碍”列表中打开“净屏”。
4. 在系统确认框中选择“允许”。
5. 返回无障碍列表或返回净屏。
6. 净屏重新显示为“已关闭”。

判断是否真正开启，必须以系统无障碍列表和下面的系统值为准，而不是只看净屏自己的页面：

```text
Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
```

## 三、测试环境与安装身份

- 设备：vivo V2502DA / PD2502
- 系统：Android 16 / OriginOS 16
- 屏幕：1260 × 2800
- 原净屏包名：`com.clearscreen.prototype`
- 原无障碍服务：`com.clearscreen.prototype/.backend.ClearScreenAccessibilityService`
- 典型测试版本：1.0.11（versionCode 12）、1.0.14（versionCode 20）
- 典型 target SDK：33

已对比过的安装身份：

```text
净屏 1.0.11：installerPackageName=com.android.packageinstaller
净屏 1.0.11：packageSource=3（本地文件）
李跳跳 2.2：installerPackageName=com.android.packageinstaller
李跳跳 2.2：packageSource=3（本地文件）
```

因此，“是否通过 vivo 互传”不是唯一变量；李跳跳与净屏在一次复测中使用了相同的系统安装器，但净屏仍然无法保持授权。两者仍存在签名、包身份、服务元数据和安装历史等差异。

## 四、已经做过的修复和实验

### 1. 修正净屏自身的状态误报

旧代码只要看到系统记录或本地状态满足其一，就显示“已开启”。现在改为必须同时满足：

- 系统保存了净屏无障碍组件；
- 无障碍服务真实触发 `onServiceConnected` 并仍在运行。

这样可以避免净屏显示假成功，但不能替系统保存授权。

### 2. 对齐李跳跳的简单服务结构

已尝试：

- 移除独立 `:accessibility` 进程；
- 去掉前台特殊服务和 `specialUse` 声明；
- 使用普通单进程 `AccessibilityService`；
- 对齐 `exported=false`、`targetSdk=33`、`settingsActivity`、反馈类型和手势能力；
- API 31+ 配置 `isAccessibilityTool=true`；
- 调整无障碍 XML 的事件类型和 flags；
- 对 target SDK 34 做过独立兼容测试，失败后已撤回。

以上改动都没有解决返回后被关闭的问题。

### 3. 排查服务崩溃和进程问题

多次检查均未发现 `FATAL EXCEPTION`、`AndroidRuntime` 崩溃或 `Crashed services`。早期还曾通过系统绑定测试看到净屏出现 `onServiceConnected`，说明服务代码不是一启动就必崩。

### 4. 完成最小纯无障碍探针

新增 `com.clearscreen.probe.pure`，只包含一个 Activity 和一个空的 AccessibilityService，不包含 React Native、Expo、VPN、悬浮窗、前台服务、后台能力、广告规则或额外应用权限。

## 五、关键真机证据

### 1. 正常系统安装器复测仍然失败

净屏 1.0.11 通过手机系统安装器安装，安装身份与李跳跳的 `com.android.packageinstaller` 对齐，target SDK 和无障碍服务结构也已对齐；但返回后净屏仍从 `ENABLED_ACCESSIBILITY_SERVICES` 消失，且没有服务崩溃记录。

所以不能把问题简单归结为“只要不用 ADB 就能解决”。

### 2. 纯探针也复现了相同问题

实体设备序列号：`10AG4M2KGF00527`

纯探针本轮通过 USB 调试安装：

```text
包名：com.clearscreen.probe.pure
installerPackageName：null
initiatingPackageName：com.android.shell
packageSource：1
```

用户在系统设置中打开纯探针后，日志记录：

```text
09-22 09:35:13.109  I ClearScreenPureProbe: onServiceConnected
09-22 09:35:55.202  W ClearScreenPureProbe: onDestroy
```

这说明系统确实启动并连接过服务，约 42 秒后服务被销毁。最终系统保存的授权名单只剩：

```text
hello.litiaotiao.app/hello.litiaotiao.app.LttService
com.vivo.dr/com.vivo.dr.LocateBehaviorAnalysisService
```

同时 `dumpsys accessibility` 显示：

```text
Bound services：只有李跳跳和 vivo 位置引擎
Binding services：{}
Crashed services：{}
```

纯探针没有业务逻辑却仍被移除，排除了“净屏主界面太重”“React/Expo 初始化影响服务”“广告识别导致退出”“前台保活不足”等解释。

## 六、与李跳跳的差异和根因判断

李跳跳能稳定开启，说明这台 vivo 手机并非完全禁止第三方无障碍服务。现有证据显示，李跳跳与净屏至少可能存在以下差异：

- 签名证书不同；
- 包名和包历史不同；
- 无障碍服务元数据仍可能不同；
- vivo 可能对不同应用身份设置不同的系统信任或白名单结果；
- 不同测试包的安装来源和安装过程可能不同。

当前可以排除的方向：

- 不是单纯的页面状态刷新；
- 不是广告识别规则导致；
- 不是 React Native 或 Expo 必然导致；
- 不是服务一启动就崩溃；
- 不是单纯把服务改成独立进程或前台服务就能解决；
- 也不能仅凭“ADB 安装”解释全部现象，因为系统安装器安装的 1.0.11 仍然失败。

当前最合理的判断是：vivo 在系统设置返回时对该应用的无障碍授权进行二次校验，校验未通过后将组件解绑并从授权名单移除。具体是签名、包身份、服务元数据还是厂商内部策略，仍需同一个探针换安装链路继续做 A/B 测试。

## 七、已经修复的另一个独立问题

此前无障碍服务会扫描净屏自己的页面，把“今日自动跳过”“自动跳过权限”等文字误判为广告的“跳过”按钮，导致用户点击首页任意位置后跳到“记录”页。

已在 `ClearScreenAccessibilityService.kt` 中：

- 忽略净屏自己的窗口；
- 收紧“跳过”文本匹配。

该问题已经通过真机验证，不应与“无障碍授权返回后关闭”混为同一个问题。

## 八、下一步最有价值的测试

用同一个 `com.clearscreen.probe.pure` APK，通过手机文件管理器的系统安装流程或 EasyShare 安装，不要直接用 ADB 安装。然后重复“打开无障碍 → 返回列表”的操作，并记录：

```text
adb shell settings --user 0 get secure enabled_accessibility_services
adb shell dumpsys accessibility
adb logcat -d -v threadtime
```

结果判断：

- 如果系统安装器/EasyShare 安装后能保持开启，重点就是 ADB 安装来源限制；
- 如果仍然被关闭，重点就是包身份、签名、服务元数据或 vivo 的系统白名单/策略；
- 如果两种方式都失败，继续修改净屏业务代码基本没有意义，需要使用系统认可的签名和发布渠道，或向 vivo 适配/申诉。

应用不能通过 `settings put secure` 伪造授权，也不能绕过 vivo 的系统校验。正式发布前必须以系统无障碍列表实际保持开启为验收标准。

## 九、代码与仓库状态

- 分支：`accessibility-diagnostics`
- 已构建并验证的诊断包：正式诊断版、李跳跳兼容版、前台保活探针、纯无障碍探针。
- 诊断包正式签名 SHA-256：`93c410454a8263736ff60bb6e27153b19f74f4c934f444531842f5162ccfa999`
- 最近已推送提交：`52860a9`（纯探针真机证据报告）。
- 当前结论：问题尚未修复，已完成根因范围收敛和真机证据记录。

# 净屏 Wiki

本文档是 `C:\develop\ClearScreen` 的项目 Wiki，记录当前实现、权限边界、构建方式、安装约定和真机验证要求。代码行为发生变化时，应同步更新本页和根目录 `README.md`，并在[修改记录](CHANGELOG.md)追加带时间的条目。每次开始修改项目前，先阅读修改记录最新条目。

最后更新：2026-09-22

## 1. 项目定位

净屏是一个以 Android 无障碍服务为核心的本地应用，用于在用户明确开启规则的应用中识别高置信度的“跳过广告”控件，以及带有明确广告语境的弹窗关闭控件。

当前应用名称是“净屏”，Android 技术包名是 `com.clearscreen.prototype`。包名、Release 签名和版本升级关系必须保持稳定，否则 Android 会把它当作新应用，原有授权和本地数据无法可靠衔接。

净屏不申请、不启动、不接管系统 VPN。网络过滤功能已经停用，用户可以同时使用自己的 VPN 客户端连接国外网络。

## 2. 工程结构

- `App.tsx`：React Native/Expo 应用入口、页面导航、总开关和权限引导。
- `src/screens/`：欢迎页、权限页、首页、应用列表、应用详情、记录和设置页面。
- `src/native/ClearScreenNative.ts`：JavaScript 与 Android 原生模块之间的类型和状态适配。
- `android/app/src/main/java/com/clearscreen/prototype/backend/ClearScreenNativeModule.kt`：原生状态、应用规则、无障碍状态和系统设置跳转。
- `android/app/src/main/java/com/clearscreen/prototype/backend/ClearScreenAccessibilityService.kt`：无障碍事件监听、广告控件识别和安全点击策略。
- `android/app/src/main/java/com/clearscreen/prototype/backend/ClearScreenStore.kt`：本地设置、应用规则和事件记录。
- `android/app/src/main/res/xml/accessibility_service_config.xml`：无障碍服务声明。
- `ACCESSIBILITY_PERSISTENCE_REPORT.md`：vivo/Android 无障碍授权保持问题的诊断记录。

## 3. 自动跳过和弹窗处理边界

### 3.1 会自动处理的控件

总开关开启、目标应用的“自动跳过”开启且应用不在白名单时，服务可能点击以下控件：

- 明确的广告跳过文字：`跳过广告`、`跳过此广告`、`跳过视频`、`Skip Ad` 等。
- 倒计时跳过文字：`3秒跳过`、`跳过3秒`、`3 Skip` 等匹配规则的控件。
- 资源 ID 中明确包含 `skip`、`jump`、`countdown`、`ad_skip` 等标记的可点击控件；资源 ID 只作为辅助信号，必须同时满足右上角紧凑位置，或存在广告/视频广告语境，才会被点击。单独的 ID 命中不再足以触发点击，避免普通跳转容器（如 `xxx_jump`）被误判。
- 位于右上角、尺寸较小的普通“跳过/Skip”控件；这是兼容部分没有暴露“广告”文字的开屏页面。
- 视频广告的右上角“跳过”：即使文字节点的可点击父节点覆盖范围较大，也会以文字节点的位置和尺寸判断广告控件，再点击其可点击父节点；动态出现的控件会在约 1 秒内进行三次短时重扫。
- 具有本地广告标记或广告资源 ID 的弹窗关闭控件，例如“关闭广告”、广告关闭叉号和 `ad_close`、`close_ad`、`adclose` 等资源。
- 整页遮罩广告：当可访问节点同时呈现广告强标记、多个商业/营销词、较大的遮罩或广告面板，并在右上角暴露紧凑的“关闭”/`X`/图标控件时，使用独立的全屏广告规则关闭它；无文字的 `ImageView` 关闭图标也只在这一组证据同时成立时处理。

### 3.2 明确不会自动处理的内容

- 普通业务按钮：`确定`、`允许`、`授权`、`下一步`、`领取`、`打开`、`下载` 等。
- 没有可访问节点或没有可点击动作的图片、Canvas、视频内嵌控件。
- 没有广告标记的普通“关闭”、返回按钮或叉号。
- 只有系统 `ACTION_DISMISS`、没有明确关闭控件的原生 Dialog。
- 仅因为屏幕其他位置出现“会员”“福利”“促销”等商业文字的弹窗。
- 净屏自己的页面。服务会跳过自己的包名，避免把“今日自动跳过”等页面文字当成广告控件。

服务只执行实际的 `ACTION_CLICK`，不再使用通用 `ACTION_DISMISS` 作为兜底关闭动作。为了避免误关业务弹窗，个别没有可访问广告标记的广告可能不会被处理，这是有意的安全取舍。

### 3.3 关闭条件

以下任一条件成立时，服务不会执行广告动作：

- 总开关关闭。
- 目标应用规则关闭自动跳过。
- 目标应用加入白名单。
- 无障碍服务没有被系统授权或没有完成绑定。
- 控件不可见、禁用、没有可点击父节点或没有有效屏幕位置。

## 4. Android 和 vivo 权限

### 4.1 用户必须手动授权

普通第三方应用不能静默打开无障碍，也不能通过代码修改系统安全设置。首次使用需要：

1. 在净屏权限页点击“自动跳过权限”。
2. 进入系统无障碍设置，手动打开净屏服务。
3. 在系统确认框中点击允许。
4. 返回净屏，确认状态从“启动中”变为“已开启”。

### 4.2 Android 受限设置

通过 USB、本地文件或其他旁加载方式安装时，Android 可能限制敏感无障碍服务。遇到服务开关无法保持开启时：

1. 打开“应用信息”。
2. 打开右上角菜单中的“允许受限设置”（如果系统提供）。
3. 返回无障碍设置重新开启净屏。
4. 如果系统没有这个入口，使用可信应用商店或 vivo 官方互传重新安装。

### 4.3 vivo/iQOO 后台保护

如果 vivo 返回设置列表后把服务改成“已关闭”，这通常是系统的自启动或后台限制，不是应用身份字段问题。请在系统设置中：

- 允许净屏自启动；
- 允许后台运行或后台耗电；
- 关闭对净屏的电池优化/智能冻结；
- 必要时在最近任务中锁定净屏。

应用只能引导用户打开这些页面，不能代替用户修改厂商开关。

## 5. APK 构建与安装约定

### 5.1 固定安装方式：网页版互传

以后普通 APK 安装统一使用“互传”的网页版流程，不使用 USB/ADB 直接安装：

1. 在电脑上生成 APK。
2. 在电脑打开官方网页版“互传”。
3. 通过网页版把 APK 发送到自己的手机。
4. 在手机端通过“互传”接收文件。
5. 在手机端从互传收到的文件中点击安装。

升级已有净屏时使用升级安装，不要先卸载旧版，也不要清除应用数据。必须保持包名 `com.clearscreen.prototype` 和同一 Release 签名，这样才能保留无障碍授权、应用规则、白名单和记录。

### 5.2 构建命令

在项目根目录执行：

```powershell
npx tsc --noEmit
cd android
.\gradlew.bat :app:compileDebugKotlin
```

生成手机安装包时使用 Release：

```powershell
.\gradlew.bat :app:assembleRelease
```

正式 Release 需要本机已有签名配置。首次准备签名或生成授权诊断包时，可使用：

```powershell
powershell -ExecutionPolicy Bypass -File .\tools\New-ClearScreenSigning.ps1
powershell -ExecutionPolicy Bypass -File .\tools\Build-AccessibilityDiagnostics.ps1
```

`assembleDebug` 主要用于连接 Metro 调试，不作为手机日常安装包。没有启动 8081 服务时，Debug 包可能显示 `Unable to load script`。

### 5.3 安装后检查

- 确认应用名称显示为“净屏”。
- 确认无障碍列表中出现净屏服务。
- 确认开启总开关后，自己的 VPN 连接没有被断开或替换。
- 确认系统 VPN 页面中没有出现净屏 VPN。
- 在目标应用中分别测试明确“跳过广告”、广告关闭叉号和普通业务弹窗。
- 记录页只显示真实无障碍处理结果，不把网络过滤当作有效功能。

## 6. 诊断和验证边界

验证结果必须区分：

- TypeScript 编译通过：只证明前端类型正确。
- Kotlin 编译通过：只证明 Android 原生代码可以编译。
- APK 成功安装：只证明安装流程可用。
- 无障碍服务在设置页显示已开启：只证明系统授权状态存在。
- 真机实际点击和弹窗保持不变：才证明识别策略符合预期。

当前推荐的最小验证命令：

```powershell
npx tsc --noEmit
cd android
.\gradlew.bat :app:compileDebugKotlin
```

无障碍授权诊断资料见 [ACCESSIBILITY_PERSISTENCE_REPORT.md](../../ACCESSIBILITY_PERSISTENCE_REPORT.md)。该报告用于记录安装来源、授权列表和 vivo 行为，不代表普通 APK 可以绕过系统授权。

## 7. 发布检查清单

- [ ] `app.json` 和 `android/app/build.gradle` 的版本号、`versionCode` 已同步。
- [ ] 包名仍是 `com.clearscreen.prototype`。
- [ ] 使用原 Release 签名，没有更换证书。
- [ ] `npx tsc --noEmit` 通过。
- [ ] `:app:compileDebugKotlin` 通过。
- [ ] Release APK 通过网页版互传传到手机，并在手机端用互传安装。
- [ ] 没有卸载旧版、没有清除应用数据。
- [ ] vivo/iQOO 真机重新检查无障碍、自启动和后台运行。
- [ ] 确认净屏不申请、不启动、不接管 VPN。
- [ ] 用至少一个广告页面和一个必须人工处理的业务弹窗做回归测试。

## 8. 变更记录

项目逐次修改的时间线见[净屏修改记录](CHANGELOG.md)。本节保留概要，细节以修改记录为准。

### 2026-09-22

- 收紧广告弹窗识别：普通 `ACTION_DISMISS` 不再作为关闭兜底。
- 普通关闭按钮不再使用全屏商业文字作为广告依据。
- 增加整页遮罩广告规则：要求广告强标记、营销词簇、较大遮罩/广告面板和右上角紧凑关闭控件共同成立，兼容无文字的关闭图标并保持普通业务弹窗保护。
- 增强视频广告“跳过”识别：分离文字节点和可点击父节点的几何判断，并增加动态加载短时重试与 `builtin.skip.video-ad.edge` 诊断规则。
- 移除 ClearScreen 的 VPN Service、VPN 权限和网络过滤入口，避免抢占用户自己的 VPN。
- 固定 APK 通过网页版互传发送到手机，再由手机端互传安装。

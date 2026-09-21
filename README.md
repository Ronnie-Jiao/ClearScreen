# 净屏

高保真 React Native + Expo 前端原型，当前以 **Android** 为目标平台。仓库同时保留早期 Hatchable Web 原型在 `public/` 目录。

## 应用命名与打包约定

- 对外应用名称、Android 桌面名称和安装包显示名称统一为 **净屏**。
- `ClearScreen` 仅作为英文品牌副标题、代码标识和 Android `applicationId` 使用，不作为用户看到的应用名称。
- 后续打包或重新生成 Android 原生工程时，必须保持 `app.json` 的 Expo 应用名和 `android/app/src/main/res/values/strings.xml` 中的 `app_name` 为 `净屏`。
- Android `applicationId` `com.clearscreen.prototype` 是技术标识，保持不变。

## 升级与数据保留约定

- 更新安装包时使用“升级安装”，不要先卸载旧版，也不要执行清除应用数据。
- 必须保持 `com.clearscreen.prototype` 包名不变，并使用同一发布签名；否则 Android 会把它视为不同应用，旧数据无法直接衔接。
- 每次发布更新都递增 `android/app/build.gradle` 的 `versionCode`，同时同步 `app.json` 的 `android.versionCode` 和版本号。
- 本地设置、应用规则、白名单和拦截记录保存在 `clearscreen_backend` 中；启动时只做兼容迁移，不调用清空或覆盖旧存储。
- 发布签名文件属于本机敏感文件，不提交 Git；更换打包机器前要安全迁移同一签名文件。
- 欢迎页与权限引导页只在首次安装时显示；完成“开始使用”或选择“稍后”后会记录状态，后续启动和升级安装直接进入首页。

## 当前状态

- Android 前端高保真 UI 已实现
- iOS / HarmonyOS 暂不开发
- 不包含账号、会员、订阅或支付
- AccessibilityService、VpnService 等 Android 系统层能力已接入本地后端；首次使用仍需用户在系统设置中授权
- 无障碍规则默认覆盖新发现的应用：会优先识别倒计时“跳过/3跳过”等控件；广告弹窗同时出现广告标记与关闭控件时也会尝试处理；无法确认是广告的普通关闭按钮不会自动点击
- vivo 等系统如果在返回设置列表后仍把服务改回“已关闭”，还需要在系统设置中允许净屏自启动、后台运行并关闭电池优化；这属于系统对无障碍服务的管控，应用本身不能绕过系统授权。
- Android 16 对 ADB/本地侧载应用的无障碍可能启用“受限设置”；净屏会读取安装来源并先引导用户尝试系统无障碍页。如果 vivo 没有提供“应用信息 → 允许受限设置”入口，则必须通过 vivo EasyShare 或可信应用商店重新安装，应用自身不能绕过系统校验。
- 无障碍实现参考了李跳跳 APK 的服务声明形态：服务不再单独运行在 `:accessibility` 进程，使用单进程、`exported=false`、`settingsActivity`、通用反馈类型和 Android 12+ 的 `isAccessibilityTool` 配置；这不会伪造或绕过系统授权。
- 为便于与李跳跳的 `targetSdk=33` 做真机 A/B 测试，支持 `cd android; .\\gradlew.bat :app:assembleRelease -PclearscreenTargetSdk=33`。手机单独安装必须使用 `assembleRelease` 生成的 APK；`assembleDebug` 仅用于连接 Metro 调试，未启动 8081 服务时会显示 `Unable to load script`。未传该参数时仍使用当前 Android/Expo 目标 SDK；Android 16 的受限设置最终仍由系统根据安装来源决定。

## 无障碍授权诊断构建

当前仓库提供独立的授权诊断分支构建，不把未经真机验证的身份实验混入普通发布包：

1. 首次在本机生成固定 Release 签名：`powershell -ExecutionPolicy Bypass -File .\\tools\\New-ClearScreenSigning.ps1`。`android/keystore.properties` 与 `android/clearscreen-release.keystore` 已加入忽略列表，不能提交到 Git；正式更新必须继续使用同一份证书。
2. 构建正式签名的诊断包、兼容包和极简探针：`powershell -ExecutionPolicy Bypass -File .\\tools\\Build-AccessibilityDiagnostics.ps1`。
3. 诊断包默认使用新的 `com.clearscreen.app` 包名，并移除 VPN、悬浮窗、后台和旧存储等可选能力；极简探针包为 `com.clearscreen.probe`，只有 Activity 和一个空的无障碍服务。
4. 在手机上完成一次“打开 → 返回列表”的复现后，运行 `powershell -ExecutionPolicy Bypass -File .\\tools\\Collect-AccessibilityDiagnostics.ps1 -PackageName com.clearscreen.app -ClearLogcat`，脚本会保存安装来源、首次安装时间、包信息、无障碍列表和 logcat。
5. 用 `powershell -ExecutionPolicy Bypass -File .\\tools\\Compare-AccessibilityPackages.ps1` 对比净屏与李跳跳的安装历史和来源字段。

普通 Release 构建现在要求本机存在正式签名配置；仅用于临时开发的 Debug 签名需要显式传 `-PallowDebugSigning=true`。`lttCompat` 构建只保留李跳跳风格的 `isAccessibilityTool=true` 变体，正式配置不再把该语义字段当作授权保活开关。

## 已实现页面

1. 启动页
2. 首次使用 / 欢迎页
3. 权限开启页
4. 首页
5. 应用列表
6. 应用详情
7. 记录
8. 设置
9. 外观
10. 白名单

## 已实现交互

- 启动页自动进入欢迎页
- 首页 / 应用 / 设置三 Tab 切换
- 总开关
- 权限状态开关
- App 自动跳过开关
- App 详情自动跳过 / 网络过滤 / 白名单
- 应用搜索与筛选
- 记录筛选
- 设置开关
- 外观主题选择和动效开关
- 白名单筛选、启用与保存
- 返回历史栈

## 运行

需要 Node.js、npm 和 Expo 环境。

```bash
npm install
npm run start
```

Android：

```bash
npm run android
```

## 目录

```text
ClearScreen/
├─ App.tsx
├─ src/
│  ├─ components/
│  ├─ screens/
│  ├─ assets.ts       # 内嵌 Logo / App 图标，便于 GitHub 单仓库同步
│  ├─ data.ts
│  └─ theme.ts
├─ public/             # 早期 Hatchable Web 交互原型
├─ package.json
├─ app.json
└─ tsconfig.json
```

## 视觉

- 主色：`#0B78F2`
- 青色：`#14B8B0`
- 深蓝文字：`#092868`
- 浅蓝背景：`#F4FCFF`
- 大圆角白色卡片、蓝青渐变、柔和阴影

页面按已确认的高保真 UI 图进行布局还原，使用响应式尺寸适配常见 Android 手机纵向屏幕。

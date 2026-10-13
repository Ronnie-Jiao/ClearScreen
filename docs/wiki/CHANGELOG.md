# 净屏修改记录

本文件按时间倒序记录项目修改，供后续 AI 和开发者开始任务前快速了解最近变化。自本记录建立起，新增条目的时间使用中国标准时间（UTC+08:00）；既有条目的时间按文件系统记录的本地最后修改时间整理。

## 2026-10-13

### 2026-10-13 16:50:29 +08:00 — 完成纯 RNOH 共享界面隔离试验的打包验证

- 新建 `targets/harmony-bare-pilot/`，以 RNOH 官方 `0.84-main` 模板为工程骨架（上游提交 `f1ed468c5427ca92fcd4d0b79c0073cd3fad3b11`；React Native/RNOH `0.84.1`），并保留 MIT 许可文本与来源说明。
- 鸿蒙试验直接导入现有首页、应用页、设置页、主题、数据和资源；通过试验专属的纯色渐变替代层避免把 Expo 原生模块带入。页面交互只在内存中变化，不接入 Android/iOS 主工程数据。
- 将主工程 TypeScript 检查排除新的 Harmony 工程，更新项目 README/Wiki 和旧 Expo 试验说明。明确鸿蒙无障碍、后台、应用枚举、隐私存储、广告处理尚未实现；尚无可安装 HAP。
- 验证：试验 `npm run typecheck` 通过；`npm test -- --runInBand` 1 项通过；Harmony Metro bundle 成功（970,803 字节）并导出 13 个资源文件（402,037 字节）；主工程 `npx tsc --noEmit` 通过；`git diff --check` 通过（仅有仓库既有 LF/CRLF 提示）。
- 安全审计：`npm audit --omit=dev` 报告 8 项中危、25 项高危、0 项严重；试验依赖未做升级或安全审查，不得作为发布依赖。
- 未执行 HAP 构建、模拟器/真机运行或签名验证：本机缺少 DevEco Studio、ohpm、hvigorw、hdc 与 HarmonyOS 设备/模拟器环境。
- 涉及文件：`targets/harmony-bare-pilot/`、`targets/harmony-pilot/README.md`、`tsconfig.json`、`README.md`、`docs/wiki/README.md`、`docs/wiki/CHANGELOG.md`。

### 2026-10-13 16:25:35 +08:00 — 建立 HarmonyOS 隔离复用试验并记录工具链阻塞

- 在 `targets/harmony-pilot/` 生成独立 Expo SDK 55 / React Native 0.83.6 / RNOH 0.84.1 工程骨架，并生成 RNOH 注册文件；当前 EntryAbility 仍是静态 ArkUI 壳页，未接入 React Native 宿主或净屏共享页面。若近期找不到兼容的 Expo/RNOH 组合，建议另开纯 React Native/RNOH 隔离试验，不强行改主工程依赖。
- 依赖安装未通过：npm peer 检查发现 RNOH 0.84.1 要求 React Native 0.84.1，与脚手架声明的 0.83.6 冲突。未使用 `--force` 或 `--legacy-peer-deps` 绕过。现有净屏主工程是 Expo 54 / React Native 0.81.5；未改动 Android/iOS 依赖。
- `expo-harmony doctor` 报告配置骨架存在，但退出码为失败；`expo-harmony env` 显示本机缺少 DevEco Studio、ohpm、hvigorw 和 hdc。没有构建 HAP、运行模拟器或安装真机。
- 为隔离原生 Hvigor 文件，主工程 `tsconfig.json` 排除 `targets/harmony-pilot`；README/Wiki 说明当前状态和继续条件。
- 验证：`expo-harmony create` 和 `prebuild --host-template rnoh` 完成；`npm add expo-linear-gradient@~15.0.0 @expo-harmony/expo-linear-gradient` 因 npm peer 版本冲突失败；`expo-harmony doctor` 1 个失败项；`expo-harmony env` 1 个失败项。主工程 `npx tsc --noEmit` 首次被试验 Hvigor 文件纳入而失败，增加排除项后复验通过；`prebuild --check` 无差异，`git diff --check` 通过（仅有仓库既有的 LF/CRLF 提示）；HAP/真机验证未执行。
- 涉及文件：`targets/harmony-pilot/`、`tsconfig.json`、`README.md`、`docs/wiki/README.md`、`docs/wiki/CHANGELOG.md`。

### 2026-10-13 15:43:10 +08:00 — 明确 iOS 系统网址过滤的开发状态

- iOS 首页新增系统广告过滤状态卡片：iOS 26 以下显示“需要 iOS 26”，iOS 26 及以上显示“尚未接入”；没有增加虚假的开启开关或处理次数。保留原 Safari Web Extension 入口。
- 新增 iOS URL Filter 版本状态判断；同步更新 README、Wiki 和隐私政策，明确 Apple URL Filter 是基于网址数据拦截部分网络资源，不会识别屏幕或点击广告按钮，并列出 Bloom/PIR、控制扩展、CloudKit 注册、Network Extension entitlement/签名和 OHTTP Relay 资格等未完成前置条件。
- 验证：`npx tsc --noEmit` 通过；`npm run test:safari` 6 项通过；`npx expo config --type prebuild --json` 通过；`git diff --check` 通过（仅有仓库既有的 LF/CRLF 提示）。未生成或构建 iOS 原生工程：当前环境为 Windows，且没有 Apple Team ID、URL Filter PIR 服务及相应 Apple 配置；未做 Xcode 签名或 iPhone 真机测试。
- 涉及文件：`src/platform/ios/urlFilter.ts`、`src/screens/IOSSafariScreen.tsx`、`README.md`、`docs/wiki/README.md`、`docs/wiki/隐私政策.md`、`docs/wiki/CHANGELOG.md`。

### 2026-10-13 15:18:00 +08:00 — 增加共用界面的 iOS Safari 网页广告处理入口

- iOS 主应用复用现有 React Native 组件，并单独显示 Safari 扩展引导与 iOS 隐私说明；Android 仍使用原有页面和原生无障碍流程。
- 新增 Safari Web Extension：用户启用扩展、授予网站访问权限并手动打开过滤后，才在 Safari 网页内识别明确的广告跳过/关闭控件；普通关闭、导航、取消、下载和购买不作为处理目标。同一广告区域只处理一次。默认关闭，不上传或保存网页文字、网址和识别结果。
- 补充 iOS 本地隐私说明、README/Wiki 的平台范围及限制；说明图片/Canvas、嵌入框架和缺少明确按钮的广告可能无法处理，并记录公开隐私政策网页仍须补充 iOS 说明后才能用于商店提交。扩展开关页遵守项目文案约束，不增加按钮旁的功能说明小字。
- 验证：`npm run test:safari` 通过（6 项）；`npx tsc --noEmit`、扩展脚本 `node --check`、`npx expo config --type prebuild --json` 和 `git diff --check` 通过。iOS 原生工程未能生成/构建：当前环境为 Windows，且尚未提供 Apple Team ID；未进行 Xcode 签名、iPhone 安装或 Safari 真机验证。
- 涉及文件：`App.tsx`、`app.json`、`package.json`、`package-lock.json`、`README.md`、`src/screens/IOSSafariScreen.tsx`、`src/screens/PrivacyPolicyScreen.tsx`、`targets/safari/expo-target.config.json`、`targets/safari/SafariWebExtensionHandler.swift`、`targets/safari/assets/manifest.json`、`targets/safari/assets/ad-filter-core.js`、`targets/safari/assets/content.js`、`targets/safari/assets/popup.html`、`targets/safari/assets/popup.css`、`targets/safari/assets/popup.js`、`tests/safari-ad-filter.test.cjs`、`docs/wiki/README.md`、`docs/wiki/隐私政策.md`、`docs/wiki/CHANGELOG.md`。

### 2026-10-13 12:38:00 +08:00 — 补齐净屏隐私同意、公开政策页与商店合规材料

- Android 1.0.28 首次启动先展示版本化隐私政策；同意前不读取启动器应用列表、系统无障碍状态、旧规则/记录或其他应用界面。无障碍服务每次处理前检查本地同意记录；用户撤回时停止界面处理并清除本机规则、记录和设置，不替用户切换系统无障碍授权。设置增加政策重看与撤回入口。
- 正式版移除互联网、网络状态、外部存储、悬浮窗、振动和忽略电池优化权限，关闭系统备份；无障碍服务声明不请求手势能力。正式 Release 版本为 `1.0.28` / `versionCode=34`。
- 按 Google ML Kit 官方披露补充 SDK 可能联系 Google 获取更新/兼容信息及发送性能、使用指标的说明；不再声称“无 INTERNET 权限即可保证 SDK 不会联网”。同步更正无障碍进程说明：当前 Android 清单未设置独立进程。新增小米/荣耀/vivo/OPPO 合规核查与待办；明确平台预审、企业认证、APP 备案和未成年人策略尚未完成。
- 依负责人确认的公司名称与邮箱，创建并公开发布[净屏隐私政策](https://clearscreen-privacy-policy.buzzyorbit64.chatgpt.site)，网页仓库提交 `a2db22f5bb23fe0ddb476de94c031795502b443a`，Sites 版本 1。公开页面 HTTP 返回 200，核对标题、公司主体和邮箱可访问。
- 验证：`npx tsc --noEmit`、`:app:testReleaseUnitTest`、`:app:assembleRelease`、`git diff --check` 通过；`aapt dump permissions` 检查最终 APK 仅列出应用自身动态接收器标记权限，没有平台 `uses-permission`；`apksigner verify --print-certs` 验证签名通过。APK SHA-256：`33DAF306898C6477FDEB5CDDC68F4E4A23D1508730C2D99803FBE6F440B39E95`。
- 涉及文件：`App.tsx`、`README.md`、`app.json`、`android/app/build.gradle`、`android/app/src/debug/AndroidManifest.xml`、`android/app/src/debugOptimized/AndroidManifest.xml`、`android/app/src/main/AndroidManifest.xml`、`android/app/src/main/java/com/clearscreen/prototype/MainApplication.kt`、`android/app/src/main/java/com/clearscreen/prototype/backend/ClearScreenAccessibilityService.kt`、`ClearScreenNativeModule.kt`、`ClearScreenPrivacyConsentStore.kt`、`ClearScreenStore.kt`、`android/app/src/main/res/xml*/accessibility_service_config.xml`、`android/app/src/lttCompat/res/xml-v31/accessibility_service_config.xml`、`src/native/ClearScreenNative.ts`、`src/privacy.ts`、`src/screens/PrivacyPolicyScreen.tsx`、`src/screens/SettingsScreen.tsx`、`docs/wiki/README.md`、`docs/wiki/隐私政策.md`、`docs/wiki/应用商店合规核查.md`、`docs/wiki/CHANGELOG.md`；公开政策网页源文件位于独立仓库 `C:\develop\ClearScreenPrivacyPolicy`。
- 未向小米、荣耀、vivo 或 OPPO 提交应用；未在手机上安装/实测首启同意流程。商店控制台、备案和平台分类/预审仍待负责人办理或确认。

### 2026-10-13 10:02:14 +08:00 — 通过 USB 升级安装净屏 1.0.27

- 使用 Release APK 对 vivo V2502DA（`10AG4M2KGF00527`）执行 `adb install -r`，安装命令返回 `Success`；未卸载应用或主动清除应用数据。
- 验证：`com.clearscreen.prototype` 为 `versionCode=33` / `versionName=1.0.27`，`lastUpdateTime=2026-10-13 10:00:15`，`pm path` 返回已安装 APK 路径；安装包 SHA-256 为 `BA43C0E75B8B34F8A5B5643DE82F8D842C7E658286B3DE487CA2306DF5242501`。未检查无障碍授权状态或应用内广告处理效果。
- 涉及文件：`docs/wiki/CHANGELOG.md`；已安装设备：vivo V2502DA。

## 2026-10-12

### 2026-10-12 19:20:22 +08:00 — 按新版页面规划收敛 Android 界面

- 将 Android 底部导航保持为“首页 / 应用 / 设置”三项，记录改为从首页和应用详情进入的二级页；更新首页状态操作、应用/排除应用的一致开关、记录详情和设置页对应的 README/Wiki 页面说明。
- 未保存规则的新应用默认关闭，避免首次加载就默认启用广告处理；应用规则保存失败时重新读取原生状态恢复界面并提示重试。清空记录只保留一处确认，并明确告知清空后无法恢复。
- 欢迎页移除功能宣传卡片和“不占用 VPN”宣传文案；隐藏未接入的外观页面路由，避免展示非当前功能。
- 验证：`npx tsc --noEmit`、`:app:testReleaseUnitTest`、`:app:compileReleaseKotlin`、`:app:assembleRelease`、`git diff --check` 均通过。未安装到手机，未做真机页面回归。
- 涉及文件：`App.tsx`、`src/screens/SettingsScreen.tsx`、`src/screens/WelcomeScreen.tsx`、`android/app/src/main/java/com/clearscreen/prototype/backend/ClearScreenStore.kt`、`README.md`、`docs/wiki/README.md`、`docs/wiki/CHANGELOG.md`。

### 2026-10-12 17:44:18 +08:00 — 按当前界面代码修订 Notion 页面说明

- 将 Notion「02｜页面结构与交互」改为当前 ClearScreen Android 1.0.27 页面实装说明：记录 4 个底部入口、启动与权限页、首页、应用及详情、记录、设置、白名单和外观页面；移除旧规划中不符合当前页面的三入口、VPN 授权及网络过滤界面描述。
- 标出当前未接通的入口和外观控件，并记录设置页“30 天”与记录页“7 天”的文案冲突；加入不添加功能描述性小字的界面文案约束。
- 验证：Notion 保存后重新读取，确认更新后的页面结构、版本号、文案冲突和文案约束均已写入；对照 `App.tsx`、`src/screens/*`、`src/components/BottomNav.tsx` 与 Android 版本号。此次仅更新文档，未运行构建/测试，未进行手机逐页实测。
- 涉及文件：Notion 页面 https://app.notion.com/p/3e164df90ef18133900ac29293af4100；`docs/wiki/CHANGELOG.md`。代码文件仅用于核对，未修改。

### 2026-10-12 17:25:17 +08:00 — 完成 vivo 安装 ClearScreen 1.0.27

- 使用 Release APK 通过 USB 对 vivo V2502DA（`10AG4M2KGF00527`）执行 `adb install -r`，安装过程返回 `Success`；未卸载应用或主动清除应用数据。
- 验证：设备仍在线，`com.clearscreen.prototype` 当前为 `versionCode=33` / `versionName=1.0.27`，`lastUpdateTime=2026-10-12 17:25:07`，`pm path` 返回已安装 APK 路径；安装包 SHA-256 为 `2A6153FF2C6BC95712AD31E7EEBD45728ACE390921C6A2D65FB7445DDE02473A`。未运行测试或进行应用内界面回归。
- 涉及文件：`docs/wiki/CHANGELOG.md`；已安装设备：vivo V2502DA。

## 2026-10-12

### 2026-10-12 17:12:40 +08:00 — 调整首页、应用、记录与设置的功能布局

- 底部导航增加“记录”；首页集中显示服务状态和自动处理范围，并可直接切换分类；应用页的“白名单”筛选改为实际筛选，白名单管理设为独立入口；设置页按权限服务与常用设置重新排列；白名单管理页将即时保存后的操作收口为“完成”。
- 同步更新 README 与 Wiki 的页面职责和入口说明；版本更新为 `versionCode 33` / `versionName 1.0.27`。
- 验证：Release APK 构建产物包名为 `com.clearscreen.prototype`、版本为 `1.0.27`；`apksigner verify` 通过，APK SHA-256 为 `2A6153FF2C6BC95712AD31E7EEBD45728ACE390921C6A2D65FB7445DDE02473A`；Xiaomi 22101317C 已安装并核实为 `versionCode=33` / `versionName=1.0.27`。vivo V2502DA 的安装返回 `INSTALL_FAILED_ABORTED: User rejected permissions`，当前仍是 `1.0.26`，未重试。未运行测试，未做真机界面回归。
- 涉及文件：`src/components/BottomNav.tsx`、`src/components/UI.tsx`、`src/screens/HomeScreen.tsx`、`src/screens/AppsScreen.tsx`、`src/screens/RecordsScreen.tsx`、`src/screens/SettingsScreen.tsx`、`src/screens/WhitelistScreen.tsx`、`App.tsx`、`README.md`、`docs/wiki/README.md`、`app.json`、`android/app/build.gradle`、`docs/wiki/CHANGELOG.md`。

### 2026-10-12 16:42:00 +08:00 — 通过 USB 升级安装至 vivo 与小米手机

- 使用 `adb install -r` 将正式 Release APK 升级安装到 vivo V2502DA（`10AG4M2KGF00527`）和 Xiaomi 22101317C（`2f8898c80822`）；未卸载旧版或清除应用数据。
- 验证：两台设备均安装 `com.clearscreen.prototype`、`versionCode=32`、`versionName=1.0.26`，`pm path` 均返回 APK 路径；安装包 SHA-256 为 `76C522682A3820A9149369DF8A013355D54D61C55489EC2599C64928D2EEDC70`。未重新构建，未在设备上回归应用功能或检查无障碍授权状态。
- 涉及文件：`docs/wiki/CHANGELOG.md`；已安装设备：vivo V2502DA、Xiaomi 22101317C。

### 2026-10-12 16:29:47 +08:00 — 建立界面说明小字约束并清理现有文案

- 在项目 Wiki 中规定界面不保留功能、用途、效果、条件或下一步操作的说明性小字，并将约束同步到仓库协作指引；删除原生界面与保留的旧版原型中的冗余说明，保留必要的状态、数据及权限/安全提示。
- 验证：`git diff --check` 通过；针对本次清理的旧说明文案扫描无匹配。未运行测试或构建。
- 涉及文件：`AGENTS.md`、`App.tsx`、`docs/wiki/README.md`、`docs/wiki/CHANGELOG.md`、`public/app.js`、`src/screens/AppDetailScreen.tsx`、`src/screens/HomeScreen.tsx`、`src/screens/PermissionsScreen.tsx`、`src/screens/SettingsScreen.tsx`。

### 2026-10-12 16:01:06 +08:00 — 增加跨应用开屏与局部弹窗 OCR 识别兜底

- 不增加滴滴、京东、淘宝、抖音等应用专属规则。普通开屏“跳过/倒计时”现在还需位于顶部右侧、控件紧凑、同屏有大面积图片/视频创意，并满足广告标记或多项促销线索及行动文案；截图必须新鲜且与当前无障碍树一致。
- 弹窗没有标准 popup/dialog 控件名时，允许从不属于列表滚动区的有边界局部容器取 OCR 证据；要求同一区域有广告标记+行动文案，或至少三项促销线索+行动文案，且叉号/关闭控件是独立的小型控件并贴近容器上边或侧边。支持分行 OCR 文案组合；仍只执行真实无障碍控件的点击，不使用坐标，不点下载、购买或领取等广告内容。
- 补充通用优惠券、福利、促销和“去使用/戳我下载”等文案识别，并增加开屏与弹窗正反例策略单测。版本更新为 `versionCode 32` / `versionName 1.0.26`，同步更新 README 与 Wiki。
- 验证：`:app:testReleaseUnitTest` 共 10 项通过；`npx tsc --noEmit` 通过；`:app:assembleRelease` 成功；APK 包名 `com.clearscreen.prototype`、版本 `1.0.26`、应用名“净屏”；`apksigner verify` 通过，v2 签名有效且发布证书 SHA-256 为 `93c410454a8263736ff60bb6e27153b19f74f4c934f444531842f5162ccfa999`；APK SHA-256 为 `76C522682A3820A9149369DF8A013355D54D61C55489EC2599C64928D2EEDC70`；本条写入后 `git diff --check` 通过。未安装到手机，未对截图中的实际页面执行真机识别/误触回归；因此不宣称这些具体广告已能关闭。
- 涉及文件：`android/app/src/main/java/com/clearscreen/prototype/backend/ClearScreenAccessibilityService.kt`、`android/app/src/main/java/com/clearscreen/prototype/backend/AdDecisionPolicy.kt`、`android/app/src/test/java/com/clearscreen/prototype/backend/AdDecisionPolicyTest.kt`、`android/app/build.gradle`、`app.json`、`README.md`、`docs/wiki/README.md`、`docs/wiki/CHANGELOG.md`。

## 2026-10-10

### 2026-10-10 15:35:19 +08:00 — 记录正式签名证书并备份项目签名材料

- 新增 Wiki 页面记录正式证书公钥 PEM、证书 MD5/SHA-1/SHA-256 指纹及公钥自身的 MD5/SHA-1/SHA-256 摘要，并明确区分“证书指纹”和“公钥指纹”；在 Wiki 入口增加链接。Wiki 不保存私钥或密码。
- 按用户指定位置，在百度同步盘的 `系统秘钥\ClearScreen` 下备份正式 Release 密钥库、Gradle 签名密码配置、签名 lineage 和 Debug 密钥库；附项目密钥清单。仓库内原文件保留不动。
- 验证：从 Release 密钥库导出的证书信息与现有 Release APK 的签名者 MD5、SHA-1、SHA-256 一致；`apksigner verify` 通过；备份的 4 个文件均与源文件 SHA-256 一致；Wiki 公钥 PEM 可解析为 4096 位 RSA 公钥，公钥 MD5/SHA-1/SHA-256 与页面记录一致；`git diff --check` 通过。未重新构建 APK。
- 涉及文件：`docs/wiki/签名证书.md`、`docs/wiki/README.md`、`docs/wiki/CHANGELOG.md`；外部备份 `C:\Users\22567\Documents\BaiduSyncdisk\其它\系统秘钥\ClearScreen\`（`ClearScreen-密钥清单.md`、`android/clearscreen-release.keystore`、`android/keystore.properties`、`android/clearscreen-signing-lineage.bin`、`android/app/debug.keystore`）。

### 2026-10-10 09:59:07 +08:00 — 通过 USB 升级小米真机并核对权限状态

- 确认第二台设备序列号 `2f8898c80822` 的厂商为 Xiaomi、型号 `22101317C`，安装前净屏为 `com.clearscreen.prototype` / `versionCode=30` / `versionName=1.0.24`。使用同一份已核对的 Release APK 执行 `adb install -r`，返回 `Success`；安装后系统显示 `versionCode=31` / `versionName=1.0.25`，`pm path` 可找到包文件。
- 验证：小米系统的已启用无障碍服务列表及绑定列表均不含净屏，需用户在手机上手动授权；未替用户更改系统权限，也未执行广告识别真机正负例回归。未重新构建 APK。
- 涉及文件：`docs/wiki/CHANGELOG.md`（仅记录本次安装和验证结果）。

### 2026-10-10 09:57:25 +08:00 — 改用 USB 数据线默认安装并升级 vivo 真机

- 应用户要求，废止“普通 APK 必须通过网页版互传安装”的固定规则。Wiki 改为默认核对设备序列号后使用 USB/ADB `install -r` 升级；保留受限设置情况下可信应用商店或厂商互传的排障备选，不改写旧历史记录。根 README 的 Wiki 入口同步更新。
- 使用现有 Release APK `android/app/build/outputs/apk/release/app-release.apk`（SHA-256 `B4C13C82EDD154A093CD14ED08DC9EB6B8C0E7F38959688872D489E28E92B263`）对 vivo `10AG4M2KGF00527` 执行 `adb install -r`，返回 `Success`；系统确认包名 `com.clearscreen.prototype`、`versionCode=31`、`versionName=1.0.25`，无障碍服务在系统列表中仍已启用并绑定。小米尚未出现在 `adb devices -l`，未执行安装。
- 验证：文档 `git diff --check` 通过；未重新构建 APK，未执行广告识别真机正负例回归，不能仅凭安装证明识别效果。
- 涉及文件：`docs/wiki/README.md`、`README.md`、`docs/wiki/CHANGELOG.md`。

## 2026-10-09

### 2026-10-09 22:24:44 +08:00 — 扩展视频与内嵌广告识别并收紧误点门槛

- 针对播放器前贴片/中插的延迟“跳过”，在首次界面变化后的短重扫之外，检测到播放器进入或明确视频广告线索时增加最长 45 秒的有限时监测；每次仍只检查可见、启用、尺寸受限的真实跳过节点，不能跳过没有可用按钮的强制广告。预筛 OCR 跳过/X 文字，减少监测扫描工作量。
- 新增同一无障碍子树内的广告徽标与直接可点击小型关闭节点配对，可覆盖符合结构的暂停广告和页面内嵌广告卡片；普通 X 须在卡片右上边缘，底部居中仅接受广告专用关闭语义。为节点记录树路径并按真实父子路径核对，避免同屏相邻卡片混用证据。
- 普通“跳过”、单独的 `jump`/`skip` 资源 ID、无文字图片图标及过大的父容器不再单独触发；收紧弹窗目标尺寸和广告浮层归属，并去除可指向整个广告容器的宽泛关闭 ID。彩云天气两条窄规则只接受关闭节点自身可点击。系统权限/安装界面以及包含外部应用启动确认文案的窗口拒绝广告动作；动作后前台切换只记录“结果未确认”，不记为关闭成功。
- 版本递增到 `versionCode 31` / `versionName 1.0.25`；同步 README 与 Wiki。增加可独立运行的正反例策略测试，涵盖系统确认、跨卡片证据、普通倒计时、无文字图片图标及内嵌广告按钮位置/尺寸。
- 验证：`:app:testReleaseUnitTest` 8 个测试通过，`:app:assembleRelease`、`npx tsc --noEmit`、`git diff --check` 通过；APK 包名 `com.clearscreen.prototype`，桌面名“净屏”，版本 1.0.25，v2 签名有效且发布证书 SHA-256 仍为 `93c410454a8263736ff60bb6e27153b19f74f4c934f444531842f5162ccfa999`。APK SHA-256：`B4C13C82EDD154A093CD14ED08DC9EB6B8C0E7F38959688872D489E28E92B263`。`adb devices -l` 未检测到设备，因此未安装到手机，影视 App 前贴片/暂停广告和相似业务页面的真机正负例回归未执行；不能据此宣称零误点或全覆盖。
- 涉及文件：`android/app/src/main/java/com/clearscreen/prototype/backend/ClearScreenAccessibilityService.kt`、`android/app/src/main/java/com/clearscreen/prototype/backend/AdDecisionPolicy.kt`、`android/app/src/test/java/com/clearscreen/prototype/backend/AdDecisionPolicyTest.kt`、`android/app/build.gradle`、`app.json`、`README.md`、`docs/wiki/README.md`、`docs/wiki/CHANGELOG.md`、`android/app/build/outputs/apk/release/app-release.apk`。

### 2026-10-09 10:07:26 +08:00 — 增加彩云天气 SDK 插屏底部关闭按钮窄规则

- 根据手机当前无障碍树，针对彩云天气 `com.nowcasting.activity` 的“大众点评”插屏增加专用候选：必须同时匹配 `cll_close_click` 小型底部居中可点击 `ImageView`、同一插屏窗口内的 `cll_interstitial_ad_gray`/`cll_gdt_ad_view`、`cll_ad_tips=广告` 和包含“跳转详情页/第三方应用”的 `cll_ad_tip_title`。证据和结构任一不符均不点击；规则仅调用该节点的 `ACTION_CLICK`，不使用坐标或 `ACTION_DISMISS`。
- 版本递增至 `versionCode 30` / `versionName 1.0.24`，同步更新根 README 与 Wiki。
- 验证：`:app:compileReleaseKotlin`、`:app:assembleRelease`、`npx tsc --noEmit`、`git diff --check` 均通过；APK 包名和版本元数据正确，v2 签名有效且证书与既有 Release 版本相同（SHA-256 `93c41045…fa999`）。已通过 `adb install -r` 安装到 USB 手机；系统确认 `1.0.24`，净屏无障碍服务保持启用和绑定。安装前同款广告仍在；安装后该弹层不再显示，但同时出现过“彩云天气想要打开大众点评”的系统确认提示。助手没有操作该提示；用户确认点击了“拒绝”，大众点评未打开。随后观察到彩云天气页面且无该广告，但仍无法确认系统提示是否由广告跳转动作触发，也无法独立归因广告消失是由本次关闭规则造成，因此真机结果记为“广告消失、动作归因与安全性未验证”，不宣称验收通过。
- 涉及文件：`android/app/src/main/java/com/clearscreen/prototype/backend/ClearScreenAccessibilityService.kt`、`android/app/build.gradle`、`app.json`、`README.md`、`docs/wiki/README.md`、`docs/wiki/CHANGELOG.md`、`android/app/build/outputs/apk/release/app-release.apk`。

### 2026-10-09 09:53:01 +08:00 — 修复彩云天气 SVIP 海报关闭漏判与 OCR 回调生命周期

- 根据真机无障碍树，为彩云天气 `com.nowcasting.activity` 的 `entrance_image` 海报和同父节点 `close` 按钮增加专用窄规则。只有小型、可见、可点击且位于海报下方居中的关闭节点，与海报几何关系匹配，并且同一张新鲜 OCR 截图在海报内部识别到 SVIP 年卡/会员、抽奖或奖品及“立即查看”等组合文案时才点击；不以页面其他位置的广告或营销文案补证，不使用坐标点击，也不放宽其他应用规则。
- 修复 ML Kit OCR 完成回调与无障碍服务销毁时关闭线程池之间的竞态；服务连接后按总开关、目标应用规则和白名单状态补扫一次当前前台页面。
- 版本递增至 `versionCode 29` / `versionName 1.0.23`，同步更新 README 与 Wiki。
- 验证：`:app:assembleRelease` 成功；`npx tsc --noEmit` 通过；`git diff --check` 通过（仅有 LF/CRLF 提示）。APK 包名/版本元数据正确，v2 签名有效且证书与现有 Release 证书一致（SHA-256 `93c41045…fa999`）。已通过 `adb install -r` 升级安装至 USB 连接的 Android 14 手机；安装后确认版本 `1.0.23`，无障碍服务仍启用并绑定。安装后第一张截图中目标海报仍在；之后未手动点击的情况下，该海报在返回彩云天气时已不再显示，但期间页面/活动有切换，Release 日志没有可独立归因的动作记录，因此本次真机自动关闭结果记为“观察到消失，归因未确认”，不能据此宣称命中规则；应用内总开关和分类开关状态未核实。
- 涉及文件：`android/app/src/main/java/com/clearscreen/prototype/backend/ClearScreenAccessibilityService.kt`、`android/app/build.gradle`、`app.json`、`README.md`、`docs/wiki/README.md`、`docs/wiki/CHANGELOG.md`、`android/app/build/outputs/apk/release/app-release.apk`。

## 2026-10-08

### 2026-10-08 11:55:33 +08:00 — 修复开屏独立 OCR 门控并兼容分行广告文案

- 针对 1.0.20 仍漏掉顶部居中“跳过”的反馈，确认并修复一处可能阻断该规则的调度缺口：OCR 截图此前只检查弹窗/信息流开关；当用户只启用开屏/视频跳过时，扫描会提前返回。现在只要三类功能任一开启就允许 OCR 扫描。
- 同一限定海报区域内的 OCR 行文案现在先按位置组合，再判断“浏览器 + 看剧/追剧 + 金币/惊喜促销”，以适配竖屏广告将一句文案拆成多行的情况。仍要求当前树签名一致、截图新鲜、顶部中央目标为小型独立可点击节点；不点击大范围父节点、不使用坐标点击，也不降低其他规则阈值。
- 版本递增到 `versionCode 27` / `versionName 1.0.21`，同步更新 README 与 Wiki。
- 验证：`:app:assembleRelease` 成功；`npx tsc --noEmit` 通过；`git diff --check` 通过（仅有既存 LF/CRLF 提示）。APK 元数据为 `com.clearscreen.prototype` / `1.0.21` / `净屏`；v2 签名有效且证书与旧版相同（SHA-256 `93c41045…fa999`）。`app-release.apk` 与命名副本 SHA-256 一致：`77E614432F63C902727F2ACA2EA26E74EDDBB3972FEC7E9D275C376F4874BC5A`。当前没有连接 Android 设备，因此未能确认该问题在手机上的实际根因，也未执行安装和正负例行为回归。
- 涉及文件：`android/app/src/main/java/com/clearscreen/prototype/backend/ClearScreenAccessibilityService.kt`、`android/app/build.gradle`、`app.json`、`README.md`、`docs/wiki/README.md`、`docs/wiki/CHANGELOG.md`、`android/app/build/outputs/apk/release/app-release.apk`、`android/app/build/outputs/apk/release/ClearScreen-1.0.21-release.apk`。

### 2026-10-08 11:31:41 +08:00 — 补充顶部居中第三方跳转开屏广告识别

- 根据用户提供的彩云天气开屏广告截图，新增窄范围跳过规则：从 OCR 单词级边界提取顶部“跳过”并对应到独立可点击节点；仅在目标位于顶部中央且尺寸受限，并且同一张未过期、无障碍树签名一致的截图同时含“跳转第三方页面/应用”提示、浏览器追剧文案和金币/惊喜促销动作时触发。位置或“跳过”单独出现不触发；不执行坐标点击，也不放宽其他位置的广告规则。命中规则 ID：`builtin.skip.splash.top-center-third-party`。
- 版本递增到 `versionCode 26` / `versionName 1.0.20`，并同步更新 README 与 Wiki 的适用范围和安全边界。
- 验证：`:app:assembleRelease` 成功；`npx tsc --noEmit` 通过；`git diff --check` 通过（仅有既存 LF/CRLF 提示）。Release APK 核对包名 `com.clearscreen.prototype`、版本 `1.0.20`、桌面名称“净屏”；APK v2 签名有效，证书 SHA-256 与原 Release 证书一致（`93c41045…fa999`）。`app-release.apk` 与命名副本 SHA-256 一致：`423EA159A5EB30F38100B3656FB9D57A7E3D9AF09217C1376EB4A87A6BD727FF`。未连接 Android 设备，未进行手机安装及该广告的实际识别/误触正负例回归；构建成功不能替代真机验证。
- 涉及文件：`android/app/src/main/java/com/clearscreen/prototype/backend/ClearScreenAccessibilityService.kt`、`android/app/build.gradle`、`app.json`、`README.md`、`docs/wiki/README.md`、`docs/wiki/CHANGELOG.md`、`android/app/build/outputs/apk/release/app-release.apk`、`android/app/build/outputs/apk/release/ClearScreen-1.0.20-release.apk`。

### 2026-10-08 11:13:12 +08:00 — 收紧广告证据归属并补强卡片内 X 识别

- 根据天气页截图优化 `ClearScreenAccessibilityService.kt`：卡片内无标签的关闭图标可由 OCR 识别独立 X，再要求该 X 与受限尺寸的可点击无障碍节点重合、位于同一卡片边缘，且同一卡片内有明确广告标记（如“推荐广告”）后才允许点击；仅靠 OCR X 不足以触发。无明确标记时，回退识别提高为同卡片至少三种不同商业线索加行动文案，候选卡片最大面积收紧至屏幕的 45%。仍只执行 `ACTION_CLICK`，不猜坐标、不点整卡。
- 修复跨区域证据拼接风险：弹窗 OCR 文案限定在关闭控件所属的浮层区域；全屏通用根节点不能把页面其他位置的推荐广告/会员文案汇总成广告弹窗证据。截图中脱离卡片的单独 X 若没有同浮层广告节点或广告资源 ID，保持不点。
- 如果 OCR 截图前后的无障碍树签名变化，丢弃旧图文证据并最多重采一次；重采后仍变化就不点击，避免页面动态更新导致漏判时误用过期截图。
- 在 `docs/wiki/README.md` 增加长期安全原则：防误关优先于覆盖率，规则需有正例和相似业务页面负例回归；在根 `README.md` 同步简述。递增 `android/app/build.gradle` 与 `app.json` 至 `versionCode 25` / `versionName 1.0.19`。
- 构建 `:app:assembleRelease` 成功，生成 `android/app/build/outputs/apk/release/app-release.apk`，并复制为 `ClearScreen-1.0.19-release.apk`。APK 核验：包名 `com.clearscreen.prototype`、版本 `1.0.19`、桌面名称“净屏”；v2 签名有效且与原 Release 证书一致（SHA-256 `93c41045…fa999`）。文件 SHA-256：`C9A9B704FEFA02789E6C2CCCED9FAD8DC49BE7F38EB4757A72C00E1D47A7F545`。
- 验证：`:app:compileReleaseKotlin`、`:app:assembleRelease`、`npx tsc --noEmit`、`app.json` 解析/版本一致性、APK 元数据与签名、复制文件哈希一致性均通过；`git diff --check` 通过（仅有仓库既存的 LF/CRLF 提示）。未执行手机安装和广告正负例真机回归；当前没有连接的 Android 设备。
- 涉及文件：`android/app/src/main/java/com/clearscreen/prototype/backend/ClearScreenAccessibilityService.kt`、`android/app/build.gradle`、`app.json`、`README.md`、`docs/wiki/README.md`、`docs/wiki/CHANGELOG.md`、两个 Release APK。

### 2026-10-08 10:54:10 +08:00 — 构建 1.0.18 Release 安装包

- 执行 `android\gradlew.bat :app:assembleRelease`，构建成功。
- 生成 `android/app/build/outputs/apk/release/app-release.apk`，并复制为 `android/app/build/outputs/apk/release/ClearScreen-1.0.18-release.apk`，两个文件 SHA-256 一致：`14A4E7FE1197D65F4018AE9AD7E917B38CB37DFA5CE33F8FA8518AD98524597A`；文件大小 101,194,359 字节。
- 核验 APK 元数据：包名 `com.clearscreen.prototype`、版本 `1.0.18`、`versionCode 24`、桌面名称“净屏”；APK v2 签名验证通过，证书 SHA-256 与既有 Release 证书一致（`93c41045…fa999`）。
- 验证：Release 构建、APK 元数据、APK v2 签名和复制文件哈希一致性均通过。构建期间有 Android SDK XML/Gradle 兼容性提示，但不影响成功；未执行手机安装及广告识别真机验证。
- 涉及文件：上述两个 Release APK、`docs/wiki/CHANGELOG.md`。

### 2026-10-08 10:47:22 +08:00 — 接入本地 OCR 与卡片级信息流广告关闭

- 新增 APK 内置的中文 OCR：Android 11 及以上由无障碍服务截取当前显示并仅在设备内存处理；截图不保存、不上传。OCR 证据会与截图前后的无障碍树签名、控件边界和区域一起校验，页面变化时丢弃旧截图证据。
- 增加信息流/瀑布流卡片规则：广告文字必须落在同一候选卡片内，X/关闭必须是卡片边缘的小型独立可点击节点；不点整卡、不做猜测坐标点击。成功后短暂重扫下一张卡片。为开屏跳过控件增加父节点大小限制；广告资源 ID 改为分段精确标记，避免 `download_button`、`recycler_adapter` 等误命中。
- 设置页增加开屏/视频、弹窗、信息流三类独立开关，旧安装默认开启且保留其他已有设置；服务声明补充截图能力、滚动/文本事件，系统无障碍服务说明本地 OCR 用途；检测到密码字段或聚焦的文本输入时不截图。
- 更新 `android/app/build.gradle` 与 `app.json` 到 `versionCode 24` / `versionName 1.0.18`，同步更新 README 和本 Wiki 的覆盖范围、隐私与漏识边界。
- 涉及文件：`android/app/src/main/java/com/clearscreen/prototype/backend/ClearScreenAccessibilityService.kt`、`ClearScreenStore.kt`、`ClearScreenNativeModule.kt`、`android/app/src/main/res/values/strings.xml`、`android/app/src/main/res/xml/accessibility_service_config.xml`、`xml-v30/accessibility_service_config.xml`、`xml-v31/accessibility_service_config.xml`、`android/app/build.gradle`、`app.json`、`App.tsx`、`src/native/ClearScreenNative.ts`、`src/screens/SettingsScreen.tsx`、`README.md`、`docs/wiki/README.md`、`docs/wiki/CHANGELOG.md`。
- 验证：`npx tsc --noEmit` 通过；`.\gradlew.bat :app:compileDebugKotlin` 通过；`git diff --check` 通过；`app.json` 与三份无障碍服务 XML 解析通过。未构建 Release APK；未在真机验证识别、点击准确率和厂商截图限制。OCR/规则不是专用训练的广告分类模型，实际覆盖率与误点率仍需真机样本回归。

## 2026-10-07

### 2026-10-07 23:10:20 +08:00 — 收紧广告语境判定，修复汽水音乐“我的”页误点小程序

- 背景：用户反馈在汽水音乐“我的”个人主页（图一）停留时，会被误识别并点击，跳转到“小程序”列表页（图二）。排查结论：该页没有任何真实广告，但存在三处误判叠加——(1) 文案“免广告·铃声·全景声”里包含“广告”二字，被当成广告强标记；(2) 页面上的 `VIP`/`会员`/`特权` 等商业词加上一个大面积的页面滚动容器，被当成“整页遮罩广告”成立；(3) 跳过通道的“资源 ID 单证据”分支只要落在右上角紧凑位置即可点击，而“小程序”入口的位置（约屏幕高度 43%、横向 73%）恰好满足旧的松阈值（顶部 48% 以内）。
- 更新 `android/app/src/main/java/com/clearscreen/prototype/backend/ClearScreenAccessibilityService.kt`：
  - 新增 `NEGATED_AD_MARKERS` 与 `scrubNegatedAdMarkers`，`免广告`/`无广告`/`去广告`/`屏蔽广告`/`拦截广告` 等否定说法不再计入广告强标记（资源 ID 类标记不变）；`关闭广告`/`跳过广告` 是控件自身文字，保留为广告线索，避免只暴露“关闭广告”按钮的弹窗广告识别不到；
  - 新增 `SCROLL_CONTAINER_CLASSES` 与 `isOverlayLayer`，整页遮罩要求的“大面积面板”必须是弹窗/广告类浮层（类名或资源 ID 含 popup/dialog/overlay/splash/ad 等），页面自身的列表与滚动容器（RecyclerView/ListView/ScrollView/ViewPager/WebView）不再计入；
  - 新增 `isVeryNearTopRight`（顶部 30% 以内、横向 60% 起、右边界 75% 起），资源 ID 单证据分支的位置兜底由松阈值改为该严格阈值。
- 同步更新 `docs/wiki/README.md` 第 3.1 节和根目录 `README.md` 的识别边界说明。
- 版本：`android/app/build.gradle` 递增为 `versionCode 23` / `versionName 1.0.17`，`app.json` 同步为 `1.0.17` / `versionCode 23`。
- 验证：`.\gradlew.bat :app:compileDebugKotlin` 通过；`.\gradlew.bat :app:assembleRelease` 构建成功，产物 `android/app/build/outputs/apk/release/app-release.apk`，另复制一份 `ClearScreen-1.0.17-release.apk` 便于发送到手机。apksigner 校验使用同一份 Release 证书（CN=ClearScreen，SHA-256 `93c41045…fa999`），`aapt2 dump badging` 核对包名 `com.clearscreen.prototype`、`versionCode 23`、`versionName 1.0.17`、桌面名称“净屏”，可直接升级安装。汽水音乐真机复现与回归（确认个人主页不再被点击、真实开屏/视频广告仍能跳过）未执行，需真机验证。

## 2026-09-26

### 2026-09-26 19:14:48 +08:00 — 递增版本并产出 1.0.16 正式 Release 安装包

- 更新 `android/app/build.gradle`（`versionCode 22`、`versionName 1.0.16`）和 `app.json`（`version`、`android.versionCode`），保持两处同步。
- 执行 `.\gradlew.bat :app:assembleRelease`，构建成功（含 Metro JS bundle 打包），产物为 `android/app/build/outputs/apk/release/app-release.apk`，另复制一份 `ClearScreen-1.0.16-release.apk` 便于发送到手机。
- 核对结果：包名 `com.clearscreen.prototype`、`versionCode 22`、`versionName 1.0.16`、桌面名称“净屏”；APK 使用本机 Release 证书（CN=ClearScreen，SHA-256 `93c41045…fa999`）签名，通过 apksigner v2 校验。
- 该包含 1.0.15 之后的两项改动：资源 ID 跳过规则收紧、记录页显示命中规则 ID。真机安装与行为验证未执行。
- 说明：构建输出目录清理时旧的 `ClearScreen-1.0.15-release.apk` 已被移除，签名证书未更换，可直接升级安装。

### 2026-09-26 19:09:51 +08:00 — 记录页显示命中规则 ID

- 目的：为新规则验证提供可观测入口。此前本地事件已存 `ruleId`，但界面只显示“已关闭弹窗广告”这类文案，出问题只能靠猜。
- 更新 `android/app/src/main/java/com/clearscreen/prototype/backend/ClearScreenNativeModule.kt`：向 JS 输出的记录增加 `ruleId` 字段。
- 更新 `src/data.ts` 的 `LogItem` 类型增加可选 `ruleId`；更新 `src/screens/RecordsScreen.tsx`，在记录行文案下方以小字显示命中的规则 ID。
- 验证：`npx tsc --noEmit` 通过；`.\gradlew.bat :app:compileDebugKotlin` 通过。真机界面显示效果未验证。
- 说明：本次只做可观测性改动，未新增“带广告角标的弹窗/信息流卡片”识别规则；该部分按约定先做真机节点取证。

### 2026-09-26 17:06:38 +08:00 — 收紧资源 ID 跳过规则，修复滴滴行程页误点

- 背景：用户反馈在滴滴拼车行程页（图一）会被误识别并点击，导致跳转到“拼车圈”页（图二）。排查结论：该页没有任何广告/营销文本，弹窗关闭通道无法触发；误点来自跳过通道的“资源 ID 单证据”分支——只要控件 `viewIdResourceName` 含 `skip`/`jump`/`countdown`/`ad_skip` 就基础分 96+28=124，而阈值仅 96，位置和语境只是加分项，因此任何普通跳转容器（如 `xxx_jump`）都会被点击。
- 更新 `android/app/src/main/java/com/clearscreen/prototype/backend/ClearScreenAccessibilityService.kt`：新增 `resourceOnly` 判定（仅资源 ID 命中、无跳过文字标签），该情况必须同时满足“右上角且尺寸紧凑”，或存在广告强标记/祖先广告信号/视频广告语境，才允许点击；规则 ID 仍为 `builtin.skip.resource-id`。
- 同步更新 `docs/wiki/README.md` 第 3.1 节和根目录 `README.md` 的识别边界说明。
- 验证：已执行 `.\gradlew.bat :app:compileDebugKotlin`，Kotlin 编译通过。滴滴真机复现与回归（确认行程页不再被点击、真实开屏/视频广告仍能跳过）未执行，需要真机验证。

## 2026-09-23

### 2026-09-23 10:02:38 +08:00 — 建立修改记录和 AI 开工约定

- 新增本文件，整理最近的代码、安装和文档变更。
- 新增根目录 `AGENTS.md`：要求 AI 修改项目前先阅读本文件最新条目，并在每次修改完成后追加带时间、文件和验证结果的记录。
- 更新 Wiki 与根目录 README 的入口链接。
- 验证：检查现有文件、工作区状态和文件修改时间；未执行构建或自动化测试（本次只改文档和协作指引）。

## 2026-09-22

### 2026-09-22 15:42:03 +08:00 — 更新 Wiki 与安装约定

- 更新 `docs/wiki/README.md`，记录无障碍广告处理边界、vivo 授权设置和正式 APK 的网页版互传安装流程。
- 记录普通业务弹窗保护、整页遮罩广告条件，以及腾讯视频类右上角“跳过”处理说明。
- 时间依据：Wiki 文件系统最后修改时间。

### 2026-09-22 15:40:36 +08:00 — 收紧弹窗识别并改进视频广告跳过

- 更新 `android/app/src/main/java/com/clearscreen/prototype/backend/ClearScreenAccessibilityService.kt`：不再以通用 `ACTION_DISMISS` 关闭弹窗；对整页广告要求广告/营销证据、大面积面板和右上角关闭控件共同成立。
- 增加视频广告右上角“跳过”识别：按文字节点定位、点击可点击父节点，并对动态出现的控件在约 1 秒内重扫三次；诊断规则为 `builtin.skip.video-ad.edge`。
- 验证结果（当次任务记录）：Kotlin 编译、Release 构建、TypeScript 检查和 APK 签名验证通过；腾讯视频真机实际点击仍待验证。
- 时间依据：无障碍服务源文件系统最后修改时间。

### 2026-09-22 14:43:02 +08:00 — 同步 Release 版本号

- 更新 `android/app/build.gradle` 和 `app.json`，将应用版本同步到 `1.0.15`、Android `versionCode` 同步到 `21`。
- 时间依据：两个版本配置文件系统最后修改时间。

### 2026-09-22 13:04:09 +08:00 — 移除 VPN 占用路径

- 更新 Android 原生模块，移除应用启动/停止 VPN 服务的调用；同时从 Android Manifest 移除 VPN 服务声明及相关权限，并删除 `ClearScreenVpnService.kt`。
- 更新 JS 与界面状态，使净屏不申请、不启动、不接管用户自己的 VPN。
- 时间依据：Android 原生模块系统最后修改时间；相关 Manifest 文件最后修改于 13:04:18 +08:00。

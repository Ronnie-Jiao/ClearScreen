# 净屏修改记录

本文件按时间倒序记录项目修改，供后续 AI 和开发者开始任务前快速了解最近变化。自本记录建立起，新增条目的时间使用中国标准时间（UTC+08:00）；既有条目的时间按文件系统记录的本地最后修改时间整理。

## 2026-10-22

### 2026-10-22 20:44:54 +08:00 — 统一美化应用内提示与确认弹窗

- 新增共用 `AppDialog` 与提示队列，将恢复默认、清空记录、隐私撤回、权限引导、保存失败和操作结果等原生提示替换为白色圆角卡片；统一状态图标、文字层级、淡色取消按钮、渐变主按钮及红色危险操作按钮。
- 保留各入口的必要告知、确认和取消回调；提示依次展示，确认回调防止重复执行。长内容滚动，窄屏、大字体、较长按钮文案或三个操作时纵向排列。
- vivo 自启动引导及版本信息弹窗复用同一组件；发布页打开失败在当前窗口内提示。主工程和 HarmonyOS 隔离试验入口均接入提示容器，同步 README 与 Wiki。
- 验证：主工程及 HarmonyOS 隔离试验的 `npx tsc --noEmit`、`git diff --check` 通过；源码扫描确认主工程不再直接调用原生 `Alert.alert`。未运行功能测试、未启动应用或执行真机/UI 验收，未打包或安装。
- 涉及文件：`src/components/AppDialog.tsx`、`App.tsx`、`src/screens/SettingsScreen.tsx`、`src/screens/SettingsDetailScreen.tsx`、`src/screens/ServicePermissionsScreen.tsx`、`targets/harmony-bare-pilot/App.tsx`、`README.md`、`docs/wiki/README.md`、本修改记录。

### 2026-10-22 20:35:16 +08:00 — 加快记录保留周期切换反馈

- 选择保留周期时先即时更新选中状态；原生保存成功后不再构建并回传包含应用清单、历史记录及权限的完整快照，保存失败时恢复到最近已保存的周期。
- 验证：`npx tsc --noEmit`、`:app:compileDebugKotlin --offline --console=plain`、`git diff --check` 均通过；未运行功能测试、未打包或安装。
- 涉及文件：`App.tsx`、`src/native/ClearScreenNative.ts`、`android/app/src/main/java/com/clearscreen/prototype/backend/ClearScreenNativeModule.kt`、本修改记录。

### 2026-10-22 20:34:14 +08:00 — 精简隐私政策阅读页底部内容

- 从隐私政策阅读页移除开发者与联系方式卡片、公开版链接和撤回按钮，并调整未成年人说明及撤回说明文案。
- 将 Android“撤回同意并删除”入口移至“设置 > 隐私与数据”，继续使用原确认和数据清除流程；同步 README、设置层级 Wiki 和隐私政策操作路径。
- 验证：`npx tsc --noEmit`、`git diff --check` 通过；未运行功能测试、未打包或安装。
- 涉及文件：`src/screens/PrivacyPolicyScreen.tsx`、`src/screens/SettingsDetailScreen.tsx`、`App.tsx`、`README.md`、`docs/wiki/README.md`、`docs/wiki/隐私政策.md`、本修改记录。

### 2026-10-22 20:31:50 +08:00 — 移除服务与权限页下方状态卡

- 删除“系统授权 / 服务连接 / 最近异常”状态卡及其单独状态计算，移除卡片间距；保留上方四项权限设置入口。
- 更新设置层级 Wiki，避免继续列出已移除的状态卡。
- 验证：`npx tsc --noEmit`、`git diff --check` 通过；未运行功能测试、未打包或安装。
- 涉及文件：`src/screens/ServicePermissionsScreen.tsx`、`App.tsx`、`docs/wiki/README.md`、本修改记录。

### 2026-10-22 20:28:59 +08:00 — 固定首页最近记录卡片容量和行高

- 记录卡片按标题加五行记录预留固定高度，并继续填充底部导航上方的剩余空间；卡片高度不再由当前记录条数决定。移除记录行的自动拉伸，每条固定 60dp，从标题下方顺序排列，少于五条时在内容下方留白。
- 标题与“查看全部”保留在列表上方，记录区独立滚动；继续最多展示五条，保留原记录入口及首页状态操作。同步 Wiki 布局说明。
- 验证：主工程及 HarmonyOS 隔离试验的 `npx tsc --noEmit`、`git diff --check` 均通过；未执行功能测试或真机/UI 验收，未打包或安装。
- 涉及文件：`src/screens/HomeScreen.tsx`、`docs/wiki/README.md`、本修改记录。

### 2026-10-22 14:50:08 +08:00 — 构建并升级安装净屏 1.0.58

- 按用户明确要求，将最新源码构建为正式 Release：`1.0.58` / versionCode `64`，包名 `com.clearscreen.prototype`，APK `android/app/build/outputs/apk/净屏.apk`，SHA-256 `66B7AA4CD62E89BB943D0D9E9DC2884E3C6E28CDA143E6D4D4198C3C60843738`。APK 正式签名证书 SHA-256 与项目记录一致：`93c410454a8263736ff60bb6e27153b19f74f4c934f444531842f5162ccfa999`。
- 通过 USB 对 vivo V2502DA（`10AG4M2KGF00527`）执行覆盖升级安装，ADB 返回 `Success`。安装后设备包版本为 `1.0.58` / `64`，首次安装时间仍为 `2026-09-20 15:33:13`，未卸载或清除应用数据。
- 验证：`:app:assembleRelease --offline --console=plain` 成功；`apksigner verify --print-certs`、APK 包名/版本/标签核对、正式证书指纹与 SHA-256 核对、ADB 安装及设备版本/首次安装时间核对通过。未执行功能测试或 UI 验收。
- 涉及文件/产物：`app.json`、`android/app/build.gradle`、`android/app/build/outputs/apk/净屏.apk`、本修改记录，以及本机系统秘钥目录 ClearScreen 的项目基本信息和验证与复用说明。

### 2026-10-22 13:28:41 +08:00 — 扩展主流安卓厂商的权限状态读取与只读设置核验

- 新增共享品牌配置，设置跳转和读取共用 vivo/iQOO、小米/Redmi/POCO、华为/荣耀、OPPO/realme/一加、三星、魅族、联想/ZUI/摩托罗拉、中兴/努比亚/红魔、华硕、诺基亚、Google/Nothing/Sony、传音和乐视等分流。仅登记存在时可核验的系统包，未知组件仍回退自身应用信息；新增旧 Nokia 后台例外页候选，不把名单或品牌视为授权。
- 小米尝试系统仍允许访问的本应用自启动查询，只接收明确 0/1；没有接入隐藏 API 解禁、默认已开启或猜测 AppOp 的做法。保留 vivo 已有的自身 provider 查询，其他情况走受限只读设置页核验或保持未知，不声明所有 ROM 均开放读取。
- 页面核验仅由用户主动打开自启动/后台设置启动，校验系统应用身份、净屏包名或无同名冲突的名称、明确正向含义、局部唯一真实可勾选控件及同窗两次稳定结果。华为/荣耀自动管理、三星休眠名单、关联启动、混合勾选、聚焦输入或歧义不作为已开启。结果仅短时留在内存，退出引导/服务断开/撤回同意会失效；没有系统写入、自动点击、手势、截图或 OCR。
- 后台状态增加实际厂商允许/禁止值，Android 标准限制为 true 时优先显示受限；电池优化、无障碍继续独立查询。系统设置始终提前退出广告动作路径，即使广告总开关暂停，也只核验用户主动打开的自身权限。同步隐私说明及品牌矩阵，未知仍明确显示需手动确认。
- 验证：主工程 `npx tsc --noEmit`、`:app:compileDebugKotlin :app:processDebugMainManifest --offline --console=plain`、`git diff --check` 均通过；只做源码/静态/编译检查，未运行功能测试，未启动或操作手机/模拟器，未打包、安装或逐品牌真机验收。没有升级依赖、版本号或签名。
- 涉及文件：`ClearScreenOemProfile.kt`、`ClearScreenSettingsObserver.kt`、`ClearScreenPermissionReader.kt`、`ClearScreenSettingsNavigator.kt`、`ClearScreenNativeModule.kt`、`ClearScreenAccessibilityService.kt`（均位于 Android backend 目录）、`android/app/src/main/AndroidManifest.xml`、`App.tsx`、`src/native/ClearScreenNative.ts`、`src/screens/ServicePermissionsScreen.tsx`、`src/screens/PrivacyPolicyScreen.tsx`、`README.md`、`docs/wiki/README.md`、`docs/wiki/权限状态核验.md`、`docs/wiki/隐私政策.md`、本修改记录，以及本机系统秘钥目录 ClearScreen 的验证与复用说明。

### 2026-10-22 13:08:20 +08:00 — 接入 vivo 自启动实际状态并解除权限核验阻塞

- 只读核对 V2502DA 的 vivo PermissionManager 系统 APK，确认自身包名过滤及 `currentstate` 的允许/禁止含义；新增独立读取器，仅查询净屏自身包名和 UID 的唯一记录。vivo/iQOO 在接口允许时显示“已开启/未开启”，缺失、未知或被拒绝仍显示“需手动确认”；不使用受保护 appfilter 接口，不新增或自动授予系统权限。
- 后台运行按 Android 标准接口显示“受限制/未受限”，不将其当成所有厂商电源规则均放行；电池优化及无障碍继续读取各自真实状态。进入服务与权限页或从设置返回时优先核验，不等待应用列表图标读取；独立线程、provider 等待上限和前端超时避免一直“核验中”。后台列表刷新不覆盖较新的权限状态，撤回同意时作废旧结果。
- 补充接口证据、状态含义及限制文档，并在本机项目资料的验证与复用说明中登记。验证：`npx tsc --noEmit`、`:app:compileDebugKotlin :app:processDebugMainManifest --offline --console=plain`、`git diff --check` 均通过。仅进行了只读诊断和静态/编译检查，未运行功能测试、未启动应用验收、未打包或安装；新 APK 中的实际开关开/关回显仍需真机确认。
- 涉及文件：`App.tsx`、`src/native/ClearScreenNative.ts`、`src/screens/ServicePermissionsScreen.tsx`、`android/app/src/main/java/com/clearscreen/prototype/backend/ClearScreenPermissionReader.kt`、`android/app/src/main/java/com/clearscreen/prototype/backend/ClearScreenNativeModule.kt`、`README.md`、`docs/wiki/README.md`、`docs/wiki/权限状态核验.md`、`docs/wiki/CHANGELOG.md`、`D:\BaiduSyncdisk\其它\系统秘钥\ClearScreen\验证与复用说明.md`。

### 2026-10-22 12:34:17 +08:00 — 构建并升级安装净屏 1.0.57

- 当前工作区构建正式 Release，版本 `1.0.57` / versionCode `63`，包名 `com.clearscreen.prototype`；安装包为 `android/app/build/outputs/apk/净屏.apk`，SHA-256：`8FDDACAA67776F9AC0C458785F5C850788DBC807E07D02ED87EB6925AB20C7A7`。APK 证书 SHA-256 与项目正式证书一致。
- 通过 USB 对唯一连接的 vivo V2502DA（`10AG4M2KGF00527`）执行 `adb install -r --no-streaming` 成功。设备显示版本 `1.0.57` / `63`，首次安装时间保持 `2026-09-20 15:33:13`，净屏无障碍服务仍在系统启用列表中；未卸载或清除数据。
- 验证：`npx tsc --noEmit`、`:app:assembleRelease --offline --console=plain`、APK 包名/版本/正式签名核验、设备升级后版本及首次安装时间核验、`git diff --check` 均通过。未执行广告功能测试或页面 UI 验收。
- 同步更新本机项目资料目录中的 `项目基本信息.md` 与 `验证与复用说明.md`，记录当前版本、APK 哈希、正式签名及设备升级核验。
- 涉及文件/产物：`app.json`、`android/app/build.gradle`、`docs/wiki/CHANGELOG.md`、`android/app/build/outputs/apk/净屏.apk`、`D:\BaiduSyncdisk\其它\系统秘钥\ClearScreen\项目基本信息.md`、`D:\BaiduSyncdisk\其它\系统秘钥\ClearScreen\验证与复用说明.md`。

### 2026-10-22 12:19:40 +08:00 — 完善多品牌后台设置与返回核验

- 服务与权限分列“自动跳过服务 / 开机自启动 / 后台运行 / 电池优化”；接入 vivo/iQOO、小米/Redmi/POCO、华为/荣耀、OPPO/realme/一加、华硕及乐视自启动候选页，以及部分品牌的后台/电源设置候选。按品牌校验入口可用性和访问权限，启动失败逐项回退到应用信息或系统设置；三星及未知自启动入口使用应用信息。厂商组件均为运行时候选，未据此声明全机型适配已验收。
- 电池优化独立使用系统例外列表及真实查询结果；自启动与无法完整读取的厂商后台开关显示“需手动确认”，标准后台限制明确开启时显示“受限制”。读取失败明确保留未知/失败状态，打开设置或完成引导不记为授权。
- 返回前台时刷新并延时复核，重复核验只读取权限状态，避免反复枚举应用图标和记录；离开前台或打开另一项设置使旧结果失效。区分无障碍开关与服务连接，修复只因读取失败就跳转隐私同意页的问题，设置入口失败时提供明确提示。
- 同步 README、Wiki 的入口、状态及实现参考。验证：`npx tsc --noEmit`、`:app:compileDebugKotlin :app:processDebugMainManifest --offline --console=plain`、`git diff --check` 均通过；未运行功能测试、未启动应用或进行真机/UI 验收、未打包或安装。
- 涉及文件：`App.tsx`、`src/screens/ServicePermissionsScreen.tsx`、`src/native/ClearScreenNative.ts`、`android/app/src/main/java/com/clearscreen/prototype/backend/ClearScreenSettingsNavigator.kt`、`android/app/src/main/java/com/clearscreen/prototype/backend/ClearScreenNativeModule.kt`、`android/app/src/main/AndroidManifest.xml`、`README.md`、`docs/wiki/README.md`、`docs/wiki/CHANGELOG.md`。

### 2026-10-22 12:04:19 +08:00 — 将隐私政策和恢复默认入口移到设置主页

- 按截图把“隐私政策”和“恢复默认设置”放到设置主页“隐私与数据”之后、“帮助与反馈”之前；从隐私与数据详情页移除原卡片，保留政策阅读、恢复确认及原操作。
- 更新 README 与 Wiki 的设置层级说明。验证：主工程和 HarmonyOS 隔离试验的 `npx tsc --noEmit`、`git diff --check` 均通过；未启动应用或进行 UI 验收、未打包或安装。
- 涉及文件：`src/screens/SettingsScreen.tsx`、`src/screens/SettingsDetailScreen.tsx`、`App.tsx`、`targets/harmony-bare-pilot/App.tsx`、`README.md`、`docs/wiki/README.md`、`docs/wiki/CHANGELOG.md`。

### 2026-10-22 12:01:26 +08:00 — 恢复默认时将记录保留周期设为 7 天

- 新安装的记录保留周期默认值为 7 天；“恢复默认设置”现在也恢复为 7 天，并在确认提示中说明。恢复时继续保留已有处理记录和应用排除规则；更新安装继续保留用户已保存的选择。
- 验证：`npx tsc --noEmit`、`:app:compileDebugKotlin`、`git diff --check` 均通过；未运行自动化测试、未启动应用或进行 UI 验收、未打包或安装。
- 涉及文件：`android/app/src/main/java/com/clearscreen/prototype/backend/ClearScreenStore.kt`、`src/screens/SettingsDetailScreen.tsx`、`README.md`、`docs/wiki/README.md`、`docs/wiki/CHANGELOG.md`。

### 2026-10-22 11:59:46 +08:00 — 对齐首页今日处理摘要

- 将“今日处理”数值行垂直居中到首页状态行，标签保持在数值上方并右对齐，修正截图中的视觉错位。
- 验证：`npx tsc --noEmit`、`git diff --check` 均通过；未启动应用、未进行 UI 验收、未打包或安装。
- 涉及文件：`src/screens/HomeScreen.tsx`、`docs/wiki/CHANGELOG.md`。

### 2026-10-22 11:59:04 +08:00 — 对齐首页最近记录入口

- 将首页“查看全部”改为独立的水平点击行，统一文字行高并使用标准右箭头图标，使入口与标题垂直居中、右侧和内容区对齐。
- 验证：`npx tsc --noEmit`、`git diff --check` 均通过；未启动应用、未进行 UI 验收、未打包或安装。
- 涉及文件：`src/screens/HomeScreen.tsx`、`docs/wiki/CHANGELOG.md`。

### 2026-10-22 11:57:49 +08:00 — 统一三个主页面品牌字号

- 首页和应用页品牌标题改用设置页的字号规格；底部导航继续由共享组件统一字号。同步记录字号数值。
- 验证：`npx tsc --noEmit`、`git diff --check` 均通过；未启动应用、未进行 UI 验收、未打包或安装。
- 涉及文件：`src/screens/HomeScreen.tsx`、`src/screens/AppsScreen.tsx`、`docs/wiki/README.md`、`docs/wiki/CHANGELOG.md`。

### 2026-10-22 11:52:32 +08:00 — 自动跳过服务行直达系统设置

- 服务与权限页点击“自动跳过服务”直接打开 Android 无障碍设置，不再进入净屏内部权限说明页；保留 Android 受限设置提示逻辑。
- 更新根 README 与 Wiki 的设置交互说明。验证：`npx tsc --noEmit`、`git diff --check` 均通过；未启动应用、未进行 UI 验收、未打包或安装。
- 涉及文件：`App.tsx`、`src/screens/ServicePermissionsScreen.tsx`、`README.md`、`docs/wiki/README.md`、`docs/wiki/CHANGELOG.md`。

### 2026-10-22 11:49:45 +08:00 — 精简帮助与反馈页面

- 移除帮助与反馈页中的“意见反馈”和“导出诊断报告”卡片及对应操作逻辑，保留常见问题内容，并同步页面说明。
- 验证：`npx tsc --noEmit`、`git diff --check` 均通过；未启动应用、未进行 UI 验收、未打包或安装。
- 涉及文件：`App.tsx`、`src/screens/SettingsDetailScreen.tsx`、`README.md`、`docs/wiki/README.md`、`docs/wiki/CHANGELOG.md`。

### 2026-10-22 11:47:58 +08:00 — 美化 vivo 后台保护提示

- 将系统原生提示框改为净屏风格的圆角弹层，优化图标、说明文字、背景遮罩和操作按钮；保留取消和打开 vivo 自启动管理的行为。
- 验证：`npx tsc --noEmit`、`git diff --check` 均通过；未启动应用、未进行 UI 验收、未打包或安装。
- 涉及文件：`App.tsx`、`src/screens/ServicePermissionsScreen.tsx`、`docs/wiki/CHANGELOG.md`。

### 2026-10-22 11:47:45 +08:00 — 精简关于净屏页面

- 按截图移除“检查更新”和“APP备案号”入口，删除两项之间及其下方由相关卡片造成的布局空隙；关于页仅保留应用标识和版本信息。
- 更新根 README 与 Wiki 页面结构说明。验证：`npx tsc --noEmit`、`git diff --check` 均通过；未打包、未安装，未进行应用 UI 验收。
- 涉及文件：`src/screens/SettingsDetailScreen.tsx`、`README.md`、`docs/wiki/README.md`、`docs/wiki/CHANGELOG.md`。

### 2026-10-22 11:45:28 +08:00 — 固定应用页顶部并独立滚动应用列表

- 将品牌标题、“全部开启”、搜索框和应用筛选固定在列表上方；仅 App 列表在底部导航上方的剩余区域内滚动，保留搜索、筛选、单应用开关和全部开启逻辑。
- 更新根 README 与 Wiki 的应用页交互说明。验证：`npx tsc --noEmit`、`git diff --check` 均通过；未打包、未安装，未进行应用 UI 验收。
- 涉及文件：`src/screens/AppsScreen.tsx`、`README.md`、`docs/wiki/README.md`、`docs/wiki/CHANGELOG.md`。

### 2026-10-22 11:44:00 +08:00 — 填充首页最近记录区域并升级安装 1.0.56 / 62

- 首页最近记录由最多 3 条增至 5 条；记录卡片和列表行按可用高度延展，滚动区域为底部导航预留空间，避免内容下方出现大块背景空白。更新首页 Wiki 说明。
- Android 版本升至 1.0.56 / versionCode 62。`npx tsc --noEmit`、`:app:assembleRelease --offline --console=plain`、`git diff --check` 均通过。正式 Release APK 包名 `com.clearscreen.prototype`、应用名“净屏”、minSdk 24 / targetSdk 36；签名证书 SHA-256 为 `93c410454a8263736ff60bb6e27153b19f74c934f444531842f5162ccfa999`。原始 APK 与命名 APK SHA-256 均为 `86FE1223D6982AE62932BC5BA822E8091C1692E884C01B7BF54CB3E20408CC68`。
- 使用 `adb install -r --no-streaming` 成功升级安装到 vivo V2502DA（`10AG4M2KGF00527`）。安装后为 1.0.56 / 62；首次安装时间保持 `2026-09-20 15:33:13`，更新时间 `2026-10-22 11:43:55`，未卸载或清除应用数据。未打开应用做功能/UI验收，实际首页视觉效果留待用户确认。
- 涉及文件：`src/screens/HomeScreen.tsx`、`app.json`、`android/app/build.gradle`、`docs/wiki/README.md`、`docs/wiki/CHANGELOG.md`；产物 `android/app/build/outputs/apk/release/app-release.apk`、`android/app/build/outputs/apk/净屏.apk`。保留其他窗口现有工作区修改，未提交或推送 Git。

## 2026-10-21

### 2026-10-21 20:16:31 +08:00 — 整理 ClearScreen 项目重要资料索引

- 在 `D:\BaiduSyncdisk\其它\系统秘钥\ClearScreen\` 新增项目基本信息、验证与复用说明、Apple D-U-N-S 申请记录，记录项目标识、备案/隐私政策入口及申请跟进状态；申请记录置于项目专用资料目录，不加入 Git。
- 更新 Wiki 入口，说明重要资料和签名备份的存放位置及使用前核对要求。
- 目录里原有 Android Release 密钥库、Gradle 签名配置和 lineage 已存在；本次没有重复搬运或覆盖。已与仓库副本逐项比对 SHA-256，三项均一致。
- 将本机 `output/pdf/` 中的 1.0.46 程序鉴别材料和用户手册 PDF 移入系统秘钥目录的 `软件著作权申请材料/`，并在合规核查页注明归档路径与版本局限。
- 验证：资料目录原有文件盘点、签名备份 SHA-256 一致性核对及 PDF 移动前后 SHA-256 保持一致通过；`git diff --check` 通过。未运行应用测试。
- 涉及文件：`docs/wiki/README.md`、`docs/wiki/应用商店合规核查.md`、`docs/wiki/CHANGELOG.md`；外部资料 `D:\BaiduSyncdisk\其它\系统秘钥\ClearScreen\项目基本信息.md`、`验证与复用说明.md`、`Apple-D-U-N-S-申请记录.md`、`软件著作权申请材料/净屏_程序鉴别材料_Android_1.0.46.pdf`、`软件著作权申请材料/净屏_用户操作手册_Android_1.0.46.pdf`。

### 2026-10-21 17:43:20 +08:00 — 构建并升级安装净屏 1.0.55 / 61 到 vivo

- 按用户要求执行 `:app:assembleRelease --offline --console=plain`，正式 Release 构建成功。APK 包名 `com.clearscreen.prototype`、版本 `1.0.55 / versionCode 61`、minSdk 24、targetSdk 36，证书 SHA-256 与净屏正式签名证书一致：`93c410454a8263736ff60bb6e27153b19f74f4c934f444531842f5162ccfa999`。
- 正式安装包位于 `android/app/build/outputs/apk/净屏.apk`，SHA-256 为 `8CF929AC25D2B219F965F9FFF6EEFEE5E5114910373EDAB3732925964E520BEA`；与 Gradle 原始 APK 哈希一致。ADB 无法正确处理中文路径，因此从同哈希的原始 APK 执行安装。
- vivo V2502DA（`10AG4M2KGF00527`）安装前为 `1.0.55 / 61`，执行 `adb install -r --no-streaming` 成功；安装后版本仍为 `1.0.55 / 61`，首次安装时间保持 `2026-09-20 15:33:13`，更新时间为 `2026-10-21 17:43:04`。未卸载应用或清空数据。
- 验证：Release 构建、APK 包名/版本/SDK 元数据、正式证书、产物哈希及 USB 升级安装通过。未启动应用或进行广告关闭功能验收，留待用户自行测试。
- 涉及文件/产物：`android/app/build/outputs/apk/release/app-release.apk`、`android/app/build/outputs/apk/净屏.apk`、`docs/wiki/CHANGELOG.md`。保留其他窗口工作区修改，未提交或推送 Git。

### 2026-10-21 17:37:08 +08:00 — 补充 WiFi 与彩云广告侧边叉号及扭动开屏布局

- 对照本轮五张截图检查候选路径：信息流普通 X 原先主要接受卡片右上角，窄广告行的正常触摸区域还可能被卡片高度占比限制拒绝；开屏组合只接受左上广告标记和原有跳转文案。新增同卡片右侧中部/右下角路径 `builtin.feed.close.ocr-right-edge`，要求同一无滚动/输入/敏感内容的有界卡片、广告标记、行动文案、新鲜局部 OCR、真实直接可点击小控件和重合 X。窄横幅保留屏幕尺寸/面积及 X 验证，避免把正常触摸区域仅因占行高比例较大过滤；“了解更多”纳入行动文案，不能单独作为广告证据。
- WiFi 视频海报新增 `builtin.popup.close.wifi-video-poster`：同一稳定浮层内必须有大幅有界创意、创意右上广告徽标、紧邻海报外侧右上的真实小 X，以及下方“点击图片 / 下载或跳转第三方应用 / 立即打开”完整组合；不借用背景 WiFi 列表的文字或按钮。支持含真实图片/视频子节点的局部原生创意容器，已有会员、横幅、灵珠和日历组合仍按原限制处理。
- 开屏支持紧邻跳过左侧的右上小广告标记，并补充“扭动手机开启详情 / 扭一扭开启详情 / 摇一摇开启详情”与底部第三方跳转披露组合；仍要求真实可用小跳过控件、无滚动内容和同窗新鲜证据。OCR 组合同时保留词框与行框，避免“广告 跳过3”合行后漏掉独立徽标；等待启用、业务教程和仅有动作提示的页面不满足条件，不点击广告正文或扭动入口。
- X 图形分析仅在已暴露的小型可点击节点内补充偏移、小尺寸、矩形留白及浅灰样式，保留四对角笔画、中心及背景对照；图形不单独授权动作。WiFi/彩云局部广告关闭以及完整扭动开屏可在普通点击确认未生效后，重新校验同一真实节点和局部/整窗 OCR，最多补一次短触摸。候选先通过既有门槛，再提高已确认局部广告弹窗角落和 WiFi 精确视频海报的优先级，避免先操作背景信息流。保留 `cll_close_click` 假叉号硬性拒绝、系统确认、敏感输入、提醒/闹钟保护和去重；手势结束不直接计为关闭成功。
- 验证：使用本机 JBR 21、离线执行 `:app:compileReleaseKotlin` 最终通过（22 秒）；`git diff --check` 通过。自动化测试、集成/端到端测试、手机功能/UI 验收均未执行，按用户要求由用户测试；没有启动或操作手机应用，未打包、安装或调整版本号，现有安装包未由本轮更新。编译成功不代表五张截图中的广告已实机关闭。
- 涉及文件：`android/app/src/main/java/com/clearscreen/prototype/backend/AdCloseIconDetector.kt`、`AdCreativeRules.kt`、`AdGeometryRules.kt`、`WifiPromotionRules.kt`、`ClearScreenAccessibilityService.kt`、`README.md`、`docs/wiki/README.md`、`docs/wiki/CHANGELOG.md`。保留其他任务的现有修改，未提交或推送 Git。

### 2026-10-21 13:07:41 +08:00 — 构建净屏 1.0.54 / 60 并完成 vivo 升级安装

- 按用户“安装 app”的要求，以当前工作区源码运行 `android\\gradlew.bat :app:assembleRelease`，正式构建成功。Gradle Release APK 与命名文件 `android/app/build/outputs/apk/净屏.apk` 均为包名 `com.clearscreen.prototype`、版本 `1.0.54 / versionCode 60`、minSdk 24、targetSdk 36；两个 APK 的 SHA-256 相同：`B636EB100B86DB84BB03F4D6B53090507A34EC57DF1BD1631B168FC814CB0FE0`。签名验证通过，证书 SHA-256 为 `93c410454a8263736ff60bb6e27153b19f74f4c934f444531842f5162ccfa999`。
- vivo V2502DA（`10AG4M2KGF00527`）原安装版本为 `1.0.54 / 60`，首次安装时间 `2026-09-20 15:33:13`。经用户在设备端完成系统确认后，`adb install -r --no-streaming` 返回 `Success`。安装后仍为 `1.0.54 / 60`，更新时间为 `2026-10-21 13:06:44`，首次安装时间仍为 `2026-09-20 15:33:13`；确认使用升级安装，没有卸载或清空数据。
- 验证：`:app:assembleRelease`、APK 包名/版本/SDK 元数据、正式签名、产物 SHA-256 核验及 USB 升级安装通过；安装后核对版本与首次安装时间。未打开净屏进行功能/UI 验收。
- 涉及文件/产物：`android/app/build/outputs/apk/release/app-release.apk`、`android/app/build/outputs/apk/净屏.apk`、`docs/wiki/CHANGELOG.md`。保留其他窗口的工作区修改，未提交或推送 Git。

### 2026-10-21 13:05:07 +08:00 — 准备 iOS EAS 云构建环境

- 增加 iOS 内部设备安装和 App Store Connect 两种 EAS 构建档案，配置本地 iOS build number 递增；补充根级生成的 `ios/` 目录忽略规则，并记录 Windows 云构建路径、签名和设备注册前提。
- 安装 EAS CLI 25.3.0（Node.js 24.18.0）；验证 EAS 档案 JSON、`npx expo config --type introspect --json` 中的主应用 bundle ID、build number 和 Safari 扩展目标，以及 `git diff --check` 均通过。
- 未登录 Expo/Apple 账号、未创建或关联 EAS 云项目、未生成签名证书/配置文件，未发起云构建、提交或安装。
- 涉及文件：`eas.json`、`app.json`、`.gitignore`、`README.md`、`docs/wiki/README.md`、`docs/wiki/CHANGELOG.md`。

### 2026-10-21 13:00:01 +08:00 — 收拢设置首页并合并相关内容

- 设置首页从平铺的记录、隐私、帮助、反馈、更新、诊断、备案和版本条目收拢为“服务与权限 / 隐私与数据 / 帮助与反馈 / 关于净屏”四个入口，保留品牌栏和底部导航的既有尺寸，不新增首页分组说明或按钮辅助文案。
- 新增设置分类内容页：“隐私与数据”集中记录保留、清空记录、隐私政策和恢复默认，保留周期改为页内四组选项，取消单独选择弹窗；“帮助与反馈”直接展示各平台常见问题，并提供意见反馈和 Android 诊断分享，取消帮助弹窗；“关于净屏”将版本与检查更新合到同一卡片，并保留 APP 备案号查询。隐私政策继续使用独立阅读页及既有撤回入口，清空和恢复默认继续确认，网络查询、邮件草稿和诊断分享仍只由用户主动触发。
- 三个新分类接入现有页面历史；顶部返回及 Android 系统返回按来源逐级返回，查看隐私政策后返回“隐私与数据”。分类内容沿用 `COMPACT_UI` 紧凑规格；同步共享设置页的 HarmonyOS 隔离试验调用，不新增鸿蒙原生能力。更新 README 和 Wiki 中的内容层级说明。
- 验证：主工程 `npx tsc --noEmit`、HarmonyOS 隔离试验本地 `tsc --noEmit`、`git diff --check` 通过。按用户非网页项目约定未运行自动化测试、未启动或操作应用进行功能/UI 验收；未打包、安装或调整版本号，实际页面效果由用户验收。
- 涉及文件：`src/screens/SettingsScreen.tsx`、`src/screens/SettingsDetailScreen.tsx`、`App.tsx`、`targets/harmony-bare-pilot/App.tsx`、`README.md`、`docs/wiki/README.md`、`docs/wiki/CHANGELOG.md`。保留其他任务现有修改，未提交或推送 Git。

### 2026-10-21 12:52:09 +08:00 — 精简设置页分组标题和空隙

- 删除设置页“关于”和“其他”标题，去掉两处标题占用的上下留白；保留卡片间原有间距、“隐私与数据”标题及所有设置项。
- 验证：`npx tsc --noEmit`、`git diff --check` 通过；未运行功能测试或手工 UI 验收，未打包或安装。
- 涉及文件：`src/screens/SettingsScreen.tsx`、`docs/wiki/CHANGELOG.md`。

### 2026-10-21 10:43:29 +08:00 — 统一页面顶部安全区域和留白

- 根据首页、记录详情、设置、隐私政策和服务与权限截图排查顶部布局：共享背景原来只有 6dp 固定顶部内边距，页面另叠加 0–34dp 不等的补偿，在已启用 edge-to-edge 的 Android 上没有读取实际状态栏和挖孔高度。
- Android/iOS 入口接入 Expo SDK 54 对应的 `react-native-safe-area-context`（锁定 5.6.2），使用初始窗口数据和后续原生安全区域更新；通过平台无关的 `ScreenInsetsContext` 提供顶部高度，避免共享组件直接依赖原生模块。共享背景统一按顶部安全区域加 16dp 排布内容，保留背景图全屏延伸，移除状态栏的固定背景色/非透明设置。
- 去除首页、应用列表、设置、应用详情、处理记录、记录详情、排除应用、自动跳过服务、服务与权限、隐私同意/阅读、iOS 首页及既有外观页重复的顶部补偿。同类页面统一起始留白，保留现有字体、图标、按钮、卡片内部间距、底部导航及操作逻辑。更新 README 和 Wiki，并明确只在用户要求时打包、安装或分发。
- 验证：`npx tsc --noEmit`、`git diff --check` 通过；依赖安装只新增安全区域库，没有执行生命周期构建脚本。未运行自动化测试或手工功能/UI 验收，未打包、未安装、未调整版本号；现有 APK 不包含本次修改，实际页面效果待用户确认。
- 涉及文件：`App.tsx`、`package.json`、`package-lock.json`、`src/components/ScreenInsets.ts`、`src/components/UI.tsx`、`src/screens/` 下上述 12 个页面样式、`README.md`、`docs/wiki/README.md`、`docs/wiki/CHANGELOG.md`。保留其他任务现有修改，未提交或推送 Git。

## 2026-10-20

### 2026-10-20 17:31:48 +08:00 — 补充中华万年历广告布局并完成 vivo 再升级

- 核对 vivo 安装为 1.0.52 / 58、服务运行且用户同意/总开关均开启。被动读取旧版摘要发现 WiFi 曾有被接受的普通节点点击，但随后 OCR 因窗口变化作废；这些摘要无法证明用户提供的五张广告截图已关闭。最近任务中的日历宿主为 `cn.etouch.ecalendar`，不是此前仅允许 WiFi 的横幅规则范围；没有主动打开或操作广告页面做验收。
- 扩展既有 `WifiPromotionRules` 中同款“惊喜等你拿 / 点击到落地页或第三方 APP / 关闭悬浮球可继续浏览应用”组合，仅允许 WiFi 万能钥匙和中华万年历两宿主。横幅比例下限调整为 2.4:1 以覆盖已提供约 2.7:1 的样式；关闭提示可为横幅紧邻下方、位置对齐的兄弟元素，但实际操作仍须验证整窗新鲜 OCR。支持带真实图片子节点的独立原生创意容器，并在没有滚动/输入/敏感内容且精确组合完整时允许广告专属根节点；不能从普通列表或别处广告文字补证据。会员红包与“玩转灵珠”仍仅限 WiFi，最多一次重新取证的短触摸额度保留。
- 新增中华万年历视频下载卡片右下角 X 窄规则：同一稳定局部卡片内须有视频、下方推荐/下载/广告脚注、视频上沿的滴滴数值版本、开发者公司及隐私/权限披露，再关联真实小型可点击 X；没有节点时不发送 OCR 坐标手势。只处理脚注 X，不点击中间的会员提示、“去看看”、领取、开通或下载按钮；普通会员页、其他宿主、顶部/中部业务按钮与缺失本卡片证据的样式均不满足条件。
- 日历主页节气卡片的单独“提醒”文字不再一概阻断广告：只在系统窗口事件确认 `cn.etouch.ecalendar.module.main.ui.MainActivity`、窗口 ID 一致、年月标题与日历/黄历/天气/我的四导航均存在时忽略该单独标签。复合提醒、闹钟/提醒控件 ID、提醒窗口、未知上下文及其他应用继续受保护，未取消系统确认保护。
- 收紧关闭结果判断：同应用换窗记为待确认；SDK 重建相同类型/资源 ID/边界的控件仍按在场处理，不能只因旧节点消失就增加成功统计。数值摘要扩展为 `wifi.*` 与 `calendar.*`，每个固定阶段只保留一条内存记录，动作含内置规则编号，不保存截图、节点或 OCR 文字，失去同意、解绑或销毁时清除。同步 README 和 Wiki；新增横幅跨宿主、相邻提示、日历脚注 X 及提醒保护正反例测试源码。
- 验证：`npx tsc --noEmit`、`:app:compileReleaseKotlin`、`:app:compileDebugUnitTestKotlin`、`:app:assembleRelease`、`git diff --check` 通过。按全局非网页项目约定只编译测试源码，没有运行自动化测试或手工功能验收。Release APK 应用名“净屏”、包名 `com.clearscreen.prototype`、版本 1.0.53 / versionCode 59、minSdk 24 / targetSdk 36；正式签名验证通过，证书 SHA-256 保持 `93c410454a8263736ff60bb6e27153b19f74f4c934f444531842f5162ccfa999`。命名 APK 与原始 APK 的 SHA-256 均为 `D4A504F079D37D20989AD3C11FDBBCA58B29DF038A0C1D888251CC2B17C4AFAF`。
- 第一次 USB 升级被 vivo 以 `INSTALL_FAILED_ABORTED: User rejected permissions` 拒绝；用户明确回复“继续升级”后，使用相同正式签名 APK 执行 `install -r --no-streaming` 成功。核对 vivo V2502DA（`10AG4M2KGF00527`）为 1.0.53 / 59、更新时间 2026-10-20 17:30:56、首次安装时间仍为 2026-09-20 15:33:13；没有卸载或清除数据。净屏服务自检 `running=true consent=true master=true`，无障碍启用列表保留净屏和联控，WiFi 的 `VIBRATE: ignore` 仍保持。升级后正常使用的被动摘要出现 `builtin.popup.close.resource-id` 被接受的节点动作、目标不再可见，以及其他帧 `windowStable=false / no-candidate`；不据此将五个具体广告都记为真机关闭成功，实际关闭仍待用户确认。
- 涉及文件：`android/app/src/main/java/com/clearscreen/prototype/backend/WifiPromotionRules.kt`、`ProtectedUiPolicy.kt`、`ClearScreenAccessibilityService.kt`、`android/app/src/test/java/com/clearscreen/prototype/backend/WifiPromotionRulesTest.kt`、`ProtectedUiPolicyTest.kt`、`android/app/build.gradle`、`app.json`、`README.md`、`docs/wiki/README.md`、`docs/wiki/CHANGELOG.md`、`android/app/build/outputs/apk/净屏.apk`。保留其他窗口现有修改，未提交或推送 Git。

### 2026-10-20 17:20:23 +08:00 — 限制 WiFi 万能钥匙震动并修正无效点击后的回退状态

- 用户再次反馈左下角“跳过”无效并连续震动。核对 vivo 上的系统震动历史，最近一条为 17:14:10.762、发起包 `com.snda.wifilocating`、持续约 1906ms；原 `VIBRATE` AppOp 为 `allow`。按用户停止异常震动的明确要求，仅将该包设置为 `ignore`，操作后及净屏升级后均回读确认，未改来电、全局或其他应用震动设置。该调整只作用于当前手机的 WiFi 万能钥匙；如需恢复，可将同一包的 `VIBRATE` 设回原值 `allow`。净屏普通 APK 没有据此获得禁止其他应用震动的系统权限。
- 修正一条可确认的失败路径：首次无障碍点击未带有可触摸的 OCR 证据时，原来即使核对到控件仍在，也不会标记该目标未生效，导致后来获得完整新鲜 OCR 仍无法使用一次性短触摸回退。现在独立记录点击失败；左下角开屏按钮失败后优先补采新 OCR，实际发送短触摸前仍重新验证前台、同一窗口、显示区域、广告证据与真实控件；补点额度仍只有一次，不放宽未知页面坐标点击或系统确认保护。此代码路径并未由本次短时广告现场复现确认，不能据此断言是此次漏跳的唯一原因。
- WiFi 诊断增加最近扫描、截图请求、OCR 结果/判定及动作/结果摘要，按固定阶段各保留一条进程内记录，切回桌面后仍可通过服务 dump 读取；只记录计数、耗时、窗口编号和固定状态，不保留节点、截图或 OCR 文字，解绑、销毁或撤回隐私同意时清除。同步根 README 和 Wiki，区分 APK 能力与本机单应用权限调整。
- Android 版本升至 1.0.52 / versionCode 58。验证：`npx tsc --noEmit`、`:app:compileReleaseKotlin`、`:app:assembleRelease` 和 `git diff --check` 通过。Release APK 应用名“净屏”、包名 `com.clearscreen.prototype`、minSdk 24 / targetSdk 36；正式签名验证通过，证书 SHA-256 仍为 `93c410454a8263736ff60bb6e27153b19f74f4c934f444531842f5162ccfa999`。命名文件 `android/app/build/outputs/apk/净屏.apk` 与原始 APK 的 SHA-256 均为 `D5284B848E10065BF8AAD1E743F895903DDF370548DBFF0F784903D788644570`。
- 使用同签名 ASCII 原始 APK 通过 USB `install -r --no-streaming` 成功升级 vivo V2502DA（`10AG4M2KGF00527`）；安装后版本 1.0.52 / 58，更新时间 2026-10-20 17:20:12，首次安装时间仍为 2026-09-20 15:33:13。服务自检为 `running=true consent=true master=true`，系统启用列表保留净屏与联控，无待绑定或崩溃项；未卸载或清除数据。没有运行自动化测试、没有打开或操作广告页做功能验收；权限配置与安装核对不等于该广告已跳过或震动体感已验收。
- 涉及文件：`android/app/src/main/java/com/clearscreen/prototype/backend/ClearScreenAccessibilityService.kt`、`android/app/build.gradle`、`app.json`、`README.md`、`docs/wiki/README.md`、`docs/wiki/CHANGELOG.md`、`android/app/build/outputs/apk/净屏.apk`。保留其他窗口的现有修改，未提交或推送 Git。

### 2026-10-20 17:09:02 +08:00 — 补充 WiFi 万能钥匙三种促销浮层并完成 vivo 升级

- 核对手机升级前仍为 1.0.49 / 55；此前 1.0.50 安装被系统拒绝，本次将上轮无效点击/防连点修正与新规则一并打包为 1.0.51 / versionCode 57。保留工作区其他窗口的现有修改，未提交或推送 Git。
- 新增 `WifiPromotionRules`，仅匹配 `com.snda.wifilocating` 的三种已提供截图组合：会员红包同时要求恭喜到账标题、SVIP 月卡专属红包、优惠后/原价金额、领取并使用与上方独立叉号；第三方横幅同时要求创意内“惊喜等你拿”、落地页/第三方提示、紧邻下方的关闭悬浮球提示及自身右侧叉号；游戏悬浮入口仅匹配右下紧凑创意中的“玩转灵珠”及自身上方叉号。不能仅靠会员、游戏、红包、屏幕角落或页面别处的广告字样点击。
- 三类候选均要求同一无滚动、无输入、无敏感文案的稳定独立无障碍子树，新鲜局部 OCR、真实图片/WebView 创意以及与 X 图形重合的直接可点击小控件；叉号没有真实节点、归属不明或局部节点变化时不操作。节点点击无效时，在重新验证同一节点和局部 OCR 后最多补发一次 90ms 短触摸，继承前一轮动作额度保护；不长按、不滑动，不把触摸完成直接计为关闭成功。
- 增加 `wifi-ocr-no-action` 数值诊断，记录窗口、节点/小控件/叉号/创意计数、窗口稳定性与拒绝原因，方便短时广告自然出现后排查；不保存截图或 OCR 文字。新增正例与普通会员页、其他包、背景文字、相邻按钮、过大/错位目标、整页游戏区域等负例测试源码，完成编译；按全局项目测试约定没有执行自动化测试或手工页面验收。
- 同步 README、Wiki、应用内及 Wiki 隐私说明中的真实节点短触摸回退；未新增数据采集种类、上传或持久化界面内容，政策版本保持不变，公开政策网站本次未操作。
- 验证：`npx tsc --noEmit`、`:app:compileReleaseKotlin`、`:app:compileDebugUnitTestKotlin`、`:app:assembleRelease` 与 `git diff --check` 通过。Release APK 应用名“净屏”、包名 `com.clearscreen.prototype`、版本 1.0.51 / 57、minSdk 24 / targetSdk 36；正式签名验证通过，证书 SHA-256 保持 `93c410454a8263736ff60bb6e27153b19f74f4c934f444531842f5162ccfa999`。`android/app/build/outputs/apk/净屏.apk` 与原始 APK 的 SHA-256 均为 `D2446C1B1C2CF38AD6A3107DE8D1F48EB8EC333206D78A9AAD0D7272D4A37044`。
- 经用户同意继续升级后，通过 USB `install -r --no-streaming` 成功安装到 vivo V2502DA（`10AG4M2KGF00527`）；安装后版本为 1.0.51 / 57，更新时间 2026-10-20 17:08:54，首次安装时间仍为 2026-09-20 15:33:13，没有卸载或清除数据。系统无障碍仍启用且服务位于 Bound services，服务自检为 `running=true consent=true master=true`。没有操作目标广告页面，安装成功不代表这三种广告已经通过真机关闭验收。
- 涉及文件：`android/app/src/main/java/com/clearscreen/prototype/backend/WifiPromotionRules.kt`、`ClearScreenAccessibilityService.kt`、`android/app/src/test/java/com/clearscreen/prototype/backend/WifiPromotionRulesTest.kt`、`android/app/build.gradle`、`app.json`、`src/screens/PrivacyPolicyScreen.tsx`、`README.md`、`docs/wiki/README.md`、`docs/wiki/隐私政策.md`、`docs/wiki/CHANGELOG.md`、`android/app/build/outputs/apk/净屏.apk`。

### 2026-10-20 16:56:48 +08:00 — 修正左下角跳过的无效点击回退与重复尝试

- 读取已连接 vivo V2502DA 的现有日志：1.0.49 在 15:51:33 对 WiFi 万能钥匙的左下角“跳过”发送过一次 `builtin.skip.splash.ocr-creative` 点击，系统返回接受，但 650ms 后仍判定动作未生效。系统震动历史的发起包为 `com.snda.wifilocating`，其中 15:50:39、15:51:13、15:51:28 的多脉冲震动均早于这次净屏点击；不把这些连续震动归因于净屏连点，也未改动系统震动设置。
- 新增 `AdActionAttemptGuard`：发送前占用额度，系统拒绝动作也保留去重；同一窗口/资源 ID/控件类型/边界持续出现时，不随倒计时、OCR 文案或规则变化重复点击。目标连续 30 秒未被观察到才释放额度，最多缓存 80 个目标；节流使用单调时钟。中国移动限定 OCR 开屏同步按窗口去重，避免倒计时文字变化产生重复手势。
- 左下角开屏跳过在无障碍点击被拒绝或核对后仍可见时，可在新鲜同窗 OCR、同创意广告证据和同一真实小型可点击控件均再次确认后，对该节点中心短触摸一次。OCR 跳过文字须完整落在目标内，目标自身不能带业务/促销文案；不存在真实节点、敏感输入、系统确认、页面/显示尺寸变化时不发送。回退后无论失败、取消还是发送被拒绝都不连点；手势完成不直接计为关闭成功，仍检查目标是否消失。失败后优先补采新 OCR，省去一次无 OCR 的固定延时重扫。
- Android 版本升至 1.0.50 / versionCode 56；同步根 README 与 Wiki。新增动作额度状态测试源码，完成测试源码编译，但按全局项目测试约定未运行自动化测试、未打开目标广告或进行手工功能验收。
- 验证：`npx tsc --noEmit`、`:app:compileReleaseKotlin`、`:app:compileDebugUnitTestKotlin`、`:app:assembleRelease` 和 `git diff --check` 通过。Release APK 应用名“净屏”、包名 `com.clearscreen.prototype`、版本 1.0.50 / 56、minSdk 24 / targetSdk 36；正式签名验证通过，证书 SHA-256 保持 `93c410454a8263736ff60bb6e27153b19f74f4c934f444531842f5162ccfa999`。`android/app/build/outputs/apk/净屏.apk` 与原始 APK 的 SHA-256 均为 `26CCB76378BA70197C17F9C7683A59E045B98727FF9D168FE74109323B2E60D0`。
- 升级安装未完成：Windows ADB 传输中文文件名失败后，使用同哈希的 ASCII 原始 APK 执行 `install -r --no-streaming`，vivo 返回 `INSTALL_FAILED_ABORTED: User rejected permissions`。核对手机仍为 1.0.49 / 55，首次安装时间仍为 2026-09-20 15:33:13；没有卸载、清除数据或代替用户确认系统安装权限，待手机端允许后再重试。
- 涉及文件：`android/app/src/main/java/com/clearscreen/prototype/backend/AdActionAttemptGuard.kt`、`ClearScreenAccessibilityService.kt`、`android/app/src/test/java/com/clearscreen/prototype/backend/AdActionAttemptGuardTest.kt`、`android/app/build.gradle`、`app.json`、`README.md`、`docs/wiki/README.md`、`docs/wiki/CHANGELOG.md`、`android/app/build/outputs/apk/净屏.apk`。

### 2026-10-20 15:45:08 +08:00 — 补齐左下角跳过与贴边悬浮广告识别

- 为左下角全屏“跳过”增加独立候选：要求当前窗口的新鲜 OCR、大幅可见创意内同时存在明确广告标记和跳转/详情行动文案、页面不可滚动，并且命中小型真实可点击控件；不使用 OCR 坐标。
- 为贴边悬浮创意增加包名无关的关闭路径：真实小型关闭控件须贴合图片/视频或有界非滚动浮层的右边缘，并在同一创意内部的新鲜 OCR 中具备明确广告标记，或至少两项促销线索和行动文案。页面其他位置的广告字样不为该控件补证据；局部证据不足时不点击。
- 补充“点此领取”“点击到落地页”“点击卡片跳转”“免单”“超1分钟”等 OCR 词项与位置/策略回归测试。Android 版本升至 1.0.49 / versionCode 55。
- 验证：`:app:testDebugUnitTest`、`:app:assembleRelease`、`git diff --check` 通过。Release APK 包名 `com.clearscreen.prototype`、应用名“净屏”、版本 1.0.49 / 55、minSdk 24 / targetSdk 36；正式签名验证通过，证书 SHA-256 为 `93c410454a8263736ff60bb6e27153b19f74f4c934f444531842f5162ccfa999`。命名 APK 与 Gradle 原始产物 SHA-256 均为 `013193D84A1A86AC1B73667968EDB51CCE62243DAB0291EF5D766F8161E2B86E`。
- 通过 USB `install -r` 升级安装到 vivo V2502DA，核对安装版本 1.0.49 / 55；系统无障碍开关仍启用且净屏服务已绑定。未打开这四张截图对应的目标页面复现，因此不代表这些广告均已通过实机识别验收；局部 OCR 证据不足的悬浮促销贴片仍会保守跳过。
- 涉及文件：`android/app/src/main/java/com/clearscreen/prototype/backend/ClearScreenAccessibilityService.kt`、`AdDecisionPolicy.kt`、`AdGeometryRules.kt`、对应单元测试、`android/app/build.gradle`、`app.json`、`README.md`、`docs/wiki/README.md`、`docs/wiki/CHANGELOG.md`、`android/app/build/outputs/apk/净屏.apk`。

### 2026-10-20 14:36:15 +08:00 — 补齐全屏广告关闭通用识别回退

- 视频播放器仍优先使用更严格的视频广告规则；只有该规则没有找到安全候选时，才继续评估包名无关的通用跳过、弹窗和卡片规则。专用规则命中时不扩大到其他候选。
- 为全屏广告中的普通“关闭”增加受限通用路径：需要当前窗口的新鲜 OCR、右上角紧凑真实可点击控件、控件落在大幅视觉创意中，并在同一创意内识别到明确广告标记；不使用屏幕坐标点击。
- Android 版本升至 1.0.48 / versionCode 54；Release 安装包：`android/app/build/outputs/apk/净屏.apk`。SHA-256：`F0537DAA828B1C31FED57C1CDCA4D445A334C4BC4878471B1DB9648938D21AAE`。包名、版本、SDK 与正式签名核验通过；命名 APK 与 Gradle 原始 APK 哈希一致。
- 验证：`:app:compileReleaseKotlin`、`:app:compileDebugUnitTestKotlin`、`:app:assembleRelease`、APK 元数据/签名核验及 `git diff --check` 通过；未运行自动化测试，未在三个广告页面复现，未安装手机。本次不代表爱奇艺、中国移动或百度地图广告已通过真机验收。
- 涉及文件：`android/app/src/main/java/com/clearscreen/prototype/backend/ClearScreenAccessibilityService.kt`、`AdDecisionPolicy.kt`、`AdGeometryRules.kt`、对应三份测试文件、`android/app/build.gradle`、`app.json`、`README.md`、`docs/wiki/README.md`、`docs/wiki/CHANGELOG.md`、`android/app/build/outputs/apk/净屏.apk`。

### 2026-10-20 14:11:15 +08:00 — 通用识别横向窄广告条

- 新增通用窄横幅候选几何：非滚动局部区域须满足横向广告条形状；仍需同区域明确广告标记、OCR 小叉号与右侧小型独立无障碍点击控件精确重合。大面积点击区、相邻加号/工具键、仅凭位置或横幅外观均不操作，不新增未知页面坐标手势。
- 增加横幅正例、较高内容卡片负例、相邻加号负例和过大点击区负例测试；Android 版本同步升至 1.0.47 / versionCode 53。
- 验证：`:app:testDebugUnitTest`、`:app:assembleRelease`、`git diff --check` 通过。Release APK 包名 `com.clearscreen.prototype`、版本 1.0.47 / 53、minSdk 24 / targetSdk 36；正式签名验证通过，证书 SHA-256 与上一版相同。命名 APK 与 Gradle 原始产物 SHA-256 均为 `059CFC7C512ED5D04E1CCE5E5D4DB9492902646483283B5BE6DF75AFB1EAE574`。
- 通过 `adb install -r` 将该版本升级安装到已连接 vivo V2502DA，安装前为 1.0.44 / versionCode 50；安装后核对 1.0.47 / 53，系统无障碍服务仍启用。未在这三种广告页面上复现并确认关闭结果，不视为广告真机验收通过。
- 涉及文件：`android/app/src/main/java/com/clearscreen/prototype/backend/AdGeometryRules.kt`、`ClearScreenAccessibilityService.kt`、`android/app/src/test/java/com/clearscreen/prototype/backend/AdGeometryRulesTest.kt`、`android/app/build.gradle`、`app.json`、`README.md`、`docs/wiki/README.md`、`docs/wiki/CHANGELOG.md`、`android/app/build/outputs/apk/净屏.apk`。

### 2026-10-20 01:15:27 +08:00 — 统一正式 APK 文件名为“净屏”

- 保留现有 Android 桌面/安装器应用名“净屏”和 `com.clearscreen.prototype` 包名；正式 Release 构建完成后自动复制签名 APK 为 `android/app/build/outputs/apk/净屏.apk`，Gradle 原始产物保持不变。
- 更新根目录 README 和 Wiki 打包说明，明确对外发送和安装使用 `净屏.apk`。
- 验证：`:app:assembleRelease` 成功；命名 APK 与 Gradle 原始 APK 的 SHA-256 一致，正式签名验证通过，包名 `com.clearscreen.prototype`、版本 `1.0.46` / versionCode `52`、应用显示名“净屏”核对通过；`git diff --check` 通过。未安装手机。
- 涉及文件：`android/app/build.gradle`、`README.md`、`docs/wiki/README.md`、`docs/wiki/CHANGELOG.md`、`android/app/build/outputs/apk/净屏.apk`。

## 2026-10-19

### 2026-10-19 16:33:28 +08:00 — 构建并核验当前 Release APK

- 在当前工作树运行 `android\gradlew.bat :app:assembleRelease`，构建成功；Gradle 判断 APK 与当前输入一致，相关打包任务为 `UP-TO-DATE`，产物位于 `android/app/build/outputs/apk/release/app-release.apk`。
- 核对包名 `com.clearscreen.prototype`、版本 `1.0.46` / versionCode `52`；`apksigner verify --print-certs` 通过，正式证书 SHA-256 为 `93c410454a8263736ff60bb6e27153b19f74f4c934f444531842f5162ccfa999`。APK SHA-256：`866157FE9F3D07938397A253589528660CDA6E45C4A7DD1B7C7A52AABE030180`。
- 验证：`npx tsc --noEmit`、Release 构建、包名/版本核对、签名验证和 `git diff --check` 均通过。此次未安装到手机或上传应用商店。
- 涉及文件：`android/app/build/outputs/apk/release/app-release.apk`、`docs/wiki/CHANGELOG.md`。

## 2026-10-18

### 2026-10-18 15:55:48 +08:00 — 修正应用列表导航遮挡并更新小米截图

- 应用管理页给可滚动区域留出底部导航高度，并将应用图标从 42 调至 36、单行高度从 62 调至 54、名称字号从 15 调至 14，避免列表行透在底部导航下；首页和其他页面未改。
- 使用当前源码运行 `:app:assembleRelease`，构建成功；通过 `adb install -r` 升级安装到 `ClearScreen_Store`，核对 `com.clearscreen.prototype` / 1.0.46 / versionCode 52。`apksigner verify --print-certs` 通过，证书指纹保持 `93c41045…fa999`；已确认隐私选择保留，系统无障碍服务仍未授权。
- 用新版重新采集并目视复核 4 张截图，替换 `output/xiaomi-screenshots/` 中原来的图：1080×1920 PNG，每张小于 1 MB，应用列表首屏可见 8 个完整条目且导航未遮挡。AVD 屏幕尺寸已恢复 1080×2424。
- 验证：Release 构建、`adb install -r`、包名/版本核对、签名核验、截图尺寸/体积/目视检查、`git diff --check` 均通过。未运行自动化测试；未上传 APK 或截图到小米控制台。
- 涉及文件：`src/screens/AppsScreen.tsx`、`docs/wiki/应用商店合规核查.md`、`docs/wiki/CHANGELOG.md`、`output/xiaomi-screenshots/01-home.png`、`output/xiaomi-screenshots/02-app-management.png`、`output/xiaomi-screenshots/03-help.png`、`output/xiaomi-screenshots/04-privacy-policy.png`。
### 2026-10-18 15:46:09 +08:00 — 采集小米应用商店功能截图并复核前置条件

- 用户本人在 Android 16 AVD 中选择“同意并继续”后，从已安装的 Release APK 1.0.46 / versionCode 52 采集首页、应用管理、使用帮助和隐私政策共 4 张原始 PNG 截图，保存于 `output/xiaomi-screenshots/`；截图未编辑。
- 检查四张图均为 1080×1920 且每张小于 1 MB，并逐张目视复核。应用管理页底部导航会透出下一行内容；意见反馈入口在当前 AVD 中启动 Gmail 欢迎页，未配置邮件帐户，因此未作为截图，也未创建或发送邮件。
- AVD 屏幕尺寸已恢复为 1080×2424。没有开启无障碍权限、修改应用代码、重建 APK 或上传小米控制台。
- 验证：PNG 尺寸/文件头、体积及目视复核通过；`git diff --check`（本次两份 Wiki 文件）通过。未运行自动化测试、未构建 APK、未上传或提交应用商店。
- 涉及文件：`docs/wiki/应用商店合规核查.md`、`docs/wiki/CHANGELOG.md`、`output/xiaomi-screenshots/01-home.png`、`output/xiaomi-screenshots/02-app-management.png`、`output/xiaomi-screenshots/03-help.png`、`output/xiaomi-screenshots/04-privacy-policy.png`。

### 2026-10-18 15:41:43 +08:00 — 统一首页、应用页和设置页 Logo 尺寸

- 将应用页紧凑品牌栏的 Logo 从 54×54 调整为与首页、设置页一致的 68×68，并同步增加品牌栏最小高度，避免图标挤压标题或操作按钮。
- 验证：`npx tsc --noEmit`、`git diff --check` 均通过；未执行构建或设备视觉验收。
- 涉及文件：`src/components/UI.tsx`、`docs/wiki/CHANGELOG.md`。

### 2026-10-18 15:41:02 +08:00 — 调整无障碍权限页标题顶部留白

- 将“自动跳过服务”权限页整体内容起始位置下移，避开状态栏和前置摄像头区域。
- 验证：`git diff --check` 通过；未执行构建或真机视觉验收。
- 涉及文件：`src/screens/PermissionsScreen.tsx`、`docs/wiki/CHANGELOG.md`。

### 2026-10-18 15:40:27 +08:00 — 调整隐私政策页标题顶部留白

- 增加隐私政策同意页标题上方留白，避免标题贴近或挤入状态栏；正文卡片与底部操作保持不变。
- 验证：`git diff --check` 通过；未执行构建或真机视觉验收。
- 涉及文件：`src/screens/PrivacyPolicyScreen.tsx`、`docs/wiki/CHANGELOG.md`。

### 2026-10-18 15:33:57 +08:00 — 准备软著申请材料并复核小米截图前置状态

- 从当前 1.0.46 源码整理程序鉴别材料（首尾各 30 页，共 60 页）和现有 Notion 用户手册，生成 2 页 PDF；仅保存在本机 output/pdf/，该目录通过本地 Git exclude 排除，未上传。
- 检查 PDF 页数和逐页代码行号并渲染目视核对：程序材料 60 页，每页 50 行，覆盖源码首 1–1500 行与末 1708–3207 行；手册 2 页，每页 30 行以上。未见截断或版面溢出。中国版权保护中心申请尚缺申请人、权属链、主体证明和签章，未登录、未提交。
- 更新合规核查 Wiki，修正线上政策状态，记录软著材料准备情况及 AVD 隐私选择待本人完成。小米 4 张真实功能截图仍未采集。
- 验证：Poppler 页数检查、PDF 文本提取/页行数检查及关键页面渲染目视检查通过；未运行自动化测试，未提交软著或应用商店。
- 涉及文件：docs/wiki/应用商店合规核查.md、docs/wiki/CHANGELOG.md、本机忽略目录 output/pdf/净屏_程序鉴别材料_Android_1.0.46.pdf、output/pdf/净屏_用户操作手册_Android_1.0.46.pdf。


### 2026-10-18 15:26:30 +08:00 — 安装 1.0.46 到独立 Android 16 AVD 并复核小米发布前置条件

- 未改动旧虚拟机和手机数据；在新建的 `ClearScreen_Store` AVD 安装已验签的 `com.clearscreen.prototype` 1.0.46 / versionCode 52。首次启动停在隐私政策选择页，未替用户同意；功能截图尚未采集。
- 小米开发者中心已登录，应用列表为空；开发者资料页显示企业认证未完成，营业执照号和照片未提交。未创建应用、上传 APK 或提交审核。
- 核对中国版权保护中心官方 R11 流程；登记门户需要登录，申请人、权属链、主体证明和签章材料待权利人确认。更新 Notion 软著准备页，并链接现有 1.0.46《用户操作手册》。没有创建账号、填报或提交申请。
- 验证：ADB 安装成功，核对目标 AVD、包名、versionName/versionCode；小米开发者信息页和软著官方指南已现场核对；Notion 页面更新后重新读取确认。未执行自动化测试或应用商店提交。
- 涉及文件：`docs/wiki/应用商店合规核查.md`、`docs/wiki/CHANGELOG.md`；Notion 页面：[软件著作权登记准备｜净屏 Android 版](https://app.notion.com/p/3fd64df90ef18160aadafd1a627354ff?pvs=204)。

### 2026-10-18 15:00:16 +08:00 — 同步隐私政策并构建最新 Android Release APK

- 将 Android 版本从 `1.0.45 / versionCode 51` 升至 `1.0.46 / versionCode 52`，同步 `android/app/build.gradle` 与 `app.json`。补充 Android 应用内隐私政策对限定中国移动开屏无障碍手势的说明，保持与 `2026-10-16-v1` 线上政策一致；未改变实际处理逻辑。正式 Release APK 重建成功，包名/版本/minSdk/targetSdk、签名和 SHA-256 已核验；未安装真机或进行广告行为回归。
- 公开隐私政策从线上 `2026-10-13-v1` 更新并部署为 `2026-10-16-v1`，与应用内、源码和 Wiki 一致；补齐 APP 备案号、限定中国移动开屏手势识别范围、手动检查更新网络说明和当前隐私联系邮箱。Sites 第 2 版部署成功，并在内置浏览器重新载入验证页面内容。
- 在 ClearScreen Notion 项目下创建并复核“软件著作权登记准备｜净屏 Android 版”子页面，整理项目资料、国家版权局官方申请要求、材料清单及尚待权利人确认的信息；软著申请尚未提交。腾讯云 APP 备案号、域名和状态已核对。
- 验证：`:app:assembleRelease` 成功；`aapt` 包名/版本/SDK 元数据、`apksigner` 签名、APK 文件 SHA-256、包内政策版本与隐私邮箱标记、线上政策页面、主仓库 `git diff --check` 均核验通过。未执行自动化测试、真机安装或应用商店提交。
- 涉及文件：`src/screens/PrivacyPolicyScreen.tsx`、`android/app/build.gradle`、`app.json`、`docs/wiki/应用商店合规核查.md`、`docs/wiki/CHANGELOG.md`、`C:\develop\ClearScreenPrivacyPolicy\dist/index.html`。公开政策源码提交：`ec8bfabc80d73645a02828c08f20a58bfb8b8407`；APK SHA-256：`73FA2C9BBF7D9E4EB3116F56985466E07018AEF19461CCA2B26D614D1FCFA0F2`。Notion 子页面：[软件著作权登记准备｜净屏 Android 版](https://app.notion.com/p/3fd64df90ef18160aadafd1a627354ff?pvs=204)。

### 2026-10-18 02:04:08 +08:00 — 完成小米应用商店发布前合规核查，暂不上传

- 对照小米开发者生态政策、上架要求、隐私合规标准和应用创建指南；确认公开隐私页仍为 2026-10-13-v1，且旧版网络权限说明和隐私邮箱与当前源码不一致。现有 Release APK 为 1.0.45 / versionCode 51，早于最新联系邮箱源码修改。
- 记录净屏自动关闭第三方应用广告与小米“干扰其他应用正常使用”条款之间的审核风险；需平台预审确认。仓库未找到软著/电子版权证明和至少 4 张商店截图；开发者后台当前显示登录/注册，未上传或提交。
- 验证：对 APK 执行 aapt 包名/版本/SDK/权限与 ABI 核验、apksigner 签名验证；现场打开公开隐私页核对版本、网络说明及邮箱。未执行构建、测试或小米设备兼容性测试。
- 涉及文件：docs/wiki/应用商店合规核查.md、docs/wiki/CHANGELOG.md。

### 2026-10-18 01:56:32 +08:00 — 配置用户反馈与隐私联系邮箱

- 应用内意见反馈改用 support@cekoro.cn；Android/iOS 隐私政策及个人信息权利请求改用 contact@cekoro.cn。同步更新 Wiki 与公开隐私政策静态页；未使用无对应普通用户场景的商务、创始人和人事地址。
- 这次只修正联系渠道，未改变个人信息处理目的、方式或范围，因此保留 `2026-10-16-v1` 政策版本，不触发用户重新同意。邮箱由负责人提供，未验证收件能力；公开静态页尚未部署，商店控制台未修改。
- 验证：`npx tsc --noEmit`、主仓库 `git diff --check`、公开政策仓库 `git diff --check` 均通过；未执行 Android 构建、真机或商店控制台验收。
- 涉及文件：`src/privacy.ts`、`src/screens/SettingsScreen.tsx`、`src/screens/PrivacyPolicyScreen.tsx`、`docs/wiki/隐私政策.md`、`docs/wiki/应用商店合规核查.md`、`docs/wiki/CHANGELOG.md`、`C:\develop\ClearScreenPrivacyPolicy\dist\index.html`。

### 2026-10-18 01:36:53 +08:00 — 准备 vivo 提审版本并完成发布前核查

- 将 Android 发布版本从 `1.0.44` / `versionCode 50` 升至 `1.0.45` / `versionCode 51`，生成对应正式签名 Release APK。
- 对照 vivo 2026-09-23 版审核规范、应用分类和企业注册材料要求；核实公开隐私页线上仍为 `2026-10-13-v1`，而应用内为 `2026-10-16-v1`。将本地公开政策静态页更新到当前政策内容，补齐无障碍手势、检查更新网络请求和备案号；因 Sites 账户中无此站点项目，未部署。
- 记录控制台企业认证审核未通过及需补充的视频材料，未接受平台协议、未上传主体材料、APK 或提交审核。
- 验证：`npx tsc --noEmit` 通过；`:app:assembleRelease` 成功；`aapt` 包名/版本/SDK/权限核验、`apksigner` 签名核验、`git diff --check` 和本地政策关键内容检查通过。未安装真机、未做广告行为回归、未运行 vivo 隐私/安全自检。
- 涉及文件：`android/app/build.gradle`、`app.json`、`docs/wiki/应用商店合规核查.md`、`docs/wiki/CHANGELOG.md`、`C:\develop\ClearScreenPrivacyPolicy\dist\index.html`。

## 2026-10-18

### 2026-10-18 01:20:04 +08:00 — 补充净屏 APP 备案信息

- 根据负责人提供的腾讯云控制台截图，在设置“关于”中展示 `粤ICP备2026144630号-4A`，点击后打开工信部备案查询；同步加入应用内隐私政策、README 和 Wiki，并把备案待办更新为已通过。备案域名为 `clearscreen.cekoro.cn`，状态显示正常；这不代表应用商店审核通过。
- 公开隐私政策的本地静态页源码已加入备案号。Sites 列表未找到对应项目，按仓库配置的项目 ID 查询也返回不存在，因此没有部署，线上页面仍待核对。
- 验证：主项目与公开政策仓库的 `git diff --check` 均通过；未执行自动化测试、构建或真机验收。
- 涉及文件：`src/privacy.ts`、`src/screens/SettingsScreen.tsx`、`src/screens/PrivacyPolicyScreen.tsx`、`README.md`、`docs/wiki/README.md`、`docs/wiki/隐私政策.md`、`docs/wiki/应用商店合规核查.md`、`docs/wiki/CHANGELOG.md`、`C:\develop\ClearScreenPrivacyPolicy\dist\index.html`。

## 2026-10-17

### 2026-10-17 00:32:23 +08:00 — 撤回未确认的设置页分组调整

- 按用户说明先提供层级目录供其评估，将上一轮对设置页的分组及排序恢复为原样；不改变任何功能入口或动作。
- Wiki 同步恢复为不限定分组名称的功能说明。
- 验证：未执行。
- 涉及文件：`src/screens/SettingsScreen.tsx`、`docs/wiki/README.md`、`docs/wiki/CHANGELOG.md`。

### 2026-10-17 00:14:08 +08:00 — 整理设置页功能分组

- 将“关于”拆分为“帮助与反馈”和“关于”，把使用帮助、意见反馈、Android 诊断报告与版本信息、检查更新分别归组；把“其他”更名为“设置管理”，恢复默认继续放在页面末尾。
- Wiki 同步记录设置页的分组顺序与各组内容。
- 验证：`git diff --check` 通过；未执行自动化测试或真机视觉验收。
- 涉及文件：`src/screens/SettingsScreen.tsx`、`docs/wiki/README.md`、`docs/wiki/CHANGELOG.md`。

### 2026-10-17 00:11:37 +08:00 — 移除冗余欢迎页

- 首次同意隐私政策后直接进入“自动跳过服务”权限页，保留“稍后再说”进入首页的选择；已完成引导的用户仍直接进入首页。
- 删除仅展示品牌图与开始/稍后按钮的独立欢迎页，避免在隐私同意和权限设置之间增加空白步骤。
- 验证：`git diff --check`；未执行 TypeScript 检查、构建或真机验收。
- 涉及文件：`App.tsx`、`src/screens/WelcomeScreen.tsx`（删除）、`src/assets.ts`、`README.md`、`docs/wiki/README.md`、`docs/wiki/CHANGELOG.md`。

## 2026-10-16

### 2026-10-16 15:17:29 +08:00 — 修复中国移动消息页广告被保护规则拦截

- 从 Vivo 的中国移动消息页日志确认，整页此前因“话费账单提醒”“卡券到账提醒”“专属提醒不错过”中的通用“提醒”字样被误判为时间敏感界面，广告识别在候选阶段前即被阻断。现在只在中国移动包名下从保护判断中移除这三段固定消息文案；其他闹钟/计时文字、受保护窗口和控制资源 ID 仍保持拦截。
- 同页内嵌广告的 X 是独立可点击节点，位于局部卡片右上；更新后通过同卡片 OCR 广告标记和图形 X 识别，`builtin.popup.close.embedded-ad-card` 执行后复查确认目标消失，日志记为 `result=success`。这是本次 Vivo 消息卡片的实际关闭验证。
- 版本更新为 `1.0.44` / `versionCode 50`。`:app:assembleRelease --offline` 成功，`git diff --check` 通过；未执行自动化测试。
- 对 Vivo V2502DA 使用 `adb install -r` 升级返回 `Success`；设备确认版本 `1.0.44` / `50`，更新时间 `2026-10-16 15:16:48`，首次安装时间仍为 `2026-09-20 15:33:13`；系统无障碍列表仍启用净屏，服务能力为 `161`（含手势）。APK SHA-256：`E3835CE17E07F5C0BE7AC1DE68D294C1D2ACD91656550A0E4B8A7F3895D99133`。
- 本轮未重新复现 55GB 全屏开屏；包内保留其限定的同包、同窗口和新鲜 OCR 修正，因此本轮的真机关闭验证只覆盖消息卡片。
- 涉及文件：`android/app/src/main/java/com/clearscreen/prototype/backend/ProtectedUiPolicy.kt`、`android/app/build.gradle`、`app.json`、`README.md`、`docs/wiki/README.md`、`docs/wiki/CHANGELOG.md`。

### 2026-10-16 14:27:59 +08:00 — 修正中国移动 55GB 开屏倒计时识别

- 为 `com.greenpoint.android.mc10086.activity` 增加“55GB大流量卡、39元、40GB云盘、点击跳转详情页面”同屏组合识别；即使大图未暴露为常见图片控件或无障碍树带滚动标记，也需右上角小型倒计时控件匹配。OCR 身份校验不再因该小型跳过控件的数字/启用状态切换而作废，但实际候选与执行仍要求当前可见、启用、独立可点击的目标。
- 版本更新为 `1.0.40` / `versionCode 46`。`:app:assembleRelease --offline` 成功；`git diff --check` 通过；未执行自动化测试。
- 使用 `adb -s 10AG4M2KGF00527 install -r` 升级 Vivo V2502DA，返回 `Success`；设备确认 `1.0.40` / `46`，`lastUpdateTime=2026-10-16 14:27:29`，首次安装时间仍为 `2026-09-20 15:33:13`，净屏仍在系统无障碍启用列表中。Release APK SHA-256：`CCAB8E6C3C9E02AB63AAAD32AADFF3FF218AE3B9F9E15626B6984C5E529CC9C0`。小米设备未改动。
- 未在手机上重新打开中国移动广告页做识别/关闭复测；安装与版本核对不代表广告行为验收通过。
- 涉及文件：`android/app/src/main/java/com/clearscreen/prototype/backend/AdCreativeRules.kt`、`ClearScreenAccessibilityService.kt`、`android/app/build.gradle`、`app.json`、`README.md`、`docs/wiki/README.md`、`docs/wiki/CHANGELOG.md`。

### 2026-10-16 13:54:33 +08:00 — 将诊断状态并入服务与权限页

- 将系统授权、服务连接和最近异常状态直接显示在“服务与权限”页，移除“故障诊断”入口及独立页面；保留未授权或连接异常时的“自动跳过服务”修复入口。
- 同步更新 README 与 Wiki 页面说明。
- 验证：`npx tsc --noEmit`、`git diff --check` 通过；未执行构建或真机/模拟器界面验收。
- 涉及文件：`src/screens/ServicePermissionsScreen.tsx`、`src/screens/DiagnosticsScreen.tsx`（删除）、`App.tsx`、`README.md`、`docs/wiki/README.md`、`docs/wiki/CHANGELOG.md`。

### 2026-10-16 13:54:08 +08:00 — 缩小设置页文字字号

- 根据截图将设置页品牌标题、分区标题、列表项、状态值及弹窗文字适度调小；logo、图标和各行高度保持不变。
- 验证：`git diff --check` 通过；未执行构建、测试或真机/模拟器视觉验收。
- 涉及文件：`src/screens/SettingsScreen.tsx`、`src/components/UI.tsx`、`docs/wiki/CHANGELOG.md`。

### 2026-10-16 13:21:28 +08:00 — 按三张漏关截图改进局部证据、图形叉号和移动轮播弹窗

- 按用户最新要求，本轮只修改、编译打包并升级安装，由用户自行测试；未新增或执行单元测试、截图识别回归、广告真机测试，也未在手机打开应用或操作广告/系统授权。
- 将 OCR 的整页原始文字签名限制改为同一包名、窗口 ID、根区域及实际证据子树的节点身份、资源 ID、类型、位置、标签和可操作状态校验。仅规范化已可跳过的倒计时数字，覆盖数字与“跳过”分离成兄弟节点的 SDK；“几秒后可跳过”等等待提示继续拒绝。局部弹窗外的天气变化不再作废该弹窗的证据；开屏仍检查整窗。执行前页面发生变化时重新识别同一节点、规则与位置，刷新后再次检查目标身份、标签和可点击状态。
- 新增 `AdCloseIconDetector.kt`：在设备内对已有小型可点击控件采样四条对角笔画、中心及间隙背景，补充文字 OCR 不输出图形 X 的情况；每帧最多 40 个候选，结果仍需同一局部区域的广告证据，只执行真实节点 `ACTION_CLICK`。不生成坐标点击，也不绕过 `cll_close_click` 假叉号硬拦截。
- 对中国移动截图中的“广东移动 / 视频号福利 / 5GB / 立即前往 / 限量文案”增加包名限定的轮播海报规则：必须有同一非滚动、无输入框容器内的实际海报节点、下方成对“上一条/下一条”和海报右上方的小型独立关闭控件；其关闭语义仍须有文字、资源 ID 或 X 图形。补充“安全下载”为同卡片内的广告行动文案及彩云下载合规卡片条件，不点击下载入口。
- 执行 `:app:assembleRelease --offline` 成功，Release 编译、打包及 lintVital 完成。重打包中曾遇并行窗口写入 `SettingsScreen.tsx` 时文件暂缺，文件恢复后重试成功，未覆盖该窗口修改。`git diff --check` 通过。未把编译、DEX 标记或安装成功视为广告行为验收。
- 本任务先构建 `1.0.38` / `44`；共用工作区和 APK 随后由并行任务更新到 `1.0.39` / `45`。USB 保留数据升级返回 `Success`，Vivo V2502DA `10AG4M2KGF00527` 实际确认为 `1.0.39` / `45`，更新于 `2026-10-16 13:18:04`；首次安装时间仍为 `2026-09-20 15:33:13`，数据目录 `/data/user/0/com.clearscreen.prototype` 保留，无障碍启用列表仍包含净屏。
- 最终交付 APK 与手机已安装 `base.apk` 的 SHA-256 完全一致：`A69F0FFA23C0E1E7E33572A590C30FCF89D0BFF784E24F199C11D68A657F70E0`；签名证书仍为 `93C410454A8263736FF60BB6E27153B19F74F4C934F444531842F5162CCFA999`。已核对包中有图形 X、局部证据校验、分离倒计时和移动轮播规则的 DEX 标记。固定副本：`C:\Users\22567\AppData\Local\Temp\clearscreen-delivery-20261016\ClearScreen-1.0.39-A69F0FFA.apk`，避免共用输出被后续打包覆盖。
- 最终包同时包含设置页任务更新的隐私政策版本 `2026-10-16-v1`；按当前实现，旧版本同意不足以继续处理其他应用界面。用户需先打开净屏并自行确认新版隐私提示，再验证广告；本轮未代替用户确认。
- 涉及文件：`android/app/src/main/java/com/clearscreen/prototype/backend/ClearScreenAccessibilityService.kt`、`AdSemanticRules.kt`、`AdCreativeRules.kt`、新增 `AdCloseIconDetector.kt`，`android/app/build.gradle`、`app.json`、`README.md`、`docs/wiki/README.md` 与本记录。保留所有并行界面、设置、隐私和其他工作区修改。

### 2026-10-16 13:21:06 +08:00 — 收紧应用页控件和列表行尺寸

- 缩小应用页品牌标题、全部开启按钮、搜索框、筛选项、应用图标、应用名称和列表行高度；应用开关视觉尺寸同步缩小，并保留约 48dp 的触摸区域。
- 验证：`git diff --check` 通过；未执行构建、测试或真机/模拟器界面预览。
- 涉及文件：`src/screens/AppsScreen.tsx`、`src/components/UI.tsx`、`docs/wiki/CHANGELOG.md`。

### 2026-10-16 13:20:05 +08:00 — 增加首页顶部留白

- 将首页内容与系统状态栏之间的留白增加 8dp，避免品牌标题区显得贴近屏幕上沿；仅调整首页，不改变其他页面。
- 验证：`git diff --check` 通过；未执行构建、测试或真机/模拟器界面预览。
- 涉及文件：`src/screens/HomeScreen.tsx`、`docs/wiki/CHANGELOG.md`。

### 2026-10-16 13:20:53 +08:00 — 补齐设置页反馈、帮助、更新、诊断和恢复入口

- 设置页新增意见反馈邮件草稿、常见问题、手动检查 GitHub Releases、匿名诊断报告分享和恢复默认设置。诊断报告不导出包名、界面文字或屏幕内容；恢复默认关闭总开关并重置自动处理偏好，保留应用规则、处理记录和保留周期。
- 为手动更新查询声明 Android `INTERNET` 权限，仅在用户点按时请求公开版本信息；同步应用内隐私政策版本到 `2026-10-16-v1`，更新 Wiki、README 和商店合规说明。公开托管的政策页面尚未同步。
- 验证：`npx tsc --noEmit`、`:app:compileDebugKotlin`、`:app:assembleRelease` 和 `git diff --check` 均通过。通过 `adb install -r` 更新 vivo V2502DA；设备确认包名 `com.clearscreen.prototype`、版本 `1.0.39`、版本代码 `45`，首次安装时间未变。未执行测试或真机界面验收。
- 涉及文件：`src/screens/SettingsScreen.tsx`、`App.tsx`、`src/native/ClearScreenNative.ts`、`src/privacy.ts`、`src/screens/PrivacyPolicyScreen.tsx`、`app.json`、`android/app/build.gradle`、`android/app/src/main/AndroidManifest.xml`、`android/app/src/main/java/com/clearscreen/prototype/backend/ClearScreenStore.kt`、`android/app/src/main/java/com/clearscreen/prototype/backend/ClearScreenNativeModule.kt`、`android/app/src/main/java/com/clearscreen/prototype/backend/ClearScreenPrivacyConsentStore.kt`、`README.md`、`docs/wiki/README.md`、`docs/wiki/隐私政策.md`、`docs/wiki/应用商店合规核查.md`、`docs/wiki/CHANGELOG.md`。

### 2026-10-16 13:10:24 +08:00 — 将今日处理次数并入首页状态卡

- 将“今日处理”次数移入首页“工作中”等状态所在的上方卡片，并保留点击次数区域查看记录的操作；移除独立统计卡，使最近记录上移。
- 验证：`git diff --check` 通过；未执行构建、测试或真机/模拟器界面预览。
- 涉及文件：`src/screens/HomeScreen.tsx`、`docs/wiki/CHANGELOG.md`。

### 2026-10-16 13:00:26 +08:00 — 增加应用页顶部留白

- 按用户截图将应用页顶部留白从 20dp 增加到 28dp，让 logo 与系统状态栏之间多出 8dp 空间。
- 验证：`git diff --check` 通过；未执行构建、测试或真机/模拟器界面预览。
- 涉及文件：`src/screens/AppsScreen.tsx`、`docs/wiki/CHANGELOG.md`。

### 2026-10-16 12:59:36 +08:00 — 缩小应用页控件并压低应用行高度

- 按用户截图缩小应用页品牌标题区、顶部按钮、搜索框、筛选项、应用图标和开关；应用行最小高度从 82dp 降到 72dp。紧凑样式仅用于应用页，开关保留较大的触摸范围。
- 验证：相关源码 `git diff --check` 通过；未执行构建、测试或真机/模拟器界面预览。
- 涉及文件：`src/screens/AppsScreen.tsx`、`src/components/UI.tsx`、`docs/wiki/CHANGELOG.md`。

### 2026-10-16 12:58:01 +08:00 — 将服务与权限项目收拢到独立子页

- 设置首页将“自动跳过服务”“后台运行”“故障诊断”三个入口替换为单一“服务与权限”入口；点击后进入新子页，原三项操作及状态显示保留，返回键回到设置页。
- 同步更新 README 和项目 Wiki 的页面结构说明；Harmony 共享设置页接入新回调。
- `git diff --check` 通过；未执行构建、测试或真机验收，未构建或安装 APK。
- 涉及文件：`App.tsx`、`src/screens/SettingsScreen.tsx`、`src/screens/ServicePermissionsScreen.tsx`、`targets/harmony-bare-pilot/App.tsx`、`README.md`、`docs/wiki/README.md`、`docs/wiki/CHANGELOG.md`。

### 2026-10-16 12:57:44 +08:00 — 增加应用页品牌标题与状态栏间距

- 按用户截图将应用页内容顶部留白从 12dp 增加到 20dp，为 logo/标题区与系统状态栏增加 8dp 间距；仅影响应用页。
- 验证：未执行构建、测试或真机/模拟器界面验收。
- 涉及文件：`src/screens/AppsScreen.tsx`、`docs/wiki/CHANGELOG.md`。

### 2026-10-16 12:56:14 +08:00 — 移除设置页的撤回同意行

- 按用户截图移除设置页“撤回隐私同意”行；撤回并删除入口移至应用内隐私政策页，确认对话和原有停止处理、删除本机数据流程保留。
- 同步修正应用内隐私说明、公开隐私政策和商店合规核查记录，避免文案仍指向设置页。
- `git diff --check` 通过；未执行构建、测试或真机验收，未构建或安装 APK。
- 涉及文件：`App.tsx`、`src/screens/SettingsScreen.tsx`、`src/screens/PrivacyPolicyScreen.tsx`、`targets/harmony-bare-pilot/App.tsx`、`docs/wiki/隐私政策.md`、`docs/wiki/应用商店合规核查.md`、`docs/wiki/CHANGELOG.md`。

### 2026-10-16 12:49:09 +08:00 — 屏蔽彩云天气广告跳转假叉号并升级 Vivo

- 复核彩云天气 7.63.1 的 SDK 代码和小米真机日志后，确认 `cll_close_click` 虽然画成 X，实际绑定的是广告点击/跳转；`cll_close_3` 才是已核实的关闭控件。小米旧版日志中该假叉号的 `ACTION_CLICK` 被接受后紧接着出现系统“打开抖音”确认页，旧规则不能视为安全关闭。现场材料：`C:\Users\22567\AppData\Local\Temp\clearscreen-xiaomi-live-20261015\23-actions.log`、`23-frame13.png`。
- 在 `ClearScreenAccessibilityService.kt` 的统一候选入口排除假叉号节点、其子节点及以该节点为真实点击目标的候选，并在执行前再拒绝目标资源 ID；`AdCreativeRules.kt` 只保留 `cll_close_3` 为已核实插屏关闭 ID。加入假叉号负例单测。同步修正 README/Wiki 中旧的 `cll_close_click` 关闭说明。版本更新为 `1.0.37` / versionCode `43`，Gradle 与 `app.json` 已同步。
- 使用 JBR 21、离线执行 `:app:testReleaseUnitTest :app:assembleRelease` 成功，77 项单元测试通过，0 失败/错误/跳过；Release lintVital 通过。APK `android\app\build\outputs\apk\release\app-release.apk` SHA-256：`44A334611A45F57876EBBE5BF290B81F6015E79BE6B3700BDD2E69B171509216`；包名 `com.clearscreen.prototype`，签名证书 SHA-256 `93C410454A8263736FF60BB6E27153B19F74F4C934F444531842F5162CCFA999`。
- 按用户要求仅对 Vivo V2502DA（序列号 `10AG4M2KGF00527`）执行 `adb -s 10AG4M2KGF00527 install -r --no-streaming`，返回 `Success`。设备确认 versionName `1.0.37` / versionCode `43`，更新于 `2026-10-16 12:48:37`；首次安装时间仍为 `2026-09-20 15:33:13`，数据目录 `/data/user/0/com.clearscreen.prototype` 和原签名包更新路径保留。系统启用无障碍列表仍包含净屏服务。小米设备未在本次升级。
- 安装成功和单测不代表新版本的广告关闭已通过真机验收；本次没有在 Vivo 启动应用或复测广告，也没有操作系统确认页。六张截图对应的下载卡片与中国移动开屏正例仍需分别在可复现样本上做真机验证。
- 涉及文件：`android/app/src/main/java/com/clearscreen/prototype/backend/AdCreativeRules.kt`、`ClearScreenAccessibilityService.kt`，相应 `AdCreativeRulesTest.kt`，`android/app/build.gradle`、`app.json`、`README.md`、`docs/wiki/README.md` 和本记录。

## 2026-10-15

### 2026-10-15 22:02:46 +08:00 — 小米授权已获用户同意，系统确认页模拟点击未生效

- 用户明确选择“允许你在系统界面开启并继续测试”。重新读取小米 `2f8898c80822` 的确认页后，分别尝试点击页面已显示的风险勾选框及其说明区域；界面复核均仍为未选中，“确定”仍不可用。记录为“该系统页面未响应 USB 模拟点击”，没有证据把它扩大归因成所有小米设备或所有 USB 输入的限制。
- 已请求用户在手机上直接勾选并确认，用户回复“手机暂时不在手边”。当前保留系统高敏感权限确认页；净屏仍为 `1.0.32` / `38`，系统无障碍启用列表仅有联控，净屏尚未开启。没有改写系统安全设置、修改联控或绕过该确认。
- 真实广告自动关闭、普通页面误关和六张截图样式的真机验收仍未执行，待系统授权完成后继续。证据截图：`C:\Users\22567\AppData\Local\Temp\clearscreen-xiaomi-1.0.32-20261015\03-accessibility-checkbox.png`。本次仅追加测试进度，未改代码、构建或重复单元测试；`git diff --check` 通过。

### 2026-10-15 19:26:01 +08:00 — 小米 USB 升级完成，真机测试等待系统无障碍确认

- 按用户要求，对唯一当前连接的小米 `22101317C`（序列号 `2f8898c80822`，Android 14 / API 34，物理显示 1080×2400）执行 `adb -s 2f8898c80822 install -r`，安装返回 `Success`；系统确认净屏 `1.0.32` / versionCode `38`，更新于 `2026-10-15 19:20:28`。首次安装时间仍为 `2026-10-09 09:22:57`，原数据目录及历史处理记录保留，未卸载或清除数据。
- 使用上一条记录已验证的正式签名 APK，SHA-256 仍为 `5B5E052EC3D0394BE26700BC15E7594ECF6DD24D093D5CDC6B35A3021F640595`。应用可进入首页，显示“需要开启服务”；系统启用列表当时仅有联控，净屏无障碍未授权。
- 已打开系统“无障碍 → 已下载的应用 → 净屏 → 使用净屏”。小米弹出“无障碍为高度敏感权限”确认页，要求勾选知晓风险并确认读取屏幕/执行操作。当前保留该系统确认页，已向用户请求明确选择，尚未勾选或确认，也未使用命令绕过系统授权。
- 本机已安装彩云天气 `7.63.1` / versionCode `350`；未发现中国移动截图对应的 `com.greenpoint.android.mc10086.activity`。真实广告点击、六张截图对应广告复现和普通页面误关测试均尚未执行，待授权后继续，不能把本次安装及首页显示记为算法验收。
- 现场证据位于 `C:\Users\22567\AppData\Local\Temp\clearscreen-xiaomi-1.0.32-20261015`，含首页和系统授权确认截图及首次界面结构。仅追加测试进度记录，未改算法、应用规则、后台设置或其他项目代码；未重新构建或重复运行单元测试。

### 2026-10-15 19:18:25 +08:00 — 按六张漏关截图补充下载卡片、开屏和延迟识别

- 对照用户提供的六张截图：彩云天气滴滴弹窗、推荐广告卡片及两处带应用合规信息的下载卡片，中国移动两张“2 跳过 / 1 跳过”开屏。修正本地 OCR 将完整屏幕误缩放到应用根窗口的问题，改按物理显示尺寸映射，并排除落在应用窗口外的文本；显示尺寸、窗口 ID、包名、树结构和控件状态变化时丢弃旧证据，点击前再次校验当前树。
- OCR 的小叉号同时从逐字元素和整行结果提取，减少与应用名称/版本同行时的漏识别。节点正文与无障碍描述分别匹配，并规范化全角字符和不可见分隔符；“2 跳过 + 按钮”等标签不再被拼成无效文本。“几秒后可跳过 / Skip in”、业务跳过和普通关闭权限等文案不触发广告动作。
- 彩云天气下载卡片新增 `builtin.feed.close.download-disclosure`：仅在同一个有边界的卡片内具备应用名称、数值版本号、开发者、至少两项功能/权限/隐私/备案链接及明确下载文案时使用；仍要求卡片边缘真实可点击的小型关闭控件。例外只对 `com.nowcasting.activity` 生效；正常天气、会员权益、隐私页面、商店和其他应用不因这些合规字段触发。非滚动的局部 WebView 可作为卡片，真正滚动的页面容器继续排除。补充“抢先下载”作为局部广告行动文案，只识别关闭控件，不点击下载按钮。
- 对截图中的中国移动开屏组合补充节点/OCR 联合依据：左上角小型明确广告徽标、下半屏完整“点击跳转详情页面/第三方应用”等提示、右上角真实小型跳过按钮，并要求没有可滚动页面内容。先读取当前节点文字（`builtin.skip.splash.labelled-redirect`），缺少文字时使用新鲜 OCR；不依赖 SDK 必须暴露 ImageView 等创意类名，不把普通右上角按钮作为位置兜底。
- 用按前台包名、窗口及任务身份隔离的有限重扫替换仅 1 秒的三次重扫：普通内容重扫至约 1.8 秒，新窗口/广告候选最多检查 11 秒；候选仍存在时节流补采 OCR，连续事件不会延长同一任务的截止时间，切换前台/窗口、关闭规则或停止服务后旧任务失效。广告动作后最多再检查两次，覆盖后续广告层或被接受但未生效的点击；统计时同时刷新原控件，OCR 过期本身不再被视为关闭成功。
- 保留原有系统确认、闹钟/计时器、隐私同意、总开关、单应用排除及可点击目标尺寸保护；不使用坐标点击、通用 `ACTION_DISMISS`、云端识别或付费接口。保留工作区此前的界面、规则存储和单应用禁用相关修改。
- 版本同步为 `1.0.32` / versionCode `38`。使用本机 `jbr-21.0.11`、`NODE_ENV=production` 和既有临时测试依赖镜像 init 文件，以离线模式执行 `:app:testReleaseUnitTest :app:assembleRelease` 成功；67 项测试通过（较之前增加 19 项，覆盖截图转录的下载卡片/开屏组合、相似非广告负例、OCR 显示坐标、标签变体和过期任务），无失败、错误或跳过。Release 的 lintVital 与 `git diff --check` 通过；未执行前端 TypeScript、Harmony 或 iOS 检查，本次未修改其页面代码。最初尝试 Android Studio 自带 JBR 25 时 Gradle 插件解析失败，切换本机已有 JBR 21 后通过，未更改全局 Java 配置或项目依赖。
- APK：`android/app/build/outputs/apk/release/app-release.apk`，SHA-256 `5B5E052EC3D0394BE26700BC15E7594ECF6DD24D093D5CDC6B35A3021F640595`；包名仍为 `com.clearscreen.prototype`，名称“净屏”，原 Release 证书 SHA-256 `93C410454A8263736FF60BB6E27153B19F74F4C934F444531842F5162CCFA999`，`apksigner verify` 通过。
- 真机边界：只读核对到 vivo `10AG4M2KGF00527`（物理显示 1260×2800）仍为 `1.0.31` / `37`，净屏无障碍已启用并绑定、具有截图能力；读取时为系统通知面板，没有取得上述广告的实时节点树。未安装新版、未操作系统授权或其他应用。新增用例是依据截图手工转录的文案/几何单元回归，未执行这些图片的 ML Kit OCR 准确率测试或真机关闭验证，不能宣称六张广告均已实际关闭。
- 涉及文件：`android/app/src/main/java/com/clearscreen/prototype/backend/ClearScreenAccessibilityService.kt`、`AdSemanticRules.kt`、`AdGeometryRules.kt`、新增 `AdCreativeRules.kt`、`AdScanCoordinator.kt`，对应的 `AdSemanticRulesTest.kt`、`AdGeometryRulesTest.kt`、新增 `AdCreativeRulesTest.kt`、`AdScanCoordinatorTest.kt`，以及 `android/app/build.gradle`、`app.json`、`README.md`、`docs/wiki/README.md`、本修改记录。

### 2026-10-15 10:15:11 +08:00 — 应用默认开启、全部开启和单应用排除保持一致

- 按用户要求，将应用页右上角“排除应用”替换为“全部开启”：启用全部已加载应用、清除其排除状态并切回“全部”筛选，不受当前搜索或筛选范围限制；单应用仍通过原开关关闭，无新增功能说明小字。
- 原生规则改为未保存规则的应用默认开启，后续新增应用也默认开启；已有显式关闭/白名单选择保持不变，直到用户主动操作。列表、详情与服务读取同一规则，关闭状态不会因刷新、重建存储对象或升级被覆盖。
- 新增原生批量规则保存接口，一次写入并确认落盘；单应用更新复用同一路径。界面回到前台时先等待规则保存完成再读取状态，避免旧快照覆盖刚刚修改的开关。
- 无障碍服务在开始 OCR、截图返回、识别结果回调和最终点击前重新检查总开关与单应用规则；关闭应用后不再安排该应用的后续 OCR，已在途结果不再触发广告动作。广告判断阈值与系统界面保护规则未变。
- 版本同步为 `1.0.31` / versionCode `37`。`:app:testReleaseUnitTest` 48 项通过（其中新增规则存储测试 6 项，覆盖新装/新增应用默认开启、关闭保存、旧规则保持、179 个规则批量恢复、其他设置保持和写入失败上报）；主工程 `npx tsc --noEmit`、Harmony 共用页面试验 `npm run typecheck`、`:app:assembleRelease` 与签名验证通过。
- 本机首次测试因 Maven Central/Google 仓库 TLS 连接失败未执行；使用临时 Gradle init 文件 `C:\Users\22567\AppData\Local\Temp\clearscreen-test-json-mirror.gradle`，仅为 `org.json` 测试依赖及其父 POM 使用 Aliyun 镜像后，测试和构建成功。项目生产依赖及仓库端点未改变。
- 通过 `adb -s 2f8898c80822 install -r` 更新 Xiaomi `22101317C`，系统确认 `1.0.31` / `37`、更新于 `2026-10-15 10:10:15`，首次安装时间仍为 `2026-10-09 09:22:57`。APK SHA-256：`959CEA90A2C20EF54FA02AD17DCE72FF3F8E99410110CC95F46EA2FED23D34DA`；证书 SHA-256：`93C410454A8263736FF60BB6E27153B19F74F4C934F444531842F5162CCFA999`。
- 真机核验：更新后显示默认“已开启 179 / 已排除 0”；手动关闭 58 同城与 Behance 后变为 `177 / 2`，回到桌面再返回应用后仍保留关闭；在“已排除”筛选下搜索 Behance，只显示一个关闭项时点“全部开启”，两项均恢复，计数回到 `179 / 0`，再次前台刷新后保持。已清空测试搜索，最终保留全部开启；截图为 `C:\Users\22567\AppData\Local\Temp\clearscreen-apps-1.0.31.png`。
- 设备系统的净屏无障碍当时未开启（启用列表仅有 LinkPilot），未修改系统授权/后台设置，也未在其他 App 中验收真实广告处理；新装及冷重启规则行为由存储测试验证，真机未卸载、清除数据或强制停止应用。
- 涉及文件：`App.tsx`、`src/screens/AppsScreen.tsx`、`src/native/ClearScreenNative.ts`、`android/app/src/main/java/com/clearscreen/prototype/backend/ClearScreenStore.kt`、`ClearScreenNativeModule.kt`、`ClearScreenAccessibilityService.kt`、`android/app/src/test/java/com/clearscreen/prototype/backend/ClearScreenStoreTest.kt`、`android/app/build.gradle`、`app.json`、`targets/harmony-bare-pilot/App.tsx`、`README.md`、`docs/wiki/README.md`、`design-qa.md`、`docs/wiki/CHANGELOG.md`。

### 2026-10-15 09:39:51 +08:00 — 恢复设置页视觉改版前的层级布局

- 按用户反馈，将设置页恢复到当前 Git 基线 `HEAD` 中保留的视觉改版前结构：移除额外的大号“设置”页标题、分组标题图标和行图标，恢复简洁分组标题、文字行、原行高及原有分组间距；保留隐私政策、撤回同意、记录保留、权限与诊断等入口和处理逻辑。
- 设置页源码与 `HEAD:src/screens/SettingsScreen.tsx` 完全一致；`npx tsc --noEmit`、`git diff --check` 通过。未构建 APK、未安装设备；恢复后的真机视觉验收未执行。
- 同步更新 `design-qa.md`，标明设置页源码已恢复但尚未重新构建/安装验证。
- 涉及文件：`src/screens/SettingsScreen.tsx`、`design-qa.md`、`docs/wiki/CHANGELOG.md`。

## 2026-10-13

### 2026-10-13 19:05:19 +08:00 — 对照 ChatGPT 高保真稿核验 Android 界面与设备版本

- 重新打开并逐页核对 ChatGPT 对话“生成首页设计稿”中的首页、应用、记录和设置参考图；对照当前 Android 首页、应用和设置实现及小米真机截图。当前实现包含品牌页头、蓝白背景、圆角卡片、应用筛选列表和三项底部导航；应用管理参考图里仍画有四项导航，按现行产品结构保留三项。
- 查明用户反馈截图不是 USB 小米真机上的最新安装：USB 设备 `2f8898c80822`（`22101317C`，Android 14，1080×2400）实际为 `1.0.30` / versionCode `36`；Android Studio 虚拟设备 `emulator-5554`（1080×2424）仍为 `1.0.2` / versionCode `3`。没有更新或清除虚拟设备数据；后续视觉验收必须先明确查看的是哪台设备。
- `npx tsc --noEmit`、`npx expo install --check`、`git diff --check` 通过；后者仅输出仓库既有 LF/CRLF 转换提示。Release APK 已存在并已按此前记录安装到 USB 小米；本次未重新构建、未改动无障碍或手机权限。用户对 `1.0.30` 真机界面的最终视觉确认尚未取得。
- 涉及文件：`design-qa.md`、`docs/wiki/CHANGELOG.md`。

### 2026-10-13 18:06:17 +08:00 — 构建并更新小米手机 Android App

- 使用当前工作区代码构建 Android Release APK，版本为 `1.0.28`（versionCode `34`）。
- 目标设备：USB 小米 `22101317C`（Android 14，序列号 `2f8898c80822`）。先比对新旧 APK 的签名证书 SHA-256 一致，再执行 `adb -s 2f8898c80822 install -r android/app/build/outputs/apk/release/app-release.apk`；安装成功，使用 `-r` 保留原应用数据。
- 安装后确认系统记录为 `1.0.28` / versionCode `34`，主界面 Activity 正常显示；首屏停在隐私政策同意页，未代替用户选择同意或拒绝。
- 验证：`:app:assembleRelease` 成功；新旧 APK 签名一致；设备版本与前台页面已核实。模拟器未能启动（电脑空间不足），未用于本次安装；未修改无障碍权限或其他手机设置。
- APK SHA-256：`ADBA1C98B45E130DF47C5E069506DE3C05299D7979DCFB9AEEAC27E6138636FF`。
- 涉及文件：`android/app/build/outputs/apk/release/app-release.apk`、`docs/wiki/CHANGELOG.md`。

### 2026-10-13 16:57:12 +08:00 — 汇入远端 main 的其他 Agent 改动并完成联合验证

- 将远端 `origin/main` 合并到 `accessibility-diagnostics`，纳入此前集成的通用广告语义/几何判断、视频广告专用规则、闹钟/计时器保护、GitHub Android 回归工作流及配套测试/文档；与本地 Android 隐私合规、iOS/Safari、HarmonyOS 试验及 Wiki 改动共同保留。
- 合并无冲突。当前仓库仅有一个 Git worktree；本任务没有发现其他 ClearScreen 活跃任务或额外挂载工作区。工作区未发现凭据格式匹配项。
- 验证：`npx tsc --noEmit` 通过；`npm run test:safari` 6 项通过；`android/gradlew.bat :app:testReleaseUnitTest` 通过；Harmony 试验 `npm run typecheck` 与 Jest 1 项通过；`git diff --cached --check` 通过。
- 尚未执行 Release APK 构建、HAP 构建或 Android/HarmonyOS 真机验收；本次仅整合提交并推送 Git。
- 涉及文件：`android/app/src/main/java/com/clearscreen/prototype/backend/AdGeometryRules.kt`、`AdSemanticRules.kt`、`ProtectedUiPolicy.kt`、`VideoAdRules.kt`、`ClearScreenAccessibilityService.kt`、`android/app/src/test/java/com/clearscreen/prototype/backend/`、`.github/workflows/android-recognition.yml`、`docs/AD_DETECTION_ARCHITECTURE.md`、`docs/AD_DETECTION_V2_TEST_PLAN.md`、`docs/VIDEO_AD_SUPPORT.md`、`docs/wiki/CHANGELOG.md`。

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

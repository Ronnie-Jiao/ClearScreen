# 净屏修改记录

本文件按时间倒序记录项目修改，供后续 AI 和开发者开始任务前快速了解最近变化。自本记录建立起，新增条目的时间使用中国标准时间（UTC+08:00）；既有条目的时间按文件系统记录的本地最后修改时间整理。

## 2026-10-18

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

# 净屏修改记录

本文件按时间倒序记录项目修改，供后续 AI 和开发者开始任务前快速了解最近变化。自本记录建立起，新增条目的时间使用中国标准时间（UTC+08:00）；既有条目的时间按文件系统记录的本地最后修改时间整理。

## 2026-11-13

### 2026-11-13 00:38:11 +08:00 — 将根目录 README 调整为英文优先的中英文版本

- 将完整英文介绍放在前面，并在后半部分提供对应的简体中文版本；在页首增加语言跳转链接。
- 保留并统一项目功能、平台范围、权限与隐私、开发步骤、文档链接和许可证说明。
- 验证：git diff --check 通过；仅修改文档，未运行自动化测试或构建。
- 涉及文件：README.md、本修改记录。

### 2026-11-13 00:15:48 +08:00 — 扩展帮助与反馈为可搜索使用说明并支持页面直达

- 将「帮助与反馈」扩展为 Android/iOS APP 使用说明和问题排查，覆盖自动跳过、应用管理、学习关闭、记录、系统推荐、通知、后台权限与隐私数据等实际功能。
- 增加按标题、正文和关键词搜索的输入框与搜索按钮；结果可点按直达对应 APP 页面，返回帮助页时保留搜索条件。
- 同步更新 README 与 Wiki。
- 验证：`npx tsc --noEmit` 与相关文件 `git diff --check` 通过。未运行自动化测试、未构建或安装，未进行应用 UI 验收。
- 涉及文件：`App.tsx`、`src/screens/SettingsDetailScreen.tsx`、`README.md`、`docs/wiki/README.md`、本修改记录。

### 2026-11-13 00:14:49 +08:00 — 重写 GitHub 项目介绍 README

- 将根目录 README 整理为面向 GitHub 访客的项目首页，介绍产品定位、功能范围、平台现状、权限与隐私边界、安装和开发入口，并链接到项目 Wiki 与政策文档。
- 清理过时的导航描述和过细的开发流水；未将未接入的平台能力、未经声明的许可证或全部广告覆盖写成已实现。
- 验证：`git diff --check` 通过；文档链接目标检查通过。未运行构建或自动化测试（仅文档修改）。
- 涉及文件：`README.md`、本修改记录。

### 2026-11-13 00:12:57 +08:00 — 清理五天前的修改记录

- 移除 2026-11-07 及更早的历史条目，保留最近五天记录。
- 历史临时文件与旧 APK 尚未清理：文件删除命令被自动审核拦截，未执行。
- 验证：保留的最早记录为 2026-11-08 01:18:46 +08:00；未执行构建或测试。

## 2026-11-12

### 2026-11-12 23:55:32 +08:00 — 构建并覆盖安装净屏 1.0.100

- 从当前工作区构建 Release APK，并将 `android/app/build.gradle` 与 `app.json` 同步递增到 `versionName 1.0.100` / `versionCode 106`。
- `:app:assembleRelease --offline --console=plain` 成功，`npx tsc --noEmit` 通过。APK 核对为包名 `com.clearscreen.prototype`、应用名“净屏”、`1.0.100 / 106`；APK v2 签名验证通过，正式证书 SHA-256 为 `93c410454a8263736ff60bb6e27153b19f74f4c934f444531842f5162ccfa999`。`outputs/apk/release/app-release.apk` 与 `outputs/apk/净屏.apk` 哈希一致（SHA-256 `5DC4CC209E5B0A85C1F96720FAE3367A75FFE11BD160BC2283C806B39779AE67`，118,654,247 字节）。
- 经 USB 对小米 22101317C（序列号 `2f8898c80822`）执行 `adb install -r --no-streaming`，返回 `Success`；设备回读 `1.0.100 / 106`，首次安装时间仍为 `2026-11-06 18:47:52`。无障碍服务在安装前后均启用。未运行自动化测试或手工功能验收。
- 验证：`git diff --check` 通过。涉及文件：`android/app/build.gradle`、`app.json`、APK 产物、本修改记录。

### 2026-11-12 23:51:38 +08:00 — 关于页增加作者和 GitHub 仓库入口

- 在「关于净屏」版本卡片下增加作者 `Ronnie Jiao` 和 GitHub 图标；点击图标通过系统链接打开 `https://github.com/Ronnie-Jiao/ClearScreen`，无法打开时显示提示。
- 同步更新 README 与 Wiki 的关于页说明。
- 验证：`npx tsc --noEmit` 通过；`git diff --check` 通过。未运行自动化测试、未构建或安装，未进行设备 UI 验收。
- 涉及文件：`src/screens/SettingsDetailScreen.tsx`、`README.md`、`docs/wiki/README.md`、本修改记录。

### 2026-11-12 22:51:19 +08:00 — 恢复首页自定义关闭窗口的更多入口

- 首页「自定义关闭窗口」右侧「更多」恢复进入全部学习关闭记录页，可逐条删除规则；广告页页头的「应用管理」仍用于管理应用列表。
- 同步更新 README 与 Wiki 的导航说明。
- 涉及文件：`App.tsx`、`src/screens/HomeScreen.tsx`、`README.md`、`docs/wiki/README.md`、本修改记录。
- 验证：`npx tsc --noEmit` 通过；`git diff --check` 通过。未运行自动化测试、未构建、未打包或安装，未进行设备 UI 验收。

### 2026-11-12 22:39:59 +08:00 — 构建并覆盖安装净屏 1.0.99

- 将 `android/app/build.gradle` 与 `app.json` 同步递增到 `versionName 1.0.99` / `versionCode 105`，包含彩云天气内嵌视频广告卡片识别优化。
- `:app:assembleRelease --offline --console=plain` 成功，`npx tsc --noEmit` 通过。Release APK 核对为包名 `com.clearscreen.prototype`、应用名“净屏”、`1.0.99 / 105`；APK v2 签名验证通过，正式证书 SHA-256 为 `93c410454a8263736ff60bb6e27153b19f74f4c934f444531842f5162ccfa999`。`outputs/apk/release/app-release.apk` 与 `outputs/apk/净屏.apk` 哈希一致（SHA-256 `107B3F7D95ACB8AB564FCA519DF160F1420CB3FDD610849D5A343F6EECC354D4`，118,653,203 字节）。
- 经 USB 对小米 22101317C（序列号 `2f8898c80822`）执行 `adb install -r --no-streaming`，返回 `Success`；设备回读 `1.0.99 / 105`，首次安装时间仍为 `2026-11-06 18:47:52`。无障碍服务在安装前后均启用。未运行自动化测试或手工广告功能验收。
- 验证：`git diff --check` 通过。涉及文件：`android/app/build.gradle`、`app.json`、APK 产物、`docs/wiki/CHANGELOG.md`。

### 2026-11-12 19:52:31 +08:00 — 补充彩云天气内嵌视频广告卡片关闭识别

- 针对彩云天气主页内嵌视频卡片增加局部组合识别：同一新鲜 OCR 画面须包含标题中的“滴滴/该省”、卡片底部“广告”标记、“滑动跳转至详情页或第三方应用”披露和“打车立省”行动文案，并将右上角 OCR 小叉号与真实、独立、启用的小型无障碍关闭节点对应。规则仅限 `com.nowcasting.activity`，不会点击推广按钮；`cll_close_click` 仍作为广告跳转入口全局禁止。
- 更新 README 与 Wiki 的彩云天气广告识别边界。
- 验证：`:app:compileDebugKotlin --offline --console=plain` 成功；`git diff --check` 通过。未运行自动化测试、未打包或安装、未进行真机广告场景验收。
- 涉及文件：`android/app/src/main/java/com/clearscreen/prototype/backend/AdCreativeRules.kt`、`android/app/src/main/java/com/clearscreen/prototype/backend/ClearScreenAccessibilityService.kt`、`README.md`、`docs/wiki/README.md`、本修改记录。

### 2026-11-12 19:42:59 +08:00 — 补充中国移动居中套餐弹层关闭识别

- 针对首页居中套餐促销弹层增加包名与布局限定：新鲜 OCR 必须在同一创意中匹配「广告」「套餐升级享」「30GB + 200分钟」「19元/月」，并确认价格下方中央的小型 X 与真实、独立、可点击节点对应。规则仅点击无障碍节点；「7天内不再提示」复选项不参与识别，也不按坐标操作。
- 更新 README 与 Wiki 的中国移动弹层识别边界。
- 验证：`:app:compileDebugKotlin --offline --console=plain` 成功；`git diff --check` 通过。未运行自动化测试、未打包或安装、未进行真机广告场景验收。
- 涉及文件：`android/app/src/main/java/com/clearscreen/prototype/backend/AdCreativeRules.kt`、`android/app/src/main/java/com/clearscreen/prototype/backend/ClearScreenAccessibilityService.kt`、`README.md`、`docs/wiki/README.md`、本修改记录。

### 2026-11-12 19:33:53 +08:00 — 补充中国电信品牌开屏跳过识别

- 针对 `com.ct.client` 中国电信品牌开屏，增加包名与画面布局限定：同屏识别下方居中的「中国电信」、中央两列「用户至上 / 用心服务」，并要求右下角出现真实、启用、尺寸受限的「跳过」控件；页面不可滚动。仅通过无障碍节点点击，不按 OCR 坐标操作。
- 更新 README 与 Wiki 的开屏识别边界。
- 验证：`:app:compileDebugKotlin --offline --console=plain` 成功；`git diff --check` 通过。未运行自动化测试、未打包或安装、未进行真机广告场景验收。
- 涉及文件：`android/app/src/main/java/com/clearscreen/prototype/backend/AdCreativeRules.kt`、`android/app/src/main/java/com/clearscreen/prototype/backend/ClearScreenAccessibilityService.kt`、`README.md`、`docs/wiki/README.md`、本修改记录。

### 2026-11-12 19:24:09 +08:00 — 补充中国移动首页礼盒悬浮广告关闭识别

- 针对首页右侧「20GB 特惠包」礼盒素材，增加仅限中国移动包名的局部关闭组合：新鲜 OCR 必须在同一紧凑创意内识别「20GB」与「特惠包」，右边缘 OCR 小叉号位于促销文案上方，并与真实小型可点击节点重合。无需从主页借用“广告”标记或“去查看”按钮；不使用坐标手势。
- 更新 README 与 Wiki 的悬浮广告识别边界。
- 验证：`:app:compileDebugKotlin --offline --console=plain` 成功；`git diff --check` 通过。未运行自动化测试、未打包或安装、未进行真机广告场景验收。
- 涉及文件：`android/app/src/main/java/com/clearscreen/prototype/backend/AdCreativeRules.kt`、`android/app/src/main/java/com/clearscreen/prototype/backend/ClearScreenAccessibilityService.kt`、`README.md`、`docs/wiki/README.md`、本修改记录。

### 2026-11-12 19:15:22 +08:00 — 补充中国移动千兆宽带开屏识别与倒计时重试

- 根据截图为中国移动「千兆宽带 / 秋季特惠 / 1元/天起 / 全家畅享」素材补充包名限定识别，要求同屏左上「广告」徽标、下半屏详情跳转提示和右上紧凑跳过字样；宽带 OCR 例外还要求无可滚动内容。
- OCR 坐标手势现在等待倒计时归零。若无障碍树已有对应真实节点，则限时重扫并继续由节点启用状态决定是否操作，不绕过禁用状态；动作仍按窗口去重并记为待确认。更新 README 与 Wiki 规则说明。
- 验证：`:app:compileDebugKotlin --offline --console=plain` 成功；`git diff --check` 通过。未运行自动化测试、未打包或安装、未进行真机广告场景验收。
- 涉及文件：`android/app/src/main/java/com/clearscreen/prototype/backend/AdCreativeRules.kt`、`android/app/src/main/java/com/clearscreen/prototype/backend/AdSemanticRules.kt`、`android/app/src/main/java/com/clearscreen/prototype/backend/ClearScreenAccessibilityService.kt`、`README.md`、`docs/wiki/README.md`、本修改记录。

### 2026-11-12 19:04:29 +08:00 — 校正广告导航文档入口描述

- 将 README 与 Wiki 中仍指向设置首页的系统广告、应用通知和营销通知旧入口描述改为当前底部「广告」页；同步说明应用管理列表从广告页页头和首页「自定义关闭窗口」进入。
- 涉及文件：`README.md`、`docs/wiki/README.md`、本修改记录。
- 验证：`git diff --check` 通过；未运行自动化测试、未构建、未打包或安装，未进行设备 UI 验收。

### 2026-11-12 19:01:23 +08:00 — 修复学习关闭规则重放不重试及广告文案变化失配

- 修复学习关闭控件尝试一次后长期不再重试的问题：冷却现在依据最近一次真实点击计时，未确认时30秒后可重试；确认关闭后清除该控件的冷却，允许后续广告展示再次匹配。重复规则按同一控件身份共享冷却。
- 对广告卡片上下文摘要变化增加有限容错：只有目标自身身份、卡片锚点、相对路径保持一致且当前页面仅有一个匹配目标时才允许忽略整卡摘要变化；延时执行前重新读取并点击当前树中唯一匹配的真实节点，因此节点边界变化不会单独阻止动作。应用版本、窗口、目标唯一性、滚动及点击安全检查保持有效。规则仍使用 selector.version=2，不要求重新学习。
- 更新 README 与 Wiki 对学习重放及重试规则的说明。
- 验证：`:app:compileDebugKotlin --offline --console=plain` BUILD SUCCESSFUL；相关文件 `git diff --check` 通过。未运行自动化测试、未打包或安装、未进行 UI 或真机广告场景验收。
- 涉及文件：`android/app/src/main/java/com/clearscreen/prototype/backend/ClearScreenAdLearning.kt`、`README.md`、`docs/wiki/README.md`、本修改记录。

### 2026-11-12 18:56:09 +08:00 — 补充 WiFi 万能钥匙游戏悬浮广告识别

- 根据截图新增 WiFi 万能钥匙右侧游戏浮层窄规则：要求同一贴边小创意内同时识别「天天打企鹅」和「好运赢大奖」，并匹配紧邻的真实 X 图形及小型可点击目标。
- 允许 X 图形由其小型可点击父控件接收点击；控件仍须包含识别到的 X，且尺寸、位置、创意归属和稳定 OCR 证据全部符合限制。仅限 WiFi 万能钥匙，不符合组合时不操作。
- 同步更新 README 与 Wiki。
- 验证：`:app:compileDebugKotlin --offline --console=plain` 与 `git diff --check` 通过；未运行自动化测试、未打包安装、未进行真机广告场景验收。
- 涉及文件：`android/app/src/main/java/com/clearscreen/prototype/backend/WifiPromotionRules.kt`、`android/app/src/main/java/com/clearscreen/prototype/backend/ClearScreenAccessibilityService.kt`、`README.md`、`docs/wiki/README.md`、本修改记录。

### 2026-11-12 18:55:54 +08:00 — 将系统广告入口集中到广告导航页

- 底部原“应用”导航改名为“广告”，并由系统推荐开关、应用通知、营销通知三个入口替换应用列表内容；设置首页移除重复的系统广告和营销通知入口。
- 原应用规则列表保留在广告页右上角“应用管理”；首页“自定义关闭窗口”的“更多”改为直接进入该列表。权限引导完成且无障碍服务可用时仍进入应用管理页。
- 涉及：`App.tsx`、`src/components/BottomNav.tsx`、`src/components/UI.tsx`、`src/screens/AdsScreen.tsx`、`src/screens/AppsScreen.tsx`、`src/screens/HomeScreen.tsx`、`src/screens/SettingsScreen.tsx`、`src/screens/SettingsDetailScreen.tsx`、`README.md`、`docs/wiki/README.md`。
- 验证：`npx tsc --noEmit` 通过；`git diff --check` 通过（Git 提示若干既有文件可能发生 LF/CRLF 转换，无空白错误）。未运行自动化或设备功能/UI验收，未构建、打包或安装。

### 2026-11-12 18:54:42 +08:00 — 扩大学习关闭的小控件点选识别范围

- 针对学习关闭时较小按钮点选后经常找不到控件，扩大精确命中为空时的候选范围：可处理宽高均不超过 56dp 的紧凑控件，候选触摸区域至少扩至 72dp，单边最多扩 28dp。范围扩展仅用于发现候选；真实节点边界框选、身份唯一性、安全检查、确认后的操作方式均保持不变。
- 验证：`:app:compileDebugKotlin --offline --console=plain` BUILD SUCCESSFUL；`git diff --check` 通过（仅报告既有文件的换行符转换提示）。未运行自动化或设备功能/UI验收，未打包或安装。
- 涉及文件：`android/app/src/main/java/com/clearscreen/prototype/backend/ClearScreenAdLearning.kt`、`README.md`、`docs/wiki/README.md`、本修改记录。

### 2026-11-12 18:52:28 +08:00 — 按本机品牌与系统入口筛除未适配推荐开关

- 系统推荐开关现在先按设备品牌选目录，再核对目标系统包已安装且启用、必需 OEM 包存在，以及设置路由可解析到该操作信任的系统页面；缺少目录、系统包或可用入口的条目不返回给页面。vivo 全局搜索项额外要求全局搜索系统包存在。Android 13+ 的 Google 隐私沙盒三个开关改用独立的 Android 隐私设置路由，避免在小米设备误进 MIUI「广告服务」页。
- 页面打开后的自动操作仍要求可信系统包、准确页面和开关标签匹配；版本/语言变化导致运行时匹配失败时不会点击。包与路由的预筛选不能提前得知 OEM 页面运行时改名后的控件，因此此类版本差异仍需对应真机验收。
- 验证：`:app:compileDebugKotlin :app:processDebugMainManifest --offline --console=plain --quiet` 成功；`git diff --check` 通过。未运行测试、未打包安装、未进行真机开关验收；不同 OEM 的路由仍待设备验证。
- 涉及文件：`android/app/src/main/java/com/clearscreen/prototype/backend/ClearScreenUiCleanup.kt`、`ClearScreenRecommendCatalog.kt`、`ClearScreenRecommendRoutes.kt`、`android/app/src/main/AndroidManifest.xml`、`README.md`、`docs/wiki/README.md`、本修改记录。

### 2026-11-12 18:43:29 +08:00 — 首页自定义关闭窗口更多入口打开应用列表

- 「自定义关闭窗口」的「更多」现在打开与底部「应用」相同的应用列表；新增「规则」入口，仍可查看全部学习规则并逐条删除。
- 验证：`npx tsc --noEmit` 与 `git diff --check` 通过；未进行应用界面或真机验收。
- 涉及文件：`src/screens/HomeScreen.tsx`、`README.md`、`docs/wiki/README.md`、本修改记录。

### 2026-11-12 18:42:17 +08:00 — 构建并升级安装净屏 Release 1.0.98

- 将 `app.json` 与 `android/app/build.gradle` 同步递增至 `1.0.98` / versionCode `104`。`:app:assembleRelease --offline --console=plain` 成功；APK 包名 `com.clearscreen.prototype`、应用名“净屏”，大小 118,652,543 字节，SHA-256 `1762F75D108F9C5D61CF5733302D478CC6884C4E7298C58E9B432B9B5D348421`。正式证书 SHA-256 为 `93c410454a8263736ff60bb6e27153b19f74f4c934f444531842f5162ccfa999`，apksigner v2 验证通过；正式命名 APK 与 Gradle 原始 APK 散列相同。
- 使用 ASCII 路径的原始 Release APK 执行 `adb install -r --no-streaming`，覆盖安装到 Xiaomi 22101317C（`2f8898c80822`）返回 `Success`。设备读回 `1.0.98` / `104`，首次安装时间保持 `2026-11-06 18:47:52`；净屏无障碍服务仍在系统启用列表。未卸载、未清数据、未启动应用，未进行功能/UI验收。中文命名 APK 路径的 ADB 推送命令报路径错误，改用与之散列相同的原始 APK 成功安装。
- 验证：`npx tsc --noEmit`、`:app:assembleRelease --offline --console=plain`、包名/版本读取、签名校验及设备安装后读回通过。
- 涉及文件：`app.json`、`android/app/build.gradle`、`README.md`、本修改记录；APK：`android/app/build/outputs/apk/净屏.apk`。

### 2026-11-12 18:37:25 +08:00 — 明确小米「资讯与推荐」入口的下一步

- 用户反馈点进「资讯与推荐」后看不到对应设置。截图中的页面实际是全局搜索「搜索设置」首页；「搜索提示词、应用推荐、搜索历史」位于下一层「首页展示模块」。在小米 22101317C 的搜索 APK 清单中，该子页 Activity 标记为不导出，普通应用不能直接启动。
- 将系统页提示改为列出具体项目和入口：「首页展示模块」查看搜索提示词、应用推荐和搜索历史；「宫格位推送」在浏览器「主页设置」。跳转目标与开关操作流程未改。
- 验证：`:app:compileDebugKotlin --offline --console=plain` BUILD SUCCESSFUL（40 秒）；`git diff --check` 通过。未运行测试、未打包安装、未做设备 UI 验收。
- 涉及文件：`android/app/src/main/java/com/clearscreen/prototype/backend/ClearScreenRecommendRoutes.kt`、`docs/wiki/README.md`、本修改记录。

### 2026-11-12 18:21:54 +08:00 — 移除系统广告页的快应用与广告组件入口

- 用户在截图中要求删除「快应用与系统广告组件」入口。`SystemCleanupScreen.tsx` 移除该卡片；`App.tsx` 移除对应页面路由、返回键分支和组件导入；帮助页 FAQ 不再指向此功能。系统推荐开关和应用通知入口保留。
- 更新 Wiki 当前导航说明。底层组件检查与停用代码保留在源码中，但当前 APP 页面无法进入该功能。
- 验证：未运行测试、构建或设备验收；工作区其他未提交修改保持原样。

### 2026-11-12 18:01:22 +08:00 — 系统广告组件页列出全部检查目标

- 用户询问组件检查的用途，并指出页面没有展示具体检查目标。现将检查结果补齐为完整的品牌白名单目标清单，逐项显示友好名称、包名与状态；未安装的目标标记为「未安装」且不显示开关，状态未知的目标显示灰态且不可操作，只有已安装且状态可确认的目标能停用或恢复。
- `ClearScreenShellOps.inspectTargets()` 不再丢弃 `absent` 目标；`CleanupTarget` 增加 `absent` 状态；`QuickComponentsScreen.tsx` 显示全部目标、包名、当前状态和确认安装数，并将开关效果文字收敛为「已停用／正常运行」。同步更新 Wiki，说明此页读取的是组件存在和停用状态，不读取广告文案或素材。
- 验证：未运行测试、构建或设备验收；保留工作区既有未提交修改。

### 2026-11-12 11:00:14 +08:00 — 系统推荐开关页不再显示无法操作的开关

- 用户要求：「这个未适配的开关，就是它没有适配它就不需要显示出来。」原先的做法是给「未适配」「需手动」项保留一行、显示状态标识并让用户点开看原因，现在改为不列出。
- 改动：`android/app/src/main/java/com/clearscreen/prototype/backend/ClearScreenUiCleanup.kt` 的 `targets()` 改为只返回 `support == "assisted"` 的目标，这是「只列能操作的开关」的落点，保证该规则不被后续登记破坏；`android/app/src/main/java/com/clearscreen/prototype/backend/ClearScreenRecommendCatalog.kt` 删掉内容中心的两项（个性化内容推荐、内容中心广告推荐）和未知品牌那一项 `generic.system.personalizedAds`，`vendorTargets()` 的兜底分支改为返回空列表，未知品牌因此走页面自身的「这台手机暂未适配」空态并保留系统广告设置入口；`src/screens/SystemRecommendScreen.tsx` 删掉「未适配／需手动」标识分支与对应的 `Badge` 引用，该页现在每行都是一个能真正翻动的开关。
- 取证（小米 22101317C / HyperOS 2 / Android 14）：把 `ClearScreenUiCleanup` 的行匹配算法按节点树复刻成脚本，逐页喂真机 uiautomator dump 回放，确认保留在列表里的每一项都能唯一命中一个开关——桌面设置页「智能助理」（`android.widget.CheckBox`，当前已关）、广告服务页「个性化广告推荐」（CheckBox，当前已关）、应用商店隐私页「个性化服务」（CheckBox，当前已关）、全局搜索「首页展示模块」页「搜索提示词」（CheckBox，当前开）与「搜索历史」（CheckBox，当前已关）、浏览器「主页设置」页「宫格位推送」（CheckBox，当前开）、负一屏设置页「推荐卡片」「热门活动」（均已关）与「智能置顶」（开）；「应用推荐」在「首页展示模块」页是导航行、本身没有开关，与「展示应用推荐」在子页的二级 stage 设计一致。原先登记为「需手动」的内容中心两项，其设置页未导出且要求 `miui.permission.USE_INTERNAL_GENERAL_API`，进入前还要先同意一次隐私协议，确实无法自动操作，按新规则不再列出；内容中心仍可从「资讯与推荐」分类入口跳转过去。
- 验证：`npx tsc --noEmit` 通过（exit 0）；`cd android; $env:JAVA_HOME='C:\Users\22567\.jdks\jbr-21.0.11'; .\gradlew.bat :app:compileDebugKotlin --offline --console=plain` BUILD SUCCESSFUL（27 秒），无新增告警。**未打包、未安装**；设备上并行窗口仍占用同一台机器且版本号在 1.0.97 / 103，本次不动版本号、不安装，端到端点击待安装后验收。
- 遗留：vivo/iQOO 的 23 项、华为、荣耀、OPPO/realme、一加、三星、摩托罗拉各 1 项、以及 Android 13+ 且带 Google Play 服务的 3 项官方广告隐私项沿用原有登记方式，本次未逐台真机回放匹配结果。

### 2026-11-12 10:28:41 +08:00 — 修正「开机自启动」显示：改读系统真实自启动状态（小米 AppOps 10008）

- 用户反馈：小米 22101317C 的系统「自启动管理」里净屏已允许（原来「允许 3 个」的第一项），净屏「服务与权限」页的「开机自启动」却一直是关闭。要求 APP 显示与系统一致、两个方向都能通，并在真机实测。
- 根因（三项叠加）：① `ClearScreenPermissionReader.readXiaomiStartup()` 用反射调 `android.miui.AppOpsUtils.getApplicationAutoStart(Context, String)`，该 helper 在设备 `/system_ext/framework/miui-framework.jar` 中为 `hiddenapi: BLOCKED`，第三方反射必失败，`runCatching` 把异常吞成 `null`；② 原生结果 `startupAllowed = null` 传到前端后，`ServicePermissionsScreen` 只把 `startupUnknown` 用于无障碍标签，`showState=false` 不渲染任何文字，于是界面画的是一个「关闭样式」的开关——即假关闭；③ 返回刷新链路本身没问题（`App.tsx` 在 `AppState` 变 `active` 时重拉权限快照），只是每次重读仍拿到 `null`。
- 取证：`adb shell appops get com.clearscreen.prototype` 显示 `MIUIOP(10008): allow`；在安全中心把净屏自启动关掉后该值立即变为 `ignore; rejectTime=...`，列表计数同步减少，确认 op 10008 与自启动开关是同一份状态。`dexdump` 显示 MIUI helper 内部就是 `AppOpsManager.checkOpNoThrow:(IILjava/lang/String;)I`，而公开 API 只有字符串 op 重载、编译期看不到数值重载。
- 改动：`android/app/src/main/java/com/clearscreen/prototype/backend/ClearScreenPermissionReader.kt` 的 `readXiaomiStartup()` 改为反射调用 `AppOpsManager.checkOpNoThrow(int, int, String)` 读 op `10008`（`MODE_ALLOWED → 已开启`、`MODE_IGNORED → 已关闭`、其余含无记录 → 未知），不使用 `setAccessible`、不解禁隐藏 API、不引入三方库；vivo 的 provider 读取（`bg_start_up_apps` 的 `currentstate` 0/1）与超时/取消逻辑未改动。`src/screens/ServicePermissionsScreen.tsx` 新增 `startupUnreadable`：读取不到时该行不再画关闭样式的开关，改为「开机自启动 / 无法读取 / 去设置」，整行可点，直接进入对应系统设置页（vivo 仍先弹说明再跳转）。`docs/wiki/权限状态核验.md` 同步更新小米来源、未知态表现与本次真机结论。
- 构建与安装：`npx tsc --noEmit` 通过（exit 0）；`cd android; $env:JAVA_HOME='C:\Users\22567\.gradle\jdks\eclipse_adoptium-17-amd64-windows.2'; .\gradlew.bat :app:assembleRelease --offline --console=plain` BUILD SUCCESSFUL（本机 `JAVA_HOME` 指向的 JDK 21 仍缺 `lib/jvm.cfg`，沿用 Adoptium 17 构建）；产物 `outputs/apk/release/app-release.apk`，包名 `com.clearscreen.prototype`、版本沿用工作区现状 1.0.97 / 103（本次不动版本号），大小 118,662,275 字节，SHA-256 `1BFD898556AA7FA1BAF39F95305C808A82CF927FEC8C09943932B0C205C3C704`。安装前确认无并行装卸（无其它 adb install / gradlew 在跑），用 `adb install -r` 覆盖安装返回 `Success`，未卸载、未 `pm clear`。
- 真机核对（小米 22101317C / HyperOS 2 / Android 14，1.0.97 / 103）：系统开（`MIUIOP(10008): allow`，安全中心列表含净屏）→ 「服务与权限」的「开机自启动」开关在右、无障碍描述「开机自启动，已开启」；在安全中心把净屏关掉（`appops get` 变 `ignore`）→ 回到 APP 显示「已关闭」，开关在左；在 APP 里点该行会直达安全中心「自启动管理」页，在系统里改回开启后返回 APP，无需杀进程即刷新为「已开启」。截图：`screenshots/2026-11-12-开机自启动-系统开启-APP显示已开启.png`、`screenshots/2026-11-12-开机自启动-系统关闭-APP显示已关闭.png`、`screenshots/2026-11-12-开机自启动-系统自启动管理-允许3个含净屏.png`。
- 排查副作用已恢复：调试期间用坐标点按 MIUI 自启动列表时误把哈啰、可话、肯德基、录音机打开（列表从「允许 3 个」涨到 7 个），已在系统页逐个关回，现为「允许 3 个」= 净屏 + 微信 ×2，与用户原始状态一致；`appops` 与安全中心列表均已一致。
- vivo 未验证：vivo V2502DA（10AG4M2KGF00527）本轮未连接，`adb devices` 只有小米。vivo 读取逻辑未改，读不到时走新增的「无法读取 · 去设置」界面分支；该分支只做了 TypeScript/Kotlin 静态检查，未在真机构造未知态实测。未覆盖项：小米无 AppOps 记录返回 `MODE_DEFAULT`、反射被平台限制时同样显示「无法读取」，两种情况均未构造实测。
- 涉及文件：`android/app/src/main/java/com/clearscreen/prototype/backend/ClearScreenPermissionReader.kt`、`src/screens/ServicePermissionsScreen.tsx`、`docs/wiki/权限状态核验.md`、本修改记录、上述三张截图。
### 2026-11-12 10:11:15 +08:00 — 小米系统推荐开关：把长期「未适配」的五项绑到真机存在的开关

- 用户提问「这个未适配是什么意思？你能帮我适配好吗？」，并附小米 22101317C 的系统推荐开关页截图，其中资讯推荐、服务推荐、内容精选、热点资讯、搜索框推荐词五行显示「未适配」。「未适配」指净屏在这台手机上没有确认到与该行精确对应的系统开关，因此不给可点开关，只显示状态标识并在点按时说明原因，避免误点业务控件。
- 取证（小米 22101317C / HyperOS 2 / Android 14，USB 调试）：用 `adb shell uiautomator dump` 读取负一屏设置页 `com.miui.personalassistant/.settings.PASettingActivity`，页面标题「负一屏」，只有「搜索框」「推荐卡片」「热门活动」「智能置顶」四个开关（搜索框开、推荐卡片关、热门活动关、智能置顶开）；该 Activity 已导出（`dumpsys package` 显示带 intent filter），shell 直接拉起成功，第三方应用可直达，不再需要用户从桌面右滑进入。用 uiautomator 读全局搜索设置页 `com.android.quicksearchbox/.preferences.SearchSettingsPreferenceActivity`，「首页展示模块」下为「搜索历史」「设置入口」「搜索提示词」「扫一扫」，「应用推荐」子页开关名为「展示应用推荐」，与净屏列表里的搜索框推荐词、应用推荐、搜索历史一一对应，「搜索框推荐词」的真实控件名就是「搜索提示词」。用 `dumpsys package` 读 `com.miui.newhome` 的 Activity 清单，内容中心的设置页 `.business.ui.settings.SettingActivity` 与 `.business.ui.notification.NotificationActivity` 均未导出，`.business.basicmode.BasicModeSettingsActivity` 等要求 `miui.permission.USE_INTERNAL_GENERAL_API`，shell 也无法启动；`com.miui.personalassistant` 的 `.service.aireco.setting.ui.RecommendationActivity` 与 `RecommendationNewActivity` 虽已导出，但进入前会先停在智能助理与内容中心的一次性隐私协议页（协议内容属用户本人同意事项，未代替用户点击）。检索 /system、/system_ext、/product、/vendor、/apex 下所有系统 APK 字符串：「资讯推荐」「服务推荐」「内容精选」零命中，「热点资讯」只命中 MIUIVideo、MIUINewHome、MIUIMusicT，结论是前四项在本机不是同名独立开关，属按 vivo 负一屏类推出的名称；内容中心 `com.miui.newhome` 的 APK 字符串里有「个性化内容推荐」「个性化广告推荐」「关闭内容推荐」「关闭广告推荐」「热点资讯」，是真机上对应的控制项。
- 改动：`android/app/src/main/java/com/clearscreen/prototype/backend/ClearScreenRecommendCatalog.kt` 删除小米列表里四条 `unreachable` 行（资讯推荐、服务推荐、内容精选、热点资讯），负一屏的推荐卡片、热门活动、智能置顶改为 `navigate = ROUTE_XIAOMI_ASSISTANT`（新增路由，直开负一屏设置页），全局搜索的搜索框推荐词补上「搜索提示词」标题别名并把页面定为「首页展示模块」，内容中心当时新增两项「个性化内容推荐」「内容中心广告推荐」，记为 `support = "manual"` 并带 `ROUTE_XIAOMI_CONTENT_CENTER`（新增路由，直开内容中心）与原因说明，后按用户「不能操作的不显示」的要求撤下（见 11:00 条目）；新增 `ROUTE_XIAOMI_ASSISTANT`、`ROUTE_XIAOMI_CONTENT_CENTER` 两个路由键。`android/app/src/main/java/com/clearscreen/prototype/backend/ClearScreenRecommendRoutes.kt` 新增负一屏设置页与内容中心两个跳转落点及其提示文案，并把它们加入小米「资讯与推荐」的候选顺序（全局搜索设置 → 浏览器设置 → 桌面设置 → 负一屏设置 → 内容中心 → 系统设置首页）。`src/screens/SystemRecommendScreen.tsx` 去掉负一屏三项操作前的「从桌面右滑进入负一屏」确认弹窗（现在由净屏直接打开该页），并给带跳转目标的「需手动」项在弹窗里增加「去打开」按钮（该分支后按 11:00 条目的要求撤下）。vivo、华为、荣耀、OPPO、一加、三星、摩托罗拉的条目未改动。
- 验证：`npx tsc --noEmit` 通过（exit 0）；`cd android; $env:JAVA_HOME='C:\Users\22567\.jdks\jbr-21.0.11'; .\gradlew.bat :app:compileDebugKotlin --offline --console=plain` BUILD SUCCESSFUL（56 秒），仅有既有的 `MainApplication.kt` 弃用告警。为确认改动后的匹配规则真能命中，把 `ClearScreenUiCleanup` 的行匹配算法按节点树复刻成脚本，喂真机 uiautomator dump 结果回放：负一屏页的「推荐卡片」「热门活动」「智能置顶」各自唯一命中一个开关（当前分别为已关、已关、开），「搜索提示词」在全局搜索「首页展示模块」页唯一命中一个开关（当前为开），「搜索历史」同样唯一命中；「应用推荐」在该页是导航行、本身没有开关，与「展示应用推荐」在子页的二级 stage 设计一致。`com.miui.personalassistant/.settings.PASettingActivity` 与 `com.miui.newhome/.business.ui.MainActivity` 均已导出（`dumpsys package` 显示带 intent filter），第三方应用可直接拉起。**未打包、未安装**，净屏端到端的点击与回读未在真机上验收；设备上并行窗口持续占用同一台机器且版本号已推进到 1.0.97 / 103，本次不动版本号、不安装。
- 遗留：内容中心的个性化开关要用户先在内容中心同意一次隐私协议才能进入设置页，净屏不代替用户同意，这两项最终不列入列表（见 11:00 条目）。

### 2026-11-12 09:56:25 +08:00 — 修正小米「资讯与推荐」跳转落点

- 用户反馈：系统推荐开关页红框那一行「资讯与推荐」跳到的手机设置页面不对。根因在 `android/app/src/main/java/com/clearscreen/prototype/backend/ClearScreenRecommendRoutes.kt` 的 `news()`：小米分支把桌面设置（`com.miui.home.Setting` → `com.miui.home.settings.MiuiHomeSettingActivity`）排在第一位，点进去是桌面设置页（只承载「智能助理」），与净屏自己的路径文案（在全局搜索或浏览器设置中检查）互相矛盾。
- 真机取证（小米 22101317C / HyperOS 2 / Android 14）：用 aapt2 读系统包清单确认 `com.android.quicksearchbox/.preferences.SearchSettingsPreferenceActivity`（全局搜索「搜索设置」）、`com.android.browser/.BrowserSettingsActivity`（浏览器设置）、`com.miui.home/.settings.MiuiHomeSettingActivity`（桌面设置）三个 Activity 都是 `exported=true`，第三方应用可以直接拉起；实测「搜索设置」页的「常规设置 → 首页展示模块」下正是「应用推荐」「搜索历史」「搜索提示词」三项，与净屏列表里的「搜索框推荐词 / 应用推荐 / 搜索历史」一一对应，而桌面设置只有「智能助理」（净屏已把它单列为一项，走 `ROUTE_XIAOMI_LAUNCHER`）。
- 改动：`news()` 的小米候选顺序改为 全局搜索设置 → 浏览器设置 → 桌面设置 → 系统设置首页；三个厂商页的提示文案改成该页面上实际该点的控件（搜索设置「首页展示模块」、浏览器「主页设置 / 宫格位推送」、桌面「智能助理」）。三处硬编码组件名抽成 `xiaomiSearchEntry` / `xiaomiBrowserEntry` / `xiaomiLauncherEntry` 三个入口工厂与常量，`ROUTE_XIAOMI_SEARCH` / `ROUTE_XIAOMI_BROWSER` / `ROUTE_XIAOMI_LAUNCHER` 复用同一份，避免同一页面多处维护。跳转成功后由原生层在系统设置页上给出一条提示，说明该点哪个控件（与已有的「开发者选项」提示同属系统页上的原生提示）；只有退到应用信息页（appDetails）时仍由应用内弹窗提示，该提示文案改为先指全局搜索的「搜索设置」。界面参考路径 `src/screens/SystemRecommendScreen.tsx` 的小米「资讯与推荐」改为「全局搜索 → 搜索设置 → 首页展示模块；浏览器 → 设置 → 主页设置」。
- 验证：`npx tsc --noEmit` 通过（exit 0）；`:app:assembleRelease --offline --console=plain` BUILD SUCCESSFUL（1 分 24 秒，251 个任务，42 executed）；APK 元数据为 `com.clearscreen.prototype`、`1.0.97` / `103`、应用名「净屏」，apksigner verify exit=0，正式证书 SHA-256 仍为 `93c410454a8263736ff60bb6e27153b19f74f4c934f444531842f5162ccfa999`；APK 大小 118,661,975 字节，SHA-256 `E57A8EBC4F3D75DF477D500B577092E4BE79119E1819223E369E76A227E48346`，`outputs/apk/净屏.apk` 与 `outputs/apk/release/app-release.apk` 字节一致。经 USB `adb install -r --no-streaming` 覆盖安装到小米 22101317C 返回 `Success`，读回 `1.0.97` / `103`，首次安装时间保持 2026-11-06 18:47:52，未卸载未清数据。
- 真机核对（小米 22101317C，1.0.97 / 103）：应用内「设置 → 系统广告 → 系统推荐开关」滚到底部点「资讯与推荐」，前台变为全局搜索的「搜索设置」页（含「首页展示模块」），同时出现净屏提示条「已打开全局搜索设置，点「首页展示模块」可关掉搜索推荐」；点进「首页展示模块」确认「卡片展示 → 应用推荐 / 搜索历史」「搜索框展示 → 搜索提示词」三项与净屏列表对应。截图：`screenshots/2026-11-12-系统推荐开关-跳转-资讯与推荐-搜索设置.png`、`screenshots/2026-11-12-系统推荐开关-跳转-首页展示模块.png`。
- 未验证与遗留：浏览器设置与桌面设置这两个回退落点本轮没有单独点击（第一候选即命中），浏览器页承载「宫格位推送」沿用既有结论；该机净屏的无障碍服务当前不在系统启用列表（`enabled_accessibility_services` 仅含 `com.linkpilot.android/.RemoteAccessibilityService`，安装前即如此，本次覆盖安装未改变），需要用户手动重新开启；本条只验证分类行跳转，不依赖无障碍服务。
- 版本口径：本次 1.0.97 自当前工作区构建，因此 09:37 记录的「快应用与系统广告组件」页改动（此前不在 1.0.96 的 APK 内）也包含在本次 APK 中；除这两项外无其他新增内容。
- 涉及文件：`android/app/src/main/java/com/clearscreen/prototype/backend/ClearScreenRecommendRoutes.kt`、`ClearScreenNativeModule.kt`、`ClearScreenSettingsNavigator.kt`、`src/screens/SystemRecommendScreen.tsx`、`android/app/build.gradle`、`app.json`、`README.md`、`docs/wiki/README.md`、本修改记录。

### 2026-11-12 09:52:00 +08:00 — 「关闭应用通知」列表按通知开关状态排序

- 用户要求：「关闭应用通知」页的列表把开关打开（通知仍开启）的应用排在前面，方便直接逐个关掉还在发通知的应用。改动前该页沿用原生返回的应用名称顺序（字母序），"还在发通知"的应用混在整份清单中间，需要逐屏翻找。
- 排序规则（改在 `src/screens/AppNotificationsScreen.tsx` 的 `visible` 计算里）：先按搜索关键字过滤，再按读取到的通知开关状态分三组排序——开启（`true`）排最前、关闭（`false`）次之、读取不到状态（`undefined`）排最后；组内保持原有名称顺序（用原索引作次级比较键，不打乱既有字母序观感）。未连接 ADB Shell 或 Shizuku 时所有应用状态都读不到，全部落入最后一组，列表顺序与改动前完全一致，不会出现"看起来没排序"的错觉。
- 只改前端排序：原生读取（`ClearScreenCleanupController.notificationStates` → `cmd appops query-op POST_NOTIFICATION`）、写入与回读确认流程、`Toggle` 的三态渲染都不动；界面没有新增任何说明文字，符合 `docs/wiki/README.md` 的界面文案约束。用户翻动某个开关后该应用会因状态变化而移动到对应分组，这是排序的直接结果。
- 验证：`npx tsc --noEmit` 通过；`:app:assembleRelease --offline --console=plain` BUILD SUCCESSFUL（2 分 14 秒，251 个任务）；APK 元数据为 `com.clearscreen.prototype`、`1.0.96` / `102`、应用名"净屏"，apksigner v2 验签通过，正式证书 SHA-256 保持 `93c410454a8263736ff60bb6e27153b19f74f4c934f444531842f5162ccfa999`；`outputs/apk/release/app-release.apk` 与 `outputs/apk/净屏.apk` 字节一致，大小 118,661,375 字节，SHA-256 `2CB9CA65DEBDFDB2810414C7490CF363FF850BCED99B1DC2DAF71169171576FD`。经 USB `adb install -r --no-streaming` 覆盖安装到小米 22101317C（Android 14）返回 `Success`，读回 `1.0.96` / `102`，首次安装时间保持 2026-11-06 18:47:52，未卸载未清数据。
- 真机核对（小米 22101317C / Android 14，1.0.96 / 102）：应用内进入「关闭应用通知」页，把整份列表从头到尾逐屏采集（每屏用 uiautomator 读出应用名与开关的 checked 状态，共 121 行、覆盖到列表最后一行「高德地图」）。结果：第 0–24 行开关全部为"开启"（Behance、Chrome、Facebook、TIM、WiFi万能钥匙极速版、下载管理、个人所得税、到点、国家反诈中心、基础信息、小米换机、小米有品、日历、时钟、淘宝、相册、短信、神州租车、网易云音乐、美团优选、美图秀秀、联控、腾讯会议、荣耀换机克隆、计算器），第 25–120 行全部为"关闭"（从 BOSS直聘、ClassIn、DejaVocab、DJI Fly、Google 到高德地图），分界处只有一个转折（最后一行"开启"是计算器，第一行"关闭"是 BOSS直聘），顺序违例数为 0。组内仍按名称顺序排列，与改动前一致。截图：`screenshots/2026-11-12-关闭应用通知-按开关排序-列表头部为已开启应用.png`、`screenshots/2026-11-12-关闭应用通知-按开关排序-列表尾部为已关闭应用.png`。
- 采样期间的干扰与处理：同一时段另一个 AI 会话正在操作这台小米手机，前台在 MIUI 自启动管理、桌面设置、系统广告页之间反复切换，前几轮滚动采集被系统页面打断；采集脚本改为逐屏落盘并只采信属于 `com.clearscreen.prototype` 的控件行，最后一次完整采样期间设备未再被抢占，才拿到上述连续样本。vivo V2502DA 本次未连接。
- 构建环境问题：本机 `JAVA_HOME` 指向的 `C:\Users\22567\jdk\jdk-21.0.12.1+1` 已被清空，Gradle 直接报 `could not open ...\lib\jvm.cfg`；本次改用 `C:\Users\22567\.jdks\jbr-21.0.11`（JetBrains Runtime 21.0.11）构建通过。需要把用户级/系统级 `JAVA_HOME` 重新指向可用 JDK，否则后续打包会直接失败。
- 版本口径与并发打包：本次 1.0.96 只包含本条的排序改动；同一工作区另一个窗口在 09:37 之后修改的 `src/screens/QuickComponentsScreen.tsx`（快应用与系统广告组件页）不在本 APK 内。该窗口随后把版本升到 1.0.97 / 103 并在 09:49 重新打包，`outputs/apk/release/app-release.apk` 与 `outputs/apk/净屏.apk` 已被覆盖为 1.0.97（SHA-256 `E57A8EBC4F3D75DF477D500B577092E4BE79119E1819223E369E76A227E48346`，118,661,975 字节，包名与应用名不变，正式证书不变），本次 1.0.96 的文件已不复存在；由于 `src/screens/AppNotificationsScreen.tsx` 自 09:28:39 起未再改动，两个包包含的排序逻辑一致。设备上当前仍安装本次验证过的 1.0.96 / 102（lastUpdateTime 09:33:25），本次没有覆盖安装 1.0.97；随后该窗口把 1.0.97 / 103 装到了设备上（lastUpdateTime 09:50:10，未卸载未清数据，首次安装时间保持 2026-11-06 18:47:52），排序源码自 09:28:39 起未变，故设备上运行的 1.0.97 与本次验证过的 1.0.96 排序逻辑一致。
- 涉及文件：`src/screens/AppNotificationsScreen.tsx`、`android/app/build.gradle`、`app.json`、`README.md`、`docs/wiki/README.md`、`screenshots/2026-11-12-关闭应用通知-按开关排序-列表头部为已开启应用.png`、`screenshots/2026-11-12-关闭应用通知-按开关排序-列表尾部为已关闭应用.png`、本修改记录。

### 2026-11-12 09:37:29 +08:00 — 快应用与系统广告组件页恢复检测结果行与行内状态说明

- 用户反馈该子页"一个都没有检测到"。真机取证（小米 22101317C，USB 读取 `dumpsys package`）：本机白名单三个包中 `com.miui.hybrid` 与 `com.miui.systemAdSolution` 的 User 0 均为 `suspended=true`（已被停用），`com.miui.hybrid.accessory` 未安装。即检查链路正常、检出 2 项，界面显示的两个蓝色开关与设备真实状态一致；"看不出检到了什么"的根因是检查结果行与行内状态说明已在 2026-11-08 12:02 按用户要求删除，页面只剩两个没有解释的开关，开关语义只存在于首次点按才弹的引导弹窗中。
- 按用户确认的方向修改（检测范围不变）：`src/screens/QuickComponentsScreen.tsx` 在列表上方恢复一行检查结果「已检查：本机 N 项，其中 M 项已被净屏关掉。」，每个组件行恢复状态说明「开：已停用，不会再自启或推广告」/「关：正常运行，会自启、推广告」/「状态读不到，暂时不能改」，行结构回到"标题 + 状态说明"两行容器 `rowText`（含新增样式 `rowHint`、`result`），右侧开关、`busy` 禁用与 `unknown` 未知态处理不变。
- 结果行与状态说明改为按当前 `targets` 实时推导（不再保存检查当时的快照），因此翻动开关后无需重新进入页面即可看到结果行与行内状态同步更新，修掉 2026-11-08 记录的"改完开关后检查结果不刷新"遗留。
- 检测范围按用户确认保持不变：小米白名单仍为 `com.miui.systemAdSolution`、`com.miui.hybrid`、`com.miui.hybrid.accessory` 三个包名；本机还装有 `com.miui.analytics`、`com.miui.yellowpage`、`com.miui.thirdappassistant`、`com.miui.guardprovider`、`com.miui.dmregservice`，按"名单外不自动操作、不误伤未知包"的既有边界不纳入检测。
- 验证：`npx tsc --noEmit` 通过（exit 0）；`git diff --check` 通过（仅既有换行符提示）。未打包、未安装、未做真机 UI 验收，页面上这一行结果和行内状态的实际观感待用户验收。
- 涉及文件：`src/screens/QuickComponentsScreen.tsx`、`README.md`、`docs/wiki/README.md`、本修改记录。

## 2026-11-09

### 2026-11-09 17:45:00 +08:00 — 修复学习代点识别不到不可点击的小型关闭控件

- 真机取证：彩云天气悬浮广告的关闭叉是 `ImageView`（约 41×55 像素），整棵无障碍树没有任何 `clickable="true"` 节点，该节点位于树第 21 层、只有六位十六进制内部描述（如 `18d5b4`）。它被 `ClearScreenAdLearning` 中「必须 `isClickable` 且暴露 `ACTION_CLICK`」的可点击性判定筛掉，选取时只能返回「未找到可点击控件」；同时当时的回放深度上限为 18，学到的规则也无法在后续帧里重新看到这个节点。
- 通用修正（不含应用名、包名、资源 ID 或坐标特例）：可见、可用、不危险、位于页面内、图标尺寸（宽高均不超过 72dp）且属于关闭语义或无有效标签的不可点击节点，允许被用户显式选中并保存选择器；点击时仍优先 `ACTION_CLICK`，只有节点确实不暴露点击动作时才在其中心发送 `dispatchGesture` 坐标手势。带标签的正文、大卡片、业务控件和受保护页面仍不参与。
- 广告 SDK 常把内部 token 填进 `content-desc`，现在把纯十六进制短 token 与 `virtualNode` 从标签中剔除，避免 `hasLabel` 误判和规则指纹随每次渲染变化。
- 学习、回放与关闭校验的默认读取深度由 18 提到 30，覆盖这类嵌套较深的广告控件树；节点数、子级数与超时预算不变。
- 验证：`:app:compileDebugKotlin --offline --console=plain`、`:app:assembleRelease --offline --console=plain` 通过，Release 类中已包含新增的坐标点击与标签过滤逻辑；实际安装版本为 1.0.95 / 101（本条早先按 1.0.94 / 100 记录有误，以设备读回为准），`adb install -r --no-streaming` 覆盖安装返回 `Success`（未卸载、未 `pm clear`）。
- 真机补测（2026-11-10 09:40–09:45 +08:00，vivo V2502DA / Android 16，1.0.95 / 101）：开启学习模式后在彩云天气开屏/插屏广告上完成「选择控件 → 点选关闭叉 → 确认选取」两轮，两轮均出现「已选中，请确认」，确认后被选中的小型 × 都被关闭，画面回到 `com.nowcasting.activity/.WeatherActivity`。截图：`screenshots/2026-11-10-学习选取小型关闭叉-1-已选中.png`、`-2-确认后关闭.png`、`-3-广告全关回主页.png`。
- 覆盖范围说明：该机广告叉实测为 `v_close` / `iv_close`，约 70×70px 且 `clickable="true"`，所以本轮真机验证走的是「可点击节点 → `ACTION_CLICK`」分支；新增的「不可点击图标 → `dispatchGesture` 坐标点击」分支和深度 30 读取在该机型上没有可复现样本，仍只有小米悬浮球取证（`tmp/cy5.xml`，整树 0 个 `clickable`）加静态确认。小米 22101317C（`2f8898c80822`）本轮全程离线，原始场景未能重现，需设备恢复后补测。
- 复测环境说明：期间屏幕一度切到 vivo「外部来源应用安装」页，经确认是另一 AI 会话正在控制该设备所致，非广告跳转；当时学习浮层提示「无法读取当前页面」并拒绝选取，未执行安装，该页已退出。
- 涉及文件：`android/app/src/main/java/com/clearscreen/prototype/backend/ClearScreenAdLearning.kt`、`docs/wiki/README.md`、`screenshots/2026-11-10-学习选取小型关闭叉-1-已选中.png`、`screenshots/2026-11-10-学习选取小型关闭叉-2-确认后关闭.png`、`screenshots/2026-11-10-学习选取关闭叉-3-广告全关回主页.png`、本修改记录。

### 2026-11-09 17:05:00 +08:00 — 修复一键连接无限转圈并补齐超时与重试

- 原生配对状态机补齐阶段超时：读不到配对码 120 秒、配对 60 秒、读不到连接端口 60 秒、连接 90 秒，超时统一转为 `failed` 并给出可读原因，不再无限停留在中间态；只在阶段变化时记录 `ClearScreenAdb` 日志，不记录配对码或端口。
- 无障碍服务在配对期间改为每 700 毫秒持续扫描并驱动超时，不再只依赖设置页的窗口事件，配对成功后的连接端口也能被读到；服务解绑时移除扫描回调。
- 服务与权限页移除会把整行永久置灰的 `adbRestoring` 待定判断；原生 ADB 调用加 6/15/45 秒超时和 150 秒页面级看门狗；失败时在 ADB 行下显示一行原因并给出「重试连接」，重试会从配对流程重新开始。
- 同步更新 README 与 Wiki 的 ADB 连接说明；版本升至 1.0.93 / 99。
- 验证：`npx tsc --noEmit`、`:app:compileDebugKotlin --offline --console=plain`、`:app:assembleRelease --offline --console=plain` 通过；已用 `adb install -r --no-streaming` 覆盖安装到小米真机 22101317C（未卸载、未 `pm clear`，安装前确认无并行安装），安装后为 `versionCode=99`、`versionName=1.0.93`。
- 真机取证与验收：设备当时锁屏（安全锁），`screencap` 返回 0 字节，无法操作界面；已确认故障背景为净屏无障碍服务未启用（`enabled_accessibility_services` 仅含 `com.linkpilot.android`）且 `adb_wifi_enabled=0`。成功态与失败态截图、超时日志未执行，待解锁后补测。
- 涉及文件：`android/app/src/main/java/com/clearscreen/prototype/backend/ClearScreenAdbPairingHelper.kt`、`ClearScreenAccessibilityService.kt`、`src/native/ClearScreenNative.ts`、`src/screens/ServicePermissionsScreen.tsx`、`README.md`、`docs/wiki/README.md`、`app.json`、`android/app/build.gradle`、本修改记录。

## 2026-11-08

### 2026-11-08 22:23:16 +08:00 — 核对并修正小米系统推荐开关绑定

- 修正负一屏目标包和页面：推荐卡片、热门活动、智能置顶匹配 `com.miui.personalassistant` 的「负一屏」设置；用户先从桌面进入该页。补充小米相关系统包的定向可见性声明，并保留系统应用、页面、标签及状态核验。
- 修复单一开关被系统暴露成内外两层节点时无法识别的问题；搜索和浏览器多级导航需核对每级页面，并持续等待回读。用户明确选择开启时不再要求开关必须先由净屏关闭；未知状态先由用户选择目标状态。
- 资讯推荐、服务推荐、内容精选、热点资讯在现有小米负一屏资料中未发现对应独立开关；搜索框推荐词仅发现影响整个搜索功能的总开关。这五项标记未适配并可查看原因；三个分类行改为设置页面入口，避免复用无关开关。
- 只读核查设备系统包与既有页面快照；小米全局搜索 12.10.0.07203 的静态资源包含「展示应用推荐」复选项（REA 包身份 `ev_e187d8da207b490f102c7fe03a5eb5886e51975b2fd5eea99a6a029997fb3811`），不代表实际操作已验收。同步更新 README、Wiki 和项目验证与复用说明。
- 验证：`npx tsc --noEmit`、`:app:compileDebugKotlin :app:processDebugMainManifest --offline --console=plain --quiet`、相关文件 `git diff --check` 通过；未进行功能测试或真机界面验收，未打包或安装。
- 涉及文件：`src/screens/SystemRecommendScreen.tsx`、`src/native/ClearScreenNative.ts`、`android/app/src/main/AndroidManifest.xml`、`android/app/src/main/java/com/clearscreen/prototype/backend/ClearScreenAccessibilityService.kt`、`ClearScreenCleanupController.kt`、`ClearScreenRecommendCatalog.kt`、`ClearScreenUiCleanup.kt`、`README.md`、`docs/wiki/README.md`、本修改记录，以及系统秘钥目录中的 `ClearScreen/验证与复用说明.md`。

### 2026-11-08 22:08:40 +08:00 — 移除 ADB 手动连接备用卡片

- 删除服务与权限页中的手动端口输入、连接按钮和本机地址卡片；自动配对/重连及连接状态开关保留，失败时显示提示，可再次点按开关重试。
- 同步更新 README 与 Wiki 对当前连接流程的说明。
- 验证：`npx tsc --noEmit` 与 `git diff --check` 通过；未进行应用界面或真机验收。
- 涉及文件：`src/screens/ServicePermissionsScreen.tsx`、`README.md`、`docs/wiki/README.md`、本修改记录。

### 2026-11-08 22:08:04 +08:00 — 精简营销通知渠道条目

- 移除各渠道行中的重要性文字，将应用名称合并到渠道标题；完整清单中存在分组名时仍保留分组名。同步调整无障碍标签，避免重复应用名。
- 验证：未执行。
- 涉及文件：`src/screens/MarketingChannelsScreen.tsx`、本修改记录。

### 2026-11-08 22:06:25 +08:00 — 删除系统推荐开关页的分组标题和空组提示

- 删除「需要开启」「需要关闭」分组标题及空组「暂无项目」提示；系统推荐开关列表按原顺序连续展示，目标判断和操作逻辑不变。
- 同步更新 Wiki。
- 验证：`npx tsc --noEmit` 与涉及文件的 `git diff --check` 通过；未进行应用界面或真机验收。
- 涉及文件：`src/screens/SystemRecommendScreen.tsx`、`docs/wiki/README.md`、本修改记录。

### 2026-11-08 22:04:45 +08:00 — 移除开机自启动旁的待确认文字

- 服务与权限页不再在「开机自启动」行内显示「待确认」文字，未确认状态仍保留在无障碍状态说明中；同步更正 README 与权限核验 Wiki。
- 验证：未执行。
- 涉及文件：`src/screens/ServicePermissionsScreen.tsx`、`README.md`、`docs/wiki/README.md`、`docs/wiki/权限状态核验.md`、本修改记录。

### 2026-11-08 21:44:43 +08:00 — 覆盖安装净屏 Release 1.0.92 到小米手机

- Release 构建成功且 Gradle 判定产物为最新；安装包 SHA-256：`078C3CC091B6F8A909D4E4AFD7B353E9AF39D89490C8BF4321CAAFB6A0EBA21E`。
- 使用 `adb install -r` 安装到 Xiaomi 22101317C（`2f8898c80822`），返回 `Success`。设备读回版本 `1.0.92 / 98`，首次安装时间保持 `2026-11-06 18:47:52`，更新安装时间为 `2026-11-08 21:43:52`；净屏无障碍服务仍启用。未启动应用或进行界面验收。
- 验证：`:app:assembleRelease --offline --console=plain` 成功，APK 散列与当前 Release 记录一致；覆盖安装与设备状态读回通过。
- 涉及文件：本修改记录；APK：`android/app/build/outputs/apk/净屏.apk`。

### 2026-11-08 18:49:06 +08:00 — 营销通知改为已连接读完整清单、未连接只观察部分渠道

- 「营销通知」按混合方案拆分读取路径：已连接应用内直接改设置通道（ADB Shell 或 Shizuku）时，用系统诊断输出读取完整渠道清单，行为与之前一致；未连接时通过「通知使用权」观察正在发通知或刚发生变更的渠道，名称不可得时按「应用名 · 营销渠道」呈现，页面标注结果不完整并提供「去连接补全」入口；未连接且没有观察到渠道时显示「未连接，暂未观察到营销渠道」，不再显示「未发现候选渠道」。
- 确认并记录系统限制：普通第三方应用即使已授予「通知使用权」，调用 `getNotificationChannels` 仍会被拒绝（`SecurityException ... isSystem=false ... does not have access`），该接口只对系统级监听服务开放，不能作为完整清单方案。监听服务仅记录渠道 id 与元数据，不读取通知标题或正文。
- 兼容已有授权：未连接时若已授予「通知使用权」自动进入观察扫描；两者都没有时弹窗说明未连接结果不完整，并提供「去授予权限」与「去连接」两个入口。已连接时隐藏未连接说明。
- 验证：`npx tsc --noEmit`、`:app:compileDebugKotlin --offline --console=plain`、`:app:assembleRelease --offline --console=plain` 通过；Release APK 为 `1.0.92 / 98`，SHA-256 `078C3CC091B6F8A909D4E4AFD7B353E9AF39D89490C8BF4321CAAFB6A0EBA21E`，大小 118,662,035 字节。通过 USB `adb install -r --no-streaming` 覆盖安装到 Xiaomi 22101317C（`2f8898c80822`）成功，设备读回 `1.0.92 / 98`，`firstInstallTime` 保持 `2026-11-06 18:47:52`，`lastUpdateTime=2026-11-08 18:44:26`。
- 真机验收：已连接状态读取完整清单（营销推广等渠道正常显示）；断开 ADB 后显示未连接说明、连接入口与空态；已连接时点按渠道行可进入对应 Android 通知渠道设置页（实测「营销推广」）。截图见 `screenshots/2026-11-08-营销通知-已连接.png`、`screenshots/2026-11-08-营销通知-未连接.png`。
- 涉及文件：`src/screens/MarketingChannelsScreen.tsx`、`App.tsx`、`android/app/src/main/java/com/clearscreen/prototype/backend/ClearScreenNotificationChannels.kt`、`ClearScreenCleanupController.kt`、`README.md`、`docs/wiki/README.md`、`app.json`、`android/app/build.gradle`、本修改记录。

### 2026-11-08 18:19:20 +08:00 — 修复重启后的服务状态与 ADB 自动重连

- 已配对且未主动断开的 ADB 连接在应用启动或回到前台时尝试恢复；优先重试已保存且仍属于本机的连接地址，失败后自动查找。主动断开会持久保留，撤回隐私同意时停止连接并清除保存的地址与端口；配对和连接操作相互串行并检查取消状态。
- 自动跳过服务独立依据系统授权和服务健康状态展示，检查中显示进度，无法确认或服务未就绪时如实显示状态。ADB 恢复期间显示进度，连接成功后才显示开启，失败后保留手动连接入口。
- 只读核查小米设备：系统记录显示安全中心于 17:29:59 强行停止净屏，当前系统无障碍启用列表已无净屏服务，无线调试仍开启；该系统授权需用户自行重新开启。同步修正 README 和 Wiki 的恢复及状态说明。
- 验证：`npx tsc --noEmit`、`:app:compileDebugKotlin --offline --console=plain --quiet`、相关文件的 `git diff --check` 通过。未运行功能测试或真机界面验收，未打包或安装。
- 涉及文件：`App.tsx`、`src/screens/ServicePermissionsScreen.tsx`、`src/native/ClearScreenNative.ts`、`android/app/src/main/java/com/clearscreen/prototype/backend/ClearScreenAdbOperations.kt`、`ClearScreenAdbPairingHelper.kt`、`ClearScreenNativeModule.kt`、`README.md`、`docs/wiki/README.md`、`docs/wiki/权限状态核验.md`、本修改记录。

### 2026-11-08 17:47:06 +08:00 — 修正小米自启动搜索页的状态核验

- 小米自启动设置进入搜索模式时，允许在已确认的同一系统窗口内只读核验净屏的唯一开关；系统识别的输入法窗口不结束会话，并跳过搜索输入框的文字和子节点。两次一致读取和原有身份校验仍保留。
- 页面核验结果在私有查询完成后再次优先读取；自启动状态无法核验时显示「待确认」，避免把未知状态描述为关闭。同步更新 README 与 Wiki。
- 验证：`npx tsc --noEmit`、`:app:compileDebugKotlin --offline --console=plain` 和相关文件的 `git diff --check` 通过；未运行功能测试或真机界面验收，未打包或安装。
- 涉及文件：`android/app/src/main/java/com/clearscreen/prototype/backend/ClearScreenSettingsObserver.kt`、`ClearScreenAccessibilityService.kt`、`ClearScreenPermissionReader.kt`、`src/screens/ServicePermissionsScreen.tsx`、`README.md`、`docs/wiki/README.md`、`docs/wiki/权限状态核验.md`、本修改记录。

### 2026-11-08 17:20:00 +08:00 — 更新小米上的净屏 APK 至 1.0.90

- 版本由 `1.0.89 / 95` 升至 `1.0.90 / 96`；Release 构建成功。正式签名证书 SHA-256 保持 `93c410454a8263736ff60bb6e27153b19f74f4c934f444531842f5162ccfa999`；Gradle 原始 APK 与 `android/app/build/outputs/apk/净屏.apk` 哈希一致，SHA-256 为 `46AB7FC696517C89B35ED18D861F2CCD7476DAB070E2F0CCB6964CE823D7D4EA`，大小 118,644,031 字节。
- 通过 USB `adb install -r --no-streaming` 覆盖安装到 Xiaomi 22101317C（`2f8898c80822`）成功。设备读回 `1.0.90 / 96`，`firstInstallTime` 保持 `2026-11-06 18:47:52`，`lastUpdateTime=2026-11-08 17:19:39`；无障碍启用列表仍包含净屏服务。未卸载、清除数据或启动应用，未进行功能/UI验收。
- 验证：`:app:assembleRelease --offline --console=plain` 成功；APK 元数据与正式签名验证通过；安装后包信息和无障碍服务状态回读通过。
- 涉及文件：`android/app/build.gradle`、`app.json`、`android/app/build/outputs/apk/净屏.apk`、本修改记录。

### 2026-11-08 15:57:32 +08:00 — 在营销通知结果中显示实际通知状态

- 营销通知候选项右侧改为状态开关，状态由通知渠道重要性读取；应用级通知关闭或通知分组关闭时也显示为关闭，无法解析状态时显示为未知。扫描会保留已关闭但符合筛选条件的候选渠道。
- 点按开关或渠道名称进入对应 Android 通知设置，返回页面后自动重新扫描并刷新状态；应用不直接改写通知渠道。
- 同步更新 README 与 Wiki 的状态说明。
- 验证：`npx tsc --noEmit`、`:app:compileDebugKotlin --offline --console=plain`、`git diff --check` 通过；未打包、安装或进行应用界面/真机验收。
- 涉及文件：`src/screens/MarketingChannelsScreen.tsx`、`src/components/UI.tsx`、`src/native/ClearScreenNative.ts`、`android/app/src/main/java/com/clearscreen/prototype/backend/ClearScreenShellOps.kt`、`android/app/src/main/java/com/clearscreen/prototype/backend/ClearScreenCleanupController.kt`、`README.md`、`docs/wiki/README.md`、本修改记录。

### 2026-11-08 15:56:54 +08:00 — 移除快应用与系统广告组件页的检查卡片

- 按截图删除子页中的检查/重新检查按钮、状态结果及连接卡片；进入子页后自动检查，未连接时提示配对并在连上后自动检查。检查失败仍通过错误提示反馈，组件列表和开关保留。
- 同步更新 README 与 Wiki 的页面流程说明。
- 验证：`npx tsc --noEmit` 与目标文件/文档的 `git diff --check` 通过；未进行应用界面或真机验收，未打包或安装。
- 涉及文件：`src/screens/QuickComponentsScreen.tsx`、`README.md`、`docs/wiki/README.md`、本修改记录。

### 2026-11-08 15:52:40 +08:00 — 将系统推荐开关分为开启和关闭两类

- 将系统推荐列表按关闭广告或推荐功能所需的系统开关状态分为「需要开启」与「需要关闭」；方向取自原生适配定义的反向开关语义，不改变状态读取和开关操作逻辑。无项目时显示空状态。
- 同步更新 Wiki 说明。
- 验证：`npx tsc --noEmit` 与 `:app:compileDebugKotlin --offline --console=plain` 通过；未进行应用界面或真机验收，未打包或安装。
- 涉及文件：`src/screens/SystemRecommendScreen.tsx`、`src/native/ClearScreenNative.ts`、`android/app/src/main/java/com/clearscreen/prototype/backend/ClearScreenNativeModule.kt`、`docs/wiki/README.md`、本修改记录。

### 2026-11-08 15:50:13 +08:00 — 修正应用通知开关成功后误报失败

- 原生层返回包含 result 字段的对象，界面层却按字符串判断，导致系统改写成功后仍显示失败。现在解析原生结果，并将 README 与 Wiki 改为准确说明该功能通过 ADB Shell 或 Shizuku 修改并回读系统通知状态。
- 验证：TypeScript 静态检查未通过，错误来自本次未修改的 QuickComponentsScreen.tsx（缺少 ActivityIndicator 导入，且将原因列表按 string 类型使用）；git diff --check 通过。未进行真机验收，未打包或安装。
- 涉及文件：src/native/ClearScreenNative.ts、README.md、docs/wiki/README.md、本修改记录。

### 2026-11-08 15:49:47 +08:00 — 快应用组件子页进入后自动显示开关

- 子页打开时若无线调试或 Shizuku 连接已可用，自动读取一次组件状态并显示实际组件开关；后续仍可手动重新检查。连接不可用时不自动读取、不展示未经核验的开关状态，保留原有连接入口。
- 同步更新 README 与 Wiki。检查结果继续不显示发现数量。
- 验证：`npx tsc --noEmit` 与涉及文件的 `git diff --check` 通过；未进行应用界面或真机验收，未打包或安装。
- 涉及文件：`src/screens/QuickComponentsScreen.tsx`、`README.md`、`docs/wiki/README.md`、本修改记录。

### 2026-11-08 15:48:58 +08:00 — 移除应用通知搜索框内层边线

- 移除搜索输入区域的内侧边线，保留外层卡片、搜索图标、输入和背景。
- 验证：`npx tsc --noEmit` 与目标文件的 `git diff --check` 通过；未进行应用界面或真机验收，未打包或安装。
- 涉及文件：`src/screens/AppNotificationsScreen.tsx`、本修改记录。

### 2026-11-08 15:46:50 +08:00 — 修正小米自启动开关已开启但净屏显示关闭

- 小米自启动管理是长列表时，输入搜索词会触发只读重扫；此前没有处理文字变化事件，导致初始列表超过节点预算后无法核验净屏这一行。
- 本次引导中已稳定读取到的净屏自启动开关优先于小米私有查询，避免系统查询覆盖当前可见开关。
- 涉及文件：`android/app/src/main/java/com/clearscreen/prototype/backend/ClearScreenAccessibilityService.kt`、`android/app/src/main/java/com/clearscreen/prototype/backend/ClearScreenPermissionReader.kt`、`docs/wiki/权限状态核验.md`、本修改记录。
- 验证：`npx tsc --noEmit`、`:app:compileDebugKotlin --offline --console=plain`、目标文件 `git diff --check` 均通过；未运行功能/UI验收，未打包或安装。

### 2026-11-08 15:45:11 +08:00 — 删除快应用组件检查完成数量提示

- 检查成功后不再显示发现数量文字；组件开关列表和未发现时的空态保留。
- 同步更新 README 与 Wiki。
- 验证：`npx tsc --noEmit` 与相关文件的 `git diff --check` 通过；未进行应用界面或真机验收，未打包或安装。
- 涉及文件：`src/screens/QuickComponentsScreen.tsx`、`README.md`、`docs/wiki/README.md`、本修改记录。

### 2026-11-08 15:44:07 +08:00 — 移除系统推荐开关的确认弹窗

- 点击可操作的系统推荐开关后直接执行开关操作；未开启无障碍、无法恢复原状态及操作失败等提示保留。
- 验证：未执行功能或真机验收，未打包或安装。
- 涉及文件：`src/screens/SystemRecommendScreen.tsx`、本修改记录。

### 2026-11-08 15:42:08 +08:00 — 删除营销通知页顶部说明

- 删除营销通知列表上方的自动扫描与通知内容隐私说明；连接、自动扫描和结果展示逻辑保持不变。
- 验证：`npx tsc --noEmit` 与目标文件的 `git diff --check` 通过；未进行应用界面或真机验收，未打包或安装。
- 涉及文件：`src/screens/MarketingChannelsScreen.tsx`、本修改记录。

### 2026-11-08 15:42:11 +08:00 — 移除营销通知渠道跳转确认弹窗

- 点击扫描结果中的通知渠道时，直接打开对应的系统渠道设置页；保留未连接时的连接提示和打开失败提示。
- 验证：`npx tsc --noEmit` 与 `git diff --check` 通过；未进行应用界面或真机验收，未打包或安装。
- 涉及文件：`src/screens/MarketingChannelsScreen.tsx`、本修改记录。

### 2026-11-08 15:10:59 +08:00 — 构建并覆盖安装净屏 1.0.89 到小米手机

- 将 Android 版本升至 1.0.89 / versionCode 95；Release APK 为 `android/app/build/outputs/apk/净屏.apk`，包名 `com.clearscreen.prototype`，大小 118,645,859 字节，SHA-256 `BA39B9F59079570C46E4F5D9EAAB21A69E6E0AFF27148378A493C1565F9125C9`。Gradle 原始 APK 与命名 APK 哈希一致；aapt 元数据、apksigner v2 验签通过，正式证书 SHA-256 保持 `93c410454a8263736ff60bb6e27153b19f74f4c934f444531842f5162ccfa999`。
- `:app:assembleRelease --offline --console=plain` 成功；通过 USB `adb install -r --no-streaming` 覆盖安装到 Xiaomi 22101317C（`2f8898c80822`）返回 `Success`。安装后读回 1.0.89 / 95，首次安装时间保持 `2026-11-06 18:47:52`，`lastUpdateTime=2026-11-08 15:09:10`，无障碍启用列表仍包含净屏服务。未卸载、清除应用数据或启动应用；未进行功能/UI验收。
- 涉及文件：`android/app/build.gradle`、`app.json`、本修改记录。

### 2026-11-08 14:54:35 +08:00 — 服务与权限页以开关显示 ADB 实际连接状态

- 将「ADB Shell 操作能力」箭头入口改为实时状态开关：只有原生连接状态确认成功时开启；关闭时发起自动配对或重连，开启时断开。自动连接失败后的手动端口输入留在本页，不再进入独立连接子页；首次连接直接启动配对流程。
- 移除连接子页及其应用导航路由；系统推荐页和应用通知页需要连接时改为打开「服务与权限」。同步更新 README 与 Wiki。
- 验证：`npx tsc --noEmit` 与涉及文件的 `git diff --check` 通过；未进行应用界面或真机验收，未打包或安装。
- 涉及文件：`App.tsx`、`src/screens/ServicePermissionsScreen.tsx`、`src/screens/SystemRecommendScreen.tsx`、已删除的 `src/screens/AdbAccessScreen.tsx`、`README.md`、`docs/wiki/README.md`、本修改记录。

### 2026-11-08 14:53:46 +08:00 — 将系统广告与营销通知拆成独立入口

- 设置首页分别显示「系统广告」和「营销通知」；系统广告页保留系统推荐开关、应用通知和快应用/系统广告组件，营销通知入口直接打开渠道扫描页。
- 将帮助与反馈中的说明拆分，并同步更新 README 与 Wiki 的页面结构和入口路径。
- 验证：`npx tsc --noEmit` 与涉及文件的 `git diff --check` 通过；未进行应用界面或真机验收，未打包或安装。
- 涉及文件：`App.tsx`、`src/screens/SettingsScreen.tsx`、`src/screens/SystemCleanupScreen.tsx`、`src/screens/SettingsDetailScreen.tsx`、`README.md`、`docs/wiki/README.md`、本修改记录。

### 2026-11-08 14:50:28 +08:00 — 营销通知页改为进入后自动扫描

- 移除扫描按钮和扫描确认弹窗；页面进入后检查授权连接，有连接时自动扫描一次，未连接时保留连接提示，连接后自动启动扫描。页面保留一行隐私告知、扫描状态、错误和结果；重新进入页面时会再次扫描。
- 同步更新 README 与 Wiki 的页面交互说明。
- 验证：`npx tsc --noEmit` 与目标文件的 `git diff --check` 通过；未进行应用界面或真机验收，未打包或安装。
- 涉及文件：`src/screens/MarketingChannelsScreen.tsx`、`README.md`、`docs/wiki/README.md`、本修改记录。

### 2026-11-08 14:46:19 +08:00 — 补快应用组件检查完成反馈

- 截图中的检查已列出「快应用服务框架」和「系统广告服务」两项，但成功后按钮恢复原文案且没有完成提示，容易被看成未响应。现在有结果时显示「检查完成，发现 N 项」，按钮改为「重新检查」；无结果时保留「未检测到可处理组件」空态。
- 检查仍只读取组件状态，不自动修改组件。
- 验证：未执行。
- 涉及文件：`src/screens/QuickComponentsScreen.tsx`、`README.md`、`docs/wiki/README.md`、本修改记录。

### 2026-11-08 14:44:50 +08:00 — 将组件开关拆分为独立卡片

- 检查按钮与状态信息留在上方卡片；将检查到的每个快应用或系统广告组件开关各自放入独立卡片，显示在下方；保留原有组件操作逻辑。
- 验证：`npx tsc --noEmit` 与 `git diff --check` 通过；未进行应用界面或真机验收，未打包或安装。
- 涉及文件：`src/screens/QuickComponentsScreen.tsx`、本修改记录。

### 2026-11-08 14:44:20 +08:00 — 移除自动连接无线调试的首次说明弹窗

- 按截图删除点击「连接无线调试」时出现的首次说明弹窗；点击后直接按当前状态自动连接，或启动配对并打开系统设置。缺少必要无障碍服务和连接失败时的提示保留。
- 同步更新 Wiki 的连接页交互说明。
- 验证：未执行。
- 涉及文件：`src/screens/AdbAccessScreen.tsx`、`docs/wiki/README.md`、本修改记录。

### 2026-11-08 14:43:41 +08:00 — 删除营销通知页常驻说明

- 移除扫描按钮上方的候选渠道说明，扫描流程和渠道列表保持不变；同步删除 Wiki 中对应的常驻提示例外。
- 验证：TypeScript 静态检查与 git diff --check 通过；未进行界面或真机验收，未打包或安装。
- 涉及文件：src/screens/MarketingChannelsScreen.tsx、docs/wiki/README.md、本修改记录。

### 2026-11-08 14:42:09 +08:00 — 将通知入口名称改为「应用通知」

- 设置入口从「关闭应用通知」改为「应用通知」，搜索应用并进入系统通知设置的功能不变；同步更新 README 与 Wiki 当前页面说明。
- 验证：`git diff --check` 通过；未进行应用界面或真机验收，未打包或安装。
- 涉及文件：`src/screens/SystemCleanupScreen.tsx`、`README.md`、`docs/wiki/README.md`、本修改记录。

### 2026-11-08 14:41:10 +08:00 — 删除连接成功说明并精简负一屏开关名称

- ADB Shell 操作能力页删除已连接时的重连说明和其下方分隔线，保留「已连接」状态及「断开」操作。
- 小米负一屏该项名称从「智能助理（负一屏总开关）」简化为「智能助理」，保持页面匹配和操作逻辑不变。
- 验证：未执行。
- 涉及文件：`src/screens/AdbAccessScreen.tsx`、`android/app/src/main/java/com/clearscreen/prototype/backend/ClearScreenRecommendCatalog.kt`、`docs/wiki/README.md`、本修改记录。

### 2026-11-08 14:40:01 +08:00 — 合并系统推荐开关页面的两个列表区块

- 将设备推荐开关列表与手动设置入口列表放入同一卡片，中间不再保留卡片外边距。
- 验证：涉及文件的 `git diff --check` 通过；未进行应用界面或真机验收，未打包或安装。
- 涉及文件：`src/screens/SystemRecommendScreen.tsx`、本修改记录。

### 2026-11-08 14:39:23 +08:00 — 移除 ADB Shell 连接成功弹窗

- 手动或自动连接成功后不再显示确认弹窗，页面连接状态照常更新；连接失败和操作异常提示保留。
- 验证：TypeScript 静态检查与 `git diff --check` 通过；未执行功能或真机验收，未打包或安装。
- 涉及文件：`src/screens/AdbAccessScreen.tsx`、本修改记录。

### 2026-11-08 14:38:45 +08:00 — 移除未连接状态行并精简无线调试按钮文案

- 未连接时不再显示「未连接」状态行和分隔留白；已连接时保留「已连接」与断开操作。按钮从「自动连接无线调试」改为「连接无线调试」，自动配对及连接流程不变。
- 同步更新 README 与 Wiki 的页面说明。
- 验证：`npx tsc --noEmit` 与涉及文件的 `git diff --check` 通过；未进行应用界面或真机验收，未打包或安装。
- 涉及文件：`src/screens/AdbAccessScreen.tsx`、`README.md`、`docs/wiki/README.md`、本修改记录。

### 2026-11-08 14:37:14 +08:00 — 合并无线调试的自动连接与失败备用流程

- 将连接页收敛为单一「连接无线调试」入口：首次使用自动启动配对并打开系统设置，已配对时自动查找并连接；自动配对或连接失败后展开手动端口输入，连接成功后隐藏备用区。
- 同步更新首次使用提示、README 与 Wiki 的连接流程说明。
- 验证：`npx tsc --noEmit` 与 `git diff --check` 通过；未启动应用或进行真机验收，未打包或安装。
- 涉及文件：`src/screens/AdbAccessScreen.tsx`、`README.md`、`docs/wiki/README.md`、本修改记录。

### 2026-11-08 14:35:18 +08:00 — 构建并升级安装净屏 1.0.88 到小米手机

- Android 版本由 1.0.87 / versionCode 93 升至 1.0.88 / 94；正式 Release 构建成功，用户安装包为 `android/app/build/outputs/apk/净屏.apk`，包名 `com.clearscreen.prototype`，应用名「净屏」。
- APK SHA-256：`FF86A9709C01F222A094E783CAA0286F380264811B624B57231A800112630240`；正式签名证书 SHA-256 与项目记录一致：`93c410454a8263736ff60bb6e27153b19f74f4c934f444531842f5162ccfa999`。Gradle APK 与命名 APK 的 SHA-256 一致。
- 使用 USB `adb install -r --no-streaming` 覆盖安装到 Xiaomi 22101317C，返回 `Success`。安装后读回 1.0.88 / 94，`lastUpdateTime=2026-11-08 14:34:55`；`firstInstallTime=2026-11-06 18:47:52` 保持不变，净屏无障碍服务仍在系统启用列表。未卸载、未清除数据，也未启动应用或做功能/UI验收。
- 验证：`:app:assembleRelease --offline --console=plain` 成功；APK 包名、版本、应用名、签名与散列核验通过；覆盖安装和安装后状态读取通过。
- 涉及文件：`android/app/build.gradle`、`app.json`、`docs/wiki/CHANGELOG.md`；APK：`android/app/build/outputs/apk/净屏.apk`。

### 2026-11-08 14:25:18 +08:00 — 将授权入口统一命名为「ADB Shell 操作能力」

- 按截图将「服务与权限」中的入口名称改为「ADB Shell 操作能力」，并同步授权连接页标题、结果提示及依赖该通道的错误提示，保持用户从入口到子页看到同一名称。
- 同步更新 README 与 Wiki 中的当前页面名称说明；未改变无线调试配对、连接和系统操作逻辑。
- 验证：`git diff --check` 通过；未执行应用界面或真机验收，未打包或安装。
- 涉及文件：`src/screens/ServicePermissionsScreen.tsx`、`src/screens/AdbAccessScreen.tsx`、`src/screens/SystemRecommendScreen.tsx`、`src/screens/AppNotificationsScreen.tsx`、`README.md`、`docs/wiki/README.md`、本修改记录。

### 2026-11-08 14:24:10 +08:00 — 将服务与权限状态改为开关指示

- 自动跳过服务、开机自启动、后台运行和电池优化改为统一开关外观；只有经核验明确可用时显示开启，未授权、未知、核验中或失败时显示关闭。点按整行仍进入对应系统设置，返回后沿用现有权限核验刷新。
- 同步更新 README 与 Wiki 的状态展示说明。
- 验证：`npx tsc --noEmit` 与涉及文件的 `git diff --check` 通过；未进行应用界面或真机验收，未打包或安装。
- 涉及文件：`src/components/UI.tsx`、`src/screens/ServicePermissionsScreen.tsx`、`README.md`、`docs/wiki/README.md`、本修改记录。

### 2026-11-08 14:23:48 +08:00 — 删除快应用子页顶部说明和空闲未连接提示

- 按截图移除快应用与系统广告组件页的顶部说明，以及未连接时空闲显示的「还没连上这台手机，检查前需要先连一次。」；检查/连接按钮、实际操作状态与错误、检查结果和组件开关保留。
- 同步更新 README 与 Wiki。
- 验证：未执行。
- 涉及文件：`src/screens/QuickComponentsScreen.tsx`、`README.md`、`docs/wiki/README.md`、本修改记录。

### 2026-11-08 14:21:58 +08:00 — 将小米负一屏细分推荐改为开关
- 将「资讯推荐、服务推荐、内容精选、热点资讯、推荐卡片、搜索框推荐词、热门活动、智能置顶」从「需手动」标识改为独立开关。启动操作后返回桌面，用户进入负一屏设置；无障碍服务仅在受信任桌面、页面标题和目标开关精确匹配并连续读取一致时操作一次、回读结果。
- 在操作确认中补充负一屏入口路径，并同步 README 与 Wiki。
- 验证：`npx tsc --noEmit`、`:app:compileDebugKotlin --offline --console=plain`、`git diff --check` 均通过；未进行应用界面或真机验收，未打包或安装。
- 涉及文件：`src/screens/SystemRecommendScreen.tsx`、`android/app/src/main/java/com/clearscreen/prototype/backend/ClearScreenRecommendCatalog.kt`、`android/app/src/main/java/com/clearscreen/prototype/backend/ClearScreenUiCleanup.kt`、`README.md`、`docs/wiki/README.md`、本修改记录。

### 2026-11-08 14:17:22 +08:00 — 移除「直接改系统设置」页的高级折叠栏

- 按截图删除「高级」标题和展开/收起控件，让连接端口、本机地址（可读取时）和「连接」按钮直接显示；字段、连接行为及其他首次使用提示保留。
- 同步更新 Wiki 的该页文案说明。
- 验证：未执行。
- 涉及文件：`src/screens/AdbAccessScreen.tsx`、`docs/wiki/README.md`、本修改记录。

### 2026-11-08 14:16:21 +08:00 — 移除关闭应用通知入口的副标题

- 按截图删除「本机应用数，可逐个关闭通知」说明，同时移除只用于该说明的数量计算和样式；单行标题、箭头及跳转行为保留。
- 验证：`git diff --check` 通过；未执行应用界面或真机验收，未打包或安装。
- 涉及文件：`App.tsx`、`src/screens/SystemCleanupScreen.tsx`、本修改记录。

### 2026-11-08 14:13:31 +08:00 — 删除系统推荐开关页的未知状态标签

- 按截图移除「系统推荐开关」两个列表区块中开关旁的「未知」胶囊标签；未知状态仍按中性灰色开关处理，并保留读屏可访问状态与点击后的原有核验逻辑。
- 更新 README 与 Wiki 对该页状态展示的说明。
- 验证：未执行。
- 涉及文件：`src/screens/SystemRecommendScreen.tsx`、`README.md`、`docs/wiki/README.md`、本修改记录。

### 2026-11-08 14:11:30 +08:00 — 发布并安装 1.0.87 到小米手机

- Android 版本从 1.0.86 / versionCode 92 提升至 1.0.87 / 93，正式 Release APK 已生成到 `android/app/build/outputs/apk/净屏.apk`。
- 发布签名校验通过（SHA-256：`93c410454a8263736ff60bb6e27153b19f74f4c934f444531842f5162ccfa999`）；通过 USB 使用 `adb install -r --no-streaming` 覆盖升级到 Xiaomi 22101317C。手机读回版本为 1.0.87 / 93，首次安装时间仍为 `2026-11-06 18:47:52`，无障碍服务保持启用。
- 验证：`npx tsc --noEmit`、`:app:assembleRelease --offline --console=plain`、APK 包名/版本/签名核验、覆盖安装及安装后状态读取均通过；未进行界面或功能验收。
- 涉及文件：`android/app/build.gradle`、`app.json`、本修改记录；APK：`android/app/build/outputs/apk/净屏.apk`。

### 2026-11-08 14:10:59 +08:00 — 将「直接改系统设置」入口移入服务与权限
- 将 Android「直接改系统设置」入口移入「服务与权限」权限卡片末尾，仅显示标题和箭头；从「系统广告与营销通知」页移除该入口，其余四项保留。
- 更新 README 与 Wiki 中的页面层级和入口路径说明。
- 验证：`npx tsc --noEmit`、`git diff --check` 均通过；未执行应用界面或真机验收，未打包或安装。
- 涉及文件：`App.tsx`、`src/screens/ServicePermissionsScreen.tsx`、`src/screens/SystemCleanupScreen.tsx`、`README.md`、`docs/wiki/README.md`、本修改记录。

### 2026-11-08 14:08:55 +08:00 — 修正开发者选项入口在小米设备上的错误落点

- 用户反馈配对引导称将跳转到开发者选项，实际却进入净屏应用信息页。MIUI/HyperOS 上改为打开系统设置首页，并提示「更多设置 → 开发者选项 → 无线调试」；其他设备仍尝试直达开发者选项，无法直达时打开系统设置并提示搜索路径。
- 将相关按钮文案改为「打开系统设置」，并同步调整配对说明与 Wiki，避免承诺直接跳转。
- 验证：`npx tsc --noEmit`、`:app:compileDebugKotlin --offline --console=plain`、`git diff --check` 均通过；未进行修改后的真机界面验收，未打包或安装。
- 涉及文件：`android/app/src/main/java/com/clearscreen/prototype/backend/ClearScreenSettingsNavigator.kt`、`src/native/ClearScreenNative.ts`、`src/screens/AdbAccessScreen.tsx`、`src/screens/QuickComponentsScreen.tsx`、`src/screens/MarketingChannelsScreen.tsx`、`docs/wiki/README.md`、本修改记录。

### 2026-11-08 14:04:47 +08:00 — 删除系统广告入口上的连接状态标签

- 按截图删除「直接改系统设置」入口右侧的「未连接」状态标签，同时移除仅用于显示该标签的连接状态读取；连接状态仍在连接页内显示，入口跳转不变。
- 同步更新界面文案约束与项目功能说明。
- 验证：未执行。
- 涉及文件：`src/screens/SystemCleanupScreen.tsx`、`docs/wiki/README.md`、`README.md`、本修改记录。

### 2026-11-08 14:04:23 +08:00 — 删除快应用子页红框中的两条常驻说明

- 按截图移除配对后的等待状态句「等你在系统里完成配对，连上后会自动开始检查。」及未检查时的占位说明「未检查 · 查完会列出本机有哪些组件」，未检查时不留空占位。顶部未标记说明、自动配对、连上后自动检查、检查按钮、实际状态/错误、扫描结果和组件开关保留。
- 同步更正文案规范中的快应用子页例外。
- 验证：`npx tsc --noEmit` 通过；涉及源码和 Wiki 的 `git diff --check` 通过；未运行应用或真机 UI 验收，未打包或安装。
- 涉及文件：`src/screens/QuickComponentsScreen.tsx`、`docs/wiki/README.md`、本修改记录。

### 2026-11-08 14:02:32 +08:00 — 移除关闭应用通知页的常驻未连接横幅

- 未连接时不再常驻显示「还没连上这台手机，暂时读不到通知开关」及「去连接」；用户尝试切换通知开关时仍弹出连接提示，修改失败仍弹出错误提示，读取失败的重试提示保留。
- 验证：`npx tsc --noEmit` 与 `git diff --check -- src/screens/AppNotificationsScreen.tsx` 通过；未运行应用或真机 UI 验收，未打包或安装。
- 涉及文件：`src/screens/AppNotificationsScreen.tsx`、本修改记录。

### 2026-11-08 13:59:17 +08:00 — 删除直接改系统设置页红框中的状态说明

- 删除「连接状态」和「自动配对」两个分区标题；未连接时不再显示「还没连上。按下面的顺序点一遍就能连上。」；空闲时隐藏「未开始自动配对」。连接结果、配对过程状态、按钮及操作逻辑保留。
- 验证：`npx tsc --noEmit` 与 `git diff --check -- src/screens/AdbAccessScreen.tsx` 通过；未运行应用或真机 UI 验收，未打包或安装。
- 涉及文件：`src/screens/AdbAccessScreen.tsx`、本修改记录。

### 2026-11-08 13:59:15 +08:00 — 对齐系统推荐开关的滑块位置

- 开关关闭时滑块靠左，开启时靠右；未知状态也不再居中，并在系统推荐开关页的常用项旁显示“未知”，避免被看作已关闭。开启为蓝色，关闭为灰色。
- 验证：`npx tsc --noEmit` 与指定源码文件的 `git diff --check` 通过；未启动应用或做真机 UI 验收，未打包或安装。
- 涉及文件：`src/components/UI.tsx`、`src/screens/SystemRecommendScreen.tsx`、本修改记录。

### 2026-11-08 13:56:33 +08:00 — 删除系统推荐开关页的「常用推荐开关」标题

- 按截图只移除红框中的分区标题和对应样式；标题下的三个设置行、开关及其交互保留。
- 验证：`npx tsc --noEmit` 与 `git diff --check -- src/screens/SystemRecommendScreen.tsx` 通过；未运行应用或真机 UI 验收，未打包或安装。
- 涉及文件：`src/screens/SystemRecommendScreen.tsx`、本修改记录。

### 2026-11-08 13:55:02 +08:00 — 移除系统推荐开关中重复的手动操作小字

- 用户指出负一屏等无法自动操作的多个条目下方重复显示“系统不允许其它应用打开负一屏设置……”一类说明，要求清掉整组同类小字，而不只是红框标注的一行。
- `src/screens/SystemRecommendScreen.tsx` 不再渲染手动条目的 `target.reason`，并删除其专用样式；每项仍显示名称和「需手动」状态，其他开关、跳转与提示逻辑不变。
- 同步更新 `docs/wiki/README.md` 的界面文案约定。
- 验证：未运行自动化测试、类型检查、构建或真机验收。
- 涉及文件：`src/screens/SystemRecommendScreen.tsx`、`docs/wiki/README.md`、本修改记录。

### 2026-11-08 13:45:54 +08:00 — 重建并覆盖安装净屏 1.0.86

- 按用户要求基于已推送的 `main`（`16c349d494540c9dc72b2bc5cfdd2c772a180c44`）重新构建 Release：`:app:assembleRelease --offline --console=plain` 成功。APK 包名 `com.clearscreen.prototype`、版本 `1.0.86` / versionCode `92`、应用名“净屏”；Gradle APK 与 `android/app/build/outputs/apk/净屏.apk` 均为 118,651,403 字节、SHA-256 `BF459C0B59531A059E2C2F5777A556FED0B4B766778A638A251CEE8A6466A1F8`，apksigner v2 验签通过，正式签名证书 SHA-256 `93c410454a8263736ff60bb6e27153b19f74f4c934f444531842f5162ccfa999`。
- 使用 USB `adb install -r --no-streaming` 覆盖安装到 Xiaomi 22101317C，返回 `Success`。安装后读回版本 `1.0.86 / 92`，`lastUpdateTime=2026-11-08 13:45:23`，`firstInstallTime=2026-11-06 18:47:52` 未变；`enabled_accessibility_services` 仍包含净屏无障碍服务。未卸载、未清除应用数据，未启动应用或进行界面/功能验收。
- 涉及文件：正式 Release APK、命名 APK、本修改记录。

### 2026-11-08 13:39:07 +08:00 — 清理公开记录中的临时设备地址与本机截图引用

- 原因：历史项目文档中保留了设备当时的局域网地址，并链接到本机 `tmp/` 临时截图；这些临时设备信息不需要随公开仓库发布。
- 修改：保留功能与真机验证结论，移除具体局域网地址及临时截图链接；包含本机网络信息的界面截图不纳入版本库。其余稳定项目截图仍按原记录保留。
- 涉及文件：`README.md`、`docs/wiki/CHANGELOG.md`。
- 验证：`npx tsc --noEmit` 通过；未执行自动化功能测试、Release 构建或装机。

### 2026-11-08 13:31:52 +08:00 — 同步 Android Expo 配置版本号

- 核对待提交工作区时发现 `android/app/build.gradle` 为 `1.0.86 / versionCode 92`，`app.json` 仍为 `1.0.86 / versionCode 91`。已将 `app.json` 同步到 versionCode 92，避免后续 Expo 原生配置生成回退版本。
- 验证：字段与 Gradle 配置逐项对照；本次只做 Git 汇总，未重新构建、安装或运行测试。
- 涉及文件：`app.json`、本修改记录。

### 2026-11-08 12:55:43 +08:00 — 「系统推荐开关」删掉行下方的「关掉后，…」说明与「直接改写与代点」小标题

- 原因：用户圈出该页顶部的「直接改写与代点」以及前几行下方的「关掉后，系统广告不再按你的兴趣精准匹配」这类句子，要求把同类文字一并清掉。
- 范围界定：只删纯展示的效果描述与内部机制小标题。第一个卡片去掉「直接改写与代点」标题；两个卡片（逐个开关、常用推荐）里每行只保留开关名（与状态标签），不再显示「关掉后，…」的效果句；`GROUP_ROWS` 中随之不再使用的 `effect` 字段一并删除。
- 保留：未适配机型的空态句、空态下「系统广告设置」行的系统路径（可操作指引）、不可自动开关行下方的「系统不允许其它应用…请到负一屏设置里关闭」（可操作指引）、「未知」/「需手动」标签、弹窗与开关逻辑，均未改动。
- 清理：行内说明文字删掉后无空白占位，行结构仍是「标题 + 开关」，分割线由行容器统一绘制，没有残迹；`rowStatus` 样式仍被空态路径行引用因此保留，未产生新死代码。
- 验证：`npx tsc --noEmit` 通过；`:app:assembleRelease --console=plain` BUILD SUCCESSFUL（698 modules）；版本递增到 1.0.86 / 92；安装前确认无并行 `adb install`，`adb -s 2f8898c80822 install -r` 返回 `Success`（未卸载、未 `pm clear`）。
- 真机截图（小米 22101317C）：`screenshots/2026-11-08-系统推荐开关-删除行下说明.png`（首个卡片无小标题、各行只剩名称与开关/标签）、`screenshots/2026-11-08-系统推荐开关-删除行下说明-底部.png`（「常用推荐开关」三行只剩名称与开关）。
- 涉及文件：`src/screens/SystemRecommendScreen.tsx`、`app.json`、`android/app/build.gradle`、本修改记录。

### 2026-11-08 12:43:30 +08:00 — 用当前代码重新构建 release 并覆盖安装（不改功能）

- 目的：按用户要求，把当前工作区代码构建成最新 release 包并覆盖安装到小米 22101317C，只做构建与装机，不改业务代码。
- 版本：`app.json` 与 `android/app/build.gradle` 均为 `1.0.85` / `91`；手机上此前已装同版本（`lastUpdateTime` 2026-11-08 12:14:21）。构建产物版本与已装版本相同，属同版本覆盖安装，未降级，因此可正常 `-r` 安装。
- 并行检查：构建前查看 `java.exe` / `adb.exe` 进程，无 Gradle 构建任务、无 `adb install`；安装前再次确认并行安装进程数为 0。
- 验证：`npx tsc --noEmit` 通过；`:app:assembleRelease --offline --console=plain` BUILD SUCCESSFUL（2m 5s）；`adb -s 2f8898c80822 install -r` 返回 `Success`，未卸载、未 `pm clear`、未跨签名。
- 读回：`versionName=1.0.85`、`versionCode=91`、`lastUpdateTime=2026-11-08 12:42:33`、`firstInstallTime=2026-11-06 18:47:52`（与安装前一致，本地数据保留）。
- 真机截图：`screenshots/2026-11-08-净屏1.0.85-装机后首页.png`（启动后首页正常，无白屏或异常）。
- 涉及文件：`android/app/build.gradle`、`app.json`（本次未改，仅确认）、本修改记录；未修改业务代码。
- 参考：当前工作区包含其它窗口尚未提交的改动，本次构建按其当前状态打包，未回滚任何人的修改。

### 2026-11-08 12:17:40 +08:00 — 「授权通道」改名为「直接改系统设置」，并删除标题下的说明小字

- 原因：用户看不懂「授权通道」——不知道授权的是什么、有什么用；要求改成普通用户一看就懂、能说明白“让净屏直接改这台手机上的设置”的说法，并把标题下面那几行小字删掉。
- 界面名称统一改为「直接改系统设置」：父页 `SystemCleanupScreen` 入口卡片标题、`AdbAccessScreen` 顶部标题、连接成功弹窗标题、`SystemRecommendScreen` 的未连接提示文案与弹窗标题（按钮由「去连接」改为「去打开」）、`ClearScreenNative` 的不支持提示。
- 原生弹窗文案同步：`ClearScreenAdbOperations` 的「授权通道未连接 / 请重新连接授权通道」改为「还没和这台手机连上 / 请重新连一次」，`ClearScreenAdbPairingHelper` 的状态文案改为「已连接」；`SystemRecommendScreen` 用于判定“需要先连接”的关键词随之从「授权」改为「还没和这台手机连上」，避免改名后漏判。
- 删除两处常驻小字：父页入口卡片下的两句说明（连上后能做什么 / 没连上会怎样），以及页面标题下的一段自我说明；卡片改为与同页其它入口一致的单行形态（标题 + `已连接`/`未连接` 标签 + 箭头），并清理仅被这两段文字使用的 `entry*`、`intro` 样式，不留空白占位。
- 页内其余描述文字的弹窗化由另一窗口完成（见上一条 12:08:46 记录），本窗口不重复改动、不回滚其改动。
- 同步 `docs/wiki/README.md` 的页面索引、页面内结构说明与「界面文案约束」两条例外条款；根目录 `README.md` 的功能说明与页面清单同步改名。
- 版本双写递增到 1.0.85 / 91（构建前另一窗口刚装上 1.0.84 / 90）。
- 验证：`npx tsc --noEmit` 通过；`:app:assembleRelease` 成功（`BUILD SUCCESSFUL`，产物 `versionCode 91 / versionName 1.0.85`）；`adb -s 2f8898c80822 install -r` 覆盖安装成功，读回 `versionName=1.0.85`、`versionCode=91`、`firstInstallTime` 仍为 2026-11-06 18:47:52（未卸载、未清数据）。装前确认本机无并行 `adb install` 进程，但期间有其它窗口在同一设备上操作，页面跳转曾被打断，已重做导航后截图。
- 真机截图（小米 22101317C）：父页入口卡片 `docs/wiki/evidence-cleanup-entry.png`（「直接改系统设置」单行 + `未连接` + 箭头，卡片下无小字）；页面本体 `docs/wiki/evidence-direct-settings.png`（标题「直接改系统设置」，标题下直接是「连接状态」，无说明段）。
- 涉及文件：`src/screens/SystemCleanupScreen.tsx`、`src/screens/AdbAccessScreen.tsx`、`src/screens/SystemRecommendScreen.tsx`、`src/native/ClearScreenNative.ts`、`android/app/src/main/java/com/clearscreen/prototype/backend/ClearScreenAdbOperations.kt`、`android/app/src/main/java/com/clearscreen/prototype/backend/ClearScreenAdbPairingHelper.kt`、`app.json`、`android/app/build.gradle`、`README.md`、`docs/wiki/README.md`、本修改记录。

### 2026-11-08 12:08:46 +08:00 — 授权通道页说明文字改为首次使用才弹的一次性弹窗

- 原因：上一版把每个按钮的作用都常驻在页面上，整页堆了小字，用户觉得“描述性文字太多”。
- 页面只保留状态与结果：`未连接`/`已连接`、一句“现在该做什么”、`未开始自动配对` 等运行状态，以及按钮本身；删除按钮上方的常驻说明、关闭服务警告、无配对项说明和底部密钥说明。
- 新增四个一次性弹窗，按功能首次点击才弹出，记录在本机，看过不再重复：`打开开发者选项`（先打开「无线调试」，并说明连上之前别关掉）、`自动查找无线调试`（接回上次连接）、`开始自动配对`（第一次连接的操作步骤与密钥只留本机）、`高级`（展开时说明自动连接没成功才用，并解释那串数字怎么填）。
- 弹窗文案不含端口、adb、配对码、包名、组件名等实现细节，只讲能得到什么、要注意什么。
- 本机持久化新增 `readAdbGuideSeen` / `markAdbGuideSeen`（SharedPreferences 字符串集合 `adbGuideSeen`），避免重启后重复弹窗。
- 与另一窗口的分工：页面标题「直接改系统设置」及其下方小字由另一窗口负责，本次未改动。
- 同步更新 `docs/wiki/README.md` 的「界面文案约束」例外条款，改为“常驻说明迁到一次性弹窗”。
- 验证：`npx tsc --noEmit` 通过；`:app:assembleRelease` 成功；`adb -s 2f8898c80822 install -r` 覆盖安装成功（安装后读回 1.0.83 / 89，`firstInstallTime` 仍为 2026-11-06 18:47:52，未卸载、未清数据；装前确认无并行 `adb install`）。
- 真机验证（小米 22101317C）：折叠态整页 `screenshots/2026-11-08-直接改系统设置-精简页.png`；首次点「高级」弹出说明 `screenshots/2026-11-08-直接改系统设置-首次点高级弹窗.png`；`am force-stop` 重启应用后再点「高级」直接展开、不再弹窗 。其余三个按钮的弹窗走同一套逻辑，未逐个点击（会跳转或触发配对，避免干扰并行窗口）。
- 涉及文件：`src/screens/AdbAccessScreen.tsx`、`src/native/ClearScreenNative.ts`、`android/app/src/main/java/com/clearscreen/prototype/backend/ClearScreenNativeModule.kt`、`android/app/src/main/java/com/clearscreen/prototype/backend/ClearScreenStore.kt`、`docs/wiki/README.md`、本修改记录。
- 参考：当前工作区修改，未提交。

### 2026-11-08 12:04:40 +08:00 — 父页「查看应用」改名为「关闭应用通知」

- 原因：按用户要求，把「系统广告与营销通知」页红框那一行（折叠后右侧显示「180 个」+ 箭头的入口）的名称由「查看应用」改为「关闭应用通知」。截图确认红框指向父页该折叠行，不是记录详情页里的「查看应用」按钮，也不涉及快应用子页。
- 改动：`src/screens/SystemCleanupScreen.tsx` 仅改该行标题文案为「关闭应用通知」。同一行内不重复出现旧称，其余页面（记录详情按钮、营销通知子页、快应用子页、授权通道）均无同名入口，因此无第二处需要对齐。折叠时右侧仍显示应用数量、展开后仍是「收起」+「搜索应用」+ 结果列表，功能、数据与跳转未改。
- 文档：`README.md` 与 `docs/wiki/README.md` 的页面清单与设置页说明中该入口称呼已同步为「关闭应用通知」（本窗口此前完成，本次核对无遗漏）。
- 验证：`npx tsc --noEmit` 通过；`:app:assembleRelease --offline --console=plain` 成功（安装前确认无并行 `adb install`；版本递增到 1.0.83 / 89，`app.json` 同步）；`adb -s 2f8898c80822 install -r` 覆盖安装成功（未卸载、未清数据，`firstInstallTime` 保持 2026-11-06 18:47:52，`lastUpdateTime` 2026-11-08 12:00:28），读回 1.0.83 / 89。
- 真机验证（小米 22101317C）：父页显示「关闭应用通知」+「180 个」，点开后变「收起」并出现「搜索应用」输入框，截图 `screenshots/2026-11-08-系统广告与营销通知-关闭应用通知.png`、`screenshots/2026-11-08-关闭应用通知-展开搜索.png`。
- 涉及文件：`src/screens/SystemCleanupScreen.tsx`、`README.md`、`docs/wiki/README.md`、`android/app/build.gradle`、`app.json`、本修改记录。
- 参考：当前工作区修改，未提交；系统推荐开关、授权通道、快应用子页相关改动由其它窗口负责，本次未触碰。

### 2026-11-08 12:02:00 +08:00 — 「快应用与系统广告组件」子页删除检查结果与行状态小字

- 原因：用户截图红框标出三段状态文字——检查完成后的「已检查：本机 N 项，其中 N 项已被净屏关掉。」，以及每个组件行下方的「开：已停用，不会再自启或推广告」。按图判断是纯展示文字，不是检查链路本身，因此只删文字与配套样式。
- `src/screens/QuickComponentsScreen.tsx`：成功检查后不再写入 done 状态与结果句，改为回到 idle；`StatusKind` 去掉 `done`；行内删掉 `rowHint` 文本与 `rowText` 包裹层，行改为「标题 + 开关」两列，标题 `flex: 1` 保持开关右对齐；删除不再引用的 `rowText` / `rowHint` / `statusDone` 样式。检查按钮、空状态提示、出错与「还没连上」提示、开关逻辑全部保留，检查链路未改。
- 保留状态提示：未连接时的「还没连上这台手机，检查前需要先连一次。」不在红框内，属于可操作提示，保留。
- 验证：`npx tsc --noEmit` 通过；`:app:assembleRelease` 成功；`adb -s 2f8898c80822 install -r` 覆盖安装成功（未卸载、未清数据、未跨签名；装前确认无并行 `adb install`）。
- 真机截图（小米 22101317C）：`screenshots/2026-11-08-快应用子页-删除检查结果与行状态小字.png`，页面已无「已检查…」结果行，无空白和分割线残迹。该截图取自未连接状态，列表未生成，因此两个组件行下的状态小字未在截图中出现；本轮设备通道未连接且被并行窗口频繁占用，未能复现「已检查后的列表」状态，行小字删除以代码与类型检查为准，需在通道连上后补一张列表截图。
- 涉及文件：`src/screens/QuickComponentsScreen.tsx`、本修改记录。

### 2026-11-08 11:56:00 +08:00 — 查「快应用与系统广告组件」检出 0 项的原因，补通用空态

- 排查（小米 22101317C，真机实跑，不只看代码）：本机存在 `com.miui.systemAdSolution`、`com.miui.hybrid` 两个组件，第三个 `com.miui.hybrid.accessory` 确实未安装。按原生代码逐字执行同样的命令（`pm list packages`、`dumpsys package … | grep suspended=`）均能正常返回，`state` 读出「正常运行」，不是 suspended 读不到被误判。装包后在真机点「检查本机组件」，结果列出 2 项（快应用服务框架、系统广告服务），说明当前检测链路在本机正常。
- 找到的真实缺口：`ClearScreenShellOps.state()` 判断「组件是否存在」时用了 `pm list packages -s`，只列系统应用；厂商广告组件有时以用户应用的形式更新或安装，会被判成「不存在」，三项全被判不存在时就表现为「检查完成、列表 0 项、没有报错」。已去掉 `-s`，只按包名是否存在判断（未知包名与其它品牌仍不参与，改动只在既有白名单内放宽存在性判断）。
- 空态：列表为空统一显示「未检测到可处理组件」，居中、带上下留白，不再是空白区域；这条分支对所有品牌通用（其它机型没有可处理组件时走同一文案）。
- 文案遵守 Wiki：页面不出现包名、端口、adb 等实现细节；空态只给一句结论，不堆说明。
- 验证：`npx tsc --noEmit` 通过（期间并发窗口正在改其它页面，短暂报错后等其完成后复验通过）；`:app:assembleRelease` BUILD SUCCESSFUL；`adb -s 2f8898c80822 install -r` 覆盖安装成功（未卸载、未 pm clear；安装前确认无并行 `adb install`）。
- 真机截图：`screenshots/2026-11-08-快应用子页-检查后列出本机2项.png`（检查后状态行「已检查：本机 2 项，其中 0 项已被净屏关掉。」+ 两行组件）。
- 未在真机复现空态：这台机器的 2 项始终存在，无法在不改动系统状态的前提下造出 0 项场景；「未检测到可处理组件」空态只有代码与类型检查，无真机截图。
- 涉及文件：`android/app/src/main/java/com/clearscreen/prototype/backend/ClearScreenShellOps.kt`、`src/screens/QuickComponentsScreen.tsx`、本修改记录。

### 2026-11-08 11:52:28 +08:00 — 「营销通知」改为单行入口 + 独立子页

- 原因：按用户要求，把「系统广告与营销通知」页里的营销通知区块改成和「快应用与系统广告组件」一致的形态：父页只留一张单行卡片（标题 + 右侧箭头），点进独立二级页。
- 父页：`SystemCleanupScreen.tsx` 删除营销通知的标题、说明、扫描按钮与渠道清单，替换为单行卡片入口（与「查看应用」「授权通道」「快应用与系统广告组件」同款），顺序仍为系统推荐开关、查看应用、授权通道、营销通知、快应用与系统广告组件；同时清理不再使用的扫描状态、渠道状态与相关弹窗逻辑。
- 子页：新增 `src/screens/MarketingChannelsScreen.tsx`，顶部标题「营销通知」+ 返回箭头，内容原样搬运（候选渠道风险提示、扫描渠道按钮、未发现候选渠道空态、候选渠道清单与应用名）；扫描渠道入口的未连接提示、自动配对流程、打开通知渠道弹窗与原生调用保持原样。仅将配对弹窗中指向「检查本机组件」的措辞改为「扫描渠道」，避免本页出现不存在的按钮名；渠道应用名缺失时兜底显示「未知应用」，不展示包名。
- 路由：`App.tsx` 新增 `marketingChannels` 页面类型、设置子页判定与渲染分支，父页新增 `onOpenMarketingChannels` 回调；返回键走父页返回栈，回到「系统广告与营销通知」而不是退出应用。
- 文档：`README.md` 与 `docs/wiki/README.md` 同步页面清单、设置页结构、子页说明和文案例外条款。
- 验证：`npx tsc --noEmit` 通过；`:app:assembleRelease` 成功（发现并行窗口正在构建，等待其结束后执行；版本递增到 1.0.82 / 88）；`adb install -r` 覆盖安装成功（未卸载、未清数据，`lastUpdateTime` 2026-11-08 11:50:38）。
- 真机验证（小米 22101317C）：父页单行入口、子页内容与返回均正常，截图 `screenshots/2026-11-08-系统广告与营销通知-营销通知入口.png`、`screenshots/2026-11-08-营销通知子页.png`、`screenshots/2026-11-08-营销通知子页-未连接提示.png`；未连接时点「扫描渠道」弹出连接提示，点返回箭头回到父页。
- 涉及文件：`src/screens/SystemCleanupScreen.tsx`、`src/screens/MarketingChannelsScreen.tsx`（新增）、`App.tsx`、`README.md`、`docs/wiki/README.md`、`android/app/build.gradle`、`app.json`、本修改记录。
- 参考：当前工作区修改，未提交。

### 2026-11-08 11:20:18 +08:00 — 「快应用与系统广告组件」文字层级复核：无重复、无包名，未再改代码

- 目的：用户反馈该子页「分组标题 + 条目名」重复表达同一件事（「快应用」下第一条又叫「快应用服务框架」，「系统广告组件」下第一条又叫「系统广告服务」），且包名 `com.miui.hybrid`、`com.miui.systemAdSolution` 常驻显示。
- 现状核对：上一个窗口整页重写时已完成该部分——`rows()` 不再渲染分类标题，直接按 `category`（快应用在前、系统广告组件在后）平铺条目；行内只显示 `item.title`，不再拼接或显示 `packageName`；单条、多条走同一套行结构，没有单条特判。
- 结论：本窗口复核后未发现需要补改之处，未修改 `QuickComponentsScreen.tsx`，避免与并行窗口冲突。
- 验证：`npx tsc --noEmit` 通过；`:app:assembleRelease` BUILD SUCCESSFUL（11:02 构建并 `adb install -r` 成功，随后被并行窗口 11:09:07 的安装覆盖，页面内容相同）。真机小米 22101317C 截图 `screenshots/2026-11-08-快应用子页-文字层级规整.png`：本机 2 项，列表仅「快应用服务框架」「系统广告服务」两行，无分组标题、无 `com.miui.*` 包名，两行排版一致（组件名 + 状态说明 + 开关）。
- 未执行：本窗口未再单独重装（安装前确认到并行窗口正在安装，按约束等待并复用其安装结果）；未改动其他窗口负责的开关、检查链路和说明文案区域。
- 涉及文件：本修改记录、上述截图；无代码改动。

### 2026-11-08 11:18:56 +08:00 — 「快应用与系统广告组件」右侧「关闭 >」改为开关，并补齐行内状态文案

- 目的：每行右侧原来是「关闭 >」文字动作，用户要「开＝净屏正在停用它、关＝它恢复原样」的开关语义，且一眼能看出该组件的真实状态。
- 现状核对：上一个窗口整页重写时已把动作换成 `Toggle`（`value={item.state === 'blocked'}`、`unknown={item.state === 'unknown'}`、`onChange={() => changeTarget(item)}`），不再有箭头。本窗口只补齐差的部分，未重写、未覆盖其它区域。
- 本次改动（仅右侧动作这一块）：行文字改成两行容器 `rowText`（组件名 + 状态说明），状态说明按开关语义直说结果——已停用「开：已停用，不会再自启或推广告」；正常运行「关：正常运行，会自启、推广告」；读不到「状态读不到，暂时不能改」。同步去掉 `rowTitle` 的自适应宽度（改由 `rowText` 占位），新增样式 `rowText` / `rowHint`，右侧仍是 `toggleBusy` 包装，`busy` 时整行开关不可点。
- 状态映射：原生 `inspectCleanupTargets` 读 `dumpsys package … User N: … suspended=true/false`；`blocked` → 开关开，`active` → 开关关，`unknown` → `Toggle` 的未知态（不画成假开假关），且 `changeTarget` 遇到 `unknown` 直接提示「无法确认状态」不改动。
- 操作路径：点开关仍走 `changeTarget` → 原确认弹窗（「关闭/恢复 XXX？」）→ `changeCleanupTarget` → `ClearScreenShellOps.changeTarget` → `pm suspend` / `pm unsuspend`（写完后回读状态确认，失败回「原状态备份已保留」）；没有退化成进二级页。
- 验证：`npx tsc --noEmit` 通过；`:app:assembleRelease` BUILD SUCCESSFUL；`adb -s 2f8898c80822 install -r` 覆盖安装成功（1.0.81，未 pm clear、未卸载，安装前确认无并行 `adb install`）。
- 真机（小米 22101317C，11:15–11:18，本机 2 项组件）逐项对照 `dumpsys`：
  - 检查后两行均显示「关：正常运行，会自启、推广告」、开关关闭；同时 `com.miui.hybrid` 与 `com.miui.systemAdSolution` 均 `suspended=false`，一致。截图 `screenshots/2026-11-08-快应用子页-开关关-组件正常运行.png`。
  - 点第一行开关：弹「关闭快应用服务框架？」→「关闭」→ 弹「处理结果：已关闭，原状态已保存。」；`suspended=false→true`，该行开关变开、文案变「开：已停用，不会再自启或推广告」，另一行保持关。截图 `screenshots/2026-11-08-快应用子页-开关开-组件已停用.png`。
  - 再点该开关：弹「恢复快应用服务框架？」→「恢复」→ 弹「已恢复原状态。」；`suspended=true→false`，开关回到关，组件恢复原状。截图 `screenshots/2026-11-08-快应用子页-恢复后开关回到关.png`。
  - 未执行：`unknown` 未知态真机复现（该机两项都能读到 `suspended`），仅核对代码路径。
- 已知遗留（不在本窗口范围、未改）：检查结果那行「其中 0 项已被净屏关掉」是检查当时的结果，改完开关后不会自动刷新（需再点一次「检查本机组件」）。
- 涉及文件：`src/screens/QuickComponentsScreen.tsx`、本修改记录、上述三张截图。

### 2026-11-08 11:18:00 +08:00 — 「快应用与系统广告组件」删掉列表下方整段说明，改为一次性弹窗

- 目的：列表下面那段「右侧文字表示每项的状态或可做的动作：『关闭』表示正在运行…停用不会卸载应用。」常驻占位，改成用户第一次真正用到时弹一次。
- 删除：该整段说明连同其样式 `resultHint` 一并移除，列表末尾直接接背景，不留空行、不留分割线空档。
- 弹窗（沿用 `useAppAlert` / `src/components/AppDialog.tsx`，不新造样式）：标题「开关怎么用」，正文「开关打开：净屏正在帮你关掉这个组件的广告。开关关闭：这个组件恢复原样、正常启用。停用不会卸载应用，随时打开开关就能恢复。」，单个按钮「知道了」，`tone: 'info'`。
- 触发与次数：检查成功且真的拿到非空列表时弹一次；若用户还没看过就先点了开关，也先弹这一次说明、不执行改动（`changeTarget` 开头拦截）。标记通过已有 `isQuickComponentsGuideSeen` / `setQuickComponentsGuideSeen`（SharedPreferences 键 `quickComponentsGuideSeen`）持久化，弹窗展示时立即写入，所以之后进页面、再检查都不再弹。补齐上一版留下的两处缺口：列表出现时先 `await` 读取存储结果再判断（消除异步竞态），首次点开关的兜底路径。
- 验证：`npx tsc --noEmit` 通过；`:app:assembleRelease` BUILD SUCCESSFUL；`adb -s 2f8898c80822 install -r` 覆盖安装成功（1.0.81 / 87，`lastUpdateTime=2026-11-08 11:09:07`，未 pm clear、未卸载；安装前确认无并行 `adb install` 与并行 Gradle 构建）。
- 真机（小米 22101317C，11:11–11:15，先经「授权通道 → 自动查找无线调试」连上，本机 2 项组件）：
  - 第一次点「检查本机组件」出列表，同时弹出「开关怎么用」说明弹窗，截图 `screenshots/2026-11-08-快应用子页-首次检查弹出开关说明.png`。
  - 关掉弹窗后再点一次「检查本机组件」：不再弹，列表正常；页面底部已无那段说明、也无多余空档，截图 `screenshots/2026-11-08-快应用子页-再次检查不再弹窗-列表底部无说明.png`。
  - 再点第一项开关：走原来的「关闭 XXX？」确认弹窗，没有被说明弹窗挡住（按「取消」未改动系统状态）。
  - 未复现「首次点开关也弹说明」这一路径：该标记在本次已置为已看，且包不可调试（`package not debuggable`）、按约束不 `pm clear`，无法把标记重置回未看；该路径只有代码实现，未在真机跑出。
- 设备说明：验证期间另一窗口的应用反复抢占前台，多步导航被切成系统页；本次按控件坐标逐回合截图确认落在净屏上。为跑通检查，临时开启过无障碍服务（用于本机自动配对），验证后已恢复 `accessibility_enabled=0`、`enabled_accessibility_services=null`。
- 涉及文件：`src/screens/QuickComponentsScreen.tsx`、本修改记录。

### 2026-11-08 11:07:06 +08:00 — 补「检查本机组件」点击反馈链路的剩余漏洞

- 背景：上一版已给该按钮加了进行中状态、30 秒超时和未连接/Shizuku/无障碍/隐私分类提示；本窗口只补仍可能让用户觉得“点了没反应”的部分，未重写页面。
- `QuickComponentsScreen.tsx`（仅检查链路，未动说明文字、开关行和底部说明）：
  - 未连接判定不再静默降级：原来 `getCleanupAccessStatus()` 失败会被 `.catch(() => access)` 换成缓存值继续走，连接状态读不出来时可能误判为已连接/未连接。现在读失败直接显示「检查没完成：<可读原因>」并刷新连接状态，不再吞异常。
  - `refreshAccess` 也套上 8 秒超时：原生状态读取若卡住，不再让页面永远停在「检查中」且按钮不恢复。
  - 检查失败后主动刷新连接状态，确保页内「现在连上」按钮与实际断连一致，不会出现“提示去连但页面上没有按钮”。
  - `explainError` 识别 `授权通道未连接 / 请先启动 / 请先授予` 等原生错误，统一成一句「还没连上这台手机，所以查不了。点「现在连上」，用无线调试配对一次再试。」；超时类提示排在 Shizuku 判断之前，避免把断连超时说成“没连上”。
- 真实原因：点击本身有绑定、异常也非被空 catch 吞掉；旧版在未连授权通道时把用户送去别的页面、本页不留任何进行中/结果占位，返回后仍是原样；且前置校验用 `skipRunning` 会把已开启服务误判成未开。上一版已改掉死路与误判，本轮补上状态读取失败无反馈、断连后按钮不出现两个漏洞。
- 验证：`npx tsc --noEmit` 通过。设备当前安装 1.0.81 / 87（并行窗口 11:03 覆盖安装，构建于 11:01，源文件本次改动 10:59 已包含），未再重复装包，未 pm clear、未卸载。
- 真机（小米 22101317C，11:04–11:06）：
  - 未连接时点「检查本机组件」：按钮进入检查流程后本页立刻出现「还没连上这台手机，所以没法检查。点「现在连上」…」，同时弹出「先让净屏连上这台手机」弹窗（取消 / 现在连上），不再毫无变化。截图 `screenshots/2026-11-08-检查本机组件-未连接有反馈.png`、`screenshots/2026-11-08-检查本机组件-连接引导弹窗.png`。
  - 在弹窗里点「现在连上」：本页直接发起本机自动配对，出现「去系统里点两下」引导和「等你在系统里完成配对，连上后会自动开始检查。」状态行。截图 `screenshots/2026-11-08-检查本机组件-页内发起配对.png`。
  - 未执行：从配对成功到返回自动出结果的完整链路（需在系统无线调试里点「使用配对码配对设备」，本轮设备被并行窗口反复切前台，未完成）；未连接路径检查在一秒内完成，进行中状态瞬时，未截到加载态。
- 涉及文件：`src/screens/QuickComponentsScreen.tsx`、本修改记录。

### 2026-11-08 10:53:22 +08:00 — Wiki 新增长期界面规范：解释信息一律放弹窗

- 按用户要求，在 `docs/wiki/README.md` 的「界面文案约束」小节新增「解释信息一律放弹窗（长期规范）」：需要解释/提示/告知的内容统一用 `src/components/AppDialog.tsx` 弹窗呈现，在用户真正用到时弹出；功能区块下不常驻大段说明；页面只留必要的一行结果说明；弹窗文案不写实现细节和技术术语，只说能得到什么、要注意什么；同一提示不每次进页都弹，只在首次进入、首次操作、出错或条件不满足时出现。同时注明原有各页“例外”只描述现状，新增或改动界面按该规范收敛。
- 验证：仅文档改动，未改代码，未执行构建或真机验证。
- 涉及文件：`docs/wiki/README.md`、本修改记录。

### 2026-11-08 10:47:00 +08:00 — 「检查本机组件」改为手机内自己配对，删除电脑 adb 说法

- 原因：用户在该页点「检查本机组件」时被「需要先连接授权通道 / 无线调试配对或电脑 adb 连上后就能使用，也可以连接 Shizuku」挡住，误以为必须配合电脑。核查结论：配对、连接、停用组件全部由应用内的本机 ADB 客户端完成，从来没有必须用电脑的步骤；「电脑 adb」只是「授权通道 → 高级」里的一条可选兜底命令，文案把它写成了前置条件，且未连接时只把人送到别的页面，形成死路。
- 入口弹窗（`QuickComponentsScreen.tsx`，实际承载「检查本机组件」；`SystemCleanupScreen.tsx` 内旧内联卡片已由并行窗口拆出，同步了同样逻辑）：标题改「先让净屏连上这台手机」，正文「不用电脑，也不用数据线：打开「无线调试」，用配对码连一次，之后就能在这里检查、停用这些组件。」，按钮改为「现在连上」；点后直接调用本机自动配对，不再只做页面跳转。连上返回后自动开始检查（`pendingInspect`）。
- 分档如实处理：Android 10 及以下 `supported=false` 时明确「这一档需要你自己关」，给「去系统设置」；自动跳过服务（无障碍）未开启时先提示「还差一步：开启自动跳过服务」并给「去开启」，不再静默等待；配对已启动后给「去系统里点两下」的引导（打开开发者选项 → 无线调试 → 使用配对码配对设备，配对码由净屏自己读）。
- 授权通道页（`AdbAccessScreen.tsx`）：删除「电脑 adb 兜底」标题、说明和 `adb tcpip 5555` / `adb connect` 两条命令及对应样式；开头写明「全程在这台手机上完成」；自动配对卡片写清系统里要点哪两下、配对码自动读取；补两条如实说明：无障碍未开时读不到配对码、部分厂商系统没有「使用配对码配对设备」时净屏连不上、只能到系统设置手动关。Android 10 及以下隐藏自动配对与端口入口，改给「去系统设置手动关」。
- 前置校验从 `skipRunning` 改为 `skip`（系统设置里服务已启用）：真机上服务已绑定、系统开关已开时 `skipRunning` 仍会短暂为 false，会把可用状态误判成「服务没开」，这是上一轮把用户挡在门外的直接原因之一。
- 文档：`docs/wiki/README.md` 授权链路段落同步为手机内自连，并登记两档确实做不到的情况；删掉「电脑侧 adb tcpip 作为兜底，命令写在应用内」的旧说明。
- 版本：`app.json` / `android/app/build.gradle` 升至 1.0.81 / 87（并行窗口刚装 1.0.80 / 86）。
- 验证：`npx tsc --noEmit` 通过；`:app:assembleRelease` BUILD SUCCESSFUL；`adb -s 2f8898c80822 install -r` 覆盖安装成功（1.0.81，`lastUpdateTime=2026-11-08 10:44:33`，未卸载、未清数据、未跨签名）。安装前确认设备上次更新为 10:36:12（并行窗口的 1.0.80），无并行安装。
- 真机截图（小米 22101317C）：`screenshots/2026-11-08-检查本机组件-手机内自己连.png`（新入口弹窗，已无「电脑 adb」）；`screenshots/2026-11-08-检查本机组件-自动跳过未开.png`（无障碍未开时的如实提示）。
- 未执行验证：从「现在连上」到配对成功、返回后自动检查的完整链路本轮未跑通。设备同时被并行窗口占用（前台多次切到 SpotCue、系统设置；并行窗口在 10:43 记录中说明其测试后把 `accessibility_enabled` 恢复为 0），无法保证点击落在净屏。自动配对的原生实现沿用此前已验证的 `startAdbAutoPair`，本次只改了入口与前置校验，需设备空闲时补一次全链路复验。
- 涉及文件：`src/screens/QuickComponentsScreen.tsx`、`src/screens/SystemCleanupScreen.tsx`、`src/screens/AdbAccessScreen.tsx`、`docs/wiki/README.md`、`app.json`、`android/app/build.gradle`、本修改记录。

### 2026-11-08 10:45:00 +08:00 — 「系统推荐开关」八项改为真实开关形态，补小米对照项与品牌兜底

- 原因：上一版把搜索提示词、应用推荐、搜索历史、宫格位推送、资讯推荐、服务推荐、内容精选、热点资讯八项做成「需手动」标签或纯跳转，操作仍落在用户身上；且部分项下方带技术性小字。按要求八项与上方「个性化广告推荐 / 个性化服务 / 智能助理」统一为开关行，保留「关掉后……」结果说明，不再出现技术小字。
- 代点级联：`ClearScreenUiCleanup.kt` 新增 `Stage(entry, page)` 与 `Session.stageIndex/stageClickedAt/stageAttempts`。`observe()` 先按页面链点击入口行（`ACTION_CLICK`，每级最多 2 次、1.6 秒防连点、12 秒找不到入口即结束），全部入口点完后再按原有「页头 30% + 行内唯一开关 + 两次采样」逻辑点真实开关；每级成功后不再重复点击。这样「首页展示模块」「主页设置」这类二级页不再需要用户自己找。
- 小米目录（`ClearScreenRecommendCatalog.kt`）：`xiaomi.search.hint / history` 一级入口「首页展示模块」、`xiaomi.search.app` 两级「首页展示模块 → 应用推荐」（开关文案别名「展示应用推荐」）、`xiaomi.browser.grid` 一级「主页设置」，四项 `support` 由 `manual` 改为默认 `assisted`，删除旧 `reason` 技术小字，走已有无障碍代点流程。
- 负一屏 4 项（资讯推荐、服务推荐、内容精选、热点资讯）仍为 `unreachable`：本机负一屏设置页只开放搜索框、推荐卡片、热门活动、智能置顶，且其它应用无法打开该页，所以不做假开关；行内只留一句可执行指引「系统不允许其它应用打开负一屏设置，请桌面右滑进入负一屏，在右上角设置里关闭」，右侧保留「需手动」标识，与开关行形态明确区分。
- 小米对照补齐：新增已核实的负一屏真实项「推荐卡片」「搜索框推荐词」「热门活动」「智能置顶」（同为 `unreachable`，带同一句指引），小米侧由 11 项增至 15 项，与 vivo 对照清单（11 项）同量级；未删除任何既有项。
- 品牌登记：`when` 增加 SAMSUNG → 自定义服务（含英文别名），其余未登记品牌落入通用兜底 `generic`（个性化广告，`manual` + 指引），不再返回空列表；小米、华为/荣耀、OPPO/一加/realme、vivo/iQOO、三星、通用均在同一张表内可扩展登记，页面不写死小米。
- 界面（`SystemRecommendScreen.tsx`、`UI.tsx`）：开关值由「state === 'off'」纠正为「state === 'on'」（开关开=推荐开，关=推荐关）；`Toggle` 新增 `unknown` 形态（灰色轨道 + 居中滑块 + 无障碍值「未知」），未确认过的代点项并排显示「未知」小标识，不把未知画成开或关；技术小字渲染行只保留给 `unreachable` 项作为去关路径说明。确认弹窗路径补充「全局搜索设置 → 首页展示模块（→ 应用推荐）」「浏览器设置 → 主页设置」。
- 验证：`:app:assembleRelease` BUILD SUCCESSFUL；`adb -s 2f8898c80822 install -r` 覆盖安装成功（未卸载、未清数据、未跨签名；安装前确认无并行 `adb install`）。
- 真机截图（小米 22101317C，10:39/10:42）：`screenshots/2026-11-08-系统推荐开关-八项全开关-顶部.png`、`screenshots/2026-11-08-系统推荐开关-八项全开关-底部.png`。可见八项均为开关行 + 「未知」标识，需手动项带一句指引，底部为「常用推荐开关」。
- 未执行验证：无障碍代点级联（含两级「应用推荐」）本轮未在真机跑通。测试前系统里无障碍服务处于关闭状态，临时开启后设备被并行窗口的应用（SpotCue）抢占前台，点击与截图多次落到别的应用；为避免干扰已把 `accessibility_enabled` 恢复为 0。该流程本轮只做了构建与代码校验，需在设备空闲时复验。
- 涉及文件：`android/app/src/main/java/com/clearscreen/prototype/backend/ClearScreenUiCleanup.kt`、`ClearScreenRecommendCatalog.kt`、`src/screens/SystemRecommendScreen.tsx`、`src/components/UI.tsx`、本修改记录。

### 2026-11-08 10:43:00 +08:00 — 「授权通道」入口去重，只保留「系统广告与营销通知」页的完整卡片

- 原因：「授权通道」在两处重复出现。一是「系统推荐开关」页最底部一行纯入口（只有标题 + 箭头，没有状态和说明），二是「系统广告与营销通知」页一张完整卡片（「授权通道」+「未连接/已连接」状态标签 + 「连上后能做什么」「没连上会怎样」两句说明）。按要求只留一处。
- 保留「系统广告与营销通知」页的完整卡片：用户能先看到当前是否已连接，再决定要不要点进去；而且同页的「营销通知」「快应用与系统广告组件」本来就依赖授权通道（后者现已拆成同页下的独立二级页，入口仍在同页），入口和使用场景在同一页，不用来回跳。
- 删除「系统推荐开关」页底部那张 Card：连同其中 `Pressable`、标题文本与 `Ionicons` 箭头一起删掉，不留空白（该卡片下方即页面结尾，`paddingBottom` 沿用页面级样式）。该行没有专用样式，复用同一文件的 `s.row`/`s.rowText`/`s.card`，因此无样式可清；删除后 `Ionicons` 在本文件已无引用，一并移除导入。`onOpenAdbAccess` 仍被「需要先连接授权通道」弹窗的「去连接」按钮使用，保留未删。
- 依赖检查：系统推荐开关页所有需要授权的动作（上方「直接改写与代点」开关、「常用推荐开关」三行开关）在未连接时走 `changeDirectTarget` 的错误分支，弹「需要先连接授权通道」并给出「去连接」直达授权通道页；无障碍代点未开启时另有「去开启」弹窗。不依赖授权通道的路径（需手动项、跳转系统设置页）照旧。不存在静默失败路径，因此未额外加提示行。
- 路径判断：留一处后两页都走得通。设置 → 系统广告与营销通知（有卡片）→ 授权通道；系统推荐开关页是前一页的下一级，写开关失败时弹窗里的「去连接」会直接进授权通道页，回到推荐页按返回即可。没有把入口改为留在推荐页：推荐页比父页更深一层，从设置进入要多一次跳转；而父页除推荐开关外的其他区块都要用授权通道，把入口留在父页对多数操作路径更近，信息也更完整。
- 验证：`npx tsc --noEmit` 通过；`:app:assembleRelease` BUILD SUCCESSFUL；`adb -s 2f8898c80822 install -r` 覆盖安装成功（1.0.79 / 85，`lastUpdateTime` 2026-11-08 10:21:59，`firstInstallTime` 仍为 2026-11-06 18:47:52，未卸载、未清数据）。本次沿用 versionCode 85 不递增：并行窗口同一时间也在向这台设备安装，递增会让它的旧构建变成降级安装而失败。
- 真机验证（小米 22101317C）：系统推荐开关页滑到最底，最后一张卡片是「常用推荐开关」，其后没有「授权通道」行、也没有空白，截图 `screenshots/2026-11-08-授权通道入口-仅保留一处-系统推荐开关页底部.png`；上一级「系统广告与营销通知」页的授权通道卡片保持原样（状态「未连接」+ 两句说明），截图 `screenshots/2026-11-08-授权通道入口-仅保留一处-系统广告与营销通知页.png`。10:36:12 该设备又被并行窗口覆盖安装为它自己的构建，上面两张交付截图取自当前安装版本（该构建同样包含本次删除），未重复抢装。
- 涉及文件：`src/screens/SystemRecommendScreen.tsx`、本修改记录。
- 参考：当前工作区修改，未提交。

### 2026-11-08 10:41:00 +08:00 — 「快应用与系统广告组件」从系统广告页拆成独立子页

- 原因：原区块（说明、检查本机组件按钮、快应用/系统广告组件清单）直接铺在「系统广告与营销通知」页，页面过长；改为一行子栏入口 + 独立二级页，页面更短，功能不变。
- `src/screens/SystemCleanupScreen.tsx`：删除该区块的说明文字、检查按钮、组件清单，以及不再使用的 `targets` / `inspect` / `changeTarget` / `componentGroup` 逻辑和对应样式；原位置改为单行卡片入口「快应用与系统广告组件」+ 右侧箭头，形态与「查看应用」「授权通道」一致。
- 新增 `src/screens/QuickComponentsScreen.tsx`：复用现有二级页 `TopBar`，标题「快应用与系统广告组件」；完整承接原说明、检查本机组件按钮、快应用/系统广告组件分组清单，以及停用/恢复确认、无线调试配对、连上后自动检查的原有逻辑。
- `App.tsx`：新增 `quickComponents` 路由与页面渲染；把 `quickComponents` 加入硬件返回键的二级页判断，按返回键回到「系统广告与营销通知」而不是退出应用。
- 父页剩余顺序按截图确认：系统推荐开关 → 查看应用 → 授权通道 → 营销通知（扫描渠道）→ 快应用与系统广告组件入口，卡片间距保持原 12，未调整其他窗口正在改的「系统推荐开关」列表与「授权通道」入口。
- 版本：`versionName` 1.0.80 / `versionCode` 86，`app.json` 同步。
- 验证：`npx tsc --noEmit` 通过；`:app:assembleRelease` 成功；`adb -s 2f8898c80822 install -r` 覆盖安装成功（未卸载、未清数据、未跨签名）。
- 真机截图（小米 22101317C）：父页 `screenshots/quick-components-parent.png`（10:38）、子页 `screenshots/quick-components-sub.png`（10:38）。
- 子页「检查本机组件」点击后的授权/配对前置弹窗已在真机触发；完整检查与停用需要连上无线调试配对，本次未执行。
- 设备争用说明：另一窗口在同一台设备上同时安装与导航（期间前台出现过 SpotCue、授权通道、系统推荐开关等页面），固定路径截图被抢多次，最终在 10:38 连续操作抢到两张有效截图；设备安装时间为 10:36:12 的 1.0.80 构建。
- 涉及文件：`src/screens/SystemCleanupScreen.tsx`、`src/screens/QuickComponentsScreen.tsx`（新增）、`App.tsx`、`android/app/build.gradle`、`app.json`、`docs/wiki/README.md`、本修改记录。
- 参考：当前工作区修改，未提交。

### 2026-11-08 10:03:34 +08:00 — 系统推荐开关页三条常用项改为整行跳转到品牌具体设置页

- 原因：「广告与隐私 / 资讯与推荐 / 应用商店推广」原来只显示「设置 → 隐私与安全 → …」这类路径文字，用户要自己照着找。现在点击行文字直接跳转，路径文字不再作为主要内容展示，只在开关确认弹窗里作辅助说明。
- 新增集中路由表 `android/app/src/main/java/com/clearscreen/prototype/backend/ClearScreenRecommendRoutes.kt`：按路线 `settings.adPrivacy` / `settings.news` / `store.home` 注册候选页，每个候选带 `vendor` / `system` 级别与下一步提示。覆盖小米（HyperOS/MIUI）、华为、荣耀、OPPO/一加（realme 归 OPPO）、vivo（iQOO 归 vivo）、三星，其余品牌走标准隐私设置兜底；应用商店包名映射 vivo `com.bbk.appstore`、OPPO/一加 `com.heytap.market`、荣耀 `com.hihonor.appmarket`、华为 `com.huawei.appmarket`、三星 `com.sec.android.app.samsungapps`，未安装则继续降级。
- `ClearScreenSettingsNavigator.openRecommendRoute` 按列表逐级尝试，每级先 `canOpen`（`resolveActivityInfo` + enabled/exported/permission 预检）再启动，分别捕获 `ActivityNotFoundException` 与厂商拒收的 `RuntimeException`；全部失败落到净屏应用信息页，并返回针对该路线的 `hint`（商店：从桌面打开商店进「我的 → 设置」；资讯：在系统设置搜索「推荐」或进负一屏设置）。
- `ClearScreenNativeModule` 新增 `openRecommendRoute(route)`，`src/native/ClearScreenNative.ts` 新增对应桥接与 `RecommendRouteResult` 类型；`src/screens/SystemRecommendScreen.tsx` 只在结果为 `appDetails` 时弹「没有直达设置页」告知下一步。
- 防误触：行内左侧文字区为跳转热区，右侧开关独立处理点击，避免包一层整行 `Pressable` 后点开关被抢事件而误跳页面；没有可自动操作目标的项显示「需手动」并整块可点跳转。
- 未改动开关项本身的增删与列表结构，避免与并行窗口的「开关列表补齐」冲突。
- 验证：`npx tsc --noEmit` 通过；`android\app\build\outputs\apk\release\app-release.apk` 由 `:app:assembleRelease` 产出并 `adb -s 2f8898c80822 install -r` 覆盖安装成功（1.0.79，`lastUpdateTime` 2026-11-08 09:55:14，未卸载、未清数据）。
- 小米 22101317C（HyperOS 2）逐行点击实测，三行均在 `vendor` 级直达：
  - 广告与隐私 → `com.android.settings/.ad.AdServiceSettings`（「广告服务」页，含「个性化广告推荐」开关），截图 `screenshots/2026-11-08-系统推荐开关-跳转-广告与隐私.png`。
  - 资讯与推荐 → `com.miui.home/.settings.MiuiHomeSettingActivity`（桌面设置，含「智能助理」负一屏总开关），截图 `screenshots/2026-11-08-系统推荐开关-跳转-资讯与推荐.png`。
  - 应用商店推广 → `com.xiaomi.market/.business_ui.settings.recommend.RecommendSettingsFragmentActivity`（商店「隐私」页，含「个性化服务」开关），截图 `screenshots/2026-11-08-系统推荐开关-跳转-应用商店推广.png`。
  - 右侧开关点击只弹确认弹窗，`topResumedActivity` 保持 `com.clearscreen.prototype/.MainActivity`，未误跳页面，截图 `screenshots/2026-11-08-系统推荐开关-开关不跳转.png`。
  - 本次三行都直接命中厂商页，`system` 与 `appDetails` 降级分支未在真机上触发，仅完成代码实现，未执行真机验证。
- 设备争用说明：测试期间另一窗口在同一台设备上频繁安装与导航（10:01 又生成过新的 APK），首次点击偶尔被抢后需重试一次；上表结果为在本任务 09:55:14 安装的构建上逐次截图确认所得。
- 涉及文件：`android/app/src/main/java/com/clearscreen/prototype/backend/ClearScreenRecommendRoutes.kt`（新增）、`ClearScreenSettingsNavigator.kt`、`ClearScreenNativeModule.kt`、`src/native/ClearScreenNative.ts`、`src/screens/SystemRecommendScreen.tsx`、本修改记录。
- 参考：当前工作区修改，未提交。

### 2026-11-08 09:50:26 +08:00 — 系统推荐开关页删掉技术小字，改用「需手动」标识

- 原因：列表每条开关下方原本显示「系统不开放直达搜索子设置，打开后点「首页展示模块」里的这个开关」「系统不开放直达首页子页面，打开后点「主页设置」里的这个开关」「负一屏设置受系统保护，其它应用无法打开；请在桌面右滑进入负一屏关闭」这类实现细节与操作教学，用户看着累，也没说明打开后能得到什么。
- `src/screens/SystemRecommendScreen.tsx` 每条开关的副行只保留一句「关掉后……」的用户结果说明；不再拼接「已关闭 / 仍开着」状态前缀，也不再显示 `target.reason` 技术说明。
- 无法自动操作的项不再输出原因文字，只在右侧显示紧凑版「需手动」标签；标签复用现有 `Badge` 组件，行结构与点击区域保持不变，避免行高忽大忽小。
- 同时删除「常用推荐开关」分组在缺少目标项时追加的「系统把这一项锁在自己的页面里，第三方应用无法改写；请自己打开：…」兜底说明。
- 未改动开关项本身的增删，未实现任何跳转逻辑；这两部分分别由其他会话负责。
- 同步在 `docs/wiki/README.md` 的「界面文案约束」登记本页例外：只保留结果说明，技术说明与操作教学不显示，无法自动操作的项用「需手动」标识。
- 验证：`npx tsc --noEmit` 通过；`:app:assembleRelease` 成功；`adb -s 2f8898c80822 install -r` 覆盖安装成功（1.0.79 / 85，`lastUpdateTime` 2026-11-08 09:47:37，`firstInstallTime` 仍为 2026-11-06 18:47:52，未清数据、未卸载）。
- 真机验证：小米 22101317C 页首及底部均已检查；截图中每条只剩结果说明，技术小字已消失，无法自动操作的项均显示「需手动」。设备同时被其他会话占用，底部截图里开关右侧的箭头属于另一会话正在做的跳转改动，不是本次文案改动。
- 涉及文件：`src/screens/SystemRecommendScreen.tsx`、`docs/wiki/README.md`、本修改记录。
- 参考：当前工作区修改，未提交。

### 2026-11-08 09:48:30 +08:00 — 快应用与系统广告组件区块补上用途说明和结果含义

- 原因：该区块只有「检查设备」按钮和两行「尚未检查设备」，用户看不出检查能得到什么、有什么用。
- 标题下加一段说明：快应用和系统广告组件会在后台自启、推广告、占内存；点检查只读取本机信息，几秒后列出装了哪些、现在开着还是已关，开着的一键停用、随时能恢复。按钮文案由「检查设备」改为「检查本机组件」，与说明里的动作一致。
- 未检查状态改为「未检查 · 查完会显示本机有哪些快应用组件 / 广告组件」；检查后没有命中时改为「本机没有检出受支持的…」，不再显示「没有匹配的受支持项目」。
- 有检查结果时在区块底部加一行动作含义：右侧「关闭」表示正在运行、点一下停用，「恢复」表示改回净屏保存的原状态，「已关闭」表示已停用；停用不会卸载应用。
- 文案只描述代码实际能做到的事：按机型预设清单检查已安装的目标组件及其开关状态，不承诺识别哪些组件正在推广告。
- 改动文件：`src/screens/SystemCleanupScreen.tsx`（并行窗口已把「更多清理」页合并进「系统广告与营销通知」页），`docs/wiki/README.md`（登记该区块的文案例外）。
- 验证：`npx tsc --noEmit` 通过；`:app:assembleRelease` BUILD SUCCESSFUL；`adb -s 2f8898c80822 install -r` 覆盖安装成功（1.0.79 / 85，未卸载、未清数据）。真机未检查状态截图 `screenshots/2026-11-08-系统广告与营销通知-快应用区块-未检查.png`。检查完成态截图未取得：该机授权通道为「未连接」，点检查只会弹出连接提示，需连上授权通道后才能产生结果；结果行文案随结果条件显示。设备同时有并行窗口在用，`lastUpdateTime` 可能被并行安装覆盖。

### 2026-11-08 09:46:41 +08:00 — 授权通道入口卡片补上作用说明和状态标签

- 原因：入口卡片当时只有「授权通道 / 未连接」两行，普通用户看不出这个功能有什么用、不连会怎样。
- 卡片改为「标题 + 已连接/未连接标签」，标题右侧保留箭头；标签旁不再让「未连接」独占一行。卡片内加两句用户语言说明：连上后净屏不用数据线就能直接改这台手机的系统广告开关、停用快应用和广告组件、清理广告通知渠道；没连上时下面的扫描和清理都用不了，连上后手机重启需要再连一次。
- 文案不含端口、adb、配对码等术语；同时在 `docs/wiki/README.md` 的「界面文案约束」里登记这张入口卡片的例外。
- 改动文件：`src/screens/MoreCleanupScreen.tsx`（新增标签与两句说明、`Badge`/样式），`docs/wiki/README.md`。并行窗口随后把「更多清理」页合并进「系统广告与营销通知」页，卡片与文案被原样保留到 `src/screens/SystemCleanupScreen.tsx`，该页顶部的例外登记也同步改成了合并后的路径。
- 验证：`npx tsc --noEmit` 通过；release 构建覆盖安装到小米 22101317C（未卸载、未清数据，`firstInstallTime` 保持 2026-11-06 18:47:52）；真机截图 `screenshots/2026-11-08-系统广告与营销通知-合并后顶部.png`。

### 2026-11-08 09:45:01 +08:00 — 系统推荐开关页把「手动去设置」三项改成开关行

- 原因：该组「广告与隐私 / 资讯与推荐 / 应用商店推广」原本是三条跳转行，其中「资讯与推荐」跳到系统「桌面」页，页内没有对应开关，用户无法关闭。
- 三项改为与本页上方推荐行相同的开关行：按类别解析出一个可直接操作的目标，复用现有的直接改写/无障碍代点流程，右侧显示同一个 `Toggle`，行下沿用该目标的结果说明；不再跳转后把操作留给用户。
- 小米 22101317C（HyperOS 2）对应目标：广告与隐私 → 个性化广告推荐；资讯与推荐 → 负一屏「智能助理」总开关（关掉后桌面右滑不再进入负一屏资讯页）；应用商店推广 → 应用商店「个性化服务」。
- 分组标题由「手动去设置」改为「常用推荐开关」，与实际行为一致。
- 某一类别在本机没有可操作目标（只剩 manual/unreachable）时，该行不显示开关，改为「需手动」标记并写明第三方应用无法改写、需要用户自己去系统哪一级关闭，避免假开关。
- 代点确认弹窗为负一屏总开关补上「桌面设置 → 智能助理」路径，避免提示错误引导到全局搜索。
- 移除该组原有的 `openRecommendRoute` 跳转代码与三个路由常量；页面其余分组、文案和顶部提示未改动。
- 验证：`npx tsc --noEmit` 通过；`:app:assembleRelease` BUILD SUCCESSFUL；`adb -s 2f8898c80822 install -r` 覆盖安装成功（1.0.79 / 85，`firstInstallTime` 仍为 2026-11-06 18:47:52，未清数据）。
- 真机验证：系统推荐开关页三行为开关形态、行下有结果说明，分组标题为「常用推荐开关」，截图为 `screenshots/2026-11-08-系统推荐开关-常用推荐开关三行开关.png`。测试机当前未启用净屏无障碍服务，点开关会按既有流程提示先开启服务（未做实际改写）。
- 涉及文件：`src/screens/SystemRecommendScreen.tsx`、本修改记录。
- 参考：当前工作区修改，未提交。

### 2026-11-08 09:44:38 +08:00 — 系统推荐开关页移除无障碍服务提示条

- 原因：该页顶部的「无障碍服务未开启，暂时不能自动帮你关这些开关。」文字与「去开启无障碍服务」按钮属于多余提示，且无障碍未开启时该页开关仍可正常展示和操作（代点开关会在点击时自行提示）。
- 删除该整块提示：文字、按钮及其条件渲染分支，标题下方直接进入「直接改写与代点」区块，不留空白占位与间距。
- 清理只服务该提示的样式 `status`、`action`、`actionText`；`accessibilityReady`、`readAccessibility`、`openAccessibility` 仍被开关点击授权判断与错误提示使用，保留。
- 未改动同页其他文案、跳转、开关列表与授权通道入口；未触碰其他会话改动。
- 验证：`npx tsc --noEmit` 通过；`:app:assembleRelease` BUILD SUCCESSFUL；`adb -s 2f8898c80822 install -r` 覆盖安装成功（1.0.79 / 85，`firstInstallTime` 仍为 2026-11-06 18:47:52，未清数据、未卸载）。
- 真机验证：净屏无障碍服务未启用状态下（`enabled_accessibility_services` 仅为联控服务），小米 22101317C 页面顶部标题下直接是「直接改写与代点」，开关列表显示正常。
- 涉及文件：`src/screens/SystemRecommendScreen.tsx`、本修改记录。
- 参考：当前工作区修改，未提交。

### 2026-11-08 09:42:08 +08:00 — 「更多清理」三块内容并入「系统广告与营销通知」页

- 原因：按用户要求取消单独的“更多清理”入口，把授权通道、营销通知、快应用与系统广告组件整体上移，与系统推荐开关、查看应用同级，避免用户多点一层。
- 页面结构：`SystemCleanupScreen.tsx` 依次渲染系统推荐开关、查看应用（180 个）、授权通道卡片、营销通知（标题 + 候选渠道说明 + 扫描渠道）、快应用与系统广告组件（标题 + 说明 + 检查本机组件 + 快应用/系统广告组件两组状态），全部为同级卡片；滚动到底可见最后两组状态行，无遮挡与重叠。
- 路由清理：`App.tsx` 删除 `moreCleanup` 页面类型、`MoreCleanupScreen` 导入、设置子页判定与渲染分支；`SystemCleanupScreen` 的 `onOpenMoreCleanup` 改为 `onOpenAdbAccess`，授权通道卡片直接进入「授权通道」页；删除 `src/screens/MoreCleanupScreen.tsx`，无死链残留。
- 内容与逻辑：授权通道、扫描渠道、检查本机组件、目标停用/恢复的文案、弹窗、状态与原生调用保持原样搬运，未改写；另将帮助与反馈中“更多清理里的 Shizuku”条目改名为“系统广告与营销通知里的 Shizuku 是什么？”，路径由“设置 > 系统广告与营销通知 > 更多清理”改为“设置 > 系统广告与营销通知”。
- 文档：同步更新 `README.md`（页面清单与已实现交互）和 `docs/wiki/README.md`（工程结构、设置页结构、界面文案约束例外条款与授权通道入口路径）。
- 验证：`npx tsc --noEmit` 通过；`:app:assembleRelease` 成功（发现并行窗口正在构建，等待其结束后再执行；JS bundle 于 09:33:55 重新生成，已包含本次改动）；设备上为 1.0.79 / 85（`lastUpdateTime` 2026-11-08 09:34:38，`firstInstallTime` 仍为 2026-11-06 18:47:52，未清数据、未卸载），经截图确认是新版页面。帮助与反馈里的 FAQ 标题改名发生在该次安装之后，未重新安装，仅代码层生效。
- 真机验证：小米 22101317C 截图 `screenshots/2026-11-08-系统广告与营销通知-合并后顶部.png`（顶部到快应用区块）与 `screenshots/2026-11-08-系统广告与营销通知-合并后滚动到底.png`（滚动到底，含系统广告组件状态）。期间设备被另一会话/窗口占用，前台数次被切走，已重试取得。
- 涉及文件：`src/screens/SystemCleanupScreen.tsx`、`src/screens/MoreCleanupScreen.tsx`（删除）、`App.tsx`、`src/screens/SettingsDetailScreen.tsx`、`README.md`、`docs/wiki/README.md`、`screenshots/`、本修改记录。
- 参考：当前工作区修改，未提交。

### 2026-11-08 09:30:37 +08:00 — 授权通道页改成先讲能做什么，再讲怎么点

- 原因：上一版虽已按按钮写了作用，但首屏仍以「打开开发者选项」「自动查找无线调试」这些技术动作开场，用户看完不知道这页开了之后到底能得到什么。
- 页首新增一句目的说明：「连上之后，净屏就能直接改这台手机的广告推荐开关，也能关掉快应用和系统广告组件；不用 Root，也不用再装别的工具。」内容对应实际能力：系统推荐开关的直接改写，以及更多清理里的快应用/系统广告组件停用。
- 按钮旁文案改为讲结果：打开开发者选项「在这里把「无线调试」打开，净屏才能拿到改系统开关的权限。」；自动查找无线调试「以前连过的话，点一下就能接回来，接着改系统开关，不用重新走一遍。」；开始自动配对「第一次连接点这里：跟着系统的弹窗确认一次，净屏就自动连上，之后能直接改系统开关。」
- 状态行提示改为「还没连上。按下面的顺序点一遍就能连上。」与「已经连上，可以去改系统开关了；重启手机后需要再连一次。」
- 「高级」保留端口与「连接」，开头写明「自动配对没连上时才用这里」，并用一句人话解释端口是「无线调试」页上显示的一串数字；输入占位改为「「无线调试」页里的数字」。电脑 adb 兜底改为「会用电脑命令的话……不熟悉可以跳过这一节。」底部隐私行改为「连接用的密钥只保存在这台手机上，不会上传。」
- 连接成功弹窗去掉 Shizuku 术语，统一为「已连接，现在可以直接改系统开关了。」；端口校验提示改为「请填写「无线调试」页面上显示的那串数字。」
- 同步更新 `docs/wiki/README.md` 的「界面文案约束」例外条款，登记页首目的说明与「高级」内的人话解释。
- 验证：`npx tsc --noEmit` 通过；`:app:assembleRelease` 成功；`adb -s 2f8898c80822 install -r` 覆盖安装成功（1.0.79 / 85，`firstInstallTime` 仍为 2026-11-06 18:47:52，未清数据）。
- 真机验证：小米 22101317C 整页状态已检查。期间该设备被另一会话持续占用（前台被反复切到系统设置、无障碍、系统推荐开关等页面），「高级」展开态截图多次被切走，未取得。
- 涉及文件：`src/screens/AdbAccessScreen.tsx`、`docs/wiki/README.md`、本修改记录。
- 参考：当前工作区修改，未提交。

### 2026-11-08 09:14:52 +08:00 — 授权通道页改为按按钮说明作用，技术细节收进「高级」

- 原因：该页四个按钮只写操作名，用户不知道开启后能得到什么；页面上还直接暴露本机地址、连接端口和 `adb tcpip 5555` / `adb connect 手机IP:5555`，不容易看懂。
- 连接状态卡片：状态行只保留「正在读取状态」「这台手机不支持无线调试配对」「已连接」「未连接」，下方一句「先打开「无线调试」，再点下面的按钮。」或「现在可以直接改系统开关，重启手机后需要再连一次。」；移除原「本机地址」与「重启或关闭无线调试后需重新连接」两行。
- 按钮作用说明：打开开发者选项「打开后才能打开「无线调试」，下面的按钮才用得上。」；自动查找无线调试「以前配对过的话，点一下会自动连回来，不用再填任何数字。」；开始自动配对「点一下，净屏会自己读出配对码并连上；连上后不用数据线也能改系统开关。」；连接「自动配对没连上时，把「无线调试」页显示的端口抄到这里，再点一次。」；删除「需先在开发者选项打开“无线调试”。」前置说明。
- 新增「高级」折叠区（默认收起）：收进连接端口输入与「连接」按钮、本机地址和「电脑 adb 兜底」两行命令；一级界面不再出现 IP、端口与 adb 命令。自动配对下方的「连接」卡片整体移入该区。
- 底部隐私行改为「配对码和密钥只保存在这台手机上，不会上传。」
- 同步在 `docs/wiki/README.md` 的「界面文案约束」登记本页例外，避免后续按旧规则删除这些作用说明。
- 验证：`npx tsc --noEmit` 通过；`:app:assembleRelease` 成功；`adb -s 2f8898c80822 install -r` 覆盖安装成功（1.0.78 / 84，`firstInstallTime` 仍为 2026-11-06 18:47:52，未清数据）。
- 真机验证：小米 22101317C 整页及高级展开区域均已检查（包含本机网络信息的截图未纳入版本库）；另确认展开后无障碍树中「高级」「收起」「连接端口」均存在。
- 说明：真机操作期间设备同时被另一会话占用，多次截图被切到系统设置页作废；仅采用应用处于前台时拍到的三张。
- 涉及文件：`src/screens/AdbAccessScreen.tsx`、`docs/wiki/README.md`、本修改记录。
- 参考：当前工作区修改，未提交。

### 2026-11-08 01:18:46 +08:00 — 1.0.78：收拢广告通知页与授权通道页，加固设置跳转

- 系统广告与营销通知页删除「按应用查看通知」小标题、展开区整段用途说明与搜索按钮上的「输入应用名称查找」，保留「系统推荐开关」「查看应用 / 收起」「搜索应用」占位符和输入框，删除后各区块自然收拢。
- 授权通道页把「重启或关闭无线调试会断开」「需在开发者选项打开无线调试」「配对完成后自动连接」三段长说明压缩为状态行下的一行短提示，其余卡片只保留标题、状态行和按钮。
- `ClearScreenSettingsNavigator` 按「厂商专有 Activity → 系统标准 Intent → 本应用详情页」顺序逐级尝试并捕获 `ActivityNotFoundException`/`RuntimeException`，开发者选项与按应用通知新增品牌候选；只能打开应用信息页时弹出 Toast 说明下一步入口，避免点击无反应或崩溃。
- 本机控制与权限页不属于本仓库：全库检索与 git 历史均无该页，该页实际位于 LinkPilot `MainActivity.kt` 的 `LOCAL_CONTROL` 分支，本轮未改动 LinkPilot，待用户确认归属后再处理。
- 版本号：`app.json` 同步为 1.0.78 / versionCode 84，与 `android/app/build.gradle` 一致。
- 验证：`npx tsc --noEmit` 通过；Gradle 无残留锁，`:app:assembleRelease` BUILD SUCCESSFUL；`adb install -r` 覆盖安装到小米 22101317C（2f8898c80822），包版本 1.0.78（84），`firstInstallTime` 未变化（未清数据）。
- 真机验证：系统广告与营销通知页收拢与保留项、授权通道页短提示均有整页截图；「去开启无障碍服务」落到 `com.android.settings/.accessibility.MiuiAccessibilitySettingsActivity`；「打开开发者选项」落到 `com.miui.securitycenter/com.miui.appmanager.ApplicationsDetailsActivity`（应用信息页 + 兜底提示）。按应用通知跳转在本轮被设备并发占用打断（包一度被另一会话 `pm disable`、前台被反复切走），未完成截图取证，组件 `com.android.settings/.Settings$AppNotificationSettingsActivity` 已确认存在。
- 涉及文件：`src/screens/SystemCleanupScreen.tsx`、`src/screens/AdbAccessScreen.tsx`、`android/app/src/main/java/com/clearscreen/prototype/backend/ClearScreenSettingsNavigator.kt`、`ClearScreenNativeModule.kt`、`app.json`、`android/app/build.gradle`、本修改记录。
- 参考：当前工作区修改，未提交。

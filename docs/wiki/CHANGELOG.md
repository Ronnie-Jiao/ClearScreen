# 净屏修改记录

本文件按时间倒序记录项目修改，供后续 AI 和开发者开始任务前快速了解最近变化。自本记录建立起，新增条目的时间使用中国标准时间（UTC+08:00）；既有条目的时间按文件系统记录的本地最后修改时间整理。

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

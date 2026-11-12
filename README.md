# ClearScreen | 净屏

<p align="center">
  <img src="assets/logo.png" alt="ClearScreen / 净屏 logo" width="112" />
</p>

<p align="center"><strong>Clean your screen. Focus on life.</strong><br />净化屏幕，专注生活</p>

<p align="center"><a href="#english">English</a> · <a href="#简体中文">简体中文</a></p>

## English

ClearScreen is an Android-first ad assistance app. With the accessibility service enabled by the user, it can recognize certain ad skip and close controls in apps the user has enabled. It also provides entry points for system recommendation settings, app notifications, and marketing notification channels.

ClearScreen acts only when the target is clear. If the evidence is insufficient, a control is ambiguous, or a system page cannot be verified, it skips the action. It does not guarantee that every ad will be removed and does not grant system permissions on the user's behalf.

[GitHub repository](https://github.com/Ronnie-Jiao/ClearScreen) · [Privacy policy](docs/wiki/隐私政策.md) · [Changelog](docs/wiki/CHANGELOG.md)

### Features

- **Ad skip and close:** Recognizes high-confidence ad skip controls and close controls shown in a clear ad context, in apps enabled by the user.
- **Learn to close:** The user can select a close control in the target app and save a rule. Matching uses the control and the structure of its surrounding ad area rather than screen coordinates. A rule runs only when the target can be uniquely confirmed; otherwise, it is skipped.
- **System ad settings:** Checks supported system recommendation settings and guides the user to the relevant system page. Available controls depend on the device manufacturer, OS version, and settings pages present on the device.
- **Notification management:** Provides pages for app notifications and marketing notification channels. Reading or changing complete notification channel data, and managing some system ad components, requires user-authorized ADB Shell or Shizuku access. Device support varies.
- **Activity history and app rules:** Review processing results, manage which apps are enabled, and manage learned close rules.
- **In-app guide:** The Help & Feedback section explains Android and iOS features and troubleshooting. Search titles, descriptions, and keywords, then open the related app page from a result.

### Platform support

| Platform | Current capabilities | Scope and limitations |
| --- | --- | --- |
| Android | Main development platform; accessibility-based ad assistance, learned close rules, system recommendation settings, and notification management | Permissions must be granted by the user. Recognition and manufacturer settings compatibility depend on app UI structure, accessibility nodes, and OS version. |
| iOS | Safari web extension | Works only on websites where the user enables and authorizes the extension. It does not read screens across apps. System-wide ad filtering is not integrated. |
| Web | Early interactive prototype under **public/** | Not a complete web version of the Android/iOS app. |
| HarmonyOS | Isolated UI reuse experiments | HarmonyOS accessibility, app management, and ad processing are not implemented; these experiments are not a usable app release. |

### Permissions, safety, and privacy

Android ad assistance depends on the system accessibility service. When enabled, the service reads accessible UI structure in the current app and, in supported cases, may use on-device screenshot text recognition to assess ad context. It acts only after the user accepts the privacy policy, enables the service and master switch, and leaves the target app rule enabled.

System recommendation settings and notification features may require the user to connect ADB Shell or authorize Shizuku. ClearScreen does not use root access or create, take over, or rely on a system VPN. Whether a system setting can be read or changed depends on the device manufacturer and OS version. The app stops when it cannot confirm the target.

App rules, learned rules, processing history, and preferences are stored locally. ClearScreen does not save ad screenshots or recognized page text. Google ML Kit may contact Google services to obtain component information and may send diagnostic or usage metrics. GitHub is accessed only when the user explicitly checks for release information. See the [privacy policy](docs/wiki/隐私政策.md) for details.

### Download and development

#### Install the Android app

If a release is available, download it from [GitHub Releases](https://github.com/Ronnie-Jiao/ClearScreen/releases). To preserve local data when updating, install over the existing app and keep the same application ID and release signing key. Do not uninstall the previous version first.

#### Run locally

Requirements: Node.js, npm, Android Studio and Android SDK, plus an Android device or emulator.

    npm install
    npm run android

Start the Expo development server with:

    npm run start

For release signing, versioning, building, and installation, see the [Android build and installation guide](docs/wiki/README.md#5-apk-构建与安装约定). Do not commit release keys or signing configuration.

### Documentation

- [Project Wiki: architecture, capabilities, permissions, and build guidance](docs/wiki/README.md)
- [Changelog](docs/wiki/CHANGELOG.md)
- [Privacy policy](docs/wiki/隐私政策.md)
- [Permission verification and manufacturer notes](docs/wiki/权限状态核验.md)
- [App store compliance review](docs/wiki/应用商店合规核查.md)

### Repository layout

    ClearScreen/
    ├─ App.tsx                     # React Native app entry point
    ├─ src/                        # Screens, components, theme, and native module adapters
    ├─ android/                    # Android services and platform integration
    ├─ targets/safari/             # Safari web extension
    ├─ targets/harmony-pilot/      # Isolated HarmonyOS UI reuse experiment
    ├─ targets/harmony-bare-pilot/ # Separate HarmonyOS screen reuse experiment
    ├─ public/                     # Early web prototype
    └─ docs/wiki/                  # Project Wiki, privacy policy, and changelog

### License

There is currently no root-level LICENSE file. This repository does not explicitly grant permission to copy, modify, or redistribute its source code. Do not assume an open-source license.

---

## 简体中文

净屏（ClearScreen）是一款以 Android 为主要平台的广告辅助工具。用户开启无障碍服务后，应用可以在用户启用的应用中识别部分广告跳过和关闭控件。应用还提供系统推荐设置、应用通知和营销通知渠道的管理入口。

净屏只在目标明确时操作。识别证据不足、控件存在歧义或无法核验系统页面时，会跳过操作。它不能保证清除所有广告，也不会代替用户授予系统权限。

[GitHub 项目仓库](https://github.com/Ronnie-Jiao/ClearScreen) · [隐私政策](docs/wiki/隐私政策.md) · [修改记录](docs/wiki/CHANGELOG.md)

### 功能

- **广告跳过与关闭：** 在用户启用的应用中识别高置信度的广告跳过控件，以及具有明确广告语境的关闭控件。
- **学习关闭：** 用户可以在目标应用中选择关闭控件并保存规则。规则依据控件及其所在广告区域的结构进行匹配，不使用屏幕坐标。只有能够唯一确认目标时才执行；否则跳过。
- **系统广告设置：** 检查受支持的系统推荐设置，并引导用户进入对应系统页面。可用开关取决于设备品牌、系统版本和设备上实际存在的设置页面。
- **通知管理：** 提供应用通知和营销通知渠道页面。读取或修改完整通知渠道数据，以及管理部分系统广告组件，需要用户授权 ADB Shell 或 Shizuku。设备支持情况各不相同。
- **处理记录与应用规则：** 查看处理结果、管理启用自动处理的应用，并管理学习关闭规则。
- **应用内使用说明：** “帮助与反馈”介绍 Android/iOS 功能并提供问题排查。可以搜索标题、正文和关键词，再从结果跳转到相关应用页面。

### 平台支持

| 平台 | 当前能力 | 范围与限制 |
| --- | --- | --- |
| Android | 主要开发平台；基于无障碍服务的广告辅助、学习关闭、系统推荐设置和通知管理 | 需要用户逐项授权。识别及厂商设置兼容性受应用界面、无障碍节点和系统版本影响。 |
| iOS | Safari 网页扩展 | 仅在用户启用扩展并授权的网站中运行；不跨 App 读取屏幕。尚未接入系统级广告过滤。 |
| Web | 早期交互原型，位于 public/ 目录 | 不是 Android/iOS 应用的完整 Web 版本。 |
| HarmonyOS | 隔离的界面复用试验 | 尚未实现鸿蒙无障碍服务、应用管理和广告处理；这些试验不是可用的应用版本。 |

### 权限、安全与隐私

Android 广告辅助功能依赖系统无障碍服务。开启后，服务会读取当前应用中可访问的界面结构；在支持的场景下，也可能使用设备端截图文字识别判断广告语境。只有用户同意隐私政策、开启服务和总开关，并且目标应用规则处于启用状态时，服务才会操作。

系统推荐设置和通知功能可能需要用户连接 ADB Shell 或授权 Shizuku。净屏不使用 Root 权限，也不会创建、接管或依赖系统 VPN。系统设置能否读取或修改取决于设备品牌和系统版本；应用无法确认目标时会停止操作。

应用规则、学习规则、处理记录和偏好设置保存在本机。净屏不会保存广告截图或识别出的页面文字。Google ML Kit 可能联系 Google 服务获取组件信息，也可能发送诊断或使用指标。只有用户主动检查版本信息时，应用才会访问 GitHub。详情见[隐私政策](docs/wiki/隐私政策.md)。

### 下载与开发

#### 安装 Android 应用

如已发布版本，可从 [GitHub Releases](https://github.com/Ronnie-Jiao/ClearScreen/releases) 下载。升级并保留本机数据时，请在原应用上覆盖安装，并保持包名和发布签名不变。不要先卸载旧版本。

#### 本地运行

需要 Node.js、npm、Android Studio 和 Android SDK，以及一台 Android 设备或模拟器。

    npm install
    npm run android

使用以下命令启动 Expo 开发服务：

    npm run start

Release 签名、版本递增、构建和安装步骤见 [Android 构建与安装说明](docs/wiki/README.md#5-apk-构建与安装约定)。请勿将发布密钥或签名配置提交到仓库。

### 项目文档

- [项目 Wiki：架构、功能边界、权限与构建说明](docs/wiki/README.md)
- [修改记录](docs/wiki/CHANGELOG.md)
- [隐私政策](docs/wiki/隐私政策.md)
- [权限状态核验与品牌说明](docs/wiki/权限状态核验.md)
- [应用商店合规核查](docs/wiki/应用商店合规核查.md)

### 仓库结构

    ClearScreen/
    ├─ App.tsx                     # React Native 应用入口
    ├─ src/                        # 页面、组件、主题与原生模块适配
    ├─ android/                    # Android 服务与平台集成
    ├─ targets/safari/             # Safari 网页扩展
    ├─ targets/harmony-pilot/      # 隔离的 HarmonyOS 界面复用试验
    ├─ targets/harmony-bare-pilot/ # 独立的 HarmonyOS 页面复用试验
    ├─ public/                     # 早期 Web 原型
    └─ docs/wiki/                  # 项目 Wiki、隐私政策与修改记录

### 许可证

仓库目前没有根目录 LICENSE 文件。本仓库没有明确授予复制、修改或再分发源码的权限；请勿据此假定项目采用开源许可证。
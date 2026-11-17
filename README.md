# 净屏 | ClearScreen

<p align="center">
  <img src="assets/logo.png" alt="净屏 / ClearScreen" width="112" />
</p>

<p align="center"><strong>净化屏幕，畅享生活</strong><br />Clean your screen. Enjoy life.</p>

<p align="center"><a href="#简体中文">简体中文</a> · <a href="#english">English</a></p>

## 简体中文

净屏帮助减少常用应用中的广告干扰，并集中管理应用通知和受支持的系统推荐设置。

### 主要功能

- **自动跳过与关闭广告：** 应用刚进入前台时，直接识别 X、“关闭”“跳过”（含“跳过 01”等倒计时）控件并尝试关闭，无需品牌、广告标记或跳转文案，不限定按钮位置。启动后会短时补查延迟出现的控件；首次触摸无效时可在重新核验后有限重试。后续页面按局部广告证据处理。
- **学习关闭：** 遇到无法识别的广告关闭按钮时，可手动选择控件。净屏会在本机保存规则，并在之后遇到相似广告时尝试自动关闭。
- **小型浮动广告：** 对能确认广告或促销控件结构的小浮层，按其关闭控件和 X 图形处理，不绑定广告文案或固定屏幕位置；结构无法确认时可能无法自动处理。
- **应用管理：** 自行选择允许净屏处理的应用，也可随时关闭单个应用或全部自动处理。
- **通知与系统推荐管理：** 查看和管理应用通知、营销通知渠道及受支持的系统推荐设置。点击应用图标或名称可打开对应的系统通知设置；若手机系统不接受直接修改，也可在那里调整。可用项目会因手机品牌和系统版本而异。
- **处理记录：** 查看广告处理结果，并管理已学习的关闭规则。

### 平台支持

- **Android：** 支持广告处理、学习关闭、应用管理、通知管理和部分系统推荐设置。
- **iOS Safari：** 支持 Safari 网页扩展；仅在用户启用扩展并授权的网站中工作，不读取其他 App 的屏幕。

### 使用与隐私

Android 自动处理需要用户开启系统无障碍权限，并启用希望净屏处理的应用。部分通知渠道或系统设置需要额外授权，具体以应用内提示为准。广告识别受应用界面和手机系统限制，不能保证覆盖所有广告。

开启自动处理后，净屏使用带常驻通知的前台服务维持后台运行，关闭界面不会主动停止服务；暂停净屏或撤销无障碍授权会停止该前台服务。重新打开时会检查连接，已有系统设置写入授权的设备可尝试恢复仍然开启但已断开的无障碍服务。系统强行停止、厂商清理或用户撤销授权仍可能需要手动处理。

应用规则、学习规则和处理记录保存在本机。净屏不会保存广告截图或识别出的页面文字；Google ML Kit 可能按其说明处理诊断或使用指标。详情见[公开隐私政策](https://clearscreen-privacy-policy.buzzyorbit64.chatgpt.site)。

### 获取应用

[下载 Android 版本](https://github.com/Ronnie-Jiao/ClearScreen/releases)

## English

ClearScreen helps reduce ad interruptions in supported apps and brings app notifications and supported system recommendations together in one place.

### Features

- **Skip or close ads:** During app startup, recognizes X, close and skip controls, including countdowns, without requiring a brand, ad badge, redirect text or a particular screen position. Brief rescans check for late controls; an ineffective first touch can be retried after the control is checked again. Later pages use local ad evidence.
- **Learn to close:** Select an unfamiliar ad close control once. ClearScreen saves the rule on your device and can try to close similar ads later.
- **Small floating ads:** Uses identifiable ad or promotion-layer structure and its close control or X shape, without matching creative copy or a fixed screen position. Unknown structures may remain unsupported.
- **Choose apps:** Control which apps ClearScreen can handle, individually or all at once.
- **Manage notifications and recommendations:** Review app notifications, marketing notification channels, and supported system recommendation settings. Tap an app icon or name to open its system notification settings; use that page if the device does not accept a direct change. Availability varies by device and Android version.
- **Review activity:** See processing results and manage learned close rules.

### Platforms

- **Android:** Ad assistance, learned close rules, app selection, notification management, and selected system recommendation settings.
- **iOS Safari:** A web extension that works only on websites where you enable and authorize it. It does not read screens across apps.

### Use and privacy

On Android, automatic ad assistance requires the accessibility permission and the apps you choose to enable. Some notification channels or system settings may require additional authorization. Ad recognition depends on app interfaces and device support, so it cannot cover every ad.

While automatic assistance is enabled, a foreground service with an ongoing notification keeps it running after the app UI closes. Pausing assistance or revoking accessibility stops that foreground service. On reopening, devices with an existing secure-settings write grant can try reconnecting an accessibility service that remains enabled but disconnected. Force-stop, vendor cleanup, or a revoked grant may still require manual intervention.

App rules, learned rules, and activity records stay on your device. ClearScreen does not save ad screenshots or recognized page text. Google ML Kit may process diagnostic or usage metrics as described by Google. See the [public privacy policy](https://clearscreen-privacy-policy.buzzyorbit64.chatgpt.site).

### Get ClearScreen

[Download the Android app](https://github.com/Ronnie-Jiao/ClearScreen/releases)

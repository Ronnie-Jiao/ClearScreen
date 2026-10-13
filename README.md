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
- 仅接入 AccessibilityService 等本地 Android 能力；不申请、不启动、不接管系统 VPN，首次使用仍需用户在系统设置中授权无障碍
- 广告识别以避免误关为先：不能只凭 X/叉号、屏幕角落、单个商业词或屏幕其他位置的广告标记点击。广告证据、卡片/弹窗归属和独立的小型可点击关闭控件必须能在同一容器内对应；证据不完整、页面结构变化或点击目标过大时不操作。未知应用需要同时做广告正例和业务页面负例回归；这能降低误点风险，但不能承诺绝对零误点或全覆盖。
- 通用 OCR 补充规则：开屏倒计时须同时匹配右上角小型“跳过”控件和大面积广告画面，并有广告标记或多项促销线索及行动文案；弹窗即使没有标准“弹窗”控件名，也只在同一个有边界的局部容器里确认广告标记+行动文案，或至少三项促销线索+行动文案，并确认独立小叉号位于容器边缘时才处理。仍只点击真实无障碍控件，不用屏幕坐标，也不点击下载/购买等广告内容。
- 彩云天气中下部无文字关闭按钮仅有一条窄规则：必须同时匹配该应用的 `entrance_image` 与 `close` 资源 ID、同一无障碍父容器、海报下方居中且尺寸受限的真实可点击节点，以及海报图内的 `SVIP + 年卡 + 会员 + 抽奖/奖品 + 立即查看` OCR 文案；未知应用、资源 ID 或文案不匹配时不点击。
- 彩云天气另一种 SDK 插屏仅在包名匹配、底部居中的小型可点击 `cll_close_click` 与 `cll_interstitial_ad_gray`/`cll_gdt_ad_view` 结构匹配，并且同一插屏树内有明确“广告”标记和“跳转详情页/第三方应用”提示时关闭；任一结构或文案缺失、关闭目标位置/尺寸不符时不点，不把屏幕别处证据拼接进来。
- 无障碍规则覆盖用户允许的新应用：开屏/视频候选必须有明确广告跳过文字、广告专用资源 ID 或同一局部区域的视频广告证据，并且实际可点击目标尺寸受限。普通“跳过”、单独的 `jump`/`skip` ID、倒计时和右上角位置都不能单独授权点击。视频按钮首次出现时短时重扫；确认进入播放器或出现视频广告线索后，最长监测 45 秒，以捕捉延迟启用的跳过按钮。没有可点击“跳过”的强制广告不会被绕过。
- 暂停广告和内嵌广告卡片：同一无障碍子树必须有明确“广告/推荐广告”等小徽标和独立的小型关闭节点；一般 X 只能位于卡片右上边缘，底部居中按钮还需明确的“关闭广告”文字或广告关闭资源 ID。只凭图片控件的外观、页面其他位置的广告文字或普通播放器按钮不会点击。普通弹窗也必须有所属广告浮层证据；系统权限/安装确认界面不自动操作。不使用坐标点击、通用 `ACTION_DISMISS` 或 VPN。
- vivo 等系统如果在返回设置列表后仍把服务改回“已关闭”，还需要在系统设置中允许净屏自启动、后台运行并关闭电池优化；这属于系统对无障碍服务的管控，应用本身不能绕过系统授权。
- Android 16 对 ADB/本地侧载应用的无障碍可能启用“受限设置”；净屏会读取安装来源并先引导用户尝试系统无障碍页。如果 vivo 没有提供“应用信息 → 允许受限设置”入口，则必须通过 vivo EasyShare 或可信应用商店重新安装，应用自身不能绕过系统校验。
- 无障碍实现参考了李跳跳 APK 的服务声明形态：服务使用 `exported=false`、`settingsActivity`、通用反馈类型和 Android 12+ 的 `isAccessibilityTool` 配置，并在独立的 `:accessibility` 进程中运行；主界面的 React Native/Expo 进程不会被无障碍服务加载。这不会伪造或绕过系统授权。
- 为便于与李跳跳的 `targetSdk=33` 做真机 A/B 测试，支持 `cd android; .\\gradlew.bat :app:assembleRelease -PclearscreenTargetSdk=33`。手机单独安装必须使用 `assembleRelease` 生成的 APK；`assembleDebug` 仅用于连接 Metro 调试，未启动 8081 服务时会显示 `Unable to load script`。未传该参数时仍使用当前 Android/Expo 目标 SDK；Android 16 的受限设置最终仍由系统根据安装来源决定。

1.0.18 在 Android 11 及以上增加 APK 内置中文 OCR，与无障碍节点结构和卡片区域结合识别弹窗、信息流/瀑布流广告；当时首页曾提供开屏/视频、弹窗和信息流分类开关。1.0.19 增加 OCR 识别卡片内独立 X 的辅助路径、树变化时的一次性新截图重试，并收紧广告证据的容器归属。1.0.20–1.0.21 增加特定顶部居中开屏广告的 OCR 组合证据及开屏独立 OCR 调度。1.0.23–1.0.24 为彩云天气两种已见样式增加资源 ID、局部结构和文案联合校验；后一种插屏的真机动作归因及安全性尚未验证。1.0.25 增加有时限的视频广告监测、明确徽标和关闭节点同子树的内嵌/暂停广告处理、系统界面保护及误点门槛收紧。1.0.26 增加跨应用的全屏开屏 OCR 证据和局部弹窗容器兜底，以覆盖应用未标注弹窗类型的广告结构。1.0.27 调整底部导航、首页高频控制、应用筛选和排除应用入口。图像只在本机内存识别，不保存、不上传、不使用 VPN。此方案是 OCR 与结构规则融合，不是专用训练模型；Canvas、受保护窗口、强制观看广告或没有可访问关闭节点的广告仍可能漏过，不能承诺零误点或全覆盖。

## 项目 Wiki

- [净屏 Wiki](docs/wiki/README.md)：项目架构、无障碍识别边界、vivo 授权、USB 数据线安装、构建和发布检查清单。
- [修改记录](docs/wiki/CHANGELOG.md)：按中国标准时间倒序记录近期代码和文档变化；AI 每次开始修改前先读最新条目（仓库指引见 `AGENTS.md`）。

## 无障碍授权诊断构建

当前仓库提供独立的授权诊断分支构建，不把未经真机验证的身份实验混入普通发布包：

1. 首次在本机生成固定 Release 签名：`powershell -ExecutionPolicy Bypass -File .\\tools\\New-ClearScreenSigning.ps1`。`android/keystore.properties` 与 `android/clearscreen-release.keystore` 已加入忽略列表，不能提交到 Git；正式更新必须继续使用同一份证书。
2. 构建正式签名的诊断包、兼容包和极简探针：`powershell -ExecutionPolicy Bypass -File .\\tools\\Build-AccessibilityDiagnostics.ps1`。
3. 诊断包默认使用新的 `com.clearscreen.app` 包名，并移除 VPN、悬浮窗、后台和旧存储等可选能力；极简探针包为 `com.clearscreen.probe`，只有 Activity 和一个空的无障碍服务。
4. 在手机上完成一次“打开 → 返回列表”的复现后，运行 `powershell -ExecutionPolicy Bypass -File .\\tools\\Collect-AccessibilityDiagnostics.ps1 -PackageName com.clearscreen.app -ClearLogcat`，脚本会保存安装来源、首次安装时间、包信息、无障碍列表和 logcat。
5. 用 `powershell -ExecutionPolicy Bypass -File .\\tools\\Compare-AccessibilityPackages.ps1` 对比净屏与李跳跳的安装历史和来源字段。

普通 Release 构建现在要求本机存在正式签名配置；仅用于临时开发的 Debug 签名需要显式传 `-PallowDebugSigning=true`。`lttCompat` 构建只保留李跳跳风格的 `isAccessibilityTool=true` 变体，正式配置不再把该语义字段当作授权保活开关。

## 已实现页面

1. 启动页与欢迎页
2. 自动跳过服务权限
3. 首页
4. 应用列表
5. 应用详情
6. 处理记录与记录详情
7. 设置
8. 排除应用
9. 故障诊断

## 已实现交互

- 底部导航为首页、应用、设置；处理记录从首页或应用详情进入
- 首页按服务授权、服务连接、应用选择和总开关状态提供对应操作
- 首页展示真实处理统计和最近记录
- 应用列表支持搜索、已开启和已排除筛选；应用详情与列表使用同一个启用开关
- 排除应用页即时保存，与应用列表共享同一条规则
- 处理记录支持全部、已执行和失败筛选，并可查看记录详情
- 设置提供权限、后台指引、故障诊断、记录保留、清空记录和版本信息
- 返回时保留页面来源和列表状态

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

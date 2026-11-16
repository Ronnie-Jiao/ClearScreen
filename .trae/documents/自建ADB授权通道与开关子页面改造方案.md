# 自建 ADB 授权通道 + 开关子页面改造方案

## Context

用户希望净屏不依赖 Shizuku 也能执行清理（改系统推荐开关、停用快应用/广告组件、扫描营销通知渠道）。已拍板走「自建类似 Shizuku 的授权通道」：Android 11+ 无线调试配对，App 自己当 ADB 客户端；电脑 `adb tcpip 5555` 作为兜底。用户接受这仍是**一次性授权**，不是零授权。

本方案同时合并此前挂起的 4 项改动，避免反复改同一处 UI：
1. 「系统推荐开关」拆成独立子页面，原页留一行入口。
2. 开关尽量直连系统设置，不走无障碍（能直接写的 3 个 vivo 开关已走 Settings.Secure）。
3. 删除灰色说明小字；权限未授权、不可用原因、危险后果改为正常字号状态行。
4. 补齐「更多清理」里的快应用/系统广告组件，让「检查设备」有真实结果。

## 关键技术结论（已核实）

- 选型：`com.github.MuntashirAkon:libadb-android:3.1.1`（Apache-2.0，JitPack，AOSP libadb 的 JNI 封装）+ `org.conscrypt:conscrypt-android:2.5.3`。
  - 已确认公开 API：`pair(host, port, code)`、`connect(host, port)`（自动 A_STLS 升级）、`connectTls/connectTcp/autoConnect`（mDNS）、`openStream("shell:...")`、`disconnect()/close()`、`isConnected()`。
  - 传递依赖：androidx.annotation、bcprov-jdk15to18 1.81、spake2-android 2.2.1。
- Android 侧能力等价：ADB shell 与 Shizuku 用户服务都是 uid 2000，现有 `settings`/`content insert`/`pm suspend`/`dumpsys notification --noredact` 命令可原样复用。
- `WRITE_SETTINGS` 不加：它只能写 `Settings.System`，vivo 三个键在 `Settings.Secure`，需要 `WRITE_SECURE_SETTINGS`（系统签名/特权才有），普通 App 申请不到。官方可行路径就是 shell 级授权。
- 23 个推荐开关里只有 3 个有可直接写的 secure 键；其余 20 个状态在负一屏/浏览器/应用商店的私有数据里，任何第三方 App 都无法直接改写，继续保留「打开页面 + 无障碍代点」的兜底。**这是系统限制，不做虚假承诺。**
- 安全边界：私钥/证书只存 App 私有目录（0700/0600），不进备份；日志不打印配对码、私钥、完整命令、通知正文；JS 侧只暴露语义化操作，绝不暴露 `exec(String)`；命令参数全部来自原生白名单。

## 实施顺序

### 阶段 0：依赖闸门（先验证，失败就停）

- `android/build.gradle` 已配置 JitPack，确认可解析 `libadb-android:3.1.1` 与 `conscrypt-android:2.5.3`（含 spake2、bcprov）。
- `app/build.gradle` 加依赖，`abiFilters` 只留 `arm64-v8a`（真机架构），降低体积与对齐风险。
- 在线跑一次 `:app:assembleDebug`，解包 APK：确认 `libadb.so` 存在、LOAD 段 16KB 对齐、`zipalign -c -P 16 4` 通过；装机后调一次空实现的状态方法，确认 native 库能加载。
- 若 JitPack 404 / native 加载失败：停止并汇报，退回只做「电脑 adb 授权」方案，不硬上。

### 阶段 1：抽 ShellOps（保证 Shizuku 不回退）

- 新建 `ClearScreenShellOps.kt`：把 `ClearScreenPrivilegedService.kt` 里的 settings 读/写、`pm list/inspect`、`change`、`scanChannels` 解析搬进来，抽象为 `exec(args: List<String>): Result(code, output)`。
- `ClearScreenPrivilegedService` 只保留 AIDL 参数转发，AIDL 契约不变。
- 真机用 Shizuku 跑一遍 `inspect/change/scan/applyUiCleanupDirect`，与改动前逐字段比对。

### 阶段 2：ADB 客户端（先原生、后 UI）

- `ClearScreenAdbKeys.kt`：RSA 2048 + 自签名 X.509（BouncyCastle builder，不全局注册 provider），PEM 存 `filesDir/adb/`，目录 0700、文件 0600。
- `ClearScreenAdbClient.kt`：继承 `AbsAdbConnectionManager`；`setApi(Build.VERSION.SDK_INT)`、`setTimeout(15s)`、`setThrowOnUnauthorised(true)`；串行化 shell 执行（Mutex）；断线自动重连一次；输出上限 20MB；不做端口扫描。
- `ClearScreenAdbOperations.kt`：只接受枚举化操作；包名只能来自 `ClearScreenCleanupRules.targets()`/`ClearScreenUiCleanup.targets()`，设置键只能来自 `ClearScreenDirectSettings` 白名单，值只允许备份里读到的原值与目标值；命令用固定 argv 拼接并做字符校验。
- 状态判定（每次真实探测，不靠内存布尔）：`unsupported`(<11) / `not_paired`（无密钥）/ `paired_offline`（连不上）/ `auth_rejected`（TLS 或授权被撤销）/ `connected`。`connected` 需执行固定 `id` 并校验 `uid=2000(shell)`。
- `ClearScreenCleanupController` 增加 `withShell`：ADB 已连通优先，否则 Shizuku，两者都没有就抛出明确中文原因；`applyUiCleanupDirect` 改走 `withShell`。
- `ClearScreenNativeModule` 新增：`getAdbAccessStatus`、`pairAdb`、`connectAdb`、`autoConnectAdb`、`disconnectAdb`、`openDeveloperOptions`、`getAdbLocalInfo`；`CleanupAccess` 增加 `adbMode` 等字段。
- `src/native/ClearScreenNative.ts` 补类型与方法。

### 阶段 3：UI 结构（先结构后接线）

- `App.tsx`：`Page` 增加 `systemRecommend`、`adbAccess`；同步 `settingsChild` 判定与渲染分支，返回行为保持一致。
- 新建 `src/screens/SystemRecommendScreen.tsx`：`TopBar compact` + 返回 + 可滚动卡片；每行只有标题与开关，无灰色副标题；仅在受阻时就地显示正常字号状态行（如「无障碍未开启，20 项无法操作」+ 去开启）；开关状态来自原数据源并读回真实值。
- `SystemCleanupScreen.tsx`：删除整块开关 UI 与说明段，替换为一行入口「系统推荐开关」；删除灰色小字（`广告与隐私·未处理` 等副标题、`358 个应用`、`更多清理` 副标题、路径小字）；路径改为正常字号；保留营销通知风险提示。
- 新建 `src/screens/AdbAccessScreen.tsx`：状态行、打开无线调试、配对端口/配对码输入与配对按钮、电脑兜底命令（可选中复制）+ 手动 host/port 连接、断开/忘记授权。未授权、配对失败、端口失效都给正常字号原因与下一步。
- `MoreCleanupScreen.tsx`：Shizuku 卡片改为「授权通道」入口；删除灰色说明与包名副标题；保留渠道风险提示；检查结果用正常字号状态行。

### 阶段 4：vivo 组件清单

- 保留 `com.vivo.hybrid`（快应用服务）。
- 增加 `com.vivo.magazine`（锁屏杂志）到「系统广告组件」，但真机验证 suspend/unsuspend 后锁屏与 SystemUI 正常；异常则撤掉。
- 不加 `com.vivo.abe`：行为/权限决策引擎，停用会影响权限、自启动、省电，风险大于收益。

### 阶段 5：验证与记录

- `npx tsc --noEmit`。
- `cd android && .\gradlew.bat :app:assembleDebug`（首次联网解析依赖）。
- 装机：`adb install -r`；逐页截图（系统广告页、推荐开关子页、授权通道页）。
- 需用户手动配合：开无线调试、读/输配对码、点系统授权弹窗、重启验证降级、电脑执行 `adb tcpip 5555`。
- 按 AGENTS.md 在 `docs/wiki/CHANGELOG.md` 顶部追加记录，写明实际验证与「未验证」项；不 commit / push。

## 主要改动文件

- 原生：`ClearScreenShellOps.kt`（新）、`ClearScreenAdbKeys.kt`（新）、`ClearScreenAdbClient.kt`（新）、`ClearScreenAdbOperations.kt`（新）、`ClearScreenPrivilegedService.kt`、`ClearScreenCleanupController.kt`、`ClearScreenNativeModule.kt`、`ClearScreenCleanupRules.kt`、`android/app/build.gradle`、`AndroidManifest.xml`（仅在前台/网络权限缺失时补）。
- JS：`src/native/ClearScreenNative.ts`、`App.tsx`、`src/screens/SystemRecommendScreen.tsx`（新）、`src/screens/AdbAccessScreen.tsx`（新）、`SystemCleanupScreen.tsx`、`MoreCleanupScreen.tsx`。
- 文档：`docs/wiki/CHANGELOG.md`。

## 风险与已知限制

- 无线配对必须用户手动开无线调试并输入 6 位配对码；重启后无线调试默认关闭，需要重新打开（密钥一般还在，端口会变）。
- Android 16 的本地网络限制可能影响 mDNS；因此主路径用显式 host/port，mDNS 只做辅助。
- 电脑兜底必须有一台电脑执行 `adb tcpip 5555`，且 App 首次连接时用户要在手机上点「允许调试」。
- 16KB page size 是二进制级风险，阶段 0 不通过就不继续。
- 锁屏杂志 suspend 有厂商依赖风险，真机验证不通过就撤掉。
- 已停用的组件在卸载 App 前必须先恢复；沿用现有提示。

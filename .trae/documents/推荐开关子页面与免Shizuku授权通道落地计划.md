# 推荐开关子页面 + 免 Shizuku 授权通道 落地计划

## 一、结论（先看这段）

1. **能直接改的只有 3 个**：vivo 的「个性化广告 / 个性化推荐 / 智能推荐服务」，它们存在 `Settings.Secure`（`vivo_recommended_status`、`vivo_personalized_recommend_status`、`vivo_smart_recommend_service_status`），普通 App 即使申请 `WRITE_SETTINGS` 也写不进去，必须走 shell 身份（uid 2000）。所以「开关直连系统设置、不走无障碍」= **必须先有授权通道**（Shizuku 或自建 ADB），这是 Android 的硬限制，不是实现偷懒。
2. **另外 20 个开关改不了**：负一屏、浏览器、应用商店的开关存在厂商 App 自己的私有数据里，第三方应用没有任何官方接口可写。这批继续走「打开对应页面 + 无障碍代点」，这是唯一可行路径，会在界面上如实标注。
3. **自建 ADB 通道可行**：用 `libadb-android`（JitPack、Apache-2.0、AOSP libadb 的 JNI 封装）做 Android 11+ 无线调试配对；配对后 shell 通道与 Shizuku 用户服务同为 uid 2000，命令可以完全复用。电脑侧 `adb tcpip 5555` + `adb connect` 作为兜底。
4. **这仍是一次性授权**：重启后连接失效需重连（部分机型需重新配对）；无线调试关闭时需要重新打开。界面上会明确显示状态，不静默失败。
5. **不新增危险权限**：`INTERNET` 已存在，不需要 `WRITE_SETTINGS`、不需要新运行时权限；风险控制在「只暴露枚举化的最小命令集」。

---

## 二、现状（调用链）

### 2.1 页面与入口

- 路由是 `App.tsx` 里的 `Page` 状态机 + `history` 栈：[App.tsx](file:///c:/develop/ClearScreen/App.tsx#L26) 定义 `Page`，[App.tsx](file:///c:/develop/ClearScreen/App.tsx#L283-L284) 的 `go/back`，[App.tsx](file:///c:/develop/ClearScreen/App.tsx#L286-L296) 的 settings 子页返回键处理（判定条件在 [App.tsx](file:///c:/develop/ClearScreen/App.tsx#L288)）。
- 设置页入口：[SettingsScreen.tsx](file:///c:/develop/ClearScreen/src/screens/SettingsScreen.tsx#L36) `row('系统广告与营销通知', onSystemCleanup, true)`。
- 渲染分支：[App.tsx](file:///c:/develop/ClearScreen/App.tsx#L438-L439) 的 `systemCleanup` / `moreCleanup`。
- 当前「系统推荐开关」整块在 [SystemCleanupScreen.tsx](file:///c:/develop/ClearScreen/src/screens/SystemCleanupScreen.tsx#L212-L261) 里，和「按应用查看通知」「更多清理」入口堆在同一页；开关默认折叠，展开后每行是 `Toggle compact`。

### 2.2 开关数据来源

- 目标清单是原生写死的 vivo 白名单：[ClearScreenUiCleanup.kt](file:///c:/develop/ClearScreen/android/app/src/main/java/com/clearscreen/prototype/backend/ClearScreenUiCleanup.kt#L16-L43)（23 项，按 `system/news/store` 三组）。
- 其中 3 项带 `settingKey`（`ClearScreenDirectSettings.spec()` 能取到）→ 标记 `direct=true`；其余 20 项无设置键。
- 列表与状态回读：[ClearScreenNativeModule.kt](file:///c:/develop/ClearScreen/android/app/src/main/java/com/clearscreen/prototype/backend/ClearScreenNativeModule.kt#L341-L361) `getUiCleanupTargets()`。direct 项用 `ClearScreenDirectSettings.readLocal()` **本地直接回读真实值**（读设置不需要权限），其余项读净屏上次确认的状态。
- 直写：[ClearScreenNativeModule.kt](file:///c:/develop/ClearScreen/android/app/src/main/java/com/clearscreen/prototype/backend/ClearScreenNativeModule.kt#L370-L400) `applyUiCleanupDirect()`，现在走 `cleanupController.withPrivilegedService` → Shizuku 用户服务 → `readSetting` / `writeSetting`（`content insert` 写入 + 回读校验，因为 `settings put` 在 OriginOS 16 会被静默回写）。写入前把原值存 `system_cleanup_backups`，写入后存 `system_cleanup_states`。
- 辅助流程（非 direct）：[SystemCleanupScreen.tsx](file:///c:/develop/ClearScreen/src/screens/SystemCleanupScreen.tsx#L149-L182) → `ClearScreenNative.startUiCleanup(id, restore)` → `ClearScreenUiCleanup` 起一次单目标会话，由无障碍服务在指定页面内匹配控件、点一次、回读确认。
- 授权与后端：[ClearScreenCleanupController.kt](file:///c:/develop/ClearScreen/android/app/src/main/java/com/clearscreen/prototype/backend/ClearScreenCleanupController.kt#L35-L39) `accessStatus()`（只报 Shizuku `running/granted`）、[L86-L121](file:///c:/develop/ClearScreen/android/app/src/main/java/com/clearscreen/prototype/backend/ClearScreenCleanupController.kt#L86-L121) `withService`（每次重新 bind Shizuku 用户服务，15 秒超时、最多 3 次）。
- Shizuku 服务实现：[ClearScreenPrivilegedService.kt](file:///c:/develop/ClearScreen/android/app/src/main/java/com/clearscreen/prototype/backend/ClearScreenPrivilegedService.kt) —— 固定 argv 执行 `/system/bin/settings`、`/system/bin/content`、`/system/bin/pm suspend|unsuspend`、`/system/bin/dumpsys`；键名/取值有正则白名单；包名只来自 `ClearScreenCleanupRules` 白名单。

### 2.3 现状的问题

1. 没有 Shizuku 就完全不能直写，`shizukuHint()` 只能把人引去装 Shizuku。
2. 整组 23 个开关挤在主页面折叠区里，展开后很长，且带灰色副标题（`页面名 · 状态 · 方式`）。
3. `MoreCleanupScreen` 的授权卡只认 Shizuku，文案与入口也绑定 Shizuku。

---

## 三、能力边界（明确写清楚，不承诺做不到的）

| 目标 | 能否由第三方改 | 依据 / 代价 |
| --- | --- | --- |
| 3 个 vivo 推荐键（Secure 表） | **能**，但需要 uid 2000（Shizuku 或 ADB） | 普通应用写需要 `WRITE_SECURE_SETTINGS`（系统签名/特权）。只有 shell 通道可行 |
| 另外 20 个厂商开关 | **不能** | 数据在 `com.vivo.hiboard` / `com.vivo.browser` / `com.bbk.appstore` 私有存储，无公开接口，只能无障碍代点 |
| 亮度 / 屏幕超时 / 自动旋转 | 本次不涉及 | `WRITE_SETTINGS` 可改部分全局项；本次需求没有这类开关，不添加无效权限 |
| 快应用服务开关（`switch_quick_app_service`） | **能**（同上，Shell/Secure 表） | 已有实现，继续复用 |
| 停用/冻结厂商组件（`pm suspend`） | **能**（Shell 身份） | 只对白名单包名；不新增 `com.vivo.abe`（行为决策引擎，风险高） |
| 零授权直接改系统设置/卸载他人 App | **做不到** | 不给这种承诺 |

---

## 四、方案与分阶段改动

### 阶段 0：依赖闸门（最高风险，先做）

**文件**：[android/app/build.gradle](file:///c:/develop/ClearScreen/android/app/build.gradle#L211-L218)

```groovy
implementation("com.github.MuntashirAkon:libadb-android:3.1.1")
implementation("org.conscrypt:conscrypt-android:2.5.3")
```

- 不加 `abiFilters`（保留 AAR 自带 ABI，模拟器 x86_64 也要能装）。
- 不加 `usesCleartextTraffic`、不加新权限（ADB 走原始 TCP，`INTERNET` 已够）。
- **闸门验证**：`cd android; .\gradlew.bat :app:assembleDebug --console=plain`（首次需联网拉 JitPack）；成功后检查 APK 内 `lib/*/libadb*.so` 是否存在，并用 `zipalign -c -P 16 -v 4 <apk>` 检查 16KB 对齐；装机确认 App 启动不崩（native 库能加载）。
- **失败处理**：JitPack 解析失败 / native 加载失败 / 16KB 对齐失败 → 停止，不硬上；回退为「仅保留 Shizuku + 电脑 adb 说明页」，并如实汇报原因。

### 阶段 1：抽出统一 Shell 层（避免两套后端行为分叉）

**新文件**：`android/app/src/main/java/com/clearscreen/prototype/backend/ClearScreenShellOps.kt`

```kotlin
internal interface ClearScreenShellRunner {
  fun userId(): Int
  fun run(vararg args: String): ClearScreenShellOps.Result   // code + output
  fun openStream(args: String): ClearScreenShellOps.Stream   // 供 ADB 长输出/流式读取
}
internal object ClearScreenShellOps {
  data class Result(val code: Int, val output: String)
  fun readSetting(runner, namespace, key): String
  fun writeSetting(runner, namespace, key, value): String     // content insert + 回读校验
  fun state(runner, target, user): String
  fun changeTarget(runner, target, expected, desired): String // pm suspend/unsuspend 或 secure 键
  fun scanChannels(runner): String                            // 只保留渠道元数据
  fun inspectTargets(runner): String
}
```

- 把 [ClearScreenPrivilegedService.kt](file:///c:/develop/ClearScreen/android/app/src/main/java/com/clearscreen/prototype/backend/ClearScreenPrivilegedService.kt#L25-L219) 里的 `readSetting/writeSetting/state/changeTarget/scanChannels/inspectTargets` + 正则白名单整体迁入，服务只剩 AIDL 转发 + `userId()` 基于 `Binder.getCallingUid()`。
- 回归要求：Shizuku 路径行为与现在**完全一致**（同样的报错文案、同样的回读校验、同样的备份顺序）。
- 验证：`:app:compileDebugKotlin`；有 Shizuku 的机器跑 `inspect / change / scan / applyUiCleanupDirect`。

### 阶段 2：ADB 授权通道（密钥 + 客户端 + 操作层）

**新文件 1**：`ClearScreenAdbKeys.kt`
- RSA 2048 + 自签名 X.509（BouncyCastle builder，**不**全局注册 provider，避免和平台 BC 冲突）。
- 私钥/证书存 `filesDir/adb/adbkey`、`filesDir/adb/adbkey.pub`，目录 0700、文件 0600；已存在则复用。
- `allowBackup=false` 已满足「密钥不随备份外流」。
- 任何日志都不输出配对码、私钥、证书内容。

**新文件 2**：`ClearScreenAdbClient.kt`
- 依赖 libadb：`pair(host, port, code)`、`connect(host, port)`（A_STLS 自动升级 TLS）、`autoConnect(context, port)`（mDNS，辅助）、`openStream("shell:<argv>")`、`disconnect()`、`close()`。
- 状态机：`unsupported`（API < 30 无线调试配对不可用）/ `no_key` / `not_connected` / `pairing` / `connected_unverified` / `connected` / `unauthorized` / `error`。
- **只有执行 `id` 返回含 `uid=2000(shell)` 才置 `connected`**，避免「连上了但没授权」被当成可用。
- `setThrowOnUnauthorised(true)`；`setTimeout` 限制在 10–15 秒；配对/连接都放在单独单线程 executor，不阻塞 UI 线程。
- 断线：每次 `run()` 前检查 `isConnected()`，失败一次即降级为 `not_connected` 并回传可读原因（无线调试关闭 / 授权被撤销 / 端口变化）。

**新文件 3**：`ClearScreenAdbOperations.kt`
- 组合 Keys + Client + `ClearScreenShellRunner`：`run(vararg args)` 用 `openStream("shell:...")` 执行，参数逐个来自枚举，不接受 JS 传入的任意命令。
- 命令集**只允许**：`id`、`/system/bin/settings get`、`/system/bin/content insert`、`/system/bin/pm list packages -s`、`/system/bin/pm suspend|unsuspend`、`/system/bin/dumpsys package`、`/system/bin/dumpsys notification --noredact`。
- 所有可变参数（namespace/key/value/user/pkg/targetId）在进入前经过与 Shizuku 服务相同的正则/白名单校验（`KEY_PATTERN`、`VALUE_PATTERN`、`ClearScreenCleanupRules`、`ClearScreenUiCleanup.targets()`）。

**改动**：[ClearScreenCleanupController.kt](file:///c:/develop/ClearScreen/android/app/src/main/java/com/clearscreen/prototype/backend/ClearScreenCleanupController.kt)

```kotlin
fun <T> withShell(block: (ClearScreenShellRunner) -> T): T =
  if (adb.isVerifiedConnected()) block(adb.runner()) else withService { service -> block(serviceRunner(service)) }

fun shellAccessStatus(): WritableMap  // { running, granted, adbState, adbConnected, adbSupported }
```
- `accessStatus()` 保留原 `running/granted`（不破坏旧调用），额外加 `adbState`、`adbConnected`、`adbSupported`。
- `inspectTargets / changeTarget / scanChannels` 改为走 `withShell`，ADB 优先，ADB 不可用自动回退 Shizuku；两者都不可用时报「请先建立授权通道」。
- `applyUiCleanupDirect` 的 `withPrivilegedService` 改为 `withShell`。

**改动**：[ClearScreenNativeModule.kt](file:///c:/develop/ClearScreen/android/app/src/main/java/com/clearscreen/prototype/backend/ClearScreenNativeModule.kt)

新增 `@ReactMethod`：
- `getAdbAccessStatus(promise)` → `{supported, state, connected, paired, host, port, message}`
- `pairAdb(host, port, code, promise)`（host 仅 IPv4 字面量、port 1–65535、code 6 位数字）
- `connectAdb(host, port, promise)`
- `autoConnectAdb(promise)`
- `disconnectAdb(promise)`
- `openDeveloperOptions(promise)`（走现有 `ClearScreenSettingsNavigator`）
- `getAdbManualInfo(promise)` → 本机 adb 可用的端口提示与电脑兜底命令（不返回密钥）

**改动**：[src/native/ClearScreenNative.ts](file:///c:/develop/ClearScreen/src/native/ClearScreenNative.ts#L68-L76)
- `CleanupAccess` 增加可选 `adbState`、`adbConnected`、`adbSupported`。
- 新增 `AdbAccessStatus` 类型与对应方法。
- 新增错误前缀约定：`ADB_UNSUPPORTED` / `ADB_NOT_PAIRED` / `ADB_UNAUTHORIZED` / `ADB_OFFLINE`，TS 侧只做文案映射，不做命令拼接。

### 阶段 3：界面（先定结构，再接线）

**3.1 路由**（[App.tsx](file:///c:/develop/ClearScreen/App.tsx#L26)、[L288](file:///c:/develop/ClearScreen/App.tsx#L288)、[L438-L439](file:///c:/develop/ClearScreen/App.tsx#L438-L439)）
- `Page` 增加 `'systemRecommend' | 'adbAccess'`。
- `settingsChild` 判定加入这两个页面（它们从 `systemCleanup`/`moreCleanup` 进入，历史里已有 `settings`，返回键沿用现有逻辑）。
- 新增渲染分支：`<SystemRecommendScreen onBack={back} onOpenAccess={...}/>`、`<AdbAccessScreen onBack={back}/>`。

**3.2 新页面 `src/screens/SystemRecommendScreen.tsx`**
- `TopBar compact title="系统推荐开关" onBack`，`ScrollView` 可滚动。
- 数据：进入时、`AppState` 变 `active` 时、每次操作后调 `getUiCleanupTargets()`，**不回存本地副本**；开关值直接来自该接口。
- 分组三张 `Card compact`：`系统设置` / `资讯与推荐` / `应用商店`（由 `category` 映射），行内只有标题 + `Toggle compact`；状态行只在必要时机出现。
- 点击 direct 项 → `applyUiCleanupDirect(id, 'off'|'on')`；失败且错误含授权 → 弹「需要授权通道」带「去授权」（跳 `adbAccess`）。
- 点击非 direct 项 → 沿用 `startUiCleanup` + 确认弹窗；无障碍未开时卡片顶部显示正常字号状态行 + 「去开启」按钮（不点进去才发现不能用）。
- 恢复路径：每个开关再次打开时，按已保存的原值写回（direct）或按原状态恢复（assisted），与现有语义一致。

**3.3 改 `SystemCleanupScreen.tsx`**
- 删除整块开关列表（[L212-L261](file:///c:/develop/ClearScreen/src/screens/SystemCleanupScreen.tsx#L212-L261)），替换为一行入口：标题「系统推荐开关」，点击 `go('systemRecommend')`。
- 删除灰色小字：卡内说明段、「手动去设置」的路径副标题、「更多清理」的副标题。
- 路径文本（`paths.system/news/store`）改为正常字号 `C.text`，不再是灰色小字。
- 保留并改成正常字号状态行：无障碍未开启提示（带「去开启」）、通知关闭风险提示。
- `shizukuHint` 改为「授权通道」措辞，不再只提 Shizuku。

**3.4 新页面 `src/screens/AdbAccessScreen.tsx`**
- `TopBar compact title="授权通道"`，`ScrollView`。
- 状态行（正常字号）：`未开启无线调试` / `已配对，未连接` / `连接中…` / `已连接（可清理）` / `授权被拒绝` / `此设备不支持`。
- 操作：`开启无线调试`（跳开发者选项/无线调试页）、两个输入框（`配对端口`、`配对码`，占位即用途）、`配对`、`连接端口`（默认填无线调试页显示的端口）、`连接`、`断开连接`。
- 电脑兜底：一段等宽命令（正常字号）`adb pair <IP:配对端口> <配对码>` / `adb connect <IP:端口>`；以及 `adb tcpip 5555` + `adb connect` 的 USB 兜底说明，作为必要信息用正常字号。
- 必要提示（正常字号）：`重启后需要重新连接`、`关闭无线调试会断开`；不写灰色说明小字。

**3.5 改 `MoreCleanupScreen.tsx`**
- 「系统开关授权（Shizuku）」卡替换为一行入口「授权通道」→ `go('adbAccess')`，状态行显示当前通道（`未连接` / `Shizuku 已连接` / `ADB 已连接`）。
- 删除「仅列出当前品牌中可识别的项目；实际支持情况随系统版本变化。」灰色小字。
- 通知渠道的 app 名从灰色副标题改为同一行显示（`渠道名（应用名）`）。
- 「尚未检查设备」「没有匹配的受支持项目」保留为正常字号状态行（这是当前状态，不是说明小字）。
- 营销通知风险提示保留（正常字号）。

### 阶段 4：系统广告组件增项（可回退）

**文件**：[ClearScreenCleanupRules.kt](file:///c:/develop/ClearScreen/android/app/src/main/java/com/clearscreen/prototype/backend/ClearScreenCleanupRules.kt#L13-L14)
- vivo 增加 `pkg("com.vivo.magazine", "锁屏杂志", "ads")`，与 `quick:vivo_service` 并列。
- **不加** `com.vivo.abe`（权限/行为决策引擎，风险高）。
- 装机验证：`inspect` 显示该包 → `changeTarget` 关闭 → 锁屏、SystemUI、桌面正常 → 再恢复。若锁屏异常或包不存在，立即移除该行并在日志中写明「未通过真机验证已撤下」。

### 阶段 5：验证与记录

- `npx tsc --noEmit`
- `cd android; .\gradlew.bat :app:assembleDebug --console=plain`（首次联网；之后可 `--offline`）
- 装机（vivo V2502DA）：进 `系统推荐开关` 子页 → 返回 → 再进，开关状态一致；3 个 direct 项在 ADB 已连接时可直写并回读；非 direct 项在无障碍开启时可走辅助流程。
- ADB 流程实测：开无线调试 → 输配对码 → 配对 → 连接 → 状态变「已连接」→ 直写 3 个键 → 拔掉/关无线调试 → 状态回「已断开」→ 重连成功。
- 未验证项必须写明（如需用户手动开无线调试、重启后重新配对的步骤）。
- 按 AGENTS.md 在 [docs/wiki/CHANGELOG.md](file:///c:/develop/ClearScreen/docs/wiki/CHANGELOG.md) 顶部追加 `YYYY-MM-DD HH:mm:ss +08:00` 记录：目的、文件、构建结果、真机结果、未验证项。
- 同步检查 `README.md` / `docs/wiki/README.md` 中关于 Shizuku 的说明，改为「授权通道（ADB 或 Shizuku）」。
- **不 git commit / push**。

---

## 五、界面文案与视觉规则（本轮统一执行）

- 删除所有灰色说明小字/副标题/提示小字（`C.text2` + `caption` 的说明性文本）。
- 必须保留的必要信息（权限未授权、功能不可用原因、危险操作后果、重启需重连、命令示例）→ 正常字号（`COMPACT_UI.body`，`C.text`），就地显示，不另开教程。
- 行高：开关行统一 `minHeight: 56`、`paddingVertical: 8`；分组间距用现有 `sectionTitle` + `Card.compact`（`marginBottom: 12`），删掉小字后不再额外补空行。
- 只用现有组件：`Background / TopBar compact / Card compact / Toggle compact / Chevron / AppDialog`；不引入第二套样式或导航。

---

## 六、已定取舍（执行时不再另行选择）

1. **不做全局「一键恢复全部系统开关」**：只保留逐项恢复（再次打开开关即按保存的原值恢复）。避免误改用户没在净屏里动过的系统项；`设置 → 恢复默认` 仍只清净屏自己的数据，不触碰系统设置。
2. **ADB 主路径用显式 IP + 端口**，mDNS `autoConnect` 只作为「同一 WiFi 下尝试」的辅助按钮，不作为唯一路径（Android 16 本地网络限制有风险）。
3. **不加 `WRITE_SETTINGS`**：对 3 个 Secure 键无效，加了只是假承诺。
4. **不加 ABI 过滤**：保留模拟器可测；APK 体积增大可接受。
5. **不加根权限方案**：仅文档说明，不实现。
6. **版本号本轮不动**：只出 debug 包验证，正式发版时再递增 `versionCode/versionName`。

---

## 七、风险与回退

| 风险 | 处理 |
| --- | --- |
| JitPack 拉不到 / native 加载失败 | 阶段 0 闸门停止，回退「Shizuku + 电脑 adb 说明页」，如实汇报 |
| vivo 对 `content insert` 静默回写 | 沿用现有「写入后回读校验」，失败报「系统没有接受这次操作」，不谎报成功 |
| ADB 配对在部分 ROM 上端口/授权弹窗差异 | 状态机把 `unauthorized` 单列，提示「请在手机上允许调试」；提供 mDNS 与电脑兜底 |
| `shell:` 拼接注入 | 只允许枚举操作 + 固定 argv，参数走既有正则/白名单，JS 永远拿不到 `exec(String)` |
| 无障碍服务未开时非 direct 项不可用 | 子页顶部正常字号状态行 + 「去开启」，不静默失败 |
| 关闭锁屏杂志影响锁屏 | 真机验证不过即撤下该条白名单项 |

---

## 八、涉及文件清单

**新增**
- `android/app/src/main/java/.../backend/ClearScreenShellOps.kt`
- `android/app/src/main/java/.../backend/ClearScreenAdbKeys.kt`
- `android/app/src/main/java/.../backend/ClearScreenAdbClient.kt`
- `android/app/src/main/java/.../backend/ClearScreenAdbOperations.kt`
- `src/screens/SystemRecommendScreen.tsx`
- `src/screens/AdbAccessScreen.tsx`

**修改**
- `App.tsx`、`src/screens/SystemCleanupScreen.tsx`、`src/screens/MoreCleanupScreen.tsx`
- `src/native/ClearScreenNative.ts`
- `ClearScreenPrivilegedService.kt`、`ClearScreenCleanupController.kt`、`ClearScreenNativeModule.kt`、`ClearScreenCleanupRules.kt`
- `android/app/build.gradle`
- `README.md`、`docs/wiki/README.md`、`docs/wiki/CHANGELOG.md`

---

## 九、落地结论（真机实测，2026-11-06）

1. **系统配对弹窗不能跨应用存活**：`com.android.settings` 的「使用配对码配对设备」弹窗一旦离开前台就关闭，配对端口随即变为 `ECONNREFUSED`。因此配对码只能由无障碍服务在弹窗仍在前台时读取（`adbPairingScan`，60/300/900ms 三次），读取后应用退到后台完成配对。计划里“用户抄写配对码”的设想不成立，已按此实现。
2. **adbd 不保证发送 CLSE**：命令输出会全部到达，但流不关闭，`read()` 无限 `wait()`。实测表现为“连上后每条命令都 20s 超时”。最终方案是把 `; echo; echo __CS_EXIT:$?` 拼在命令尾部，用哨兵同时承担“输出结束”和“退出码”，读到即 `break` 并主动 `stream.close()`。
3. **libadb 的两个状态陷阱**：`connect()` 在旧 socket 仍报 `isConnected()` 时直接返回 false（重连前必须先 `disconnect()`）；`close()` 会 `destroy()` 缓存的私钥对象（断开只能关 socket，不能关 manager）。
4. **状态判定规则修正**：同一包在 `dumpsys package` 里可能有多条记录且 `suspended` 值不一致，改为“任一记录 `suspended=true` 即视为已关闭”。
5. 验证结论：Android 14 / HyperOS 上配对、连接、读取、`pm suspend` 与 `pm unsuspend` 全部走通，PC 侧 `dumpsys` 复核无残留。电脑 `adb tcpip 5555` 兜底分支仍未实测。

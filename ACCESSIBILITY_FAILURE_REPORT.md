# ClearScreen 无障碍授权返回后自动关闭问题报告

日期：2026-09-21

## 一、用户看到的现象

1. ClearScreen 自己的权限页面显示“自动跳过权限：已开启”。
2. 进入系统“无障碍”设置后，手动打开“净屏”。
3. 返回系统无障碍列表或返回 ClearScreen 后，“净屏”又显示“已关闭”。
4. 同一台 vivo 手机上，李跳跳 2.2 不通过 vivo 互传也能保持开启。

## 二、测试环境

- 手机：vivo V2502DA
- 系统：Android 16 / OriginOS 16
- ClearScreen 测试版本：1.0.14，versionCode 20
- targetSdk：33
- 安装方式：USB ADB 安装
- ClearScreen 包名：`com.clearscreen.prototype`
- 无障碍服务：`com.clearscreen.prototype/.backend.ClearScreenAccessibilityService`

## 三、已经确认的事实

### 1. ClearScreen 的无障碍服务确实被系统识别

系统服务查询可以找到：

```text
com.clearscreen.prototype/.backend.ClearScreenAccessibilityService
```

因此不是“没有声明服务”或“系统完全找不到服务”。

### 2. 当前 App 之前存在状态误报

旧代码用的是：

```text
系统记录里有权限 OR 服务已连接 = 显示已开启
```

这会造成 App 自己显示绿色“已开启”，但系统实际没有完成授权。

1.0.14 已改为只有以下两项同时成立才显示“已开启”：

- 系统保存了无障碍授权；
- 服务真实连接并正在运行。

这能修复“App 自己说成功”的误报，但不能替系统授予权限。

### 3. 服务本身不是完全无法运行

此前通过 USB 做过一次可回退的系统绑定测试。系统直接写入授权后，ClearScreen 服务出现过：

```text
onServiceConnected
```

并且在系统的 Enabled services、Bound services 中都能看到净屏，没有出现 Crashed services。

这证明 Kotlin 服务代码不是一启动就必崩，服务也不是完全不兼容 Android 16。

### 4. 1.0.14 已经改成接近李跳跳的简单服务结构，但仍然失败

1.0.14 已经做了以下调整：

- 去掉独立的 `:accessibility` 进程；
- 去掉无障碍服务上的前台特殊服务声明；
- 去掉 `specialUse` 和相关前台通知；
- 改为普通单进程 AccessibilityService；
- 服务断开时立即把运行状态标记为关闭。

但是用户实际测试后，系统仍然没有保留授权。

因此，“只是独立进程或前台服务导致的问题”目前已经不能作为唯一根因。

### 5. 这次失败发生在服务连接之前

用户测试 1.0.14 后，系统状态为：

```text
enabled_accessibility_services=
com.vivo.dr/com.vivo.dr.LocateBehaviorAnalysisService
```

ClearScreen 不在系统的 Enabled services 和 Bound services 中。

同时没有看到 ClearScreen 的：

```text
ClearScreenA11y: onServiceConnected
```

这说明系统没有真正把 ClearScreen 授权给无障碍管理器，服务甚至没有进入正常连接阶段。

## 四、与李跳跳的实际差异

已从手机上读取到李跳跳的信息：

- 包名：`hello.litiaotiao.app`
- 服务：`.LttService`
- targetSdk：33
- 服务使用普通声明；没有 ClearScreen 原来那套独立进程和前台特殊服务配置。

但“李跳跳没有通过 vivo 互传安装”并不能直接证明它和 ClearScreen 的安装来源完全相同。它可能来自 QQ、浏览器下载、系统安装器或其他被 vivo 视为不同来源的安装链路。

所以目前有两个需要区分的方向：

1. ClearScreen 的包信息、签名、安装来源或 Android 16/vivo 的受限设置策略导致系统不接受授权；
2. ClearScreen 的无障碍配置文件、包权限或系统识别信息仍然与李跳跳存在差异。

目前证据还不足以只锁定其中一个。

## 五、当前最可能的根因范围

这不是 ClearScreen 页面按钮颜色的问题，也不是普通的后台运行问题。核心现象是：

> vivo 系统设置页暂时允许用户点击开关，但在退出页面时没有把 ClearScreen 写入最终的无障碍授权列表，或者写入后马上被系统安全策略移除。

可能原因按优先级排列：

1. vivo/Android 16 对 USB ADB 侧载应用的无障碍授权限制；
2. ClearScreen 的签名、安装来源或包元数据与李跳跳不同；
3. AccessibilityService XML 仍有与李跳跳不一致、被 vivo 特殊处理的配置；
4. vivo 自带的无障碍/清理组件在授权确认后移除了 ClearScreen。

目前没有证据表明是 ClearScreen 服务代码启动后崩溃，因为本次失败时没有 `onServiceConnected`，也没有 ClearScreen 崩溃记录。

## 六、建议交给后续 AI 的继续排查方案

### A. 做一个最小探针 APK

只保留一个最简单的 AccessibilityService：

- 不使用 React Native；
- 不使用 VPN；
- 不使用前台服务；
- 不读取广告规则；
- 只在 `onServiceConnected` 写一条日志。

如果最小探针也会被 vivo 返回后关闭，问题基本可以确定在安装来源、签名或 vivo 系统策略，而不是 ClearScreen 业务代码。

### B. 用与李跳跳完全相同的安装链路测试

重点不是“是不是互传”，而是安装器和系统记录是否相同。需要比较：

- installerPackageName；
- initiatingPackageName；
- packageSource；
- 签名证书摘要；
- 是否出现“允许受限设置”；
- 是否点击了系统二次确认框中的“允许”。

### C. 抓取一次完整的系统日志

在用户点击 ClearScreen 开关之前清空日志，然后按以下顺序操作：

1. 点击净屏；
2. 点击系统确认框中的“允许”；
3. 返回无障碍列表；
4. 返回 ClearScreen。

需要重点查找：

- `AccessibilityManagerService`；
- `PackageManager`；
- `com.vivo.accessibility`；
- `com.vivo.permissionmanager`；
- `single-cleaner`；
- `restricted settings`；
- `ClearScreenA11y`。

只有这一步日志能明确说明是“权限被拒绝”“服务绑定失败”还是“vivo 清理移除”。

## 七、当前代码状态

- 已保留真实状态判断，避免 App 显示假成功；
- 已完成 1.0.14 Release 编译；
- 1.0.14 已安装到手机；
- 用户已复现返回后仍关闭；
- 本轮实验代码目前不应被当作最终修复版本发布；
- 在根因进一步确认前，不建议继续反复修改广告识别规则或 UI 提示。

## 八、给非技术人员的结论

现在不是“你没有点对”，也不是净屏页面单纯显示错了。系统确实没有把这个 App 的无障碍权限最终保存下来。

李跳跳能用，说明 vivo 手机并不是完全禁止这类软件。下一步必须把李跳跳和净屏的“安装身份”和“无障碍服务登记信息”逐项对比，不能再只依靠猜测互传、后台或前台服务。


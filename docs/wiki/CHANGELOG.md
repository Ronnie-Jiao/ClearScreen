# 净屏修改记录

本文件按时间倒序记录项目修改，供后续 AI 和开发者开始任务前快速了解最近变化。自本记录建立起，新增条目的时间使用中国标准时间（UTC+08:00）；既有条目的时间按文件系统记录的本地最后修改时间整理。

## 2026-11-08

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

## 2026-11-07

### 2026-11-07 22:55:07 +08:00 — 增加 OnePlus、Motorola 与 Android 通用广告隐私适配

- 系统推荐开关新增 OnePlus“限制广告跟踪”和 Motorola“停用广告个性化”目标，按已勾选即关闭个性化广告的反向语义识别；新增 Android 13 及以上、系统版 Google Play 服务可用时显示的广告主题、应用建议广告、广告衡量三个独立目标，并声明其 Google Play 服务包可见性。
- 辅助识别支持逐项精确中英文标签与页面别名，页面和控件未匹配时仍不点击；操作确认路径补充 OnePlus、Motorola 和 Android 广告隐私入口。
- 参考官方资料：[Android 广告隐私](https://support.google.com/android/answer/13720755?hl=zh-Hans)、[OnePlus 用户手册](https://service.oneplus.com/content/dam/support/user-manuals/common/OnePlus_Nord_2_User_Manual_EN.pdf)、[Motorola Android 14 用户手册](https://en-us.support.motorola.com/app/answers/detail/a_id/180146)。Android 三个隐私项不覆盖所有广告；适配是否出现取决于系统版本、Google 服务及设备实际页面。
- 验证：`npx tsc --noEmit`、`:app:compileDebugKotlin --offline --console=plain` 成功；`git diff --check` 通过（有既有 LF/CRLF 提示）。未运行自动化测试，未打包、未安装、未做真机验收。
- 涉及文件：`android/app/src/main/java/com/clearscreen/prototype/backend/ClearScreenUiCleanup.kt`、`ClearScreenCleanupController.kt`、`ClearScreenNativeModule.kt`、`android/app/src/main/AndroidManifest.xml`、`src/screens/SystemRecommendScreen.tsx`、`README.md`、`docs/wiki/README.md`、本修改记录。
- 参考：当前工作区修改，未提交。

### 2026-11-07 22:15:13 +08:00 — 同步新增品牌开关到 README 与 Wiki

- README 和 Wiki 更新为当前登记的 vivo/iQOO、小米/Redmi/POCO、华为、荣耀、OPPO/realme 适配范围；说明华为与 OPPO 反向开关语义、未适配品牌手动路径，以及新增项目尚未真机验收。
- 验证：文档与当前源码及本轮官方厂商资料核对；未额外运行检查。
- 涉及文件：`README.md`、`docs/wiki/README.md`、本修改记录。

### 2026-11-07 22:13:04 +08:00 — 扩展华为、荣耀和 OPPO 的广告跟踪开关

- 在既有小米开关基础上新增华为、荣耀、OPPO/realme 的逐项目标；新增开关语义映射，识别“限制广告跟踪/追踪”这类勾选后表示已停用个性化广告的反向控件，确保关闭与恢复仍按“功能开启/关闭”判断，并保留原状态恢复和单次点击保护。
- 更新品牌路径：小米同时列出 HyperOS 与 MIUI 官方路径；华为、荣耀、OPPO 显示厂商对应入口。其他尚未登记的品牌继续提供手动入口，不模拟不存在的统一开关。
- 验证：`npx tsc --noEmit`、`:app:compileDebugKotlin --offline --console=plain`、`git diff --check` 均通过；未打包、未安装、未做真机验收。
- 涉及文件：`android/app/src/main/java/com/clearscreen/prototype/backend/ClearScreenUiCleanup.kt`、`src/screens/SystemRecommendScreen.tsx`、本修改记录。

### 2026-11-07 22:08:47 +08:00 — 更新 README 与 Wiki 的推荐开关支持说明

- README 和 Wiki 现区分 vivo/iQOO 原有项目、小米/Redmi/POCO 新增的系统个性化广告项目，以及其他品牌仍提供系统设置入口和手动路径的状态；补充小米官方路径来源和未完成真机验收的限制。
- 验证：文档内容已与当前实现核对；未额外运行检查。
- 涉及文件：`README.md`、`docs/wiki/README.md`、本修改记录。

### 2026-11-07 22:03:50 +08:00 — 增加小米/Redmi/POCO 的系统个性化广告开关入口

- 原因：小米设备此前没有收录到 `ClearScreenUiCleanup.targets()`，因此“直接改写与代点”显示空状态。新增小米系统目标“个性化广告推荐”，仅在系统设置包的“广告服务”页准确识别该开关后，沿用既有的两次稳定读取、保存原状态、最多点击一次并回读确认流程；确认提示和手动路径使用小米官方路径。打开入口回到系统设置首页，避免使用未经验证的厂商私有深链。
- 其他未适配品牌仍显示对应手动入口和厂商路径，不显示虚构的可自动操作开关；小米具体机型/系统版本的无障碍节点和实际点击仍需真机验收。
- 验证：`npx tsc --noEmit`、`:app:compileDebugKotlin --offline --console=plain`、`git diff --check` 均通过；未打包、未安装、未做真机验收。
- 涉及文件：`android/app/src/main/java/com/clearscreen/prototype/backend/ClearScreenUiCleanup.kt`、`android/app/src/main/java/com/clearscreen/prototype/backend/ClearScreenSettingsNavigator.kt`、`src/screens/SystemRecommendScreen.tsx`、本修改记录。

### 2026-11-07 21:56:59 +08:00 — 精简系统营销通知页面提示

- 按截图删除“按应用查看通知”分组标题、展开后用途提醒，以及未输入搜索词时的“输入应用名称查找”空状态。保留“查看应用”折叠行、搜索框、搜索结果与无匹配提示。
- 验证：`git diff --check` 与 `npx tsc --noEmit`；未执行打包或功能/UI验收。
- 涉及文件：`src/screens/SystemCleanupScreen.tsx`、本修改记录。

## 2026-11-06

### 2026-11-06 19:59:35 +08:00 — 新增自建授权通道（无线调试配对 + 应用内 ADB 客户端），清理功能不再依赖 Shizuku

- 目标：用户已拍板“自建类似 Shizuku 的授权通道”。实现为 Android 11 及以上的无线调试配对：净屏自己充当 ADB 客户端，拿到与 `adb shell` 同级的权限（uid 2000），仍属一次用户主动授权，不是零授权。
- 语义层：`ClearScreenShellOps` / `ClearScreenShellRunner` 保留为唯一命令入口，Shizuku 与 ADB 各实现一个 runner；ADB 侧新增 `ClearScreenAdbOperations`、`ClearScreenAdbClient`、`ClearScreenAdbKeys`、`ClearScreenAdbPairingHelper`。只暴露语义方法（读写设置、检查/改写目标、扫描渠道），不向界面或脚本提供任意命令执行接口，参数按白名单校验。
- 密钥与配对：RSA 2048 自签证书由 `bcprov/bcpkix-jdk15to18:1.81` 生成，私钥与证书存于私有目录 `filesDir/adb`；配对码与私钥不写日志。系统配对弹窗离开前台即被系统关闭（实测），所以配对码由净屏的无障碍服务在弹窗仍在前台时读取（60/300/900ms 三次扫描），随后应用退到后台完成配对，用户不必抄写配对码。
- 实测修复四处缺陷：①`AbsAdbConnectionManager.connect()` 在旧 socket 仍报告已连接时直接返回 false，导致“连接失败”假象——连接前先断开旧 socket；②`close()` 会销毁缓存的私钥对象，导致断线后无法再认证——断开只关 socket，保留密钥与 manager；③adbd 命令结束后不一定发送 CLSE，等 EOF 会永久挂起——把 `__CS_EXIT:$?` 同时当作结束标记与退出码，读到即结束并主动关流；④`dumpsys package` 对同一包可能返回多条记录（前后 `suspended=true`/`false` 混杂），旧的“必须恰好一条”规则会误报“无法确认”——改为任一记录 `suspended=true` 即视为已关闭。
- 界面：`SystemRecommendScreen` 增加「授权通道」入口；`AdbAccessScreen` 提供连接状态、自动配对、连接端口直连、电脑 adb 兜底与安全说明；`MoreCleanupScreen` 增加授权通道状态入口。按文案约束删除纯说明副标题（“系统推荐、资讯推荐、应用商店推广”“快应用服务、营销通知渠道”“仅列出当前品牌中可识别的项目。”“无线调试配对，或电脑 adb 拉起”），保留断线重连原因、危险操作后果与安全边界告知（正常字号）。
- 验证：`:app:assembleRelease` 成功；`adb install -r` 返回 `Success`。Redmi Note 12 5G（Android 14 / HyperOS）：无线调试配对通过，连接成功，`dumpsys package` 读取、`pm suspend` → `pm unsuspend` 往返均成功，PC 侧 `dumpsys` 复核全部记录 `suspended=false`（测试期改动已还原）。设备地址、端口及配对过程截图未纳入版本库。
- **未验证**：电脑侧 `adb tcpip 5555` 兜底分支未实测，只按 USB/无线调试两条路径验证；手机重启、关闭无线调试或换网络后的恢复依赖用户手动重连；Android 11–13 与其他品牌未验证。覆盖安装会清空系统里的无障碍启用项（本次重装后读回 `null`，已用 `settings put secure` 恢复），用户升级后需重新开启无障碍。
- 涉及文件：`android/app/src/main/java/com/clearscreen/prototype/backend/`（ClearScreenAdbOperations.kt、ClearScreenAdbClient.kt、ClearScreenAdbKeys.kt、ClearScreenAdbPairingHelper.kt、ClearScreenShellOps.kt、ClearScreenNativeModule.kt、ClearScreenAccessibilityService.kt）、`src/native/ClearScreenNative.ts`、`src/screens/AdbAccessScreen.tsx`、`src/screens/MoreCleanupScreen.tsx`、`src/screens/SystemCleanupScreen.tsx`、`src/screens/SystemRecommendScreen.tsx`、`android/app/build.gradle`、`docs/wiki/README.md`、`README.md`、本修改记录。

### 2026-11-06 12:32:03 +08:00 — 修掉推荐开关的死路：提示改为可直接跳转开启无障碍，并修正 system 类开关的启动落点

- 用户在 vivo 真机点「个性化广告」旁的开关，弹出“无法启动这个操作／请先开启净屏无障碍服务”后没有下一步，只能自己去找系统设置。核对手机状态确认根因：`enabled_accessibility_services` 里只有 `com.linkpilot.android/.RemoteAccessibilityService`，净屏的无障碍服务不在启用列表（该状态在覆盖安装前即存在，非本次改动造成）。尝试用 `adb shell settings put secure enabled_accessibility_settings` 代为开启，命令返回 0 但回读值不变，OriginOS 静默拒绝 shell 写入该键；随后手机从 Windows USB 总线整体消失，adb 与系统设置均无法再操作设备。
- 应用侧改为可自行走出死路：`src/screens/SystemCleanupScreen.tsx` 读取无障碍运行状态（`ClearScreenNative.getAccessibilityHealth`），未开启时在“系统推荐开关”卡内显示“净屏的无障碍服务未开启，开启后才能操作这些开关”与「去开启无障碍服务」按钮（直达系统无障碍设置页）；点击开关时若服务未运行，不再只提示“请先开启”，而是弹出带「去开启」按钮的确认框；启动操作报错信息含“无障碍”时同样给出该按钮。
- 修正 system 类开关的启动落点：`ClearScreenNativeModule.startUiCleanup` 原先对 system 分类调用 `openSystemPrivacySettings()`，在 OriginOS 16 上落到的是「隐私」页（使用无障碍功能、权限管理器等），并不含个性化广告／个性化推荐／智能推荐服务三个开关，这三项辅助操作必然匹配失败；改用新增的 `openSystemAdPrivacySettings()`（vivo 直达「广告与隐私」页，其它品牌沿用通用隐私设置页，行为不变）。
- 验证：`npx tsc --noEmit` 通过；`:app:assembleRelease --offline --console=plain` 成功（13 秒增量，含新增的原生入口改动）；APK 元数据为 `com.clearscreen.prototype`、`1.0.76` / `82`、应用名“净屏”，apksigner v2 验签通过，正式证书 SHA-256 保持 `93c410454a8263736ff60bb6e27153b19f74f4c934f444531842f5162ccfa999`，`outputs/apk/release/app-release.apk` 与 `outputs/apk/净屏.apk` 字节一致，大小 106,544,953 字节，SHA-256 `F353A52DE80B8D3A7269B9AD761BDE5C3EF3D43FD7CAA0D0061DFE82BA4CDB09`。
- **未安装、未做真机验收**：设备在打包期间从 USB 总线断开，无法覆盖安装，也无法逐项验证 23 个开关与手机系统开关的实际联动；无障碍服务仍需用户在系统设置里手动开启。应用名、包名与签名未变。
- 涉及文件：`src/screens/SystemCleanupScreen.tsx`、`android/app/src/main/java/com/clearscreen/prototype/backend/ClearScreenNativeModule.kt`、`app.json`、`android/app/build.gradle`、`docs/wiki/README.md`、本修改记录、正式 APK。

### 2026-11-06 12:22:23 +08:00 — 打包并覆盖安装净屏 1.0.75

- 把当日三项界面改动（系统广告页收敛为两块加一个入口、Shizuku 文案改普通用户用语、系统推荐开关改为真开关）打成 Android 正式 Release，版本递增至 `1.0.75` / versionCode `81`；应用名“净屏”、包名 `com.clearscreen.prototype` 与正式签名保持不变。`app.json` 与 `android/app/build.gradle` 的版本号已同步。
- 验证：`:app:assembleRelease --offline --console=plain` 成功（47 秒，JS bundle 重新生成）；`npx tsc --noEmit` 通过。APK 元数据为包名 `com.clearscreen.prototype`、`1.0.75` / `81`、应用名“净屏”；apksigner v2 验签通过，正式证书 SHA-256 保持 `93c410454a8263736ff60bb6e27153b19f74f4c934f444531842f5162ccfa999`；Gradle 产物 `android/app/build/outputs/apk/release/app-release.apk` 与命名产物 `android/app/build/outputs/apk/净屏.apk` 字节一致，大小 106,543,957 字节，SHA-256 `20E8CECBE0C711549EE57E8B3717CAC1D326EAF36F04B30CA62C4BBAB95BD71B`。
- 安装：通过 USB `adb install -r --no-streaming` 覆盖安装到 vivo V2502DA（`10AG4M2KGF00527`）返回 `Success`，读回 `1.0.75` / `81`，首次安装时间保持 `2026-09-20 15:33:13`，未卸载、未清除数据。Xiaomi 22101317C 本次未连接。
- 状态提示：安装前后 vivo 上净屏的无障碍服务都不在系统启用列表（`enabled_accessibility_services` 只有 `com.linkpilot.android/.RemoteAccessibilityService`），该状态不是本次覆盖安装造成的；需要用户在系统设置里重新开启净屏无障碍服务，否则广告跳过与推荐开关辅助操作都不会工作。
- 未做功能/UI 验收：开关与手机系统开关的实际联动、页面折叠后的观感仍待用户真机确认；未运行自动化测试。
- 涉及文件：`app.json`、`android/app/build.gradle`、`README.md`、`docs/wiki/README.md`、本修改记录、正式 APK。

### 2026-11-06 12:15:54 +08:00 — 系统推荐开关改为真开关，翻动即关闭或恢复手机上的广告开关

- 用户要求把「系统推荐开关」列表里的“关掉”文字按钮改成开关形式，开关直接控制手机上的广告开关。复核原生辅助流程后确认它已具备所需能力：按目标状态判断是否需要点击（关闭期望为关、恢复期望为开），目标已是期望状态就不点击、不会把已关好的开关重新打开，最多点击一次并回读确认，关闭前先保存原值以便恢复。因此开关直接复用该流程，不新增任何系统写入手段，也不需要 Shizuku。
- 界面：`src/screens/SystemCleanupScreen.tsx` 的系统推荐开关列表每项改为 `Toggle`（复用 `src/components/UI.tsx` 既有开关组件，非原生 Switch），副标题改为“页面名 · 状态”，状态取已关闭／仍开着／未处理；点击开关先弹确认框说明将进入的系统页面，再启动辅助关闭或按原状态恢复；没有可恢复记录时打开操作会被拒绝并提示到系统设置自行打开；操作进行中该行不再重复触发，返回应用后重新读取列表与结果再刷新一次状态。
- 原生：新增本机存储 `system_cleanup_states` 记录净屏**已确认**的开关状态，与“保存的原值（可恢复记录）”分开存放；`ClearScreenUiCleanup` 在回读确认关闭时写入 `off`、确认恢复时写入 `on` 并同时清除恢复记录，未确认的失败路径不写入；`getUiCleanupTargets` 新增 `state` 字段（`on`/`off`/`unknown`），`src/native/ClearScreenNative.ts` 的 `UiCleanupTarget` 同步增加该字段。
- 语义：开关打开＝净屏已确认该系统开关处于关闭状态；灰色＝未处理或仍开着。此前只记录“原值为开”的恢复记录，点击失败时无法区分是否真的关掉，这次用独立的状态字段修正了界面显示可能与系统实际不符的问题。
- 与并行改动衔接：同期另一窗口新增了 `openSystemAdPrivacySettings`（vivo 直达“广告与隐私”页），应用内「广告与隐私」行与「打开系统设置」按钮已改用该入口；本次改动沿用同一入口，未回退或覆盖对方改动，1.0.74 已安装到 vivo V2502DA 的事实也不受本次影响。
- 验证：`npx tsc --noEmit` 通过（exit 0）；`cd android; .\gradlew.bat :app:compileDebugKotlin --offline --console=plain` 成功（20 秒，仅既有弃用警告）；`git diff --check` 通过（仅既有换行符提示）。未打包、未安装、未做真机 UI 验收；开关与手机系统开关的实际联动需真机确认。
- 涉及文件：`src/screens/SystemCleanupScreen.tsx`、`src/native/ClearScreenNative.ts`、`android/app/src/main/java/com/clearscreen/prototype/backend/ClearScreenStore.kt`、`ClearScreenUiCleanup.kt`、`ClearScreenNativeModule.kt`、`README.md`、`docs/wiki/README.md`、本修改记录。

### 2026-11-06 12:08:10 +08:00 — 修正系统广告页跳转落点，vivo 直达“广告与隐私”页，打包安装净屏 1.0.74

- 用户反馈“系统广告与营销通知”页的跳转指示不对（截图标出「设置 → 安全 → 更多安全设置 → 广告与隐私」一行，并附上点击后实际到达的「隐私」页截图：使用无障碍功能、权限管理器、位置数据共享方面的更新、显示密码、锁屏通知、在锁屏状态下显示媒体）。
- 真机核对 vivo V2502DA（Android 16 / OriginOS 16）：文案路径本身正确——设置首页确有“安全”入口，“安全”页底部确有“更多安全设置”，该页确有“广告与隐私”条目，点击后是标题为“广告与隐私”的页面，含个性化广告、个性化推荐、智能推荐服务三项开关。问题在跳转落点：原实现调用 `Settings.ACTION_PRIVACY_SETTINGS`，在 OriginOS 16 上解析到的正是截图那一页，不含个性化广告相关开关，文案与落点不一致。
- 修正：新增原生入口 `ClearScreenSettingsNavigator.openSystemAdPrivacySettings()` 与 React 侧 `openSystemAdPrivacySettings`。vivo 优先用 `android.settings.SETTINGS.SUB_SETTINGS` + `extra=fragment:com.vivo.settings.securityandprivacy.adprivacy.AdAndPrivacyFragmentForOsTwo`（vivo 把该 action 路由到 `com.vivo.settings.VivoSubSettings`，Fragment 名需在系统 Fragment 注册表内才生效），其次回退 `com.android.settings.MORE_SECURITY_PRIVACY_SETTINGS`（该页仍列有“广告与隐私”入口），再回退通用隐私设置页；其它品牌沿用原通用入口。action、extra 与 Fragment 类名属互操作候选而非稳定 Android API，仅在 vivo V2502DA 实测通过，系统版本变化时需重新核对；候选按顺序回退，无法识别时不会落到空白页。应用内“广告与隐私”行与“打开系统设置”按钮改用新入口；文案路径经真机逐层核对无误，未改动。系统设置辅助操作的厂商开关流程不依赖跳转 Intent，仍在页面内按控件匹配操作，未改动。
- 验证：`:app:compileDebugKotlin --offline --console=plain` 与 `:app:assembleRelease --offline --console=plain` 均成功；`npx tsc --noEmit` 通过。APK 元数据为包名 `com.clearscreen.prototype`、`1.0.74` / `80`、应用名“净屏”；`apksigner verify` 的 v2 签名通过，证书 SHA-256 仍为 `93c410454a8263736ff60bb6e27153b19f74f4c934f444531842f5162ccfa999`；APK `android/app/build/outputs/apk/release/app-release.apk` SHA-256 `6D1F19994C3D353E180B170748ACF86CED5E286F0E462A9193D1A0E9E4636565`，大小 106,543,477 字节。通过 USB `adb install -r --no-streaming` 覆盖安装到 vivo V2502DA 返回 `Success`，读回 1.0.74 / 80，首次安装时间保持 2026-09-20 15:33:13。端到端实测：应用内点「广告与隐私」后前台变为 `com.android.settings/com.vivo.settings.VivoSubSettings`，页面标题为“广告与隐私”，含个性化广告、个性化推荐、智能推荐服务三项开关，与文案一致。净屏无障碍服务状态本次未核验（vivo 覆盖安装后曾把该服务移出系统启用列表）；Xiaomi 22101317C 本次未连接。
- 涉及文件：`android/app/src/main/java/com/clearscreen/prototype/backend/ClearScreenSettingsNavigator.kt`、`android/app/src/main/java/com/clearscreen/prototype/backend/ClearScreenNativeModule.kt`、`src/native/ClearScreenNative.ts`、`App.tsx`、`android/app/build.gradle`、`app.json`、`README.md`、`docs/wiki/README.md`、本修改记录、正式 APK。

### 2026-11-06 11:57:06 +08:00 — 启动加载提速：应用清单并行解析，首屏约 3.4 秒降到约 0.9 秒

- 用户反馈刚打开净屏时启动页停留太久，并附启动页截图。复核源码并加临时计时埋点实测：启动页要等 `getSnapshot()` 返回才切页，其中应用清单占绝大部分——冷启动 4274ms（包查询 79ms、应用名称 1466ms、应用图标 2691ms、逐应用规则 9ms），热启动 2169ms；权限读取仅 18–48ms、处理记录仅 2–3ms；JavaScript 侧另有一次固定 500ms 延时。根因有两条：每个可启动应用的名称被取了两次（排序一次、填充一次），且 357 个应用的名称与图标资源串行读取、串行编码。
- 修正：`ClearScreenNativeModule.installedApps()` 把每个应用的应用名称与启动图标改为在固定大小线程池（`min(6, max(2, CPU 核数))`）中并行解析，每个应用的名称只读取一次；图标仍在内存中编码为 data URI，不落盘、不新增任何本地持久化数据，排序、逐应用规则、隐私口径全部不变。模块 `invalidate()` 时关闭该线程池。`App.tsx` 去掉切页前那次固定 500ms 延时，快照返回即进入首页。
- 验证：加临时计时埋点实测，改动后冷启动应用清单 730ms、整份快照 745ms，热启动应用清单 626ms、整份快照 641ms（`ActivityTaskManager` 显示 Activity +211ms，RN 启动后约 0.7 秒完成快照即切页，启动页停留由约 3.4–4.3 秒降到约 0.9 秒）；临时埋点已在正式包中移除。`npx tsc --noEmit` 通过；`:app:assembleRelease --offline --console=plain` 成功（32 秒）；APK `android/app/build/outputs/apk/净屏.apk` 与 `release/app-release.apk` 哈希一致，SHA-256 `34F2D469CF063F0EEA8A2945E0F28D07386ADA6F531C1AFF51EC5953ED386DED`，大小 106,542,237 字节；aapt 元数据为 `1.0.73` / `79`、包名 `com.clearscreen.prototype`、应用名“净屏”；`apksigner verify` 返回 0，正式证书 SHA-256 保持 `93c410454a8263736ff60bb6e27153b19f74f4c934f444531842f5162ccfa999`。通过 USB `adb install -r --no-streaming` 覆盖安装到 vivo V2502DA（`10AG4M2KGF00527`）返回 `Success`，读回 `1.0.73` / `79`，首次安装时间保持 `2026-09-20 15:33:13`。启动实测：约 1.2 秒已进入首页，首页最近记录与“自定义关闭窗口”的应用图标、名称渲染正常（357 个应用清单快照无异常）。未卸载、清除数据；Xiaomi 22101317C 本次未连接，未更新。
- 版本说明：`1.0.72` / versionCode `78` 已被同期另一窗口占用并安装到 vivo，本次顺延为 `1.0.73` / `79`。
- 遗留事项：vivo 上净屏无障碍服务当前不在系统启用列表（同期另一窗口 11:36 覆盖安装后即掉出，`settings put` 写入被系统回写覆盖），需用户在手机上手动重新开启；本次改动不涉及无障碍授权逻辑，但该状态下广告自动跳过不会执行。
- 涉及文件：`android/app/src/main/java/com/clearscreen/prototype/backend/ClearScreenNativeModule.kt`、`App.tsx`、`android/app/build.gradle`、`app.json`、`README.md`、`docs/wiki/README.md`、本修改记录、正式 APK。

### 2026-11-06 11:56:25 +08:00 — 系统广告页收到两块加一个入口，Shizuku 能力移入“更多清理”页

- 用户追问“这里有必要搞这么多设置吗”。核对源码后确认三处重复或低收益：自动辅助已覆盖广告与隐私、全局搜索与负一屏、应用商店同一批页面同一批开关，三条手动路径只是让用户自己点，只在自动匹配失败时有兜底价值；Shizuku 在 vivo 上只额外提供快应用服务开关和营销通知渠道候选列表，却要求用户安装第三方 App 并授予 shell 权限。按用户确认的方案（Shizuku 移出该页另设入口、手动路径收进“系统推荐开关”卡内作兜底）执行。
- 主页面 `src/screens/SystemCleanupScreen.tsx` 现在只有两块加一个入口：①vivo/iQOO 的“系统推荐开关”卡（23 个厂商开关逐项辅助关闭），卡内新增“手动去设置”折叠行，展开后是广告与隐私、资讯与推荐、应用商店推广三条可跳转的手动路径；无自动辅助的品牌显示“系统广告设置”卡（当前设备、厂商路径、“打开系统设置”）加同样折叠的手动路径；②“按应用查看通知”仍默认折叠为一行标题；③底部“更多清理”入口卡进入独立页面。vivo 上原先的“当前设备”卡不再单独占位，其“广告与隐私”路径收进兜底行。
- 新增 `src/screens/MoreCleanupScreen.tsx`，承载原 `SystemCleanupExtras.tsx` 的 Shizuku 内容：系统开关授权（状态未连接／等待授权／已连接，按钮“去连接”“重新连接”，启动与授权说明在连接弹窗）、营销通知扫描、快应用与系统广告组件检查。原 `SystemCleanupExtras.tsx` 的内容拆分后删除，厂商自动辅助逻辑与厂商路径改由 `SystemCleanupScreen.tsx` 承担，`cleanupPaths` 随之迁到该文件。`App.tsx` 新增 `moreCleanup` 页面分支、返回栈与 Android 返回键处理，主页面通过 `onOpenMoreCleanup` 进入。
- 行为不变：授权流程、状态备份与回读、逐项人工确认、隐私口径、跳转目标、所用原生接口全部保持原样，未新增权限或后台行为；“帮助与反馈”的常见问题改为“更多清理里的 Shizuku 是什么？”并指向新入口路径。
- 验证：`npx tsc --noEmit` 通过（exit 0）；`cd android; .\gradlew.bat :app:compileDebugKotlin --offline --console=plain` 成功（20 秒，仅 `MainApplication.kt` 既有弃用警告）；`git diff --check` 通过（仅既有换行符提示）。未打包、未安装、未做真机 UI 验收，新入口与折叠后的实际观感待用户验收。
- 涉及文件：`src/screens/SystemCleanupScreen.tsx`、`src/screens/MoreCleanupScreen.tsx`（新增）、`src/screens/SystemCleanupExtras.tsx`（删除）、`src/screens/SettingsDetailScreen.tsx`、`App.tsx`、`README.md`、`docs/wiki/README.md`、本修改记录。

### 2026-11-06 11:46:18 +08:00 — 系统广告与营销通知页收敛为四块，减少平铺卡片

- 用户反馈该页“要搞的设置也太多了”，要求简化并减少非必要项。按用户确认的方案（折叠收敛、Shizuku 进阶能力保留但折进「更多（可选）」、按应用列表默认折叠），页面由 8 张平铺卡片收敛为四块，功能一项未删。
- `src/screens/SystemCleanupScreen.tsx`：「系统广告设置」卡新增折叠行「更多设置位置」，展开后显示「资讯与推荐」「应用商店推广」两条手动路径，整行可直接跳转对应系统页面，取消原先两个独立分区；「按应用查看通知」默认折叠为一行标题（副标题显示可管理应用数），展开后才显示操作提醒、搜索框和结果列表；新增 `cleanupPaths` 取用品牌路径。
- `src/screens/SystemCleanupExtras.tsx`：删除独立的 Shizuku 卡和「营销通知」「快应用与系统广告组件」两个分区，合并为「更多（可选）」一张卡——折叠时只显示「系统开关授权（Shizuku）」和连接状态（未连接／等待授权／已连接），展开后依次是「去连接」按钮、营销通知扫描、快应用与系统广告组件检查。组件不再接收 `manufacturer`，改由 `SystemCleanupScreen` 统一取用品牌路径；随之移除未再使用的 `systemAdPath` 导出与 `cardTitle` 样式。
- 折叠只改变展示层级：授权流程、状态备份与回读、逐项人工确认、隐私口径和跳转目标全部保持原样，未新增任何权限或后台行为。界面文案仍遵守 Wiki 的界面文案约束。
- 验证：`npx tsc --noEmit` 通过（exit 0）；`cd android; .\gradlew.bat :app:compileDebugKotlin --offline --console=plain` 成功（23 秒，仅 `MainApplication.kt` 既有弃用警告）；`git diff --check` 通过（仅既有换行符提示）。未打包、未安装、未做真机 UI 验收，折叠后的实际观感待用户验收。同期另一窗口把应用列表改为搜索后显示，本次改动保留了该搜索逻辑，两处改动合并在同一工作区。
- 涉及文件：`src/screens/SystemCleanupScreen.tsx`、`src/screens/SystemCleanupExtras.tsx`、`README.md`、`docs/wiki/README.md`、本修改记录。

### 2026-11-06 11:41:46 +08:00 — 按应用查看通知改为搜索后才显示应用列表

- 用户在真机截图上标出「系统广告与营销通知」页的「按应用查看通知」区块：该分区把全部可启动应用（真机约 357 个）按字母序无差别罗列，与"系统广告与营销通知"这一主题无关的条目占绝大多数，同时把「营销通知」渠道扫描结果和「快应用与系统广告组件」检查结果压在长列表下方。复核源码确认该列表是唯一不依赖 Shizuku 就能跳到单个应用系统通知设置的入口（同页「营销通知」渠道扫描须先连 Shizuku 才能列出候选），因此不整块删除，经用户确认改为保留能力、默认不罗列、输入搜索后才显示结果。
- 改动：`src/screens/SystemCleanupScreen.tsx` 中搜索词先 `trim` 再转小写（`keyword`），列表仅在有搜索词时渲染；无搜索词时显示空状态"输入应用名称查找"，有搜索词但无匹配时显示"未找到应用"；匹配结果最多显示 50 条，超过时追加"共 N 个应用，已显示前 50 个"。跳转系统通知设置的调用链（`ClearScreenNative.openAppNotificationSettings` → `ClearScreenSettingsNavigator.openAppNotificationSettings`）、"只列出可从桌面打开的应用"的筛选条件和搜索框交互均未改变。卡片说明删去"仅显示可从桌面打开的应用"这一行为说明，按 Wiki 界面文案约束只保留"关闭前请检查用途，保留订单、物流、安全和验证码提醒。"这条操作提醒。
- 并行改动衔接：本次修改期间另一窗口正在改写同一页面（新增「查看应用」折叠入口与「更多设置位置」、把资讯与商店路径上移、把 Shizuku 与营销通知等收进「更多（可选）」）。对方在 11:43 读到的文件已含本次搜索逻辑并在其改写中予以保留，最终文件为两处改动合并后的状态，本次搜索规则未被覆盖；未回退或覆盖对方的任何改动。
- 验证：`npx tsc --noEmit` 通过（exit 0）；`git diff --check` 通过（仅既有换行符提示）。未打包、未安装、未做真机 UI 验收，搜索与列表的实际观感待用户验收。`targets/harmony-bare-pilot` 未引用该页面，共享 UI 复用边界不变。工作区同时有其他任务的改动，本次只改该页面的列表渲染与说明文案，未清理或覆盖其他改动。
- 涉及文件：`src/screens/SystemCleanupScreen.tsx`、`README.md`、`docs/wiki/README.md`、本修改记录。

### 2026-11-06 11:36:25 +08:00 — 打包并覆盖安装净屏 1.0.72

- 上一窗口 11:21 打包的 1.0.71 不含本次"系统推荐开关"文案改动（用户的改文案指令在 11:21:07 发出，该 APK 在 11:21:10 写出），因此版本递增至 `1.0.72` / versionCode `78`；应用名"净屏"、包名 `com.clearscreen.prototype`、minSdk 24 / targetSdk 36 与正式签名保持不变。工作区同时含其他窗口的底部导航安全区与 Shizuku 卡片文案改动，本次一并打入正式包。
- 验证：`:app:assembleRelease --offline --console=plain` 成功（58 秒；Metro 因缓存为空重建 JS bundle，694 模块）；APK `android/app/build/outputs/apk/净屏.apk` 与 `release/app-release.apk` 哈希一致，SHA-256 `71BE2B0676941531B26FED2C9A4423E393405DDD7CDEFD356BF0260231C3D4CB`，大小 106,540,337 字节；aapt 元数据为 `1.0.72` / `78`、包名 `com.clearscreen.prototype`、应用名"净屏"；`apksigner verify` 返回 0，正式证书 SHA-256 保持 `93c410454a8263736ff60bb6e27153b19f74f4c934f444531842f5162ccfa999`。
- 安装：前两次 `adb install -r --no-streaming` 返回 `INSTALL_FAILED_ABORTED: User rejected permissions`（vivo 安装确认弹窗未确认即超时自动拒绝）；监测 `dumpsys window` 确认 `com.android.packageinstaller.PackageInterceptActivity` 弹窗出现，用户在手机端确认后再次发起返回 `Success`。设备读回 `1.0.72` / `78`，`lastUpdateTime` 为 2026-11-06 11:33:25，首次安装时间保持 `2026-09-20 15:33:13`；未卸载、未清除数据。Xiaomi 22101317C 本次未连接，未更新。
- 无障碍授权：覆盖安装后 vivo 将净屏无障碍服务移出系统启用列表（`enabled_accessibility_services` 仅剩 `com.linkpilot.android/.RemoteAccessibilityService`）。用 `settings put secure enabled_accessibility_services` 写入并切换 `accessibility_enabled` 重载管理器，均被系统回写覆盖；直接 `am start -a android.settings.ACCESSIBILITY_DETAILS_SETTINGS` 被 `OPEN_ACCESSIBILITY_DETAILS_SETTINGS` 权限拒绝。已把 `android.settings.ACCESSIBILITY_SETTINGS` 列表页推送到设备，需用户在手机上手动开启净屏无障碍服务；截至本记录该服务仍未启用。期间切换 `accessibility_enabled` 导致联控远程操作短暂解绑，随后已恢复重新绑定。
- 真机 UI 验收：启动应用进入“系统广告与营销通知”页，展开“查看开关”列表，确认分区标题显示“系统推荐开关”、说明显示“净屏可以帮你关掉手机自带系统里的推荐开关。不同手机版本里这些开关的位置可能略有不同。”、列表项右侧显示“关掉”；点击“个性化广告”弹窗显示“关掉「个性化广告」？”，正文为“请在手机上打开设置 → 安全 → 更多安全设置 → 广告与隐私。净屏会自动把它关掉；没找到这个开关就不会改动。”。按 UTF-16 检索打包用 Hermes bundle：新文案“系统推荐开关”“查看开关”存在，旧文案“辅助关闭”“查看项目”“收起项目”已不存在；APK 的 `classes.dex` 中原生提示“这台手机没有这个开关”“没能确认是否已关成功”“已关掉并确认”“这个开关已经是现在这样了”均存在。验收用弹窗以“取消”关闭，未触发任何辅助操作，系统设置未被改动。
- 涉及文件：`android/app/build.gradle`、`app.json`、`ACCESSIBILITY_PERSISTENCE_REPORT.md`、`README.md`、`docs/wiki/README.md`、本修改记录、正式 APK。

### 2026-11-06 11:25:55 +08:00 — 底部导航避让系统手势导航区

- 用户反馈一级底部导航“这一栏有点太顶下面的边了”，并附真机截图（应用页，底部“首页/应用/设置”文字压在系统手势条上）。复核源码确认：底部导航用 `position:absolute; bottom:0` 固定在屏幕最底部，没有预留系统导航栏（手势区）高度；该机（vivo V2502DA，Android 16 / SDK 36）导航栏 inset 为 63px（约 18dp），系统手势条直接画在导航文字下方，文字与手势条重叠。
- 修正：`ScreenInsetsContext` 增加 `bottom` 安全区值，由 Android/iOS 入口通过 `react-native-safe-area-context` 提供（共享页面仍不直接导入原生安全区域库，HarmonyOS 隔离试验的纯 UI 导入边界不变）；底部导航高度由固定 70dp 改为 70dp + 底部 inset，图标和文字整体上移，导航栏白色底板仍延伸到屏幕底部，背景图延伸到屏幕底部的规则不变，导航图标、字号和卡片内部尺寸不变。首页、应用列表、设置三个一级页面的滚动区按新导航栏高度同步预留空间，末条内容不会被导航栏遮挡。
- 验证：`npx tsc --noEmit` 通过（主工程）；`targets/harmony-bare-pilot` 的 `npx tsc --noEmit` 通过，共享 UI 复用边界未被破坏；`:app:assembleRelease --offline --console=plain` 成功（1 分 15 秒）；APK `android/app/build/outputs/apk/净屏.apk` 与 `release/app-release.apk` 哈希一致，SHA-256 `2D6F099B86A4B911AD674026C504E98AB619D327D96751BD3DDAAEA07CC55E0E`，大小 106,539,881 字节；aapt 元数据为 `1.0.71` / `77`、包名 `com.clearscreen.prototype`、应用名“净屏”；apksigner 验签通过，正式证书 SHA-256 保持 `93c410454a8263736ff60bb6e27153b19f74f4c934f444531842f5162ccfa999`。通过 USB `adb install -r --no-streaming` 覆盖安装到 vivo V2502DA（`10AG4M2KGF00527`）返回 `Success`，读回 `1.0.71` / `77`，首次安装时间保持 `2026-09-20 15:33:13`，净屏无障碍服务仍在启用列表。启动应用实测：首页和应用页的导航图标与文字整体上移，与系统手势条之间留出明显空隙，手势条落在导航栏底部预留区内；应用列表滚动正常，未见内容被导航栏遮挡（未逐项滚到 357 个应用的末行核对）。未卸载、清除数据；Xiaomi 22101317C 本次未连接，未更新。工作区同时有其他任务的改动，本次只改底部导航安全区相关文件，未清理或覆盖其他改动。
- 涉及文件：`src/components/ScreenInsets.ts`、`src/components/BottomNav.tsx`、`src/screens/AppsScreen.tsx`、`src/screens/HomeScreen.tsx`、`src/screens/SettingsScreen.tsx`、`App.tsx`、`android/app/build.gradle`、`app.json`、`README.md`、`docs/wiki/README.md`、本修改记录、正式 APK。

### 2026-11-06 11:24:46 +08:00 — Shizuku 卡片改为普通用户可读的表述

- 用户反馈“系统广告与营销通知”页的 Shizuku 卡片只显示技术名词，看不出这是什么、能干什么。按用户确认的改法，只改标题、状态和按钮，完整说明放进弹窗和“帮助与反馈”的常见问题，设置页不新增说明小字，继续遵守 Wiki 的界面文案约束。
- 卡片标题由“Shizuku”改为“系统开关授权（Shizuku）”；状态由“未启动 / 已启动，等待授权 / 已连接并授权”改为“未连接 / 等待授权 / 已连接”；按钮由“连接 Shizuku”改为“去连接”，已连接时仍为“重新连接”。状态判定、授权流程与可执行能力不变。
- 弹窗改为普通用户用语：未启动时提示“Shizuku 是免 Root 的小工具，启动并授权后净屏才能获得系统开关权限。请先安装并启动：Android 11 及以上可在手机上用无线调试启动，或用数据线连接电脑启动。”并保留官方下载按钮；未获得授权时提示“请在 Shizuku 中允许净屏使用。”；缺少授权时提示“请先连接 Shizuku，并在 Shizuku 中允许净屏使用。”；扫描渠道确认弹窗改为“净屏会临时读取系统里的通知设置，找出可能是广告推送的通知渠道；读取过程中可能读到通知内容，但不会展示、保存或上传。请逐项核对候选渠道。”，隐私口径不变。
- “帮助与反馈”的 Android 常见问题新增一条“系统广告与营销通知里的 Shizuku 是什么？”，说明它是免 Root 的小工具，通过 USB 调试或无线调试启动并授权后可直接关闭快应用服务等系统开关并列出疑似广告推送的通知渠道，不连接不影响广告自动跳过，手机重启或 Shizuku 停止后需要重新连接。
- 验证：`npx tsc --noEmit` 通过；`cd android; .\gradlew.bat :app:compileDebugKotlin --offline --console=plain` 成功（15 秒）；`git diff --check` 通过（仅既有换行符提示）。未启动应用、未做 UI 验收，未打包或安装；卡片与弹窗的实际观感待用户验收。工作区同时有其他任务的改动，本次只改上述界面文案与说明文档，未清理或覆盖其他改动。
- 涉及文件：`src/screens/SystemCleanupExtras.tsx`、`src/screens/SettingsDetailScreen.tsx`、`README.md`、`docs/wiki/README.md`、本修改记录。

### 2026-11-06 11:23:36 +08:00 — 系统推荐开关区块改为普通用户可读的表述

- 用户反馈设置页"系统设置辅助操作"区块偏术语、看不懂。功能与交互不变，仅改写用户可见文案：分区标题改为"系统推荐开关"；卡片说明改为"净屏可以帮你关掉手机自带系统里的推荐开关。不同手机版本里这些开关的位置可能略有不同。"；折叠按钮由"查看项目 / 收起项目"改为"查看开关 / 收起"；列表右侧由"辅助关闭"改为"关掉"。
- 点击某项后的确认弹窗改为"关掉「xxx」？"，并直接给出手机上的进入路径：设置 → 安全 → 更多安全设置 → 广告与隐私；设置 → 系统管理与升级 → 全局搜索；桌面右滑进入的负一屏设置；vivo 浏览器 → 设置；应用商店 → 我的 → 设置。子页面名不再与上级路径重复（原先"在系统设置中进入广告与隐私，再进入"广告与隐私""），提示改为"净屏会自动把它关掉；没找到这个开关就不会改动"。
- 原生侧用户可见提示同步改为口语：无法启动辅助操作→无法启动这个操作；当前设备没有此辅助规则→这台手机没有这个开关；上一项辅助操作尚未结束→上一个操作还没结束；没有此项目的原状态记录→没有之前保存的记录，不能恢复；未找到匹配的开关，未作修改→没找到这个开关，没有改动；页面已变化，辅助操作已停止→页面已经变了，操作已停止；服务已停止／操作超时／未能确认更改结果→没能确认是否已关成功，请到设置里查看；已关闭并确认→已关掉并确认；当前已是目标状态→这个开关已经是现在这样了；系统未接受操作，恢复记录已保留→系统没有接受这次操作，设置没有被改动；无法安全确认开关，操作已停止→没能确认这个开关，操作已停止。
- 隐私政策正文、Android 无障碍服务说明（`android/app/src/main/res/values/strings.xml` 与 `src/screens/PermissionsScreen.tsx`）未改动：这些是与商店申报和线上公开政策页对齐的法定告知，改写需同步政策版本号与公开页，不属于本次界面文案范围。
- 同文件内 Shizuku 卡片文案（"系统开关授权（Shizuku）""去连接"等）由其他窗口同时改动，本次未覆盖、未回退。
- 验证：`npx tsc --noEmit` 通过（exit 0）；`:app:compileDebugKotlin --offline --console=plain` 成功，仅 `MainApplication.kt` 既有弃用警告。未打包、未安装、未做真机 UI 验收，新文案需真机确认显示效果。
- 涉及文件：`src/screens/SystemCleanupExtras.tsx`、`src/native/ClearScreenNative.ts`、`android/app/src/main/java/com/clearscreen/prototype/backend/ClearScreenUiCleanup.kt`、`docs/wiki/README.md`、本修改记录。

## 2026-11-04

### 2026-11-04 12:00:13 +08:00 — 修正点选歧义误杀与整页读取失败，打包安装净屏 1.0.70

- 1.0.69 真机验收未通过（用户截图 11:39 / 11:40，彩云天气 OPPEIN 广告）：点“×”仍出现“附近有多个控件，请点准目标”，另一处“×”出现“无法读取当前页面”。dex 复核确认 1.0.69 已含 `tapPrefers` 修正（运行的确实是新代码）；当时页面节点树未能留存（日志已轮转、dump 抓到无关窗口），按代码路径定位两条故障链并修复。
- 歧义误杀：重叠的关闭/无标签图标簇（广告 WebView 与原生树的重复“×”节点）在触点更偏另一节点中心时被判平局。`tapPrefers` 改为图标簇直接认排序首位；关闭语义目标命中自身中心半区即胜出；图标目标深命中（中心度 ≥0.75 且领先 0.2）可胜过仅擦边的带标签兄弟。尺寸与命中精度相当、角色不同的真平局仍拒绝。
- 整页读取失败：点选/确认读取上限 1500 节点 / 600 子级 / 深度 18 不足以覆盖广告 WebView 深层 HTML 节点树，截断即整帧丢弃（“无法读取当前页面”）；聚焦输入框也会整页拒绝。选取的点选、确认与关闭核验读取预算放宽至 3000 节点 / 1000 子级 / 深度 30、超时 4.5 秒；选取期间聚焦普通输入框不再整页拒绝（密码框仍拦截，输入节点仍不参与候选与学习指纹）；已捕获窗口按 id 解析，不再因更高层级窗口拒绝读取（确认点击仍校验遮挡与安全）。选取时新增 `frame-truncated`、`selection-rejected` 固定状态日志，不记录页面内容。
- 验证：`:app:compileDebugKotlin` 与 `:app:assembleRelease --offline --console=plain` 均成功；APK `净屏.apk` 与 `release/app-release.apk` 哈希一致，SHA-256 `512f5fdc5728245dc15dfd268398e9be235b37634c010a2041e84bd03c4cc004`，大小 106,539,469 字节；aapt 元数据为 `1.0.70` / `76`、包名 `com.clearscreen.prototype`、应用名“净屏”；apksigner v2 验签通过，正式证书 SHA-256 保持 `93c410454a8263736ff60bb6e27153b19f74f4c934f444531842f5162ccfa999`。通过 USB `adb install -r --no-streaming` 覆盖安装到 vivo V2502DA（`10AG4M2KGF00527`）与 Xiaomi 22101317C（`2f8898c80822`），均返回 `Success`；两机读回 `1.0.70` / `76`，首次安装时间分别保持 `2026-09-20 15:33:13` 与 `2026-10-09 09:22:57`，净屏无障碍服务仍启用。未启动应用、未做功能/UI 验收，该广告点选仍待用户验收；若仍失败，新增固定状态日志可区分截断/拒绝原因。
- 涉及文件：`android/app/src/main/java/com/clearscreen/prototype/backend/ClearScreenAdLearning.kt`、`android/app/src/main/java/com/clearscreen/prototype/backend/ClearScreenAccessibilityService.kt`、`android/app/build.gradle`、`app.json`、`README.md`、`docs/wiki/README.md`、本修改记录、正式 APK。

### 2026-11-04 11:11:59 +08:00 — 打包并双机覆盖安装净屏 1.0.69

- 将 11:03 的学习选取歧义按点选位置判定修正打成 Android 正式 Release，递增至 `1.0.69` / versionCode `75`；应用名“净屏”、包名 `com.clearscreen.prototype` 和正式签名保持不变。安装包 `android/app/build/outputs/apk/净屏.apk`，大小 106,539,465 字节，SHA-256 `5510323e9be8e66a735923a4dcbb687cef0eed24ef5b1bb1b73b6659e5e83f5f`；Gradle 原始 APK 与命名 APK 哈希一致。
- 验证：`:app:assembleRelease --offline --console=plain` 成功；aapt 元数据为包名 `com.clearscreen.prototype`、版本 `1.0.69` / `75`、应用名“净屏”；apksigner v2 验签通过，正式证书 SHA-256 保持 `93c410454a8263736ff60bb6e27153b19f74f4c934f444531842f5162ccfa999`。命名包中文文件名在 Git Bash 下传给 aapt/adb 会乱码，元数据核验与安装均使用字节一致的 `release/app-release.apk`。通过 USB `adb install -r --no-streaming` 覆盖安装到 vivo V2502DA（`10AG4M2KGF00527`）和 Xiaomi 22101317C（`2f8898c80822`），安装命令均返回 `Success`；两机读回 `1.0.69` / `75`，首次安装时间分别保持 `2026-09-20 15:33:13` 与 `2026-10-09 09:22:57`，净屏无障碍服务仍在启用列表。未卸载、清除数据或启动应用；未进行控件选择功能/UI验收，点准小“×”场景仍待用户验收。
- 涉及文件：`android/app/build.gradle`、`app.json`、`README.md`、`docs/wiki/README.md`、本修改记录、正式 APK。

### 2026-11-04 11:03:00 +08:00 — 点准小“×”仍报多控件，歧义改为按点选位置判定

- 用户反馈 1.0.68 上精确点到广告小“×”仍提示“附近有多个控件，请点准目标”。复核源码确认：点选命中多个互不嵌套的紧凑可点控件（如“×”与其后重叠的紧凑广告图、跨 WebView/原生树的重复“×”节点）时一律判为歧义，未按点选位置区分。
- `ClearScreenAdLearning.kt` 细化歧义判定，重叠候选仅在点选位置无法区分时拒绝：同位重复节点、关闭语义且不小于对手或点得更准、面积不足对手一半且命中中心半区、命中更深（面积不更大且中心度领先 0.2 以上）、双方同属关闭/无标签图标簇、容差路径下距点击明显更近（8dp 以上）的候选按既有排序（关闭语义优先、更小优先、更深优先）选中；尺寸与命中精度相当且角色不同的重叠候选仍拒绝并保留原提示。排序、危险控件拦截、确认点击与关闭验证、规则唯一匹配、不保存坐标等约定不变。
- 验证：`:app:compileDebugKotlin --offline --console=plain` 成功（BUILD SUCCESSFUL，仅 `MainApplication.kt` 既有弃用警告）；`git diff --check` 通过；未跟踪学习源码行尾空白与冲突标记检查通过（LF 行尾）。未运行自动化或真机功能/UI测试；按仓库约定未打包、未安装，1.0.68 不含本次修改，具体广告待用户更新后验收。
- 涉及文件：`android/app/src/main/java/com/clearscreen/prototype/backend/ClearScreenAdLearning.kt`、`README.md`、`docs/wiki/README.md`、本修改记录。

## 2026-11-03

### 2026-11-03 19:21:25 +08:00 — 打包并双机覆盖安装净屏 1.0.68

- 将小型关闭控件选取容差和持续状态反馈修正打成 Android 正式 Release，递增至 `1.0.68` / versionCode `74`；应用名“净屏”、包名 `com.clearscreen.prototype` 和正式签名保持不变。安装包 `android/app/build/outputs/apk/净屏.apk`，大小 106,539,469 字节，SHA-256 `3E8B5856CB60C8A29AB34AA92C89FE766C3343F86008ECF4CDBF1C8AA9F8789E`；Gradle 原始 APK 与命名 APK 哈希一致。
- 验证：`:app:assembleRelease --offline --console=plain` 成功；aapt 元数据为包名 `com.clearscreen.prototype`、版本 `1.0.68` / `74`、应用名“净屏”；apksigner v2 验签通过，正式证书 SHA-256 保持 `93c410454a8263736ff60bb6e27153b19f74f4c934f444531842f5162ccfa999`。通过 USB `adb install -r --no-streaming` 覆盖安装到 vivo V2502DA（`10AG4M2KGF00527`）和 Xiaomi 22101317C（`2f8898c80822`），安装命令均返回 `Success`。小米读回 `1.0.68` / `74`，首次安装时间仍为 `2026-10-09 09:22:57`，净屏无障碍服务仍启用；vivo 安装后从 ADB 断开，未能读回版本、首次安装时间或服务状态（安装前为 `1.0.67`，首次安装时间 `2026-09-20 15:33:13`，服务启用）。未卸载、清除数据或启动应用；未进行控件选择功能/UI验收，实际小“×”仍待用户验收。
- 涉及文件：`android/app/build.gradle`、`app.json`、`README.md`、`docs/wiki/README.md`、本修改记录、正式 APK；百度同步盘 ClearScreen 项目基本信息、验证说明和广告识别复用说明同步更新。

### 2026-11-03 18:54:10 +08:00 — 改进小型关闭控件选取反馈

- 用户反馈小型“×”点选后没有提示和选中框，取消按钮仍可点击；该广告暂时无法重现，手机现有日志没有保留此次点选记录，不能确认其控件是否向无障碍服务暴露或断定全部原因。
- `ClearScreenAdLearning.kt` 在精确命中为空时，对尺寸不超过 40dp 的真实可点击控件按有限范围扩大点选容差；只接受关闭语义或无标签的小型控件，多处不相关候选、危险控件、身份不唯一或当前窗口变化时拒绝。显示的边框、保存的规则和确认动作仍使用实际控件，规则不保存坐标。
- 点选时持续显示读取、结果或失败状态；读取中再次点选不会作废正在等待的结果，取消仍可操作。日志只增加固定拒绝状态，不记录页面内容。
- 验证：`:app:compileDebugKotlin --offline --console=plain` 成功；`git diff --check` 以及未跟踪学习源码的空白和冲突标记检查通过（Git 提示部分既有文件的换行格式）。未运行自动化或手工功能/UI测试，未打包或安装；现有1.0.67 APK及手机版本不含本次修改，具体广告仍需用户在更新后验收。
- 涉及文件：`android/app/src/main/java/com/clearscreen/prototype/backend/ClearScreenAdLearning.kt`、`README.md`、`docs/wiki/README.md`、本修改记录；百度同步盘项目基本信息、验证说明和广告识别复用说明同步更新。

### 2026-11-03 18:18:12 +08:00 — 构建并双机安装净屏 1.0.67

- 将 17:39 的学习控件选择入口及输入框快照修正打包为 Android 正式 Release，版本递增至 `1.0.67` / versionCode `73`；应用名“净屏”、包名 `com.clearscreen.prototype` 与正式签名保持不变。安装包 `android/app/build/outputs/apk/净屏.apk`，大小 106,539,469 字节，SHA-256 `C4F0924EEF0DBD31F8DE391B8032C92514907FD16525E29C1637D994655F5625`；Gradle 原始 APK 与命名 APK 哈希一致。
- 验证：`:app:assembleRelease --offline --console=plain` 成功；aapt 元数据为包名 `com.clearscreen.prototype`、版本 `1.0.67` / `73`；apksigner v2 验签通过，证书 SHA-256 保持 `93c410454a8263736ff60bb6e27153b19f74f4c934f444531842f5162ccfa999`。通过 USB `adb install -r --no-streaming` 覆盖安装到 vivo V2502DA（`10AG4M2KGF00527`）和 Xiaomi 22101317C（`2f8898c80822`），均返回 `Success`。两台设备读回 `1.0.67` / `73`，首次安装时间分别保持 `2026-09-20 15:33:13` 与 `2026-10-09 09:22:57`，净屏无障碍服务仍在启用列表；未卸载、清除数据或启动应用，未运行功能/UI验收。
- 涉及文件：`android/app/build.gradle`、`app.json`、`README.md`、`docs/wiki/README.md`、本修改记录、正式 APK；百度同步盘 ClearScreen 项目基本信息、验证说明和广告识别复用说明同步更新。

### 2026-11-03 17:39:47 +08:00 — 修正学习控件选择入口等待

- 用户确认 vivo 升级至1.0.66后点击“选择控件”仍无响应；现有日志没有记录这次按钮回调，不能确认设备上的全部成因。源码发现入口与后台扫描共用读取占位，且页面出现任意可编辑节点就拒绝整页。
- 后台快照/手动事件与主动选择使用各自一个有界读取通道；选择时立即显示“读取中”，读取经安全校验的窗口身份后再进入选取层，取消和超时会作废迟到结果。点选和确认仍读取目标页面控件树，核验当前窗口、唯一目标和遮挡。增加不含页面信息的按钮点击和拒绝状态日志。
- 未聚焦普通输入框不再导致整页学习或规则重放被拒绝；学习快照不读取输入框及其子节点的内容，密码框、聚焦输入、受保护界面与敏感目标仍阻止操作。
- 验证：`:app:compileDebugKotlin --offline --console=plain`、`git diff --check` 和未跟踪学习源码的空白/冲突标记检查通过；未运行自动化或手工功能/UI测试，未打包或安装。现有1.0.66 APK与手机安装版本不含本次修正，实际触摸和识别效果待用户后续验收。
- 涉及文件：`android/app/src/main/java/com/clearscreen/prototype/backend/ClearScreenAdLearning.kt`、`android/app/src/main/java/com/clearscreen/prototype/backend/ClearScreenAccessibilityService.kt`、`README.md`、`docs/wiki/README.md`、本修改记录；百度同步盘项目基本信息、验证说明和广告识别复用说明同步更新。

### 2026-11-03 17:15:18 +08:00 — 构建并双机安装净屏 1.0.66

- 将学习控件选择的后台读取及浮层目标窗口焦点修正打包为 Android 正式 Release，版本递增至 `1.0.66` / versionCode `72`；应用名“净屏”、包名 `com.clearscreen.prototype` 与正式签名保持不变。安装包 `android/app/build/outputs/apk/净屏.apk`，大小 106,539,465 字节，SHA-256 `897D6560D6193CFDF2C7463C8BAEE53A18BD098C9D95ECCA96391BD83D9BA8E0`；Gradle 原始 APK 与命名 APK 哈希一致。
- 验证：`:app:assembleRelease --offline --console=plain` 成功；aapt 元数据为包名 `com.clearscreen.prototype`、版本 `1.0.66` / `72`；apksigner v2 验签通过，证书 SHA-256 保持 `93c410454a8263736ff60bb6e27153b19f74f4c934f444531842f5162ccfa999`。通过 `adb install -r --no-streaming` 覆盖安装到 vivo V2502DA（`10AG4M2KGF00527`）和 Xiaomi 22101317C（`2f8898c80822`），均返回 `Success`。两台设备读回 `1.0.66` / `72`，首次安装时间分别保持 `2026-09-20 15:33:13` 与 `2026-10-09 09:22:57`，净屏无障碍服务仍在启用列表；未卸载、清除数据或启动应用，未运行功能/UI验收。
- 涉及文件：`android/app/build.gradle`、`app.json`、`README.md`、`docs/wiki/README.md`、本修改记录、正式 APK；百度同步盘 ClearScreen 项目基本信息与验证说明同步更新。

### 2026-11-03 16:38:11 +08:00 — 修正学习选取时界面线程阻塞及浮层焦点冲突

- 针对用户反馈“屏幕任何地方及取消均不能点击”，将学习快照、进入选择、手动点击事件源、点选和关闭确认的节点查询移出界面线程，复用单个后台任务占位；单次等待2.5秒超时，原始系统调用未返回时不堆积新线程。等待入口读取期间保留工具栏；取消/退出/删除/会话变化使旧任务失效，迟到结果丢弃，节点和事件副本回收。节点读取期间不持有隐私操作锁，点击仍检查授权、目标、窗口和当前会话；未改为坐标操作。补充只含固定状态的学习输入/读取日志。
- 修正已核实本应用无障碍选取浮层后，`learningRoot` 又通过 `isEligibleForegroundPackage` 要求下方目标持有前台焦点的矛盾。只对核实过的本次目标窗口使用浮层读取路径；普通扫描仍要求前台应用，保护应用、敏感界面、目标身份及遮挡检查保留。此为源码可确认的问题，尚未真机复现并证明覆盖用户截图的全部原因。
- 验证：最终 `:app:compileDebugKotlin --offline --console=plain` 成功（18秒）；`git diff --check` 及未跟踪学习源码的空白/冲突标记静态检查通过。只读查询时vivo仍为1.0.64 / 70（最后更新时间13:53:11），随后USB断开，仅小米在线。未运行自动化或手工功能/UI测试，未打包安装；现有1.0.65 APK不包含本次修正。
- 涉及文件：`ClearScreenAdLearning.kt`、`ClearScreenAccessibilityService.kt`、`README.md`、`docs/wiki/README.md`、本修改记录；百度同步盘项目基本信息、广告识别算法与复用说明同步补充未发布状态。

### 2026-11-03 15:06:22 +08:00 — 构建净屏 1.0.65 并尝试更新 vivo

- 按用户要求将当前源码正式打包，版本递增至 `1.0.65` / versionCode `71`；包名 `com.clearscreen.prototype`、应用名“净屏”及 Release 签名保持不变。APK 为 `android/app/build/outputs/apk/净屏.apk`，大小 106,539,469 字节，SHA-256 `83A8F208B6479C0D36C2B3B4B7CA93419CAE2C74D2A24DCAA52B2014AFD9D84D`；Gradle 原始 APK 与命名 APK 哈希一致。
- 验证：`:app:assembleRelease --offline --console=plain` 成功；APK 元数据为 `com.clearscreen.prototype`、`1.0.65` / `71`，`apksigner verify` 的 v2 签名通过，证书 SHA-256 仍为 `93c410454a8263736ff60bb6e27153b19f74f4c934f444531842f5162ccfa999`。通过 USB 推送至 vivo V2502DA 后，`adb install -r --no-streaming` 返回 `INSTALL_FAILED_ABORTED: User rejected permissions`，安装未完成；设备仍为 `1.0.64` / `70`，首次安装时间保持 `2026-09-20 15:33:13`，净屏无障碍服务仍启用。Xiaomi 22101317C 未更新。未卸载、清除数据、打开应用或进行功能/UI验收；待手机允许安装后重试。
- 涉及文件：`android/app/build.gradle`、`app.json`、`README.md`、`docs/wiki/README.md`、本修改记录、正式 APK；百度同步盘 ClearScreen 项目基本信息与验证说明同步更新。

### 2026-11-03 14:31:08 +08:00 — 修正控件选取触摸与取消

- 全屏选取层改为按 Android 视图层级分发按钮触摸，页面区域由根视图消费手势；选取层“取消”返回学习工具栏，工具栏“取消”仍结束学习。所点控件树在单个后台读取任务中解析，取消不会等待读取；会话变化或重新点选后丢弃迟到结果，确认前仍重新检查目标、窗口与遮挡。停止选中后的周期性全树重读，减少界面线程阻塞；无法读取或无法区分真实控件时给出提示，不进行坐标点击。
- 验证：`:app:compileDebugKotlin --offline --console=plain` 最终成功，`git diff --check` 通过。设备只读状态查询时截图中的选取浮层已不存在，未复现触摸分发；未运行自动化或真机功能/UI测试，未打包或安装，实际效果待用户验收。
- 涉及文件：`ClearScreenAdLearning.kt`、`README.md`、`docs/wiki/README.md`、本修改记录。

### 2026-11-03 13:53:16 +08:00 — 完成 vivo 安装净屏 1.0.64

- vivo V2502DA USB 重新连接后，通过 `adb install -r --no-streaming` 完成升级。设备读回 `com.clearscreen.prototype` 版本 `1.0.64` / versionCode `70`，首次安装时间保持 `2026-09-20 15:33:13`，无障碍启用列表仍包含净屏服务。至此两台手机均安装 1.0.64；未卸载、清除数据或启动应用，未进行功能/UI验收。
- 涉及文件：`README.md`、`docs/wiki/README.md`、本修改记录；百度同步盘项目基本信息与验证说明同步更新。

### 2026-11-03 13:19:09 +08:00 — 构建并升级安装净屏 1.0.64

- 将控件选择无响应修正打包为 Android 正式 Release，版本 `1.0.64` / versionCode `70`，保持包名 `com.clearscreen.prototype` 和原 Release 证书。安装包 `android/app/build/outputs/apk/净屏.apk`，大小 106,539,469 字节，SHA-256 `1D54E1A27A2F2ABC431D7D0F4659583344AEF31CD0ED9379715EE1D0B66B257E`；Gradle 原始 APK 与净屏.apk 哈希一致。
- 验证：`:app:assembleRelease --offline --console=plain` 成功；APK 元数据为包名 `com.clearscreen.prototype`、版本 `1.0.64` / `70`；`apksigner verify` 的 v2 签名通过，正式证书 SHA-256 为 `93c410454a8263736ff60bb6e27153b19f74f4c934f444531842f5162ccfa999`。已通过 `adb install -r --no-streaming` 升级安装到 Xiaomi 22101317C（`2f8898c80822`），设备读回 `1.0.64` / `70`，首次安装时间保持 `2026-10-09 09:22:57`，无障碍服务仍启用；未卸载、清除数据或启动应用。vivo V2502DA（`10AG4M2KGF00527`）在安装时已从 ADB 断开，未能升级，待重新连接后安装。未进行功能/UI验收。
- 涉及文件：`android/app/build.gradle`、`app.json`、`README.md`、`docs/wiki/README.md`、本修改记录；百度同步盘项目基本信息与验证说明同步更新。

### 2026-11-03 13:13:45 +08:00 — 学习点击未确认关闭时返回控件选择

- 确认选取并执行控件后，若按钮仍出现或未能确认关闭，等待节点核对结束后自动重新打开控件选择层，方便重选；未确认关闭时不保存规则。选取层“取消”清除当前框选并保留选择状态。
- 验证：`:app:compileDebugKotlin --offline --console=plain` 通过，`git diff --check -- README.md docs/wiki/README.md docs/wiki/CHANGELOG.md` 通过；未运行自动化或真机功能/UI测试，未打包/安装。
- 涉及文件：`ClearScreenAdLearning.kt`、`README.md`、`docs/wiki/README.md`、本修改记录。

### 2026-11-03 13:12:12 +08:00 — 修正控件选择入口无响应

- 显式学习时不再因当前应用处于排除/白名单状态而拒绝读取；该状态仍阻止自动重放，重新启用应用后才会执行学习规则。复杂界面的选取和确认提高控件树读取上限至最多1500个节点、单父节点最多600个子节点；后台学习快照轮询从180毫秒调为400毫秒，选取状态下重读调为700毫秒，减少控件树扫描对工具栏触摸响应的占用。读取失败时提示当前界面暂不可读取。
- 验证：`:app:compileDebugKotlin --offline --console=plain`成功，`git diff --check`通过；未运行自动化或真机功能/UI测试，未打包或安装。设备日志没有提供可用于确认按钮点击回调的记录，设备端行为仍待真机复现确认。
- 涉及文件：`ClearScreenAdLearning.kt`、`ClearScreenAccessibilityService.kt`、`README.md`、`docs/wiki/README.md`、本修改记录。

### 2026-11-03 13:11:41 +08:00 — 修正控件选取取消后的状态恢复

- 选取层“取消”改为在当前全屏选取层中清除框选和确认按钮，继续等待点选；不再移除并重建浮层，避免焦点恢复延迟导致闪回工具栏。
- 验证：`:app:compileDebugKotlin --offline --console=plain` 通过，`git diff --check -- docs/wiki/CHANGELOG.md` 通过；未运行自动化或真机功能/UI测试，未打包/安装。
- 涉及文件：`ClearScreenAdLearning.kt`、本修改记录。

### 2026-11-03 13:10:37 +08:00 — 修正启动页背景底部留白

- 共享背景图改为绝对边界填充，避免内容区顶部安全区留白缩短背景图片高度；启动页背景延伸到底部，原有内容布局不变。README 与界面 Wiki 已同步说明。
- 验证：`npx tsc --noEmit` 与 `git diff --check` 通过；未运行应用或执行真机 UI 验收，未构建、打包或安装。
- 涉及文件：`src/components/UI.tsx`、`README.md`、`docs/wiki/README.md`、本修改记录。

### 2026-11-03 13:06:08 +08:00 — 学习关闭入口直接启动

- 首页点击“学习关闭”时立即调用学习启动，不再展示介绍弹窗或等待用户二次点击“开始学习”；学习状态已开启时继续使用该入口结束学习。无障碍服务未启用或总开关关闭时，仍走原有权限引导/提示。
- 验证：`npx tsc --noEmit` 与 `git diff --check` 通过；未运行自动化或真机功能/UI测试，未打包或安装。
- 涉及文件：`App.tsx`、`docs/wiki/README.md`、本修改记录。

### 2026-11-03 13:01:37 +08:00 — 构建并双机升级安装 1.0.63

- 按用户要求将最新选取触摸修正打包为 Android 正式 Release，版本递增至 `1.0.63` / versionCode `69`；保持应用名“净屏”、包名 `com.clearscreen.prototype` 和原 Release 签名。正式安装文件 `android/app/build/outputs/apk/净屏.apk`，大小 106,523,861 字节，SHA-256 `C169B897C268B2ABF0E8500449A6D63373E6E70D6F804E6A61443D0E9AFC3061`，与 Gradle 原始产物一致。
- 验证：`:app:assembleRelease --offline --console=plain`、`npx tsc --noEmit`、`git diff --check`、APK 包名/版本核对及 `apksigner verify --verbose --print-certs` 通过；证书 SHA-256 仍为 `93c410454a8263736ff60bb6e27153b19f74f4c934f444531842f5162ccfa999`。通过 USB `adb install -r --no-streaming` 覆盖安装到 vivo V2502DA（`10AG4M2KGF00527`）和 Xiaomi 22101317C（`2f8898c80822`），均成功；系统读回版本 `1.0.63` / `69`。首次安装时间分别保持 `2026-09-20 15:33:13`、`2026-10-09 09:22:57`，无障碍启用列表仍包含净屏服务。未卸载或清除数据，未打开应用；触摸拦截和控件识别尚待用户真机验收。
- 涉及文件：`android/app/build.gradle`、`app.json`、`README.md`、`docs/wiki/README.md`、本修改记录、Release APK；百度同步盘 ClearScreen 项目基本信息和验证说明同步更新。

### 2026-11-03 12:52:44 +08:00 — 修正学习控件选择的触摸拦截与浮层焦点读取

- 针对用户反馈“选取时点击跳过直接关闭广告、无法检测控件”，将原非聚焦选择窗及透明子视图接收触摸的路径改为全屏触摸模态窗口，根容器消费完整手势；只有底部按钮栏交给子控件，点目标仅解析/框选，拖动、多指和取消不选择，确认才调用 ACTION_CLICK。设置整屏布局及输入法排除；返回键收起选择层并恢复持续学习工具栏。选取层取消重选、工具栏取消结束的行为保留。
- 选择期间记录目标应用窗口标识；只有确认为本应用无障碍浮层时，才读取该标识对应且没有其他应用窗口覆盖的目标，适配浮层接管焦点后的节点读取。普通工具栏继续使用唯一聚焦应用；保护应用、敏感输入、业务按钮、确认前遮挡/当前窗口及唯一控件核对继续生效。
- 可用跳过控件及其内部数字倒计时使用稳定身份，优先选择有关闭/跳过语义的点击节点；等待可跳过、禁用和业务保护不放宽。既有带计时文字的规则可能需重新学习，不删除原规则。
- 验证：`:app:compileDebugKotlin --offline --console=plain`通过（最终构建16秒）；`git diff --check`与未跟踪学习源码的空白/冲突标记静态检查通过。只读检查时仅小米在线，未获得截图状态下的选取输入窗口，不能据此将设备上的具体输入分发原因视为已复现。未运行自动化/真机功能或UI测试，未打包/安装；现有1.0.62不包含本次修正。
- 涉及文件：`ClearScreenAdLearning.kt`、`ClearScreenAccessibilityService.kt`、`README.md`、`docs/wiki/README.md`、本修改记录；百度同步盘 ClearScreen 广告识别算法与复用说明同步补充。

### 2026-11-03 12:10:25 +08:00 — 构建并双机升级安装 1.0.62

- 按用户要求将当前源码构建为正式 Android Release，版本递增至 `1.0.62` / versionCode `68`；保持包名 `com.clearscreen.prototype`、净屏应用名及原 Release 签名。安装文件为 `android/app/build/outputs/apk/净屏.apk`，大小 106,523,861 字节，SHA-256 `C36B0A098FB7E99A2D27D87FA2335B9A979701171E1DAFCFED6CDF6F87BAF6A7`；Gradle 原始 APK 与命名 APK 哈希一致。
- 验证：`:app:assembleRelease --offline --console=plain`、`npx tsc --noEmit`、`git diff --check`、APK 包名/版本核对和 `apksigner verify --verbose --print-certs` 通过；签名证书 SHA-256 仍为 `93c410454a8263736ff60bb6e27153b19f74f4c934f444531842f5162ccfa999`。通过 USB `adb install -r --no-streaming` 覆盖安装到 vivo V2502DA（`10AG4M2KGF00527`）与 Xiaomi 22101317C（`2f8898c80822`），均返回 `Success`，系统读回 `1.0.62` / `68`；首次安装时间分别保持 `2026-09-20 15:33:13` 与 `2026-10-09 09:22:57`，无障碍启用列表仍包含净屏服务。未卸载或清除应用数据，未启动应用或进行功能/UI验收；控件学习和滚动查找效果仍待用户实机验收。隐私政策版本为 `2026-11-03-v3`，首次启动需重新同意，应用数据保留。
- 涉及文件：`android/app/build.gradle`、`app.json`、`README.md`、`docs/wiki/README.md`、本修改记录、`android/app/build/outputs/apk/净屏.apk`、`android/app/build/outputs/apk/release/app-release.apk`；百度同步盘 ClearScreen 项目基本信息与验证说明同步更新。

### 2026-11-03 11:58:28 +08:00 — 学习关闭改为识别真实控件并跟随滚动后的节点

- 悬浮入口改为“选择控件”。点选后立即读取真实可点击节点，按实际边界框选并持续刷新；只有唯一、安全且可区分的控件才显示确认。确认后重新核对控件和遮挡，通过 ACTION_CLICK 操作，不再画固定圆圈或向所选坐标发送触摸；无节点的自绘区域提示无法学习。读取本应用选取浮层下方的节点时，只选择唯一聚焦应用，保留原有系统确认、敏感输入、排除应用及业务控件保护。保留并行修改的取消行为：选取层“取消”重选，工具栏“取消”结束学习。
- 新规则只保存应用更新时间、按钮资源标识/类型/文字哈希及所属卡片内相对结构/上下文摘要，不持久化屏幕坐标、根边界或整页布局。重放在当前树中唯一定位按钮，滚动后按新位置查找，等待180ms并复读避免移动中点击；局部内容变化、列表复用其他内容、多个相同目标、应用更新或不可读取时跳过，不主动滚动。关闭确认跟踪实际节点，连续两次核对移除及页面变化；滚动或仅禁用控件不算关闭，完成/取消/超时回收节点。
- 旧位置/视觉规则保留供删除、标为“需重新学习”并停止执行。存储按应用版本和控件身份去重，容量限制前先去重，保留同一应用的其他规则。隐私说明同步2026-11-03-v3，下次升级需重新同意但保留原记录；公开站点尚未同步。
- 验证：主工程与 Harmony 隔离试验 TypeScript 类型检查、`:app:compileDebugKotlin --offline --console=plain` 均通过；`git diff --check` 通过。未运行自动化或真机功能/UI测试，未打包或安装；手机上的1.0.61不包含本次改动。截图中的按钮是否暴露节点、滚动及多卡片行为仍待用户验收。
- 涉及文件：`ClearScreenAdLearning.kt`、`ClearScreenAccessibilityService.kt`、`ClearScreenStore.kt`、`ClearScreenNativeModule.kt`、`ClearScreenPrivacyConsentStore.kt`、`App.tsx`、`src/data.ts`、`src/native/ClearScreenNative.ts`、`src/privacy.ts`、`src/screens/LearnedCloseRecords.tsx`、`src/screens/PrivacyPolicyScreen.tsx`、`README.md`、`docs/wiki/README.md`、`docs/wiki/隐私政策.md`、`docs/wiki/应用商店合规核查.md`、本记录；百度同步盘的广告识别算法与复用说明同步补充。

### 2026-11-03 11:09:22 +08:00 — 选点取消后重新选择关闭位置

- 选点浮层的“取消”现在丢弃当前坐标并重新打开位置选择；只有悬浮工具栏中的“取消”会结束学习。重选期间避免工具栏抢先重现，若目标页面读取失败则恢复工具栏。
- 同步 README 与界面 Wiki 中两种“取消”的行为说明。
- 验证：`npx tsc --noEmit`、`:app:compileDebugKotlin --offline --console=plain`、`git diff --check` 均通过；未运行功能测试、未打包或安装。
- 涉及文件：`android/app/src/main/java/com/clearscreen/prototype/backend/ClearScreenAdLearning.kt`、`README.md`、`docs/wiki/README.md`、本修改记录。

### 2026-11-03 10:58:03 +08:00 — 将 1.0.61 覆盖安装到两台手机

- 按用户要求，通过 USB 将正式 Release 1.0.61 / versionCode 67 覆盖安装到 vivo V2502DA（`10AG4M2KGF00527`）和 Xiaomi 22101317C（`2f8898c80822`）。保持包名及原 Release 签名，未卸载或清除应用数据；vivo 安装前需在系统风险提示中勾选确认并继续。
- 安装后只读核对两台设备均为 `1.0.61` / `67`。首次安装时间保持：vivo `2026-09-20 15:33:13`，Xiaomi `2026-10-09 09:22:57`。系统无障碍启用列表仍包含净屏服务，崩溃服务列表为空。未启动应用，未执行功能/UI验收。
- 涉及文件：`README.md`、`docs/wiki/README.md`、本记录；项目基本信息与验证记录同步更新至百度同步盘 ClearScreen 资料目录。

### 2026-11-03 10:54:43 +08:00 — 构建正式 Release 1.0.61

- 按用户要求将当前源码打包为 Android Release APK，版本递增至 `1.0.61` / versionCode `67`；保持包名 `com.clearscreen.prototype` 与原正式 Release 签名。安装包为 `android/app/build/outputs/apk/净屏.apk`。
- 验证：`:app:assembleRelease --offline --console=plain` 构建成功；APK 核对为应用名“净屏”、包名 `com.clearscreen.prototype`、版本 `1.0.61` / `67`；`apksigner verify --verbose --print-certs` 通过，正式证书 SHA-256 为 `93c410454a8263736ff60bb6e27153b19f74f4c934f444531842f5162ccfa999`。命名 APK 和 Gradle 原始 APK 均为 106,523,713 字节、SHA-256 `73E5F5D0FB7377FBFBD30EB65E780A03C0AEF89F1B04753328315684B30E78F6`。未安装，未执行功能/UI验收。
- 涉及文件：`android/app/build.gradle`、`app.json`、`README.md`、`docs/wiki/README.md`、本记录；项目基本信息与验证记录同步更新至百度同步盘 ClearScreen 资料目录。

### 2026-11-03 10:38:07 +08:00 — 首页显示自定义关闭规则并支持逐条删除

- 首页最近处理记录缩减为三条，“查看全部”改为“更多”并打开处理记录；保留今日处理数字的今日筛选入口。下方新增“自定义关闭窗口”卡片显示最近三条学习规则，更多页列出全部已保存规则（现有上限80条）；以应用和时间展示，应用不可用时回退包名与通用图标。
- 新增带确认的逐条删除，通过持久化规则的唯一标识删除对应规则，保留其他规则及处理记录；共享规则存储锁，并作废尚未执行的旧规则回调。学习中与返回首页时刷新规则列表。隐私政策补充逐条删除入口，沿用待发布的 `2026-11-03-v2`。
- 验证：主工程与 Harmony 隔离试验 `tsc --noEmit`、`:app:compileDebugKotlin --offline --console=plain`、`git diff --check` 通过；未执行自动化/真机功能及 UI 验收，未打包或安装。公开隐私政策仍待发布前同步。
- 涉及文件：`App.tsx`、`src/data.ts`、`src/native/ClearScreenNative.ts`、`src/screens/HomeScreen.tsx`、`src/screens/LearnedCloseRecords.tsx`、`src/screens/RecordsScreen.tsx`、`src/screens/RecordDetailScreen.tsx`、`src/screens/PrivacyPolicyScreen.tsx`、`ClearScreenStore.kt`、`ClearScreenAdLearning.kt`、`ClearScreenNativeModule.kt`、`README.md`、`docs/wiki/README.md`、`docs/wiki/隐私政策.md`、本记录；复用说明追加到百度同步盘 ClearScreen 资料目录。

### 2026-11-03 10:26:53 +08:00 — 支持连续选取并确认学习位置

- 学习会话改为持续至用户点击“取消”；学习成功或失败后保留悬浮工具栏，可继续切换应用学习。锁屏、总开关关闭、撤回同意及无障碍服务中断等仍会结束会话。
- “选取位置”先标记选中的点并显示“确认选取”；确认后才触发一次用户指定点按，继续沿用页面身份、遮挡和受保护控件检查。延迟操作带会话身份和单次操作超时，取消或超时后的旧回调不能用于下一次选取。视觉规则记录并屏蔽悬浮栏范围，避免将工具栏混入识别特征；旧规则继续按原方式匹配。
- 学习监听时长发生变化，应用内及本地隐私政策更新为 `2026-11-03-v2`；下次安装并启动新版需重新同意，原有规则和记录保留。公开政策站点未同步，发布前需更新。
- 验证：`npx tsc --noEmit`、`:app:compileDebugKotlin --offline --console=plain`、`git diff --check` 通过；未执行功能测试或 UI 真机验收，未打包或安装。
- 涉及文件：`ClearScreenAdLearning.kt`、`ClearScreenPrivacyConsentStore.kt`、`App.tsx`、`src/privacy.ts`、`src/native/ClearScreenNative.ts`、`src/screens/PrivacyPolicyScreen.tsx`、`README.md`、`docs/wiki/README.md`、`docs/wiki/隐私政策.md`、`docs/wiki/应用商店合规核查.md`、本记录；复用说明追加到百度同步盘 ClearScreen 资料目录。

### 2026-11-03 10:21:59 +08:00 — 修复学习位置选取导致无障碍服务崩溃

- 只读核对 vivo 设备 1.0.60 的系统退出记录和崩溃堆栈：10:17、10:18 两次均为 `ClearScreenAdLearning.selectPosition` 选取位置后空指针；悬浮层移除后匿名 View 的 `handler` 变为 null，主进程退出，系统因而显示服务故障，即使授权开关仍开启。此证据仅覆盖这两次崩溃，不代表所有后台故障原因相同。
- 使用服务持有的 handler 投递延迟任务，延迟执行前核对学习会话和授权状态；位置选取期间使用 pending 防止重复选取，处理触摸取消及旧回调，避免已取消会话触发后续操作。
- 验证：`:app:compileDebugKotlin --offline --console=plain` 通过；`git diff --check` 通过。未执行自动化或真机功能验收，未打包或安装，已安装的 1.0.60 仍包含该问题。Android 负责无障碍服务连接，不能保证自行恢复所有系统停止或断连。
- 涉及文件：`ClearScreenAdLearning.kt`、`README.md`、`docs/wiki/README.md`、`docs/wiki/权限状态核验.md`、本修改记录；诊断和复用说明追加到百度同步盘 ClearScreen 资料目录。

### 2026-11-03 10:17:24 +08:00 — 更新并安装 1.0.60 到两台手机

- 按用户要求将 Android 版本递增至 `1.0.60` / versionCode `66`，保持包名 `com.clearscreen.prototype` 和 Release 签名；Release 安装包为 `android/app/build/outputs/apk/净屏.apk`。
- `:app:assembleRelease --offline --console=plain` 构建成功。APK 元数据、v2 正式签名核验通过；证书 SHA-256 为 `93c410454a8263736ff60bb6e27153b19f74f4c934f444531842f5162ccfa999`。命名 APK 与 Gradle 原始产物 SHA-256 均为 `9C6D041A266CAC556009F34726E0A3D62FC3E7B78354B5D94AD79F1D2F9A9A47`。因 ADB 无法解析中文文件路径，使用与命名 APK 哈希一致的原始产物路径完成安装。
- 通过 `adb install -r --no-streaming` 覆盖安装到 vivo V2502DA（`10AG4M2KGF00527`）和 Xiaomi 22101317C（`2f8898c80822`），两台均返回 `Success`，系统读回版本 `1.0.60` / `66`。vivo 首次安装时间保持 `2026-09-20 15:33:13`，小米保持 `2026-10-09 09:22:57`；未卸载或清除应用数据。安装后只读检查显示两台设备的净屏无障碍服务仍在启用列表和 Bound services 内，Crashed services 为空。
- 验证：`git diff --check`、APK 元数据/签名/文件哈希核验及两台设备的版本与无障碍授权状态读回通过。未启动应用、未进行广告处理或锁屏恢复功能验收。
- 涉及文件：`android/app/build.gradle`、`app.json`、`README.md`、`docs/wiki/README.md`、本修改记录；百度同步盘 ClearScreen 项目基本信息和验证说明同步更新。

### 2026-11-03 10:06:02 +08:00 — 排查并修正锁屏后无障碍处理停滞风险

- 用户确认“锁屏/后台一段时间后失效，关闭再开启无障碍可以恢复”。只读检查 vivo：安装版本仍为 `1.0.59 / 65`，组件在启用列表及 Bound services 内、Crashed services 为空，服务报告运行且当时没有待处理截图；历史退出记录有多次 `LOW_MEMORY`（2026-10-29、10-31、11-01，部分为 single-cleaner）。系统曾因内存压力结束应用是已确认事实，但不能据此断定本次所有失效都是这一原因。
- 修正锁屏/服务中断后的旧状态保留：统一取消扫描、媒体重试、学习会话及旧 OCR 证据，解锁后重新读取当前页面。增加扫描入口的瞬时异常隔离，避免失效节点导致整个进程异常退出；保留广告与敏感界面安全条件。
- OCR 改为有身份的单请求和 8 秒期限，完成路径统一释放占位与输入图像；迟到结果不能清除新请求或参与点击。已进入 ML 的任务等待完成再回收输入，不无限创建新识别；若 SDK 永久不完成则保留可诊断的堵塞状态、首页不显示健康，不伪装自动恢复。空闲约 60 秒、锁屏及内存回收时释放空闲识别器与缓存，忙碌识别器延后释放。
- 处理图片长边限制为 1920，OCR 回调仅携带身份与几何信息，不持有实时节点；隐藏和超过遍历预算的节点及时回收。内部帧年龄、节流和媒体监测统一使用单调时钟。服务 dump 增加屏幕状态、最近事件年龄、暂停原因与 OCR 堵塞标记。缩小图像后的识别效果仍需真机检查。
- 验证：`:app:compileDebugKotlin --offline --console=plain`、`git diff --check` 通过；只读取设备诊断，未切换无障碍/后台权限、未打开或操作应用进行功能验收，未运行自动化测试，未打包或安装。不能承诺绕过厂商收权、冻结或系统低内存回收。
- 涉及文件：`ClearScreenAccessibilityService.kt`、`ClearScreenAccessibilityHealth.kt`、新增 `OcrRequestGate.kt`、`README.md`、`docs/wiki/README.md`、本记录；重要资料补入现有 `D:\BaiduSyncdisk\其它\系统秘钥\ClearScreen\广告识别算法与复用说明.md`，保留其他窗口所有现有修改。

### 2026-11-03 09:35:48 +08:00 — 修正无障碍失效后首页仍显示工作中

- 将“工作中”的条件从启用列表、进程与服务运行标记扩展为新鲜的实时可用性检查：验证净屏自身的新界面事件送达、系统读取/手势能力声明及自身当前窗口根节点刷新。检查不点击、不截图、不执行 OCR、不保存页面文字，不把近期没有广告或成功记录当作故障。
- 前台每轮结束约 3 秒后复核；返回应用和恢复焦点重新检查。服务断开/能力缺失显示“需要修复”，超时、遮挡或无法确认显示“状态待确认”；暂停、失焦、中断、解绑、销毁或撤回同意作废旧结果。单次原生等待上限 1.8 秒、结果有效期 8 秒，阻塞探测不重复排队，过期/迟到结果不能恢复绿色状态。
- 轻量状态读取只更新无障碍字段，保留其他权限值；完整权限读取不覆盖更新的健康结果。首页、服务权限页与权限引导使用一致的判断，并保留系统设置恢复入口。保留其他窗口新增的广告学习及既有系统清理修改。
- 验证：`npx tsc --noEmit`、`:app:compileDebugKotlin --offline --console=plain` 和 `git diff --check` 通过。未运行自动化/功能测试、未操作手机或应用验收，未打包或安装。本检查仅验证事件和界面读取链路，实际广告点击与本次失效场景仍待用户真机验证。
- 涉及文件：`App.tsx`、`src/native/ClearScreenNative.ts`、`src/screens/HomeScreen.tsx`、`PermissionsScreen.tsx`、`ServicePermissionsScreen.tsx`、`ClearScreenAccessibilityHealth.kt`、`ClearScreenAccessibilityService.kt`、`ClearScreenNativeModule.kt`、`README.md`、`docs/wiki/README.md`、`docs/wiki/权限状态核验.md`、本修改记录。

### 2026-11-03 09:35:01 +08:00 — 首页新增手动学习关闭广告

- 在 Android 首页状态卡“今日处理”右侧加入“学习关闭”图标按钮，学习中可取消，同一入口可清除本地学习规则；按钮仅显示名称，不附功能描述小字。学习告知使用现有 AppDialog。
- 原生服务增加90秒学习会话，期间暂停常规自动点击，监听手动关闭/跳过按钮；没有可访问节点时提供无障碍悬浮“选取位置”，转发用户所选的一次点按。只有同应用内目标消失及页面/视觉区域变化后才保存，失败、取消、切应用或未确认不保存。
- 最多80条规则保存在现有私有存储；记录应用更新时间、按钮身份/位置、界面哈希，以及无节点路径所需的量化颜色特征/视觉指纹，不保存原始界面文字、截图或触摸轨迹，不上传。自动重放核对应用版本、根区域、页面及按钮；视觉位置另需Android 11及以上、同窗两帧及按钮/背景/周边特征匹配。保留排除应用、系统确认、敏感输入、业务控件、假叉号、遮挡和重复点击保护；手势完成不直接算关闭成功。
- 取消、超时、服务中断/解绑/销毁及撤回同意会结束学习；清除学习可独立删除规则，恢复默认保留规则并关闭总开关。隐私政策版本更新至2026-11-03-v1，升级后需重新同意但不清除原数据；线上公开政策本轮未发布，发布前需同步。
- 涉及文件：`src/screens/HomeScreen.tsx`、`App.tsx`、`src/native/ClearScreenNative.ts`、`ClearScreenAdLearning.kt`、`ClearScreenAccessibilityService.kt`、`ClearScreenNativeModule.kt`、`ClearScreenStore.kt`、三份无障碍服务XML配置、`ClearScreenPrivacyConsentStore.kt`、`src/privacy.ts`、`src/screens/PrivacyPolicyScreen.tsx`、`README.md`、`docs/wiki/README.md`、`docs/wiki/隐私政策.md`、`docs/wiki/应用商店合规核查.md`、本修改记录。复用说明增补到系统秘钥目录已有的`ClearScreen/广告识别算法与复用说明.md`，未重复保存密钥或用户规则。
- 验证：`npx tsc --noEmit`、`:app:compileDebugKotlin :app:processDebugResources --offline --console=plain`、`git diff --check`通过。未执行自动化/功能测试，未启动或操作应用，未做真机/UI验收，未打包、安装或发布。保留其他窗口现有修改。

## 2026-10-29

### 2026-10-29 20:38:12 +08:00 — 构建并覆盖安装 1.0.59

- 按用户要求将当前源码的 Android 版本更新至 `1.0.59` / versionCode `65`，保持应用包名和正式发布签名；安装包保存为 `android/app/build/outputs/apk/净屏.apk`。
- 在 vivo V2502DA 上覆盖安装成功，未卸载或清除数据；安装后系统显示 `1.0.59` / versionCode `65`，首次安装时间仍为 `2026-09-20 15:33:13`。
- 验证：`:app:assembleRelease --offline --console=plain`、`git diff --check`、APK 包名/版本/标签和正式签名核验通过。安装包与手机安装后的文件 SHA-256 均为 `8382A1519B8C7EF39EA65A0AD55179BF2C5868342EFEDECF5BFA10D101786FDB`；未执行功能测试或 UI 验收。
- 涉及文件：`app.json`、`android/app/build.gradle`、`README.md`、`docs/wiki/README.md`、本修改记录。

### 2026-10-29 16:03:31 +08:00 — 扩展系统广告与营销通知处理

- 按用户选择接入可选 Shizuku：用户主动授权后可扫描候选营销通知渠道，逐项打开系统渠道页核对；系统诊断输出可能包含通知正文，代码仅提取候选渠道元数据，不展示、保存或上传正文，也不自动关闭渠道。未匹配诊断格式时报告失败。
- 加入按品牌的系统、资讯与商店参考路径；vivo/iQOO 的部分系统广告、全局搜索、负一屏、浏览器和商店开关可由用户逐项启动无障碍辅助，在目标页面和开关明确匹配、连续采样且未遮挡时最多操作一次并回读。其他品牌及未匹配开关仍需手动处理，不能保证覆盖全部广告。
- 加入当前品牌允许清单内广告组件与快应用项目的逐项检查、关闭和恢复；Shizuku 操作前同步保存原状态，操作后回读。vivo 使用可读取时的快应用服务开关；未知状态不执行，无恢复记录不自动恢复。撤回同意或卸载会删除恢复记录但不会自动恢复系统设置。
- 隐私政策更新至 `2026-10-29-v2`，补充 Shizuku、通知渠道与系统设置辅助处理范围；公开政策页仍需发布前同步。随 Android 应用保留 Android Ad Cleaner 和 Shizuku API 的 MIT 许可文本。
- 验证：`npx tsc --noEmit`、`:app:compileDebugKotlin :app:processDebugResources --offline --console=plain` 和 `git diff --check` 通过；未运行应用或真机功能测试，未打包或安装。
- 涉及文件：`App.tsx`、`src/screens/SystemCleanupScreen.tsx`、`SystemCleanupExtras.tsx`、`PermissionsScreen.tsx`、`PrivacyPolicyScreen.tsx`、`src/native/ClearScreenNative.ts`、`src/privacy.ts`、`android/app/build.gradle`、`android/app/proguard-rules.pro`、Android 清单与 `strings.xml`、`IClearScreenPrivilegedService.aidl`、`ClearScreenCleanupRules.kt`、`ClearScreenCleanupController.kt`、`ClearScreenPrivilegedService.kt`、`ClearScreenUiCleanup.kt`、`ClearScreenAccessibilityService.kt`、`ClearScreenNativeModule.kt`、`ClearScreenSettingsNavigator.kt`、`ClearScreenPrivacyConsentStore.kt`、`ClearScreenStore.kt`、`android/app/src/main/assets/licenses/`、`README.md`、`docs/wiki/README.md`、`docs/wiki/隐私政策.md`、`docs/wiki/应用商店合规核查.md`、本修改记录。

### 2026-10-29 15:34:21 +08:00 — 新增系统广告设置与营销通知管理

- 在 Android 设置中加入“系统广告与营销通知”页面：可打开系统隐私设置，并按可从桌面打开的应用进入 Android 通知设置；系统广告选项和通知开关由用户自行确认，不读取通知内容/状态、不自动修改或挂起系统组件。
- 更新隐私政策至 `2026-10-29-v1`，说明用户主动跳转通知设置时所选应用包名的使用范围；保留本机规则、记录和设置，更新后需重新同意当前政策。公开隐私政策页面尚未同步，发布前需更新并核验。
- 验证：`npx tsc --noEmit`、`:app:compileDebugKotlin :app:processDebugResources --offline --console=plain`、`git diff --check` 通过。未运行应用/真机功能测试，未打包或安装。
- 涉及文件：`App.tsx`、`src/screens/SystemCleanupScreen.tsx`、`src/screens/SettingsScreen.tsx`、`src/native/ClearScreenNative.ts`、`src/privacy.ts`、`src/screens/PrivacyPolicyScreen.tsx`、`android/app/src/main/java/com/clearscreen/prototype/backend/ClearScreenNativeModule.kt`、`ClearScreenSettingsNavigator.kt`、`ClearScreenPrivacyConsentStore.kt`、`README.md`、`docs/wiki/README.md`、`docs/wiki/隐私政策.md`、`docs/wiki/应用商店合规核查.md`、本修改记录。

## 2026-10-27

### 2026-10-27 10:09:27 +08:00 — 更新 1.0.58 软件著作权申请材料

- 按当前源码整理 Android 1.0.58 / versionCode 64 的程序鉴别材料与用户操作手册；程序 PDF 使用 `ClearScreenAccessibilityService.kt` 首尾各 30 页、每页 50 行，共 60 页，手册为 5 页、每页 30 条说明。保留原始代码内容，仅作页码、行号与排版。
- 将 1.0.58 两份 PDF 归档到系统秘钥目录，与历史 1.0.46 材料并存；更新重要资料索引及商店合规页。负责人已确认本次著作权人、独立开发/原始取得、开发完成日期和未发表状态；内置浏览器不可用，本次未上传材料、未继续申请表或提交。
- 验证：`app.json` 与 `android/app/build.gradle` 均核实为 1.0.58 / versionCode 64；PDF 页数分别为 60 与 5，程序材料逐页检查为 50 条并覆盖源码行 1–1500、2753–4252，手册逐页检查为 30 条并渲染目视检查通过。依据《计算机软件著作权登记办法》第十条检查程序和文档行数要求。未执行应用测试或构建。
- 涉及文件：`docs/wiki/README.md`、`docs/wiki/应用商店合规核查.md`、本修改记录；外部归档：`D:\BaiduSyncdisk\其它\系统秘钥\ClearScreen\软件著作权申请材料\`、`验证与复用说明.md`。

### 2026-10-27 09:38:29 +08:00 — 延伸启动页背景至底部手势导航区域

- 将 Android 导航栏改为透明并关闭系统对比遮罩，使边缘到边缘布局下启动页背景可以延伸至底部手势区域；同步记录共享页面的底部背景规则。
- 验证：`git diff --check`、`:app:processDebugResources --offline --console=plain` 通过；未运行功能测试、未启动应用、未做真机视觉验收、未打包或安装。
- 涉及文件：`android/app/src/main/res/values/styles.xml`、`README.md`、`docs/wiki/README.md`、本修改记录。

## 2026-10-25

### 2026-10-25 14:56:12 +08:00 — 更新邓白氏案件跟进资料

- 根据负责人提供的邓白氏邮件，将案件标记为“身份资料查询已受理、等待处理”；邮件中的编码栏为空，因此没有把跟踪编号或案件编号误记为已分配编码。
- 将资料目录内的申请记录改为中文文件名 `邓白氏编码申请记录.md`，补入跟踪编号、案件编号、申请人自定义申请键及提交时间；同步更新项目基本信息与 Wiki 入口索引。
- 验证：核对原邮件内容、中文记录文件及 Wiki 引用；`git diff --check` 通过。未执行应用测试。
- 涉及文件：`docs/wiki/README.md`、`docs/wiki/CHANGELOG.md`；外部资料 `D:\BaiduSyncdisk\其它\系统秘钥\ClearScreen\项目基本信息.md`、`验证与复用说明.md`、`邓白氏编码申请记录.md`。

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

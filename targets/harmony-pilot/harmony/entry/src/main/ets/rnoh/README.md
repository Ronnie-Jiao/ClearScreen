# RNOH 宿主接入清单（hostTemplate=rnoh 骨架）

本目录由 `expo-harmony prebuild --host-template rnoh`（或 `--with-rnoh-host` /
`harmony.extension.hostTemplate: "rnoh"`）生成，属于受管文件骨架：**接入 RNOH 宿主时请勿直接手改
本 README，宿主代码建议按下文“升级路径”管理**。

> 验证边界（如实说明）：CLI/生成器/受管保护由自动化测试覆盖；“RNOH 宿主完整运行 + HAP
> 构建 + 真机验收”属于**需 DevEco Studio / HarmonyOS 真机**验证的环节，本仓库不承诺已通过
> 实机验收——详见 docs/architecture.md 的验收矩阵与 acceptance/capability-board.md。

## 需要接入/对齐的宿主内容（比照 RNOH 官方脚手架与上游 @expo-harmony/template）

1. **EntryAbility 桥接**：当前 `entry/src/main/ets/entryability/EntryAbility.ets` 是“工程就绪壳”
   （加载静态页）。接入 RNOH 后需改为 RNOH 宿主实现：RNOHCoreContext 初始化、
   `windowStage.loadContent` 加载 RNOH 页面/入口，与 `@rnoh/react-native-openharmony` 的
   宿主示例对齐（本项目不代造未经编译验证的宿主代码）。
2. **bundle 加载**：`expo-harmony bundle` 产出 `resources/rawfile/bundle.harmony.js`（release）
   或 Metro（debug，`EXPO_METRO_TARGET=harmony`），由 RNOH 宿主按 rawfile/URL 加载。
3. **注册保持受管**：`RNOHPackagesFactory.ets/.h`、`autolinking.cmake` 由 `expo-harmony`
   管理（注册片段来自 content/autolink.json v2，仅装配注册表提供的逐字行），接入后请继续用
   `expo-harmony sync / prebuild --host-template rnoh` 维护，不要手改（漂移保护会拦截）。

## 升级路径（shell → rnoh，两条都行）

- **路径 A（推荐，受管优先）**：直接用 `prebuild --host-template rnoh` 生成骨架 →
  按 RNOH 官方脚手架把宿主代码对照补齐到受管文件对应位置 → 需要“托管”自研宿主片段时，
  把它们加入 `harmony.extension.protectedFiles`（转为用户托管：不再被覆盖、不漂移误报、
  `--clean` 保留），再 `sync --force` 一次完成移交。
- **路径 B（用户托管优先）**：先按 RNOH 官方脚手架/上游生成完整宿主工程替换 EntryAbility 等 →
  把这些文件加入 `harmony.extension.protectedFiles` 声明 → 运行 `prebuild --host-template rnoh`
  只补齐 autolink 注册与依赖基线，不碰受保护文件。

## 首次验证步骤（DevEco / 真机）

1. `ohpm install`（在 harmony/ 下）安装 `@rnoh/react-native-openharmony@0.84.1` 等 HAR 依赖；
2. DevEco Studio 打开 harmony/，对齐本机 SDK/hvigor 版本提示；
3. debug：起 Metro（8081）后运行 entry；release：`expo-harmony bundle` 后构建 signed HAP 冷启动；
4. 验收记录写入 `harmony.extension.evidence` 或 `.expo-harmony/evidence.json`（doctor 会并入报告）。

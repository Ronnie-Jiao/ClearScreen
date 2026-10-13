# HarmonyOS（RNOH）开发指南

> 由 expo-harmony（0.1.1）生成。本文件属于可重建的受管产物，可用 `expo-harmony init` 重新生成。

本项目是 Expo SDK 55（React Native 0.83）驱动的 React Native 工程，通过 expo-harmony 工具链在
HarmonyOS / OpenHarmony（RNOH 0.84）上运行。同一套业务代码覆盖 iOS / Android / HarmonyOS 三端，
全程沿用 Expo CNG 工作流：配置在 `app.json`，原生工程 `harmony/` 是生成物。

## 版本基线

| 项 | iOS / Android | HarmonyOS |
| --- | --- | --- |
| React Native | react-native@0.83.6（Expo SDK 55 指定） | @react-native-oh/react-native-harmony@0.84.1（RNOH） |
| React | react@19.2.0 | react-harmony（npm alias → react@19.2.3） |
| 原生构建 | CocoaPods / Gradle | OHPM + Hvigor（@rnoh/react-native-openharmony@0.84.1） |
| HarmonyOS SDK | - | compatibleSdkVersion 23 / targetApiVersion 24（`app.json` 的 `harmony` 块可覆盖） |

双 React 共存原理：一个 bundle 只能有一个 react 实例。Harmony 打包时 Metro 通过
`EXPO_METRO_TARGET=harmony` 开启分流，把 `react` 导入重定向到 `react-harmony`；
iOS/Android 打包时该规则不生效。业务代码无需条件导入。

## 常用命令

```sh
pnpm doctor:harmony    # 环境 + 配置 + 能力分级诊断；退出码 0 通过 / 1 失败 / 2 仅警告
pnpm prebuild:harmony  # 从 app.json 重新生成 harmony/（CNG）
pnpm check:harmony     # 校验 harmony/ 与配置是否同步（无差异退出 0，有差异退出 2）
pnpm sync:harmony      # 仅重写受管 autolinking 注册文件（漂移时需 --force）
pnpm bundle:harmony    # EXPO_METRO_TARGET=harmony 导出 JS bundle 到 harmony rawfile
```

## app.json 的 harmony 配置

```jsonc
{
  "expo": {
    "platforms": ["ios", "android", "harmony"],
    "plugins": ["expo-router", "expo-harmony"],
    "harmony": {
      "bundleName": "com.example.app",   // 必填，prebuild 前置检查
      "label": "示例",
      "icon": "./assets/icon.svg",        // HarmonyOS 图标建议 SVG
      "versionCode": 1,
      "permissions": [                    // 写入 entry/src/main/module.json5
        { "name": "ohos.permission.CAMERA", "reason": "$string:camera_reason", "usedScene": { "abilities": ["EntryAbility"], "when": "inuse" } }
      ],
      "signingConfigFile": "./signing.json" // 可选：签名材料（勿入库），支持环境变量注入
    }
  }
}
```

`expo-harmony` 插件建议放在 plugins 数组末尾。权限的 `$string:` reason 会在 prebuild 时自动生成
对应字符串资源。

## 原生依赖与 autolinking

- 业务依赖请用 `expo-harmony install <包名>` / `uninstall` 管理（内部走兼容表，自动追加 RNOH 适配包
  与 patch），不要直接用 `expo install` / `pnpm add` 处理需要鸿蒙原生的包。
- `expo-harmony list` 查看兼容表；`scan --apply` 对账已装依赖。
- 受管注册文件：`harmony/entry/src/main/ets/RNOHPackagesFactory.ets`、
  `harmony/entry/src/main/cpp/RNOHPackagesFactory.h`、`harmony/entry/src/main/cpp/autolinking.cmake`。
  被手工修改后 `sync`/`prebuild --check` 会阻断（漂移保护），还原或加 `--force`。
- 自定义原生 Package 请注册在 `PackageProvider.ets` / `PackageProvider.cpp`（归用户管理，工具不覆盖）。
- 手工 `ohpm install` 安装的 HAR：`oh-package.json5` 采用“保留手工依赖、重写基线键”的合并策略，
  不会丢你加的东西。

## RNOH 宿主接入（首次真机运行前必读）

当前 `harmony/` 由 prebuild 生成的是**工程就绪壳**：可在 DevEco Studio 中构建安装，显示“就绪页”，
用于验证 bundleName/图标/权限/模块声明链路。要让页面真正跑 React Native，需要按以下方式之一接入
RNOH 原生宿主（RNOHCoreContext + EntryAbility 桥接）：

1. 以 RNOH 官方脚手架（`@react-native-oh/react-native-harmony-cli`）或上游
   `@expo-harmony/template` 生成的宿主工程为对照，把 EntryAbility/页面替换为 RNOH 宿主实现；
2. 把生成物与自动注册文件（RNOHPackagesFactory 等）对齐后，重新执行
   `expo-harmony prebuild` 保持受管状态一致；
3. DevEco 侧执行 `ohpm install` 安装 `@rnoh/react-native-openharmony` 等 HAR 依赖。

> 状态说明：v0.1 交付的 CLI/诊断/生成器已通过自动化测试；“RNOH 宿主完整接入 + HAP 真机验收”
> 属需 DevEco/真机的验证步骤，见仓库 `docs/architecture.md` 的验收矩阵与 roadmap。

## 签名

`harmony/` 是生成物（`--clean` 会整体删除），签名材料请放在外部 JSON（字段与 DevEco
signingConfigs 一致：`certpath/storeFile/profile/storePassword/keyAlias/keyPassword/signAlg`），
用 `app.config.js` 按环境变量注入：

```js
module.exports = ({ config }) => {
  const file = process.env.EXPO_HARMONY_SIGNING_CONFIG_FILE;
  return { ...config, harmony: { ...config.harmony, ...(file ? { signingConfigFile: file } : {}) } };
};
```

## 常见问题

- `ohpm: command not found`：把 DevEco 的 `ohpm/bin` 加入 PATH（或设置 `DEVECO_HOME`）。
- 真机无法加载 bundle：确保 Metro（8081）运行、设备网络可达，必要时 `hdc rport tcp:8081 tcp:8081`。
- 受管文件被改导致阻断：还原或 `expo-harmony sync --force`（先备份）。
- 依赖或 shim 更新后白屏：清 Metro 缓存（`HARMONY_METRO_CLEAR=1` 后重启 Metro）。

详细说明见 expo-harmony 仓库 `docs/guide.md`、`docs/support-matrix.md`、`docs/architecture.md`。

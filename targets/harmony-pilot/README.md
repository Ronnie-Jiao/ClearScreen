# 净屏 HarmonyOS 隔离试验

本目录由 `@oh-expo/expo-harmony@0.1.1` 生成，用于验证 React Native 页面能否与 HarmonyOS 工程隔离复用。它不是净屏鸿蒙安装包。

## 当前结论

- 已生成 Expo SDK 55 / React Native 0.83.6 / RNOH 0.84.1 的独立工程骨架。
- `harmony/` 只有 ArkUI 静态壳页和 RNOH 注册骨架；React Native 宿主和净屏共享页面均未接入。
- 依赖安装遇到版本冲突：RNOH 0.84.1 要求 React Native 0.84.1，而模板声明 0.83.6。未使用忽略 peer 检查的方式强行安装。
- 本机缺少 DevEco Studio、ohpm 和 hdc；未构建或安装 HAP。
- 净屏现有 Android/iOS 工程及依赖未升级。主工程 TypeScript 检查排除此目录。

## 继续条件

纯 React Native/RNOH 的隔离尝试和共享 UI 验证现已移至 [`../harmony-bare-pilot`](../harmony-bare-pilot/README.md)。本 Expo 骨架仅保留作版本冲突记录，不继续作为页面复用路线。

无障碍广告处理仍需单独实现 HarmonyOS `AccessibilityExtensionAbility`，并由用户在系统设置授权。安装 DevEco Studio/SDK 后构建 HAP，再用鸿蒙模拟器或真机验收；没有这些证据前，不宣称鸿蒙功能可用。

## 相关文件

- `app.json`：本试验的 Expo / Harmony 配置。
- `harmony/`：可重建的原生工程骨架。
- `docs/HARMONY.md`：脚手架生成的配置说明和 RNOH 接入边界。

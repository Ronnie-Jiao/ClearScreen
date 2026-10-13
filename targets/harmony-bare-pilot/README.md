# 净屏 HarmonyOS React Native 隔离试验

本目录用 RNOH 官方 0.84 工程模板验证净屏现有 React Native 页面能否在 HarmonyOS 工程中复用。它与主应用依赖隔离，不是可发布或可用的鸿蒙版本。

## 当前实现

- 直接导入主工程 `src/screens/`、`src/data.ts`、`src/theme.ts` 和 `src/assets.ts`；Metro 只额外监听主工程的 `src/` 与 `assets/`，不复制页面源码。
- 预览首页、应用、设置三个页面；应用开关和记录保留选项只在预览内存中变化，不写入正式数据。
- `expo-linear-gradient` 在这个试验里映射到 React Native `View`，用首个颜色代替渐变，避免把 Expo 原生模块带进 RNOH。阴影、渐变和系统栏边距仍须真机验收。
- 无障碍服务、后台保活、应用列表、诊断、隐私存储和广告处理都没有实现。首页保持“需要开启服务”的未授权状态，不模拟鸿蒙系统授权或真实运行结果。
- 工程骨架来自 RNOH 官方 `0.84-main` 分支 `packages/template/template`；模板中的 React Native 与 RNOH 均为 `0.84.1`。官方 npm 初始化器当前引用的默认模板地址未能从 npm 获取，因此使用官方源码仓库内的模板文件进行隔离试验。模板许可见 [`LICENSE.RNOH`](LICENSE.RNOH)，上游版本为 `f1ed468c5427ca92fcd4d0b79c0073cd3fad3b11`。

## 验证边界

- `npm install`、TypeScript 检查和 Harmony Metro JS bundle 检查只证明依赖/共享页面可被打包，不等于 HAP 构建或真机运行。
- 验证结果：TypeScript 检查通过，Jest 1 项通过；Harmony Metro bundle 成功生成（970,803 字节）并导出 13 个资源文件（合计 402,037 字节）。`npm audit --omit=dev` 当前报告 8 项中危、25 项高危；这是隔离试验依赖，未经安全审查和升级前不得用于发布。
- 这台电脑当前缺少 DevEco Studio、ohpm、hvigorw 和 hdc；尚不能构建 HAP、运行鸿蒙模拟器或安装真机。
- RNOH 官方开发说明要求 DevEco Studio，并以 HarmonyOS NEXT 模拟器或真机完成运行验证。用户授权、签名和系统无障碍服务还需独立接入。

## 后续步骤

1. 安装 DevEco Studio 与匹配的 HarmonyOS SDK、ohpm 和 HDC。
2. 安装项目 npm 依赖，先通过 Harmony Metro 打包检查。
3. 在 DevEco Studio 构建并启动此 UI 预览，核对共享页面的布局、图片、导航和交互。
4. UI 验收后，再单独设计 ArkTS 原生能力层与 AccessibilityExtensionAbility；未经系统授权不得运行。

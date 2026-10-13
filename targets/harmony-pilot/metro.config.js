'use strict';

/**
 * Metro 配置：HarmonyOS 平台分流。
 *
 * - 常规打包（iOS/Android/Web）：EXPO_METRO_TARGET 非 harmony 时，
 *   withHarmonyConfig 原样返回配置，不加载任何 RNOH 依赖 —— 其他平台同事无需鸿蒙工具链。
 * - Harmony 打包：`expo-harmony` 相关命令会自动设置 EXPO_METRO_TARGET=harmony，
 *   此时启用 RNOH resolver，并把 `react` 重定向到 `react-harmony`
 *   （= react@19.2.3，与 RNOH 0.84 渲染层配套），实现双 React 共存。
 */

const { getDefaultConfig } = require('expo/metro-config');

// expo-harmony 未安装（发布前以 git 依赖 / @oh-expo scope 安装）时优雅降级。
let withHarmonyConfig = (config) => config;
for (const moduleName of ['@oh-expo/expo-harmony/metro-config', 'expo-harmony/metro-config']) {
  try {
    ({ withHarmonyConfig } = require(moduleName));
    break;
  } catch (err) {
    // 尝试下一个
  }
}
if (process.env.EXPO_METRO_TARGET === 'harmony' && withHarmonyConfig === undefined) {
  console.warn('[expo-harmony] 未找到 expo-harmony/metro-config，Harmony 打包将不生效');
}

const projectRoot = __dirname;
const isHarmony = process.env.EXPO_METRO_TARGET === 'harmony';
const config = getDefaultConfig(projectRoot);

module.exports = withHarmonyConfig(config, {
  enabled: isHarmony,
  projectRoot,
  aliases: {
    // RNOH 0.84 与 Expo SDK 55 的 React 渲染层版本不同：harmony 打包时指向 react-harmony。
    react: 'react-harmony',
  },
});

/**
 * Copyright (c) 2025 Huawei Technologies Co., Ltd.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

const {mergeConfig, getDefaultConfig} = require('@react-native/metro-config');
const path = require('node:path');
const {
  createHarmonyMetroConfig,
} = require('@react-native-oh/react-native-harmony/metro.config');

/**
 * @type {import("metro-config").MetroConfig}
 */
const repositoryRoot = path.resolve(__dirname, '../..');
const config = {
  watchFolders: [
    path.join(repositoryRoot, 'src'),
    path.join(repositoryRoot, 'assets'),
  ],
  resolver: {
    disableHierarchicalLookup: true,
    nodeModulesPaths: [path.join(__dirname, 'node_modules')],
    resolveRequest(context, moduleName, platform) {
      if (moduleName === 'expo-linear-gradient') {
        return context.resolveRequest(
          context,
          path.join(__dirname, 'LinearGradientShim.tsx'),
          platform,
        );
      }
      return context.resolveRequest(context, moduleName, platform);
    },
  },
  transformer: {
    getTransformOptions: async () => ({
      transform: {
        experimentalImportSupport: false,
        inlineRequires: true,
      },
    }),
  },
};

module.exports = mergeConfig(
  getDefaultConfig(__dirname),
  createHarmonyMetroConfig({
    reactNativeHarmonyPackageName: '@react-native-oh/react-native-harmony',
  }),
  config,
);


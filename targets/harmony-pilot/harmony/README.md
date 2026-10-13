# expo-harmony 生成的 HarmonyOS 工程壳

本目录由 `expo-harmony prebuild` 从 `app.json`（CNG 配置驱动）生成，是可重建的生成物。

- 修改配置请编辑项目根目录 `app.json` 的 `harmony` 块，再运行 `expo-harmony prebuild`。
- 受管文件被手工修改后，`sync` / `prebuild --check` 会提示漂移，可用 `--force` 覆盖。
- 签名材料（证书/keystore/profile）不要放在本目录内，`--clean` 会整体删除本目录；
  请在 `app.json` 的 `harmony.signingConfigFile` 指向外部 JSON（支持从环境变量注入）。
- 首次接入 RNOH 原生宿主（RNOHCoreContext / EntryAbility 桥接）时，请按项目根目录
  `docs/HARMONY.md` 的“RNOH 宿主接入”一节完成，随后由 `expo-harmony` 维护 autolinking
  注册文件（`RNOHPackagesFactory.ets/.h`、`autolinking.cmake`）。

版本基线：Expo SDK 55 / React Native 0.83 / RNOH 0.84（双 React 共存），详见 docs/HARMONY.md。

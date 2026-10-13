import { UIAbility, Want, AbilityConstant } from '@kit.AbilityKit';
import { window } from '@kit.ArkUI';
import { hilog } from '@kit.PerformanceAnalysisKit';

/**
 * 由 expo-harmony 生成的壳 EntryAbility。
 *
 * 当前形态：加载静态“就绪页”以验证 CNG 链路（bundleName/图标/权限/模块声明）可在 DevEco 中构建运行。
 *
 * RNOH 宿主接入（运行 React Native / Expo bundle）时：
 *   1. 按项目根目录 docs/HARMONY.md 的“RNOH 宿主接入”一节，把 EntryAbility 改为 RNOH
 *      (RNOHCoreContext + ReactNativeInstance) 宿主，或从 RNOH 官方生成物对齐；
 *   2. 保持本文件由 expo-harmony 受管（不要手改后期待 prebuild 覆盖），
 *      或在 app.json 通过 harmony 扩展配置关闭本文件的再生成。
 */
const TAG = 'EntryAbility';

export default class EntryAbility extends UIAbility {
  onCreate(want: Want, launchParam: AbilityConstant.LaunchParam): void {
    hilog.info(0x0000, TAG, 'expo-harmony shell onCreate');
  }

  onWindowStageCreate(windowStage: window.WindowStage): void {
    windowStage.loadContent('pages/Index', (err) => {
      if (err.code) {
        hilog.error(0x0000, TAG, 'loadContent failed: %{public}s', JSON.stringify(err));
        return;
      }
      hilog.info(0x0000, TAG, 'loadContent succeeded');
    });
  }
}

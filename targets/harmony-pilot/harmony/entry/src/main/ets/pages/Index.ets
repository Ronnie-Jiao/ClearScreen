import { hilog } from '@kit.PerformanceAnalysisKit';

/**
 * 由 expo-harmony 生成的“工程就绪页”。
 * 用于在接入 RNOH 宿主前，先验证 CNG 链路：bundleName、图标、权限、模块声明均可被 DevEco 构建安装。
 */
const TAG = 'Index';

@Entry
@Component
struct Index {
  @State appLabel: string = 'ClearScreen';
  @State bundleName: string = 'com.example.clearscreen';

  build() {
    Column({ space: 16 }) {
      Text('expo-harmony')
        .fontSize(34)
        .fontWeight(FontWeight.Bold)
        .fontColor('#0A59F7')
      Text('工程已就绪 · Expo SDK 55 基线')
        .fontSize(16)
        .fontColor('#333333')
      Divider()
        .color('#EEEEEE')
        .margin({ top: 8, bottom: 8 })
      Text(`App: ${this.appLabel}`)
        .fontSize(14)
        .fontColor('#666666')
      Text(`Bundle: ${this.bundleName}`)
        .fontSize(14)
        .fontColor('#666666')
      Text('接入 RNOH 宿主后，此页将被 Expo bundle 页面替换。')
        .fontSize(12)
        .fontColor('#999999')
        .textAlign(TextAlign.Center)
        .margin({ top: 24 })
    }
    .width('100%')
    .height('100%')
    .justifyContent(FlexAlign.Center)
    .padding(24)
    .onAppear(() => {
      hilog.info(0x0000, TAG, 'shell page appeared');
    })
  }
}

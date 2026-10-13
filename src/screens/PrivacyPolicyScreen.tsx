import React from 'react';
import { Linking, Platform, Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { Background, Card, PrimaryButton, TopBar } from '../components/UI';
import { PRIVACY_POLICY_URL } from '../privacy';
import { C } from '../theme';

type Section = { title: string; body: string };
const ANDROID_SECTIONS: Section[] = [
  {
    title: '收集和使用的信息',
    body: '同意后，净屏读取可启动应用的名称、包名和图标，用于应用列表和规则设置。开启无障碍服务后，净屏读取当前应用标识及相关界面控件；Android 11 及以上可能截取当前界面并在本机进行文字识别。仅在你开启总开关并为该应用开启规则后，才尝试处理广告控件。截图和识别出的界面文字不会保存。',
  },
  {
    title: '本地记录和诊断',
    body: '应用规则和处理记录保存在本机。记录可能包含应用包名、规则编号、处理结果和时间，默认保留 7 天，可在设置中选择关闭、1 天、7 天或 30 天，最多保存 5000 条。故障诊断还会读取设备品牌、安装来源、净屏自身的安装/更新时间、签名证书指纹及无障碍状态，仅在本机显示。',
  },
  {
    title: '第三方组件和网络',
    body: '净屏使用 Google ML Kit 中文文字识别，图片和识别结果在本机处理，不发送给 Google。Google 说明该组件可能联系 Google 服务器获取组件更新或兼容信息，并发送性能和使用指标。当前正式版不声明 Android 互联网权限；尚未对所有设备上的 SDK 网络行为进行运行时验证，因此按第三方组件可能处理诊断指标进行披露。净屏不接入账号、广告、独立统计、推送或支付服务。',
  },
  {
    title: '保存、删除和你的选择',
    body: '数据保存在 Android 应用私有空间，不提供云端同步，且不参与系统备份。应用规则保留到你删除规则、撤回同意或卸载应用；处理记录按上方周期自动清理。你可以在设置中清空记录，或撤回同意并删除本机规则、记录和设置。撤回后净屏停止读取其他应用界面；Android 系统中的无障碍开关由你在系统设置中管理，净屏不会代替你修改。',
  },
  {
    title: '未成年人保护',
    body: '净屏不是专门面向未满14周岁儿童设计的产品。如需处理未满14周岁未成年人的个人信息，应依法取得其父母或其他监护人的同意。监护人如需帮助，请通过下方邮箱联系。',
  },
  {
    title: '开发者和联系',
    body: '深圳市曜构智能科技有限公司（Shenzhen LumaForge Technologies Co., Ltd.）。如需查询、更正或删除个人信息，或提出隐私问题，请联系 2256764885@qq.com。',
  },
];

const IOS_SECTIONS: Section[] = [
  {
    title: 'Safari 网页处理',
    body: '只有你在 Safari 设置中启用净屏扩展、在扩展菜单中开启过滤并允许访问相应网站后，扩展才会处理该 Safari 网页。扩展会在本机临时检查网页中的可访问文字、控件标识和布局，只尝试点击明确标记为广告的跳过或关闭控件；不截屏、不使用 OCR，也不操作其他 iPhone App。',
  },
  {
    title: '保存和共享',
    body: '扩展开关保存在 Safari 扩展的本机存储中。净屏不保存或上传网页内容、网址、浏览历史或识别结果，不建立广告过滤服务器，也不接入账号、广告、统计、推送或支付服务。Safari 自身负责保存你授予的网站访问权限。',
  },
  {
    title: '关闭和撤回权限',
    body: '你可以在 Safari 的净屏扩展面板中关闭过滤，也可以在 iPhone 系统设置的 Safari 扩展设置中停用扩展或撤销网站访问权限。关闭或撤销后，扩展不再处理相应网页。',
  },
  {
    title: '未成年人保护',
    body: '净屏不是专门面向未满14周岁儿童设计的产品。如需处理未满14周岁未成年人的个人信息，应依法取得其父母或其他监护人的同意。监护人如需帮助，请通过下方邮箱联系。',
  },
  {
    title: '开发者和联系',
    body: '深圳市曜构智能科技有限公司（Shenzhen LumaForge Technologies Co., Ltd.）。如需提出隐私问题，请联系 2256764885@qq.com。',
  },
];

export default function PrivacyPolicyScreen({
  mode,
  onBack,
  onAccept,
  onDecline,
}: {
  mode: 'consent' | 'read';
  onBack: () => void;
  onAccept?: () => void;
  onDecline?: () => void;
}) {
  const openFullPolicy = () => Linking.openURL(PRIVACY_POLICY_URL);
  const sections = Platform.OS === 'ios' ? IOS_SECTIONS : ANDROID_SECTIONS;
  return (
    <Background>
      <View style={s.root}>
        {mode === 'read' ? (
          <TopBar title="隐私政策" onBack={onBack} />
        ) : (
          <Text style={s.pageTitle}>隐私政策</Text>
        )}
        <ScrollView contentContainerStyle={s.content} showsVerticalScrollIndicator={false}>
          <Text style={s.intro}>
            {mode === 'consent'
              ? '请先阅读隐私政策。你同意后，净屏才会读取应用列表、无障碍状态或其他应用界面。'
              : Platform.OS === 'ios'
                ? 'iOS Safari 扩展说明 · 2026年10月13日'
                : '版本 2026-10-13-v1 · 2026年10月13日生效'}
          </Text>
          {sections.map(section => (
            <Card key={section.title} style={s.card}>
              <Text style={s.sectionTitle}>{section.title}</Text>
              <Text style={s.body}>{section.body}</Text>
            </Card>
          ))}
          {Platform.OS === 'android' ? (
            <Pressable accessibilityRole="link" onPress={openFullPolicy} style={s.linkButton}>
              <Text style={s.link}>打开公开版隐私政策</Text>
            </Pressable>
          ) : null}
        </ScrollView>
        {mode === 'consent' ? (
          <View style={s.actions}>
            <PrimaryButton title="同意并继续" onPress={onAccept || (() => {})} />
            <Pressable accessibilityRole="button" onPress={onDecline} style={s.decline}>
              <Text style={s.declineText}>拒绝并退出</Text>
            </Pressable>
          </View>
        ) : null}
      </View>
    </Background>
  );
}

const s = StyleSheet.create({
  root: { flex: 1, paddingHorizontal: 18 },
  pageTitle: { fontSize: 28, fontWeight: '900', color: C.navy, paddingVertical: 16 },
  content: { paddingBottom: 20 },
  intro: { fontSize: 15, lineHeight: 23, fontWeight: '700', color: C.text, marginBottom: 12 },
  card: { padding: 16, marginBottom: 10 },
  sectionTitle: { fontSize: 17, fontWeight: '900', color: C.navy, marginBottom: 8 },
  body: { fontSize: 14, lineHeight: 22, color: C.text },
  linkButton: { alignSelf: 'center', paddingVertical: 14, paddingHorizontal: 18 },
  link: { color: C.blue, fontSize: 15, fontWeight: '800', textDecorationLine: 'underline' },
  actions: { gap: 8, paddingTop: 8, paddingBottom: 10 },
  decline: { minHeight: 44, alignItems: 'center', justifyContent: 'center' },
  declineText: { color: C.text2, fontSize: 15, fontWeight: '800' },
});

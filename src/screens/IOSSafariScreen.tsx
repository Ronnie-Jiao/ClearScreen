import React, { useState } from 'react';
import { Platform, Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { Background, Badge, BrandHeader, Card, Chevron, PrimaryButton, SquareIcon } from '../components/UI';
import { C } from '../theme';
import { getIOSURLFilterReadiness, getIOSURLFilterReadinessLabel } from '../platform/ios/urlFilter';

export default function IOSSafariScreen({
  onPrivacyPolicy,
}: {
  onPrivacyPolicy: () => void;
}) {
  const [stepsVisible, setStepsVisible] = useState(false);
  const urlFilterReadiness = getIOSURLFilterReadiness(Platform.Version);

  return (
    <Background>
      <View style={s.root}>
        <ScrollView contentContainerStyle={s.content} showsVerticalScrollIndicator={false}>
          <BrandHeader tagline={false} />
          <Card style={s.systemFilter}>
            <View style={s.heroHeading}>
              <SquareIcon symbol="↗" size={58} />
              <View style={s.heroCopy}>
                <Text style={s.title}>系统广告过滤</Text>
                <Badge green={false}>{getIOSURLFilterReadinessLabel(urlFilterReadiness)}</Badge>
              </View>
            </View>
            <View style={s.statusRows}>
              <View style={s.statusRow}>
                <Text style={s.statusLabel}>最低系统版本</Text>
                <Text style={s.statusValue}>iOS 26</Text>
              </View>
              <View style={[s.statusRow, s.lastStatusRow]}>
                <Text style={s.statusLabel}>过滤方式</Text>
                <Text style={s.statusValue}>广告网址拦截</Text>
              </View>
            </View>
          </Card>

          <Card style={s.hero}>
            <View style={s.heroHeading}>
              <SquareIcon symbol="S" size={58} />
              <View style={s.heroCopy}>
                <Text style={s.title}>Safari 网页广告过滤</Text>
                <Badge green={false}>Safari 扩展</Badge>
              </View>
            </View>
            <Text style={s.body}>
              扩展只在 Safari 网页中运行。你可以在 Safari 扩展菜单中开启或暂停广告跳过与关闭。
            </Text>
          </Card>

          <PrimaryButton
            title={stepsVisible ? '收起开启步骤' : '查看开启步骤'}
            onPress={() => setStepsVisible(value => !value)}
          />

          {stepsVisible ? (
            <Card style={s.steps}>
              <Text style={s.step}>1. 在系统设置的 Safari 扩展中打开“净屏”。</Text>
              <Text style={s.step}>2. 在 Safari 网页菜单中选择“净屏”，允许访问当前网站。</Text>
              <Text style={s.step}>3. 打开扩展开关；未授权的网站不会处理。</Text>
            </Card>
          ) : null}

          <Card style={s.scope}>
            <View style={s.scopeHeading}>
              <Text style={s.scopeTitle}>功能范围</Text>
              <Chevron />
            </View>
            <Text style={s.body}>
              仅尝试点击明确的广告跳过/关闭控件。网页文字只在本机临时检查，不保存或上传；不处理其他 iPhone App 内的广告。
            </Text>
          </Card>

          <Pressable accessibilityRole="button" onPress={onPrivacyPolicy} style={s.policyRow}>
            <Text style={s.policyText}>隐私政策</Text>
            <Chevron />
          </Pressable>
        </ScrollView>
      </View>
    </Background>
  );
}

const s = StyleSheet.create({
  root: { flex: 1 },
  content: { padding: 18, paddingBottom: 28 },
  systemFilter: { padding: 18, gap: 16, marginBottom: 14 },
  hero: { padding: 18, gap: 16, marginBottom: 14 },
  heroHeading: { flexDirection: 'row', alignItems: 'center', gap: 14 },
  heroCopy: { flex: 1, gap: 8 },
  title: { color: C.navy, fontSize: 21, lineHeight: 27, fontWeight: '900' },
  body: { color: C.text2, fontSize: 15, lineHeight: 23, fontWeight: '600' },
  statusRows: { paddingHorizontal: 4 },
  statusRow: { minHeight: 46, flexDirection: 'row', alignItems: 'center', borderBottomWidth: 1, borderBottomColor: C.line },
  lastStatusRow: { borderBottomWidth: 0 },
  statusLabel: { flex: 1, color: C.text2, fontSize: 15, fontWeight: '700' },
  statusValue: { color: C.navy, fontSize: 15, fontWeight: '900' },
  steps: { marginTop: 12, padding: 16, gap: 10 },
  step: { color: C.text, fontSize: 15, lineHeight: 23, fontWeight: '700' },
  scope: { marginTop: 14, padding: 16, gap: 10 },
  scopeHeading: { minHeight: 36, flexDirection: 'row', alignItems: 'center' },
  scopeTitle: { flex: 1, color: C.navy, fontSize: 18, fontWeight: '900' },
  policyRow: {
    minHeight: 62,
    marginTop: 12,
    paddingHorizontal: 16,
    borderRadius: 18,
    backgroundColor: 'rgba(255,255,255,.94)',
    flexDirection: 'row',
    alignItems: 'center',
  },
  policyText: { flex: 1, color: C.navy, fontSize: 17, fontWeight: '900' },
});

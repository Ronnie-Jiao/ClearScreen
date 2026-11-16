import React, { useState } from 'react';
import { ScrollView, StyleSheet, Text, View } from 'react-native';
import { Background, Card, SquareIcon, Toggle, TopBar } from '../components/UI';
import { C } from '../theme';

export default function AppearanceScreen({ onBack }: { onBack: () => void }) {
  const [motion, setMotion] = useState(true);

  return (
    <Background>
      <ScrollView contentContainerStyle={s.page} showsVerticalScrollIndicator={false}>
        <TopBar title="外观" onBack={onBack} />

        <Card style={s.group}>
          <Text style={s.head}>显示模式</Text>
          <View style={s.modeRow}>
            <View style={s.sun}><Text style={s.sunGlyph}>☀</Text></View>
            <Text style={s.label}>白天模式</Text>
            <Text style={s.active}>已启用</Text>
          </View>
        </Card>

        <Card style={s.group}>
          <Text style={s.head}>界面设置</Text>
          <View style={s.row}>
            <SquareIcon symbol="▦" tone="blue" size={52} />
            <Text style={s.label}>图标风格</Text>
            <View style={s.segment}><Text style={[s.seg, s.segOn]}>圆角</Text><Text style={s.seg}>线性</Text></View>
          </View>
          <View style={s.row}>
            <SquareIcon symbol="□" tone="green" size={52} />
            <Text style={s.label}>卡片圆角</Text>
            <View style={s.segment}><Text style={s.seg}>小</Text><Text style={[s.seg, s.segOn]}>中</Text><Text style={s.seg}>大</Text></View>
          </View>
          <View style={s.row}>
            <SquareIcon symbol="✦" tone="purple" size={52} />
            <Text style={s.label}>动效</Text>
            <Toggle value={motion} onChange={() => setMotion(!motion)} />
          </View>
        </Card>

        <Card style={s.group}>
          <Text style={s.head}>预览效果</Text>
          <View style={s.preview}>
            <Text style={s.mockBrand}>◈ 净屏</Text>
            <View style={s.mockCard}><Text style={s.mockCheck}>✓</Text><Toggle value /></View>
            <View style={s.mockLine} />
            <View style={[s.mockLine, { width: '72%' }]} />
            <Text style={s.caption}>白天模式</Text>
          </View>
        </Card>
      </ScrollView>
    </Background>
  );
}

const s = StyleSheet.create({
  page: { paddingHorizontal: 18, paddingBottom: 36 },
  group: { padding: 16, marginBottom: 14 },
  head: { fontSize: 21, fontWeight: '900', color: C.navy, marginBottom: 16 },
  modeRow: { minHeight: 72, flexDirection: 'row', alignItems: 'center', gap: 14 },
  sun: { width: 52, height: 52, borderRadius: 16, alignItems: 'center', justifyContent: 'center', backgroundColor: '#E8F6FF' },
  sunGlyph: { color: C.blue, fontSize: 30, fontWeight: '900' },
  label: { fontSize: 19, fontWeight: '900', color: C.navy, flex: 1 },
  active: { color: C.blue, fontSize: 15, fontWeight: '900' },
  row: { minHeight: 96, flexDirection: 'row', alignItems: 'center', gap: 14, borderBottomWidth: 1, borderBottomColor: C.line },
  segment: { flexDirection: 'row', backgroundColor: '#EEF4FB', borderRadius: 14, padding: 3 },
  seg: { paddingHorizontal: 12, paddingVertical: 9, fontWeight: '800', color: '#5C7598' },
  segOn: { backgroundColor: C.blue, color: '#FFFFFF', borderRadius: 11, overflow: 'hidden' },
  preview: { minHeight: 250, borderRadius: 18, backgroundColor: '#EAF8FE', padding: 16 },
  mockBrand: { fontSize: 20, fontWeight: '900', color: C.navy },
  mockCard: { height: 88, borderRadius: 18, backgroundColor: '#FFFFFF', marginTop: 16, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', padding: 12 },
  mockCheck: { width: 46, height: 46, textAlign: 'center', textAlignVertical: 'center', borderRadius: 23, backgroundColor: '#17BCCC', color: '#FFFFFF', fontSize: 30, fontWeight: '900', overflow: 'hidden' },
  mockLine: { height: 44, borderRadius: 14, backgroundColor: '#FFFFFF', marginTop: 10 },
  caption: { textAlign: 'center', marginTop: 18, fontSize: 17, fontWeight: '900', color: '#6D84A7' },
});

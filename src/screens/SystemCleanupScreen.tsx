import React from 'react';
import { Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { Background, Card, Chevron, TopBar } from '../components/UI';
import { C, COMPACT_UI } from '../theme';

export default function SystemCleanupScreen({
  onBack,
  onOpenSystemRecommend,
  onOpenAppNotifications,
}: {
  onBack: () => void;
  onOpenSystemRecommend: () => void;
  onOpenAppNotifications: () => void;
}) {
  return (
    <Background>
      <ScrollView contentContainerStyle={s.page} showsVerticalScrollIndicator={false} keyboardShouldPersistTaps="handled">
        <TopBar compact title="系统广告" onBack={onBack} />

        <Card compact style={s.card}>
          <Pressable accessibilityRole="button" onPress={onOpenSystemRecommend} style={[s.row, s.rowPlain]}>
            <View style={s.rowText}>
              <Text style={s.rowTitle}>系统推荐开关</Text>
            </View>
            <Chevron compact />
          </Pressable>
        </Card>

        <Card compact style={s.card}>
          <Pressable accessibilityRole="button" onPress={onOpenAppNotifications} style={[s.row, s.rowPlain]}>
            <View style={s.rowText}>
              <Text style={s.rowTitle}>应用通知</Text>
            </View>
            <Chevron compact />
          </Pressable>
        </Card>
      </ScrollView>
    </Background>
  );
}

const s = StyleSheet.create({
  page: { paddingHorizontal: 18, paddingBottom: 36 },
  card: { paddingHorizontal: COMPACT_UI.cardPadding, paddingTop: 4, paddingBottom: 4, marginBottom: 12 },
  row: { minHeight: 54, flexDirection: 'row', alignItems: 'center', gap: 10, borderBottomWidth: 1, borderBottomColor: C.line, paddingVertical: 8 },
  rowPlain: { borderBottomWidth: 0 },
  rowText: { flex: 1 },
  rowTitle: { fontSize: COMPACT_UI.body, fontWeight: '800', color: C.text },
});

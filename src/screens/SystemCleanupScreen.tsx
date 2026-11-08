import React, { useCallback, useEffect, useState } from 'react';
import { AppState, Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { AppItem } from '../data';
import { Background, Badge, Card, Chevron, TopBar } from '../components/UI';
import { ClearScreenNative, type CleanupAccess } from '../native/ClearScreenNative';
import { C, COMPACT_UI } from '../theme';

export default function SystemCleanupScreen({
  apps,
  onBack,
  onOpenSystemRecommend,
  onOpenAppNotifications,
  onOpenAdbAccess,
  onOpenQuickComponents,
  onOpenMarketingChannels,
}: {
  apps: AppItem[];
  onBack: () => void;
  onOpenSystemRecommend: () => void;
  onOpenAppNotifications: () => void;
  onOpenAdbAccess: () => void;
  onOpenQuickComponents: () => void;
  onOpenMarketingChannels: () => void;
}) {
  const launchableCount = apps.filter(app => Boolean(app.packageName)).length;

  const [access, setAccess] = useState<CleanupAccess>({ running: false, granted: false });
  const refreshAccess = useCallback(async () => {
    try { setAccess(await ClearScreenNative.getCleanupAccessStatus()) } catch { setAccess({ running: false, granted: false }) }
  }, []);
  useEffect(() => {
    void refreshAccess();
    const listener = AppState.addEventListener('change', state => {
      if (state === 'active') void refreshAccess();
    });
    return () => listener.remove();
  }, [refreshAccess]);

  const connected = access.adbConnected || (access.running && access.granted);
  const accessLabel = connected ? '已连接' : '未连接';

  return (
    <Background>
      <ScrollView contentContainerStyle={s.page} showsVerticalScrollIndicator={false} keyboardShouldPersistTaps="handled">
        <TopBar compact title="系统广告与营销通知" onBack={onBack} />

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
              <Text style={s.rowTitle}>关闭应用通知</Text>
              <Text style={s.rowDesc}>本机 {launchableCount} 个应用，可逐个关掉通知。</Text>
            </View>
            <Chevron compact />
          </Pressable>
        </Card>

        <Card compact style={s.card}>
          <Pressable accessibilityRole="button" onPress={onOpenAdbAccess} style={[s.row, s.rowPlain]}>
            <View style={s.rowText}>
              <Text style={s.rowTitle}>直接改系统设置</Text>
            </View>
            <Badge compact green={connected}>{accessLabel}</Badge>
            <Chevron compact />
          </Pressable>
        </Card>

        <Card compact style={s.card}>
          <Pressable accessibilityRole="button" onPress={onOpenMarketingChannels} style={[s.row, s.rowPlain]}>
            <View style={s.rowText}>
              <Text style={s.rowTitle}>营销通知</Text>
            </View>
            <Chevron compact />
          </Pressable>
        </Card>

        <Card compact style={s.card}>
          <Pressable accessibilityRole="button" onPress={onOpenQuickComponents} style={[s.row, s.rowPlain]}>
            <View style={s.rowText}>
              <Text style={s.rowTitle}>快应用与系统广告组件</Text>
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
  rowDesc: { marginTop: 2, fontSize: COMPACT_UI.caption, fontWeight: '600', color: C.text2 },
});

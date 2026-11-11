import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { AppState, Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { AppItem } from '../data';
import { useAppAlert } from '../components/AppDialog';
import { Background, Card, Toggle, TopBar } from '../components/UI';
import { ClearScreenNative, type MarketingChannel, type NotificationAccessStatus } from '../native/ClearScreenNative';
import { C, COMPACT_UI } from '../theme';

type ScanMode = 'full' | 'observed';

function hasPrivilegedRead(status: { running?: boolean; granted?: boolean; adbConnected?: boolean } | null): boolean {
  return Boolean(status && (status.adbConnected || (status.running && status.granted)));
}

export default function MarketingChannelsScreen({ apps, onBack, onOpenServicePermissions }: {
  apps: AppItem[]; onBack: () => void; onOpenServicePermissions: () => void;
}) {
  const showAlert = useAppAlert();
  const [access, setAccess] = useState<NotificationAccessStatus>({ supported: true, granted: false });
  const [privileged, setPrivileged] = useState(false);
  const [mode, setMode] = useState<ScanMode>('observed');
  const [channels, setChannels] = useState<MarketingChannel[]>([]);
  const [channelsScanned, setChannelsScanned] = useState(false);
  const [busy, setBusy] = useState('');
  const [accessLoaded, setAccessLoaded] = useState(false);
  const autoScanStarted = useRef(false);
  const promptShown = useRef(false);
  const refreshChannelsOnResume = useRef(false);
  const appNames = useMemo(() => new Map(apps.filter(app => app.packageName).map(app => [app.packageName!, app.name])), [apps]);
  const refreshAccess = useCallback(async () => {
    const [notification, cleanup] = await Promise.all([
      ClearScreenNative.getNotificationAccessStatus().catch(() => ({ supported: false, granted: false })),
      ClearScreenNative.getCleanupAccessStatus().catch(() => null),
    ]);
    setAccess(notification);
    setPrivileged(hasPrivilegedRead(cleanup));
    setAccessLoaded(true);
  }, []);
  const fail = useCallback((title: string, error: unknown) =>
    showAlert(title, error instanceof Error ? error.message : String(error), undefined, { tone: 'error' }), [showAlert]);
  const openAccessSettings = useCallback(() => {
    void ClearScreenNative.openNotificationAccessSettings().catch(error => fail('无法打开系统设置', error));
  }, [fail]);
  const requireAccess = useCallback(() => {
    if (!access.supported) {
      showAlert('这台手机不支持按渠道读取', '当前系统版本没有通知渠道接口。仍可以到系统设置里逐个应用关闭通知。', [
        { text: '知道了', style: 'cancel' },
      ], { tone: 'warning' });
      return;
    }
    showAlert('还没有读取通知渠道的方式', '未连接时只能看到正在发通知的渠道，结果不完整；系统也不允许净屏列出其他应用的全部渠道。可以授予「通知使用权」，或连接服务后读取完整清单。', [
      { text: '取消', style: 'cancel' },
      { text: '去授予权限', onPress: openAccessSettings },
      { text: '去连接', onPress: onOpenServicePermissions },
    ], { tone: 'warning' });
  }, [access.supported, onOpenServicePermissions, openAccessSettings, showAlert]);
  const scanChannels = useCallback(async () => {
    if (busy) return;
    try {
      setBusy('channels');
      const cleanup = await ClearScreenNative.getCleanupAccessStatus().catch(() => null);
      const full = hasPrivilegedRead(cleanup);
      setPrivileged(full);
      setMode(full ? 'full' : 'observed');
      setChannels(await ClearScreenNative.scanMarketingChannels());
      setChannelsScanned(true);
    } catch (error) { setChannels([]); setChannelsScanned(false); fail('扫描失败', error) }
    finally { setBusy('') }
  }, [busy, fail]);
  const openChannelSettings = useCallback(async (channel: MarketingChannel) => {
    if (busy) return;
    refreshChannelsOnResume.current = true;
    try { await ClearScreenNative.openMarketingChannel(channel.packageName, channel.id) }
    catch (error) {
      refreshChannelsOnResume.current = false;
      fail('无法打开渠道设置', error);
    }
  }, [busy, fail]);
  useEffect(() => {
    void refreshAccess();
    const listener = AppState.addEventListener('change', state => {
      if (state === 'active') {
        void refreshAccess();
        if (refreshChannelsOnResume.current) {
          refreshChannelsOnResume.current = false;
          void scanChannels();
        }
      }
    });
    return () => listener.remove();
  }, [refreshAccess, scanChannels]);
  useEffect(() => {
    if (!accessLoaded) return;
    if (!privileged && !access.granted) {
      autoScanStarted.current = false;
      if (!promptShown.current) {
        promptShown.current = true;
        requireAccess();
      }
      return;
    }
    promptShown.current = false;
    if (autoScanStarted.current || busy) return;
    autoScanStarted.current = true;
    void scanChannels();
  }, [access.granted, accessLoaded, busy, privileged, requireAccess, scanChannels]);

  const observed = !privileged;
  return (
    <Background>
      <ScrollView contentContainerStyle={s.page} showsVerticalScrollIndicator={false}>
        <TopBar compact title="营销通知" onBack={onBack} />
        {observed ? <Text style={s.note}>未连接服务时只列出正在发通知或刚发生变更的渠道，结果不完整；连接后可读取完整清单。</Text> : null}
        {observed ? <Pressable accessibilityRole="button" accessibilityLabel="去服务与权限连接" disabled={!!busy}
          onPress={onOpenServicePermissions} style={s.actionRow}>
          <Text style={s.action}>去连接补全</Text>
        </Pressable> : null}
        <Card compact style={s.card}>
          {busy === 'channels' ? <Text accessibilityLiveRegion="polite" style={s.empty}>正在扫描…</Text> : null}
          {channelsScanned && !channels.length ? (
            <Text style={s.empty}>{mode === 'full' ? '未发现候选渠道' : '未连接，暂未观察到营销渠道'}</Text>
          ) : null}
          {channels.map(channel => {
            const appName = appNames.get(channel.packageName) || '未知应用';
            const stateLabel = channel.state === 'on' ? '已开启' : channel.state === 'off' ? '已关闭' : '状态未知';
            const channelName = channel.name?.trim();
            const title = channelName ? `${channelName} · ${appName}` : `${appName} · 营销渠道`;
            const meta = mode === 'full' ? channel.group?.trim() : '';
            return <View key={`${channel.packageName}:${channel.id}`} style={s.row}>
              <Pressable accessibilityRole="button" accessibilityLabel={`打开${title}通知设置`}
                disabled={!!busy} onPress={() => { void openChannelSettings(channel) }} style={s.rowText}>
                <Text style={s.rowTitle}>{title}</Text>{meta ? <Text numberOfLines={1} style={s.rowStatus}>{meta}</Text> : null}
              </Pressable>
              <Toggle compact value={channel.state === 'on'} unknown={channel.state === 'unknown'}
                accessibilityLabel={`${title}，${stateLabel}；点按打开系统通知设置`}
                onChange={() => { void openChannelSettings(channel) }} />
            </View>;
          })}
        </Card>
      </ScrollView>
    </Background>
  );
}

const s = StyleSheet.create({
  page: { paddingHorizontal: 18, paddingBottom: 36 },
  note: { fontSize: COMPACT_UI.body, color: C.text2, lineHeight: 20, marginBottom: 6 },
  actionRow: { alignSelf: 'flex-start', paddingVertical: 6, marginBottom: 8 },
  action: { fontSize: COMPACT_UI.body, fontWeight: '800', color: C.text },
  card: { paddingHorizontal: COMPACT_UI.cardPadding, paddingTop: 4, paddingBottom: 4, marginBottom: 12 },
  empty: { paddingVertical: 22, textAlign: 'center', fontSize: COMPACT_UI.body, fontWeight: '700', color: C.text },
  row: { minHeight: 54, flexDirection: 'row', alignItems: 'center', gap: 10, borderBottomWidth: 1, borderBottomColor: C.line, paddingVertical: 8 },
  rowText: { flex: 1 },
  rowTitle: { fontSize: COMPACT_UI.body, fontWeight: '800', color: C.text },
  rowStatus: { fontSize: COMPACT_UI.body, color: C.text, marginTop: 3, lineHeight: 20 },
});

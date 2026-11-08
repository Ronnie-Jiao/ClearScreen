import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { AppState, Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { AppItem } from '../data';
import { useAppAlert } from '../components/AppDialog';
import { Background, Card, Chevron, TopBar } from '../components/UI';
import { ClearScreenNative, type CleanupAccess, type MarketingChannel } from '../native/ClearScreenNative';
import { C, COMPACT_UI } from '../theme';

export default function MarketingChannelsScreen({ apps, onBack }: { apps: AppItem[]; onBack: () => void }) {
  const showAlert = useAppAlert();
  const [access, setAccess] = useState<CleanupAccess>({ running: false, granted: false });
  const [channels, setChannels] = useState<MarketingChannel[]>([]);
  const [channelsScanned, setChannelsScanned] = useState(false);
  const [busy, setBusy] = useState('');
  const appNames = useMemo(() => new Map(apps.filter(app => app.packageName).map(app => [app.packageName!, app.name])), [apps]);
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

  const fail = (title: string, error: unknown) =>
    showAlert(title, error instanceof Error ? error.message : String(error), undefined, { tone: 'error' });
  const connected = access.adbConnected || (access.running && access.granted);
  const openAccessibility = () => {
    void ClearScreenNative.openAccessibilitySettings().catch(error => fail('无法打开系统设置', error));
  };
  /** All on this phone: read the pairing code via the accessibility service and pair in the background. */
  const startPhonePairing = async () => {
    try {
      setBusy('pair');
      const adb = await ClearScreenNative.getAdbAccessStatus();
      if (!adb.supported) {
        showAlert('这一档需要你自己关', '这台手机是 Android 10 或更早的系统，没有「无线调试」，净屏没法自己拿到改系统开关的权限。这些开关只能到系统设置里手动关。', [
          { text: '知道了', style: 'cancel' },
          { text: '去系统设置', onPress: () => { void ClearScreenNative.openSystemAdPrivacySettings().catch(error => fail('无法打开系统设置', error)) } },
        ], { tone: 'warning' });
        return;
      }
      const health = await ClearScreenNative.getAccessibilityHealth().catch(() => null);
      if (health && !health.permissions.skip) {
        showAlert('还差一步：开启自动跳过服务', '净屏要靠它在你打开配对弹窗时读出配对码。开启后再回来扫描渠道，就能自己连上。', [
          { text: '取消', style: 'cancel' },
          { text: '去开启', onPress: openAccessibility },
        ], { tone: 'warning' });
        return;
      }
      await ClearScreenNative.startAdbAutoPair();
      showAlert('去系统里点两下', '现在净屏会自动读出配对码，不用抄。打开「开发者选项」→「无线调试」，点亮开关，再点「使用配对码配对设备」；配对弹窗出现后净屏会自己连上。连好后回到净屏，会自动开始扫描。', [
        { text: '稍后', style: 'cancel' },
        { text: '去开发者选项', onPress: () => { void ClearScreenNative.openDeveloperOptions().catch(error => fail('无法打开系统设置', error)) } },
      ], { tone: 'info' });
    } catch (error) {
      fail('无法开始配对', error);
    } finally {
      setBusy('');
    }
  };
  const requireAccess = () => {
    if (connected) return true;
    showAlert('先让净屏连上这台手机', '不用电脑，也不用数据线：打开「无线调试」，用配对码连一次，之后就能在这里扫描和核对通知渠道。', [
      { text: '取消', style: 'cancel' },
      { text: '现在连上', onPress: () => { void startPhonePairing() } },
    ], { tone: 'warning' });
    return false;
  };
  const scanChannels = () => {
    if (!requireAccess() || busy) return;
    showAlert('扫描通知渠道？', '净屏会临时读取系统里的通知设置，找出可能是广告推送的通知渠道；读取过程中可能读到通知内容，但不会展示、保存或上传。请逐项核对候选渠道。', [
      { text: '取消', style: 'cancel' },
      { text: '扫描', onPress: () => { void (async () => {
        try {
          setBusy('channels');
          setChannels(await ClearScreenNative.scanMarketingChannels());
          setChannelsScanned(true);
        } catch (error) { setChannels([]); setChannelsScanned(false); fail('扫描失败', error) }
        finally { setBusy('') }
      })() } },
    ]);
  };

  return (
    <Background>
      <ScrollView contentContainerStyle={s.page} showsVerticalScrollIndicator={false}>
        <TopBar compact title="营销通知" onBack={onBack} />
        <Card compact style={s.card}>
          <Text style={s.body}>候选渠道需逐项核对；建议保留订单、物流、安全和验证码提醒。</Text>
          <Pressable accessibilityRole="button" disabled={!!busy} onPress={scanChannels} style={s.action}>
            <Text style={s.actionText}>{busy === 'channels' ? '扫描中…' : channelsScanned ? '重新扫描渠道' : '扫描渠道'}</Text>
          </Pressable>
          {channelsScanned && !channels.length ? <Text style={s.empty}>未发现候选渠道</Text> : null}
          {channels.map(channel => <Pressable key={`${channel.packageName}:${channel.id}`} accessibilityRole="button"
            onPress={() => showAlert('打开通知渠道', `请确认系统页面显示“${channel.name}”。如果只显示应用通知总开关，请返回，避免关闭重要通知。`, [
              { text: '取消', style: 'cancel' },
              { text: '打开', onPress: () => { void ClearScreenNative.openMarketingChannel(channel.packageName, channel.id).catch(error => fail('无法打开渠道设置', error)) } },
            ], { tone: 'warning' })} style={s.row}>
            <View style={s.rowText}><Text style={s.rowTitle}>{channel.name}</Text><Text style={s.rowStatus}>{appNames.get(channel.packageName) || '未知应用'}</Text></View>
            <Chevron compact />
          </Pressable>)}
        </Card>
      </ScrollView>
    </Background>
  );
}

const s = StyleSheet.create({
  page: { paddingHorizontal: 18, paddingBottom: 36 },
  card: { paddingHorizontal: COMPACT_UI.cardPadding, paddingTop: 4, paddingBottom: 4, marginBottom: 12 },
  body: { fontSize: COMPACT_UI.body, lineHeight: 21, color: C.text, marginTop: 8 },
  action: { minHeight: COMPACT_UI.buttonHeight, marginTop: 12, marginBottom: 5, paddingHorizontal: 13, borderRadius: 13, backgroundColor: '#E9F4FF', flexDirection: 'row', alignItems: 'center', justifyContent: 'center' },
  actionText: { fontSize: COMPACT_UI.body, fontWeight: '800', color: C.blue },
  empty: { paddingVertical: 22, textAlign: 'center', fontSize: COMPACT_UI.body, fontWeight: '700', color: C.text },
  row: { minHeight: 54, flexDirection: 'row', alignItems: 'center', gap: 10, borderBottomWidth: 1, borderBottomColor: C.line, paddingVertical: 8 },
  rowText: { flex: 1 },
  rowTitle: { fontSize: COMPACT_UI.body, fontWeight: '800', color: C.text },
  rowStatus: { fontSize: COMPACT_UI.body, color: C.text, marginTop: 3, lineHeight: 20 },
});

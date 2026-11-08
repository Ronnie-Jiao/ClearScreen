import React, { useCallback, useEffect, useRef, useState } from 'react';
import { AppState, Pressable, ScrollView, StyleSheet, Text, TextInput, View } from 'react-native';
import { Background, Card, Chevron, TopBar } from '../components/UI';
import { useAppAlert } from '../components/AppDialog';
import { ClearScreenNative, type AdbAccessStatus, type AdbActionResult } from '../native/ClearScreenNative';
import { C, COMPACT_UI } from '../theme';

export default function AdbAccessScreen({ onBack }: { onBack: () => void }) {
  const showAlert = useAppAlert();
  const [status, setStatus] = useState<AdbAccessStatus | null>(null);
  const [host, setHost] = useState('');
  const [connPort, setConnPort] = useState('');
  const [busy, setBusy] = useState('');
  const [advanced, setAdvanced] = useState(false);
  const [accessibilityReady, setAccessibilityReady] = useState<boolean | null>(null);
  const refresh = useCallback(async () => {
    try {
      const next = await ClearScreenNative.getAdbAccessStatus();
      setStatus(next);
      // Wireless debugging does not listen on loopback; default to the WLAN address.
      setHost(current => current || next.host || next.localAddress || '');
      if (next.port > 0) setConnPort(String(next.port));
    } catch { /* The next explicit operation will report any error. */ }
    try {
      const health = await ClearScreenNative.getAccessibilityHealth();
      setAccessibilityReady(Boolean(health.permissions.skip));
    } catch { /* Keep the last known state. */ }
  }, []);
  useEffect(() => {
    void refresh();
    const listener = AppState.addEventListener('change', state => {
      if (state === 'active') void refresh();
    });
    return () => listener.remove();
  }, [refresh]);

  const fail = (title: string, error: unknown) =>
    showAlert(title, error instanceof Error ? error.message : String(error), undefined, { tone: 'error' });
  const number = (value: string) => Number(value.trim());
  const validPort = (value: string) => /^\d{1,5}$/.test(value.trim()) && number(value) >= 1 && number(value) <= 65535;
  const run = async (label: string, action: () => Promise<AdbActionResult>, done: string) => {
    setBusy(label);
    try {
      const result = await action();
      await refresh();
      if (result.ok) showAlert('直接改系统设置', done);
      else showAlert('未连接', result.message, undefined, { tone: 'warning' });
    } catch (error) {
      fail('操作失败', error);
    } finally {
      setBusy('');
    }
  };
  const connect = () => {
    if (!validPort(connPort)) {
      showAlert('连接端口不正确', '请填写「无线调试」页面上显示的那串数字。', undefined, { tone: 'warning' });
      return;
    }
    void run('connect', () => ClearScreenNative.connectAdb(host.trim(), number(connPort)),
      '已连接，现在可以直接改系统开关了。');
  };
  const disconnect = async () => {
    setBusy('disconnect');
    try {
      await ClearScreenNative.disconnectAdb();
      await refresh();
    } catch (error) {
      fail('断开失败', error);
    } finally {
      setBusy('');
    }
  };
  const autoStage = status?.autoPairStage ?? '';
  const autoRunning = ['waiting-code', 'pairing', 'waiting-port', 'connecting'].includes(autoStage);
  useEffect(() => {
    if (!autoRunning) return;
    const timer = setInterval(() => { void refresh(); }, 1200);
    return () => clearInterval(timer);
  }, [autoRunning, refresh]);
  const startAutoPair = async () => {
    if (accessibilityReady === false) {
      showAlert('先开启自动跳过服务', '净屏要靠它在配对弹窗出现时读出配对码；不开启的话，自动配对会一直等不到配对码。', [
        { text: '取消', style: 'cancel' },
        { text: '去开启', onPress: () => { void ClearScreenNative.openAccessibilitySettings().catch(error => fail('无法打开系统设置', error)) } },
      ], { tone: 'warning' });
      return;
    }
    setBusy('auto-pair');
    try {
      setStatus(await ClearScreenNative.startAdbAutoPair());
    } catch (error) {
      fail('无法自动配对', error);
    } finally {
      setBusy('');
    }
  };
  const stopAutoPair = async () => {
    setBusy('auto-pair');
    try {
      setStatus(await ClearScreenNative.stopAdbAutoPair());
    } catch (error) {
      fail('停止失败', error);
    } finally {
      setBusy('');
    }
  };
  /** Each explanation is shown only the first time that feature is actually used. */
  const guideSeen = useRef<Set<string> | null>(null);
  useEffect(() => {
    void ClearScreenNative.readAdbGuideSeen()
      .catch(() => [])
      .then(seen => { if (guideSeen.current === null) guideSeen.current = new Set(seen); });
  }, []);
  const explainOnce = (key: string, title: string, message: string, action: () => void) => {
    const seen = guideSeen.current ?? (guideSeen.current = new Set<string>());
    if (seen.has(key)) { action(); return; }
    seen.add(key);
    void ClearScreenNative.markAdbGuideSeen(key).catch(() => undefined);
    showAlert(title, message, [{ text: '知道了', onPress: action }], { tone: 'info' });
  };
  const openDeveloperOptions = () => explainOnce(
    'developer-options', '先打开「无线调试」',
    '净屏要自己改系统开关，得借用「无线调试」。在这一页把它打开，再回到这里点下面的按钮，净屏就能连上；连上之前别把它关掉。',
    () => { void ClearScreenNative.openDeveloperOptions().catch(error => fail('无法打开开发者选项', error)); },
  );
  const findLastAccess = () => explainOnce(
    'auto-find', '接回上次的连接',
    '以前连过的话，点这里就能直接接回来，不用重新确认；接上之后马上就能改系统开关。',
    () => { void run('auto', () => ClearScreenNative.autoConnectAdb(), '已连接，现在可以直接改系统开关了。'); },
  );
  const startPairing = () => explainOnce(
    'auto-pair', '第一次连接走这里',
    '在「无线调试」页点亮开关，再选带「配对」字样的那一项；手机上弹出确认框时点一下确认，净屏会自己接上，不用你抄任何数字。连接时用的密钥只留在这台手机上，不会上传。',
    () => { void startAutoPair(); },
  );
  const toggleAdvanced = () => {
    if (advanced) { setAdvanced(false); return; }
    explainOnce(
      'advanced', '自动连接没成功再用',
      '把「无线调试」页上显示的那串数字填进来，再点「连接」；效果和自动连接一样，填错也只是连不上，不会影响手机。',
      () => setAdvanced(true),
    );
  };
  const statusText = !status ? '正在读取状态'
    : !status.supported ? '这台手机不支持无线调试配对'
      : status.connected ? '已连接'
        : '未连接';
  const guidance = !status ? '稍等一下，正在读取这台手机的状态。'
    : !status.supported ? '这台手机是 Android 10 或更早的系统，没有「无线调试」这一项。这一档净屏没法自己拿到权限，这些开关只能到系统设置里手动关。'
      : status.connected ? '已经连上，可以去改系统开关了；重启手机后需要再连一次。'
        : '还没连上。按下面的顺序点一遍就能连上。';
  const showManual = status?.supported !== false;
  const autoTitle = autoStage === 'waiting-code' ? '等待配对弹窗'
    : autoStage === 'pairing' ? '正在配对'
      : autoStage === 'waiting-port' ? '配对成功，正在读取连接端口'
        : autoStage === 'connecting' ? '正在连接'
          : autoStage === 'connected' ? '已连接'
            : autoStage === 'failed' ? (status?.autoPairMessage || '自动配对失败')
              : '未开始自动配对';

  return (
    <Background>
      <ScrollView contentContainerStyle={s.page} showsVerticalScrollIndicator={false} keyboardShouldPersistTaps="handled">
        <TopBar compact title="直接改系统设置" onBack={onBack} />

        <Text style={s.sectionTitle}>连接状态</Text>
        <Card compact style={s.card}>
          <View style={s.row}>
            <View style={s.rowText}>
              <Text style={s.rowTitle}>{statusText}</Text>
            </View>
            {status?.connected ? <Pressable accessibilityRole="button" disabled={!!busy} onPress={() => { void disconnect() }}>
              <Text style={s.link}>{busy === 'disconnect' ? '断开中…' : '断开'}</Text>
            </Pressable> : null}
          </View>
          <Text style={s.guidance}>{guidance}</Text>
          {status?.supported === false ? <Pressable accessibilityRole="button" disabled={!!busy} onPress={() => {
            void ClearScreenNative.openSystemAdPrivacySettings().catch(error => fail('无法打开系统设置', error));
          }} style={s.action}>
            <Text style={s.actionText}>去系统设置手动关</Text>
          </Pressable> : <>
            <Pressable accessibilityRole="button" disabled={!!busy} onPress={openDeveloperOptions} style={s.action}>
              <Text style={s.actionText}>打开开发者选项</Text>
            </Pressable>
          </>}
          {status?.supported && status?.paired ? <>
            <Pressable accessibilityRole="button" disabled={!!busy} onPress={findLastAccess} style={s.action}>
              <Text style={s.actionText}>{busy === 'auto' ? '查找中…' : '自动查找无线调试'}</Text>
            </Pressable>
          </> : null}
        </Card>

        {showManual ? <>
          <Text style={s.sectionTitle}>自动配对</Text>
          <Card compact style={s.card}>
            <Text style={s.rowTitle}>{autoTitle}</Text>
            {autoStage === 'waiting-code' || autoStage === 'pairing' || autoStage === 'waiting-port' || autoStage === 'connecting'
              ? <Text style={s.rowStatus}>{status?.autoPairMessage}</Text> : null}
            {autoRunning ? <Pressable accessibilityRole="button" disabled={!!busy} onPress={() => { void stopAutoPair(); }} style={s.action}>
              <Text style={s.actionText}>{busy === 'auto-pair' ? '停止中…' : '停止'}</Text>
            </Pressable> : <Pressable accessibilityRole="button" disabled={!!busy} onPress={startPairing} style={s.action}>
              <Text style={s.actionText}>{busy === 'auto-pair' ? '启动中…' : '开始自动配对'}</Text>
            </Pressable>}
          </Card>
        </> : null}

        {showManual ? <Card compact style={s.card}>
          <Pressable accessibilityRole="button" onPress={toggleAdvanced} style={[s.row, !advanced && s.rowPlain]}>
            <View style={s.rowText}>
              <Text style={s.rowTitle}>高级</Text>
            </View>
            <Text style={s.value}>{advanced ? '收起' : '展开'}</Text>
            <Chevron compact />
          </Pressable>
          {advanced ? <>
            <View style={s.field}>
              <Text style={s.label}>连接端口</Text>
              <TextInput value={connPort} onChangeText={setConnPort} keyboardType="number-pad"
                placeholder="「无线调试」页里的数字" placeholderTextColor={C.text2} style={s.input} />
            </View>
            <Pressable accessibilityRole="button" disabled={!!busy} onPress={connect} style={s.action}>
              <Text style={s.actionText}>{busy === 'connect' ? '连接中…' : '连接'}</Text>
            </Pressable>
            {status?.localAddress ? <View style={s.infoRow}>
              <Text style={s.label}>本机地址</Text>
              <Text style={s.infoValue}>{status.localAddress}</Text>
            </View> : null}
          </> : null}
        </Card> : null}
      </ScrollView>
    </Background>
  );
}

const s = StyleSheet.create({
  page: { paddingHorizontal: 18, paddingBottom: 36 },
  card: { paddingHorizontal: COMPACT_UI.cardPadding, paddingTop: 4, paddingBottom: 12, marginBottom: 12 },
  sectionTitle: { fontSize: COMPACT_UI.body, fontWeight: '800', color: C.navy, paddingHorizontal: 8, paddingBottom: 8 },
  row: { minHeight: 54, flexDirection: 'row', alignItems: 'center', gap: 10, borderBottomWidth: 1, borderBottomColor: C.line, paddingVertical: 8 },
  rowPlain: { borderBottomWidth: 0 },
  rowText: { flex: 1 },
  rowTitle: { fontSize: COMPACT_UI.body, fontWeight: '800', color: C.text },
  rowStatus: { fontSize: COMPACT_UI.body, color: C.text, marginTop: 3 },
  guidance: { fontSize: COMPACT_UI.body, lineHeight: 20, color: C.text, fontWeight: '600', marginTop: 8 },
  value: { fontSize: COMPACT_UI.body, fontWeight: '700', color: C.blue },
  infoRow: { minHeight: 54, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', gap: 10, borderBottomWidth: 1, borderBottomColor: C.line },
  infoValue: { fontSize: COMPACT_UI.body, color: C.text },
  link: { fontSize: COMPACT_UI.body, fontWeight: '800', color: C.blue, paddingHorizontal: 4 },
  action: { minHeight: COMPACT_UI.buttonHeight, marginTop: 12, paddingHorizontal: 13, borderRadius: 13, backgroundColor: '#E9F4FF', flexDirection: 'row', alignItems: 'center', justifyContent: 'center' },
  actionText: { fontSize: COMPACT_UI.body, fontWeight: '800', color: C.blue },
  field: { minHeight: 54, flexDirection: 'row', alignItems: 'center', gap: 10, borderBottomWidth: 1, borderBottomColor: C.line },
  label: { fontSize: COMPACT_UI.body, fontWeight: '800', color: C.text },
  input: { flex: 1, fontSize: COMPACT_UI.body, fontWeight: '600', marginLeft: 8, color: C.text, paddingVertical: 0, textAlign: 'right' },
});

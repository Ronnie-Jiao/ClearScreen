import React, { useCallback, useEffect, useRef, useState } from 'react';
import { ActivityIndicator, AppState, Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { useAppAlert } from '../components/AppDialog';
import { Background, Card, Toggle, TopBar } from '../components/UI';
import { ClearScreenNative, type CleanupAccess, type CleanupTarget } from '../native/ClearScreenNative';
import { C, COMPACT_UI } from '../theme';

const CHECK_TIMEOUT_MS = 30_000;
const ACCESS_TIMEOUT_MS = 8_000;

type StatusKind = 'idle' | 'working' | 'error' | 'blocked';
type Status = { kind: StatusKind; text: string };

/** A hung native call must surface as a sentence instead of an endless「检查中…」. */
const withTimeout = <T,>(work: Promise<T>, ms: number): Promise<T> =>
  new Promise<T>((resolve, reject) => {
    const timer = setTimeout(() => reject(new Error(`超过 ${Math.round(ms / 1000)} 秒没有响应`)), ms);
    work.then(
      value => { clearTimeout(timer); resolve(value) },
      error => { clearTimeout(timer); reject(error) },
    );
  });

export default function QuickComponentsScreen({ onBack }: { onBack: () => void }) {
  const showAlert = useAppAlert();
  const [access, setAccess] = useState<CleanupAccess>({ running: false, granted: false });
  const [accessKnown, setAccessKnown] = useState(false);
  const [targets, setTargets] = useState<CleanupTarget[]>([]);
  const [targetsScanned, setTargetsScanned] = useState(false);
  const [busy, setBusy] = useState('');
  const [status, setStatus] = useState<Status>({ kind: 'idle', text: '' });
  const pendingInspect = useRef(false);
  const guideSeen = useRef<boolean | null>(null);
  const guideReady = useRef<Promise<boolean> | null>(null);
  useEffect(() => {
    guideReady.current = ClearScreenNative.isQuickComponentsGuideSeen()
      .catch(() => true)
      .then(seen => { if (guideSeen.current === null) guideSeen.current = seen; return seen });
  }, []);
  /** Persist as soon as it is shown, so later checks and later taps stay quiet. */
  const markGuideSeen = () => {
    guideSeen.current = true;
    void ClearScreenNative.setQuickComponentsGuideSeen().catch(() => undefined);
  };
  const showSwitchGuide = () => {
    markGuideSeen();
    showAlert('开关怎么用', '开关打开：净屏正在帮你关掉这个组件的广告。开关关闭：这个组件恢复原样、正常启用。停用不会卸载应用，随时打开开关就能恢复。', [
      { text: '知道了' },
    ], { tone: 'info' });
  };
  const refreshAccess = useCallback(async () => {
    try { setAccess(await withTimeout(ClearScreenNative.getCleanupAccessStatus(), ACCESS_TIMEOUT_MS)) }
    catch { setAccess({ running: false, granted: false }) }
    finally { setAccessKnown(true) }
  }, []);
  useEffect(() => {
    void refreshAccess();
    const listener = AppState.addEventListener('change', state => {
      if (state === 'active') void refreshAccess();
    });
    return () => listener.remove();
  }, [refreshAccess]);

  const fail = (title: string, error: unknown) =>
    showAlert(title, explainError(error), undefined, { tone: 'error' });
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
        showAlert('还差一步：开启自动跳过服务', '净屏要靠它在你打开配对弹窗时读出配对码。开启后再回来点「检查本机组件」，就能自己连上。', [
          { text: '取消', style: 'cancel' },
          { text: '去开启', onPress: openAccessibility },
        ], { tone: 'warning' });
        return;
      }
      await ClearScreenNative.startAdbAutoPair();
      pendingInspect.current = true;
      setStatus({ kind: 'blocked', text: '等你在系统里完成配对，连上后会自动开始检查。' });
      showAlert('去系统里点两下', '现在净屏会自动读出配对码，不用抄。打开「开发者选项」→「无线调试」，点亮开关，再点「使用配对码配对设备」；配对弹窗出现后净屏会自己连上。连好后回到净屏，会自动开始检查。', [
        { text: '稍后', style: 'cancel' },
        { text: '去开发者选项', onPress: () => { void ClearScreenNative.openDeveloperOptions().catch(error => fail('无法打开系统设置', error)) } },
      ], { tone: 'info' });
    } catch (error) {
      setStatus({ kind: 'error', text: `没能开始配对：${explainError(error)}` });
      fail('无法开始配对', error);
    } finally {
      setBusy('');
    }
  };
  const askToConnect = () => {
    showAlert('先让净屏连上这台手机', '不用电脑，也不用数据线：打开「无线调试」，用配对码连一次，之后就能在这里检查、打开这些开关。', [
      { text: '取消', style: 'cancel' },
      { text: '现在连上', onPress: () => { void startPhonePairing() } },
    ], { tone: 'warning' });
  };
  const requireAccess = () => {
    if (connected) return true;
    askToConnect();
    return false;
  };
  const inspect = async () => {
    if (busy) return;
    setBusy('targets');
    setStatus({ kind: 'working', text: '正在读取本机组件…' });
    try {
      // Re-read the channel first: the cached value can be stale after the
      // system drops the wireless debugging session. A failed read is a real
      // failure, not a reason to quietly reuse the old value.
      let current: CleanupAccess;
      try {
        current = await withTimeout(ClearScreenNative.getCleanupAccessStatus(), ACCESS_TIMEOUT_MS);
      } catch (error) {
        setStatus({ kind: 'error', text: `检查没完成：${explainError(error)}` });
        void refreshAccess();
        return;
      }
      setAccess(current);
      const ready = current.adbConnected || (current.running && current.granted);
      if (!ready) {
        pendingInspect.current = true;
        setStatus({ kind: 'blocked', text: '还没连上这台手机，所以没法检查。点「现在连上」，用无线调试配对一次，连好后会自动检查。' });
        askToConnect();
        return;
      }
      const list = await withTimeout(ClearScreenNative.inspectCleanupTargets(), CHECK_TIMEOUT_MS);
      setTargets(list);
      setTargetsScanned(true);
      setStatus({ kind: 'idle', text: '' });
      // Explain the switches once, the first time the list is actually seen.
      const seen = guideSeen.current === null
        ? await (guideReady.current ?? Promise.resolve(true))
        : guideSeen.current;
      if (list.length && !seen) showSwitchGuide();
    } catch (error) {
      setStatus({ kind: 'error', text: `检查没完成：${explainError(error)}` });
      // The channel may have dropped between the status read and the scan:
      // refresh so the「现在连上」button matches what actually happened.
      void refreshAccess();
    } finally {
      setBusy('');
    }
  };
  // Pairing was started from this page: run the check as soon as the channel comes back.
  useEffect(() => {
    if (!pendingInspect.current || !connected) return;
    pendingInspect.current = false;
    void inspect();
  }, [connected]);
  const changeTarget = (target: CleanupTarget) => {
    if (busy) return;
    // First switch tap also counts as first use: explain before changing anything.
    if (guideSeen.current !== true) { showSwitchGuide(); return; }
    if (!requireAccess()) return;
    if (target.state === 'unknown') {
      showAlert('无法确认状态', '系统未提供可靠状态，本次不会更改。', undefined, { tone: 'warning' });
      return;
    }
    const restore = target.state === 'blocked';
    if (restore && !target.canRestore) {
      showAlert('没有恢复记录', '该项目没有净屏保存的原状态，无法自动恢复。', undefined, { tone: 'warning' });
      return;
    }
    showAlert(`${restore ? '恢复' : '关闭'}${target.title}？`, restore
      ? '将按净屏保存的原状态恢复此项目。'
      : '关闭可能影响快应用或锁屏画报等系统功能。原状态仅保存在净屏，撤回同意或卸载前请先恢复。', [
      { text: '取消', style: 'cancel' },
      { text: restore ? '恢复' : '关闭', onPress: () => { void (async () => {
        try {
          setBusy(target.id);
          const result = await ClearScreenNative.changeCleanupTarget(target.id, restore);
          try { setTargets(await ClearScreenNative.inspectCleanupTargets()) }
          catch { setTargets([]); setTargetsScanned(false) }
          const messages: Record<string, string> = {
            blocked: '已关闭，原状态已保存。', restored: '已恢复原状态。',
            already_blocked: '系统显示已经关闭，未作修改。', already_restored: '系统已处于原状态。',
          };
          showAlert('处理结果', messages[result.result] || '处理结束。', undefined, { tone: 'success' });
        } catch (error) {
          try { setTargets(await ClearScreenNative.inspectCleanupTargets()) } catch { /* keep the last display */ }
          setStatus({ kind: 'error', text: `${restore ? '恢复' : '关闭'}${target.title}没完成：${explainError(error)}` });
          fail('处理失败', error);
        } finally { setBusy('') }
      })() } },
    ], { tone: 'warning' });
  };
  const rows = () => {
    if (!targetsScanned) return <Text style={s.empty}>未检查 · 查完会列出本机有哪些组件</Text>;
    if (!targets.length) return <Text style={s.empty}>未检测到可处理组件</Text>;
    // Keep the previous order (quick app components first) without repeating
    // the category in a heading above every row.
    const ordered = [
      ...targets.filter(item => item.category === 'quick'),
      ...targets.filter(item => item.category === 'ads'),
    ];
    return ordered.map(item => <View key={item.id} style={s.row}>
      <Text style={s.rowTitle}>{item.title}</Text>
      <View style={busy ? s.toggleBusy : undefined} pointerEvents={busy ? 'none' : 'auto'}>
        <Toggle compact value={item.state === 'blocked'} unknown={item.state === 'unknown'} onChange={() => changeTarget(item)} />
      </View>
    </View>);
  };
  const needsConnection = accessKnown && !connected;
  const statusText = needsConnection && status.kind === 'idle'
    ? { kind: 'blocked' as StatusKind, text: '还没连上这台手机，检查前需要先连一次。' }
    : status;

  return (
    <Background>
      <ScrollView contentContainerStyle={s.page} showsVerticalScrollIndicator={false}>
        <TopBar compact title="快应用与系统广告组件" onBack={onBack} />
        <Card compact style={s.card}>
          <Text style={s.intro}>快应用和系统广告组件会在后台自启、推广告、占内存。检查只读取本机信息，不会改动任何东西；下面按本机实际情况列出可处理的组件。</Text>
          <Pressable accessibilityRole="button" disabled={!!busy} onPress={() => { void inspect() }} style={[s.action, !!busy && s.actionBusy]}>
            {busy === 'targets' ? <ActivityIndicator size="small" color={C.blue} /> : null}
            <Text style={s.actionText}>{busy === 'targets' ? '检查中…' : '检查本机组件'}</Text>
          </Pressable>
          {statusText.kind !== 'idle' ? <Text style={[s.status, statusText.kind === 'error' && s.statusError]}>{statusText.text}</Text> : null}
          {needsConnection ? <Pressable accessibilityRole="button" disabled={!!busy} onPress={() => { void startPhonePairing() }} style={[s.connect, !!busy && s.actionBusy]}>
            <Text style={s.connectText}>{busy === 'pair' ? '正在准备连接…' : '现在连上'}</Text>
          </Pressable> : null}
          {rows()}
        </Card>
      </ScrollView>
    </Background>
  );
}

/** Turn native failures into one sentence a user can act on. */
function explainError(error: unknown): string {
  const raw = (error instanceof Error ? error.message : String(error ?? '')).trim();
  if (/隐私|同意/.test(raw)) return '隐私授权已失效，请回首页重新同意隐私政策后再试。';
  if (/无障碍|自动跳过/.test(raw)) return '自动跳过服务没开，净屏读不到系统信息。请在系统设置里开启后再试。';
  if (/没有响应|超时|timeout|timed out/i.test(raw)) return '手机系统命令没有响应，连接可能已经断了。点「现在连上」重连后再试。';
  if (/Shizuku|授权通道|未连接|没有连接|请先启动|请先授予/i.test(raw)) return '还没连上这台手机，所以查不了。点「现在连上」，用无线调试配对一次再试。';
  if (!raw) return '系统没有给出原因，请重试；仍然不行就重连这台手机。';
  return raw;
}

const s = StyleSheet.create({
  page: { paddingHorizontal: 18, paddingBottom: 36 },
  card: { paddingHorizontal: COMPACT_UI.cardPadding, paddingTop: 4, paddingBottom: 12, marginBottom: 12 },
  row: { minHeight: 54, flexDirection: 'row', alignItems: 'center', gap: 10, borderBottomWidth: 1, borderBottomColor: C.line, paddingVertical: 8 },
  rowTitle: { flex: 1, fontSize: COMPACT_UI.body, fontWeight: '800', color: C.text },
  toggleBusy: { opacity: 0.45 },
  empty: { paddingVertical: 22, textAlign: 'center', fontSize: COMPACT_UI.body, fontWeight: '700', color: C.text },
  intro: { marginTop: 8, fontSize: COMPACT_UI.body, lineHeight: 21, color: C.text },
  status: { fontSize: COMPACT_UI.body, lineHeight: 21, color: C.text, marginTop: 10 },
  statusError: { color: '#DF1645' },
  action: { minHeight: COMPACT_UI.buttonHeight, marginTop: 12, marginBottom: 5, paddingHorizontal: 13, borderRadius: 13, backgroundColor: '#E9F4FF', flexDirection: 'row', alignItems: 'center', justifyContent: 'center', gap: 8 },
  actionBusy: { opacity: 0.55 },
  actionText: { fontSize: COMPACT_UI.body, fontWeight: '800', color: C.blue },
  connect: { minHeight: 40, marginTop: 8, paddingHorizontal: 13, borderRadius: 12, backgroundColor: '#E9F4FF', alignItems: 'center', justifyContent: 'center' },
  connectText: { fontSize: COMPACT_UI.body, fontWeight: '800', color: C.blue },
});

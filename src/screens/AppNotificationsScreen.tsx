import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { ActivityIndicator, AppState, FlatList, Pressable, StyleSheet, Text, TextInput, View } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { useAppAlert } from '../components/AppDialog';
import { AppIcon, Background, Card, Toggle, TopBar } from '../components/UI';
import { AppItem } from '../data';
import { ClearScreenNative, type CleanupAccess } from '../native/ClearScreenNative';
import { C, COMPACT_UI } from '../theme';

const ROW_HEIGHT = 56;
const ACCESS_TIMEOUT_MS = 8_000;
const READ_TIMEOUT_MS = 30_000;
const WRITE_TIMEOUT_MS = 30_000;

type Entry = AppItem & { packageName: string };

/** A hung native call must surface as a sentence instead of an endless「读取中…」. */
const withTimeout = <T,>(work: Promise<T>, ms: number): Promise<T> =>
  new Promise<T>((resolve, reject) => {
    const timer = setTimeout(() => reject(new Error('手机系统没有响应，请重试')), ms);
    work.then(
      value => { clearTimeout(timer); resolve(value) },
      error => { clearTimeout(timer); reject(error) },
    );
  });

export default function AppNotificationsScreen({
  apps,
  onBack,
  onOpenAccess,
}: {
  apps: AppItem[];
  onBack: () => void;
  onOpenAccess: () => void;
}) {
  const showAlert = useAppAlert();
  const [query, setQuery] = useState('');
  const [access, setAccess] = useState<CleanupAccess>({ running: false, granted: false });
  const [accessKnown, setAccessKnown] = useState(false);
  const [states, setStates] = useState<Record<string, boolean>>({});
  const [loading, setLoading] = useState(false);
  const [loadFailed, setLoadFailed] = useState(false);
  const [busyPkg, setBusyPkg] = useState('');

  const entries = useMemo(
    () => apps.filter((app): app is Entry => Boolean(app.packageName)),
    [apps],
  );
  const keyword = query.trim().toLocaleLowerCase();
  const visible = useMemo(() => {
    const matched = keyword
      ? entries.filter(app => app.name.toLocaleLowerCase().includes(keyword))
      : entries;
    // 开关打开的排在前面，方便逐个关闭还在发通知的应用；组内保持原有名称顺序。
    const group = (app: Entry) => {
      const state = states[app.packageName];
      if (state === true) return 0;
      if (state === false) return 1;
      return 2;
    };
    return matched
      .map((app, index) => ({ app, index }))
      .sort((a, b) => group(a.app) - group(b.app) || a.index - b.index)
      .map(item => item.app);
  }, [entries, keyword, states]);

  const loadStates = useCallback(async (knownAccess?: CleanupAccess) => {
    setLoading(true);
    setLoadFailed(false);
    try {
      const current = knownAccess ?? await withTimeout(ClearScreenNative.getCleanupAccessStatus(), ACCESS_TIMEOUT_MS);
      setAccess(current);
      setAccessKnown(true);
      const connected = current.adbConnected || (current.running && current.granted);
      if (!connected) {
        setStates({});
        return;
      }
      const list = await withTimeout(
        ClearScreenNative.readAppNotificationStates(entries.map(app => app.packageName)),
        READ_TIMEOUT_MS,
      );
      const next: Record<string, boolean> = {};
      list.forEach(item => { next[item.packageName] = item.enabled });
      setStates(next);
    } catch {
      setLoadFailed(true);
      setAccessKnown(true);
      // The channel can drop between the status read and the batch read.
      void ClearScreenNative.getCleanupAccessStatus()
        .then(status => { setAccess(status) })
        .catch(() => undefined);
    } finally {
      setLoading(false);
    }
  }, [entries]);

  useEffect(() => {
    void loadStates();
    const listener = AppState.addEventListener('change', state => {
      if (state === 'active') void loadStates();
    });
    return () => listener.remove();
  }, [loadStates]);

  const connected = access.adbConnected || (access.running && access.granted);

  const askToConnect = () => {
    showAlert('先让净屏连上这台手机', '通知开关保存在手机系统里，连上之后才能读取和修改。', [
      { text: '取消', style: 'cancel' },
      { text: '现在连上', onPress: onOpenAccess },
    ], { tone: 'warning' });
  };

  const change = async (app: Entry) => {
    if (busyPkg) return;
    if (!connected) { askToConnect(); return; }
    const current = states[app.packageName];
    if (current === undefined) {
      showAlert('还读不到这个应用的通知状态', '先重新读取一次，再决定是否修改。', [
        { text: '取消', style: 'cancel' },
        { text: '重新读取', onPress: () => { void loadStates() } },
      ], { tone: 'warning' });
      return;
    }
    const desired = !current;
    setBusyPkg(app.packageName);
    try {
      const result = await withTimeout(
        ClearScreenNative.setAppNotificationEnabled(app.packageName, desired),
        WRITE_TIMEOUT_MS,
      );
      // The native side writes, reads back and verifies before returning.
      setStates(prev => ({ ...prev, [app.packageName]: result === 'enabled' || result === 'already_enabled' }));
    } catch (error) {
      // Never flip the switch locally on failure: ask the system what it now reads.
      try {
        const [actual] = await ClearScreenNative.readAppNotificationStates([app.packageName]);
        if (actual) setStates(prev => ({ ...prev, [app.packageName]: actual.enabled }));
      } catch { /* keep the last confirmed display */ }
      showAlert('没有改成功', explainError(error), undefined, { tone: 'error' });
    } finally {
      setBusyPkg('');
    }
  };

  const status = () => {
    if (!accessKnown) return null;
    if (loadFailed) return <View style={s.notice}>
      <Text style={s.noticeText}>读取没完成，开关状态暂时未知。</Text>
      <Pressable accessibilityRole="button" onPress={() => { void loadStates() }} hitSlop={8}><Text style={s.noticeAction}>重试</Text></Pressable>
    </View>;
    if (loading) return <View style={s.notice}>
      <Text style={s.noticeText}>正在读取本机应用…</Text>
    </View>;
    return null;
  };

  return (
    <Background>
      <View style={s.page}>
        <TopBar compact title="关闭应用通知" onBack={onBack} />

        <Card compact style={s.searchCard}>
          <View style={s.search}>
            <Ionicons name="search-outline" size={18} color={C.text2} />
            <TextInput
              value={query}
              onChangeText={setQuery}
              placeholder="搜索应用"
              placeholderTextColor="#8296B1"
              style={s.input}
              returnKeyType="search"
              autoCorrect={false}
            />
            {query ? (
              <Pressable accessibilityRole="button" accessibilityLabel="清除搜索" onPress={() => setQuery('')} hitSlop={12}>
                <Ionicons name="close-circle" size={18} color={C.text2} />
              </Pressable>
            ) : null}
          </View>
        </Card>

        {status()}

        <Card compact style={s.listCard}>
          {loading && !entries.length ? (
            <View style={s.center}>
              <ActivityIndicator size="small" color={C.blue} />
            </View>
          ) : (
            <FlatList
              data={visible}
              keyExtractor={app => app.packageName}
              keyboardShouldPersistTaps="handled"
              showsVerticalScrollIndicator={false}
              contentContainerStyle={s.listContent}
              initialNumToRender={12}
              windowSize={7}
              removeClippedSubviews
              getItemLayout={(_, index) => ({ length: ROW_HEIGHT, offset: ROW_HEIGHT * index, index })}
              renderItem={({ item }) => {
                const state = states[item.packageName];
                const rowBusy = busyPkg === item.packageName;
                return <View style={s.row}>
                  <AppIcon source={item.icon} size={COMPACT_UI.appIcon} />
                  <Text style={s.name} numberOfLines={1}>{item.name}</Text>
                  <View style={rowBusy ? s.toggleBusy : undefined} pointerEvents={rowBusy ? 'none' : 'auto'}>
                    <Toggle compact value={state === true} unknown={state === undefined} onChange={() => { void change(item) }} />
                  </View>
                </View>;
              }}
              ListEmptyComponent={(
                <Text style={s.empty}>{keyword ? '没有找到这个应用' : '本机没有可处理的应用'}</Text>
              )}
            />
          )}
        </Card>
      </View>
    </Background>
  );
}

/** Turn native failures into one sentence a user can act on. */
function explainError(error: unknown): string {
  const raw = (error instanceof Error ? error.message : String(error ?? '')).trim();
  if (/隐私|同意/.test(raw)) return '隐私授权已失效，请回首页重新同意隐私政策后再试。';
  if (/没有响应|超时|timeout|timed out/i.test(raw)) return '手机系统命令没有响应，连接可能已经断了。去「ADB Shell 操作能力」重连后再试。';
  if (/Shizuku|授权通道|未连接|没有连接|请先启动|请先授予/i.test(raw)) return '还没连上这台手机。去「ADB Shell 操作能力」连上后再试。';
  if (!raw) return '系统没有给出原因，请重试；仍然不行就重新连接这台手机。';
  return raw;
}

const s = StyleSheet.create({
  page: { flex: 1, paddingHorizontal: 18, paddingBottom: 12 },
  searchCard: { paddingHorizontal: COMPACT_UI.cardPadding, paddingVertical: 8, marginBottom: 10 },
  search: { height: 44, borderRadius: 13, backgroundColor: '#F7FBFF', flexDirection: 'row', alignItems: 'center', paddingHorizontal: 11 },
  input: { flex: 1, fontSize: COMPACT_UI.body, fontWeight: '600', marginLeft: 8, color: C.text, paddingVertical: 0 },
  notice: { flexDirection: 'row', alignItems: 'center', gap: 8, marginBottom: 8, paddingHorizontal: 4 },
  noticeText: { flex: 1, fontSize: COMPACT_UI.caption, fontWeight: '600', color: C.text2 },
  noticeAction: { fontSize: COMPACT_UI.caption, fontWeight: '800', color: C.blue },
  listCard: { flex: 1, paddingHorizontal: COMPACT_UI.cardPadding, paddingTop: 0, paddingBottom: 4, marginBottom: 0 },
  listContent: { paddingBottom: 32 },
  row: { height: ROW_HEIGHT, flexDirection: 'row', alignItems: 'center', gap: 10, borderBottomWidth: 1, borderBottomColor: C.line },
  name: { flex: 1, fontSize: COMPACT_UI.body, fontWeight: '800', color: C.text },
  toggleBusy: { opacity: 0.45 },
  center: { flex: 1, alignItems: 'center', justifyContent: 'center' },
  empty: { paddingVertical: 32, textAlign: 'center', fontSize: COMPACT_UI.body, fontWeight: '700', color: C.text },
});

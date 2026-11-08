import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { AppState, Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { Badge, Background, Card, Chevron, Toggle, TopBar } from '../components/UI';
import { useAppAlert } from '../components/AppDialog';
import { ClearScreenNative, type UiCleanupTarget } from '../native/ClearScreenNative';
import { C, COMPACT_UI } from '../theme';

const accessHint = (message: string) => {
  if (message.includes('配对')) return '还没和这台手机连上，请先打开「直接改系统设置」连一次。';
  if (message.includes('Shizuku')) return '还没和这台手机连上，请先打开「直接改系统设置」连一次。';
  if (message.includes('超时')) return '连接超时，请再试一次。';
  return '要先打开「直接改系统设置」连一次，才能在这里开关。';
};

const ROUTE_AD_PRIVACY = 'settings.adPrivacy';
const ROUTE_NEWS = 'settings.news';
const ROUTE_STORE = 'store.home';

/** The three category shortcuts: the row jumps to the brand page, the switch changes the setting. */
const GROUP_ROWS = [
  { key: 'ad', title: '广告与隐私', category: 'system', route: ROUTE_AD_PRIVACY },
  { key: 'news', title: '资讯与推荐', category: 'news', route: ROUTE_NEWS },
  { key: 'store', title: '应用商店推广', category: 'store', route: ROUTE_STORE },
] as const;

function guide(manufacturer: string) {
  const brand = (manufacturer || '').toLowerCase();
  if (brand.includes('vivo') || brand.includes('iqoo')) return {
    system: '设置 → 安全 → 更多安全设置 → 广告与隐私',
    news: '设置 → 系统管理与升级 → 全局搜索；负一屏 → 设置 → 个性化推荐',
    store: '应用商店 → 我的 → 设置 → 个性化推荐 / 服务管理',
  };
  if (brand.includes('xiaomi') || brand.includes('redmi') || brand.includes('poco')) return {
    system: 'HyperOS：设置 → 隐私与安全 → 安全 → 更多安全设置 → 广告服务；MIUI：设置 → 安全 → 更多安全设置 → 广告服务',
    news: '在全局搜索或浏览器设置中检查热搜与资讯推荐',
    store: '应用商店 → 我的 → 设置 → 隐私设置 / 模式切换',
  };
  if (brand.includes('oppo') || brand.includes('realme')) return {
    system: '设置 → 其他设置 → 设备与隐私 → 设备标识与广告 → 限制广告追踪',
    news: '负一屏或桌面搜索 → 设置 → 热点资讯',
    store: '软件商店 → 我的 → 设置 → 隐私 / 服务管理',
  };
  if (brand.includes('oneplus')) return {
    system: '设置 → 隐私 → 设备 ID 和广告 → 限制广告跟踪',
    news: '桌面或浏览器设置中查找资讯推荐',
    store: '应用商店 → 设置 → 个性化推荐',
  };
  if (brand.includes('motorola') || brand.includes('moto')) return {
    system: '设置 → 安全与隐私 → 隐私 → 广告 → 停用广告个性化',
    news: '在桌面、浏览器或应用商店设置中查找推荐选项',
    store: '在应用商店设置中查找推广选项',
  };
  if (brand.includes('honor')) return {
    system: '设置 → 隐私 → 其他隐私设置 → 广告与隐私',
    news: '在设置中搜索“新闻资讯”，或打开负一屏设置',
    store: '应用市场 → 我的 → 设置 → 内容推荐服务',
  };
  if (brand.includes('huawei')) return {
    system: '设置 → 隐私 → 广告与隐私（部分版本在 HMS Core → 广告）',
    news: '负一屏 → 设置 → 个性化广告 / 内容服务推荐',
    store: '应用市场 → 我的 → 设置 → 隐私设置',
  };
  return {
    system: '在系统设置中搜索“广告”或“个性化推荐”',
    news: '在系统搜索、负一屏或浏览器设置中查找资讯推荐',
    store: '在应用商店的设置中查找推荐和推广选项',
  };
}

export default function SystemRecommendScreen({
  manufacturer,
  onBack,
  onOpenSystemPrivacy,
  onOpenAdbAccess,
}: {
  manufacturer: string;
  onBack: () => void;
  onOpenSystemPrivacy: () => void;
  onOpenAdbAccess: () => void;
}) {
  const showAlert = useAppAlert();
  const [targets, setTargets] = useState<UiCleanupTarget[]>([]);
  const [busyTarget, setBusyTarget] = useState('');
  const [accessibilityReady, setAccessibilityReady] = useState<boolean | null>(null);
  const paths = useMemo(() => guide(manufacturer), [manufacturer]);
  const fail = (title: string, error: unknown) =>
    showAlert(title, error instanceof Error ? error.message : String(error), undefined, { tone: 'error' });
  const openRoute = (route: string) => {
    void ClearScreenNative.openRecommendRoute(route).then(landed => {
      if (landed.level === 'appDetails') {
        showAlert('没有直达设置页', landed.hint || '请在系统设置中手动查找。', undefined, { tone: 'warning' });
      }
    }).catch(error => fail('无法打开系统设置', error));
  };
  const refreshTargets = useCallback(async () => {
    try {
      setTargets(await ClearScreenNative.getUiCleanupTargets());
      const result = await ClearScreenNative.takeUiCleanupResult();
      if (result) {
        showAlert(result.title, result.message);
        setTargets(await ClearScreenNative.getUiCleanupTargets());
      }
      setBusyTarget('');
    } catch { /* The next explicit operation will report any error. */ }
  }, [showAlert]);
  const readAccessibility = useCallback(async () => {
    try {
      const health = await ClearScreenNative.getAccessibilityHealth();
      setAccessibilityReady(Boolean(health.permissions.skipRunning));
    } catch { /* The next explicit operation will report any error. */ }
  }, []);
  useEffect(() => {
    void refreshTargets();
    void readAccessibility();
    const listener = AppState.addEventListener('change', state => {
      if (state === 'active') {
        setTimeout(() => { void refreshTargets(); void readAccessibility(); }, 300);
      }
    });
    return () => listener.remove();
  }, [refreshTargets, readAccessibility]);

  const openAccessibility = () => {
    void ClearScreenNative.openAccessibilitySettings().catch(error => fail('无法打开系统设置', error));
  };
  const requireAccessibility = () => {
    if (accessibilityReady !== false) return true;
    showAlert('需要开启净屏无障碍服务', '这些开关由净屏的无障碍服务代为操作，开启后才能使用。', [
      { text: '取消', style: 'cancel' },
      { text: '去开启', onPress: openAccessibility },
    ], { tone: 'warning' });
    return false;
  };
  const changeDirectTarget = async (target: UiCleanupTarget, closing: boolean) => {
    setBusyTarget(target.id);
    try {
      await ClearScreenNative.applyUiCleanupDirect(target.id, closing ? 'off' : 'on');
      setTargets(await ClearScreenNative.getUiCleanupTargets());
      showAlert(target.title, closing ? '已经关掉了。' : '已经恢复为开启。');
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error);
      if (message.includes('Shizuku') || message.includes('配对') || message.includes('还没和这台手机连上')) {
        showAlert('要先打开直接改系统设置', accessHint(message), [
          { text: '取消', style: 'cancel' },
          { text: '去打开', onPress: onOpenAdbAccess },
        ], { tone: 'warning' });
        return;
      }
      fail('无法改写系统设置', error);
    } finally {
      setBusyTarget('');
    }
  };
  const changeTarget = (target: UiCleanupTarget) => {
    const closing = target.state !== 'off';
    if (target.direct) {
      void changeDirectTarget(target, closing);
      return;
    }
    if (!requireAccessibility()) return;
    if (!closing && !target.canRestore) {
      showAlert('无法打开', '净屏没有保存这个开关原来的状态，无法自动打开，请到系统设置里自己打开。', undefined, { tone: 'warning' });
      return;
    }
    const path = target.id.startsWith('android.adPrivacy.')
      ? `设置 → Google → 所有服务 → 隐私和安全 → 广告 → 广告隐私权 → ${target.page}`
      : target.id === 'xiaomi.search.app' ? '全局搜索设置 → 首页展示模块 → 应用推荐'
      : target.id.startsWith('xiaomi.search.') ? '全局搜索设置 → 首页展示模块'
      : target.id === 'xiaomi.browser.grid' ? '浏览器设置 → 主页设置'
      : target.id.includes('launcher.assistant') ? '桌面设置 → 智能助理'
      : target.entry === 'home' ? '打开桌面后向右滑进入负一屏设置' : target.entry === 'browser' ? '在浏览器中进入设置'
      : target.category === 'store' ? '在应用商店中进入“我的 → 设置”' : target.category === 'news' ? '在系统设置中进入全局搜索' : `设置路径：${paths.system}`;
    showAlert(`${closing ? '关掉' : '打开'}「${target.title}」？`, closing
      ? `${path}。净屏只会操作页面内可明确识别的对应开关；未找到时不会更改。`
      : `净屏会按保存的原状态，把“${target.page}”里的这个开关恢复为开启。`, [
      { text: '取消', style: 'cancel' },
      { text: closing ? '关掉' : '打开', onPress: () => {
        setBusyTarget(target.id);
        void ClearScreenNative.startUiCleanup(target.id, !closing).catch(error => {
          setBusyTarget('');
          const message = error instanceof Error ? error.message : String(error);
          if (message.includes('无障碍')) {
            showAlert('需要开启净屏无障碍服务', '这些开关由净屏的无障碍服务代为操作，开启后才能使用。', [
              { text: '取消', style: 'cancel' },
              { text: '去开启', onPress: openAccessibility },
            ], { tone: 'warning' });
            return;
          }
          fail('无法启动这个操作', error);
        });
      } },
    ], { tone: 'warning' });
  };
  /** The reachable switch behind each category shortcut; null means the system keeps it private. */
  const groupTarget = (key: typeof GROUP_ROWS[number]['key']) => {
    const row = GROUP_ROWS.find(item => item.key === key)!;
    const usable = targets.filter(target =>
      target.category === row.category && (target.direct || target.support === 'assisted'));
    const preferred = key === 'news'
      ? usable.find(target => target.id.includes('launcher.assistant'))
        ?? usable.find(target => target.title.includes('总开关'))
        ?? usable.find(target => target.title.includes('资讯'))
      : usable.find(target => target.direct);
    return preferred ?? usable[0] ?? null;
  };

  return (
    <Background>
      <ScrollView contentContainerStyle={s.page} showsVerticalScrollIndicator={false}>
        <TopBar compact title="系统推荐开关" onBack={onBack} />

        <Card compact style={s.card}>
          {targets.length === 0
            ? <>
              <Text style={s.emptyStatus}>这台手机暂未适配自动开关，可先打开对应设置页面手动关闭。</Text>
              <Pressable accessibilityRole="button" onPress={onOpenSystemPrivacy} style={[s.row, s.rowPlain]}>
                <View style={s.rowText}>
                  <Text style={s.rowTitle}>系统广告设置</Text>
                  <Text style={s.rowStatus}>{paths.system}</Text>
                </View>
                <Chevron compact />
              </Pressable>
            </>
            : targets.map(target => {
              const toggleable = target.direct || target.support === 'assisted';
              const unknown = target.state === 'unknown';
              return <View key={target.id} style={[s.row, !toggleable && s.rowPlain]}>
                <View style={s.rowText}>
                  <Text style={s.rowTitle}>{target.title}</Text>
                  {!toggleable && target.reason ? <Text style={s.rowReason}>{target.reason}</Text> : null}
                </View>
                {toggleable
                  ? <View style={s.stateWrap}>
                      {unknown ? <Badge green={false} compact>未知</Badge> : null}
                      <Toggle compact unknown={unknown} value={target.state === 'on'}
                        onChange={() => { if (!busyTarget) changeTarget(target); }} />
                    </View>
                  : <Badge green={false} compact>需手动</Badge>}
              </View>;
            })}
        </Card>

        <Text style={s.sectionTitle}>常用推荐开关</Text>
        <Card compact style={s.card}>
          {GROUP_ROWS.map((row, index) => {
            const target = groupTarget(row.key);
            return <View key={row.key} style={[s.row, index === GROUP_ROWS.length - 1 && s.rowPlain]}>
              <Pressable accessibilityRole="button" accessibilityHint={`打开${row.title}的系统设置页`}
                onPress={() => openRoute(row.route)} style={s.rowText}>
                <Text style={s.rowTitle}>{row.title}</Text>
              </Pressable>
              {target
                ? <Toggle compact unknown={target.state === 'unknown'} value={target.state === 'on'}
                    onChange={() => { if (!busyTarget) changeTarget(target); }} />
                : <Pressable accessibilityRole="button" accessibilityHint={`打开${row.title}的系统设置页`}
                    onPress={() => openRoute(row.route)} style={{ flexDirection: 'row', alignItems: 'center', gap: 6 }}>
                    <Badge green={false} compact>需手动</Badge>
                    <Chevron compact />
                  </Pressable>}
            </View>;
          })}
        </Card>
      </ScrollView>
    </Background>
  );
}

const s = StyleSheet.create({
  page: { paddingHorizontal: 18, paddingBottom: 36 },
  card: { paddingHorizontal: COMPACT_UI.cardPadding, paddingTop: 4, paddingBottom: 4, marginBottom: 12 },
  sectionTitle: { fontSize: COMPACT_UI.body, fontWeight: '800', color: C.navy, paddingHorizontal: 8, paddingBottom: 8 },
  row: { minHeight: 54, flexDirection: 'row', alignItems: 'center', gap: 10, borderBottomWidth: 1, borderBottomColor: C.line, paddingVertical: 8 },
  rowPlain: { borderBottomWidth: 0 },
  rowText: { flex: 1 },
  stateWrap: { flexDirection: 'row', alignItems: 'center', gap: 6 },
  rowTitle: { fontSize: COMPACT_UI.body, fontWeight: '800', color: C.text },
  rowStatus: { fontSize: COMPACT_UI.body, color: C.text, marginTop: 3, lineHeight: 20 },
  rowReason: { fontSize: 12, color: C.text2, marginTop: 2, lineHeight: 17 },
  emptyStatus: { fontSize: COMPACT_UI.body, color: C.text, paddingHorizontal: 8, paddingVertical: 14, lineHeight: 21 },
});

import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { AppState, Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { Background, Card, Chevron, Toggle, TopBar } from '../components/UI';
import { useAppAlert } from '../components/AppDialog';
import { ClearScreenNative, type UiCleanupTarget } from '../native/ClearScreenNative';
import { C, COMPACT_UI } from '../theme';

const accessHint = (message: string) => {
  if (message.includes('配对')) return '还没和这台手机连上，请在「服务与权限」中打开 ADB Shell 操作能力。';
  if (message.includes('Shizuku')) return '还没和这台手机连上，请在「服务与权限」中打开 ADB Shell 操作能力。';
  if (message.includes('超时')) return '连接超时，请再试一次。';
  return '请先在「服务与权限」中连接 ADB Shell 操作能力，再回来开关。';
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

const ACTION_ORDER = [true, false] as const;

const needsSystemToggleOn = (target: UiCleanupTarget | null) =>
  target?.needsSystemToggleOn === true;

function guide(manufacturer: string) {
  const brand = (manufacturer || '').toLowerCase();
  if (brand.includes('vivo') || brand.includes('iqoo')) return {
    system: '设置 → 安全 → 更多安全设置 → 广告与隐私',
    news: '设置 → 系统管理与升级 → 全局搜索；负一屏 → 设置 → 个性化推荐',
    store: '应用商店 → 我的 → 设置 → 个性化推荐 / 服务管理',
  };
  if (brand.includes('xiaomi') || brand.includes('redmi') || brand.includes('poco')) return {
    system: 'HyperOS：设置 → 隐私与安全 → 安全 → 更多安全设置 → 广告服务；MIUI：设置 → 安全 → 更多安全设置 → 广告服务',
    news: '全局搜索 → 搜索设置 → 首页展示模块；浏览器 → 设置 → 主页设置',
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
  onOpenServicePermissions,
}: {
  manufacturer: string;
  onBack: () => void;
  onOpenSystemPrivacy: () => void;
  onOpenServicePermissions: () => void;
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
      setAccessibilityReady(Boolean(health.permissions.skip));
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
        showAlert('要先连接 ADB Shell 操作能力', accessHint(message), [
          { text: '取消', style: 'cancel' },
          { text: '去连接', onPress: onOpenServicePermissions },
        ], { tone: 'warning' });
        return;
      }
      fail('无法改写系统设置', error);
    } finally {
      setBusyTarget('');
    }
  };
  const changeTargetTo = (target: UiCleanupTarget, closing: boolean) => {
    if (target.direct) {
      void changeDirectTarget(target, closing);
      return;
    }
    if (!requireAccessibility()) return;
    const start = () => {
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
    };
    start();
  };
  const changeTarget = (target: UiCleanupTarget) => {
    if (target.state === 'unknown') {
      showAlert(target.title, '当前状态未确认，请选择要设置的状态。', [
        { text: '取消', style: 'cancel' },
        { text: '开启', onPress: () => changeTargetTo(target, false) },
        { text: '关闭', onPress: () => changeTargetTo(target, true) },
      ]);
      return;
    }
    changeTargetTo(target, target.state !== 'off');
  };
  // Category rows open pages; they are not separate system switches.
  const categoryTargets = GROUP_ROWS.map(row => ({ row, target: null }));
  const renderTarget = (target: UiCleanupTarget, plain: boolean) => (
    <View key={target.id} style={[s.row, plain && s.rowPlain]}>
      <View style={s.rowText}><Text style={s.rowTitle}>{target.title}</Text></View>
      <View style={s.stateWrap}>
        <Toggle compact unknown={target.state === 'unknown'} value={target.state === 'on'}
          onChange={() => { if (!busyTarget) changeTarget(target); }} />
      </View>
    </View>
  );
  const renderCategory = (row: typeof GROUP_ROWS[number], plain: boolean) =>
    <Pressable key={row.key} accessibilityRole="button" accessibilityHint={`打开${row.title}的系统设置页`}
      onPress={() => openRoute(row.route)} style={[s.row, plain && s.rowPlain]}>
      <View style={s.rowText}><Text style={s.rowTitle}>{row.title}</Text></View>
      <Chevron compact />
    </Pressable>;
  const actionRows = ACTION_ORDER.flatMap(needsOn => {
    const groupTargets = targets.filter(target => needsSystemToggleOn(target) === needsOn);
    const groupCategories = categoryTargets.filter(({ target }) => needsSystemToggleOn(target) === needsOn);
    return [
      ...groupTargets.map(target => (last: boolean) => renderTarget(target, last)),
      ...groupCategories.map(({ row }) => (last: boolean) => renderCategory(row, last)),
    ];
  });

  return (
    <Background>
      <ScrollView contentContainerStyle={s.page} showsVerticalScrollIndicator={false}>
        <TopBar compact title="系统推荐开关" onBack={onBack} />

        <Card compact style={s.card}>
          {targets.length === 0 ? <>
              <Text style={s.emptyStatus}>这台手机暂未适配自动开关，可先打开对应设置页面手动关闭。</Text>
              <Pressable accessibilityRole="button" onPress={onOpenSystemPrivacy} style={[s.row, s.rowPlain]}>
                <View style={s.rowText}>
                  <Text style={s.rowTitle}>系统广告设置</Text>
                  <Text style={s.rowStatus}>{paths.system}</Text>
                </View>
                <Chevron compact />
              </Pressable>
            </> : null}
          {actionRows.map((renderRow, index) => renderRow(index === actionRows.length - 1))}
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
  stateWrap: { flexDirection: 'row', alignItems: 'center', gap: 6 },
  rowTitle: { fontSize: COMPACT_UI.body, fontWeight: '800', color: C.text },
  rowStatus: { fontSize: COMPACT_UI.body, color: C.text, marginTop: 3, lineHeight: 20 },
  emptyStatus: { fontSize: COMPACT_UI.body, color: C.text, paddingHorizontal: 8, paddingVertical: 14, lineHeight: 21 },
});

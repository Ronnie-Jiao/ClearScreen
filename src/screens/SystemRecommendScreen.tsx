import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { AppState, Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { Background, Card, Chevron, Toggle, TopBar } from '../components/UI';
import { useAppAlert } from '../components/AppDialog';
import { ClearScreenNative, type UiCleanupTarget } from '../native/ClearScreenNative';
import { C, COMPACT_UI } from '../theme';

const accessHint = (message: string) => {
  if (message.includes('配对')) return '还没和这台手机连上，请在「服务与权限」中打开「系统设置权限」。';
  if (message.includes('Shizuku')) return '还没和这台手机连上，请在「服务与权限」中打开「系统设置权限」。';
  if (message.includes('超时')) return '连接超时，请再试一次。';
  return '请先在「服务与权限」中连接「系统设置权限」，再回来开关。';
};

const ACTION_ORDER = [true, false] as const;

/** Assisted switches have no readable system value; the label reflects the last confirmed state. */
type AssistedStatus = 'toSet' | 'set' | 'unknown';
const STATUS_LABEL: Record<AssistedStatus, string> = {
  toSet: '去设置', set: '已设置', unknown: '未知',
};
function assistedStatus(target: UiCleanupTarget): AssistedStatus {
  if (target.state === 'off') return 'set';
  if (target.state === 'on') return 'toSet';
  return target.canRestore ? 'unknown' : 'toSet';
}
function StatusPill({ status, busy, title, onPress }: {
  status: AssistedStatus; busy: boolean; title: string; onPress: () => void;
}) {
  const pillTone = status === 'set' ? s.pillSet : status === 'unknown' ? s.pillUnknown : s.pillToSet;
  const textTone = status === 'set' ? s.pillTextSet : status === 'unknown' ? s.pillTextUnknown : s.pillTextToSet;
  return <Pressable accessibilityRole="button" accessibilityLabel={`${title}，${STATUS_LABEL[status]}`}
    accessibilityState={{ disabled: busy }} disabled={busy} onPress={onPress}
    style={({ pressed }) => [s.pill, pillTone, pressed && s.pillPressed, busy && s.pillBusy]}>
    <Text style={[s.pillText, textTone]}>{STATUS_LABEL[status]}</Text>
  </Pressable>;
}

const needsSystemToggleOn = (target: UiCleanupTarget | null) =>
  target?.needsSystemToggleOn === true;

function guide(manufacturer: string) {
  const brand = (manufacturer || '').toLowerCase();
  if (brand.includes('vivo') || brand.includes('iqoo')) return {
    system: '设置 → 安全 → 更多安全设置 → 广告与隐私',
    news: '设置 → 系统管理与升级 → 全局搜索；负一屏 → 设置 → 个性化推荐',
    store: '应用商店 → 我的 → 右上角设置 → 关于 → 服务管理 / 个性化推荐',
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
  const visibleTargets = useMemo(() => {
    const seen = new Set<string>();
    return targets.filter(target => {
      if (target.id.startsWith('android.adPrivacy.')) return false;
      if (target.category !== 'system') return false;
      if (seen.has(target.title)) return false;
      seen.add(target.title);
      return true;
    });
  }, [targets]);
  const paths = useMemo(() => guide(manufacturer), [manufacturer]);
  const fail = (title: string, error: unknown) =>
    showAlert(title, error instanceof Error ? error.message : String(error), undefined, { tone: 'error' });
  // Every switch row can land the user on the exact system settings page that
  // owns it — independent of whether the automatic toggle is available.
  const openTargetSettings = (target: UiCleanupTarget) => {
    void ClearScreenNative.openTargetSettingsPage(target.id).then(landed => {
      if (landed.level === 'appDetails') {
        showAlert('没有直达设置页', landed.hint || '请在系统设置中手动查找。', undefined, { tone: 'warning' });
      }
    }).catch(error => fail('无法打开系统设置', error));
  };
  const refreshTargets = useCallback(async () => {
    try {
      setTargets(await ClearScreenNative.getUiCleanupTargets());
      const result = await ClearScreenNative.takeUiCleanupResult();
      // Successful on/off changes are reflected by the switch/pill itself — no dialog.
      // Only surface an unconfirmed/failed result so silent failures stay visible.
      if (result && /没|超时|失败/.test(result.message)) {
        showAlert(result.title, result.message, undefined, { tone: 'warning' });
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
      // No success dialog: the toggle itself is the feedback.
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error);
      if (message.includes('Shizuku') || message.includes('配对') || message.includes('还没和这台手机连上')) {
        showAlert('需要系统设置权限', accessHint(message), [
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
    if (!target.direct && assistedStatus(target) === 'set') {
      showAlert(target.title, '这一项已经设置好了。', [
        { text: '取消', style: 'cancel' },
        { text: '重新设置', onPress: () => changeTargetTo(target, true) },
        { text: '恢复开启', onPress: () => changeTargetTo(target, false) },
      ]);
      return;
    }
    // Assisted switches have no live value to flip: "去设置" and "未知" both mean "set it now".
    changeTargetTo(target, target.direct ? target.state !== 'off' : true);
  };
  const renderTarget = (target: UiCleanupTarget) => (
    <Card compact key={target.id} style={s.card}>
      <View style={s.row}>
        <Pressable accessibilityRole="button" accessibilityHint={`打开「${target.title}」的系统设置页`}
          onPress={() => openTargetSettings(target)} style={s.rowText}>
          <Text style={s.rowTitle}>{target.title}<Text style={s.rowJump}> ›</Text></Text>
        </Pressable>
        <View style={s.stateWrap}>
          {target.direct ?
            <Toggle compact unknown={target.state === 'unknown'} value={target.state === 'on'}
              onChange={() => { if (!busyTarget) changeTarget(target); }} /> :
            <StatusPill status={assistedStatus(target)} busy={Boolean(busyTarget)} title={target.title}
              onPress={() => { if (!busyTarget) openTargetSettings(target); }} />}
        </View>
      </View>
    </Card>
  );
  const actionRows = ACTION_ORDER.flatMap(needsOn =>
    visibleTargets.filter(target => needsSystemToggleOn(target) === needsOn)
      .map(target => renderTarget(target)));

  return (
    <Background>
      <ScrollView contentContainerStyle={s.page} showsVerticalScrollIndicator={false}>
        <TopBar compact title="系统推荐设置" onBack={onBack} />

        {visibleTargets.length === 0 ?
          <Card compact style={s.card}>
            <Text style={s.emptyStatus}>这台手机暂未适配自动开关，可先打开对应设置页面手动关闭。</Text>
            <Pressable accessibilityRole="button" onPress={onOpenSystemPrivacy} style={s.row}>
              <View style={s.rowText}>
                <Text style={s.rowTitle}>系统广告设置</Text>
                <Text style={s.rowStatus}>{paths.system}</Text>
              </View>
              <Chevron compact />
            </Pressable>
          </Card> : null}
        {actionRows}
      </ScrollView>
    </Background>
  );
}

const s = StyleSheet.create({
  page: { paddingHorizontal: 18, paddingBottom: 36 },
  card: { paddingHorizontal: COMPACT_UI.cardPadding, paddingTop: 8, paddingBottom: 8, marginBottom: 12 },
  row: { minHeight: 46, flexDirection: 'row', alignItems: 'center', gap: 10 },
  rowText: { flex: 1 },
  stateWrap: { flexDirection: 'row', alignItems: 'center', gap: 6 },
  pill: { minWidth: 68, paddingHorizontal: 12, paddingVertical: 7, borderRadius: 14, alignItems: 'center' },
  pillToSet: { backgroundColor: '#EAF4FF' },
  pillSet: { backgroundColor: C.greenBg },
  pillUnknown: { backgroundColor: '#EDF1F6' },
  pillPressed: { opacity: 0.7 },
  pillBusy: { opacity: 0.5 },
  pillText: { fontSize: COMPACT_UI.caption, fontWeight: '800', lineHeight: 16 },
  pillTextToSet: { color: C.blue },
  pillTextSet: { color: C.green },
  pillTextUnknown: { color: '#7186A3' },
  rowTitle: { fontSize: COMPACT_UI.body, fontWeight: '800', color: C.text },
  rowJump: { color: C.blue, fontWeight: '400' },
  rowStatus: { fontSize: COMPACT_UI.body, color: C.text, marginTop: 3, lineHeight: 20 },
  emptyStatus: { fontSize: COMPACT_UI.body, color: C.text, paddingHorizontal: 8, paddingVertical: 14, lineHeight: 21 },
});

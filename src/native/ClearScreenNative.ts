import { NativeModules, Platform } from 'react-native';
import { AppItem, LogItem, LearnedCloseRecord } from '../data';
import { LOGO } from '../assets';

type NativeApp = {
  id?: string;
  packageName?: string;
  name?: string;
  iconUri?: string;
  skip?: boolean;
  network?: boolean;
  whitelist?: boolean;
  isSystemApp?: boolean;
};

type NativeLog = LogItem & { packageName?: string; result?: string; timestamp?: number };

export type AccessibilityHealth = 'ready' | 'unavailable' | 'unknown';
export type AccessibilityPermissions = Pick<BackendPermissions,
  'skip' | 'skipRunning' | 'accessibilitySettingEnabled' | 'accessibilityServiceBound' |
  'accessibilityProcessAlive' | 'accessibilityStoredServices' | 'accessibilityHealth'>;

export type BackendPermissions = {
  skip: boolean;
  skipRunning: boolean;
  accessibilitySettingEnabled: boolean;
  accessibilityServiceBound: boolean;
  accessibilityProcessAlive: boolean;
  accessibilityHealth: AccessibilityHealth;
  accessibilityStoredServices: string;
  network: boolean;
  networkRunning: boolean;
  bg: boolean;
  batteryOptimizationIgnored: boolean | null;
  backgroundRestricted: boolean | null;
  startupAllowed: boolean | null;
  startupManagement?: 'autostart' | 'sleeping';
  backgroundAllowed: boolean | null;
  isVivoFamily: boolean;
  vendorStartupSettingsAvailable: boolean;
  // Android has no public API for OEM self-start switches. This field
  // means the user completed the guided check, not that the system exposed a
  // verifiable permission grant to the app.
  vendorStartupGuideConfirmed: boolean;
  deviceManufacturer: string;
  restrictedSettingsLikely: boolean;
};

export function pickAccessibilityPermissions(value: BackendPermissions): AccessibilityPermissions {
  const { skip, skipRunning, accessibilitySettingEnabled, accessibilityServiceBound,
    accessibilityProcessAlive, accessibilityStoredServices, accessibilityHealth } = value;
  return { skip, skipRunning, accessibilitySettingEnabled, accessibilityServiceBound,
    accessibilityProcessAlive, accessibilityStoredServices, accessibilityHealth };
}

export type InstallSourceDiagnostics = {
  installingPackageName: string | null;
  initiatingPackageName: string | null;
  originatingPackageName: string | null;
  packageSource: number;
  packageSourceLabel: string;
  adbInstallLikely: boolean;
  restrictedSettingsLikely: boolean;
  firstInstallTime: number;
  lastUpdateTime: number;
  signingCertificateSha256: string | null;
};

export type CleanupAccess = { running: boolean; granted: boolean; adbSupported?: boolean; adbConnected?: boolean; adbState?: string };
export type AppNotificationState = { packageName: string; enabled: boolean | null };
export type AppNotificationResult = 'enabled' | 'disabled' | 'already_enabled' | 'already_disabled';
export type AdbAccessStatus = {
  supported: boolean;
  state: 'unsupported' | 'idle' | 'pairing' | 'connecting' | 'connected' | 'error';
  connected: boolean;
  paired: boolean;
  reconnectRequested?: boolean;
  host: string;
  port: number;
  message: string;
  localAddress: string;
  autoPairStage?: '' | 'waiting-code' | 'pairing' | 'waiting-port' | 'connecting' | 'connected' | 'failed';
  autoPairMessage?: string;
};
export type AdbActionResult = { ok: boolean; message: string };
export type AdLearningState = { active: boolean; count: number; message: string; records: LearnedCloseRecord[] };
export type CleanupTarget = {
  id: string; title: string; category: 'quick' | 'ads'; packageName: string;
  state: 'active' | 'blocked' | 'unknown' | 'absent'; canRestore: boolean;
};
export type MarketingChannel = {
  packageName: string; id: string; name: string; state: 'on' | 'off' | 'unknown';
  /** 通知渠道的重要性；0 表示渠道自身的开关是关的。 */
  importance?: number;
  /** 渠道分组名；系统没有分组时为 null。 */
  group?: string | null;
};
export type NotificationAccessStatus = { supported: boolean; granted: boolean };
export type UiCleanupTarget = {
  id: string; title: string; category: 'system' | 'news' | 'store'; page: string; entry: 'settings' | 'home' | 'browser';
  canRestore: boolean; state: 'on' | 'off' | 'unknown'; direct: boolean;
  /** Whether the system switch must be on to disable its ad/recommendation feature. */
  needsSystemToggleOn: boolean;
  /** Plain-language outcome of closing this switch. */
  effect: string;
  /** assisted: exact-page operation · manual: manual path · unreachable: no verified matching control. */
  support: 'assisted' | 'manual' | 'unreachable';
  reason?: string | null;
  navigate?: string | null;
};
export type UiCleanupDirectResult = { result: 'changed' | 'already'; state: 'on' | 'off' };
/** Which fallback level a manual-setup row reached, for honest user feedback. */
export type RecommendRouteResult = {
  level: 'vendor' | 'system' | 'appDetails';
  component: string;
  hint?: string | null;
};

export type BackendSnapshot = {
  backendReady: boolean;
  privacyConsentAccepted: boolean;
  privacyPolicyVersion: string;
  onboardingCompleted: boolean;
  masterEnabled: boolean;
  installedAppCount: number;
  todaySkipCount: number;
  todayNetworkCount: number;
  permissions: BackendPermissions;
  installSource: InstallSourceDiagnostics;
  apps: AppItem[];
  logs: LogItem[];
  settings: {
    startup: boolean;
    autoUpdate: boolean;
    debug: boolean;
    skipSplashVideo: boolean;
    closePopupAds: boolean;
    closeFeedAds: boolean;
    logRetentionDays: number;
  };
  versionName: string;
};

const native = Platform.OS === 'android' ? NativeModules.ClearScreenBackend : undefined;

type SettingsMethod='openAccessibilitySettings'|'openAppDetailsSettings'|'openStartupSettings'|'openBackgroundSettings'|'openBatterySettings'|'openSystemPrivacySettings'|'openSystemAdPrivacySettings'|'openSystemHomeSettings'|'openAppNotificationSettings'|'openVendorStore'|'openMarketingChannel'|'openDeveloperOptions'|'openNotificationAccessSettings';
async function openNativeSettings(method: SettingsMethod, ...args: string[]): Promise<void> {
  if (typeof native?.[method] !== 'function' || await withNativeTimeout<any>(native[method](...args), 8000, '系统设置打开超时，请重试') !== true) {
    throw new Error('系统设置入口不可用');
  }
}

/** Keeps a native call from holding the UI in a pending state forever. */
function withNativeTimeout<T>(read: Promise<T>, timeoutMs: number, message: string): Promise<T> {
  return new Promise<T>((resolve, reject) => {
    const timer = setTimeout(() => reject(new Error(message)), timeoutMs);
    read.then(value => { clearTimeout(timer); resolve(value) }, error => { clearTimeout(timer); reject(error) });
  });
}

function withPermissionTimeout<T>(read: Promise<T>): Promise<T> {
  return new Promise<T>((resolve, reject) => {
    const timer = setTimeout(() => reject(new Error('权限状态读取超时')), 4000);
    read.then(value => { clearTimeout(timer); resolve(value) }, error => { clearTimeout(timer); reject(error) });
  });
}

function normalizeApp(app: NativeApp): AppItem {
  return {
    id: app.id || app.packageName || `unknown-${app.name || 'app'}`,
    packageName: app.packageName || app.id,
    name: app.name || app.packageName || '未知应用',
    icon: app.iconUri ? { uri: app.iconUri } : LOGO,
    skip: Boolean(app.skip),
    // Network filtering is intentionally disabled so ClearScreen never owns a
    // VPN slot needed by the user's separate VPN client.
    network: false,
    // The single per-app switch is authoritative: off means excluded.
    whitelist: !Boolean(app.skip),
    isSystemApp: Boolean(app.isSystemApp),
  };
}

function fallbackSnapshot(): BackendSnapshot {
  return {
    backendReady: false,
    privacyConsentAccepted: false,
    privacyPolicyVersion: '2026-11-14-v1',
    onboardingCompleted: false,
    masterEnabled: false,
    installedAppCount: 0,
    todaySkipCount: 0,
    todayNetworkCount: 0,
    permissions: {
      skip: false,
      skipRunning: false,
      accessibilitySettingEnabled: false,
      accessibilityServiceBound: false,
      accessibilityProcessAlive: false,
      accessibilityHealth: 'unknown',
      accessibilityStoredServices: '',
      network: false,
      networkRunning: false,
      bg: false,
      batteryOptimizationIgnored: null,
      backgroundRestricted: null,
      startupAllowed: null,
      backgroundAllowed: null,
      isVivoFamily: false,
      vendorStartupSettingsAvailable: false,
      vendorStartupGuideConfirmed: false,
      deviceManufacturer: 'unknown',
      restrictedSettingsLikely: false,
    },
    installSource: {
      installingPackageName: null,
      initiatingPackageName: null,
      originatingPackageName: null,
      packageSource: -1,
      packageSourceLabel: 'unknown',
      adbInstallLikely: false,
      restrictedSettingsLikely: false,
      firstInstallTime: 0,
      lastUpdateTime: 0,
      signingCertificateSha256: null,
    },
    apps: [],
    logs: [],
    settings: {
      startup: true,
      autoUpdate: true,
      debug: false,
      skipSplashVideo: true,
      closePopupAds: true,
      closeFeedAds: true,
      logRetentionDays: 7,
    },
    versionName: 'unknown',
  };
}

export function normalizeSnapshot(raw: any): BackendSnapshot {
  if (!raw || !native) return fallbackSnapshot();
  const apps = Array.isArray(raw.apps) ? raw.apps.map(normalizeApp) : [];
  const logs = Array.isArray(raw.logs)
    ? (raw.logs as LogItem[]).filter(log => log.type !== 'network')
    : [];
  const installSource: InstallSourceDiagnostics = {
    installingPackageName: raw.installSource?.installingPackageName ?? null,
    initiatingPackageName: raw.installSource?.initiatingPackageName ?? null,
    originatingPackageName: raw.installSource?.originatingPackageName ?? null,
    packageSource: Number(raw.installSource?.packageSource ?? -1),
    packageSourceLabel: String(raw.installSource?.packageSourceLabel || 'unknown'),
    adbInstallLikely: Boolean(raw.installSource?.adbInstallLikely),
    restrictedSettingsLikely: Boolean(raw.installSource?.restrictedSettingsLikely),
    firstInstallTime: Number(raw.installSource?.firstInstallTime || 0),
    lastUpdateTime: Number(raw.installSource?.lastUpdateTime || 0),
    signingCertificateSha256: raw.installSource?.signingCertificateSha256 ?? null,
  };
  return {
    backendReady: Boolean(raw.backendReady),
    privacyConsentAccepted: Boolean(raw.privacyConsentAccepted),
    privacyPolicyVersion: String(raw.privacyPolicyVersion || '2026-11-14-v1'),
    onboardingCompleted: Boolean(raw.onboardingCompleted),
    masterEnabled: Boolean(raw.masterEnabled),
    installedAppCount: Number(raw.installedAppCount || apps.length),
    todaySkipCount: Number(raw.todaySkipCount || 0),
    todayNetworkCount: 0,
    permissions: {
      skip: Boolean(raw.accessibilitySettingEnabled ?? raw.accessibilityEnabled),
      skipRunning: Boolean(raw.accessibilityRunning) && raw.accessibilityHealth === 'ready',
      accessibilitySettingEnabled: Boolean(raw.accessibilitySettingEnabled ?? raw.accessibilityEnabled),
      accessibilityServiceBound: Boolean(raw.accessibilityServiceBound ?? raw.accessibilityRunning),
      accessibilityProcessAlive: Boolean(raw.accessibilityProcessAlive),
      accessibilityHealth: raw.accessibilityHealth === 'ready' || raw.accessibilityHealth === 'unavailable'
        ? raw.accessibilityHealth : 'unknown',
      accessibilityStoredServices: String(raw.accessibilityStoredServices || ''),
      network: false,
      networkRunning: false,
      bg: Boolean(raw.batteryOptimizationIgnored),
      batteryOptimizationIgnored: typeof raw.batteryOptimizationIgnored === 'boolean' ? raw.batteryOptimizationIgnored : null,
      backgroundRestricted: typeof raw.backgroundRestricted === 'boolean' ? raw.backgroundRestricted : null,
      startupAllowed: typeof raw.startupAllowed === 'boolean' ? raw.startupAllowed : null,
      startupManagement: raw.startupManagement === 'sleeping' ? 'sleeping' : 'autostart',
      backgroundAllowed: typeof raw.backgroundAllowed === 'boolean' ? raw.backgroundAllowed : null,
      isVivoFamily: Boolean(raw.isVivoFamily),
      vendorStartupSettingsAvailable: Boolean(raw.vendorStartupSettingsAvailable),
      vendorStartupGuideConfirmed: Boolean(raw.vendorStartupGuideConfirmed),
      deviceManufacturer: String(raw.deviceManufacturer || 'unknown'),
      restrictedSettingsLikely: installSource.restrictedSettingsLikely,
    },
    installSource,
    apps,
    logs,
    settings: {
      startup: raw.settings?.startup !== false,
      autoUpdate: raw.settings?.autoUpdate !== false,
      debug: Boolean(raw.settings?.debug),
      skipSplashVideo: raw.settings?.skipSplashVideo !== false,
      closePopupAds: raw.settings?.closePopupAds !== false,
      closeFeedAds: raw.settings?.closeFeedAds !== false,
      logRetentionDays: [0, 1, 7, 30].includes(Number(raw.settings?.logRetentionDays))
        ? Number(raw.settings.logRetentionDays)
        : 7,
    },
    versionName: String(raw.versionName || 'unknown'),
  };
}

export const ClearScreenNative = {
  available: Boolean(native),
  getAdLearningState: async (): Promise<AdLearningState> => {
    if (!native?.getAdLearningState) return {active:false,count:0,message:'',records:[]};
    const state = await withNativeTimeout<any>(native.getAdLearningState(), 4000, '学习状态读取超时，请重试');
    return {...state, records: Array.isArray(state.records) ? state.records : []};
  },
  startAdLearning: async (): Promise<AdLearningState> => {
    if (!native?.startAdLearning) throw new Error('当前客户端尚未接入学习关闭');
    const state = await withNativeTimeout<any>(native.startAdLearning(), 8000, '学习关闭启动超时，请重试');
    return {...state, records: Array.isArray(state.records) ? state.records : []};
  },
  stopAdLearning: async (): Promise<AdLearningState> => {
    if (!native?.stopAdLearning) throw new Error('学习关闭不可用');
    const state = await withNativeTimeout<any>(native.stopAdLearning(), 8000, '学习关闭停止超时，请重试');
    return {...state, records: Array.isArray(state.records) ? state.records : []};
  },
  clearAdLearning: async (): Promise<AdLearningState> => {
    if (!native?.clearAdLearning) throw new Error('学习关闭不可用');
    const state = await withNativeTimeout<any>(native.clearAdLearning(), 8000, '学习规则清除超时，请重新检查');
    return {...state, records: Array.isArray(state.records) ? state.records : []};
  },
  deleteAdLearningRule: async (id: string): Promise<AdLearningState> => {
    if (!native?.deleteAdLearningRule) throw new Error('当前客户端尚未接入单条删除');
    const state = await withNativeTimeout<any>(native.deleteAdLearningRule(id), 8000, '学习规则删除超时，请重新检查');
    return {...state, records: Array.isArray(state.records) ? state.records : []};
  },
  getSnapshot: async (): Promise<BackendSnapshot> => {
    if (!native?.getSnapshot) return fallbackSnapshot();
    return normalizeSnapshot(await withNativeTimeout(native.getSnapshot(), 15_000, '数据读取超时，请重试'));
  },
  getPermissionSnapshot: async (): Promise<Pick<BackendSnapshot,'privacyConsentAccepted'|'permissions'>> => {
    if (native?.getPermissionSnapshot) return normalizeSnapshot(await withPermissionTimeout(native.getPermissionSnapshot()));
    if (native?.getSnapshot) return normalizeSnapshot(await withPermissionTimeout(native.getSnapshot()));
    return fallbackSnapshot();
  },
  getAccessibilityHealth: async (): Promise<{ privacyConsentAccepted: boolean; permissions: AccessibilityPermissions }> => {
    if (!native?.getAccessibilityHealth) return fallbackSnapshot();
    const snapshot = normalizeSnapshot(await withPermissionTimeout(native.getAccessibilityHealth()));
    return { privacyConsentAccepted: snapshot.privacyConsentAccepted,
      permissions: pickAccessibilityPermissions(snapshot.permissions) };
  },
  acceptPrivacyPolicy: async (): Promise<BackendSnapshot> => {
    if (!native?.acceptPrivacyPolicy) {
      return { ...fallbackSnapshot(), backendReady: true, privacyConsentAccepted: true };
    }
    return normalizeSnapshot(await withNativeTimeout<any>(native.acceptPrivacyPolicy(), 20000, '初始化超时，请重试'));
  },
  withdrawPrivacyConsent: async (): Promise<BackendSnapshot> => {
    if (!native?.withdrawPrivacyConsent) return fallbackSnapshot();
    return normalizeSnapshot(await withNativeTimeout<any>(native.withdrawPrivacyConsent(), 20000, '数据清理超时，请重新检查'));
  },
  setOnboardingCompleted: async (completed: boolean) => {
    if (native?.setOnboardingCompleted) await withNativeTimeout<any>(native.setOnboardingCompleted(completed), 6000, '设置保存超时，请重试');
  },
  setMasterEnabled: async (enabled: boolean) => {
    if (native?.setMasterEnabled) await withNativeTimeout<any>(native.setMasterEnabled(enabled), 6000, '开关操作超时，请重新检查');
  },
  setAppRule: async (app: AppItem) => {
    if (native?.setAppRule) {
      await withNativeTimeout<any>(native.setAppRule(app.packageName || app.id, app.skip, false, !app.skip), 6000, '应用规则保存超时，请重新检查');
    }
  },
  setAppRules: async (apps: AppItem[]) => {
    if (native?.setAppRules) {
      await withNativeTimeout(native.setAppRules(apps.map(app => ({
        packageName: app.packageName || app.id,
        skipEnabled: app.skip,
      }))), 8000, '应用规则保存超时，请重新检查');
    } else if (native?.setAppRule) {
      // Keep development clients using an older native module compatible.
      await Promise.all(apps.map(app =>
        withNativeTimeout<any>(native.setAppRule(app.packageName || app.id, app.skip, false, !app.skip), 6000, '应用规则保存超时，请重新检查'),
      ));
    }
  },
  setLogRetentionDays: async (days: number) => {
    if (!native?.setLogRetentionDays) return;
    await withNativeTimeout<any>(native.setLogRetentionDays(days), 6000, '设置保存超时，请重试');
  },
  setSetting: async (
    key: 'startup' | 'autoUpdate' | 'debug' | 'skipSplashVideo' | 'closePopupAds' | 'closeFeedAds',
    enabled: boolean,
  ) => {
    if (native?.setSetting) await withNativeTimeout<any>(native.setSetting(key, enabled), 6000, '设置保存超时，请重试');
  },
  restoreDefaults: async (): Promise<BackendSnapshot> => {
    if (!native?.restoreDefaults) throw new Error('Restoring settings is unavailable');
    return normalizeSnapshot(await withNativeTimeout<any>(native.restoreDefaults(), 20000, '恢复默认超时，请重新检查'));
  },
  clearLogs: async () => {
    if (!native?.clearLogs) return fallbackSnapshot();
    return normalizeSnapshot(await withNativeTimeout<any>(native.clearLogs(), 20000, '记录清除超时，请重新检查'));
  },
  openAccessibilitySettings: () => openNativeSettings('openAccessibilitySettings'),
  openAppDetailsSettings: () => openNativeSettings('openAppDetailsSettings'),
  openStartupSettings: () => openNativeSettings('openStartupSettings'),
  openBackgroundSettings: () => openNativeSettings('openBackgroundSettings'),
  openBatterySettings: () => openNativeSettings('openBatterySettings'),
  openSystemPrivacySettings: () => openNativeSettings('openSystemPrivacySettings'),
  openSystemAdPrivacySettings: () => openNativeSettings('openSystemAdPrivacySettings'),
  openSystemHomeSettings: () => openNativeSettings('openSystemHomeSettings'),
  openRecommendRoute: async (route: string): Promise<RecommendRouteResult> => {
    if (typeof native?.openRecommendRoute !== 'function') throw new Error('系统设置入口不可用');
    const landed = await withNativeTimeout<any>(native.openRecommendRoute(route), 8000, '系统设置打开超时，请重试');
    const level = landed?.level;
    return {
      level: level === 'vendor' || level === 'system' ? level : 'appDetails',
      component: String(landed?.component || ''),
      hint: landed?.hint ? String(landed.hint) : null,
    };
  },
  openTargetSettingsPage: async (id: string): Promise<RecommendRouteResult> => {
    if (typeof native?.openTargetSettingsPage !== 'function') throw new Error('系统设置入口不可用');
    const landed = await withNativeTimeout<any>(native.openTargetSettingsPage(id), 8000, '系统设置打开超时，请重试');
    const level = landed?.level;
    return {
      level: level === 'vendor' || level === 'system' ? level : 'appDetails',
      component: String(landed?.component || ''),
      hint: landed?.hint ? String(landed.hint) : null,
    };
  },
  openAppNotificationSettings: (packageName: string) => openNativeSettings('openAppNotificationSettings', packageName),
  openVendorStore: () => openNativeSettings('openVendorStore'),
  openMarketingChannel: (packageName: string, channelId: string) => openNativeSettings('openMarketingChannel', packageName, channelId),
  openNotificationAccessSettings: () => openNativeSettings('openNotificationAccessSettings'),
  openDeveloperOptions: async (): Promise<'developer' | 'settingsHome' | 'aboutPhone'> => {
    if (typeof native?.openDeveloperOptions !== 'function') throw new Error('系统设置入口不可用');
    const level = await withNativeTimeout<any>(native.openDeveloperOptions(), 8000, '系统设置打开超时，请重试');
    return level === 'settingsHome' || level === 'aboutPhone' ? level : 'developer';
  },
  getAdbAccessStatus: async (): Promise<AdbAccessStatus> => {
    if (!native?.getAdbAccessStatus) throw new Error('此设备不支持直接改系统设置');
    return withNativeTimeout(native.getAdbAccessStatus(), 6000, '连接状态读取超时，请重试');
  },
  pairAdb: async (host: string, port: number, code: string): Promise<AdbActionResult> => {
    if (!native?.pairAdb) throw new Error('此设备不支持直接改系统设置');
    return withNativeTimeout(native.pairAdb(host, port, code), 45000, '配对没有完成，请重试');
  },
  connectAdb: async (host: string, port: number): Promise<AdbActionResult> => {
    if (!native?.connectAdb) throw new Error('此设备不支持直接改系统设置');
    return withNativeTimeout(native.connectAdb(host, port), 45000, '连接没有完成，请重试');
  },
  autoConnectAdb: async (): Promise<AdbActionResult> => {
    if (!native?.autoConnectAdb) throw new Error('此设备不支持直接改系统设置');
    return withNativeTimeout(native.autoConnectAdb(), 45000, '连接没有完成，请重试');
  },
  startAdbAutoPair: async (): Promise<AdbAccessStatus> => {
    if (!native?.startAdbAutoPair) throw new Error('此设备不支持直接改系统设置');
    return withNativeTimeout(native.startAdbAutoPair(), 15000, '没有开始配对，请重试');
  },
  stopAdbAutoPair: async (): Promise<AdbAccessStatus> => {
    if (!native?.stopAdbAutoPair) throw new Error('此设备不支持直接改系统设置');
    return withNativeTimeout(native.stopAdbAutoPair(), 15000, '停止配对超时，请重试');
  },
  disconnectAdb: async (): Promise<void> => {
    if (!native?.disconnectAdb) throw new Error('此设备不支持直接改系统设置');
    await withNativeTimeout(native.disconnectAdb(), 10_000, '断开连接超时，请重试');
  },
  getCleanupAccessStatus: async (): Promise<CleanupAccess> => {
    if (!native?.getCleanupAccessStatus) throw new Error('此设备不支持系统清理');
    return withNativeTimeout(native.getCleanupAccessStatus(), 8_000, '权限状态读取超时');
  },
  requestCleanupAccess: async (): Promise<boolean> => {
    if (!native?.requestCleanupAccess) throw new Error('此设备不支持系统清理');
    return withNativeTimeout<any>(native.requestCleanupAccess(), 10000, '授权请求超时，请重新检查');
  },
  inspectCleanupTargets: async (): Promise<CleanupTarget[]> => {
    if (!native?.inspectCleanupTargets) throw new Error('此设备不支持系统清理');
    return withNativeTimeout(native.inspectCleanupTargets(), 30_000, '清理项扫描超时，请重试');
  },
  changeCleanupTarget: async (id: string, restore: boolean): Promise<{ result: string }> => {
    if (!native?.changeCleanupTarget) throw new Error('此设备不支持系统清理');
    return withNativeTimeout(native.changeCleanupTarget(id, restore), 30_000, '系统命令超时，请重试');
  },
  readAppNotificationStates: async (packageNames: string[]): Promise<AppNotificationState[]> => {
    if (!native?.readAppNotificationStates) throw new Error('此设备不支持读取应用通知状态');
    const list = await withNativeTimeout(native.readAppNotificationStates(packageNames), 30_000, '通知状态读取超时，请重试');
    return Array.isArray(list)
      ? list.map((item: any) => ({
          packageName: String(item?.packageName || ''),
          enabled: typeof item?.enabled === 'boolean' ? item.enabled : null,
        }))
          .filter((item: AppNotificationState) => item.packageName)
      : [];
  },
  setAppNotificationEnabled: async (packageName: string, enabled: boolean): Promise<AppNotificationResult> => {
    if (!native?.setAppNotificationEnabled) throw new Error('此设备不支持修改应用通知状态');
    const response = await withNativeTimeout<any>(native.setAppNotificationEnabled(packageName, enabled), 30_000, '系统命令超时，请重试');
    const result = typeof response === 'string' ? response : String(response?.result ?? '');
    if (result === 'enabled' || result === 'disabled' || result === 'already_enabled' || result === 'already_disabled') return result;
    throw new Error('系统没有确认这次修改');
  },
  isQuickComponentsGuideSeen: async (): Promise<boolean> => {
    if (!native?.isQuickComponentsGuideSeen) return true;
    return withNativeTimeout<any>(native.isQuickComponentsGuideSeen(), 6000, '设置读取超时，请重试');
  },
  setQuickComponentsGuideSeen: async (): Promise<void> => {
    if (!native?.setQuickComponentsGuideSeen) return;
    await withNativeTimeout<any>(native.setQuickComponentsGuideSeen(), 6000, '设置保存超时，请重试');
  },
  readAdbGuideSeen: async (): Promise<string[]> => {
    if (!native?.readAdbGuideSeen) return [];
    const seen = await withNativeTimeout<any>(native.readAdbGuideSeen(), 6000, '设置读取超时，请重试');
    return Array.isArray(seen) ? seen.map(String) : [];
  },
  markAdbGuideSeen: async (key: string): Promise<void> => {
    if (native?.markAdbGuideSeen) await withNativeTimeout<any>(native.markAdbGuideSeen(key), 6000, '设置保存超时，请重试');
  },
  scanMarketingChannels: async (): Promise<MarketingChannel[]> => {
    if (!native?.scanMarketingChannels) throw new Error('此设备不支持通知渠道扫描');
    // Privileged dumpsys reads can take up to 35s on the native side.
    return withNativeTimeout(native.scanMarketingChannels(), 40_000, '渠道扫描超时，请重试');
  },
  getNotificationAccessStatus: async (): Promise<NotificationAccessStatus> => {
    if (!native?.getNotificationAccessStatus) return { supported: false, granted: false };
    const status = await withNativeTimeout<NotificationAccessStatus>(native.getNotificationAccessStatus(), 8_000, '权限状态读取超时');
    return { supported: Boolean(status?.supported), granted: Boolean(status?.granted) };
  },
  getUiCleanupTargets: async (): Promise<UiCleanupTarget[]> => {
    if (!native?.getUiCleanupTargets) return [];
    return withNativeTimeout(native.getUiCleanupTargets(), 10_000, '推荐项读取超时，请重试');
  },
  /** Rewrites a vendor switch inside the Android settings tables and verifies the new value. */
  applyUiCleanupDirect: async (id: string, desired: 'on' | 'off'): Promise<UiCleanupDirectResult> => {
    if (!native?.applyUiCleanupDirect) throw new Error('此设备不支持直接改写系统设置');
    const result = String(await withNativeTimeout(native.applyUiCleanupDirect(id, desired), 20_000, '系统命令超时，请重试'));
    return { result: result === 'already' ? 'already' : 'changed', state: desired };
  },
  takeUiCleanupResult: async (): Promise<{ title: string; message: string } | null> => {
    if (!native?.takeUiCleanupResult) return null;
    return withNativeTimeout(native.takeUiCleanupResult(), 8_000, '操作结果读取超时');
  },
  startUiCleanup: async (id: string, restore: boolean): Promise<void> => {
    if (!native?.startUiCleanup || await withNativeTimeout(native.startUiCleanup(id, restore), 10_000, '无法启动这个操作') !== true) throw new Error('无法启动这个操作');
  },
  confirmVendorStartupGuide: async (): Promise<BackendSnapshot | undefined> => {
    if (!native?.confirmVendorStartupGuide) return undefined;
    return normalizeSnapshot(await withNativeTimeout<any>(native.confirmVendorStartupGuide(), 20000, '状态读取超时，请重试'));
  },
};

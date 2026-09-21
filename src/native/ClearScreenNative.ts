import { NativeModules, Platform } from 'react-native';
import { APPS, AppItem, LOGS, LogItem } from '../data';
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
  versionName?: string;
};

type NativeLog = LogItem & { packageName?: string; result?: string; timestamp?: number };

export type BackendPermissions = {
  skip: boolean;
  skipRunning: boolean;
  network: boolean;
  networkRunning: boolean;
  bg: boolean;
  restrictedSettingsLikely: boolean;
};

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

export type BackendSnapshot = {
  backendReady: boolean;
  onboardingCompleted: boolean;
  masterEnabled: boolean;
  installedAppCount: number;
  todaySkipCount: number;
  todayNetworkCount: number;
  permissions: BackendPermissions;
  installSource: InstallSourceDiagnostics;
  apps: AppItem[];
  logs: LogItem[];
  settings: { startup: boolean; autoUpdate: boolean; debug: boolean };
};

const native = Platform.OS === 'android' ? NativeModules.ClearScreenBackend : undefined;

function normalizeApp(app: NativeApp): AppItem {
  return {
    id: app.id || app.packageName || `unknown-${app.name || 'app'}`,
    packageName: app.packageName || app.id,
    name: app.name || app.packageName || '未知应用',
    icon: app.iconUri ? { uri: app.iconUri } : LOGO,
    skip: Boolean(app.skip),
    network: Boolean(app.network),
    whitelist: Boolean(app.whitelist),
    isSystemApp: Boolean(app.isSystemApp),
    versionName: app.versionName,
  };
}

function fallbackSnapshot(): BackendSnapshot {
  return {
    backendReady: false,
    onboardingCompleted: false,
    masterEnabled: true,
    installedAppCount: APPS.length,
    todaySkipCount: 28,
    todayNetworkCount: 136,
    permissions: { skip: true, skipRunning: false, network: true, networkRunning: false, bg: false, restrictedSettingsLikely: false },
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
    apps: APPS,
    logs: LOGS,
    settings: { startup: true, autoUpdate: true, debug: false },
  };
}

export function normalizeSnapshot(raw: any): BackendSnapshot {
  if (!raw || !native) return fallbackSnapshot();
  const apps = Array.isArray(raw.apps) ? raw.apps.map(normalizeApp) : [];
  const logs = Array.isArray(raw.logs) ? raw.logs as LogItem[] : [];
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
    onboardingCompleted: Boolean(raw.onboardingCompleted),
    masterEnabled: Boolean(raw.masterEnabled),
    installedAppCount: Number(raw.installedAppCount || apps.length),
    todaySkipCount: Number(raw.todaySkipCount || 0),
    todayNetworkCount: Number(raw.todayNetworkCount || 0),
    permissions: {
      skip: Boolean(raw.accessibilityEnabled),
      skipRunning: Boolean(raw.accessibilityRunning),
      network: Boolean(raw.vpnPrepared),
      networkRunning: Boolean(raw.vpnRunning),
      bg: Boolean(raw.batteryOptimizationIgnored),
      restrictedSettingsLikely: installSource.restrictedSettingsLikely,
    },
    installSource,
    apps,
    logs,
    settings: {
      startup: raw.settings?.startup !== false,
      autoUpdate: raw.settings?.autoUpdate !== false,
      debug: Boolean(raw.settings?.debug),
    },
  };
}

export const ClearScreenNative = {
  available: Boolean(native),
  getSnapshot: async (): Promise<BackendSnapshot> => {
    if (!native?.getSnapshot) return fallbackSnapshot();
    return normalizeSnapshot(await native.getSnapshot());
  },
  setOnboardingCompleted: async (completed: boolean) => {
    if (native?.setOnboardingCompleted) await native.setOnboardingCompleted(completed);
  },
  setMasterEnabled: async (enabled: boolean) => {
    if (native?.setMasterEnabled) await native.setMasterEnabled(enabled);
  },
  setAppRule: async (app: AppItem) => {
    if (native?.setAppRule) {
      await native.setAppRule(app.packageName || app.id, app.skip, app.network, app.whitelist);
    }
  },
  setSetting: async (key: 'startup' | 'autoUpdate' | 'debug', enabled: boolean) => {
    if (native?.setSetting) await native.setSetting(key, enabled);
  },
  clearLogs: async () => {
    if (!native?.clearLogs) return fallbackSnapshot();
    return normalizeSnapshot(await native.clearLogs());
  },
  openAccessibilitySettings: async () => native?.openAccessibilitySettings?.(),
  openAppDetailsSettings: async () => native?.openAppDetailsSettings?.(),
  startVpn: async () => native?.startVpn?.(),
  stopVpn: async () => native?.stopVpn?.(),
  openBatterySettings: async () => native?.openBatterySettings?.(),
};

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
};

export type BackendSnapshot = {
  backendReady: boolean;
  masterEnabled: boolean;
  installedAppCount: number;
  todaySkipCount: number;
  todayNetworkCount: number;
  permissions: BackendPermissions;
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
    masterEnabled: true,
    installedAppCount: APPS.length,
    todaySkipCount: 28,
    todayNetworkCount: 136,
    permissions: { skip: true, skipRunning: false, network: true, networkRunning: false, bg: false },
    apps: APPS,
    logs: LOGS,
    settings: { startup: true, autoUpdate: true, debug: false },
  };
}

export function normalizeSnapshot(raw: any): BackendSnapshot {
  if (!raw || !native) return fallbackSnapshot();
  const apps = Array.isArray(raw.apps) ? raw.apps.map(normalizeApp) : [];
  const logs = Array.isArray(raw.logs) ? raw.logs as LogItem[] : [];
  return {
    backendReady: Boolean(raw.backendReady),
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
    },
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
  startVpn: async () => native?.startVpn?.(),
  stopVpn: async () => native?.stopVpn?.(),
  openBatterySettings: async () => native?.openBatterySettings?.(),
};

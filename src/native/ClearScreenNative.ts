import { NativeModules, Platform } from 'react-native';
import { AppItem, LogItem } from '../data';
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

export type BackendPermissions = {
  skip: boolean;
  skipRunning: boolean;
  accessibilitySettingEnabled: boolean;
  accessibilityServiceBound: boolean;
  accessibilityProcessAlive: boolean;
  accessibilityStoredServices: string;
  network: boolean;
  networkRunning: boolean;
  bg: boolean;
  isVivoFamily: boolean;
  vendorStartupSettingsAvailable: boolean;
  // Android has no public API for vivo/iQOO's self-start switch. This field
  // means the user completed the guided check, not that the system exposed a
  // verifiable permission grant to the app.
  vendorStartupGuideConfirmed: boolean;
  deviceManufacturer: string;
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
    privacyPolicyVersion: '2026-10-16-v1',
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
      accessibilityStoredServices: '',
      network: false,
      networkRunning: false,
      bg: false,
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
    privacyPolicyVersion: String(raw.privacyPolicyVersion || '2026-10-16-v1'),
    onboardingCompleted: Boolean(raw.onboardingCompleted),
    masterEnabled: Boolean(raw.masterEnabled),
    installedAppCount: Number(raw.installedAppCount || apps.length),
    todaySkipCount: Number(raw.todaySkipCount || 0),
    todayNetworkCount: 0,
    permissions: {
      skip: Boolean(raw.accessibilityEnabled),
      skipRunning: Boolean(raw.accessibilityRunning),
      accessibilitySettingEnabled: Boolean(raw.accessibilitySettingEnabled ?? raw.accessibilityEnabled),
      accessibilityServiceBound: Boolean(raw.accessibilityServiceBound ?? raw.accessibilityRunning),
      accessibilityProcessAlive: Boolean(raw.accessibilityProcessAlive),
      accessibilityStoredServices: String(raw.accessibilityStoredServices || ''),
      network: false,
      networkRunning: false,
      bg: Boolean(raw.batteryOptimizationIgnored),
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
  getSnapshot: async (): Promise<BackendSnapshot> => {
    if (!native?.getSnapshot) return fallbackSnapshot();
    return normalizeSnapshot(await native.getSnapshot());
  },
  acceptPrivacyPolicy: async (): Promise<BackendSnapshot> => {
    if (!native?.acceptPrivacyPolicy) {
      return { ...fallbackSnapshot(), backendReady: true, privacyConsentAccepted: true };
    }
    return normalizeSnapshot(await native.acceptPrivacyPolicy());
  },
  withdrawPrivacyConsent: async (): Promise<BackendSnapshot> => {
    if (!native?.withdrawPrivacyConsent) return fallbackSnapshot();
    return normalizeSnapshot(await native.withdrawPrivacyConsent());
  },
  setOnboardingCompleted: async (completed: boolean) => {
    if (native?.setOnboardingCompleted) await native.setOnboardingCompleted(completed);
  },
  setMasterEnabled: async (enabled: boolean) => {
    if (native?.setMasterEnabled) await native.setMasterEnabled(enabled);
  },
  setAppRule: async (app: AppItem) => {
    if (native?.setAppRule) {
      await native.setAppRule(app.packageName || app.id, app.skip, false, !app.skip);
    }
  },
  setAppRules: async (apps: AppItem[]) => {
    if (native?.setAppRules) {
      await native.setAppRules(apps.map(app => ({
        packageName: app.packageName || app.id,
        skipEnabled: app.skip,
      })));
    } else if (native?.setAppRule) {
      // Keep development clients using an older native module compatible.
      await Promise.all(apps.map(app =>
        native.setAppRule(app.packageName || app.id, app.skip, false, !app.skip),
      ));
    }
  },
  setLogRetentionDays: async (days: number) => {
    if (!native?.setLogRetentionDays) return fallbackSnapshot();
    return normalizeSnapshot(await native.setLogRetentionDays(days));
  },
  setSetting: async (
    key: 'startup' | 'autoUpdate' | 'debug' | 'skipSplashVideo' | 'closePopupAds' | 'closeFeedAds',
    enabled: boolean,
  ) => {
    if (native?.setSetting) await native.setSetting(key, enabled);
  },
  restoreDefaults: async (): Promise<BackendSnapshot> => {
    if (!native?.restoreDefaults) throw new Error('Restoring settings is unavailable');
    return normalizeSnapshot(await native.restoreDefaults());
  },
  clearLogs: async () => {
    if (!native?.clearLogs) return fallbackSnapshot();
    return normalizeSnapshot(await native.clearLogs());
  },
  openAccessibilitySettings: async () => native?.openAccessibilitySettings?.(),
  openAppDetailsSettings: async () => native?.openAppDetailsSettings?.(),
  openBatterySettings: async () => native?.openBatterySettings?.(),
  confirmVendorStartupGuide: async (): Promise<BackendSnapshot | undefined> => {
    if (!native?.confirmVendorStartupGuide) return undefined;
    return normalizeSnapshot(await native.confirmVendorStartupGuide());
  },
};

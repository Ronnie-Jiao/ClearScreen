import React, { useState } from 'react';
import { StatusBar } from 'react-native';
import AppsScreen from '../../src/screens/AppsScreen';
import HomeScreen from '../../src/screens/HomeScreen';
import SettingsScreen from '../../src/screens/SettingsScreen';
import { APPS, type AppItem } from '../../src/data';

type MainTab = 'home' | 'apps' | 'settings';

export default function App() {
  const [activeTab, setActiveTab] = useState<MainTab>('home');
  const [apps, setApps] = useState<AppItem[]>(() => APPS.map((app) => ({ ...app })));
  const [retentionDays, setRetentionDays] = useState(7);

  return (
    <>
      <StatusBar barStyle="dark-content" />
      {activeTab === 'home' ? (
        <HomeScreen
          master={false}
          onAction={() => undefined}
          onTab={setActiveTab}
          onPermissions={() => setActiveTab('settings')}
          onRecords={() => undefined}
          onOpenRecord={() => undefined}
          onOpenApp={() => setActiveTab('apps')}
          apps={apps}
          logs={[]}
          today={{ skip: 0, network: 0 }}
          permissions={{ skip: false, skipRunning: false }}
          checking={false}
          enabledAppCount={apps.filter((app) => app.skip).length}
        />
      ) : activeTab === 'apps' ? (
        <AppsScreen
          apps={apps}
          onChangeApps={setApps}
          onTab={setActiveTab}
          onOpen={() => undefined}
        />
      ) : (
        <SettingsScreen
          logRetentionDays={retentionDays}
          versionName="Harmony 试验"
          onTab={setActiveTab}
          onServicePermissions={() => undefined}
          onClear={() => undefined}
          onRetentionChange={setRetentionDays}
          onPrivacyPolicy={() => undefined}
        />
      )}
    </>
  );
}

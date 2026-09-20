import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Alert, AppState, StyleSheet, View } from 'react-native';
import { StatusBar } from 'expo-status-bar';
import SplashScreen from './src/screens/SplashScreen';
import WelcomeScreen from './src/screens/WelcomeScreen';
import PermissionsScreen from './src/screens/PermissionsScreen';
import HomeScreen from './src/screens/HomeScreen';
import AppsScreen from './src/screens/AppsScreen';
import AppDetailScreen from './src/screens/AppDetailScreen';
import RecordsScreen from './src/screens/RecordsScreen';
import SettingsScreen from './src/screens/SettingsScreen';
import AppearanceScreen from './src/screens/AppearanceScreen';
import WhitelistScreen from './src/screens/WhitelistScreen';
import { AppItem, LogItem } from './src/data';
import { BackendSnapshot, ClearScreenNative } from './src/native/ClearScreenNative';

const UI_SCALE = .84;

type Page='splash'|'welcome'|'permissions'|'home'|'apps'|'appDetail'|'records'|'settings'|'appearance'|'whitelist';
export default function App(){
 const [page,setPage]=useState<Page>('splash');
 const [history,setHistory]=useState<Page[]>([]);
 const [apps,setApps]=useState<AppItem[]>([]);
 const [logs,setLogs]=useState<LogItem[]>([]);
 const [selectedId,setSelectedId]=useState('');
 const [onboardingCompleted,setOnboardingCompleted]=useState(false);
 const [master,setMaster]=useState(false);
 const [permissions,setPermissions]=useState({skip:false,skipRunning:false,network:false,networkRunning:false,bg:false,restrictedSettingsLikely:false});
 const [today,setToday]=useState({skip:0,network:0});
 const [settings,setSettings]=useState({startup:true,autoUpdate:true,debug:false});
 const [backendReady,setBackendReady]=useState(false);
 const accessibilityAttempted=useRef(false);
 const applySnapshot=useCallback((snapshot:BackendSnapshot)=>{
   setBackendReady(snapshot.backendReady);
   setOnboardingCompleted(snapshot.onboardingCompleted);
   setApps(snapshot.apps);
   setLogs(snapshot.logs);
   setMaster(snapshot.masterEnabled);
   setPermissions(snapshot.permissions);
   setToday({skip:snapshot.todaySkipCount,network:snapshot.todayNetworkCount});
   setSettings(snapshot.settings);
   setSelectedId(current=>current||snapshot.apps[0]?.id||'');
 },[]);
 const refresh=useCallback(async():Promise<BackendSnapshot|undefined>=>{
   try{const snapshot=await ClearScreenNative.getSnapshot();applySnapshot(snapshot);return snapshot}catch(error){console.warn('ClearScreen backend refresh failed',error);return undefined}
 },[applySnapshot]);
 useEffect(()=>{
   let alive=true;
   const started=Date.now();
   refresh().then(snapshot=>{if(alive){const wait=Math.max(0,1200-(Date.now()-started));setTimeout(()=>alive&&setPage(snapshot?.onboardingCompleted?'home':'welcome'),wait)}});
   const subscription=AppState.addEventListener('change',state=>{
     if(state!=='active')return;
     refresh().then(snapshot=>{
       if(!alive||!accessibilityAttempted.current||!snapshot)return;
       accessibilityAttempted.current=false;
       if(snapshot.permissions.skip)return;
        const restricted=snapshot.permissions.restrictedSettingsLikely;
        setTimeout(()=>alive&&Alert.alert(
          '还没有完成授权',
          restricted
            ? '系统无障碍开关没有保持开启，当前安装来源可能触发了 Android 的“受限设置”。请打开“应用信息”，查看右上角菜单中是否有“允许受限设置”；如果没有，需要通过 vivo EasyShare 或可信应用商店重新安装。'
            : '系统无障碍开关没有保持开启，请重新打开净屏的无障碍开关；如果仍然自动关闭，请检查系统的后台运行和电池限制。',
          restricted
            ? [
              {text:'稍后',style:'cancel'},
              {text:'打开应用信息',onPress:()=>{ClearScreenNative.openAppDetailsSettings().catch(error=>console.warn('App details settings open failed',error))}},
            ]
            : [
              {text:'稍后',style:'cancel'},
              {text:'重新授权',onPress:()=>{accessibilityAttempted.current=true;ClearScreenNative.openAccessibilitySettings().catch(error=>console.warn('Accessibility settings open failed',error))}},
            ],
        ),200);
     });
   });
   return()=>{alive=false;subscription.remove()};
 },[refresh]);
 const completeOnboarding=useCallback(()=>{
   setOnboardingCompleted(true);
   ClearScreenNative.setOnboardingCompleted(true).catch(error=>console.warn('ClearScreen onboarding update failed',error));
 },[]);
 const go=(p:Page)=>{setHistory(h=>[...h,page]);setPage(p)};
 const back=()=>{setHistory(h=>{const copy=[...h];const p=copy.pop()||'home';setPage(p);return copy})};
 const tab=(t:'home'|'apps'|'settings')=>{setHistory([]);setPage(t)};
 const selected=useMemo(()=>apps.find(a=>a.id===selectedId)||apps[0],[apps,selectedId]);
 const updateApps=useCallback((next:AppItem[])=>{
   const previous=new Map(apps.map(app=>[app.id,app]));
   setApps(next);
   next.forEach(app=>{
     const before=previous.get(app.id);
     if(!before||before.skip!==app.skip||before.network!==app.network||before.whitelist!==app.whitelist){ClearScreenNative.setAppRule(app).catch(error=>console.warn('ClearScreen rule update failed',error))}
   });
 },[apps]);
 const toggleMaster=useCallback(async()=>{
   const next=!master;setMaster(next);
   try{await ClearScreenNative.setMasterEnabled(next);await refresh()}catch(error){setMaster(!next);console.warn('ClearScreen master update failed',error)}
 },[master,refresh]);
 const openPermission=useCallback(async(key:'skip'|'network'|'bg')=>{
   if(key==='skip'){
     if(!permissions.skip&&permissions.restrictedSettingsLikely){
       Alert.alert(
         '需要先确认系统限制',
         '当前安装来源可能触发 Android 的“受限设置”。如果“应用信息”右上角有“允许受限设置”，请先允许；如果没有，请通过 vivo EasyShare 或可信应用商店重新安装后再授权。',
         [
           {text:'取消',style:'cancel'},
           {text:'打开应用信息',onPress:()=>{ClearScreenNative.openAppDetailsSettings().catch(error=>console.warn('App details settings open failed',error))}},
           {text:'直接去无障碍',onPress:()=>{accessibilityAttempted.current=true;ClearScreenNative.openAccessibilitySettings().catch(error=>console.warn('Accessibility settings open failed',error))}},
         ],
       );
       return;
     }
     accessibilityAttempted.current=true;
     await ClearScreenNative.openAccessibilitySettings();
   }
   else if(key==='network')await ClearScreenNative.startVpn();
   else await ClearScreenNative.openBatterySettings();
 },[permissions]);
 const toggleSetting=useCallback((key:'startup'|'autoUpdate'|'debug')=>{
   setSettings(value=>{const next={...value,[key]:!value[key]};ClearScreenNative.setSetting(key,next[key]).catch(error=>console.warn('ClearScreen setting update failed',error));return next});
 },[]);
 let screen:React.ReactNode;
 if(page==='splash')screen=<SplashScreen/>;
 else if(page==='welcome')screen=<WelcomeScreen onStart={()=>{completeOnboarding();go('permissions')}} onLater={()=>{completeOnboarding();tab('home')}}/>;
 else if(page==='permissions')screen=<PermissionsScreen onBack={back} onContinue={()=>{completeOnboarding();refresh();tab('home')}} permissions={{skip:permissions.skip,network:permissions.network,bg:permissions.bg,restrictedSettingsLikely:permissions.restrictedSettingsLikely}} onToggle={openPermission}/>;
 else if(page==='home')screen=<HomeScreen master={master} onMaster={toggleMaster} onTab={tab} onPermissions={()=>go('permissions')} onRecords={()=>go('records')} apps={apps} logs={logs} today={today} permissions={permissions}/>;
 else if(page==='apps')screen=<AppsScreen apps={apps} onChangeApps={updateApps} onTab={tab} onOpen={id=>{setSelectedId(id);go('appDetail')}} onWhitelist={()=>go('whitelist')}/>;
 else if(page==='appDetail'&&selected)screen=<AppDetailScreen app={selected} logs={logs} onBack={back} onChange={a=>updateApps(apps.map(x=>x.id===a.id?a:x))} onRecords={()=>go('records')}/>;
 else if(page==='records')screen=<RecordsScreen apps={apps} logs={logs} onBack={back}/>;
 else if(page==='settings')screen=<SettingsScreen startup={settings.startup} autoUpdate={settings.autoUpdate} debug={settings.debug} permissions={permissions} backendReady={backendReady} onToggle={toggleSetting} onTab={tab} onAppearance={()=>go('appearance')} onPermissions={()=>go('permissions')} onClear={()=>ClearScreenNative.clearLogs().then(applySnapshot)} onRules={()=>Alert.alert('规则管理','本地规则引擎已接入，规则编辑页将在下一阶段开放。')}/>;
 else if(page==='appearance')screen=<AppearanceScreen onBack={back}/>;
 else if(page==='whitelist')screen=<WhitelistScreen apps={apps} onChangeApps={updateApps} onBack={back} onSave={()=>back()}/>;
 else screen=<HomeScreen master={master} onMaster={toggleMaster} onTab={tab} onPermissions={()=>go('permissions')} onRecords={()=>go('records')} apps={apps} logs={logs} today={today} permissions={permissions}/>;
 return <><StatusBar hidden={false} style="light" backgroundColor="#8F9498" translucent={false}/><View style={s.viewport}><View style={s.scaledRoot}>{screen}</View></View></>;
}

const s=StyleSheet.create({
 viewport:{flex:1,overflow:'hidden'},
 scaledRoot:{width:`${100/UI_SCALE}%`,height:`${100/UI_SCALE}%`,transformOrigin:'top left',transform:[{scale:UI_SCALE}]},
});

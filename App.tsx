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
type AccessibilityHandoff='accessibility'|'appInfo'|'background'|null;
export default function App(){
 const [page,setPage]=useState<Page>('splash');
 const [history,setHistory]=useState<Page[]>([]);
 const [apps,setApps]=useState<AppItem[]>([]);
 const [logs,setLogs]=useState<LogItem[]>([]);
 const [selectedId,setSelectedId]=useState('');
 const [onboardingCompleted,setOnboardingCompleted]=useState(false);
 const [master,setMaster]=useState(false);
 const [permissions,setPermissions]=useState({skip:false,skipRunning:false,network:false,networkRunning:false,bg:false,isVivoFamily:false,vendorStartupSettingsAvailable:false,vendorStartupGuideConfirmed:false,deviceManufacturer:'unknown',restrictedSettingsLikely:false});
 const [today,setToday]=useState({skip:0,network:0});
 const [settings,setSettings]=useState({startup:true,autoUpdate:true,debug:false});
 const [backendReady,setBackendReady]=useState(false);
 const accessibilityHandoff=useRef<AccessibilityHandoff>(null);
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
 const openAccessibilitySettings=useCallback(()=>{
   accessibilityHandoff.current='accessibility';
   ClearScreenNative.openAccessibilitySettings().catch(error=>console.warn('Accessibility settings open failed',error));
 },[]);
 const openAppDetailsSettings=useCallback(()=>{
   accessibilityHandoff.current='appInfo';
   ClearScreenNative.openAppDetailsSettings().catch(error=>console.warn('App details settings open failed',error));
 },[]);
 const showRestrictedSettingsGuide=useCallback(()=>{
   Alert.alert(
     '先允许受限设置',
     '这次安装来自 USB 或本地侧载，Android 可能暂时拦截无障碍服务。请在“应用信息”右上角菜单中选择“允许受限设置”，然后回到这里重新打开无障碍。若应用信息里没有这个入口，请通过 vivo EasyShare 或可信应用商店重新安装。',
     [
       {text:'取消',style:'cancel'},
       {text:'打开应用信息',onPress:openAppDetailsSettings},
       {text:'直接去无障碍',onPress:openAccessibilitySettings},
     ],
   );
 },[openAccessibilitySettings,openAppDetailsSettings]);
 useEffect(()=>{
   let alive=true;
   const started=Date.now();
   refresh().then(snapshot=>{if(alive){const wait=Math.max(0,1200-(Date.now()-started));setTimeout(()=>alive&&setPage(snapshot?.onboardingCompleted?'home':'welcome'),wait)}});
   const subscription=AppState.addEventListener('change',state=>{
     if(state!=='active')return;
     refresh().then(snapshot=>{
       const handoff=accessibilityHandoff.current;
       if(!alive||!handoff||!snapshot)return;
       accessibilityHandoff.current=null;
       const vivo=snapshot.permissions.isVivoFamily;
       if(handoff==='background'){
         if(!vivo)return;
         setTimeout(()=>alive&&Alert.alert(
           '完成 vivo 后台保护',
           '请在“自启动管理”中找到“净屏”并打开“自启动”。如果该页还提供电量或后台限制选项，也请允许后台运行。系统不允许应用代替你打开这些开关。',
           [
             {text:'稍后',style:'cancel'},
             {text:'我已完成检查',onPress:()=>{ClearScreenNative.confirmVendorStartupGuide().then(next=>{if(next)applySnapshot(next)}).catch(error=>console.warn('Vendor startup guide confirmation failed',error))}},
             {text:'重新打开自启动',onPress:()=>{accessibilityHandoff.current='background';ClearScreenNative.openBatterySettings().catch(error=>console.warn('Background settings open failed',error))}},
           ],
         ),200);
         return;
       }
       if(handoff==='appInfo'){
         if(snapshot.permissions.skip&&snapshot.permissions.skipRunning)return;
         setTimeout(()=>alive&&Alert.alert(
           '现在打开无障碍',
           '如果你已经在“应用信息”右上角选择了“允许受限设置”，现在可以打开无障碍开关。打开后请在系统确认框中点击“允许”。',
           [
             {text:'稍后',style:'cancel'},
             {text:'打开无障碍',onPress:openAccessibilitySettings},
           ],
         ),200);
         return;
       }
       if(snapshot.permissions.skip&&snapshot.permissions.skipRunning)return;
       // vivo can update the secure setting before the service process is
       // rebound. Confirm both authorization and service liveness over a few
       // reads before showing any warning to the user.
       const confirmAccessibility=async()=>{
         let latest:BackendSnapshot|undefined=snapshot;
         for(const delay of [250,700,1500]){
           await new Promise<void>(resolve=>setTimeout(resolve,delay));
           if(!alive)return;
           latest=await refresh();
           if(latest?.permissions.skipRunning)return;
         }
         if(!alive||!latest)return;
         if(latest.permissions.skip){
           setTimeout(()=>alive&&Alert.alert(
             '无障碍服务正在启动',
             '系统已经保留净屏授权，但服务还没有完成绑定。请稍等片刻后再进入应用；如果仍未启动，再检查 vivo 的自启动和后台运行设置。',
             [
               {text:'稍后',style:'cancel'},
               {text:'打开无障碍',onPress:openAccessibilitySettings},
             ],
           ),200);
           return;
         }
         if(latest.permissions.restrictedSettingsLikely&&!latest.permissions.skip){
           setTimeout(()=>alive&&showRestrictedSettingsGuide(),200);
           return;
         }
         const latestVivo=latest.permissions.isVivoFamily;
         setTimeout(()=>alive&&Alert.alert(
           latestVivo?'vivo 已停止净屏服务':'无障碍开关没有保持开启',
           latestVivo
             ? '已确认系统没有保留净屏的无障碍开关。请先打开“自启动”，再重新开启无障碍；这是 vivo 的后台保护开关，净屏无法自行代开。'
             : '请重新打开净屏的无障碍开关；如果返回后仍关闭，请检查系统的后台运行和电池限制。',
           latestVivo
             ? [
               {text:'稍后',style:'cancel'},
               {text:'打开自启动',onPress:()=>{accessibilityHandoff.current='background';ClearScreenNative.openBatterySettings().catch(error=>console.warn('Background settings open failed',error))}},
               {text:'打开无障碍',onPress:openAccessibilitySettings},
             ]
             : [
               {text:'稍后',style:'cancel'},
               {text:'打开无障碍',onPress:openAccessibilitySettings},
             ],
         ),200);
       };
       void confirmAccessibility();
     });
   });
   return()=>{alive=false;subscription.remove()};
 },[applySnapshot,openAccessibilitySettings,refresh,showRestrictedSettingsGuide]);
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
     if(permissions.restrictedSettingsLikely&&!permissions.skip){
       showRestrictedSettingsGuide();
       return;
     }
     openAccessibilitySettings();
   }
   else if(key==='network')await ClearScreenNative.startVpn();
   else if(permissions.isVivoFamily){
     Alert.alert(
       '开启 vivo 后台保护',
       '净屏会打开系统的“自启动管理”。请找到“净屏”并打开“自启动”，这样 vivo 才不会在返回后清理无障碍服务。',
       [
         {text:'取消',style:'cancel'},
         {text:'打开自启动',onPress:()=>{accessibilityHandoff.current='background';ClearScreenNative.openBatterySettings().catch(error=>console.warn('Background settings open failed',error))}},
       ],
     );
   } else await ClearScreenNative.openBatterySettings();
 },[openAccessibilitySettings,permissions,showRestrictedSettingsGuide]);
 const toggleSetting=useCallback((key:'startup'|'autoUpdate'|'debug')=>{
   setSettings(value=>{const next={...value,[key]:!value[key]};ClearScreenNative.setSetting(key,next[key]).catch(error=>console.warn('ClearScreen setting update failed',error));return next});
 },[]);
 let screen:React.ReactNode;
 if(page==='splash')screen=<SplashScreen/>;
 else if(page==='welcome')screen=<WelcomeScreen onStart={()=>{completeOnboarding();go('permissions')}} onLater={()=>{completeOnboarding();tab('home')}}/>;
 else if(page==='permissions')screen=<PermissionsScreen onBack={back} onContinue={()=>{completeOnboarding();refresh();tab('home')}} permissions={{skip:permissions.skip,skipRunning:permissions.skipRunning,network:permissions.network,bg:permissions.bg,isVivoFamily:permissions.isVivoFamily,vendorStartupGuideConfirmed:permissions.vendorStartupGuideConfirmed}} onToggle={openPermission}/>;
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

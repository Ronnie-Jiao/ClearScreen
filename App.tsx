import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Alert, AppState, BackHandler, Platform, StyleSheet, View } from 'react-native';
import { StatusBar } from 'expo-status-bar';
import SplashScreen from './src/screens/SplashScreen';
import PermissionsScreen from './src/screens/PermissionsScreen';
import HomeScreen from './src/screens/HomeScreen';
import AppsScreen from './src/screens/AppsScreen';
import AppDetailScreen from './src/screens/AppDetailScreen';
import RecordsScreen from './src/screens/RecordsScreen';
import SettingsScreen from './src/screens/SettingsScreen';
import ServicePermissionsScreen from './src/screens/ServicePermissionsScreen';
import WhitelistScreen from './src/screens/WhitelistScreen';
import RecordDetailScreen from './src/screens/RecordDetailScreen';
import PrivacyPolicyScreen from './src/screens/PrivacyPolicyScreen';
import IOSSafariScreen from './src/screens/IOSSafariScreen';
import { AppItem, LogItem } from './src/data';
import { BackendSnapshot, ClearScreenNative } from './src/native/ClearScreenNative';

type Page='splash'|'privacyConsent'|'privacyPolicy'|'permissions'|'home'|'apps'|'appDetail'|'records'|'recordDetail'|'settings'|'servicePermissions'|'whitelist';
type MainTab='home'|'apps'|'settings';
type AccessibilityHandoff='accessibility'|'appInfo'|'background'|null;
type RecordsQuery={appId?:string;todayOnly?:boolean};
export default function App(){
 const [page,setPage]=useState<Page>('splash');
 const [history,setHistory]=useState<Page[]>([]);
 const [apps,setApps]=useState<AppItem[]>([]);
 const [logs,setLogs]=useState<LogItem[]>([]);
 const [selectedId,setSelectedId]=useState('');
 const [selectedLog,setSelectedLog]=useState<LogItem|null>(null);
 const [recordsQuery,setRecordsQuery]=useState<RecordsQuery>({});
 const [accessibilityChecking,setAccessibilityChecking]=useState(false);
 const [onboardingCompleted,setOnboardingCompleted]=useState(false);
 const [master,setMaster]=useState(false);
 const [permissions,setPermissions]=useState({skip:false,skipRunning:false,network:false,networkRunning:false,bg:false,isVivoFamily:false,vendorStartupSettingsAvailable:false,vendorStartupGuideConfirmed:false,deviceManufacturer:'unknown',restrictedSettingsLikely:false});
 const [today,setToday]=useState({skip:0,network:0});
 const [settings,setSettings]=useState({logRetentionDays:7});
 const [versionName,setVersionName]=useState('unknown');
 const accessibilityHandoff=useRef<AccessibilityHandoff>(null);
 const appRuleSaveQueue=useRef<Promise<void>>(Promise.resolve());
 const applySnapshot=useCallback((snapshot:BackendSnapshot)=>{
   setVersionName(snapshot.versionName);
   setOnboardingCompleted(snapshot.onboardingCompleted);
   setApps(snapshot.apps);
   setLogs(snapshot.logs);
   setMaster(snapshot.masterEnabled);
   setPermissions(snapshot.permissions);
   setToday({skip:snapshot.todaySkipCount,network:snapshot.todayNetworkCount});
   setSettings({logRetentionDays:snapshot.settings.logRetentionDays});
   setSelectedId(current=>current||snapshot.apps[0]?.id||'');
 },[]);
 const refresh=useCallback(async():Promise<BackendSnapshot|undefined>=>{
   try{await appRuleSaveQueue.current;const snapshot=await ClearScreenNative.getSnapshot();applySnapshot(snapshot);return snapshot}catch(error){console.warn('ClearScreen backend refresh failed',error);return undefined}
 },[applySnapshot]);
 const openAccessibilitySettings=useCallback(()=>{
   accessibilityHandoff.current='accessibility';
    setAccessibilityChecking(true);
   ClearScreenNative.openAccessibilitySettings().catch(error=>console.warn('Accessibility settings open failed',error));
 },[]);
 const openAppDetailsSettings=useCallback(()=>{
   accessibilityHandoff.current='accessibility';
   setAccessibilityChecking(true);
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
   refresh().then(snapshot=>{if(alive)setTimeout(()=>alive&&setPage(Platform.OS==='ios'?'home':snapshot?.privacyConsentAccepted?(snapshot.onboardingCompleted?'home':'permissions'):'privacyConsent'),500)});
   const subscription=AppState.addEventListener('change',state=>{
     if(state!=='active')return;
     const handoff=accessibilityHandoff.current;
     accessibilityHandoff.current=null;
     if(handoff!=='accessibility'){
       void refresh().then(latest=>{
       if(alive&&Platform.OS==='android'&&!latest?.privacyConsentAccepted){setHistory([]);setPage('privacyConsent')}
       });
       return;
     }
     setAccessibilityChecking(true);
     void (async()=>{
       let latest=await refresh();
       if(!latest?.privacyConsentAccepted){if(alive){setHistory([]);setPage('privacyConsent');setAccessibilityChecking(false)}return}
       if(latest?.permissions.skip&&!latest.permissions.skipRunning){
         for(const delay of [250,700,1500]){
           await new Promise<void>(resolve=>setTimeout(resolve,delay));
           if(!alive)return;
           latest=await refresh();
           if(latest?.permissions.skipRunning)break;
         }
       }
       if(alive)setAccessibilityChecking(false);
     })();
   });
   return()=>{alive=false;subscription.remove()};
 },[refresh]);
 useEffect(()=>{
   if(page!=='privacyConsent')return;
   const subscription=BackHandler.addEventListener('hardwareBackPress',()=>{BackHandler.exitApp();return true});
   return()=>subscription.remove();
 },[page]);
 const acceptPrivacyPolicy=useCallback(async()=>{
   try{
     const snapshot=await ClearScreenNative.acceptPrivacyPolicy();
     applySnapshot(snapshot);
     setHistory([]);
     setPage(snapshot.onboardingCompleted?'home':'permissions');
   }catch(error){Alert.alert('保存失败','请重试。')}
 },[applySnapshot]);
 const withdrawPrivacyConsent=useCallback(()=>Alert.alert(
   '撤回隐私同意？',
   '净屏将停止读取其他应用界面，并删除本机应用规则、处理记录和设置。系统无障碍开关不会由净屏更改。',
   [
     {text:'取消',style:'cancel'},
     {text:'撤回并删除',style:'destructive',onPress:async()=>{
       try{
         const snapshot=await ClearScreenNative.withdrawPrivacyConsent();
         applySnapshot(snapshot);
         setApps([]);setLogs([]);setSelectedId('');setSelectedLog(null);setRecordsQuery({});setHistory([]);
         setPage('privacyConsent');
       }catch(error){
         const snapshot=await ClearScreenNative.getSnapshot().catch(()=>undefined);
         if(snapshot&&!snapshot.privacyConsentAccepted){
           applySnapshot(snapshot);setApps([]);setLogs([]);setSelectedId('');setSelectedLog(null);setHistory([]);setPage('privacyConsent');
           Alert.alert('同意已撤回','本机数据未能确认清除，请在系统设置中清除净屏数据。');
         }else Alert.alert('撤回失败','请重试。');
       }
     }}
   ],
 ),[applySnapshot]);
 const completeOnboarding=useCallback(()=>{
   setOnboardingCompleted(true);
   ClearScreenNative.setOnboardingCompleted(true).catch(error=>console.warn('ClearScreen onboarding update failed',error));
 },[]);
 const go=(p:Page)=>{setHistory(h=>[...h,page]);setPage(p)};
 const back=()=>{setHistory(h=>{const copy=[...h];const p=copy.pop()||'home';setPage(p);return copy})};
 const tab=(t:MainTab)=>{setHistory([]);setPage(t)};
 const goRecords=(query:RecordsQuery={})=>{setRecordsQuery(query);go('records')};
 const openRecord=(log:LogItem)=>{setSelectedLog(log);go('recordDetail')};
 const selected=useMemo(()=>apps.find(a=>a.id===selectedId)||apps[0],[apps,selectedId]);
 const updateApps=useCallback((next:AppItem[])=>{
   const previous=apps;
   const previousById=new Map(previous.map(app=>[app.id,app]));
   const normalized=next.map(app=>({...app,network:false,whitelist:!app.skip}));
   const changed=normalized.filter(app=>previousById.get(app.id)?.skip!==app.skip);
   setApps(normalized);
   if(!changed.length)return;
   const save=appRuleSaveQueue.current.then(async()=>{
     await ClearScreenNative.setAppRules(changed);
   });
   appRuleSaveQueue.current=save.catch(async error=>{
     console.warn('ClearScreen rule update failed',error);
     const snapshot=await ClearScreenNative.getSnapshot().catch(()=>undefined);
     if(snapshot)applySnapshot(snapshot);
     else setApps(previous);
     Alert.alert('保存失败','应用设置未保存，请重试。');
   });
 },[apps,applySnapshot]);
 const toggleMaster=useCallback(async()=>{
   const next=!master;setMaster(next);
   try{await ClearScreenNative.setMasterEnabled(next);await refresh()}catch(error){setMaster(!next);console.warn('ClearScreen master update failed',error)}
 },[master,refresh]);
  const openPermission=useCallback(async(key:'skip'|'bg')=>{
    if(key==='skip'){
      if(permissions.restrictedSettingsLikely&&!permissions.skip){
        showRestrictedSettingsGuide();
        return;
      }
      openAccessibilitySettings();
    }
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
 const enabledAppCount=apps.filter(app=>app.skip).length;
 const handleRetentionChange=async(days:number)=>{
   const snapshot=await ClearScreenNative.setLogRetentionDays(days);
   if(snapshot)applySnapshot(snapshot);
 };
 const handleRestoreDefaults=async()=>{
   const snapshot=await ClearScreenNative.restoreDefaults();
   applySnapshot(snapshot);
 };
 const handleClearLogs=()=>Alert.alert('清空记录？','清空后无法恢复。',[
   {text:'取消',style:'cancel'},
   {text:'清空',style:'destructive',onPress:()=>{ClearScreenNative.clearLogs().then(applySnapshot).catch(error=>Alert.alert('清空失败',String(error)))}}
 ]);
 const handleHomeAction=()=>{
   if(!permissions.skip){go('permissions');return}
   if(!permissions.skipRunning){go('permissions');return}
   if(enabledAppCount===0){tab('apps');return}
   if(!master){void toggleMaster();return}
   void toggleMaster();
 };
 const goApp=(id:string)=>{setSelectedId(id);go('appDetail')};
 const stopApp=(id:string)=>updateApps(apps.map(app=>app.id===id?{...app,skip:false,whitelist:true}:app));
 const permissionsProps={
   skip:permissions.skip,
   skipRunning:permissions.skipRunning,
   checking:accessibilityChecking,
   restrictedSettingsLikely:permissions.restrictedSettingsLikely,
 };
 const isMainTab=page==='home'||page==='apps'||page==='settings';
 let overlay:React.ReactNode=null;
 if(page==='splash')overlay=<SplashScreen/>;
 else if(page==='permissions')overlay=<PermissionsScreen onBack={back} onContinue={async()=>{
   completeOnboarding();
   const snapshot=await refresh();
   if(snapshot?.permissions.skip&&snapshot.permissions.skipRunning)tab('apps');
   else tab('home');
 }} onLater={()=>{completeOnboarding();tab('home')}} permissions={permissionsProps} onOpenAccessibility={()=>openPermission('skip')} onOpenAppInfo={openAppDetailsSettings} onHelp={()=>Alert.alert('开启方法','在系统设置中找到“净屏”，打开无障碍服务后返回此页。')}/>;
 else if(page==='privacyConsent')overlay=<PrivacyPolicyScreen mode="consent" onBack={()=>BackHandler.exitApp()} onAccept={()=>{void acceptPrivacyPolicy()}} onDecline={()=>BackHandler.exitApp()}/>;
 else if(page==='privacyPolicy')overlay=<PrivacyPolicyScreen mode="read" onBack={back} onWithdrawConsent={Platform.OS==='android'?withdrawPrivacyConsent:undefined}/>;
 else if(page==='appDetail'&&selected)overlay=<AppDetailScreen app={selected} logs={logs} onBack={back} onChange={a=>updateApps(apps.map(x=>x.id===a.id?a:x))} onRecords={()=>goRecords({appId:selected.id})} onOpenRecord={openRecord}/>;
 else if(page==='recordDetail'&&selectedLog){
   const logApp=apps.find(app=>app.id===selectedLog.appId);
   overlay=<RecordDetailScreen log={selectedLog} app={logApp} onBack={back} onOpenApp={()=>logApp&&goApp(logApp.id)} onStopApp={()=>logApp&&stopApp(logApp.id)}/>;
 }
 else if(page==='whitelist')overlay=<WhitelistScreen apps={apps} onChangeApps={updateApps} onBack={back}/>;
 else if(page==='servicePermissions')overlay=<ServicePermissionsScreen permissions={permissions} logs={logs} onBack={back} onPermissions={()=>go('permissions')} onBackground={()=>openPermission('bg')}/>;
 const homeScreen=Platform.OS==='ios'
   ? <IOSSafariScreen onPrivacyPolicy={()=>go('privacyPolicy')}/>
   : <HomeScreen master={master} onAction={handleHomeAction} checking={accessibilityChecking} onTab={tab} onPermissions={()=>go('permissions')} onRecords={()=>goRecords({todayOnly:true})} onOpenRecord={openRecord} onOpenApp={goApp} apps={apps} logs={logs} today={today} permissions={permissions} enabledAppCount={enabledAppCount}/>;
 const appsScreen=<AppsScreen apps={apps} onChangeApps={updateApps} onTab={tab} onOpen={goApp}/>;
 const settingsScreen=<SettingsScreen logRetentionDays={settings.logRetentionDays} versionName={versionName} logs={logs} permissions={permissions} onTab={tab} onServicePermissions={()=>go('servicePermissions')} onClear={handleClearLogs} onRetentionChange={days=>handleRetentionChange(days).catch(error=>Alert.alert('保存失败',String(error)))} onPrivacyPolicy={()=>go('privacyPolicy')} onRestoreDefaults={Platform.OS==='android'?handleRestoreDefaults:undefined}/>;
 const recordsScreen=<RecordsScreen apps={apps} logs={logs} onBack={back} onOpenRecord={openRecord} filterAppId={recordsQuery.appId} todayOnly={recordsQuery.todayOnly}/>;
 return <><StatusBar hidden={false} style="dark" backgroundColor="#EAF9FF" translucent={false}/><View style={s.viewport}>
   <View style={[s.screenLayer,{display:page==='home'?'flex':'none'}]}>{homeScreen}</View>
   <View style={[s.screenLayer,{display:page==='apps'?'flex':'none'}]}>{appsScreen}</View>
   <View style={[s.screenLayer,{display:page==='settings'?'flex':'none'}]}>{settingsScreen}</View>
   <View style={[s.screenLayer,{display:page==='records'?'flex':'none'}]}>{recordsScreen}</View>
   {!isMainTab&&page!=='records'&&overlay?<View style={s.screenLayer}>{overlay}</View>:null}
 </View></>;
}

const s=StyleSheet.create({
 viewport:{flex:1,overflow:'hidden',backgroundColor:'#EAF9FF'},
 screenLayer:{...StyleSheet.absoluteFillObject},
});

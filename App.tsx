import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { AppState, BackHandler, Platform, StyleSheet, View } from 'react-native';
import { StatusBar } from 'expo-status-bar';
import { initialWindowMetrics, SafeAreaProvider, useSafeAreaInsets } from 'react-native-safe-area-context';
import { ScreenInsetsContext } from './src/components/ScreenInsets';
import { AppDialogProvider, useAppAlert } from './src/components/AppDialog';
import SplashScreen from './src/screens/SplashScreen';
import PermissionsScreen from './src/screens/PermissionsScreen';
import HomeScreen from './src/screens/HomeScreen';
import AppsScreen from './src/screens/AppsScreen';
import AppDetailScreen from './src/screens/AppDetailScreen';
import RecordsScreen from './src/screens/RecordsScreen';
import SettingsScreen from './src/screens/SettingsScreen';
import SettingsDetailScreen, { type SettingsSection } from './src/screens/SettingsDetailScreen';
import ServicePermissionsScreen from './src/screens/ServicePermissionsScreen';
import WhitelistScreen from './src/screens/WhitelistScreen';
import RecordDetailScreen from './src/screens/RecordDetailScreen';
import PrivacyPolicyScreen from './src/screens/PrivacyPolicyScreen';
import IOSSafariScreen from './src/screens/IOSSafariScreen';
import { AppItem, LogItem } from './src/data';
import { BackendSnapshot, ClearScreenNative, type BackendPermissions } from './src/native/ClearScreenNative';

type Page='splash'|'privacyConsent'|'privacyPolicy'|'permissions'|'home'|'apps'|'appDetail'|'records'|'recordDetail'|'settings'|'servicePermissions'|'whitelist'|SettingsSection;
type MainTab='home'|'apps'|'settings';
type SettingsHandoff='accessibility'|'appInfo'|'startup'|'background'|'battery'|null;
type RecordsQuery={appId?:string;todayOnly?:boolean};
export default function App(){
 return <SafeAreaProvider initialMetrics={initialWindowMetrics}><AppDialogProvider><AppContent/></AppDialogProvider></SafeAreaProvider>;
}
function AppContent(){
 const showAlert=useAppAlert();
 const screenInsets=useSafeAreaInsets();
 const [page,setPage]=useState<Page>('splash');
 const [history,setHistory]=useState<Page[]>([]);
 const [apps,setApps]=useState<AppItem[]>([]);
 const [logs,setLogs]=useState<LogItem[]>([]);
 const [selectedId,setSelectedId]=useState('');
 const [selectedLog,setSelectedLog]=useState<LogItem|null>(null);
 const [recordsQuery,setRecordsQuery]=useState<RecordsQuery>({});
 const [accessibilityChecking,setAccessibilityChecking]=useState(false);
 const [permissionVerification,setPermissionVerification]=useState<'idle'|'checking'|'failed'>('idle');
 const [onboardingCompleted,setOnboardingCompleted]=useState(false);
 const [master,setMaster]=useState(false);
 const [permissions,setPermissions]=useState<BackendPermissions>({skip:false,skipRunning:false,accessibilitySettingEnabled:false,accessibilityServiceBound:false,accessibilityProcessAlive:false,accessibilityStoredServices:'',network:false,networkRunning:false,bg:false,batteryOptimizationIgnored:null,backgroundRestricted:null,startupAllowed:null,backgroundAllowed:null,isVivoFamily:false,vendorStartupSettingsAvailable:false,vendorStartupGuideConfirmed:false,deviceManufacturer:'unknown',restrictedSettingsLikely:false});
 const [today,setToday]=useState({skip:0,network:0});
 const [settings,setSettings]=useState({logRetentionDays:7});
 const persistedRetentionDays=useRef(7);
 const requestedRetentionDays=useRef(7);
 const retentionChangeRevision=useRef(0);
 const settledRetentionRevision=useRef(0);
 const [versionName,setVersionName]=useState('unknown');
 const settingsHandoff=useRef<SettingsHandoff>(null);
 const settingsOpening=useRef(false);
 const permissionRefreshVersion=useRef(0);
 const permissionStateRevision=useRef(0);
 const appRuleSaveQueue=useRef<Promise<void>>(Promise.resolve());
 const applySnapshot=useCallback((snapshot:BackendSnapshot,includePermissions=true)=>{
   setVersionName(snapshot.versionName);
   setOnboardingCompleted(snapshot.onboardingCompleted);
   setApps(snapshot.apps);
   setLogs(snapshot.logs);
   setMaster(snapshot.masterEnabled);
   if(includePermissions){permissionStateRevision.current+=1;setPermissions(snapshot.permissions)}
   setToday({skip:snapshot.todaySkipCount,network:snapshot.todayNetworkCount});
   persistedRetentionDays.current=snapshot.settings.logRetentionDays;
   const retentionDays=retentionChangeRevision.current===settledRetentionRevision.current
     ? snapshot.settings.logRetentionDays
     : requestedRetentionDays.current;
   requestedRetentionDays.current=retentionDays;
   setSettings({logRetentionDays:retentionDays});
   setSelectedId(current=>current||snapshot.apps[0]?.id||'');
 },[]);
 const refresh=useCallback(async(shouldApply:()=>boolean=()=>true,includePermissions=true):Promise<BackendSnapshot|undefined>=>{
   try{
     await appRuleSaveQueue.current;
     if(!shouldApply())return undefined;
     const revision=permissionStateRevision.current;
     const snapshot=await ClearScreenNative.getSnapshot();
     if(shouldApply())applySnapshot(snapshot,includePermissions&&revision===permissionStateRevision.current);
     return snapshot;
   }catch(error){console.warn('ClearScreen backend refresh failed',error);return undefined}
 },[applySnapshot]);
 const refreshPermissionSnapshot=useCallback(async(shouldApply:()=>boolean)=>{
   try{
     if(!shouldApply())return undefined;
     const snapshot=await ClearScreenNative.getPermissionSnapshot();
     if(shouldApply()){permissionStateRevision.current+=1;setPermissions(snapshot.permissions)}
     return snapshot;
   }catch(error){console.warn('Permission state refresh failed',error);return undefined}
 },[]);
 const openSystemSettings=useCallback(async(destination:Exclude<SettingsHandoff,null>,open:()=>Promise<void>)=>{
   if(settingsOpening.current)return;
   settingsOpening.current=true;
   permissionRefreshVersion.current+=1;
   settingsHandoff.current=destination;
   setPermissionVerification('idle');
   setAccessibilityChecking(destination==='accessibility'||destination==='appInfo');
   try{await open()}catch(error){
     settingsHandoff.current=null;
     setAccessibilityChecking(false);
     showAlert('无法打开设置','请在手机系统设置中找到“净屏”后设置。',undefined,{tone:'error'});
     console.warn('System settings open failed',error);
   }finally{settingsOpening.current=false}
 },[showAlert]);
 const openAccessibilitySettings=useCallback(()=>{
   void openSystemSettings('accessibility',ClearScreenNative.openAccessibilitySettings);
 },[openSystemSettings]);
 const openAppDetailsSettings=useCallback(()=>{
   void openSystemSettings('appInfo',ClearScreenNative.openAppDetailsSettings);
 },[openSystemSettings]);
 const showRestrictedSettingsGuide=useCallback(()=>{
   showAlert(
     '先允许受限设置',
     '这次安装来自 USB 或本地侧载，Android 可能暂时拦截无障碍服务。请在“应用信息”右上角菜单中选择“允许受限设置”，然后回到这里重新打开无障碍。若应用信息里没有这个入口，请通过 vivo EasyShare 或可信应用商店重新安装。',
     [
       {text:'取消',style:'cancel'},
       {text:'打开应用信息',onPress:openAppDetailsSettings},
       {text:'直接去无障碍',onPress:openAccessibilitySettings},
     ],
   );
 },[openAccessibilitySettings,openAppDetailsSettings,showAlert]);
 useEffect(()=>{
   let alive=true;
   let previousState=AppState.currentState;
   refresh(()=>alive).then(snapshot=>{if(alive)setTimeout(()=>alive&&setPage(Platform.OS==='ios'?'home':snapshot?.privacyConsentAccepted?(snapshot.onboardingCompleted?'home':'permissions'):'privacyConsent'),500)});
   const subscription=AppState.addEventListener('change',state=>{
     if(state===previousState)return;
     previousState=state;
     const version=++permissionRefreshVersion.current;
     if(state!=='active')return;
     const current=()=>alive&&version===permissionRefreshVersion.current&&AppState.currentState==='active';
     const handoff=settingsHandoff.current;
     settingsHandoff.current=null;
     const accessibilityReturn=handoff==='accessibility'||handoff==='appInfo';
     setPermissionVerification('checking');
     if(accessibilityReturn)setAccessibilityChecking(true);
     void (async()=>{
       try{
         // Read permissions first, on their independent native worker. Hundreds
         // of app icons must not keep the settings page in "checking" state.
         let latest=await refreshPermissionSnapshot(current);
         if(!current())return;
         if(!latest){setPermissionVerification('failed');return}
         if(Platform.OS==='android'&&!latest.privacyConsentAccepted){setHistory([]);setPage('privacyConsent');setPermissionVerification('idle');return}
         setPermissionVerification('idle');
         const needsBinding=latest.permissions.accessibilitySettingEnabled&&!latest.permissions.skipRunning;
         const delays=accessibilityReturn?(needsBinding?[250,700,1500]:[]):handoff?[350,850]:[];
         for(const delay of delays){
           await new Promise<void>(resolve=>setTimeout(resolve,delay));
           if(!current())return;
           latest=await refreshPermissionSnapshot(current);
           if(!current())return;
           if(!latest){setPermissionVerification('failed');return}
           if(Platform.OS==='android'&&!latest.privacyConsentAccepted){setHistory([]);setPage('privacyConsent');setPermissionVerification('idle');return}
           if(accessibilityReturn&&(latest.permissions.skipRunning||!latest.permissions.accessibilitySettingEnabled))break;
         }
         setPermissionVerification('idle');
       }finally{
         if(current()){
           setAccessibilityChecking(false);
           // Refresh history/inventory separately without replacing newer
           // permission-only results, including on a bounded read failure.
           void refresh(current,false);
         }
       }
     })();
   });
   return()=>{alive=false;subscription.remove()};
 },[refresh,refreshPermissionSnapshot]);
 useEffect(()=>{
   if(page!=='servicePermissions'||AppState.currentState!=='active')return;
   let alive=true;
   const version=++permissionRefreshVersion.current;
   const current=()=>alive&&version===permissionRefreshVersion.current&&AppState.currentState==='active';
   setPermissionVerification('checking');
   void refreshPermissionSnapshot(current).then(snapshot=>{
     if(current())setPermissionVerification(snapshot?'idle':'failed');
   });
   return()=>{alive=false};
 },[page,refreshPermissionSnapshot]);
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
   }catch(error){showAlert('保存失败','请重试。',undefined,{tone:'error'})}
 },[applySnapshot,showAlert]);
 const withdrawPrivacyConsent=useCallback(()=>showAlert(
   '撤回隐私同意？',
   '净屏将停止读取其他应用界面，并删除本机应用规则、处理记录和设置。系统无障碍开关不会由净屏更改。',
   [
     {text:'取消',style:'cancel'},
     {text:'撤回并删除',style:'destructive',onPress:async()=>{
       permissionRefreshVersion.current+=1;
       settingsHandoff.current=null;
       setPermissionVerification('idle');
       setAccessibilityChecking(false);
       try{
         const snapshot=await ClearScreenNative.withdrawPrivacyConsent();
         applySnapshot(snapshot);
         setApps([]);setLogs([]);setSelectedId('');setSelectedLog(null);setRecordsQuery({});setHistory([]);
         setPage('privacyConsent');
       }catch(error){
         const snapshot=await ClearScreenNative.getSnapshot().catch(()=>undefined);
         if(snapshot&&!snapshot.privacyConsentAccepted){
           applySnapshot(snapshot);setApps([]);setLogs([]);setSelectedId('');setSelectedLog(null);setHistory([]);setPage('privacyConsent');
           showAlert('同意已撤回','本机数据未能确认清除，请在系统设置中清除净屏数据。',undefined,{tone:'warning'});
         }else showAlert('撤回失败','请重试。',undefined,{tone:'error'});
       }
     }}
   ],
 ),[applySnapshot,showAlert]);
 const completeOnboarding=useCallback(()=>{
   setOnboardingCompleted(true);
   ClearScreenNative.setOnboardingCompleted(true).catch(error=>console.warn('ClearScreen onboarding update failed',error));
 },[]);
 const go=(p:Page)=>{setHistory(h=>[...h,page]);setPage(p)};
 const back=()=>{setHistory(h=>{const copy=[...h];const p=copy.pop()||'home';setPage(p);return copy})};
 const tab=(t:MainTab)=>{setHistory([]);setPage(t)};
 useEffect(()=>{
   const settingsDetail=page==='privacyData'||page==='helpFeedback'||page==='about';
   const settingsChild=(page==='privacyPolicy'||page==='servicePermissions'||page==='permissions')&&history.includes('settings');
   if(!settingsDetail&&!settingsChild)return;
   const subscription=BackHandler.addEventListener('hardwareBackPress',()=>{
     setHistory(history.slice(0,-1));
     setPage(history[history.length-1]||'settings');
     return true;
   });
   return()=>subscription.remove();
 },[page,history]);
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
     showAlert('保存失败','应用设置未保存，请重试。',undefined,{tone:'error'});
   });
 },[apps,applySnapshot,showAlert]);
 const toggleMaster=useCallback(async()=>{
   const next=!master;setMaster(next);
   try{await ClearScreenNative.setMasterEnabled(next);await refresh()}catch(error){setMaster(!next);console.warn('ClearScreen master update failed',error)}
 },[master,refresh]);
  const openPermission=useCallback(async(key:'skip'|'startup'|'bg'|'battery')=>{
    if(key==='skip'){
      if(permissions.restrictedSettingsLikely&&!permissions.accessibilitySettingEnabled){
        showRestrictedSettingsGuide();
        return;
      }
      openAccessibilitySettings();
    }
   else if(key==='startup')await openSystemSettings('startup',ClearScreenNative.openStartupSettings);
   else if(key==='bg')await openSystemSettings('background',ClearScreenNative.openBackgroundSettings);
   else await openSystemSettings('battery',ClearScreenNative.openBatterySettings);
 },[openAccessibilitySettings,openSystemSettings,permissions,showRestrictedSettingsGuide]);
 const enabledAppCount=apps.filter(app=>app.skip).length;
 const handleRetentionChange=async(days:number)=>{
   const revision=++retentionChangeRevision.current;
   requestedRetentionDays.current=days;
   setSettings(current=>({...current,logRetentionDays:days}));
   try{
     await ClearScreenNative.setLogRetentionDays(days);
     persistedRetentionDays.current=days;
     if(revision===retentionChangeRevision.current)settledRetentionRevision.current=revision;
   }catch(error){
     if(revision===retentionChangeRevision.current){
       const rollback=persistedRetentionDays.current;
       requestedRetentionDays.current=rollback;
       settledRetentionRevision.current=revision;
       setSettings(current=>({...current,logRetentionDays:rollback}));
     }
     throw error;
   }
 };
 const handleRestoreDefaults=async()=>{
   const snapshot=await ClearScreenNative.restoreDefaults();
   applySnapshot(snapshot);
 };
 const handleClearLogs=()=>showAlert('清空记录？','清空后无法恢复。',[
   {text:'取消',style:'cancel'},
   {text:'清空',style:'destructive',onPress:()=>{ClearScreenNative.clearLogs().then(applySnapshot).catch(error=>showAlert('清空失败',String(error),undefined,{tone:'error'}))}}
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
 }} onLater={()=>{completeOnboarding();tab('home')}} permissions={permissionsProps} onOpenAccessibility={()=>openPermission('skip')} onOpenAppInfo={openAppDetailsSettings} onHelp={()=>showAlert('开启方法','在系统设置中找到“净屏”，打开无障碍服务后返回此页。')}/>;
 else if(page==='privacyConsent')overlay=<PrivacyPolicyScreen mode="consent" onBack={()=>BackHandler.exitApp()} onAccept={()=>{void acceptPrivacyPolicy()}} onDecline={()=>BackHandler.exitApp()}/>;
 else if(page==='privacyPolicy')overlay=<PrivacyPolicyScreen mode="read" onBack={back}/>;
 else if(page==='appDetail'&&selected)overlay=<AppDetailScreen app={selected} logs={logs} onBack={back} onChange={a=>updateApps(apps.map(x=>x.id===a.id?a:x))} onRecords={()=>goRecords({appId:selected.id})} onOpenRecord={openRecord}/>;
 else if(page==='recordDetail'&&selectedLog){
   const logApp=apps.find(app=>app.id===selectedLog.appId);
   overlay=<RecordDetailScreen log={selectedLog} app={logApp} onBack={back} onOpenApp={()=>logApp&&goApp(logApp.id)} onStopApp={()=>logApp&&stopApp(logApp.id)}/>;
 }
 else if(page==='whitelist')overlay=<WhitelistScreen apps={apps} onChangeApps={updateApps} onBack={back}/>;
 else if(page==='servicePermissions')overlay=<ServicePermissionsScreen permissions={permissions} checking={permissionVerification==='checking'} verificationFailed={permissionVerification==='failed'} onBack={back} onOpenAccessibility={()=>openPermission('skip')} onStartup={()=>openPermission('startup')} onBackground={()=>openPermission('bg')} onBatteryOptimization={()=>openPermission('battery')}/>;
 else if(page==='privacyData'||page==='helpFeedback'||page==='about')overlay=<SettingsDetailScreen key={page} section={page} logRetentionDays={settings.logRetentionDays} versionName={versionName} onBack={back} onClear={handleClearLogs} onRetentionChange={days=>handleRetentionChange(days).catch(error=>showAlert('保存失败',String(error),undefined,{tone:'error'}))} onWithdrawConsent={Platform.OS==='android'?withdrawPrivacyConsent:undefined}/>;
 const homeScreen=Platform.OS==='ios'
   ? <IOSSafariScreen onPrivacyPolicy={()=>go('privacyPolicy')}/>
   : <HomeScreen master={master} onAction={handleHomeAction} checking={accessibilityChecking} onTab={tab} onPermissions={()=>go('permissions')} onRecords={()=>goRecords({todayOnly:true})} onOpenRecord={openRecord} onOpenApp={goApp} apps={apps} logs={logs} today={today} permissions={permissions} enabledAppCount={enabledAppCount}/>;
 const appsScreen=<AppsScreen apps={apps} onChangeApps={updateApps} onTab={tab} onOpen={goApp}/>;
 const settingsScreen=<SettingsScreen onTab={tab} onServicePermissions={()=>go('servicePermissions')} onOpenSection={go} onPrivacyPolicy={()=>go('privacyPolicy')} onRestoreDefaults={Platform.OS==='android'?handleRestoreDefaults:undefined}/>;
 const recordsScreen=<RecordsScreen apps={apps} logs={logs} onBack={back} onOpenRecord={openRecord} filterAppId={recordsQuery.appId} todayOnly={recordsQuery.todayOnly}/>;
 return <ScreenInsetsContext.Provider value={screenInsets}><StatusBar hidden={false} style="dark"/><View style={s.viewport}>
   <View style={[s.screenLayer,{display:page==='home'?'flex':'none'}]}>{homeScreen}</View>
   <View style={[s.screenLayer,{display:page==='apps'?'flex':'none'}]}>{appsScreen}</View>
   <View style={[s.screenLayer,{display:page==='settings'?'flex':'none'}]}>{settingsScreen}</View>
   <View style={[s.screenLayer,{display:page==='records'?'flex':'none'}]}>{recordsScreen}</View>
   {!isMainTab&&page!=='records'&&overlay?<View style={s.screenLayer}>{overlay}</View>:null}
 </View></ScreenInsetsContext.Provider>;
}

const s=StyleSheet.create({
 viewport:{flex:1,overflow:'hidden',backgroundColor:'#EAF9FF'},
 screenLayer:{...StyleSheet.absoluteFillObject},
});

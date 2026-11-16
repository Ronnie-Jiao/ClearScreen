import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { AppState, BackHandler, Platform, StyleSheet, View } from 'react-native';
import { StatusBar } from 'expo-status-bar';
import { useFonts } from 'expo-font';
import { initialWindowMetrics, SafeAreaProvider, useSafeAreaInsets } from 'react-native-safe-area-context';
import { FONT_TAGLINE, FONT_TAGLINE_SOURCE } from './src/assets';
import { ScreenInsetsContext } from './src/components/ScreenInsets';
import { AppDialogProvider, useAppAlert } from './src/components/AppDialog';
import HomeScreen from './src/screens/HomeScreen';
import AppsScreen from './src/screens/AppsScreen';
import AdsScreen from './src/screens/AdsScreen';
import AppDetailScreen from './src/screens/AppDetailScreen';
import RecordsScreen from './src/screens/RecordsScreen';
import LearnedCloseRecords from './src/screens/LearnedCloseRecords';
import SettingsScreen from './src/screens/SettingsScreen';
import SettingsDetailScreen, { type HelpDestination, type SettingsSection } from './src/screens/SettingsDetailScreen';
import ServicePermissionsScreen from './src/screens/ServicePermissionsScreen';
import AppNotificationsScreen from './src/screens/AppNotificationsScreen';
import MarketingChannelsScreen from './src/screens/MarketingChannelsScreen';
import SystemRecommendScreen from './src/screens/SystemRecommendScreen';
import WhitelistScreen from './src/screens/WhitelistScreen';
import RecordDetailScreen from './src/screens/RecordDetailScreen';
import IOSSafariScreen from './src/screens/IOSSafariScreen';
import { AppItem, LogItem, LearnedCloseRecord } from './src/data';
import { BackendSnapshot, ClearScreenNative, pickAccessibilityPermissions, type BackendPermissions, type AdLearningState } from './src/native/ClearScreenNative';

type Page='home'|'apps'|'appManagement'|'appDetail'|'records'|'recordDetail'|'learnedRecords'|'settings'|'servicePermissions'|'systemRecommend'|'appNotifications'|'marketingChannels'|'whitelist'|SettingsSection;
type MainTab='home'|'apps'|'settings';
type SettingsHandoff='accessibility'|'startup'|'background'|'battery'|null;
type RecordsQuery={appId?:string;todayOnly?:boolean};
export default function App(){
 const [fontsLoaded]=useFonts({[FONT_TAGLINE]:FONT_TAGLINE_SOURCE});
 return <SafeAreaProvider initialMetrics={initialWindowMetrics}>{fontsLoaded?<AppDialogProvider><AppContent/></AppDialogProvider>:null}</SafeAreaProvider>;
}
function AppContent(){
 const showAlert=useAppAlert();
 const screenInsets=useSafeAreaInsets();
 const sharedInsets=useMemo(()=>({top:screenInsets.top,bottom:screenInsets.bottom}),[screenInsets.top,screenInsets.bottom]);
 const [page,setPage]=useState<Page>('home');
 const [history,setHistory]=useState<Page[]>([]);
 const [apps,setApps]=useState<AppItem[]>([]);
 const [logs,setLogs]=useState<LogItem[]>([]);
 const [selectedId,setSelectedId]=useState('');
 const [selectedLog,setSelectedLog]=useState<LogItem|null>(null);
 const [recordsQuery,setRecordsQuery]=useState<RecordsQuery>({});
 const [accessibilityChecking,setAccessibilityChecking]=useState(false);
 const [permissionVerification,setPermissionVerification]=useState<'idle'|'checking'|'failed'>('idle');
 const [master,setMaster]=useState(false);
 const [learning,setLearning]=useState<AdLearningState>({active:false,count:0,message:'',records:[]});
 const learningBusy=useRef(false);
 const learningRevision=useRef(0);
 const [permissions,setPermissions]=useState<BackendPermissions>({skip:false,skipRunning:false,accessibilitySettingEnabled:false,accessibilityServiceBound:false,accessibilityProcessAlive:false,accessibilityHealth:'unknown',accessibilityStoredServices:'',network:false,networkRunning:false,bg:false,batteryOptimizationIgnored:null,backgroundRestricted:null,startupAllowed:null,backgroundAllowed:null,isVivoFamily:false,vendorStartupSettingsAvailable:false,vendorStartupGuideConfirmed:false,deviceManufacturer:'unknown',restrictedSettingsLikely:false});
 const [settings,setSettings]=useState({logRetentionDays:7});
 const [helpSearchInput,setHelpSearchInput]=useState('');
 const [helpSearchTerm,setHelpSearchTerm]=useState('');
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
   setApps(snapshot.apps);
   setLogs(snapshot.logs);
   setMaster(snapshot.masterEnabled);
   if(includePermissions){permissionStateRevision.current+=1;setPermissions(snapshot.permissions)}
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
     const revision=permissionStateRevision.current;
     const snapshot=await ClearScreenNative.getPermissionSnapshot();
     if(shouldApply()){
       const preserveHealth=revision!==permissionStateRevision.current;
       permissionStateRevision.current+=1;
       setPermissions(value=>preserveHealth?{...snapshot.permissions,...pickAccessibilityPermissions(value)}:snapshot.permissions);
     }
     return snapshot;
   }catch(error){console.warn('Permission state refresh failed',error);return undefined}
 },[]);
 const openSystemSettings=useCallback(async(destination:Exclude<SettingsHandoff,null>,open:()=>Promise<void>)=>{
   if(settingsOpening.current)return;
   settingsOpening.current=true;
   permissionRefreshVersion.current+=1;
   settingsHandoff.current=destination;
   setPermissionVerification('idle');
   setAccessibilityChecking(destination==='accessibility');
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
 const openPermission=useCallback(async(key:'skip'|'startup'|'bg'|'battery')=>{
   if(key==='skip')openAccessibilitySettings();
   else if(key==='startup')await openSystemSettings('startup',ClearScreenNative.openStartupSettings);
   else if(key==='bg')await openSystemSettings('background',ClearScreenNative.openBackgroundSettings);
   else await openSystemSettings('battery',ClearScreenNative.openBatterySettings);
 },[openAccessibilitySettings,openSystemSettings]);
 const requestAccessibilityEnable=useCallback(()=>{
   const reconnect=permissions.accessibilitySettingEnabled&&(!permissions.accessibilityServiceBound||permissions.accessibilityHealth==='unavailable');
   showAlert(reconnect?'重新开启无障碍服务':'开启无障碍服务',reconnect
     ? '系统保留了开启状态，但净屏服务未连接或未通过可用性检查。请在系统无障碍设置中将「净屏」关闭一次后重新开启；再检查自启动、后台运行和电池限制。返回后会自动核验。'
     : '请在系统无障碍设置中找到「净屏」，开启服务并允许系统确认提示。返回净屏后会自动检查状态。',[
     {text:'取消',style:'cancel'},
     {text:'去开启',onPress:()=>{void openPermission('skip')}},
   ],{tone:'warning'});
 },[openPermission,showAlert,permissions.accessibilitySettingEnabled,permissions.accessibilityServiceBound,permissions.accessibilityHealth]);
 useEffect(()=>{
   let alive=true;
   let previousState=AppState.currentState;
   void refresh(()=>alive);
   const subscription=AppState.addEventListener('change',state=>{
     if(state===previousState)return;
     previousState=state;
     const version=++permissionRefreshVersion.current;
     if(state!=='active')return;
     const current=()=>alive&&version===permissionRefreshVersion.current&&AppState.currentState==='active';
     const handoff=settingsHandoff.current;
     settingsHandoff.current=null;
         const accessibilityReturn=handoff==='accessibility';
     setPermissionVerification('checking');
     if(accessibilityReturn)setAccessibilityChecking(true);
     void (async()=>{
       try{
         // Read permissions first, on their independent native worker. Hundreds
         // of app icons must not keep the settings page in "checking" state.
         let latest=await refreshPermissionSnapshot(current);
         if(!current())return;
         if(!latest){setPermissionVerification('failed');return}
         setPermissionVerification('idle');
         const needsBinding=latest.permissions.accessibilitySettingEnabled&&!latest.permissions.skipRunning;
         const delays=accessibilityReturn?(needsBinding?[250,700,1500]:[]):handoff?[350,850]:[];
         for(const delay of delays){
           await new Promise<void>(resolve=>setTimeout(resolve,delay));
           if(!current())return;
           latest=await refreshPermissionSnapshot(current);
           if(!current())return;
           if(!latest){setPermissionVerification('failed');return}
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
   if(Platform.OS!=='android')return;
   let alive=true;
   let generation=0;
   let inFlight=false;
   let focused=true;
   let timer:ReturnType<typeof setTimeout>|undefined;
   const current=(version:number)=>alive&&version===generation&&focused&&AppState.currentState==='active';
   const check=async(version:number)=>{
     if(!current(version))return;
     if(inFlight){timer=setTimeout(()=>{void check(version)},500);return}
     inFlight=true;
     try{
       const snapshot=await ClearScreenNative.getAccessibilityHealth();
       if(!current(version))return;
       permissionStateRevision.current+=1;
       setPermissions(value=>({...value,...snapshot.permissions}));
    }catch{
      // A transient binder/read failure is not proof that the accessibility
      // service died. Keep the last confirmed switch state and only mark the
      // health probe unknown; the next 3s poll corrects a real disconnect.
      if(current(version))setPermissions(value=>({...value,accessibilityHealth:'unknown'}));
    }finally{
       inFlight=false;
       if(current(version)){
         setAccessibilityChecking(false);
         timer=setTimeout(()=>{void check(version)},3000);
       }
     }
   };
  const restart=()=>{
    generation+=1;
    if(timer)clearTimeout(timer);
    // Keep the last confirmed switch state while re-checking; otherwise a
    // short read delay on every foreground return looks like the service died.
    setPermissions(value=>({...value,accessibilityHealth:'unknown'}));
     if(focused&&AppState.currentState==='active'){
       setAccessibilityChecking(true);
       void check(generation);
     }
   };
   restart();
   const subscription=AppState.addEventListener('change',restart);
   const focusSubscription=AppState.addEventListener('focus',()=>{focused=true;restart()});
   const blurSubscription=AppState.addEventListener('blur',()=>{focused=false;restart()});
   return()=>{alive=false;generation+=1;if(timer)clearTimeout(timer);subscription.remove();focusSubscription.remove();blurSubscription.remove()};
 },[page]);
 const go=(p:Page)=>{setHistory(h=>[...h,page]);setPage(p)};
 const back=()=>{setHistory(h=>{const copy=[...h];const p=copy.pop()||'home';setPage(p);return copy})};
 const tab=(t:MainTab)=>{setHistory([]);setPage(t)};
 useEffect(()=>{
   const settingsDetail=page==='privacyData'||page==='helpFeedback'||page==='about';
   const settingsChild=(page==='servicePermissions'||page==='systemRecommend'||page==='appNotifications'||page==='marketingChannels')&&history.includes('settings');
   if(!settingsDetail&&!settingsChild)return;
   const subscription=BackHandler.addEventListener('hardwareBackPress',()=>{
     setHistory(history.slice(0,-1));
     setPage(history[history.length-1]||'settings');
     return true;
   });
   return()=>subscription.remove();
 },[page,history]);
 const goRecords=(query:RecordsQuery={})=>{setRecordsQuery(query);go('records')};
 const openHelpDestination=(destination:HelpDestination)=>{
   if(destination==='home'){tab('home');return}
   if(destination==='records'){goRecords();return}
   go(destination);
 };
 useEffect(()=>{
   if(page!=='learnedRecords'&&page!=='appManagement')return;
   const subscription=BackHandler.addEventListener('hardwareBackPress',()=>{
     setHistory(history.slice(0,-1));setPage(history[history.length-1]||'home');return true;
   });
   return()=>subscription.remove();
 },[page,history]);
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
 useEffect(()=>{
   if(Platform.OS!=='android'||(page!=='home'&&page!=='learnedRecords'))return;
   let alive=true;
   let reading=false;
   const update=()=>{
     if(AppState.currentState==='active'&&!learningBusy.current&&!reading){
       reading=true;
       const revision=learningRevision.current;
       void ClearScreenNative.getAdLearningState().then(value=>{if(alive&&!learningBusy.current&&revision===learningRevision.current)setLearning(value)}).catch(()=>{}).finally(()=>{reading=false});
     }
   };
   update();const timer=setInterval(update,1000);
   return()=>{alive=false;clearInterval(timer)};
 },[page]);
 const handleLearn=async()=>{
   if(learningBusy.current)return;
   if(learning.active){learningBusy.current=true;learningRevision.current+=1;try{setLearning(await ClearScreenNative.stopAdLearning())}catch(error){showAlert('操作失败',error instanceof Error?error.message:String(error),undefined,{tone:'error'})}finally{learningBusy.current=false}return}
   if(!permissions.skip||!permissions.skipRunning){requestAccessibilityEnable();return}
   if(!master){showAlert('请先开启净屏');return}
   learningBusy.current=true;learningRevision.current+=1;
   try{setLearning(await ClearScreenNative.startAdLearning())}
   catch(error){showAlert('操作失败',error instanceof Error?error.message:String(error),undefined,{tone:'error'})}
   finally{learningBusy.current=false}
 };
 const deleteLearned=(record:LearnedCloseRecord)=>{
   const name=apps.find(app=>app.id===record.appId||app.packageName===record.appId)?.name||record.appId||'此应用';
   showAlert('删除自定义关闭记录？',`删除后将停止使用这条规则自动关闭${name}的对应窗口。`,[
     {text:'取消',style:'cancel'},
     {text:'删除',style:'destructive',onPress:async()=>{
       if(learningBusy.current)return;
       learningBusy.current=true;learningRevision.current+=1;
       try{setLearning(await ClearScreenNative.deleteAdLearningRule(record.id))}
       catch(error){showAlert('删除失败',error instanceof Error?error.message:String(error),undefined,{tone:'error'})}
       finally{learningBusy.current=false}
     }},
   ]);
 };
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
   if(!permissions.skip||!permissions.skipRunning){requestAccessibilityEnable();return}
   if(enabledAppCount===0){go('appManagement');return}
   if(!master){void toggleMaster();return}
   void toggleMaster();
 };
 const goApp=(id:string)=>{setSelectedId(id);go('appDetail')};
 const stopApp=(id:string)=>updateApps(apps.map(app=>app.id===id?{...app,skip:false,whitelist:true}:app));
 const isMainTab=page==='home'||page==='apps'||page==='settings';
 let overlay:React.ReactNode=null;
 if(page==='appDetail'&&selected)overlay=<AppDetailScreen app={selected} logs={logs} onBack={back} onChange={a=>updateApps(apps.map(x=>x.id===a.id?a:x))} onRecords={()=>goRecords({appId:selected.id})} onOpenRecord={openRecord}/>;
 else if(page==='recordDetail'&&selectedLog){
   const logApp=apps.find(app=>app.id===selectedLog.appId);
   overlay=<RecordDetailScreen log={selectedLog} app={logApp} onBack={back} onOpenApp={()=>logApp&&goApp(logApp.id)} onStopApp={()=>logApp&&stopApp(logApp.id)}/>;
 }
 else if(page==='whitelist')overlay=<WhitelistScreen apps={apps} onChangeApps={updateApps} onBack={back}/>;
 else if(page==='learnedRecords')overlay=<LearnedCloseRecords records={learning.records} apps={apps} onBack={back} onDelete={deleteLearned}/>;
 else if(page==='servicePermissions')overlay=<ServicePermissionsScreen permissions={permissions} checking={permissionVerification==='checking'} verificationFailed={permissionVerification==='failed'} accessibilityChecking={accessibilityChecking} onBack={back} onRequestAccessibilityEnable={requestAccessibilityEnable} onStartup={()=>openPermission('startup')} onBackground={()=>openPermission('bg')} onBatteryOptimization={()=>openPermission('battery')}/>;
 else if(page==='appManagement')overlay=<AppsScreen apps={apps} onChangeApps={updateApps} onOpen={goApp} onBack={back}/>;
 else if(page==='systemRecommend')overlay=<SystemRecommendScreen manufacturer={permissions.deviceManufacturer} onBack={back} onOpenSystemPrivacy={()=>{void ClearScreenNative.openSystemAdPrivacySettings().catch(()=>showAlert('无法打开系统设置','请在手机系统设置中搜索“广告”或“个性化推荐”。',undefined,{tone:'error'}))}} onOpenServicePermissions={()=>go('servicePermissions')}/>;
 else if(page==='appNotifications')overlay=<AppNotificationsScreen apps={apps} onBack={back} onOpenAccess={()=>go('servicePermissions')}/>;
 else if(page==='marketingChannels')overlay=<MarketingChannelsScreen apps={apps} onBack={back} onOpenServicePermissions={()=>go('servicePermissions')}/>;
 else if(page==='privacyData'||page==='helpFeedback'||page==='about')overlay=<SettingsDetailScreen key={page} section={page} logRetentionDays={settings.logRetentionDays} versionName={versionName} onBack={back} onClear={handleClearLogs} onRetentionChange={days=>handleRetentionChange(days).catch(error=>showAlert('保存失败',String(error),undefined,{tone:'error'}))} onHelpNavigate={openHelpDestination} helpSearchInput={helpSearchInput} helpSearchTerm={helpSearchTerm} onHelpSearchInputChange={setHelpSearchInput} onHelpSearchSubmit={setHelpSearchTerm}/>;
 const homeScreen=Platform.OS==='ios'
   ? <IOSSafariScreen/>
   : <HomeScreen master={master} onAction={handleHomeAction} checking={accessibilityChecking} onTab={tab} onEnableAccessibility={requestAccessibilityEnable} onRecords={()=>goRecords()} onOpenRecord={openRecord} onOpenApp={goApp} apps={apps} logs={logs} permissions={permissions} enabledAppCount={enabledAppCount} onLearn={Platform.OS==='android'?()=>{void handleLearn().catch(error=>showAlert('操作失败',String(error)))}:undefined} learning={learning.active}/>;
 const appsScreen=<AdsScreen onTab={tab} onOpen={page=>go(page)} onOpenAppManagement={()=>go('appManagement')}/>;
 const settingsScreen=<SettingsScreen onTab={tab} onServicePermissions={()=>go('servicePermissions')} onOpenSection={go} onRestoreDefaults={Platform.OS==='android'?handleRestoreDefaults:undefined}/>;
 const recordsScreen=<RecordsScreen apps={apps} logs={logs} onBack={back} onOpenRecord={openRecord} filterAppId={recordsQuery.appId} todayOnly={recordsQuery.todayOnly}/>;
 return <ScreenInsetsContext.Provider value={sharedInsets}><StatusBar hidden={false} style="dark"/><View style={s.viewport}>
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

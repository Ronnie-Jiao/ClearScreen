import React, { useCallback, useEffect, useRef, useState } from 'react';
import { ActivityIndicator, AppState, Platform, Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { Background, Card, SwitchIndicator, TopBar } from '../components/UI';
import { useAppAlert } from '../components/AppDialog';
import { ClearScreenNative, type AdbAccessStatus, type AdbActionResult, type BackendPermissions } from '../native/ClearScreenNative';
import { C, COMPACT_UI } from '../theme';

type PermissionState=Pick<BackendPermissions,'skipRunning'|'accessibilityHealth'|'accessibilitySettingEnabled'|'isVivoFamily'|'vendorStartupSettingsAvailable'|'batteryOptimizationIgnored'|'backgroundRestricted'|'startupAllowed'|'backgroundAllowed'>;
const AUTO_PAIR_STAGES=new Set(['waiting-code','pairing','waiting-port','connecting']);

export default function ServicePermissionsScreen({permissions,checking=false,verificationFailed=false,accessibilityChecking=false,onBack,onOpenAccessibility,onStartup,onBackground,onBatteryOptimization}:{
  permissions:PermissionState;
  checking?:boolean;
  verificationFailed?:boolean;
  accessibilityChecking?:boolean;
  onBack:()=>void;
  onOpenAccessibility:()=>void;
  onStartup:()=>void;
  onBackground:()=>void;
  onBatteryOptimization:()=>void;
}){
  const showAlert=useAppAlert();
  const android=Platform.OS==='android';
  const [adbStatus,setAdbStatus]=useState<AdbAccessStatus|null>(null);
  const [adbBusy,setAdbBusy]=useState('');
  const [accessibilityReady,setAccessibilityReady]=useState<boolean|null>(null);
  const verified=!checking&&!verificationFailed;
  const skipEnabled=permissions.accessibilitySettingEnabled&&permissions.accessibilityHealth==='ready'&&permissions.skipRunning;
  const skipPending=!skipEnabled&&accessibilityChecking;
  const skipUnknown=!skipEnabled&&!skipPending&&permissions.accessibilityHealth==='unknown';
  const skipNeedsRepair=!skipEnabled&&!skipPending&&permissions.accessibilitySettingEnabled&&permissions.accessibilityHealth==='unavailable';
  const startupEnabled=verified&&permissions.startupAllowed===true;
  const startupUnknown=!verified||permissions.startupAllowed===null;
  // The vendor exposes no readable switch on some ROMs. Keep that honest
  // instead of painting an "off" switch the user cannot trust.
  const startupUnreadable=verified&&permissions.startupAllowed===null;
  const backgroundEnabled=verified&&permissions.backgroundRestricted!==true&&(permissions.backgroundAllowed===true||(permissions.backgroundAllowed===null&&permissions.backgroundRestricted===false));
  const batteryEnabled=verified&&permissions.batteryOptimizationIgnored===true;

  const refreshAdbStatus=useCallback(async()=>{
    if(!android)return null;
    try{
      const next=await ClearScreenNative.getAdbAccessStatus();
      setAdbStatus(next);
      return next;
    }catch{return null}
  },[android]);
  const refreshAccessibility=useCallback(async()=>{
    try{
      const health=await ClearScreenNative.getAccessibilityHealth();
      setAccessibilityReady(Boolean(health.permissions.skip));
    }catch{setAccessibilityReady(null)}
  },[]);

  useEffect(()=>{
    if(!android)return;
    void refreshAdbStatus();
    void refreshAccessibility();
    let refreshTimer:ReturnType<typeof setTimeout>|undefined;
    const listener=AppState.addEventListener('change',state=>{
      if(state==='active'){
        refreshTimer=setTimeout(()=>{void refreshAdbStatus();void refreshAccessibility()},300);
      }
    });
    return()=>{listener.remove();if(refreshTimer)clearTimeout(refreshTimer)};
  },[android,refreshAdbStatus,refreshAccessibility]);

  const autoStage=adbStatus?.autoPairStage??'';
  const autoRunning=AUTO_PAIR_STAGES.has(autoStage);
  const adbConnecting=adbStatus?.state==='connecting';
  // A failed attempt must show its reason and stay tappable; a background
  // restore must never count as pending, otherwise the row spins forever.
  const adbFailed=autoStage==='failed';
  const adbFailText=adbFailed?(adbStatus?.autoPairMessage||adbStatus?.message||''):'';
  const adbPending=!adbStatus||!!adbBusy||!!adbConnecting||autoRunning;
  useEffect(()=>{
    if(!android)return;
    const timer=setInterval(()=>{void refreshAdbStatus()},autoRunning||adbConnecting?1200:5000);
    return()=>clearInterval(timer);
  },[android,autoRunning,adbConnecting,refreshAdbStatus]);
  const fail=(title:string,error:unknown)=>showAlert(title,error instanceof Error?error.message:String(error),undefined,{tone:'error'});
  const openAccessibility=()=>{
    void ClearScreenNative.openAccessibilitySettings().catch(error=>fail('无法打开系统设置',error));
  };
  const startAutoPair=async()=>{
    if(accessibilityReady===false){
      showAlert('先开启自动跳过服务','净屏要靠它在配对弹窗出现时读出配对码；不开启的话，自动配对会一直等不到配对码。',[
        {text:'取消',style:'cancel'},
        {text:'去开启',onPress:openAccessibility},
      ],{tone:'warning'});
      return false;
    }
    setAdbBusy('auto-pair');
    try{
      setAdbStatus(await ClearScreenNative.startAdbAutoPair());
      return true;
    }catch(error){fail('无法自动配对',error);return false}
    finally{setAdbBusy('')}
  };
  const stopAutoPair=async()=>{
    setAdbBusy('stop-pair');
    try{
      setAdbStatus(await ClearScreenNative.stopAdbAutoPair());
    }catch(error){fail('停止失败',error)}
    finally{setAdbBusy('')}
  };
  const runConnection=async(label:string,action:()=>Promise<AdbActionResult>)=>{
    setAdbBusy(label);
    try{
      const result=await action();
      await refreshAdbStatus();
      if(!result.ok){
        showAlert('未连接',result.message,[
          {text:'关闭',style:'cancel'},
          {text:'重试',onPress:()=>{void runConnection(label,action)}},
        ],{tone:'warning'});
      }
    }catch(error){
      fail('操作失败',error);
    }finally{setAdbBusy('')}
  };
  const disconnect=async()=>{
    setAdbBusy('disconnect');
    try{
      await ClearScreenNative.disconnectAdb();
      await refreshAdbStatus();
    }catch(error){fail('断开失败',error)}
    finally{setAdbBusy('')}
  };
  const connectAutomatically=async(status:AdbAccessStatus)=>{
    if(!status.supported){
      showAlert('无法连接','这台手机不支持无线调试。',undefined,{tone:'warning'});
      return;
    }
    if(status.paired){
      await runConnection('auto',()=>ClearScreenNative.autoConnectAdb());
      return;
    }
    const started=await startAutoPair();
    if(!started)return;
    try{await ClearScreenNative.openDeveloperOptions()}
    catch(error){
      try{setAdbStatus(await ClearScreenNative.stopAdbAutoPair())}catch{ /* preserve the original error */ }
      fail('无法打开系统设置',error);
    }
  };
  const toggleAdb=async()=>{
    if(adbBusy)return;
    const status=adbStatus??await refreshAdbStatus();
    if(!status){showAlert('无法读取连接状态','请稍后再试。',undefined,{tone:'error'});return}
    if(status.connected){await disconnect();return}
    if(AUTO_PAIR_STAGES.has(status.autoPairStage??'')){await stopAutoPair();return}
    // A failed attempt starts over from the pairing dialog instead of retrying
    // a connection whose pairing may never have completed.
    await connectAutomatically(status.autoPairStage==='failed'?{...status,paired:false}:status);
  };
  const retryRef=useRef<()=>void>(()=>{});
  retryRef.current=()=>{void toggleAdb()};
  useEffect(()=>{
    if(!adbFailText)return;
    showAlert('连接失败',adbFailText,[
      {text:'关闭',style:'cancel'},
      {text:'重试连接',onPress:()=>retryRef.current()},
    ],{tone:'error'});
  },[adbFailText]);
  useEffect(()=>{
    if(!android||!adbPending)return;
    // Last-resort exit: a stuck native call must not leave the switch spinning.
    const timer=setTimeout(()=>{
      setAdbBusy('');
      void refreshAdbStatus();
      showAlert('连接没有完成','等了很久仍没有结果，请确认无线调试已开启，再点按开关重试。',[
        {text:'关闭',style:'cancel'},
        {text:'重试连接',onPress:()=>retryRef.current()},
      ],{tone:'warning'});
    },150000);
    return()=>clearTimeout(timer);
  },[android,adbPending]);
  const openStartupGuide=()=>{
    if(!permissions.isVivoFamily||!permissions.vendorStartupSettingsAvailable){onStartup();return}
    showAlert('开启开机自启动','在 vivo「自启动管理」中找到「净屏」并开启“自启动”。',[
      {text:'取消',style:'cancel'},
      {text:'打开自启动',onPress:onStartup},
    ],{cancelable:true});
  };
  const switchRow=(title:string,enabled:boolean,onPress:()=>void,last=false,disabled=false,stateText=enabled?'已开启':'已关闭',showState=false,pending=false)=>(
    <Pressable accessibilityRole="button" accessibilityLabel={`${title}，${stateText}`} accessibilityState={{disabled,busy:pending}} disabled={disabled} onPress={onPress}
      style={[s.row,last&&{borderBottomWidth:0}]}>
      <Text style={s.rowTitle}>{title}</Text>
      {showState?<Text style={s.pendingState}>{stateText}</Text>:null}
      {pending?<ActivityIndicator size="small" color={C.blue} style={s.pending}/>:<SwitchIndicator compact value={enabled}/>}
    </Pressable>
  );

  const adbConnected=adbStatus?.connected===true;
  const adbStateText=!adbStatus?'正在读取':adbBusy==='disconnect'?'正在断开':autoRunning?'正在配对':adbFailed?'连接失败':adbPending?'正在连接':adbConnected?'已连接':'未连接';
  return <Background><ScrollView contentContainerStyle={s.page} showsVerticalScrollIndicator={false}>
    <TopBar compact title="服务与权限" onBack={onBack}/>
    <Card compact style={s.card}>
      {switchRow('自动跳过服务',skipEnabled,onOpenAccessibility,false,false,skipPending?'正在检查':skipEnabled?'已开启':skipUnknown?'待确认':skipNeedsRepair?'需修复':'未开启',skipUnknown||skipNeedsRepair,skipPending)}
      {startupUnreadable
        ? <Pressable accessibilityRole="button" accessibilityLabel="开机自启动，无法读取，去设置" onPress={openStartupGuide} style={s.row}>
            <Text style={s.rowTitle}>开机自启动</Text>
            <Text style={s.pendingState}>无法读取</Text>
            <Text style={s.retryText}>去设置</Text>
          </Pressable>
        : switchRow('开机自启动',startupEnabled,openStartupGuide,false,false,startupUnknown?'待确认':startupEnabled?'已开启':'已关闭',startupUnknown,checking)}
      {switchRow('后台运行',backgroundEnabled,onBackground)}
      {switchRow('电池优化',batteryEnabled,onBatteryOptimization,!android)}
      {android?<>
        {switchRow('ADB Shell 操作能力',adbConnected,()=>{void toggleAdb()},true,!adbStatus||!!adbBusy||(!!adbConnecting&&!autoRunning),adbStateText,false,adbPending)}
        {adbFailed?<View style={s.failBox}>
          <Text style={s.failText}>{adbFailText||'连接没有完成'}</Text>
          <Pressable accessibilityRole="button" accessibilityLabel="重试连接" onPress={()=>{void toggleAdb()}} hitSlop={8}>
            <Text style={s.retryText}>重试连接</Text>
          </Pressable>
        </View>:null}
      </>:null}
    </Card>
  </ScrollView></Background>;
}

const s=StyleSheet.create({
  page:{paddingHorizontal:18,paddingBottom:36},
  card:{paddingHorizontal:COMPACT_UI.cardPadding},
  row:{minHeight:COMPACT_UI.rowHeight,flexDirection:'row',alignItems:'center',gap:8,borderBottomWidth:1,borderBottomColor:C.line},
  rowTitle:{flex:1,fontSize:COMPACT_UI.body,fontWeight:'800',color:C.navy},
  pending:{width:42,height:26},
  pendingState:{fontSize:COMPACT_UI.caption,color:C.text2},
  failBox:{flexDirection:'row',alignItems:'center',gap:10,paddingVertical:10,borderBottomWidth:0},
  failText:{flex:1,fontSize:COMPACT_UI.caption,color:C.text2},
  retryText:{fontSize:COMPACT_UI.caption,fontWeight:'800',color:C.blue},
});

import React, { useCallback, useEffect, useRef, useState } from 'react';
import { ActivityIndicator, AppState, Image, ImageSourcePropType, Platform, Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { Background, Card, SwitchIndicator, TopBar } from '../components/UI';
import { useAppAlert } from '../components/AppDialog';
import { ClearScreenNative, type AdbAccessStatus, type AdbActionResult, type BackendPermissions } from '../native/ClearScreenNative';
import { C, COMPACT_UI } from '../theme';
import { PERM_AUTO_SKIP, PERM_STARTUP, PERM_BACKGROUND, PERM_BATTERY, PERM_SYSTEM_SETTINGS } from '../assets';

type PermissionState=Pick<BackendPermissions,'skipRunning'|'accessibilityHealth'|'accessibilitySettingEnabled'|'isVivoFamily'|'vendorStartupSettingsAvailable'|'batteryOptimizationIgnored'|'backgroundRestricted'|'startupAllowed'|'backgroundAllowed'>;
const AUTO_PAIR_STAGES=new Set(['waiting-code','pairing','waiting-port','connecting']);
const ROW_ICONS:Record<string,ImageSourcePropType>={
  '自动跳过服务':PERM_AUTO_SKIP,
  '开机自启动':PERM_STARTUP,
  '后台运行':PERM_BACKGROUND,
  '电池优化':PERM_BATTERY,
  '系统设置权限':PERM_SYSTEM_SETTINGS,
};

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
  const [adbReadFailed,setAdbReadFailed]=useState(false);
  const [adbBusy,setAdbBusy]=useState('');
  const [accessibilityReady,setAccessibilityReady]=useState<boolean|null>(null);
  const verified=!checking&&!verificationFailed;
  const skipEnabled=permissions.accessibilitySettingEnabled&&permissions.accessibilityHealth==='ready'&&permissions.skipRunning;
  const skipPending=!skipEnabled&&accessibilityChecking;
  const skipUnknown=permissions.accessibilitySettingEnabled&&permissions.accessibilityHealth==='unknown';
  const skipNeedsRepair=!skipEnabled&&!skipPending&&permissions.accessibilitySettingEnabled&&permissions.accessibilityHealth==='unavailable';
  const startupEnabled=verified&&permissions.startupAllowed===true;
  const startupUnreadable=verified&&permissions.startupAllowed===null;
  const startupReadable=verified&&permissions.startupAllowed!==null;
  const backgroundReadable=verified&&(permissions.backgroundRestricted!==null||permissions.backgroundAllowed!==null);
  const backgroundEnabled=verified&&permissions.backgroundRestricted!==true&&(permissions.backgroundAllowed===true||(permissions.backgroundAllowed===null&&permissions.backgroundRestricted===false));
  const batteryReadable=verified&&permissions.batteryOptimizationIgnored!==null;
  const batteryEnabled=verified&&permissions.batteryOptimizationIgnored===true;
  const verificationStatus=checking?'正在检查':'待确认';
  const skipStatusText=!verified?verificationStatus:!permissions.accessibilitySettingEnabled?'未开启':skipEnabled?'已开启':skipNeedsRepair?'需修复':'待确认';
  const startupStatusText=!verified?verificationStatus:startupUnreadable?'无法读取':startupEnabled?'已开启':'未开启';
  const backgroundStatusText=!verified?verificationStatus:permissions.backgroundRestricted===true?'受限制':permissions.backgroundAllowed===true?'已允许':permissions.backgroundAllowed===false?'未允许':permissions.backgroundRestricted===false?'未受限':'待确认';
  const batteryStatusText=!verified?verificationStatus:permissions.batteryOptimizationIgnored===true?'已忽略':permissions.batteryOptimizationIgnored===false?'未忽略':'待确认';

  const refreshAdbStatus=useCallback(async()=>{
    if(!android)return null;
    setAdbReadFailed(false);
    try{
      const next=await ClearScreenNative.getAdbAccessStatus();
      setAdbStatus(next);
      setAdbReadFailed(false);
      return next;
    }catch{setAdbReadFailed(true);return null}
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
  const adbPending=(!adbStatus&&!adbReadFailed)||!!adbBusy||!!adbConnecting||autoRunning;
  const hasAdbStatus=adbStatus!==null;
  useEffect(()=>{
    if(!android||!hasAdbStatus)return;
    const timer=setInterval(()=>{void refreshAdbStatus()},autoRunning||adbConnecting?1200:5000);
    return()=>clearInterval(timer);
  },[android,hasAdbStatus,autoRunning,adbConnecting,refreshAdbStatus]);
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
  const startPairingAndOpenSettings=async()=>{
    const started=await startAutoPair();
    if(!started)return;
    try{await ClearScreenNative.openDeveloperOptions()}
    catch(error){
      try{setAdbStatus(await ClearScreenNative.stopAdbAutoPair())}catch{ /* preserve the original error */ }
      fail('无法打开系统设置',error);
    }
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
    showAlert('请开启无线调试','请在开发者选项中开启「无线调试」，再点「使用配对码配对设备」。配对窗口保持打开，净屏会自动完成连接。',[
      {text:'取消',style:'cancel'},
      {text:'去设置',onPress:()=>{void startPairingAndOpenSettings()}},
    ],{tone:'warning'});
  };
  const toggleAdb=async()=>{
    if(adbBusy)return;
    let status=adbStatus;
    if(!status){
      setAdbBusy('read-status');
      try{status=await refreshAdbStatus()}
      finally{setAdbBusy('')}
    }
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
  const showSettingsGuide=(title:string,message:string,onOpen:()=>void)=>showAlert(title,message,[
      {text:'取消',style:'cancel'},
      {text:'去设置',onPress:onOpen},
    ],{tone:'warning'});
  const openAccessibilityGuide=()=>showSettingsGuide('开启自动跳过服务','在无障碍服务列表中找到「净屏」，开启服务并允许系统确认提示。',onOpenAccessibility);
  const openStartupGuide=()=>showSettingsGuide('开启开机自启动',permissions.isVivoFamily&&permissions.vendorStartupSettingsAvailable
    ? '在 vivo「自启动管理」中找到「净屏」，开启「自启动」。'
    : '在系统设置中找到「净屏」，允许应用开机自启动。',onStartup);
  const openBackgroundGuide=()=>showSettingsGuide('允许后台运行','在打开的系统页面中允许「净屏」后台运行，或移除对净屏的后台限制。',onBackground);
  const openBatteryGuide=()=>showSettingsGuide('设置电池优化','在电池优化列表中找到「净屏」，设置为「不优化」或「不受限制」。',onBatteryOptimization);
  const rowLead=(title:string)=><>
    <Image source={ROW_ICONS[title]} resizeMode="contain" style={s.rowIcon}/>
    <View pointerEvents="none" style={s.rowDivider}/>
  </>;
  const switchRow=(title:string,enabled:boolean,onPress:()=>void,disabled=false,stateText=enabled?'已开启':'已关闭',showState=false,pending=false,footer?:React.ReactNode)=>(
    <Card compact style={s.rowCard}>
      <Pressable accessibilityRole="button" accessibilityLabel={`${title}，${stateText}`} accessibilityState={{disabled,busy:pending}} disabled={disabled} onPress={onPress}
        style={s.row}>
        {rowLead(title)}
        <Text style={s.rowTitle}>{title}</Text>
        {showState?<Text style={s.pendingState}>{stateText}</Text>:null}
        {pending?<ActivityIndicator size="small" color={C.blue} style={s.pending}/>:<SwitchIndicator value={enabled}/>}
      </Pressable>
      {footer}
    </Card>
  );
  const settingsRow=(title:string,stateText:string,onPress:()=>void,pending=false)=>(
    <Card compact style={s.rowCard}>
      <View style={s.row}>
        {rowLead(title)}
        <Text style={s.rowTitle}>{title}</Text>
        {pending?<ActivityIndicator size="small" color={C.blue} style={s.pending}/>:<Text style={s.pendingState}>{stateText}</Text>}
        <Pressable accessibilityRole="button" accessibilityLabel={`${title}设置`} onPress={onPress} hitSlop={8} style={s.settingsButton}>
          <Text style={s.retryText}>设置</Text>
        </Pressable>
      </View>
    </Card>
  );

  const adbConnected=adbStatus?.connected===true;
  const adbStateText=!adbStatus?(adbReadFailed?'读取失败':'正在读取'):adbBusy==='disconnect'?'正在断开':autoRunning?'正在配对':adbFailed?'连接失败':adbPending?'正在连接':adbConnected?'已连接':'未连接';
  return <Background><ScrollView contentContainerStyle={s.page} showsVerticalScrollIndicator={false}>
    <TopBar compact title="服务与权限" onBack={onBack}/>
    {verified
      ? switchRow('自动跳过服务',permissions.accessibilitySettingEnabled,openAccessibilityGuide,false,skipStatusText,skipUnknown||skipNeedsRepair,skipPending)
      : settingsRow('自动跳过服务',verificationStatus,openAccessibilityGuide,checking)}
    {startupReadable
      ? switchRow('开机自启动',startupEnabled,openStartupGuide,false,startupStatusText,false,false)
      : settingsRow('开机自启动',startupStatusText,openStartupGuide,checking)}
    {backgroundReadable
      ? switchRow('后台运行',backgroundEnabled,openBackgroundGuide,false,backgroundStatusText,false,false)
      : settingsRow('后台运行',backgroundStatusText,openBackgroundGuide,checking)}
    {batteryReadable
      ? switchRow('电池优化',batteryEnabled,openBatteryGuide,false,batteryStatusText,false,false)
      : settingsRow('电池优化',batteryStatusText,openBatteryGuide,checking)}
    {android?switchRow('系统设置权限',adbConnected,()=>{void toggleAdb()},
      (!adbStatus&&!adbReadFailed)||!!adbBusy||(!!adbConnecting&&!autoRunning),adbStateText,!adbStatus&&adbReadFailed,adbPending,
      adbFailed?<View style={s.failBox}>
        <Text style={s.failText}>{adbFailText||'连接没有完成'}</Text>
        <Pressable accessibilityRole="button" accessibilityLabel="重试连接" onPress={()=>{void toggleAdb()}} hitSlop={8}>
          <Text style={s.retryText}>重试连接</Text>
        </Pressable>
      </View>:null):null}
  </ScrollView></Background>;
}

const s=StyleSheet.create({
  page:{paddingHorizontal:18,paddingBottom:36},
  rowCard:{paddingHorizontal:COMPACT_UI.cardPadding,borderRadius:22,backgroundColor:'rgba(255,255,255,0.86)',borderColor:'rgba(255,255,255,0.95)',marginBottom:12},
  row:{minHeight:64,flexDirection:'row',alignItems:'center',gap:10},
  rowIcon:{width:38,height:38,flexShrink:0,borderRadius:11},
  rowDivider:{width:1,height:28,backgroundColor:'#C8DEF2',flexShrink:0},
  rowTitle:{flex:1,minWidth:0,fontSize:16,fontWeight:'900',color:C.navy,letterSpacing:-.2},
  pending:{width:50,height:30},
  settingsButton:{minWidth:56,minHeight:42,alignItems:'flex-end',justifyContent:'center'},
  pendingState:{fontSize:COMPACT_UI.caption,color:C.text2},
  failBox:{flexDirection:'row',alignItems:'center',gap:10,paddingVertical:10,borderTopWidth:1,borderTopColor:'#E3EFFA'},
  failText:{flex:1,fontSize:COMPACT_UI.caption,color:C.text2},
  retryText:{fontSize:COMPACT_UI.caption,fontWeight:'800',color:C.blue},
});

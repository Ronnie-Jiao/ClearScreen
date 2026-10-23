import React from 'react';
import { Pressable, ScrollView, StyleSheet, Text } from 'react-native';
import { Background, Card, Chevron, TopBar } from '../components/UI';
import { useAppAlert } from '../components/AppDialog';
import { C, COMPACT_UI } from '../theme';
import type { BackendPermissions } from '../native/ClearScreenNative';

type PermissionState=Pick<BackendPermissions,'skipRunning'|'accessibilitySettingEnabled'|'isVivoFamily'|'vendorStartupSettingsAvailable'|'batteryOptimizationIgnored'|'backgroundRestricted'|'startupAllowed'|'backgroundAllowed'>;

export default function ServicePermissionsScreen({permissions,checking=false,verificationFailed=false,onBack,onOpenAccessibility,onStartup,onBackground,onBatteryOptimization}:{
  permissions:PermissionState;
  checking?:boolean;
  verificationFailed?:boolean;
  onBack:()=>void;
  onOpenAccessibility:()=>void;
  onStartup:()=>void;
  onBackground:()=>void;
  onBatteryOptimization:()=>void;
}){
  const showAlert=useAppAlert();
  const skipStatus=checking?'核验中':verificationFailed?'核验失败':permissions.skipRunning?'已连接':permissions.accessibilitySettingEnabled?'需要修复':'未开启';
  const startupStatus=checking?'核验中':verificationFailed?'核验失败':permissions.startupAllowed===null?'需手动确认':permissions.startupAllowed?'已开启':'未开启';
  const backgroundStatus=checking?'核验中':verificationFailed?'核验失败':permissions.backgroundRestricted===true?'受限制':permissions.backgroundAllowed!==null?(permissions.backgroundAllowed?'已允许':'未允许'):permissions.backgroundRestricted===false?'未受限':'需手动确认';
  const batteryStatus=checking?'核验中':verificationFailed?'核验失败':permissions.batteryOptimizationIgnored===null?'读取失败':permissions.batteryOptimizationIgnored?'已忽略':'未忽略';
  const openStartupGuide=()=>{
    if(!permissions.isVivoFamily||!permissions.vendorStartupSettingsAvailable){onStartup();return}
    showAlert('开启开机自启动','在 vivo「自启动管理」中找到「净屏」并开启“自启动”。',[
      {text:'取消',style:'cancel'},
      {text:'打开自启动',onPress:onStartup},
    ],{cancelable:true});
  };
  const row=(title:string,value:string|undefined,onPress:()=>void,last=false)=>(
    <Pressable accessibilityRole="button" onPress={onPress} style={[s.row,last&&{borderBottomWidth:0}]}>
      <Text style={s.rowTitle}>{title}</Text>
      {value?<Text style={s.value}>{value}</Text>:null}
      <Chevron compact/>
    </Pressable>
  );
  return <Background><ScrollView contentContainerStyle={s.page} showsVerticalScrollIndicator={false}>
    <TopBar compact title="服务与权限" onBack={onBack}/>
    <Card compact style={s.card}>
      {row('自动跳过服务',skipStatus,onOpenAccessibility)}
      {row('开机自启动',startupStatus,openStartupGuide)}
      {row('后台运行',backgroundStatus,onBackground)}
      {row('电池优化',batteryStatus,onBatteryOptimization,true)}
    </Card>
  </ScrollView>
  </Background>;
}

const s=StyleSheet.create({
  page:{paddingHorizontal:18,paddingBottom:36},
  card:{paddingHorizontal:COMPACT_UI.cardPadding},
  row:{minHeight:COMPACT_UI.rowHeight,flexDirection:'row',alignItems:'center',gap:8,borderBottomWidth:1,borderBottomColor:C.line},
  rowTitle:{flex:1,fontSize:COMPACT_UI.body,fontWeight:'800',color:C.navy},
  value:{fontSize:COMPACT_UI.body,fontWeight:'800',color:C.text2},
});

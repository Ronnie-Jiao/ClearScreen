import React, { useState } from 'react';
import { Modal, Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { Background, BrandHeader, Card, Chevron } from '../components/UI';
import { BottomNav } from '../components/BottomNav';
import { C } from '../theme';

type MainTab='home'|'apps'|'settings';
type PermissionState={skip:boolean;skipRunning:boolean;bg:boolean;isVivoFamily?:boolean};
const RETENTION_OPTIONS:[number,string][]=[[0,'关闭记录'],[1,'1 天'],[7,'7 天'],[30,'30 天']];

export default function SettingsScreen({logRetentionDays,versionName,permissions,onTab,onPermissions,onBackground,onDiagnostics,onClear,onRetentionChange}:{
 logRetentionDays:number;
 versionName:string;
 permissions:PermissionState;
 onTab:(tab:MainTab)=>void;
 onPermissions:()=>void;
 onBackground:()=>void;
 onDiagnostics:()=>void;
 onClear:()=>void;
 onRetentionChange:(days:number)=>void;
}){
 const [retentionOpen,setRetentionOpen]=useState(false);
 const skipStatus=permissions.skipRunning?'已连接':permissions.skip?'需要修复':'未开启';
 const backgroundStatus=permissions.isVivoFamily?'待检查':permissions.bg?'已允许':'待检查';
 const row=(title:string,value?:string,onPress?:()=>void,last=false)=><Pressable accessibilityRole={onPress?'button':undefined} onPress={onPress} style={[s.row,last&&{borderBottomWidth:0}]}><Text style={s.rowTitle}>{title}</Text>{value?<Text style={s.value}>{value}</Text>:null}{onPress?<Chevron/>:null}</Pressable>;
 return <Background><View style={s.root}><ScrollView showsVerticalScrollIndicator={false} contentContainerStyle={s.scroll}>
  <BrandHeader/>
  <Text style={s.groupTitle}>服务与权限</Text>
  <Card style={s.group}>
   {row('自动跳过服务',skipStatus,onPermissions)}
   {row('后台运行',backgroundStatus,onBackground)}
   {row('故障诊断',undefined,onDiagnostics,true)}
  </Card>
  <Text style={s.groupTitle}>隐私与数据</Text>
  <Card style={s.group}>
   {row('记录保留周期',RETENTION_OPTIONS.find(([days])=>days===logRetentionDays)?.[1]||'7 天',()=>setRetentionOpen(true))}
   {row('清空记录',undefined,onClear,true)}
  </Card>
  <Text style={s.groupTitle}>关于</Text>
  <Card style={s.group}>{row('版本信息',versionName,undefined,true)}</Card>
 </ScrollView><BottomNav active="settings" onChange={onTab}/></View>
  <Modal transparent visible={retentionOpen} animationType="slide" onRequestClose={()=>setRetentionOpen(false)}>
   <View style={s.modalRoot}>
    <Pressable style={s.scrim} onPress={()=>setRetentionOpen(false)}/>
    <View style={s.sheet}>
     <Text style={s.sheetTitle}>记录保留周期</Text>
     {RETENTION_OPTIONS.map(([days,label],index)=><Pressable key={days} onPress={()=>{setRetentionOpen(false);onRetentionChange(days)}} style={[s.option,index===RETENTION_OPTIONS.length-1&&{borderBottomWidth:0}]}><Text style={s.optionLabel}>{label}</Text><Text style={s.check}>{days===logRetentionDays?'✓':''}</Text></Pressable>)}
    </View>
   </View>
  </Modal>
 </Background>;
}

const s=StyleSheet.create({
 root:{flex:1},scroll:{padding:18,paddingBottom:112},groupTitle:{fontSize:14,fontWeight:'800',color:C.text2,paddingHorizontal:8,paddingVertical:8},group:{paddingHorizontal:16,marginBottom:10},row:{minHeight:66,flexDirection:'row',alignItems:'center',gap:8,borderBottomWidth:1,borderBottomColor:C.line},rowTitle:{flex:1,fontSize:17,fontWeight:'900',color:C.navy},value:{fontSize:15,fontWeight:'800',color:C.text2},
 modalRoot:{flex:1,justifyContent:'flex-end'},scrim:{...StyleSheet.absoluteFillObject,backgroundColor:'rgba(5,18,40,.35)'},sheet:{backgroundColor:'#fff',borderTopLeftRadius:26,borderTopRightRadius:26,paddingHorizontal:20,paddingTop:18,paddingBottom:28},sheetTitle:{fontSize:19,fontWeight:'900',color:C.navy,paddingVertical:10},option:{minHeight:58,flexDirection:'row',alignItems:'center',borderBottomWidth:1,borderBottomColor:C.line},optionLabel:{flex:1,fontSize:17,fontWeight:'800',color:C.text},check:{fontSize:20,fontWeight:'900',color:C.blue},
});

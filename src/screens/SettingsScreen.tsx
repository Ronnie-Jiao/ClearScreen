import React from 'react';
import { Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { Background, BrandHeader, Card, Chevron } from '../components/UI';
import { useAppAlert } from '../components/AppDialog';
import { BottomNav } from '../components/BottomNav';
import { C } from '../theme';
import type { SettingsSection } from './SettingsDetailScreen';

type MainTab='home'|'apps'|'settings';

export default function SettingsScreen({onTab,onServicePermissions,onOpenSection,onPrivacyPolicy,onRestoreDefaults}:{
 onTab:(tab:MainTab)=>void;
 onServicePermissions:()=>void;
 onOpenSection:(section:SettingsSection)=>void;
 onPrivacyPolicy:()=>void;
 onRestoreDefaults?:()=>Promise<void>;
}){
 const showAlert=useAppAlert();
 const confirmRestoreDefaults=()=>{
  if(!onRestoreDefaults)return;
  showAlert('恢复默认设置？','总开关将关闭，自动处理选项和记录保留周期将恢复默认（7天）。应用排除规则和处理记录会保留。',[
   {text:'取消',style:'cancel'},
   {text:'恢复默认',style:'destructive',onPress:()=>{
    void onRestoreDefaults().then(()=>showAlert('设置已恢复',undefined,undefined,{tone:'success'})).catch(()=>showAlert('恢复失败','请稍后重试。',undefined,{tone:'error'}));
   }},
  ]);
 };
 const row=(title:string,onPress:()=>void,last=false)=><Pressable accessibilityRole="button" onPress={onPress} style={[s.row,last&&s.lastRow]}><Text style={s.rowTitle}>{title}</Text><Chevron/></Pressable>;

 return <Background><View style={s.root}>
  <ScrollView showsVerticalScrollIndicator={false} contentContainerStyle={s.scroll}>
   <BrandHeader smallText/>
   <Card style={s.group}>{row('服务与权限',onServicePermissions,true)}</Card>
   <Card style={s.group}>
    {row('隐私与数据',()=>onOpenSection('privacyData'))}
    {row('隐私政策',onPrivacyPolicy)}
    {onRestoreDefaults?row('恢复默认设置',confirmRestoreDefaults):null}
    {row('帮助与反馈',()=>onOpenSection('helpFeedback'))}
    {row('关于净屏',()=>onOpenSection('about'),true)}
   </Card>
  </ScrollView>
  <BottomNav active="settings" onChange={onTab}/>
 </View></Background>;
}

const s=StyleSheet.create({
 root:{flex:1},
 scroll:{paddingHorizontal:18,paddingBottom:112},
 group:{paddingHorizontal:16,marginBottom:10},
 row:{minHeight:66,paddingVertical:12,flexDirection:'row',alignItems:'center',gap:8,borderBottomWidth:1,borderBottomColor:C.line},
 lastRow:{borderBottomWidth:0},
 rowTitle:{flex:1,fontSize:15,fontWeight:'900',color:C.navy},
});

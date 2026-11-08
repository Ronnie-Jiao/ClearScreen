import React from 'react';
import { Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { Background, Badge, Card, PrimaryButton, SquareIcon, TopBar } from '../components/UI';
import { C, COMPACT_UI } from '../theme';

type PermissionState={skip:boolean;skipRunning:boolean;checking:boolean;restrictedSettingsLikely:boolean;accessibilityHealth?:'ready'|'unavailable'|'unknown'};
export default function PermissionsScreen({onBack,onContinue,onLater,permissions,onOpenAccessibility,onOpenAppInfo,onHelp}:{onBack:()=>void;onContinue:()=>void;onLater:()=>void;permissions:PermissionState;onOpenAccessibility:()=>void;onOpenAppInfo:()=>void;onHelp:()=>void}){
 const ready=!permissions.checking&&permissions.skip&&permissions.skipRunning&&permissions.accessibilityHealth==='ready';
 const status=permissions.checking?'正在检查':ready?'已准备好':!permissions.skip?'未开启':permissions.accessibilityHealth!=='unavailable'&&permissions.accessibilityHealth!=='ready'?'状态待确认':'需要修复';
 const statusGood=ready;
 const actionTitle=permissions.checking?null:ready?'选择应用':permissions.skip?'重新开启':'去系统开启';
 return <Background><ScrollView contentContainerStyle={s.page} showsVerticalScrollIndicator={false}>
  <TopBar compact title="自动跳过服务" onBack={onBack}/>
  <Card compact style={s.card}>
   <SquareIcon symbol="≫" tone="blue" size={COMPACT_UI.appIcon}/>
   <View style={s.copy}><Text style={s.title}>无障碍服务</Text><Badge compact green={statusGood}>{status}</Badge></View>
  </Card>
  <Text style={s.privacy}>用于识别广告控件，并按你的选择辅助操作系统广告设置；屏幕内容在本机处理。</Text>
  {actionTitle?<PrimaryButton compact title={actionTitle} onPress={ready?onContinue:onOpenAccessibility}/>:null}
  <View style={s.secondaryActions}>
   {!ready&&permissions.restrictedSettingsLikely?<Pressable onPress={onOpenAppInfo} hitSlop={10}><Text style={s.link}>打开应用信息</Text></Pressable>:null}
   {!ready?<Pressable onPress={onHelp} hitSlop={10}><Text style={s.link}>查看步骤</Text></Pressable>:null}
   <Pressable onPress={onLater} hitSlop={10}><Text style={s.link}>{ready?'先进入首页':'稍后再说'}</Text></Pressable>
  </View>
 </ScrollView></Background>;
}

const s=StyleSheet.create({page:{flexGrow:1,paddingHorizontal:18,paddingBottom:36},card:{minHeight:78,padding:COMPACT_UI.cardPadding,flexDirection:'row',alignItems:'center',gap:COMPACT_UI.gap,marginBottom:10},copy:{flex:1,gap:4},title:{fontSize:COMPACT_UI.heading,fontWeight:'900',color:C.navy},privacy:{fontSize:14,lineHeight:20,fontWeight:'700',color:C.text2,marginBottom:14,marginHorizontal:4},secondaryActions:{minHeight:54,flexDirection:'row',alignItems:'center',justifyContent:'center',gap:20,flexWrap:'wrap'},link:{fontSize:COMPACT_UI.body,fontWeight:'800',color:C.blue,paddingVertical:10}});

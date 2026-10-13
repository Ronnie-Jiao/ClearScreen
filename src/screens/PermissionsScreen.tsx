import React from 'react';
import { Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { Background, Badge, Card, PrimaryButton, SquareIcon, TopBar } from '../components/UI';
import { C } from '../theme';

type PermissionState={skip:boolean;skipRunning:boolean;checking:boolean;restrictedSettingsLikely:boolean};
export default function PermissionsScreen({onBack,onContinue,onLater,permissions,onOpenAccessibility,onOpenAppInfo,onHelp}:{onBack:()=>void;onContinue:()=>void;onLater:()=>void;permissions:PermissionState;onOpenAccessibility:()=>void;onOpenAppInfo:()=>void;onHelp:()=>void}){
 const ready=permissions.skip&&permissions.skipRunning;
 const status=permissions.checking?'正在检查':ready?'已准备好':permissions.skip?'需要修复':'未开启';
 const statusGood=ready;
 const actionTitle=permissions.checking?null:ready?'选择应用':permissions.skip?'重新开启':'去系统开启';
 return <Background><ScrollView contentContainerStyle={s.page} showsVerticalScrollIndicator={false}>
  <TopBar title="自动跳过服务" onBack={onBack}/>
  <Card style={s.card}>
   <SquareIcon symbol="≫" tone="blue" size={62}/>
   <View style={s.copy}><Text style={s.title}>无障碍服务</Text><Badge green={statusGood}>{status}</Badge></View>
  </Card>
  <Text style={s.privacy}>用于查找可识别的广告控件；识别在本机完成，不上传屏幕内容。</Text>
  {actionTitle?<PrimaryButton title={actionTitle} onPress={ready?onContinue:onOpenAccessibility}/>:null}
  <View style={s.secondaryActions}>
   {!ready&&permissions.restrictedSettingsLikely?<Pressable onPress={onOpenAppInfo} hitSlop={10}><Text style={s.link}>打开应用信息</Text></Pressable>:null}
   {!ready?<Pressable onPress={onHelp} hitSlop={10}><Text style={s.link}>查看步骤</Text></Pressable>:null}
   <Pressable onPress={onLater} hitSlop={10}><Text style={s.link}>{ready?'先进入首页':'稍后再说'}</Text></Pressable>
  </View>
 </ScrollView></Background>;
}

const s=StyleSheet.create({page:{flexGrow:1,padding:18,paddingBottom:36},card:{minHeight:116,padding:18,flexDirection:'row',alignItems:'center',gap:16,marginBottom:12},copy:{flex:1,gap:8},title:{fontSize:22,fontWeight:'900',color:C.navy},privacy:{fontSize:14,lineHeight:20,fontWeight:'700',color:C.text2,marginBottom:20,marginHorizontal:4},secondaryActions:{minHeight:62,flexDirection:'row',alignItems:'center',justifyContent:'center',gap:20,flexWrap:'wrap'},link:{fontSize:15,fontWeight:'900',color:C.blue,paddingVertical:10}});

import React from 'react';
import { Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { Background, BrandHeader, Card, Chevron } from '../components/UI';
import { BottomNav, useBottomNavMetrics } from '../components/BottomNav';
import { C } from '../theme';

type MainTab='home'|'apps'|'settings';
type AdsDestination='systemRecommend'|'appNotifications'|'marketingChannels';

export default function AdsScreen({onTab,onOpen,onOpenAppManagement}:{onTab:(tab:MainTab)=>void;onOpen:(page:AdsDestination)=>void;onOpenAppManagement:()=>void}){
 const {inset:navInset}=useBottomNavMetrics();
 const rows:[string,AdsDestination][]=[
  ['系统推荐开关','systemRecommend'],
  ['应用通知','appNotifications'],
  ['营销通知','marketingChannels'],
 ];
 return <Background><View style={s.root}>
  <ScrollView showsVerticalScrollIndicator={false} contentContainerStyle={[s.scroll,{paddingBottom:96+navInset}]}>
   <BrandHeader title="广告" compact smallText action={<Pressable accessibilityRole="button" accessibilityLabel="打开应用管理" onPress={onOpenAppManagement} style={s.manageButton}><Text style={s.manageText}>应用管理</Text></Pressable>}/>
   <Card style={s.group}>{rows.map(([title,destination],index)=><Pressable key={destination} accessibilityRole="button" onPress={()=>onOpen(destination)} style={[s.row,index===rows.length-1&&s.lastRow]}>
    <Text style={s.rowTitle}>{title}</Text><Chevron/>
   </Pressable>)}</Card>
  </ScrollView>
  <BottomNav active="apps" onChange={onTab}/>
 </View></Background>;
}

const s=StyleSheet.create({
 root:{flex:1},scroll:{flexGrow:1,paddingHorizontal:18},group:{paddingHorizontal:16,marginBottom:12},row:{minHeight:66,paddingVertical:12,flexDirection:'row',alignItems:'center',gap:8,borderBottomWidth:1,borderBottomColor:C.line},lastRow:{borderBottomWidth:0},rowTitle:{flex:1,fontSize:15,fontWeight:'900',color:C.navy},manageButton:{minHeight:34,justifyContent:'center',paddingHorizontal:11,borderRadius:12,backgroundColor:'#E2F2FF'},manageText:{fontSize:12,fontWeight:'900',color:C.blue},
});

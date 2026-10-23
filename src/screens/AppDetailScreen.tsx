import React from 'react';
import { Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { AppItem, LogItem } from '../data';
import { AppIcon, Background, Badge, Card, Toggle, TopBar } from '../components/UI';
import { C, COMPACT_UI } from '../theme';

export default function AppDetailScreen({app,logs,onBack,onChange,onRecords,onOpenRecord}:{app:AppItem;logs:LogItem[];onBack:()=>void;onChange:(a:AppItem)=>void;onRecords:()=>void;onOpenRecord:(log:LogItem)=>void}){
 const recent=logs.filter(log=>log.appId===app.id).slice(0,3);
 const toggle=()=>{
  const skip=!app.skip;
  onChange({...app,skip,whitelist:!skip});
 };
 return <Background><ScrollView showsVerticalScrollIndicator={false} contentContainerStyle={s.page}>
  <TopBar compact title="应用详情" onBack={onBack}/>
  <Card compact style={s.hero}><AppIcon source={app.icon} size={COMPACT_UI.appIcon}/><View style={s.identity}><Text style={s.appName}>{app.name}</Text><Badge compact green={app.skip}>{app.skip?'已开启':'已排除'}</Badge></View></Card>
  <Card compact style={s.options}><View style={s.option}><Text style={s.optTitle}>自动跳过</Text><Toggle compact value={app.skip} onChange={toggle}/></View></Card>
  <Card compact style={s.logs}>
   <View style={s.head}><Text style={s.headTitle}>最近记录</Text><Pressable onPress={onRecords} hitSlop={10}><Text style={s.link}>查看全部 ›</Text></Pressable></View>
   {recent.length?recent.map((log,index)=>{
    const result=log.type==='fail'?'处理失败':log.result==='success'?'已执行跳过':'处理记录';
    return <Pressable key={`${log.timestamp||index}`} onPress={()=>onOpenRecord(log)} style={s.log}><Text style={s.time}>{log.time}</Text><Text style={[s.logText,log.type==='fail'&&{color:C.red}]}>{result}</Text><Text style={s.chevron}>›</Text></Pressable>;
   }):<Text style={s.empty}>还没有处理记录</Text>}
  </Card>
 </ScrollView></Background>;
}

const s=StyleSheet.create({
 page:{paddingHorizontal:18,paddingBottom:36},hero:{padding:COMPACT_UI.cardPadding,flexDirection:'row',alignItems:'center',gap:COMPACT_UI.gap,marginBottom:10},identity:{flex:1,alignItems:'flex-start',gap:4},appName:{fontSize:COMPACT_UI.heading,lineHeight:22,fontWeight:'900',color:C.navy},
 options:{paddingHorizontal:COMPACT_UI.cardPadding,marginBottom:10},option:{minHeight:COMPACT_UI.rowHeight,flexDirection:'row',alignItems:'center',gap:COMPACT_UI.gap},optTitle:{fontSize:COMPACT_UI.body,fontWeight:'800',color:C.navy,flex:1},
 logs:{paddingHorizontal:COMPACT_UI.cardPadding},head:{minHeight:48,flexDirection:'row',alignItems:'center',borderBottomWidth:1,borderBottomColor:C.line},headTitle:{flex:1,fontSize:COMPACT_UI.heading,fontWeight:'900',color:C.navy},link:{fontSize:COMPACT_UI.caption,color:C.blue,fontWeight:'800'},log:{minHeight:COMPACT_UI.rowHeight,flexDirection:'row',alignItems:'center',gap:COMPACT_UI.gap,borderBottomWidth:1,borderBottomColor:C.line},time:{width:50,fontSize:COMPACT_UI.caption,color:C.text2},logText:{flex:1,fontSize:COMPACT_UI.body,fontWeight:'800',color:C.green},chevron:{fontSize:22,color:C.text2},empty:{paddingVertical:20,paddingHorizontal:10,textAlign:'center',fontSize:COMPACT_UI.body,color:C.text2},
});

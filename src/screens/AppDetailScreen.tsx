import React from 'react';
import { Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { AppItem, LogItem } from '../data';
import { AppIcon, Background, Badge, Card, Toggle, TopBar } from '../components/UI';
import { C } from '../theme';

export default function AppDetailScreen({app,logs,onBack,onChange,onRecords,onOpenRecord}:{app:AppItem;logs:LogItem[];onBack:()=>void;onChange:(a:AppItem)=>void;onRecords:()=>void;onOpenRecord:(log:LogItem)=>void}){
 const recent=logs.filter(log=>log.appId===app.id).slice(0,3);
 const toggle=()=>{
  const skip=!app.skip;
  onChange({...app,skip,whitelist:!skip});
 };
 return <Background><ScrollView showsVerticalScrollIndicator={false} contentContainerStyle={s.page}>
  <TopBar title="应用详情" onBack={onBack}/>
  <Card style={s.hero}><AppIcon source={app.icon} size={82}/><View style={s.identity}><Text style={s.appName}>{app.name}</Text><Badge green={app.skip}>{app.skip?'已开启':'已排除'}</Badge></View></Card>
  <Card style={s.options}><View style={s.option}><Text style={s.optTitle}>自动跳过</Text><Toggle value={app.skip} onChange={toggle}/></View></Card>
  <Card style={s.logs}>
   <View style={s.head}><Text style={s.headTitle}>最近记录</Text><Pressable onPress={onRecords} hitSlop={10}><Text style={s.link}>查看全部 ›</Text></Pressable></View>
   {recent.length?recent.map((log,index)=>{
    const result=log.type==='fail'?'处理失败':log.result==='success'?'已执行跳过':'处理记录';
    return <Pressable key={`${log.timestamp||index}`} onPress={()=>onOpenRecord(log)} style={s.log}><Text style={s.time}>{log.time}</Text><Text style={[s.logText,log.type==='fail'&&{color:C.red}]}>{result}</Text><Text style={s.chevron}>›</Text></Pressable>;
   }):<Text style={s.empty}>还没有处理记录</Text>}
  </Card>
 </ScrollView></Background>;
}

const s=StyleSheet.create({
 page:{padding:18,paddingBottom:36},hero:{padding:20,flexDirection:'row',alignItems:'center',gap:18,marginBottom:14},identity:{flex:1,alignItems:'flex-start',gap:8},appName:{fontSize:28,fontWeight:'900',color:C.navy},
 options:{paddingHorizontal:18,marginBottom:14},option:{minHeight:82,flexDirection:'row',alignItems:'center',gap:18},optTitle:{fontSize:21,fontWeight:'900',color:C.navy,flex:1},
 logs:{paddingHorizontal:16},head:{minHeight:58,flexDirection:'row',alignItems:'center',borderBottomWidth:1,borderBottomColor:C.line},headTitle:{flex:1,fontSize:19,fontWeight:'900',color:C.navy},link:{fontSize:15,color:C.blue,fontWeight:'900'},log:{minHeight:64,flexDirection:'row',alignItems:'center',gap:12,borderBottomWidth:1,borderBottomColor:C.line},time:{width:70,fontSize:15,color:C.text2},logText:{flex:1,fontSize:16,fontWeight:'900',color:C.green},chevron:{fontSize:27,color:C.text2},empty:{padding:24,textAlign:'center',color:C.text2},
});

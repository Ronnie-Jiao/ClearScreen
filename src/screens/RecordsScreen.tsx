import React, { useMemo, useState } from 'react';
import { Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { AppItem, LogItem } from '../data';
import { AppIcon, Background, Card, Chevron, TopBar } from '../components/UI';
import { C, COMPACT_UI } from '../theme';

type Filter='all'|'executed'|'fail';
export default function RecordsScreen({apps,logs,onBack,onOpenRecord,filterAppId,todayOnly}:{apps:AppItem[];logs:LogItem[];onBack:()=>void;onOpenRecord:(log:LogItem)=>void;filterAppId?:string;todayOnly?:boolean}){
 const [filter,setFilter]=useState<Filter>('all');
 const todayKey=new Date().toDateString();
 const list=useMemo(()=>logs
  .filter(log=>log.type!=='network')
  .filter(log=>!filterAppId||log.appId===filterAppId)
  .filter(log=>!todayOnly||(typeof log.timestamp==='number'&&new Date(log.timestamp).toDateString()===todayKey))
  .filter(log=>filter==='all'||filter==='fail'&&log.type==='fail'||filter==='executed'&&log.type!=='fail')
  .sort((a,b)=>(b.timestamp||0)-(a.timestamp||0)),[logs,filter,filterAppId,todayKey,todayOnly]);
 const tab=(id:Filter,label:string)=><Pressable accessibilityRole="tab" accessibilityState={{selected:filter===id}} hitSlop={{top:5,bottom:5}} onPress={()=>setFilter(id)} style={[s.tab,filter===id&&s.tabOn]}><Text style={[s.tabText,filter===id&&s.tabTextOn]}>{label}</Text></Pressable>;
 return <Background><ScrollView showsVerticalScrollIndicator={false} contentContainerStyle={s.page}>
  <TopBar compact title="处理记录" onBack={onBack}/>
  <View style={s.tabs}>{tab('all','全部')}{tab('executed','已执行')}{tab('fail','失败')}</View>
  <Card compact style={s.card}>{list.length?list.map((log,index)=>{
    const app=apps.find(item=>item.id===log.appId);
    if(!app)return null;
    const result=log.type==='fail'?'处理失败':log.result==='success'?'已执行跳过':'处理记录';
    return <Pressable accessibilityRole="button" key={`${log.appId}-${log.timestamp||index}`} onPress={()=>onOpenRecord(log)} style={[s.row,index===list.length-1&&{borderBottomWidth:0}]}>
     <AppIcon source={app.icon} size={COMPACT_UI.appIcon}/><View style={s.nameBox}><Text style={s.name} numberOfLines={1}>{app.name}</Text><Text style={s.time}>{log.time}</Text></View><Text style={[s.action,log.type==='fail'&&{color:C.red}]} numberOfLines={1}>{result}</Text><Chevron compact/>
    </Pressable>;
  }):<Text style={s.empty}>还没有处理记录</Text>}</Card>
 </ScrollView></Background>;
}

const s=StyleSheet.create({
 page:{flexGrow:1,paddingHorizontal:18,paddingBottom:30},tabs:{flexDirection:'row',gap:7,marginBottom:10},tab:{flex:1,minHeight:37,borderRadius:12,borderWidth:1,borderColor:C.line,backgroundColor:'#FFFFFF',alignItems:'center',justifyContent:'center'},tabOn:{backgroundColor:C.blue,borderColor:C.blue},tabText:{fontSize:COMPACT_UI.caption,fontWeight:'800',color:C.text2},tabTextOn:{color:'#fff'},
 card:{paddingHorizontal:10},row:{minHeight:COMPACT_UI.rowHeight,flexDirection:'row',alignItems:'center',gap:COMPACT_UI.gap,borderBottomWidth:1,borderBottomColor:C.line},nameBox:{flex:1,minWidth:0},name:{fontSize:COMPACT_UI.body,fontWeight:'800',color:C.text},time:{fontSize:COMPACT_UI.caption,color:C.text2,marginTop:2},action:{flexShrink:1,maxWidth:'42%',fontSize:COMPACT_UI.caption,fontWeight:'800',color:C.green},empty:{paddingVertical:20,textAlign:'center',fontSize:COMPACT_UI.body,fontWeight:'700',color:C.text2},
});

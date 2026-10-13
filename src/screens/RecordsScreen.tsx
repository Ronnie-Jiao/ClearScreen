import React, { useMemo, useState } from 'react';
import { Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { AppItem, LogItem } from '../data';
import { AppIcon, Background, Card, TopBar } from '../components/UI';
import { C } from '../theme';

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
 const tab=(id:Filter,label:string)=><Pressable onPress={()=>setFilter(id)} style={[s.tab,filter===id&&s.tabOn]}><Text style={[s.tabText,filter===id&&s.tabTextOn]}>{label}</Text></Pressable>;
 return <Background><ScrollView showsVerticalScrollIndicator={false} contentContainerStyle={s.page}>
  <TopBar title="处理记录" onBack={onBack}/>
  <View style={s.tabs}>{tab('all','全部')}{tab('executed','已执行')}{tab('fail','失败')}</View>
  <Card style={s.card}>{list.length?list.map((log,index)=>{
    const app=apps.find(item=>item.id===log.appId);
    if(!app)return null;
    const result=log.type==='fail'?'处理失败':log.result==='success'?'已执行跳过':'处理记录';
    return <Pressable accessibilityRole="button" key={`${log.appId}-${log.timestamp||index}`} onPress={()=>onOpenRecord(log)} style={[s.row,index===list.length-1&&{borderBottomWidth:0}]}>
     <AppIcon source={app.icon} size={48}/><View style={s.nameBox}><Text style={s.name} numberOfLines={1}>{app.name}</Text><Text style={s.time}>{log.time}</Text></View><Text style={[s.action,log.type==='fail'&&{color:C.red}]} numberOfLines={2}>{result}</Text><Text style={s.chevron}>›</Text>
    </Pressable>;
  }):<Text style={s.empty}>还没有处理记录</Text>}</Card>
 </ScrollView></Background>;
}

const s=StyleSheet.create({
 page:{padding:18,paddingBottom:36},tabs:{flexDirection:'row',gap:8,marginBottom:14},tab:{flex:1,minHeight:48,borderRadius:17,borderWidth:1,borderColor:'#D4E3EE',backgroundColor:'rgba(255,255,255,.72)',alignItems:'center',justifyContent:'center'},tabOn:{backgroundColor:C.blue,borderColor:C.blue},tabText:{fontSize:16,fontWeight:'900',color:'#5D769B'},tabTextOn:{color:'#fff'},
 card:{paddingHorizontal:14},row:{minHeight:84,flexDirection:'row',alignItems:'center',gap:10,borderBottomWidth:1,borderBottomColor:C.line},nameBox:{width:104},name:{fontSize:17,fontWeight:'900',color:C.navy},time:{fontSize:14,color:C.text2,marginTop:4},action:{flex:1,fontSize:15,fontWeight:'900',color:C.green},chevron:{fontSize:28,color:C.text2},empty:{paddingVertical:30,textAlign:'center',fontSize:17,fontWeight:'800',color:C.text2},
});

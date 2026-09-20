import React from 'react';
import { Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { AppItem, LogItem } from '../data';
import { AppIcon, Background, Badge, Card, Chevron, SquareIcon, Toggle, TopBar } from '../components/UI';
import { C } from '../theme';
export default function AppDetailScreen({app,logs,onBack,onChange,onRecords}:{app:AppItem;logs:LogItem[];onBack:()=>void;onChange:(a:AppItem)=>void;onRecords:()=>void}){
 const recent=logs.filter(l=>l.appId===app.id).slice(0,3);
 const set=(k:'skip'|'network'|'whitelist')=>onChange({...app,[k]:!app[k],...(k==='whitelist'&&!app.whitelist?{skip:false,network:false}:{}),...((k==='skip'||k==='network')&&!app[k]?{whitelist:false}:{})});
 return <Background><ScrollView showsVerticalScrollIndicator={false} contentContainerStyle={s.page}><TopBar title="应用详情" onBack={onBack}/>
  <Card style={s.hero}><AppIcon source={app.icon} size={96}/><View><Text style={s.appName}>{app.name}</Text><Badge>✓  已安装</Badge></View></Card>
  <Card style={s.options}>{[['≫','blue','自动跳过','skip'],['◎','green','网络过滤','network'],['★','purple','加入白名单','whitelist']].map((r:any,i)=><View key={i} style={[s.option,i===2&&{borderBottomWidth:0}]}><SquareIcon symbol={r[0]} tone={r[1]} size={60}/><Text style={s.optTitle}>{r[2]}</Text><Toggle value={(app as any)[r[3]]} onChange={()=>set(r[3])}/></View>)}</Card>
  <View style={s.info}><Text style={s.bulb}>💡</Text><Text style={s.infoText}>双重去广告</Text><Chevron/></View>
  <Card style={s.logs}><View style={s.head}><Text style={s.headTitle}>◷  最近记录</Text><Pressable onPress={onRecords}><Text style={s.link}>查看全部  ›</Text></Pressable></View>{recent.length?recent.map((l,i)=><View key={i} style={s.log}><Text style={s.time}>{l.time}</Text><Text style={[s.logText,l.type==='fail'&&{color:C.red}]}>●  {l.text}</Text></View>):<Text style={s.empty}>暂无记录</Text>}</Card>
  <Card style={s.advanced}><SquareIcon symbol="◇" tone="purple" size={58}/><Text style={s.advText}>高级设置</Text><Chevron/></Card>
 </ScrollView></Background>
}
const s=StyleSheet.create({page:{padding:18,paddingBottom:36},hero:{padding:20,flexDirection:'row',alignItems:'center',gap:20,marginBottom:14},appName:{fontSize:31,fontWeight:'900',color:C.navy,marginBottom:10},options:{paddingHorizontal:18,marginBottom:14},option:{minHeight:104,flexDirection:'row',alignItems:'center',gap:18,borderBottomWidth:1,borderBottomColor:C.line},optTitle:{fontSize:23,fontWeight:'900',color:C.navy,flex:1},info:{height:86,borderRadius:22,borderWidth:1,borderColor:'#BCE7F8',backgroundColor:'rgba(230,248,255,.9)',paddingHorizontal:20,flexDirection:'row',alignItems:'center',gap:16,marginBottom:14},bulb:{fontSize:34},infoText:{flex:1,fontSize:23,fontWeight:'900',color:C.navy},logs:{paddingHorizontal:16,marginBottom:14},head:{height:64,flexDirection:'row',alignItems:'center',borderBottomWidth:1,borderBottomColor:C.line},headTitle:{flex:1,fontSize:20,fontWeight:'900',color:C.navy},link:{fontSize:16,color:C.blue,fontWeight:'900'},log:{height:74,flexDirection:'row',alignItems:'center',borderBottomWidth:1,borderBottomColor:C.line},time:{width:90,fontSize:17,color:C.text2},logText:{fontSize:17,fontWeight:'900',color:C.green},empty:{padding:24,textAlign:'center',color:C.text2},advanced:{height:92,paddingHorizontal:20,flexDirection:'row',alignItems:'center',gap:18},advText:{flex:1,fontSize:22,fontWeight:'900',color:C.navy}})

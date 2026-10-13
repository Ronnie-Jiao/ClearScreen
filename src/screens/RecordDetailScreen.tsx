import React from 'react';
import { Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { AppItem, LogItem } from '../data';
import { AppIcon, Background, Card, PrimaryButton, TopBar } from '../components/UI';
import { C } from '../theme';

export default function RecordDetailScreen({log,app,onBack,onOpenApp,onStopApp}:{log:LogItem;app?:AppItem;onBack:()=>void;onOpenApp:()=>void;onStopApp:()=>void}){
 const failed=log.type==='fail'||log.result==='failed';
 const result=failed?'处理失败':log.result==='success'?'已执行跳过':'处理记录';
 return <Background><ScrollView contentContainerStyle={s.page} showsVerticalScrollIndicator={false}>
  <TopBar title="记录详情" onBack={onBack}/>
  <Card style={s.summary}>
   {app?<AppIcon source={app.icon} size={68}/>:null}
   <View style={s.summaryText}><Text style={s.appName}>{app?.name||'应用'}</Text><Text style={[s.result,failed&&{color:C.red}]}>{result}</Text></View>
  </Card>
  <Card style={s.details}>
   <View style={s.detailRow}><Text style={s.label}>时间</Text><Text style={s.value}>{log.time}</Text></View>
   <View style={[s.detailRow,s.lastRow]}><Text style={s.label}>结果</Text><Text style={[s.value,failed&&{color:C.red}]}>{result}</Text></View>
  </Card>
  {app?<View style={s.actions}>
   {app.skip?<PrimaryButton title="停用此 App" onPress={onStopApp}/>:<View style={s.excluded}><Text style={s.excludedText}>已排除</Text></View>}
   <Pressable onPress={onOpenApp} style={s.secondary}><Text style={s.secondaryText}>查看应用</Text></Pressable>
  </View>:null}
 </ScrollView></Background>;
}

const s=StyleSheet.create({
 page:{padding:18,paddingBottom:36},summary:{padding:18,flexDirection:'row',alignItems:'center',gap:14,marginBottom:14},summaryText:{flex:1},appName:{fontSize:23,fontWeight:'900',color:C.navy},result:{fontSize:17,fontWeight:'900',color:C.green,marginTop:6},details:{paddingHorizontal:18},detailRow:{minHeight:60,flexDirection:'row',alignItems:'center',borderBottomWidth:1,borderBottomColor:C.line},lastRow:{borderBottomWidth:0},label:{flex:1,fontSize:17,fontWeight:'800',color:C.text2},value:{fontSize:17,fontWeight:'900',color:C.navy},actions:{gap:10,marginTop:20},secondary:{minHeight:56,borderRadius:20,alignItems:'center',justifyContent:'center',backgroundColor:'#E8F4FF'},secondaryText:{fontSize:18,fontWeight:'900',color:C.blue},excluded:{minHeight:50,alignItems:'center',justifyContent:'center'},excludedText:{fontSize:16,fontWeight:'900',color:C.green},
});

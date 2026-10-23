import React from 'react';
import { Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { AppItem, LogItem } from '../data';
import { AppIcon, Background, Card, PrimaryButton, TopBar } from '../components/UI';
import { C, COMPACT_UI } from '../theme';

export default function RecordDetailScreen({log,app,onBack,onOpenApp,onStopApp}:{log:LogItem;app?:AppItem;onBack:()=>void;onOpenApp:()=>void;onStopApp:()=>void}){
 const failed=log.type==='fail'||log.result==='failed';
 const result=failed?'处理失败':log.result==='success'?'已执行跳过':'处理记录';
 return <Background><ScrollView contentContainerStyle={s.page} showsVerticalScrollIndicator={false}>
  <TopBar compact title="记录详情" onBack={onBack}/>
  <Card compact style={s.summary}>
   {app?<AppIcon source={app.icon} size={COMPACT_UI.appIcon}/>:null}
   <View style={s.summaryText}><Text style={s.appName}>{app?.name||'应用'}</Text><Text style={[s.result,failed&&{color:C.red}]}>{result}</Text></View>
  </Card>
  <Card compact style={s.details}>
   <View style={s.detailRow}><Text style={s.label}>时间</Text><Text style={s.value}>{log.time}</Text></View>
   <View style={[s.detailRow,s.lastRow]}><Text style={s.label}>结果</Text><Text style={[s.value,failed&&{color:C.red}]}>{result}</Text></View>
  </Card>
  {app?<View style={s.actions}>
   {app.skip?<PrimaryButton compact title="停用此 App" onPress={onStopApp}/>:<View style={s.excluded}><Text style={s.excludedText}>已排除</Text></View>}
   <Pressable onPress={onOpenApp} style={s.secondary}><Text style={s.secondaryText}>查看应用</Text></Pressable>
  </View>:null}
 </ScrollView></Background>;
}

const s=StyleSheet.create({
 page:{paddingHorizontal:18,paddingBottom:36},summary:{padding:COMPACT_UI.cardPadding,flexDirection:'row',alignItems:'center',gap:COMPACT_UI.gap,marginBottom:10},summaryText:{flex:1},appName:{fontSize:COMPACT_UI.heading,lineHeight:22,fontWeight:'900',color:C.navy},result:{fontSize:COMPACT_UI.body,lineHeight:20,fontWeight:'800',color:C.green,marginTop:4},details:{paddingHorizontal:COMPACT_UI.cardPadding},detailRow:{minHeight:COMPACT_UI.rowHeight,flexDirection:'row',alignItems:'center',gap:COMPACT_UI.gap,borderBottomWidth:1,borderBottomColor:C.line},lastRow:{borderBottomWidth:0},label:{flex:1,fontSize:COMPACT_UI.body,fontWeight:'800',color:C.text2},value:{flexShrink:1,fontSize:COMPACT_UI.body,fontWeight:'800',color:C.navy},actions:{gap:10,marginTop:14},secondary:{minHeight:COMPACT_UI.buttonHeight,paddingVertical:10,paddingHorizontal:14,borderRadius:14,alignItems:'center',justifyContent:'center',backgroundColor:'#E8F4FF'},secondaryText:{fontSize:COMPACT_UI.body,lineHeight:20,fontWeight:'800',color:C.blue},excluded:{minHeight:COMPACT_UI.buttonHeight,alignItems:'center',justifyContent:'center'},excludedText:{fontSize:COMPACT_UI.body,fontWeight:'800',color:C.green},
});

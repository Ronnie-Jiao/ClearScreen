import React from 'react';
import { ScrollView, StyleSheet, Text, View } from 'react-native';
import { LogItem } from '../data';
import { Background, Card, PrimaryButton, TopBar } from '../components/UI';
import { C } from '../theme';

type Permissions={skip:boolean;skipRunning:boolean;accessibilityServiceBound?:boolean};
export default function DiagnosticsScreen({permissions,logs,onBack,onRepair}:{permissions:Permissions;logs:LogItem[];onBack:()=>void;onRepair:()=>void}){
 const latestFailure=logs.find(log=>log.type==='fail');
 const row=(title:string,value:string,good:boolean,last=false)=><View style={[s.row,last&&{borderBottomWidth:0}]}><Text style={s.label}>{title}</Text><Text style={[s.value,!good&&s.valueOff]}>{value}</Text></View>;
 return <Background><ScrollView contentContainerStyle={s.page} showsVerticalScrollIndicator={false}>
  <TopBar title="故障诊断" onBack={onBack}/>
  <Card style={s.card}>
   {row('系统授权',permissions.skip?'已授权':'未授权',permissions.skip)}
   {row('服务连接',permissions.skipRunning?'已连接':'未连接',permissions.skipRunning)}
   {row('最近异常',latestFailure?.time||'暂无',Boolean(latestFailure),true)}
  </Card>
  {!permissions.skip||!permissions.skipRunning?<View style={s.action}><PrimaryButton title="去修复" onPress={onRepair}/></View>:null}
 </ScrollView></Background>;
}

const s=StyleSheet.create({page:{padding:18,paddingBottom:36},card:{paddingHorizontal:18},row:{minHeight:64,flexDirection:'row',alignItems:'center',borderBottomWidth:1,borderBottomColor:C.line},label:{flex:1,fontSize:17,fontWeight:'800',color:C.text2},value:{fontSize:17,fontWeight:'900',color:C.green},valueOff:{color:C.red},action:{marginTop:18}});

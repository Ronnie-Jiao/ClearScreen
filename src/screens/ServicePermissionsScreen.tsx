import React from 'react';
import { Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { LogItem } from '../data';
import { Background, Card, Chevron, TopBar } from '../components/UI';
import { C } from '../theme';

type PermissionState={skip:boolean;skipRunning:boolean;bg:boolean;isVivoFamily?:boolean};

export default function ServicePermissionsScreen({permissions,logs,onBack,onPermissions,onBackground}:{
  permissions:PermissionState;
  logs:LogItem[];
  onBack:()=>void;
  onPermissions:()=>void;
  onBackground:()=>void;
}){
  const skipStatus=permissions.skipRunning?'已连接':permissions.skip?'需要修复':'未开启';
  const backgroundStatus=permissions.isVivoFamily?'待检查':permissions.bg?'已允许':'待检查';
  const latestFailure=logs.find(log=>log.type==='fail');
  const row=(title:string,value:string|undefined,onPress:()=>void,last=false)=>(
    <Pressable accessibilityRole="button" onPress={onPress} style={[s.row,last&&{borderBottomWidth:0}]}>
      <Text style={s.rowTitle}>{title}</Text>
      {value?<Text style={s.value}>{value}</Text>:null}
      <Chevron/>
    </Pressable>
  );
  const statusRow=(title:string,value:string,good:boolean,last=false)=>(
    <View style={[s.statusRow,last&&{borderBottomWidth:0}]}>
      <Text style={s.statusLabel}>{title}</Text>
      <Text style={[s.statusValue,!good&&s.statusValueOff]}>{value}</Text>
    </View>
  );

  return <Background><ScrollView contentContainerStyle={s.page} showsVerticalScrollIndicator={false}>
    <TopBar title="服务与权限" onBack={onBack}/>
    <Card style={s.card}>
      {row('自动跳过服务',skipStatus,onPermissions)}
      {row('后台运行',backgroundStatus,onBackground,true)}
    </Card>
    <Card style={s.statusCard}>
      {statusRow('系统授权',permissions.skip?'已授权':'未授权',permissions.skip)}
      {statusRow('服务连接',permissions.skipRunning?'已连接':'未连接',permissions.skipRunning)}
      {statusRow('最近异常',latestFailure?.time||'暂无',!latestFailure,true)}
    </Card>
  </ScrollView></Background>;
}

const s=StyleSheet.create({
  page:{padding:18,paddingBottom:36},
  card:{paddingHorizontal:16},
  row:{minHeight:66,flexDirection:'row',alignItems:'center',gap:8,borderBottomWidth:1,borderBottomColor:C.line},
  rowTitle:{flex:1,fontSize:17,fontWeight:'900',color:C.navy},
  value:{fontSize:15,fontWeight:'800',color:C.text2},
  statusCard:{paddingHorizontal:18,marginTop:14},
  statusRow:{minHeight:64,flexDirection:'row',alignItems:'center',borderBottomWidth:1,borderBottomColor:C.line},
  statusLabel:{flex:1,fontSize:17,fontWeight:'800',color:C.text2},
  statusValue:{fontSize:17,fontWeight:'900',color:C.green},
  statusValueOff:{color:C.red},
});

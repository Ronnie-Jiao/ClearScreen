import React from 'react';
import { Pressable, StyleSheet, Text, View, useWindowDimensions } from 'react-native';
import { Background, Badge, Card, PrimaryButton, SquareIcon, TopBar } from '../components/UI';
import { C } from '../theme';

type PermissionState={skip:boolean;skipRunning?:boolean;network:boolean;bg:boolean;isVivoFamily?:boolean;vendorStartupGuideConfirmed?:boolean};
export default function PermissionsScreen({onBack,onContinue,permissions,onToggle}:{onBack:()=>void;onContinue:()=>void;permissions:PermissionState;onToggle:(k:'skip'|'network'|'bg')=>void}){
 const { width, height } = useWindowDimensions();
 const scale = Math.min(1, Math.max(.84, Math.min(width / 360, height / 800)));
 const px = (value:number) => Math.round(value * scale);
 const vendorStartupPending=Boolean(permissions.isVivoFamily&&!permissions.vendorStartupGuideConfirmed);
 const row=(key:'skip'|'network'|'bg',symbol:string,tone:any,title:string,button:string)=>{
  const enabled=key==='bg'?(permissions.bg||Boolean(permissions.isVivoFamily&&permissions.vendorStartupGuideConfirmed)):permissions[key];
  const status=key==='skip'&&!permissions.skipRunning&&permissions.skip?'●  启动中':key==='bg'&&vendorStartupPending?'●  需开自启动':key==='bg'&&permissions.isVivoFamily&&enabled?'✓  已完成检查':enabled?'✓  已开启':'●  未开启';
  const confirmed=key==='skip'?Boolean(permissions.skipRunning):enabled&&!vendorStartupPending;
  return <Card key={key} style={[s.card,{height:px(144),padding:px(14),marginBottom:px(12),gap:px(12)}]}><SquareIcon symbol={symbol} tone={tone} size={px(64)}/><View style={[s.copy,{gap:px(7)}]}><Text style={[s.title,{fontSize:px(22),lineHeight:px(27)}]}>{title}</Text><Badge green={confirmed}>{status}</Badge></View><Pressable onPress={()=>onToggle(key)} style={[s.button,{width:px(92),height:px(56),borderRadius:px(20)}]}><Text style={[s.buttonText,{fontSize:px(18),lineHeight:px(22)}]}>{key==='bg'&&permissions.isVivoFamily?'去设置':enabled&&key!=='bg'?'去设置':button}</Text></Pressable></Card>;
 };
 return <Background><View style={[s.page,{paddingHorizontal:px(20),paddingTop:px(18)}]}><TopBar title="开启权限" onBack={onBack}/><View style={s.content}>
  {row('skip','≫','blue','自动跳过权限','去设置')}
  {row('network','◎','green','网络过滤权限','去设置')}
  {row('bg','⚙','purple','后台运行','去开启')}
  <View style={[s.continue,{marginTop:px(2),paddingBottom:px(4)}]}><PrimaryButton title="继续" onPress={onContinue}/></View>
 </View></View></Background>
}
const s=StyleSheet.create({page:{flex:1},content:{flex:1},card:{flexDirection:'row',alignItems:'center'},copy:{flex:1,minWidth:0},title:{fontWeight:'900',color:C.navy},button:{backgroundColor:C.blue,alignItems:'center',justifyContent:'center',overflow:'hidden'},buttonText:{color:'#fff',fontWeight:'900'},continue:{width:'100%'}})

import React from 'react';
import { Pressable, StyleSheet, Text, View, useWindowDimensions } from 'react-native';
import { Background, Badge, Card, PrimaryButton, SquareIcon, TopBar } from '../components/UI';
import { C } from '../theme';

export default function PermissionsScreen({onBack,onContinue,permissions,onToggle}:{onBack:()=>void;onContinue:()=>void;permissions:{skip:boolean;network:boolean;bg:boolean};onToggle:(k:'skip'|'network'|'bg')=>void}){
 const { width, height } = useWindowDimensions();
 const scale = Math.min(1, Math.max(.84, Math.min(width / 360, height / 800)));
 const px = (value:number) => Math.round(value * scale);
 const row=(key:'skip'|'network'|'bg',symbol:string,tone:any,title:string,button:string)=><Card key={key} style={[s.card,{height:px(144),padding:px(14),marginBottom:px(12),gap:px(12)}]}><SquareIcon symbol={symbol} tone={tone} size={px(64)}/><View style={[s.copy,{gap:px(7)}]}><Text style={[s.title,{fontSize:px(22),lineHeight:px(27)}]}>{title}</Text><Badge green={permissions[key]}>{permissions[key]?'✓  已开启':'●  未开启'}</Badge></View><Pressable onPress={()=>onToggle(key)} style={[s.button,{width:px(92),height:px(56),borderRadius:px(20)}]}><Text style={[s.buttonText,{fontSize:px(18),lineHeight:px(22)}]}>{permissions[key]&&key!=='bg'?'去设置':button}</Text></Pressable></Card>;
 return <Background><View style={[s.page,{paddingHorizontal:px(20),paddingTop:px(18)}]}><TopBar title="开启权限" onBack={onBack}/><View style={s.content}>
  {row('skip','≫','blue','自动跳过权限','去设置')}
  {row('network','◎','green','网络过滤权限','去设置')}
  {row('bg','⚙','purple','后台运行','去开启')}
  <View style={[s.continue,{marginTop:px(2),paddingBottom:px(4)}]}><PrimaryButton title="继续" onPress={onContinue}/></View>
 </View></View></Background>
}
const s=StyleSheet.create({page:{flex:1},content:{flex:1},card:{flexDirection:'row',alignItems:'center'},copy:{flex:1,minWidth:0},title:{fontWeight:'900',color:C.navy},button:{backgroundColor:C.blue,alignItems:'center',justifyContent:'center',overflow:'hidden'},buttonText:{color:'#fff',fontWeight:'900'},continue:{width:'100%'}})

import React from 'react';
import { ScrollView, StyleSheet, Text, View } from 'react-native';
import { Background, Badge, Card, PrimaryButton, SquareIcon, TopBar } from '../components/UI';
import { C } from '../theme';

export default function PermissionsScreen({onBack,onContinue,permissions,onToggle}:{onBack:()=>void;onContinue:()=>void;permissions:{skip:boolean;network:boolean;bg:boolean};onToggle:(k:'skip'|'network'|'bg')=>void}){
 const row=(key:'skip'|'network'|'bg',symbol:string,tone:any,title:string,button:string)=><Card style={s.card}><SquareIcon symbol={symbol} tone={tone} size={88}/><View style={s.copy}><Text style={s.title}>{title}</Text><Badge green={permissions[key]}>{permissions[key]?'✓  已开启':'●  未开启'}</Badge></View><Text onPress={()=>onToggle(key)} style={s.button}>{permissions[key]&&key!=='bg'?'去设置':button}</Text></Card>;
 return <Background><View style={s.page}><TopBar title="开启权限" onBack={onBack}/><ScrollView showsVerticalScrollIndicator={false} contentContainerStyle={{paddingBottom:30}}>
  {row('skip','≫','blue','自动跳过权限','去设置')}
  {row('network','◎','green','网络过滤权限','去设置')}
  {row('bg','⚙','purple','后台运行','去开启')}
  <View style={{height:250}}/><PrimaryButton title="继续" onPress={onContinue}/>
 </ScrollView></View></Background>
}
const s=StyleSheet.create({page:{flex:1,paddingHorizontal:20,paddingTop:18},card:{minHeight:185,padding:24,marginBottom:16,flexDirection:'row',alignItems:'center',gap:18},copy:{flex:1,gap:10},title:{fontSize:24,fontWeight:'900',color:C.navy},button:{backgroundColor:C.blue,color:'#fff',fontSize:19,fontWeight:'900',paddingVertical:16,paddingHorizontal:24,borderRadius:22,overflow:'hidden'}})

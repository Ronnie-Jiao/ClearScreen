import React from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import { C } from '../theme';

type Tab='home'|'apps'|'settings';
export function BottomNav({active,onChange}:{active:Tab;onChange:(t:Tab)=>void}){
  const items:[Tab,string,string][]=[['home','首页','⌂'],['apps','应用','▦'],['settings','设置','⚙']];
  return <View style={s.nav}>{items.map(([id,label,icon])=><Pressable key={id} onPress={()=>onChange(id)} style={s.item}><Text style={[s.icon,active===id&&s.active]}>{icon}</Text><Text style={[s.text,active===id&&s.active]}>{label}</Text></Pressable>)}</View>
}
const s=StyleSheet.create({nav:{position:'absolute',left:0,right:0,bottom:0,height:88,backgroundColor:'rgba(255,255,255,.97)',borderTopWidth:1,borderTopColor:'#DDEBF4',flexDirection:'row'},item:{flex:1,alignItems:'center',justifyContent:'center',gap:2},icon:{fontSize:29,color:'#7389A7',fontWeight:'900'},text:{fontSize:14,color:'#7389A7',fontWeight:'800'},active:{color:C.blue}})

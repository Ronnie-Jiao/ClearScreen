import React from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { C } from '../theme';

type Tab='home'|'apps'|'settings';
export function BottomNav({active,onChange}:{active:Tab;onChange:(t:Tab)=>void}){
  const items:{id:Tab;label:string;icon:keyof typeof Ionicons.glyphMap;activeIcon:keyof typeof Ionicons.glyphMap}[]=[
    {id:'home',label:'首页',icon:'home-outline',activeIcon:'home'},
    {id:'apps',label:'应用',icon:'grid-outline',activeIcon:'grid'},
    {id:'settings',label:'设置',icon:'settings-outline',activeIcon:'settings'},
  ];
  return <View style={s.nav}>{items.map(({id,label,icon,activeIcon})=><Pressable key={id} accessibilityRole="tab" accessibilityState={{selected:active===id}} onPress={()=>onChange(id)} style={s.item}><View style={[s.iconWrap,active===id&&s.iconWrapActive]}><Ionicons name={active===id?activeIcon:icon} size={23} color={active===id?C.blue:'#8290A3'}/></View><Text style={[s.text,active===id&&s.active]}>{label}</Text></Pressable>)}</View>
}
const s=StyleSheet.create({nav:{position:'absolute',left:0,right:0,bottom:0,height:70,backgroundColor:'rgba(255,255,255,0.98)',borderTopWidth:1,borderTopColor:C.line,flexDirection:'row'},item:{flex:1,alignItems:'center',justifyContent:'center',gap:2},iconWrap:{width:44,height:31,alignItems:'center',justifyContent:'center',borderRadius:16},iconWrapActive:{backgroundColor:'#E4F2FF'},text:{fontSize:12,color:'#687A91',fontWeight:'800'},active:{color:C.blue}})

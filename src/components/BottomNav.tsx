import React, { useContext } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import { MaterialCommunityIcons } from '@expo/vector-icons';
import { C } from '../theme';
import { ScreenInsetsContext } from './ScreenInsets';

type Tab='home'|'apps'|'settings';

// The bar surface keeps reaching the screen bottom; the system gesture area is
// reserved below the icons and labels so they never sit on the gesture handle.
export const BOTTOM_NAV_HEIGHT = 70;

export function useBottomNavMetrics(){
  const {bottom}=useContext(ScreenInsetsContext);
  return {inset:bottom,height:BOTTOM_NAV_HEIGHT+bottom};
}

export function BottomNav({active,onChange}:{active:Tab;onChange:(t:Tab)=>void}){
  const items:{id:Tab;label:string;icon:keyof typeof MaterialCommunityIcons.glyphMap;activeIcon:keyof typeof MaterialCommunityIcons.glyphMap}[]=[
    {id:'home',label:'首页',icon:'home-variant-outline',activeIcon:'home-variant'},
    {id:'apps',label:'广告',icon:'bullhorn-outline',activeIcon:'bullhorn'},
    {id:'settings',label:'设置',icon:'cog-outline',activeIcon:'cog'},
  ];
  const {inset,height}=useBottomNavMetrics();
  return <View style={[s.nav,{height,paddingBottom:inset}]}>{items.map(({id,label,icon,activeIcon})=><Pressable key={id} accessibilityRole="tab" accessibilityState={{selected:active===id}} onPress={()=>onChange(id)} style={s.item}><View style={s.iconWrap}><MaterialCommunityIcons name={active===id?activeIcon:icon} size={26} color={active===id?C.blue:'#8094AB'}/></View><Text style={[s.text,active===id&&s.active]}>{label}</Text></Pressable>)}</View>
}
const s=StyleSheet.create({nav:{position:'absolute',left:0,right:0,bottom:0,backgroundColor:'rgba(255,255,255,0.98)',borderTopLeftRadius:24,borderTopRightRadius:24,flexDirection:'row',shadowColor:'#4A7EA2',shadowOpacity:.10,shadowRadius:14,shadowOffset:{width:0,height:-4},elevation:6},item:{flex:1,alignItems:'center',justifyContent:'center',gap:2},iconWrap:{width:54,height:32,alignItems:'center',justifyContent:'center',borderRadius:16},text:{fontSize:12,color:'#687A91',fontWeight:'800'},active:{color:C.blue}})

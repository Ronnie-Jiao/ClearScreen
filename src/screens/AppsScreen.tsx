import React, { useMemo, useState } from 'react';
import { Pressable, ScrollView, StyleSheet, Text, TextInput, View } from 'react-native';
import { AppItem } from '../data';
import { AppIcon, Background, BrandHeader, Card, Toggle } from '../components/UI';
import { BottomNav } from '../components/BottomNav';
import { C } from '../theme';

type Filter='all'|'enabled'|'white';
export default function AppsScreen({apps,onChangeApps,onTab,onOpen,onWhitelist}:{apps:AppItem[];onChangeApps:(a:AppItem[])=>void;onTab:(t:'home'|'apps'|'settings')=>void;onOpen:(id:string)=>void;onWhitelist:()=>void}){
 const [filter,setFilter]=useState<Filter>('all'); const [q,setQ]=useState('');
 const list=useMemo(()=>apps.filter(a=>(!q||a.name.includes(q))&&(filter==='all'||filter==='enabled'&&a.skip||filter==='white'&&a.whitelist)),[apps,q,filter]);
 const toggle=(id:string)=>onChangeApps(apps.map(a=>a.id===id?{...a,skip:!a.skip,whitelist:a.skip?a.whitelist:false}:a));
 const chip=(id:Filter,label:string)=><Pressable onPress={()=>id==='white'?onWhitelist():setFilter(id)} style={[s.chip,filter===id&&s.chipOn]}><Text style={[s.chipText,filter===id&&s.chipTextOn]}>{label}</Text></Pressable>;
 return <Background><View style={{flex:1}}><ScrollView showsVerticalScrollIndicator={false} contentContainerStyle={s.scroll}><BrandHeader title="应用" tagline={false}/>
  <View style={s.search}><Text style={s.searchIcon}>⌕</Text><TextInput value={q} onChangeText={setQ} placeholder="搜索应用" placeholderTextColor="#8296B1" style={s.input}/></View>
  <View style={s.chips}>{chip('all',`全部 (${apps.length})`)}{chip('enabled','已开启')}{chip('white','白名单')}</View>
  <Card style={s.list}>{list.map((a,i)=><Pressable onPress={()=>onOpen(a.id)} key={a.id} style={[s.row,i===list.length-1&&{borderBottomWidth:0}]}><AppIcon source={a.icon} size={58}/><View style={{flex:1}}><Text style={s.name}>{a.name}</Text>{a.whitelist?<Text style={s.whiteTag}>白名单</Text>:null}</View><Toggle value={a.skip} onChange={()=>toggle(a.id)}/></Pressable>)}</Card>
 </ScrollView><BottomNav active="apps" onChange={onTab}/></View></Background>
}
const s=StyleSheet.create({scroll:{padding:18,paddingBottom:110},search:{height:64,borderRadius:22,backgroundColor:'#fff',flexDirection:'row',alignItems:'center',paddingHorizontal:20,marginBottom:14,shadowColor:'#689',shadowOpacity:.08,shadowRadius:16,elevation:2},searchIcon:{fontSize:34,color:C.navy},input:{flex:1,fontSize:18,marginLeft:12,color:C.text},chips:{flexDirection:'row',gap:10,marginBottom:14},chip:{height:52,borderRadius:26,paddingHorizontal:24,borderWidth:1,borderColor:'#D5E4EF',backgroundColor:'rgba(255,255,255,.75)',alignItems:'center',justifyContent:'center'},chipOn:{backgroundColor:C.blue,borderColor:C.blue},chipText:{fontSize:17,fontWeight:'900',color:'#60789C'},chipTextOn:{color:'#fff'},list:{paddingHorizontal:16},row:{minHeight:104,flexDirection:'row',alignItems:'center',gap:16,borderBottomWidth:1,borderBottomColor:C.line},name:{fontSize:22,fontWeight:'900',color:C.navy},whiteTag:{alignSelf:'flex-start',marginTop:5,backgroundColor:C.greenBg,color:C.green,paddingHorizontal:12,paddingVertical:4,borderRadius:14,overflow:'hidden',fontSize:14,fontWeight:'900'}})

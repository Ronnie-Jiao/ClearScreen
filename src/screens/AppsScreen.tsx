import React, { useMemo, useState } from 'react';
import { Pressable, ScrollView, StyleSheet, Text, TextInput, View } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { AppItem } from '../data';
import { AppIcon, Background, BrandHeader, Card, Toggle } from '../components/UI';
import { BottomNav } from '../components/BottomNav';
import { C } from '../theme';

type Filter='all'|'enabled'|'excluded';
type MainTab='home'|'apps'|'settings';
export default function AppsScreen({apps,onChangeApps,onTab,onOpen}:{apps:AppItem[];onChangeApps:(a:AppItem[])=>void;onTab:(t:MainTab)=>void;onOpen:(id:string)=>void}){
 const [filter,setFilter]=useState<Filter>('all');
 const [query,setQuery]=useState('');
 const enabledCount=apps.filter(app=>app.skip).length;
 const list=useMemo(()=>apps.filter(app=>(!query||app.name.toLocaleLowerCase().includes(query.toLocaleLowerCase()))&&(filter==='all'||filter==='enabled'&&app.skip||filter==='excluded'&&!app.skip)),[apps,filter,query]);
 const toggle=(app:AppItem)=>{
  const skip=!app.skip;
  onChangeApps(apps.map(item=>item.id===app.id?{...item,skip,whitelist:!skip}:item));
 };
 const enableAll=()=>{
  onChangeApps(apps.map(app=>({...app,skip:true,network:false,whitelist:false})));
  setFilter('all');
 };
 const chip=(id:Filter,label:string,count:number)=><Pressable accessibilityRole="button" hitSlop={{top:5,bottom:5}} onPress={()=>setFilter(id)} style={[s.chip,filter===id&&s.chipOn]}><Text style={[s.chipText,filter===id&&s.chipTextOn]}>{label} {count}</Text></Pressable>;
 const emptyLabel=query?'未找到应用':filter==='enabled'?'还没有启用的应用':filter==='excluded'?'没有已排除的应用':'暂无应用';
 return <Background><View style={{flex:1}}>
  <View style={s.fixedHeader}>
  <BrandHeader title="应用" tagline={false} compact smallText action={<Pressable accessibilityRole="button" disabled={!apps.length} accessibilityState={{disabled:!apps.length}} onPress={enableAll} hitSlop={{top:5,bottom:5}} style={s.manageButton}><Text style={s.manageButtonText}>全部开启</Text></Pressable>}/>
  <View style={s.search}><Ionicons name="search-outline" size={18} color={C.text2}/><TextInput value={query} onChangeText={setQuery} placeholder="搜索应用" placeholderTextColor="#8296B1" style={s.input}/>{query?<Pressable accessibilityRole="button" onPress={()=>setQuery('')} hitSlop={12}><Ionicons name="close-circle" size={18} color={C.text2}/></Pressable>:null}</View>
  <View style={s.chips}>{chip('all','全部',apps.length)}{chip('enabled','已开启',enabledCount)}{chip('excluded','已排除',apps.length-enabledCount)}</View>
  </View>
  <ScrollView style={s.scrollViewport} showsVerticalScrollIndicator={false} contentContainerStyle={s.scroll}>
   <Card style={s.list}>{list.length?list.map((app,index)=><View key={app.id} style={[s.row,index===list.length-1&&{borderBottomWidth:0}]}>
    <Pressable accessibilityRole="button" onPress={()=>onOpen(app.id)} style={s.rowBody}><AppIcon source={app.icon} size={36}/><Text style={s.name} numberOfLines={1}>{app.name}</Text></Pressable>
    <Toggle compact value={app.skip} onChange={()=>toggle(app)}/>
   </View>):<Text style={s.empty}>{emptyLabel}</Text>}</Card>
  </ScrollView>
  <BottomNav active="apps" onChange={onTab}/></View></Background>;
}

const s=StyleSheet.create({
 fixedHeader:{paddingHorizontal:18},scrollViewport:{flex:1,marginBottom:70},scroll:{flexGrow:1,paddingHorizontal:18,paddingBottom:12},manageButton:{minHeight:34,justifyContent:'center',paddingHorizontal:11,borderRadius:12,backgroundColor:'#E2F2FF'},manageButtonText:{fontSize:12,fontWeight:'900',color:C.blue},
 search:{height:48,borderRadius:15,backgroundColor:'rgba(255,255,255,0.96)',borderWidth:1,borderColor:'rgba(255,255,255,0.98)',flexDirection:'row',alignItems:'center',paddingHorizontal:13,marginBottom:10},input:{flex:1,fontSize:14,fontWeight:'600',marginLeft:8,color:C.text},
 chips:{flexDirection:'row',gap:7,marginBottom:10},chip:{flex:1,minHeight:37,borderRadius:12,paddingHorizontal:4,borderWidth:1,borderColor:C.line,backgroundColor:'#FFFFFF',alignItems:'center',justifyContent:'center'},chipOn:{backgroundColor:C.blue,borderColor:C.blue},chipText:{fontSize:12,fontWeight:'800',color:C.text2},chipTextOn:{color:'#fff'},
 list:{flexGrow:1,paddingHorizontal:10},row:{minHeight:54,flexDirection:'row',alignItems:'center',gap:10,borderBottomWidth:1,borderBottomColor:C.line},rowBody:{flex:1,minHeight:50,flexDirection:'row',alignItems:'center',gap:10},name:{flex:1,fontSize:14,fontWeight:'800',color:C.text},empty:{padding:20,textAlign:'center',fontSize:14,fontWeight:'700',color:C.text2},
});

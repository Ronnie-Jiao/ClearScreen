import React, { useMemo, useState } from 'react';
import { Pressable, ScrollView, StyleSheet, Text, TextInput, View } from 'react-native';
import { AppItem } from '../data';
import { AppIcon, Background, BrandHeader, Card, Toggle } from '../components/UI';
import { BottomNav } from '../components/BottomNav';
import { C } from '../theme';

type Filter='all'|'enabled'|'excluded';
type MainTab='home'|'apps'|'settings';
export default function AppsScreen({apps,onChangeApps,onTab,onOpen,onWhitelist}:{apps:AppItem[];onChangeApps:(a:AppItem[])=>void;onTab:(t:MainTab)=>void;onOpen:(id:string)=>void;onWhitelist:()=>void}){
 const [filter,setFilter]=useState<Filter>('all');
 const [query,setQuery]=useState('');
 const enabledCount=apps.filter(app=>app.skip).length;
 const list=useMemo(()=>apps.filter(app=>(!query||app.name.toLocaleLowerCase().includes(query.toLocaleLowerCase()))&&(filter==='all'||filter==='enabled'&&app.skip||filter==='excluded'&&!app.skip)),[apps,filter,query]);
 const toggle=(app:AppItem)=>{
  const skip=!app.skip;
  onChangeApps(apps.map(item=>item.id===app.id?{...item,skip,whitelist:!skip}:item));
 };
 const chip=(id:Filter,label:string,count:number)=><Pressable accessibilityRole="button" onPress={()=>setFilter(id)} style={[s.chip,filter===id&&s.chipOn]}><Text style={[s.chipText,filter===id&&s.chipTextOn]}>{label} {count}</Text></Pressable>;
 const emptyLabel=query?'未找到应用':filter==='enabled'?'还没有启用的应用':filter==='excluded'?'没有已排除的应用':'暂无应用';
 return <Background><View style={{flex:1}}><ScrollView showsVerticalScrollIndicator={false} contentContainerStyle={s.scroll}>
  <BrandHeader title="应用" tagline={false} action={<Pressable onPress={onWhitelist} style={s.manageButton}><Text style={s.manageButtonText}>排除应用</Text></Pressable>}/>
  <View style={s.search}><Text style={s.searchIcon}>⌕</Text><TextInput value={query} onChangeText={setQuery} placeholder="搜索应用" placeholderTextColor="#8296B1" style={s.input}/>{query?<Pressable accessibilityRole="button" onPress={()=>setQuery('')} hitSlop={12}><Text style={s.clear}>×</Text></Pressable>:null}</View>
  <View style={s.chips}>{chip('all','全部',apps.length)}{chip('enabled','已开启',enabledCount)}{chip('excluded','已排除',apps.length-enabledCount)}</View>
  <Card style={s.list}>{list.length?list.map((app,index)=><View key={app.id} style={[s.row,index===list.length-1&&{borderBottomWidth:0}]}>
    <Pressable accessibilityRole="button" onPress={()=>onOpen(app.id)} style={s.rowBody}><AppIcon source={app.icon} size={56}/><Text style={s.name} numberOfLines={1}>{app.name}</Text></Pressable>
    <Toggle value={app.skip} onChange={()=>toggle(app)}/>
   </View>):<Text style={s.empty}>{emptyLabel}</Text>}</Card>
 </ScrollView><BottomNav active="apps" onChange={onTab}/></View></Background>;
}

const s=StyleSheet.create({
 scroll:{padding:18,paddingBottom:110},manageButton:{minHeight:44,justifyContent:'center',paddingHorizontal:13,borderRadius:15,backgroundColor:'#E8F4FF'},manageButtonText:{fontSize:14,fontWeight:'900',color:C.blue},
 search:{height:60,borderRadius:20,backgroundColor:'#fff',flexDirection:'row',alignItems:'center',paddingHorizontal:18,marginBottom:14,shadowColor:'#689',shadowOpacity:.08,shadowRadius:16,elevation:2},searchIcon:{fontSize:32,color:C.navy},input:{flex:1,fontSize:18,marginLeft:10,color:C.text},clear:{fontSize:26,color:C.text2,paddingHorizontal:4},
 chips:{flexDirection:'row',gap:8,marginBottom:14},chip:{flex:1,minHeight:48,borderRadius:17,paddingHorizontal:5,borderWidth:1,borderColor:'#D5E4EF',backgroundColor:'rgba(255,255,255,.75)',alignItems:'center',justifyContent:'center'},chipOn:{backgroundColor:C.blue,borderColor:C.blue},chipText:{fontSize:14,fontWeight:'900',color:'#60789C'},chipTextOn:{color:'#fff'},
 list:{paddingHorizontal:16},row:{minHeight:90,flexDirection:'row',alignItems:'center',gap:14,borderBottomWidth:1,borderBottomColor:C.line},rowBody:{flex:1,minHeight:88,flexDirection:'row',alignItems:'center',gap:15},name:{flex:1,fontSize:20,fontWeight:'900',color:C.navy},empty:{padding:28,textAlign:'center',fontSize:17,fontWeight:'800',color:C.text2},
});

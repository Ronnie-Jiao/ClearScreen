import React, { useMemo, useState } from 'react';
import { Pressable, ScrollView, StyleSheet, Text, TextInput, View } from 'react-native';
import { AppItem } from '../data';
import { AppIcon, Background, Card, Toggle, TopBar } from '../components/UI';
import { C, COMPACT_UI } from '../theme';

type Filter='all'|'excluded';
export default function WhitelistScreen({apps,onBack,onChangeApps}:{apps:AppItem[];onBack:()=>void;onChangeApps:(apps:AppItem[])=>void}){
 const [query,setQuery]=useState('');
 const [filter,setFilter]=useState<Filter>('all');
 const excludedCount=apps.filter(app=>!app.skip).length;
 const list=useMemo(()=>apps.filter(app=>(!query||app.name.toLocaleLowerCase().includes(query.toLocaleLowerCase()))&&(filter==='all'||!app.skip)),[apps,filter,query]);
 const toggle=(app:AppItem)=>{
  const skip=!app.skip;
  onChangeApps(apps.map(item=>item.id===app.id?{...item,skip,whitelist:!skip}:item));
 };
 const tab=(id:Filter,label:string,count:number)=><Pressable hitSlop={{top:5,bottom:5}} onPress={()=>setFilter(id)} style={[s.tab,filter===id&&s.tabOn]}><Text style={[s.tabText,filter===id&&s.tabTextOn]}>{label} {count}</Text></Pressable>;
 return <Background><ScrollView contentContainerStyle={s.page} showsVerticalScrollIndicator={false}>
  <TopBar compact title="排除应用" onBack={onBack}/>
  <View style={s.search}><Text style={s.searchIcon}>⌕</Text><TextInput value={query} onChangeText={setQuery} placeholder="搜索应用" placeholderTextColor="#8195B1" style={s.input}/>{query?<Pressable onPress={()=>setQuery('')} hitSlop={12}><Text style={s.clear}>×</Text></Pressable>:null}</View>
  <View style={s.tabs}>{tab('all','全部',apps.length)}{tab('excluded','已排除',excludedCount)}</View>
  <Card compact style={s.list}>{list.length?list.map((app,index)=><View key={app.id} style={[s.row,index===list.length-1&&{borderBottomWidth:0}]}><AppIcon source={app.icon} size={COMPACT_UI.appIcon}/><Text style={s.name} numberOfLines={1}>{app.name}</Text><Toggle compact value={app.skip} onChange={()=>toggle(app)}/></View>):<Text style={s.empty}>{query?'未找到应用':filter==='excluded'?'没有已排除的应用':'暂无应用'}</Text>}</Card>
 </ScrollView></Background>;
}

const s=StyleSheet.create({
 page:{paddingHorizontal:18,paddingBottom:36},search:{height:48,borderRadius:15,backgroundColor:'#fff',flexDirection:'row',alignItems:'center',paddingHorizontal:13,marginBottom:10},searchIcon:{fontSize:22,color:C.navy},input:{flex:1,fontSize:COMPACT_UI.body,marginLeft:8,color:C.text},clear:{fontSize:22,color:C.text2,paddingHorizontal:4},
 tabs:{flexDirection:'row',gap:7,marginBottom:10},tab:{flex:1,minHeight:37,borderRadius:12,borderWidth:1,borderColor:'#D4E3EE',alignItems:'center',justifyContent:'center',backgroundColor:'rgba(255,255,255,.74)'},tabOn:{backgroundColor:C.blue,borderColor:C.blue},tabText:{fontSize:COMPACT_UI.caption,fontWeight:'800',color:'#5E7698'},tabTextOn:{color:'#fff'},
 list:{paddingHorizontal:10},row:{minHeight:COMPACT_UI.rowHeight,flexDirection:'row',alignItems:'center',gap:COMPACT_UI.gap,borderBottomWidth:1,borderBottomColor:C.line},name:{flex:1,fontSize:COMPACT_UI.body,fontWeight:'800',color:C.navy},empty:{padding:20,textAlign:'center',fontSize:COMPACT_UI.body,fontWeight:'800',color:C.text2},
});

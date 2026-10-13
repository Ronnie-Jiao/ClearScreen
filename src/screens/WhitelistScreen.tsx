import React, { useMemo, useState } from 'react';
import { Pressable, ScrollView, StyleSheet, Text, TextInput, View } from 'react-native';
import { AppItem } from '../data';
import { AppIcon, Background, Card, Toggle, TopBar } from '../components/UI';
import { C } from '../theme';

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
 const tab=(id:Filter,label:string,count:number)=><Pressable onPress={()=>setFilter(id)} style={[s.tab,filter===id&&s.tabOn]}><Text style={[s.tabText,filter===id&&s.tabTextOn]}>{label} {count}</Text></Pressable>;
 return <Background><ScrollView contentContainerStyle={s.page} showsVerticalScrollIndicator={false}>
  <TopBar title="排除应用" onBack={onBack}/>
  <View style={s.search}><Text style={s.searchIcon}>⌕</Text><TextInput value={query} onChangeText={setQuery} placeholder="搜索应用" placeholderTextColor="#8195B1" style={s.input}/>{query?<Pressable onPress={()=>setQuery('')} hitSlop={12}><Text style={s.clear}>×</Text></Pressable>:null}</View>
  <View style={s.tabs}>{tab('all','全部',apps.length)}{tab('excluded','已排除',excludedCount)}</View>
  <Card style={s.list}>{list.length?list.map((app,index)=><View key={app.id} style={[s.row,index===list.length-1&&{borderBottomWidth:0}]}><AppIcon source={app.icon} size={54}/><Text style={s.name} numberOfLines={1}>{app.name}</Text><Toggle value={app.skip} onChange={()=>toggle(app)}/></View>):<Text style={s.empty}>{query?'未找到应用':filter==='excluded'?'没有已排除的应用':'暂无应用'}</Text>}</Card>
 </ScrollView></Background>;
}

const s=StyleSheet.create({
 page:{padding:18,paddingBottom:36},search:{height:60,borderRadius:20,backgroundColor:'#fff',flexDirection:'row',alignItems:'center',paddingHorizontal:18,marginBottom:14},searchIcon:{fontSize:32,color:C.navy},input:{flex:1,fontSize:18,marginLeft:10,color:C.text},clear:{fontSize:26,color:C.text2,paddingHorizontal:4},
 tabs:{flexDirection:'row',gap:10,marginBottom:14},tab:{flex:1,minHeight:48,borderRadius:18,borderWidth:1,borderColor:'#D4E3EE',alignItems:'center',justifyContent:'center',backgroundColor:'rgba(255,255,255,.74)'},tabOn:{backgroundColor:C.blue,borderColor:C.blue},tabText:{fontSize:16,fontWeight:'900',color:'#5E7698'},tabTextOn:{color:'#fff'},
 list:{paddingHorizontal:16},row:{minHeight:88,flexDirection:'row',alignItems:'center',gap:14,borderBottomWidth:1,borderBottomColor:C.line},name:{flex:1,fontSize:19,fontWeight:'900',color:C.navy},empty:{padding:28,textAlign:'center',fontSize:17,fontWeight:'800',color:C.text2},
});

import React from 'react';
import { Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { LinearGradient } from 'expo-linear-gradient';
import { AppItem, LogItem } from '../data';
import { AppIcon, Background, BrandHeader, Card, PrimaryButton } from '../components/UI';
import { BottomNav } from '../components/BottomNav';
import { C } from '../theme';

type MainTab='home'|'apps'|'settings';
type HomePermissions={skip:boolean;skipRunning:boolean};
type HomeStatus='permission'|'checking'|'repair'|'select'|'paused'|'working';

export default function HomeScreen({master,onAction,onTab,onPermissions,onRecords,onOpenRecord,onOpenApp,apps,logs,today,permissions,checking,enabledAppCount}:{
 master:boolean;
 onAction:()=>void;
 onTab:(t:MainTab)=>void;
 onPermissions:()=>void;
 onRecords:()=>void;
 onOpenRecord:(log:LogItem)=>void;
 onOpenApp:(id:string)=>void;
 apps:AppItem[];
 logs:LogItem[];
 today:{skip:number;network:number};
 permissions:HomePermissions;
 checking:boolean;
 enabledAppCount:number;
}){
 const recent=logs.slice(0,3);
 let state:HomeStatus;
 if(!permissions.skip)state='permission';
 else if(checking)state='checking';
 else if(!permissions.skipRunning)state='repair';
 else if(enabledAppCount===0)state='select';
 else if(!master)state='paused';
 else state='working';
 const statusTitle:Record<HomeStatus,string>={permission:'需要开启服务',checking:'正在检查',repair:'需要修复',select:'还没选择应用',paused:'已暂停',working:'工作中'};
 const actionTitle:Partial<Record<HomeStatus,string>>={permission:'去开启',repair:'去修复',select:'选择应用',paused:'开启净屏',working:'暂停净屏'};
 const action=actionTitle[state];
 const primaryAction=state==='permission'||state==='repair'?onPermissions:onAction;

 return <Background><View style={s.root}><ScrollView showsVerticalScrollIndicator={false} contentContainerStyle={s.scroll}>
  <BrandHeader/>
  <Card style={s.status}>
   <LinearGradient colors={state==='working'?['#16A9F0','#11D3C0']:['#AFC1D9','#C6D1DF']} style={s.orb}><Text style={s.check}>{state==='working'?'✓':state==='paused'?'Ⅱ':'!'}</Text></LinearGradient>
   <Text style={s.statusTitle}>{statusTitle[state]}</Text>
   {action?<View style={s.action}><PrimaryButton title={action} onPress={primaryAction}/></View>:null}
  </Card>
  <Pressable onPress={onRecords}>
   <Card style={s.stat}><View><Text style={s.statLabel}>今日处理</Text><View style={s.numRow}><Text style={s.num}>{today.skip}</Text><Text style={s.unit}>次</Text></View></View><Text style={s.link}>›</Text></Card>
  </Pressable>
  <Card style={s.block}>
   <View style={s.blockHead}><Text style={s.blockTitle}>最近记录</Text><Pressable onPress={onRecords}><Text style={s.linkText}>查看全部 ›</Text></Pressable></View>
   {recent.length?recent.map((log,index)=>{
    const app=apps.find(item=>item.id===log.appId);
    if(!app)return null;
    const result=log.type==='fail'?'处理失败':log.result==='success'?'已执行跳过':'处理记录';
    return <Pressable key={`${log.appId}-${log.timestamp||index}`} onPress={()=>onOpenRecord(log)} style={s.logRow}>
     <Text style={s.time}>{log.time}</Text><AppIcon source={app.icon} size={40}/><Text style={s.logName} numberOfLines={1}>{app.name}</Text>
     <Text style={[s.logAction,log.type==='fail'&&{color:C.red}]} numberOfLines={1}>{result}</Text>
    </Pressable>;
   }):<Text style={s.empty}>还没有处理记录</Text>}
  </Card>
 </ScrollView><BottomNav active="home" onChange={onTab}/></View></Background>;
}

const s=StyleSheet.create({
 root:{flex:1},scroll:{padding:18,paddingBottom:112},
 status:{padding:18,gap:14,marginBottom:12},orb:{width:72,height:72,borderRadius:36,alignItems:'center',justifyContent:'center'},check:{fontSize:40,color:'#fff',fontWeight:'900'},
 statusTitle:{fontSize:26,fontWeight:'900',color:C.navy},action:{marginTop:2},
 stat:{padding:16,marginBottom:12,flexDirection:'row',alignItems:'center',justifyContent:'space-between'},statLabel:{fontSize:16,fontWeight:'900',color:C.text},numRow:{flexDirection:'row',alignItems:'baseline',marginTop:3},num:{fontSize:36,fontWeight:'900',color:C.navy},unit:{fontSize:16,fontWeight:'800',color:C.text2,marginLeft:4},link:{fontSize:30,fontWeight:'800',color:C.blue},
 block:{paddingHorizontal:14,marginBottom:12},blockHead:{minHeight:54,flexDirection:'row',alignItems:'center',borderBottomWidth:1,borderBottomColor:C.line},blockTitle:{fontSize:19,fontWeight:'900',color:C.navy,flex:1},linkText:{fontSize:15,fontWeight:'900',color:C.blue},
 logRow:{minHeight:64,flexDirection:'row',alignItems:'center',gap:8,borderBottomWidth:1,borderBottomColor:C.line},time:{width:44,fontSize:14,color:C.text2,fontWeight:'700'},logName:{width:72,fontSize:15,fontWeight:'900',color:C.text},logAction:{flex:1,fontSize:14,fontWeight:'900',color:C.green},empty:{paddingVertical:20,textAlign:'center',color:C.text2,fontSize:16,fontWeight:'700'},
});

import React from 'react';
import { Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { LinearGradient } from 'expo-linear-gradient';
import { Ionicons } from '@expo/vector-icons';
import { AppItem, LogItem, LearnedCloseRecord } from '../data';
import { AppIcon, Background, BrandHeader, Card, PrimaryButton } from '../components/UI';
import { BottomNav, useBottomNavMetrics } from '../components/BottomNav';
import { C } from '../theme';
import { LearnedCloseRow } from './LearnedCloseRecords';

type MainTab='home'|'apps'|'settings';
type HomePermissions={skip:boolean;skipRunning:boolean;accessibilityHealth?:'ready'|'unavailable'|'unknown'};
type HomeStatus='permission'|'checking'|'unknown'|'repair'|'select'|'paused'|'working';

const RECENT_RECORD_LIMIT=3;
const RECENT_ROW_HEIGHT=60;
const RECENT_HEADER_HEIGHT=48;

export default function HomeScreen({master,onAction,onTab,onPermissions,onRecords,onTodayRecords,onOpenRecord,onOpenApp,apps,logs,today,permissions,checking,enabledAppCount,onLearn,learning=false,learnedRecords=[],onLearnedRecords,onDeleteLearned}:{
 master:boolean;
 onAction:()=>void;
 onTab:(t:MainTab)=>void;
 onPermissions:()=>void;
 onRecords:()=>void;
 onTodayRecords?:()=>void;
 onOpenRecord:(log:LogItem)=>void;
 onOpenApp:(id:string)=>void;
 apps:AppItem[];
 logs:LogItem[];
 today:{skip:number;network:number};
 permissions:HomePermissions;
 checking:boolean;
 enabledAppCount:number;
 onLearn?:()=>void;
 learning?:boolean;
 learnedRecords?:LearnedCloseRecord[];
 onLearnedRecords?:()=>void;
 onDeleteLearned?:(record:LearnedCloseRecord)=>void;
}){
 const recent=logs.slice(0,RECENT_RECORD_LIMIT);
 const {inset:navInset}=useBottomNavMetrics();
 let state:HomeStatus;
 if(checking)state='checking';
 else if(!permissions.skip)state='permission';
 else if(permissions.accessibilityHealth==='unavailable')state='repair';
 else if(permissions.accessibilityHealth!=='ready')state='unknown';
 else if(!permissions.skipRunning)state='repair';
 else if(enabledAppCount===0)state='select';
 else if(!master)state='paused';
 else state='working';
 const statusTitle:Record<HomeStatus,string>={permission:'需要开启服务',checking:'正在检查',unknown:'状态待确认',repair:'需要修复',select:'还没选择应用',paused:'已暂停',working:'工作中'};
 const statusIcon:Record<HomeStatus,keyof typeof Ionicons.glyphMap>={permission:'accessibility-outline',checking:'sync-outline',unknown:'help-outline',repair:'warning-outline',select:'grid-outline',paused:'pause-outline',working:'checkmark'};
 const actionTitle:Partial<Record<HomeStatus,string>>={permission:'去开启',unknown:'检查服务',repair:'去修复',select:'选择应用',paused:'开启净屏',working:'暂停净屏'};
 const action=actionTitle[state];
 const primaryAction=state==='permission'||state==='repair'||state==='unknown'?onPermissions:onAction;

 return <Background><View style={s.root}><ScrollView showsVerticalScrollIndicator={false} contentContainerStyle={[s.scroll,{paddingBottom:84+navInset}]}>
  <BrandHeader tagline smallText/>
  <Card style={s.status}>
   <View style={s.statusHeader}>
    <LinearGradient colors={state==='repair'||state==='unknown'?['#F59E0B','#F9735B']:state==='paused'?['#8B9BB0','#B6C3D2']:['#0B78F2','#14B8B0']} style={s.orb}>
     <Ionicons name={statusIcon[state]} size={36} color="#FFFFFF"/>
    </LinearGradient>
    <Text style={s.statusTitle} numberOfLines={1}>{statusTitle[state]}</Text>
    <Pressable accessibilityRole="button" accessibilityLabel={`今日处理${today.skip}次，查看记录`} onPress={onTodayRecords||onRecords} style={s.todaySummary}>
     <Text style={s.todayLabel}>今日处理</Text>
     <View style={s.todayValueRow}><Text style={s.todayNum}>{today.skip}</Text><Text style={s.todayUnit}>次</Text><Ionicons name="chevron-forward" size={16} color={C.blue}/></View>
    </Pressable>
    {onLearn?<Pressable accessibilityRole="button" accessibilityLabel={learning?'取消学习关闭':'学习关闭'} onPress={onLearn} style={[s.learnButton,learning&&s.learnActive]}>
     <Ionicons name={learning?'stop-circle-outline':'scan-outline'} size={22} color={C.blue}/>
     <Text style={s.learnLabel}>{learning?'学习中':'学习关闭'}</Text>
    </Pressable>:null}
   </View>
   {action?<PrimaryButton title={action} onPress={primaryAction}/>:null}
  </Card>
  <Card style={[s.block,s.recentBlock]}>
   <View style={s.blockHead}><Text style={s.blockTitle}>最近记录</Text><Pressable accessibilityRole="button" accessibilityLabel="查看更多处理记录" onPress={onRecords} style={s.viewAllButton}><Text style={s.linkText}>更多</Text><Ionicons name="chevron-forward" size={16} color={C.blue}/></Pressable></View>
   {recent.length?recent.map((log,index)=>{
    const app=apps.find(item=>item.id===log.appId);
    const result=log.type==='fail'?'处理失败':log.result==='success'?'已执行跳过':'处理记录';
    return <Pressable key={`${log.appId}-${log.timestamp||index}`} onPress={()=>onOpenRecord(log)} style={s.logRow}>
     <Text style={s.time}>{log.time}</Text>{app?<AppIcon source={app.icon} size={40}/>:<View style={s.fallbackIcon}><Ionicons name="apps-outline" size={21} color={C.blue}/></View>}<Text style={s.logName} numberOfLines={1}>{app?.name||log.appId||'未知应用'}</Text>
     <Text style={[s.logAction,log.type==='fail'&&{color:C.red}]} numberOfLines={1}>{result}</Text>
    </Pressable>;
   }):<Text style={s.empty}>还没有处理记录</Text>}
  </Card>
  {onLearnedRecords&&onDeleteLearned?<Card style={[s.block,s.recentBlock]}>
   <View style={s.blockHead}><Text style={s.blockTitle}>自定义关闭窗口</Text><Pressable accessibilityRole="button" accessibilityLabel="查看更多自定义关闭记录" onPress={onLearnedRecords} style={s.viewAllButton}><Text style={s.linkText}>更多</Text><Ionicons name="chevron-forward" size={16} color={C.blue}/></Pressable></View>
   {learnedRecords.length?learnedRecords.slice(0,3).map(record=><LearnedCloseRow key={record.id} record={record} apps={apps} onDelete={onDeleteLearned}/>):<Text style={s.empty}>暂无自定义关闭记录</Text>}
  </Card>:null}
 </ScrollView><BottomNav active="home" onChange={onTab}/></View></Background>;
}

const s=StyleSheet.create({
 root:{flex:1},scroll:{flexGrow:1,paddingHorizontal:18,paddingBottom:84},
 status:{padding:18,gap:16,marginBottom:14},statusHeader:{minHeight:76,flexDirection:'row',alignItems:'center',gap:10},orb:{width:64,height:64,borderRadius:32,alignItems:'center',justifyContent:'center'},
 statusTitle:{fontSize:20,fontWeight:'900',color:C.navy,flex:1,minWidth:0},todaySummary:{minWidth:64,minHeight:76,alignItems:'flex-end',justifyContent:'center'},todayLabel:{position:'absolute',top:8,right:0,fontSize:12,fontWeight:'800',color:C.text2},todayValueRow:{flexDirection:'row',alignItems:'center'},todayNum:{fontSize:26,fontWeight:'900',color:C.navy},todayUnit:{fontSize:12,fontWeight:'800',color:C.text2,marginLeft:3},
 learnButton:{width:52,minHeight:64,alignItems:'center',justifyContent:'center',gap:4,borderRadius:12},learnActive:{backgroundColor:'#E4F4FF'},learnLabel:{fontSize:11,lineHeight:16,color:C.blue,fontWeight:'800'},
 block:{paddingHorizontal:16,marginBottom:14},recentBlock:{minHeight:RECENT_HEADER_HEIGHT+RECENT_ROW_HEIGHT*RECENT_RECORD_LIMIT+2,flexShrink:0},blockHead:{minHeight:RECENT_HEADER_HEIGHT,flexShrink:0,flexDirection:'row',alignItems:'center',borderBottomWidth:1,borderBottomColor:C.line},blockTitle:{fontSize:19,fontWeight:'900',color:C.navy,flex:1},viewAllButton:{height:44,flexDirection:'row',alignItems:'center',justifyContent:'flex-end',gap:2},linkText:{fontSize:14,lineHeight:20,fontWeight:'900',color:C.blue},
 fallbackIcon:{width:40,height:40,borderRadius:10,alignItems:'center',justifyContent:'center',backgroundColor:'#EAF4FF'},
 logRow:{height:RECENT_ROW_HEIGHT,flexShrink:0,flexDirection:'row',alignItems:'center',gap:9,borderBottomWidth:1,borderBottomColor:C.line},time:{width:46,fontSize:13,color:C.text2,fontWeight:'700'},logName:{flex:1,minWidth:0,fontSize:15,fontWeight:'800',color:C.text},logAction:{width:94,textAlign:'right',fontSize:13,fontWeight:'800',color:C.green},empty:{paddingVertical:22,textAlign:'center',color:C.text2,fontSize:16,fontWeight:'700'},
});

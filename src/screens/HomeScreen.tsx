import React from 'react';
import { Image, ImageSourcePropType, Pressable, ScrollView, StyleSheet, Text, useWindowDimensions, View } from 'react-native';
import { LinearGradient } from 'expo-linear-gradient';
import { Ionicons } from '@expo/vector-icons';
import { AppItem, LogItem } from '../data';
import { AppIcon, Background, Card } from '../components/UI';
import { BottomNav, useBottomNavMetrics } from '../components/BottomNav';
import { C } from '../theme';
import { LOGO, SPLASH_BACKGROUND, FONT_TAGLINE, HOME_PAUSE, HOME_PLAY, HOME_HISTORY, HOME_SCAN, HOME_REFERENCE_SHIELD_WORKING, HOME_REFERENCE_SHIELD_PAUSED } from '../assets';

type MainTab='home'|'apps'|'settings';
type HomePermissions={skip:boolean;skipRunning:boolean;accessibilityHealth?:'ready'|'unavailable'|'unknown'};
type HomeStatus='permission'|'checking'|'unknown'|'select'|'paused'|'working';

const RECENT_RECORD_LIMIT=3;
const RECENT_ROW_HEIGHT=62;
const RECENT_HEADER_HEIGHT=52;

type StatusTone={orb:[string,string];ring:string};
const STATUS_TONE:Record<HomeStatus,StatusTone>={
 permission:{orb:['#0B78F2','#14B8B0'],ring:'#CFE8FF'},
 checking:{orb:['#0B78F2','#14B8B0'],ring:'#CFE8FF'},
 unknown:{orb:['#F59E0B','#F9735B'],ring:'#FFE0BE'},
 select:{orb:['#0B78F2','#14B8B0'],ring:'#CFE8FF'},
 paused:{orb:['#8B9BB0','#B6C3D2'],ring:'#DDE5EF'},
 working:{orb:['#0B78F2','#14B8B0'],ring:'#C9E6FF'},
};

/** Home-only brand row with a single-line tagline, matching the supplied design. */
function HomeBrandHeader(){
 return <View style={s.brandRow}>
  <Image source={LOGO} resizeMode="contain" style={s.brandLogo}/>
  <View style={s.brandText}>
   <Text style={s.brandTitle}>净屏</Text>
   <Text style={s.brandEnglish}>Clear<Text style={{color:C.teal}}>Screen</Text></Text>
  </View>
  <Text style={s.brandTagline} numberOfLines={1} adjustsFontSizeToFit minimumFontScale={0.75}>净化屏幕 畅享生活</Text>
 </View>;
}

export default function HomeScreen({master,onAction,onTab,onEnableAccessibility,onRecords,onOpenRecord,onOpenApp,apps,logs,permissions,checking,enabledAppCount,onLearn,learning=false}:{
 master:boolean;
 onAction:()=>void;
 onTab:(t:MainTab)=>void;
 onEnableAccessibility:()=>void;
 onRecords:()=>void;
 onOpenRecord:(log:LogItem)=>void;
 onOpenApp:(id:string)=>void;
 apps:AppItem[];
 logs:LogItem[];
 permissions:HomePermissions;
 checking:boolean;
 enabledAppCount:number;
 onLearn?:()=>void;
 learning?:boolean;
}){
 const recent=logs.slice(0,RECENT_RECORD_LIMIT);
 const {inset:navInset}=useBottomNavMetrics();
 const {width}=useWindowDimensions();
 const statusArtSize=width<350?84:width<390?94:104;
 let state:HomeStatus;
 if(checking)state='checking';
 else if(!permissions.skip||permissions.accessibilityHealth==='unavailable')state='permission';
 else if(permissions.accessibilityHealth!=='ready')state='unknown';
 else if(!permissions.skipRunning)state='permission';
 else if(enabledAppCount===0)state='select';
 else if(!master)state='paused';
 else state='working';
 const statusTitle:Record<HomeStatus,string>={permission:'需要开启服务',checking:'正在检查',unknown:'状态待确认',select:'还没选择应用',paused:'已暂停',working:'工作中'};
 const statusIcon:Record<HomeStatus,keyof typeof Ionicons.glyphMap>={permission:'accessibility-outline',checking:'sync-outline',unknown:'help-outline',select:'grid-outline',paused:'pause-outline',working:'checkmark'};
 const actionTitle:Partial<Record<HomeStatus,string>>={permission:'去开启',unknown:'检查服务',select:'选择应用',paused:'开启净屏',working:'暂停净屏'};
 const action=actionTitle[state];
 const primaryAction=state==='permission'||state==='unknown'?onEnableAccessibility:onAction;
 const tone=STATUS_TONE[state];

 return <Background backgroundSource={SPLASH_BACKGROUND}><View style={s.root}><ScrollView showsVerticalScrollIndicator={false} contentContainerStyle={[s.scroll,{paddingBottom:84+navInset}]}>
  <HomeBrandHeader/>
  <Card style={s.status}>
   <View style={s.statusPanel}>
    <View style={[s.statusArtSlot,{width:statusArtSize,height:statusArtSize}]}>
     {state==='working'||state==='paused'?<Image source={state==='working'?HOME_REFERENCE_SHIELD_WORKING:HOME_REFERENCE_SHIELD_PAUSED} resizeMode="contain" style={s.shieldImage}/>:<View style={[s.orbHalo,{borderColor:tone.ring}]}>
      <LinearGradient colors={tone.orb} start={{x:0,y:0}} end={{x:1,y:1}} style={s.orb}>
       <Ionicons name={statusIcon[state]} size={28} color="#FFFFFF"/>
      </LinearGradient>
     </View>}
    </View>
    <View style={s.statusText}>
     <Text style={s.statusTitle} numberOfLines={1} adjustsFontSizeToFit minimumFontScale={0.65}>{statusTitle[state]}</Text>
     {state==='working'?<Text style={s.statusHint} numberOfLines={1} adjustsFontSizeToFit minimumFontScale={0.75}>自动关闭广告与弹窗</Text>:null}
    </View>
    {onLearn?<Pressable accessibilityRole="button" accessibilityLabel={learning?'取消学习关闭':'学习关闭'} onPress={onLearn} style={({pressed})=>[s.learnButton,learning&&s.learnActive,pressed&&s.learnPressed]}>
      {learning?<Ionicons name="stop-circle-outline" size={24} color={C.blue}/>:<Image source={HOME_SCAN} resizeMode="contain" style={s.scanIcon}/>}
      <Text style={s.learnLabel}>{learning?'学习中':'学习关闭'}</Text>
    </Pressable>:null}
   </View>
   {action?<StatusActionButton title={action} icon={state==='working'?'pause':state==='paused'?'play':'chevron-forward'} art={state==='working'?HOME_PAUSE:state==='paused'?HOME_PLAY:undefined} onPress={primaryAction}/>:null}
  </Card>
  <Card style={[s.block,s.recentBlock]}>
   <View style={s.blockHead}><Image source={HOME_HISTORY} resizeMode="contain" style={s.blockIcon}/><Text style={s.blockTitle}>最近记录</Text><Pressable accessibilityRole="button" accessibilityLabel="查看更多处理记录" onPress={onRecords} style={s.viewAllButton}><Text style={s.linkText}>更多</Text><Ionicons name="chevron-forward" size={16} color={C.blue}/></Pressable></View>
   {recent.length?recent.map((log,index)=>{
    const app=apps.find(item=>item.id===log.appId);
    const result=log.type==='fail'?'处理失败':log.result==='success'?'已执行跳过':'处理记录';
    return <Pressable key={`${log.appId}-${log.timestamp||index}`} onPress={()=>onOpenRecord(log)} style={[s.logRow,index===recent.length-1&&s.lastRow]}>
     <Text style={s.time}>{log.time}</Text>{app?<AppIcon source={app.icon} size={40}/>:<View style={s.fallbackIcon}><Ionicons name="apps-outline" size={21} color={C.blue}/></View>}<Text style={s.logName} numberOfLines={1}>{app?.name||log.appId||'未知应用'}</Text>
     <Text style={[s.logAction,log.type==='fail'&&{color:C.red}]} numberOfLines={1}>{result}</Text>
    </Pressable>;
   }):<Text style={s.empty}>还没有处理记录</Text>}
  </Card>
 </ScrollView><BottomNav active="home" onChange={onTab}/></View></Background>;
}

const s=StyleSheet.create({
 root:{flex:1},scroll:{flexGrow:1,paddingHorizontal:16,paddingBottom:84},
 brandRow:{flexDirection:'row',alignItems:'center',gap:10,marginTop:4,marginBottom:16,minHeight:64},
 brandLogo:{width:56,height:56},brandText:{flex:1,minWidth:0},
 brandTitle:{fontSize:27,fontWeight:'900',color:C.navy,letterSpacing:-.6,lineHeight:32},
 brandEnglish:{fontSize:14,fontWeight:'900',color:C.blue,lineHeight:18},
 brandTagline:{fontFamily:FONT_TAGLINE,fontSize:19,lineHeight:27,color:C.blue,letterSpacing:.5},
 status:{padding:14,gap:12,marginBottom:16,borderRadius:28,backgroundColor:'rgba(241,249,255,0.94)'},
 statusPanel:{minHeight:112,paddingVertical:4,flexDirection:'row',alignItems:'center',gap:7},
 statusArtSlot:{flexShrink:0,alignItems:'center',justifyContent:'center'},shieldImage:{width:'100%',height:'100%',transform:[{scale:1.2}]},
 orbHalo:{width:64,height:64,borderRadius:32,borderWidth:1,alignItems:'center',justifyContent:'center',backgroundColor:'rgba(255,255,255,0.70)',flexShrink:0},
 orb:{width:52,height:52,borderRadius:26,alignItems:'center',justifyContent:'center',shadowColor:'#0B78F2',shadowOpacity:.30,shadowRadius:10,shadowOffset:{width:0,height:5},elevation:6},
 statusText:{flex:1,minWidth:0,gap:2},
 statusTitle:{fontSize:23,fontWeight:'900',color:C.navy,letterSpacing:-.3},
 statusHint:{fontSize:12,lineHeight:16,color:C.text2,fontWeight:'600'},
 learnButton:{width:78,height:68,flexShrink:0,marginRight:6,alignItems:'center',justifyContent:'center',gap:3,borderRadius:16,backgroundColor:'#FFFFFF',borderWidth:1,borderColor:'rgba(11,120,242,0.14)',shadowColor:'#4A7EA2',shadowOpacity:.10,shadowRadius:8,shadowOffset:{width:0,height:3},elevation:2},
 scanIcon:{width:22,height:22},
 learnActive:{backgroundColor:'#DFF0FF',borderColor:'rgba(11,120,242,0.35)'},learnPressed:{opacity:.7,transform:[{scale:.96}]},
 learnLabel:{fontSize:11,lineHeight:15,color:C.blue,fontWeight:'900'},
 actionButton:{alignSelf:'stretch',borderRadius:29,backgroundColor:'#0B78F2',shadowColor:'#0B78F2',shadowOpacity:.26,shadowRadius:12,shadowOffset:{width:0,height:6},elevation:5},
 actionPressed:{opacity:.92,transform:[{scale:.985}]},
 actionGradient:{height:58,borderRadius:29,flexDirection:'row',alignItems:'center',justifyContent:'center'},
 actionContent:{flexDirection:'row',alignItems:'center',gap:8},
 actionIcon:{width:32,height:32,borderRadius:16,backgroundColor:'rgba(255,255,255,0.20)',alignItems:'center',justifyContent:'center'},
 actionArt:{width:36,height:36},
 actionText:{color:'#FFFFFF',fontSize:20,fontWeight:'900',letterSpacing:.3},
 block:{paddingHorizontal:16,marginBottom:14,borderRadius:22,backgroundColor:'rgba(250,253,255,0.95)'},recentBlock:{minHeight:RECENT_HEADER_HEIGHT+RECENT_ROW_HEIGHT*RECENT_RECORD_LIMIT+2,flexShrink:0},blockHead:{minHeight:RECENT_HEADER_HEIGHT,flexShrink:0,flexDirection:'row',alignItems:'center',borderBottomWidth:1,borderBottomColor:C.line},blockIcon:{width:20,height:20,marginRight:9},blockTitle:{fontSize:19,fontWeight:'900',color:C.navy,flex:1},viewAllButton:{height:44,flexDirection:'row',alignItems:'center',justifyContent:'flex-end',gap:2},linkText:{fontSize:14,lineHeight:20,fontWeight:'900',color:C.blue},
 fallbackIcon:{width:40,height:40,borderRadius:10,alignItems:'center',justifyContent:'center',backgroundColor:'#EAF4FF'},
 logRow:{height:RECENT_ROW_HEIGHT,flexShrink:0,flexDirection:'row',alignItems:'center',gap:9,borderBottomWidth:1,borderBottomColor:C.line},lastRow:{borderBottomWidth:0},time:{width:46,fontSize:13,color:C.text2,fontWeight:'700'},logName:{flex:1,minWidth:0,fontSize:15,fontWeight:'800',color:C.text},logAction:{width:94,textAlign:'right',fontSize:13,fontWeight:'800',color:C.green},empty:{paddingVertical:22,textAlign:'center',color:C.text2,fontSize:16,fontWeight:'700'},
});

function StatusActionButton({title,icon,art,onPress}:{title:string;icon:keyof typeof Ionicons.glyphMap;art?:ImageSourcePropType;onPress:()=>void}){
 return <Pressable accessibilityRole="button" accessibilityLabel={title} onPress={onPress} style={({pressed})=>[s.actionButton,pressed&&s.actionPressed]}>
  <LinearGradient colors={['#0B78F2','#12C2C8']} start={{x:0,y:0}} end={{x:1,y:0}} style={s.actionGradient}>
   <View style={s.actionContent}>
    {art?<Image source={art} resizeMode="contain" style={s.actionArt}/>:<View style={s.actionIcon}><Ionicons name={icon} size={18} color="#FFFFFF"/></View>}
    <Text style={s.actionText}>{title}</Text>
   </View>
  </LinearGradient>
 </Pressable>;
}

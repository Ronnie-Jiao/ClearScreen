import React, { useState } from 'react';
import {
  ActivityIndicator,
  Alert,
  Linking,
  Modal,
  Platform,
  Pressable,
  ScrollView,
  Share,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import { Background, BrandHeader, Card, Chevron } from '../components/UI';
import { BottomNav } from '../components/BottomNav';
import type { LogItem } from '../data';
import type { BackendPermissions } from '../native/ClearScreenNative';
import { APP_FILING_NUMBER, APP_FILING_QUERY_URL, APP_SUPPORT_EMAIL } from '../privacy';
import { C } from '../theme';

type MainTab='home'|'apps'|'settings';
type HelpItem={title:string;body:string};
type UpdateState='checking'|'unavailable'|'failed'|'latest'|'newer'|'ahead'|'unknown';
type GitHubRelease={tag_name?:string;name?:string;body?:string;html_url?:string};

const RETENTION_OPTIONS:[number,string][]=[[0,'关闭记录'],[1,'1 天'],[7,'7 天'],[30,'30 天']];
const UPDATE_URL='https://api.github.com/repos/Ronnie-Jiao/ClearScreen/releases/latest';
const FEEDBACK_EMAIL=APP_SUPPORT_EMAIL;

const ANDROID_HELP:HelpItem[]=[
 {title:'自动跳过服务',body:'在“设置 > 服务与权限”中打开自动跳过服务，按系统提示启用“净屏”无障碍服务后返回。'},
 {title:'广告没有被处理',body:'请确认总开关、该应用和无障碍服务均已开启。净屏只处理能明确识别且有安全关闭控件的广告；强制观看或结构不支持的广告不会跳过。'},
 {title:'后台运行',body:'在“设置 > 服务与权限”中打开后台运行，并按系统页面允许净屏自启动和后台运行。'},
];
const IOS_HELP:HelpItem[]=[
 {title:'开启 Safari 扩展',body:'在 iPhone 设置中启用净屏扩展，再从 Safari 扩展菜单打开过滤，并允许访问需要处理的网站。'},
 {title:'网页广告没有被处理',body:'净屏只处理同一广告区域内明确标记的跳过或关闭控件。图片广告、嵌入框架或网页结构不兼容时可能无法处理。'},
 {title:'关闭网页处理',body:'在 Safari 扩展菜单关闭过滤，或在 iPhone 设置中停用净屏扩展或撤销网站访问权限。'},
];
const OTHER_PLATFORM_HELP:HelpItem[]=[
 {title:'功能状态',body:'当前平台的自动处理能力尚未接入。'},
];

function compareVersions(current:string,latest:string):number|undefined{
 const parse=(value:string)=>{
  const match=value.trim().replace(/^v/i,'').match(/^(\d+)\.(\d+)\.(\d+)/);
  return match?[Number(match[1]),Number(match[2]),Number(match[3])]:undefined;
 };
 const left=parse(current),right=parse(latest);
 if(!left||!right)return undefined;
 for(let index=0;index<3;index++)if(left[index]!==right[index])return left[index]-right[index];
 return 0;
}

function displayTime(log:LogItem):string{
 return typeof log.timestamp==='number'&&Number.isFinite(log.timestamp)
  ?new Date(log.timestamp).toLocaleString('zh-CN')
  :log.time||'时间未知';
}

export default function SettingsScreen({logRetentionDays,versionName,logs=[],permissions,onTab,onServicePermissions,onClear,onRetentionChange,onPrivacyPolicy,onRestoreDefaults}: {
 logRetentionDays:number;
 versionName:string;
 logs?:LogItem[];
 permissions?:Pick<BackendPermissions,'skip'|'skipRunning'|'bg'>;
 onTab:(tab:MainTab)=>void;
 onServicePermissions:()=>void;
 onClear:()=>void;
 onRetentionChange:(days:number)=>void;
 onPrivacyPolicy:()=>void;
 onRestoreDefaults?:()=>Promise<void>;
}){
 const [retentionOpen,setRetentionOpen]=useState(false);
 const [helpOpen,setHelpOpen]=useState(false);
 const [updateOpen,setUpdateOpen]=useState(false);
 const [updateState,setUpdateState]=useState<UpdateState>('checking');
 const [release,setRelease]=useState<GitHubRelease|null>(null);
 const row=(title:string,value?:string,onPress?:()=>void,last=false)=><Pressable accessibilityRole={onPress?'button':undefined} onPress={onPress} style={[s.row,last&&{borderBottomWidth:0}]}><Text style={s.rowTitle}>{title}</Text>{value?<Text style={s.value}>{value}</Text>:null}{onPress?<Chevron/>:null}</Pressable>;
 const helpItems=Platform.OS==='android'?ANDROID_HELP:Platform.OS==='ios'?IOS_HELP:OTHER_PLATFORM_HELP;

 const sendFeedback=()=>{
  const subject=encodeURIComponent('净屏意见反馈');
  const body=encodeURIComponent(`请描述遇到的问题：\n\n净屏版本：${versionName}\n\n`);
  void Linking.openURL(`mailto:${FEEDBACK_EMAIL}?subject=${subject}&body=${body}`).catch(()=>{
   Alert.alert('无法打开邮件',`请发送邮件至 ${FEEDBACK_EMAIL}`);
  });
 };

 const checkUpdates=async()=>{
  setRelease(null);
  setUpdateState('checking');
  setUpdateOpen(true);
  const controller=new AbortController();
  const timeout=setTimeout(()=>controller.abort(),10000);
  try{
   const response=await fetch(UPDATE_URL,{headers:{Accept:'application/vnd.github+json'},signal:controller.signal});
   if(response.status===404){setUpdateState('unavailable');return;}
   if(!response.ok)throw new Error(`Update request failed: ${response.status}`);
   const result=await response.json() as GitHubRelease;
   if(!result||typeof result.tag_name!=='string')throw new Error('Release version is missing');
   setRelease(result);
   const latest=result.tag_name||result.name||'';
   const comparison=compareVersions(versionName,latest);
   setUpdateState(comparison===undefined?'unknown':comparison<0?'newer':comparison>0?'ahead':'latest');
  }catch{
   setUpdateState('failed');
  }finally{
   clearTimeout(timeout);
  }
 };

 const updateTitle=updateState==='checking'?'检查更新':updateState==='newer'?'发现新版本':updateState==='latest'?'当前已是最新版本':updateState==='ahead'?'当前版本较新':updateState==='unknown'?'版本信息':updateState==='unavailable'?'暂无版本信息':'检查失败';
 const updateMessage=updateState==='checking'?'正在查询公开发布信息。':updateState==='unavailable'?'暂时没有可用的公开版本信息。':updateState==='failed'?'暂时无法获取版本信息，请检查网络后重试。':updateState==='newer'?`当前版本：${versionName}\n最新版本：${release?.tag_name||release?.name||'未知'}`:updateState==='latest'?`当前版本：${versionName}\n最新版本：${release?.tag_name||release?.name||'未知'}`:updateState==='ahead'?`当前版本：${versionName}\n最新公开版本：${release?.tag_name||release?.name||'未知'}`:`当前版本：${versionName}\n最新版本：${release?.tag_name||release?.name||'未知'}\n无法自动比较版本号。`;
 const openReleasePage=()=>{
  const url=release?.html_url;
  if(!url||!url.startsWith('https://github.com/Ronnie-Jiao/ClearScreen/releases'))return;
  void Linking.openURL(url).catch(()=>Alert.alert('无法打开发布页','请稍后重试。'));
 };
 const openAppFiling=()=>{
  void Linking.openURL(APP_FILING_QUERY_URL).catch(()=>Alert.alert('无法打开备案查询','请稍后重试。'));
 };

 const exportDiagnostics=async()=>{
  const permissionState=permissions||{skip:false,skipRunning:false,bg:false};
  const aliases=new Map<string,string>();
  const anonymize=(appId:string)=>{
   if(!aliases.has(appId))aliases.set(appId,`应用 ${aliases.size+1}`);
   return aliases.get(appId)!;
  };
  const reportLogs=logs.filter(log=>log.type!=='network').slice(0,50).map(log=>{
   const type=log.type==='skip'?'处理成功':log.type==='fail'?'处理失败':'处理';
   const result=log.result==='success'?'成功':log.result==='failed'?'失败':'未标记';
   const ruleId=log.ruleId&&/^[\w.-]{1,80}$/.test(log.ruleId)?` · 规则 ${log.ruleId}`:'';
   return `- ${displayTime(log)} · ${anonymize(log.appId)} · ${type} · ${result}${ruleId}`;
  });
  const report=[
   '净屏诊断报告',
   `版本：${versionName}`,
   `无障碍授权：${permissionState.skip?'已开启':'未开启'}`,
   `服务连接：${permissionState.skipRunning?'已连接':'未连接'}`,
   `后台运行：${permissionState.bg?'已允许':'未允许或未检测到'}`,
   `本机记录：${reportLogs.length} 条（最多导出 50 条）`,
   '记录中的应用标识已匿名化，未包含应用包名、界面文字或屏幕内容。',
   ...reportLogs,
  ].join('\n');
  try{await Share.share({title:'净屏诊断报告',message:report});}
  catch{Alert.alert('导出失败','无法打开系统分享面板，请稍后重试。');}
 };

 const confirmRestoreDefaults=()=>{
  if(!onRestoreDefaults)return;
  Alert.alert('恢复默认设置？','总开关将关闭，自动处理选项将恢复默认。应用排除规则、处理记录和记录保留周期会保留。',[
   {text:'取消',style:'cancel'},
   {text:'恢复默认',style:'destructive',onPress:()=>{
    void onRestoreDefaults().then(()=>Alert.alert('设置已恢复')).catch(()=>Alert.alert('恢复失败','请稍后重试。'));
   }},
  ]);
 };

 return <Background><View style={s.root}><ScrollView showsVerticalScrollIndicator={false} contentContainerStyle={s.scroll}>
  <BrandHeader smallText/>
  <Card style={s.group}>{row('服务与权限',undefined,onServicePermissions,true)}</Card>
  <Text style={s.groupTitle}>隐私与数据</Text>
  <Card style={s.group}>
   {row('记录保留周期',RETENTION_OPTIONS.find(([days])=>days===logRetentionDays)?.[1]||'7 天',()=>setRetentionOpen(true))}
   {row('清空记录',undefined,onClear)}
   {row('隐私政策',undefined,onPrivacyPolicy,true)}
  </Card>
  <Text style={s.groupTitle}>关于</Text>
  <Card style={s.group}>
   {row('意见反馈',undefined,sendFeedback)}
   {row('使用帮助 / 常见问题',undefined,()=>setHelpOpen(true))}
   {row('检查更新',undefined,()=>{void checkUpdates()})}
   {Platform.OS==='android'?row('诊断报告导出',undefined,()=>{void exportDiagnostics()}):null}
   {row('APP备案号',APP_FILING_NUMBER,openAppFiling)}
   {row('版本信息',versionName,undefined,true)}
  </Card>
  {onRestoreDefaults?<><Text style={s.groupTitle}>其他</Text><Card style={s.group}>{row('恢复默认设置',undefined,confirmRestoreDefaults,true)}</Card></>:null}
 </ScrollView><BottomNav active="settings" onChange={onTab}/></View>
  <Modal transparent visible={retentionOpen} animationType="slide" onRequestClose={()=>setRetentionOpen(false)}>
   <View style={s.modalRoot}>
    <Pressable style={s.scrim} onPress={()=>setRetentionOpen(false)}/>
    <View style={s.sheet}>
     <Text style={s.sheetTitle}>记录保留周期</Text>
     {RETENTION_OPTIONS.map(([days,label],index)=><Pressable key={days} onPress={()=>{setRetentionOpen(false);onRetentionChange(days)}} style={[s.option,index===RETENTION_OPTIONS.length-1&&{borderBottomWidth:0}]}><Text style={s.optionLabel}>{label}</Text><Text style={s.check}>{days===logRetentionDays?'✓':''}</Text></Pressable>)}
    </View>
   </View>
  </Modal>
  <Modal transparent visible={helpOpen} animationType="slide" onRequestClose={()=>setHelpOpen(false)}>
   <View style={s.modalRoot}>
    <Pressable style={s.scrim} onPress={()=>setHelpOpen(false)}/>
    <View style={s.sheet}>
     <Text style={s.sheetTitle}>使用帮助 / 常见问题</Text>
     <ScrollView style={s.modalScroll} showsVerticalScrollIndicator={false}>
      {helpItems.map(item=><View key={item.title} style={s.helpItem}><Text style={s.helpTitle}>{item.title}</Text><Text style={s.helpBody}>{item.body}</Text></View>)}
     </ScrollView>
     <Pressable accessibilityRole="button" onPress={()=>setHelpOpen(false)} style={s.modalAction}><Text style={s.modalActionText}>完成</Text></Pressable>
    </View>
   </View>
  </Modal>
  <Modal transparent visible={updateOpen} animationType="slide" onRequestClose={()=>setUpdateOpen(false)}>
   <View style={s.modalRoot}>
    <Pressable style={s.scrim} onPress={()=>setUpdateOpen(false)}/>
    <View style={s.sheet}>
     <Text style={s.sheetTitle}>{updateTitle}</Text>
     <ScrollView style={s.modalScroll} showsVerticalScrollIndicator={false}>
      {updateState==='checking'?<ActivityIndicator color={C.blue} style={s.spinner}/>:null}
      <Text style={s.helpBody}>{updateMessage}</Text>
      {release?.body?.trim()?<><Text style={s.helpTitle}>更新内容</Text><Text style={s.helpBody}>{release.body.trim()}</Text></>:null}
     </ScrollView>
     {release?.html_url?.startsWith('https://github.com/Ronnie-Jiao/ClearScreen/releases')?<Pressable accessibilityRole="button" onPress={openReleasePage} style={s.modalAction}><Text style={s.modalActionText}>查看发布页</Text></Pressable>:null}
     <Pressable accessibilityRole="button" onPress={()=>setUpdateOpen(false)} style={s.modalAction}><Text style={s.modalActionText}>完成</Text></Pressable>
    </View>
   </View>
  </Modal>
 </Background>;
}

const s=StyleSheet.create({
 root:{flex:1},scroll:{padding:18,paddingBottom:112},groupTitle:{fontSize:13,fontWeight:'800',color:C.text2,paddingHorizontal:8,paddingVertical:8},group:{paddingHorizontal:16,marginBottom:10},row:{minHeight:66,flexDirection:'row',alignItems:'center',gap:8,borderBottomWidth:1,borderBottomColor:C.line},rowTitle:{flex:1,fontSize:15,fontWeight:'900',color:C.navy},value:{fontSize:14,fontWeight:'800',color:C.text2},
 modalRoot:{flex:1,justifyContent:'flex-end'},scrim:{...StyleSheet.absoluteFillObject,backgroundColor:'rgba(5,18,40,.35)'},sheet:{backgroundColor:'#fff',borderTopLeftRadius:26,borderTopRightRadius:26,paddingHorizontal:20,paddingTop:18,paddingBottom:28,maxHeight:'86%'},sheetTitle:{fontSize:17,fontWeight:'900',color:C.navy,paddingVertical:10},option:{minHeight:58,flexDirection:'row',alignItems:'center',borderBottomWidth:1,borderBottomColor:C.line},optionLabel:{flex:1,fontSize:15,fontWeight:'800',color:C.text},check:{fontSize:18,fontWeight:'900',color:C.blue},
 modalScroll:{flexShrink:1},helpItem:{paddingVertical:12,borderBottomWidth:1,borderBottomColor:C.line},helpTitle:{fontSize:15,fontWeight:'900',color:C.navy,marginBottom:6},helpBody:{fontSize:14,lineHeight:21,color:C.text},spinner:{paddingVertical:18},modalAction:{minHeight:48,alignItems:'center',justifyContent:'center',marginTop:8},modalActionText:{fontSize:15,fontWeight:'900',color:C.blue},
});

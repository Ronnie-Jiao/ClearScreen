import React, { useState } from 'react';
import {
 ActivityIndicator,
 Image,
 Linking,
 Platform,
 Pressable,
 ScrollView,
 StyleSheet,
 Text,
 View,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { Background, Card, Chevron, TopBar } from '../components/UI';
import { AppDialog, type DialogButton } from '../components/AppDialog';
import { LOGO } from '../assets';
import { C, COMPACT_UI } from '../theme';

export type SettingsSection='privacyData'|'helpFeedback'|'about';
type HelpItem={title:string;body:string};
type UpdateState='checking'|'unavailable'|'failed'|'latest'|'newer'|'ahead'|'unknown';
type GitHubRelease={tag_name?:string;name?:string;body?:string;html_url?:string};

const SECTION_TITLES:Record<SettingsSection,string>={privacyData:'隐私与数据',helpFeedback:'帮助与反馈',about:'关于净屏'};
const RETENTION_OPTIONS:[number,string][]=[[0,'关闭记录'],[1,'1 天'],[7,'7 天'],[30,'30 天']];
const UPDATE_URL='https://api.github.com/repos/Ronnie-Jiao/ClearScreen/releases/latest';

const ANDROID_HELP:HelpItem[]=[
 {title:'如何开启自动跳过？',body:'在“设置 > 服务与权限”中打开自动跳过服务，按系统提示启用“净屏”无障碍服务后返回。'},
 {title:'为什么有些广告未处理？',body:'请确认总开关、该应用和无障碍服务均已开启。净屏只处理能明确识别且有安全关闭控件的广告；强制观看或结构不支持的广告不会跳过。'},
 {title:'如何保持后台运行？',body:'在“设置 > 服务与权限”中打开后台运行，并按系统页面允许净屏自启动和后台运行。'},
];
const IOS_HELP:HelpItem[]=[
 {title:'如何开启 Safari 扩展？',body:'在 iPhone 设置中启用净屏扩展，再从 Safari 扩展菜单打开过滤，并允许访问需要处理的网站。'},
 {title:'为什么有些网页广告未处理？',body:'净屏只处理同一广告区域内明确标记的跳过或关闭控件。图片广告、嵌入框架或网页结构不兼容时可能无法处理。'},
 {title:'如何关闭网页处理？',body:'在 Safari 扩展菜单关闭过滤，或在 iPhone 设置中停用净屏扩展或撤销网站访问权限。'},
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

function SettingsRow({title,value,onPress,last=false,destructive=false}:{
 title:string;
 value?:string;
 onPress:()=>void;
 last?:boolean;
 destructive?:boolean;
}){
 return <Pressable accessibilityRole="button" onPress={onPress} style={[s.row,last&&s.lastRow]}>
  <Text style={[s.rowTitle,destructive&&s.destructive]}>{title}</Text>
  {value?<Text style={s.value}>{value}</Text>:null}
  <Chevron compact/>
 </Pressable>;
}

export default function SettingsDetailScreen({section,logRetentionDays,versionName,onBack,onClear,onRetentionChange,onWithdrawConsent}:{
 section:SettingsSection;
 logRetentionDays:number;
 versionName:string;
 onBack:()=>void;
 onClear:()=>void;
 onRetentionChange:(days:number)=>void;
 onWithdrawConsent?:()=>void;
}){
 const [updateOpen,setUpdateOpen]=useState(false);
 const [updateState,setUpdateState]=useState<UpdateState>('checking');
 const [release,setRelease]=useState<GitHubRelease|null>(null);
 const [releaseError,setReleaseError]=useState(false);
 const helpItems=Platform.OS==='android'?ANDROID_HELP:Platform.OS==='ios'?IOS_HELP:OTHER_PLATFORM_HELP;

 const checkUpdates=async()=>{
  setReleaseError(false);
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
  void Linking.openURL(url).catch(()=>setReleaseError(true));
 };
 const closeUpdate=()=>setUpdateOpen(false);
 const updateButtons:DialogButton[]=releaseError
  ? [{text:'确定',onPress:()=>setReleaseError(false)}]
  : release?.html_url?.startsWith('https://github.com/Ronnie-Jiao/ClearScreen/releases')
   ? [{text:'完成',style:'cancel',onPress:closeUpdate},{text:'查看发布页',onPress:openReleasePage}]
   : [{text:'完成',onPress:closeUpdate}];
 return <Background>
  <ScrollView key={section} contentContainerStyle={s.page} showsVerticalScrollIndicator={false}>
   <TopBar compact title={SECTION_TITLES[section]} onBack={onBack}/>
   {section==='privacyData'?<>
    <Card compact style={s.group}>
     <View style={s.retentionBlock}>
      <Text style={s.fieldTitle}>记录保留周期</Text>
      <View accessibilityRole="radiogroup" style={s.retentionOptions}>
       {RETENTION_OPTIONS.map(([days,label])=>{
        const selected=days===logRetentionDays;
        return <Pressable key={days} accessibilityRole="radio" accessibilityState={{checked:selected}} onPress={()=>{if(!selected)onRetentionChange(days)}} style={[s.retentionOption,selected&&s.retentionSelected]}>
         <Text style={[s.optionLabel,selected&&s.optionSelected]}>{label}</Text>
         <View style={s.optionCheck}>{selected?<Ionicons name="checkmark" size={18} color={C.blue}/>:null}</View>
        </Pressable>;
       })}
      </View>
     </View>
     <SettingsRow title="清空记录" onPress={onClear} destructive last/>
    </Card>
    {Platform.OS==='android'&&onWithdrawConsent?<Card compact style={s.group}><SettingsRow title="撤回同意并删除" onPress={onWithdrawConsent} destructive last/></Card>:null}
   </>:section==='helpFeedback'?<>
    <Text style={s.sectionTitle}>常见问题</Text>
    <Card compact style={s.group}>
     {helpItems.map((item,index)=><View key={item.title} style={[s.helpItem,index===helpItems.length-1&&s.lastRow]}><Text style={s.helpTitle}>{item.title}</Text><Text style={s.helpBody}>{item.body}</Text></View>)}
    </Card>
   </>:<>
    <Card compact style={s.group}>
     <View style={s.aboutBrand}>
      <Image source={LOGO} style={s.aboutLogo}/>
      <Text style={s.aboutName}>净屏</Text>
      <Text style={s.aboutVersion}>版本 {versionName}</Text>
     </View>
    </Card>
   </>}
  </ScrollView>
  <AppDialog visible={updateOpen} title={releaseError?'无法打开发布页':updateTitle} message={releaseError?'请稍后重试。':updateMessage} tone={releaseError||updateState==='failed'?'error':updateState==='latest'?'success':'info'} cancelable={!releaseError} onRequestClose={closeUpdate} buttons={updateButtons}>
   {!releaseError&&updateState==='checking'?<ActivityIndicator color={C.blue} style={s.spinner}/>:null}
   {!releaseError&&release?.body?.trim()?<><Text style={s.updateHeading}>更新内容</Text><Text style={s.helpBody}>{release.body.trim()}</Text></>:null}
  </AppDialog>
 </Background>;
}

const s=StyleSheet.create({
 page:{paddingHorizontal:18,paddingBottom:36},
 group:{paddingHorizontal:COMPACT_UI.cardPadding,marginBottom:COMPACT_UI.gap},
 row:{minHeight:COMPACT_UI.rowHeight,paddingVertical:12,flexDirection:'row',alignItems:'center',gap:8,borderBottomWidth:1,borderBottomColor:C.line},
 lastRow:{borderBottomWidth:0},rowTitle:{flex:1,fontSize:COMPACT_UI.body,fontWeight:'800',color:C.navy},
 value:{maxWidth:'62%',flexShrink:1,fontSize:COMPACT_UI.caption,lineHeight:18,fontWeight:'700',textAlign:'right',color:C.text2},destructive:{color:C.red},
 sectionTitle:{fontSize:COMPACT_UI.caption,fontWeight:'800',color:C.text2,paddingHorizontal:8,paddingBottom:8},
 retentionBlock:{paddingTop:16,paddingBottom:14,borderBottomWidth:1,borderBottomColor:C.line},fieldTitle:{fontSize:COMPACT_UI.body,fontWeight:'800',color:C.navy,marginBottom:12},
 retentionOptions:{flexDirection:'row',flexWrap:'wrap',gap:8},retentionOption:{flexBasis:'45%',flexGrow:1,minHeight:COMPACT_UI.buttonHeight,paddingHorizontal:12,paddingVertical:10,borderWidth:1,borderColor:C.line,borderRadius:12,flexDirection:'row',alignItems:'center',gap:6,backgroundColor:'#F7FBFF'},
 retentionSelected:{borderColor:C.blue,backgroundColor:'#E9F4FF'},optionLabel:{flex:1,fontSize:COMPACT_UI.body,fontWeight:'700',color:C.text2},optionSelected:{color:C.blue},optionCheck:{width:18},
 helpItem:{paddingVertical:14,borderBottomWidth:1,borderBottomColor:C.line},helpTitle:{fontSize:COMPACT_UI.body,fontWeight:'900',color:C.navy,marginBottom:6},helpBody:{fontSize:COMPACT_UI.body,lineHeight:21,color:C.text},
 aboutBrand:{alignItems:'center',paddingVertical:24},aboutLogo:{width:64,height:64,marginBottom:10},aboutName:{fontSize:20,fontWeight:'900',color:C.navy},aboutVersion:{fontSize:COMPACT_UI.body,color:C.text2,marginTop:6},
 updateHeading:{fontSize:15,fontWeight:'900',color:C.navy,marginTop:14,marginBottom:6},spinner:{paddingVertical:18},
});

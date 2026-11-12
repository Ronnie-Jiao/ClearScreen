import React, { useState } from 'react';
import {
 ActivityIndicator,
 Keyboard,
 Image,
 Linking,
 Platform,
 Pressable,
 ScrollView,
 StyleSheet,
 Text,
 TextInput,
 View,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { Background, Card, Chevron, TopBar } from '../components/UI';
import { AppDialog, type DialogButton } from '../components/AppDialog';
import { LOGO } from '../assets';
import { C, COMPACT_UI } from '../theme';

export type SettingsSection='privacyData'|'helpFeedback'|'about';
export type HelpDestination='home'|'appManagement'|'servicePermissions'|'systemRecommend'|'appNotifications'|'marketingChannels'|'learnedRecords'|'records'|'privacyData';
type HelpItem={title:string;body:string;keywords:string;destination:HelpDestination};
type UpdateState='checking'|'unavailable'|'failed'|'latest'|'newer'|'ahead'|'unknown';
type GitHubRelease={tag_name?:string;name?:string;body?:string;html_url?:string};

const SECTION_TITLES:Record<SettingsSection,string>={privacyData:'隐私与数据',helpFeedback:'帮助与反馈',about:'关于净屏'};
const RETENTION_OPTIONS:[number,string][]=[[0,'关闭记录'],[1,'1 天'],[7,'7 天'],[30,'30 天']];
const UPDATE_URL='https://api.github.com/repos/Ronnie-Jiao/ClearScreen/releases/latest';
const AUTHOR_REPOSITORY_URL='https://github.com/Ronnie-Jiao/ClearScreen';

const ANDROID_HELP:HelpItem[]=[
 {title:'首次开启自动跳过',body:'进入“设置 > 服务与权限”，打开“自动跳过服务”，再按系统提示启用“净屏”无障碍服务。返回首页，状态显示“工作中”后即可使用。',keywords:'首次使用 开始 开启 权限 无障碍 自动跳过',destination:'servicePermissions'},
 {title:'开启或暂停净屏',body:'在首页点“开启净屏”开始自动处理，点“暂停净屏”暂停。首页显示“需要修复”或“状态待确认”时，按页面提示检查服务。',keywords:'首页 总开关 开关 工作中 暂停 继续',destination:'home'},
 {title:'选择要处理的应用',body:'打开“广告”页右上角的“应用管理”，用每个应用旁的开关控制是否自动处理。关闭某个应用后，净屏不会处理该应用。',keywords:'应用管理 应用列表 应用开关 排除 白名单',destination:'appManagement'},
 {title:'学习关闭广告控件',body:'在首页点“学习关闭”，切到出现广告的应用，在学习浮层点“选择控件”并点广告关闭控件；确认框选正确后点“确认选取”。规则保存在本机，只有目标仍能唯一匹配时才会执行。',keywords:'学习关闭 自定义 关闭窗口 控件 选择 确认 学习规则',destination:'home'},
 {title:'查看或删除学习规则',body:'在首页“自定义关闭窗口”区域点“更多”，可查看已学规则并逐条删除。',keywords:'学习关闭 自定义关闭窗口 规则 删除',destination:'learnedRecords'},
 {title:'查看处理记录',body:'在首页“最近记录”点“更多”查看记录；点某条记录可查看详情。',keywords:'最近记录 日志 处理结果 今日记录',destination:'records'},
 {title:'设置系统推荐开关',body:'打开“广告 > 系统推荐开关”，选择要处理的项目并按页面提示操作。只会自动操作能确认页面和开关的项目。',keywords:'系统广告 系统推荐 个性化推荐 vivo 小米 华为 设置',destination:'systemRecommend'},
 {title:'管理应用通知',body:'打开“广告 > 应用通知”查找应用并查看通知状态。需要授权或连接服务时，按页面提示前往设置或连接。',keywords:'通知 应用通知 关闭通知 搜索应用',destination:'appNotifications'},
 {title:'扫描营销通知',body:'打开“广告 > 营销通知”查看通知渠道。未连接服务时只能观察部分渠道；按页面提示连接 ADB Shell 或 Shizuku 后，才能读取更完整的清单。',keywords:'营销通知 通知渠道 扫描 ADB Shell Shizuku 连接',destination:'marketingChannels'},
 {title:'连接 ADB Shell',body:'进入“设置 > 服务与权限”，点按“ADB Shell 操作能力”并按系统页面完成无线调试配对。开关显示已连接后，相关系统状态读取能力才可用。',keywords:'ADB Shell 无线调试 配对 连接 授权',destination:'servicePermissions'},
 {title:'允许后台运行',body:'进入“设置 > 服务与权限”，按手机系统页面设置开机自启动、后台运行和电池优化。不同品牌的设置入口名称可能不同。',keywords:'后台运行 开机自启动 自启动 电池优化 后台',destination:'servicePermissions'},
 {title:'管理记录和隐私数据',body:'进入“设置 > 隐私与数据”选择记录保留周期或清空记录。Android 还可撤回同意并删除净屏保存的数据。',keywords:'隐私 数据 清空记录 保留周期 撤回同意 删除',destination:'privacyData'},
 {title:'广告没有被处理',body:'先确认首页总开关、目标应用开关和无障碍服务都已开启。净屏只处理能明确识别且有安全关闭控件的广告；强制观看、没有可访问控件或页面结构不支持的广告可能无法处理。',keywords:'故障 排查 没处理 未跳过 广告 无障碍 应用开关',destination:'servicePermissions'},
];
const IOS_HELP:HelpItem[]=[
 {title:'开启 Safari 扩展',body:'在 iPhone 设置中启用净屏扩展，再从 Safari 扩展菜单打开过滤，并允许访问需要处理的网站。',keywords:'Safari 扩展 启用 iPhone 设置 网站权限',destination:'home'},
 {title:'在网页中开启过滤',body:'打开 Safari 的扩展菜单并开启净屏过滤；首次使用时允许净屏访问当前网站。',keywords:'Safari 网页 过滤 开关 网站',destination:'home'},
 {title:'网页广告没有被处理',body:'净屏只处理同一广告区域内明确标记的跳过或关闭控件。图片广告、嵌入框架或网页结构不兼容时可能无法处理。',keywords:'广告 未处理 网页 图片 嵌入框架',destination:'home'},
 {title:'关闭网页过滤',body:'在 Safari 扩展菜单关闭过滤，或在 iPhone 设置中停用净屏扩展或撤销网站访问权限。',keywords:'关闭 停止 Safari 扩展 网站权限',destination:'home'},
];
const OTHER_PLATFORM_HELP:HelpItem[]=[
 {title:'功能状态',body:'当前平台的自动处理能力尚未接入。',keywords:'功能 平台 自动处理',destination:'home'},
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

export default function SettingsDetailScreen({section,logRetentionDays,versionName,onBack,onClear,onRetentionChange,onWithdrawConsent,onHelpNavigate,helpSearchInput='',helpSearchTerm='',onHelpSearchInputChange=()=>{},onHelpSearchSubmit=()=>{}}:{
 section:SettingsSection;
 logRetentionDays:number;
 versionName:string;
 onBack:()=>void;
 onClear:()=>void;
 onRetentionChange:(days:number)=>void;
 onWithdrawConsent?:()=>void;
 onHelpNavigate?:(destination:HelpDestination)=>void;
 helpSearchInput?:string;
 helpSearchTerm?:string;
 onHelpSearchInputChange?:(value:string)=>void;
 onHelpSearchSubmit?:(value:string)=>void;
}){
 const [updateOpen,setUpdateOpen]=useState(false);
 const [updateState,setUpdateState]=useState<UpdateState>('checking');
 const [release,setRelease]=useState<GitHubRelease|null>(null);
 const [releaseError,setReleaseError]=useState(false);
 const [githubErrorOpen,setGithubErrorOpen]=useState(false);
 const helpItems=Platform.OS==='android'?ANDROID_HELP:Platform.OS==='ios'?IOS_HELP:OTHER_PLATFORM_HELP;
 const normalizedHelpSearch=helpSearchTerm.toLocaleLowerCase();
 const visibleHelpItems=normalizedHelpSearch
  ?helpItems.filter(item=>`${item.title} ${item.body} ${item.keywords}`.toLocaleLowerCase().includes(normalizedHelpSearch))
  :helpItems;
 const submitHelpSearch=()=>{
  onHelpSearchSubmit(helpSearchInput.trim());
  Keyboard.dismiss();
 };
 const clearHelpSearch=()=>{
  onHelpSearchInputChange('');
  onHelpSearchSubmit('');
 };
 const openHelpItem=(item:HelpItem)=>{
  Keyboard.dismiss();
  onHelpNavigate?.(item.destination);
 };

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
 const openAuthorRepository=()=>{
  void Linking.openURL(AUTHOR_REPOSITORY_URL).catch(()=>setGithubErrorOpen(true));
 };
 const closeGithubError=()=>setGithubErrorOpen(false);
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
    <Text style={s.sectionTitle}>使用说明</Text>
    <View style={s.searchRow}>
     <View style={s.searchField}>
      <Ionicons name="search-outline" size={20} color={C.text2}/>
      <TextInput accessibilityLabel="搜索使用说明" value={helpSearchInput} onChangeText={onHelpSearchInputChange} onSubmitEditing={submitHelpSearch} returnKeyType="search" placeholder="搜索功能或问题" placeholderTextColor={C.text2} style={s.searchInput}/>
      {helpSearchInput?<Pressable accessibilityRole="button" accessibilityLabel="清除搜索" onPress={clearHelpSearch} hitSlop={8}><Ionicons name="close-circle" size={19} color={C.text2}/></Pressable>:null}
     </View>
     <Pressable accessibilityRole="button" accessibilityLabel="搜索" onPress={submitHelpSearch} style={({pressed})=>[s.searchButton,pressed&&s.searchButtonPressed]}>
      <Ionicons name="search" size={17} color="#FFFFFF"/><Text style={s.searchButtonText}>搜索</Text>
     </Pressable>
    </View>
    <Text style={s.sectionTitle}>{helpSearchTerm?`搜索结果（${visibleHelpItems.length}）`:'全部内容'}</Text>
    <Card compact style={s.group}>
     {visibleHelpItems.length?visibleHelpItems.map((item,index)=><Pressable key={item.title} accessibilityRole={onHelpNavigate?'button':undefined} accessibilityLabel={onHelpNavigate?`${item.title}，前往相关页面`:undefined} disabled={!onHelpNavigate} onPress={()=>openHelpItem(item)} style={[s.helpItem,index===visibleHelpItems.length-1&&s.lastRow]}>
      <View style={s.helpHeader}><Text style={s.helpTitle}>{item.title}</Text>{onHelpNavigate?<Chevron compact/>:null}</View>
      <Text style={s.helpBody}>{item.body}</Text>
     </Pressable>):<Text style={s.helpEmpty}>没有找到相关内容</Text>}
    </Card>
   </>:<>
    <Card compact style={s.group}>
     <View style={s.aboutBrand}>
      <Image source={LOGO} style={s.aboutLogo}/>
      <Text style={s.aboutName}>净屏</Text>
      <Text style={s.aboutVersion}>版本 {versionName}</Text>
     </View>
    </Card>
    <Card compact style={s.group}>
     <View style={s.authorRow}>
      <Text style={s.authorLabel}>作者</Text>
      <Text style={s.authorName}>Ronnie Jiao</Text>
      <Pressable accessibilityRole="link" accessibilityLabel="GitHub 仓库" onPress={openAuthorRepository} style={({pressed})=>[s.githubLink,pressed&&s.githubLinkPressed]}>
       <Ionicons name="logo-github" size={24} color={C.navy}/>
      </Pressable>
     </View>
    </Card>
   </>}
  </ScrollView>
  <AppDialog visible={updateOpen} title={releaseError?'无法打开发布页':updateTitle} message={releaseError?'请稍后重试。':updateMessage} tone={releaseError||updateState==='failed'?'error':updateState==='latest'?'success':'info'} cancelable={!releaseError} onRequestClose={closeUpdate} buttons={updateButtons}>
   {!releaseError&&updateState==='checking'?<ActivityIndicator color={C.blue} style={s.spinner}/>:null}
   {!releaseError&&release?.body?.trim()?<><Text style={s.updateHeading}>更新内容</Text><Text style={s.helpBody}>{release.body.trim()}</Text></>:null}
  </AppDialog>
  <AppDialog visible={githubErrorOpen} title="无法打开 GitHub" message="请稍后重试。" tone="error" cancelable onRequestClose={closeGithubError} buttons={[{text:'确定',onPress:closeGithubError}]}/>
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
 searchRow:{flexDirection:'row',alignItems:'center',gap:8,marginBottom:COMPACT_UI.gap},searchField:{height:48,flex:1,flexDirection:'row',alignItems:'center',gap:8,paddingHorizontal:13,borderRadius:14,borderWidth:1,borderColor:C.line,backgroundColor:'#FFFFFF'},searchInput:{flex:1,minWidth:0,paddingVertical:0,fontSize:COMPACT_UI.body,color:C.text},searchButton:{height:48,minWidth:76,paddingHorizontal:12,borderRadius:14,flexDirection:'row',alignItems:'center',justifyContent:'center',gap:5,backgroundColor:C.blue},searchButtonPressed:{opacity:0.78},searchButtonText:{fontSize:COMPACT_UI.caption,fontWeight:'900',color:'#FFFFFF'},
 helpItem:{paddingVertical:14,borderBottomWidth:1,borderBottomColor:C.line},helpHeader:{flexDirection:'row',alignItems:'center',gap:8,marginBottom:6},helpTitle:{flex:1,fontSize:COMPACT_UI.body,fontWeight:'900',color:C.navy},helpBody:{fontSize:COMPACT_UI.body,lineHeight:21,color:C.text},helpEmpty:{paddingVertical:24,textAlign:'center',fontSize:COMPACT_UI.body,color:C.text2},
 aboutBrand:{alignItems:'center',paddingVertical:24},aboutLogo:{width:64,height:64,marginBottom:10},aboutName:{fontSize:20,fontWeight:'900',color:C.navy},aboutVersion:{fontSize:COMPACT_UI.body,color:C.text2,marginTop:6},
 authorRow:{minHeight:58,flexDirection:'row',alignItems:'center',gap:12},authorLabel:{fontSize:COMPACT_UI.body,fontWeight:'700',color:C.text2},authorName:{flex:1,fontSize:COMPACT_UI.body,fontWeight:'800',color:C.navy},githubLink:{width:44,height:44,borderRadius:15,alignItems:'center',justifyContent:'center',backgroundColor:'#EDF5FC'},githubLinkPressed:{opacity:0.72},
 updateHeading:{fontSize:15,fontWeight:'900',color:C.navy,marginTop:14,marginBottom:6},spinner:{paddingVertical:18},
});

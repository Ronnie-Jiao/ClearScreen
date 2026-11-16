import React from 'react';
import { Image, ImageSourcePropType, Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { LinearGradient } from 'expo-linear-gradient';
import { Background, Chevron } from '../components/UI';
import { useAppAlert } from '../components/AppDialog';
import { BottomNav, useBottomNavMetrics } from '../components/BottomNav';
import { C } from '../theme';
import { LOGO, SPLASH_BACKGROUND, SETTINGS_GEAR, SETTINGS_SERVICE, SETTINGS_PRIVACY, SETTINGS_RESTORE, SETTINGS_GUIDE, SETTINGS_ABOUT } from '../assets';
import type { SettingsSection } from './SettingsDetailScreen';

type MainTab='home'|'apps'|'settings';

function SettingsHeader(){
 return <View style={s.brandRow}>
  <Image source={LOGO} resizeMode="contain" style={s.brandLogo} accessible={false}/>
  <View style={s.brandDivider}/>
  <View style={s.brandText}>
   <Text style={s.brandTitle} numberOfLines={1} adjustsFontSizeToFit minimumFontScale={0.8}>系统设置</Text>
   <Text style={s.brandEnglish} numberOfLines={1} adjustsFontSizeToFit minimumFontScale={0.8}>System <Text style={s.brandAccent}>Settings</Text></Text>
  </View>
  <Image source={SETTINGS_GEAR} resizeMode="contain" style={s.brandGear} accessible={false}/>
 </View>;
}

function SettingsEntry({title,icon,onPress}:{title:string;icon:ImageSourcePropType;onPress:()=>void}){
 return <Pressable accessibilityRole="button" accessibilityLabel={title} onPress={onPress} style={({pressed})=>[s.card,pressed&&s.cardPressed]}>
  <LinearGradient colors={['rgba(255,255,255,0.93)','rgba(230,244,255,0.76)']} start={{x:0,y:0}} end={{x:1,y:1}} style={s.cardInner}>
   <Image source={icon} resizeMode="contain" style={s.cardIcon} accessible={false}/>
   <View style={s.cardDivider}/>
   <Text style={s.cardTitle}>{title}</Text>
   <Chevron/>
  </LinearGradient>
 </Pressable>;
}

export default function SettingsScreen({onTab,onServicePermissions,onOpenSection,onRestoreDefaults}:{
 onTab:(tab:MainTab)=>void;
 onServicePermissions:()=>void;
 onOpenSection:(section:SettingsSection)=>void;
 onRestoreDefaults?:()=>Promise<void>;
}){
 const showAlert=useAppAlert();
 const {inset:navInset}=useBottomNavMetrics();
 const confirmRestoreDefaults=()=>{
  if(!onRestoreDefaults)return;
  showAlert('恢复默认设置？',undefined,[
   {text:'取消',style:'cancel'},
   {text:'恢复默认',style:'destructive',onPress:()=>{
    void onRestoreDefaults().then(()=>showAlert('设置已恢复',undefined,undefined,{tone:'success'})).catch(()=>showAlert('恢复失败','请稍后重试。',undefined,{tone:'error'}));
   }},
  ]);
 };

 return <Background backgroundSource={SPLASH_BACKGROUND}><View style={s.root}>
  <ScrollView showsVerticalScrollIndicator={false} contentContainerStyle={[s.scroll,{paddingBottom:96+navInset}]}>
   <SettingsHeader/>
   <SettingsEntry title="服务与权限" icon={SETTINGS_SERVICE} onPress={onServicePermissions}/>
   <SettingsEntry title="隐私与数据" icon={SETTINGS_PRIVACY} onPress={()=>onOpenSection('privacyData')}/>
   {onRestoreDefaults?<SettingsEntry title="恢复默认设置" icon={SETTINGS_RESTORE} onPress={confirmRestoreDefaults}/>:null}
   <SettingsEntry title="使用说明" icon={SETTINGS_GUIDE} onPress={()=>onOpenSection('helpFeedback')}/>
   <SettingsEntry title="关于净屏" icon={SETTINGS_ABOUT} onPress={()=>onOpenSection('about')}/>
  </ScrollView>
  <BottomNav active="settings" onChange={onTab}/>
 </View></Background>;
}

const s=StyleSheet.create({
 root:{flex:1},
 scroll:{flexGrow:1,paddingHorizontal:16},
 brandRow:{minHeight:138,marginBottom:16,flexDirection:'row',alignItems:'center',gap:8},
 brandLogo:{width:58,height:58,flexShrink:0},
 brandDivider:{width:1,height:44,backgroundColor:'#A5C9EA',marginHorizontal:3,flexShrink:0},
 brandText:{flex:1,minWidth:0,gap:2},
 brandTitle:{fontSize:25,lineHeight:32,fontWeight:'900',color:C.navy},
 brandEnglish:{fontSize:15,lineHeight:20,fontWeight:'900',color:C.navy},
 brandAccent:{color:C.teal},
 brandGear:{width:'34%',aspectRatio:545/455,flexShrink:0},
 card:{minHeight:65,marginBottom:9,borderRadius:18,borderWidth:1,borderColor:'rgba(255,255,255,0.95)',backgroundColor:'rgba(255,255,255,0.82)',shadowColor:'#4A7EA2',shadowOpacity:.14,shadowRadius:12,shadowOffset:{width:0,height:6},elevation:3},
 cardPressed:{opacity:.88},
 cardInner:{minHeight:63,borderRadius:17,overflow:'hidden',flexDirection:'row',alignItems:'center',paddingHorizontal:15,paddingVertical:7,gap:13},
 cardIcon:{width:48,height:48,flexShrink:0},
 cardDivider:{width:1,height:28,backgroundColor:'#B6D5F3',flexShrink:0},
 cardTitle:{flex:1,minWidth:0,fontSize:18,fontWeight:'900',color:C.navy},
});

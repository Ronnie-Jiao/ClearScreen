import React from 'react';
import { Image, ImageSourcePropType, Platform, Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { LinearGradient } from 'expo-linear-gradient';
import { Background, Chevron } from '../components/UI';
import { BottomNav, useBottomNavMetrics } from '../components/BottomNav';
import { C } from '../theme';
import { LOGO, SPLASH_BACKGROUND, ADS_APP_MANAGEMENT, ADS_SYSTEM_RECOMMEND, ADS_APP_NOTIFICATIONS, ADS_MARKETING_NOTIFICATIONS, ADS_LEARNED_CLOSE, ADS_SHIELD } from '../assets';

type MainTab='home'|'apps'|'settings';
type AdsDestination='systemRecommend'|'appNotifications'|'marketingChannels'|'learnedRecords';

// The 2026-11-15 ad-management design uses the same light wave wallpaper as the splash screen.
const ADS_WALLPAPER=SPLASH_BACKGROUND;

/** Ads-only brand row: logo, divider, bilingual page title and the holographic shield. */
function AdsBrandHeader(){
 return <View style={s.brandRow}>
  <Image source={LOGO} resizeMode="contain" style={s.brandLogo}/>
  <View style={s.brandDivider}/>
  <View style={s.brandText}>
   <Text style={s.brandTitle} numberOfLines={1} adjustsFontSizeToFit minimumFontScale={0.8}>广告管理</Text>
   <Text style={s.brandEnglish} numberOfLines={1} adjustsFontSizeToFit minimumFontScale={0.8}>Ad <Text style={{color:C.teal}}>Management</Text></Text>
  </View>
  <Image source={ADS_SHIELD} resizeMode="contain" style={s.brandShield}/>
 </View>;
}

function AdEntryCard({title,icon,onPress}:{title:string;icon:ImageSourcePropType;onPress:()=>void}){
 return <Pressable accessibilityRole="button" accessibilityLabel={title} onPress={onPress} style={({pressed})=>[s.card,pressed&&s.cardPressed]}>
  <LinearGradient colors={['rgba(255,255,255,0.93)','rgba(230,244,255,0.76)']} start={{x:0,y:0}} end={{x:1,y:1}} style={s.cardInner}>
   <Image source={icon} resizeMode="contain" style={s.cardIcon}/>
   <View pointerEvents="none" style={s.cardDivider}/>
   <Text style={s.cardTitle} numberOfLines={1}>{title}</Text>
   <Chevron/>
  </LinearGradient>
 </Pressable>;
}

export default function AdsScreen({onTab,onOpen,onOpenAppManagement}:{onTab:(tab:MainTab)=>void;onOpen:(page:AdsDestination)=>void;onOpenAppManagement:()=>void}){
 const {inset:navInset}=useBottomNavMetrics();
 const entries:{title:string;icon:ImageSourcePropType;onPress:()=>void}[]=[
  {title:'排除应用',icon:ADS_APP_MANAGEMENT,onPress:onOpenAppManagement},
  {title:'系统推荐开关',icon:ADS_SYSTEM_RECOMMEND,onPress:()=>onOpen('systemRecommend')},
  {title:'应用通知管理',icon:ADS_APP_NOTIFICATIONS,onPress:()=>onOpen('appNotifications')},
  {title:'营销通知管理',icon:ADS_MARKETING_NOTIFICATIONS,onPress:()=>onOpen('marketingChannels')},
  ...(Platform.OS==='android'?[{title:'管理学习关闭按钮',icon:ADS_LEARNED_CLOSE,onPress:()=>onOpen('learnedRecords')}]:[]),
 ];
 return <Background backgroundSource={ADS_WALLPAPER}><View style={s.root}>
  <ScrollView showsVerticalScrollIndicator={false} contentContainerStyle={[s.scroll,{paddingBottom:96+navInset}]}>
   <AdsBrandHeader/>
   {entries.map(entry=><AdEntryCard key={entry.title} title={entry.title} icon={entry.icon} onPress={entry.onPress}/>)}
  </ScrollView>
  <BottomNav active="apps" onChange={onTab}/>
 </View></Background>;
}

// 入口卡片规格与另一首页保持一致（行高/间距/图标/字号/字重），修改时需两页同步。
const s=StyleSheet.create({
 root:{flex:1},scroll:{flexGrow:1,paddingHorizontal:16},
 brandRow:{minHeight:94,marginBottom:12,flexDirection:'row',alignItems:'center',gap:8},
 brandLogo:{width:50,height:50,flexShrink:0},
 brandDivider:{width:1,height:36,backgroundColor:'#A5C9EA',marginHorizontal:3,flexShrink:0},
 brandText:{flex:1,minWidth:0,gap:2},
 brandTitle:{fontSize:25,lineHeight:32,fontWeight:'600',color:C.navy},
 brandEnglish:{fontSize:15,lineHeight:20,fontWeight:'600',color:C.navy},
 brandShield:{width:'26%',aspectRatio:645/585,flexShrink:0},
 card:{minHeight:65,marginBottom:9,borderRadius:18,borderWidth:1,borderColor:'rgba(255,255,255,0.95)',backgroundColor:'rgba(255,255,255,0.82)',shadowColor:'#4A7EA2',shadowOpacity:.14,shadowRadius:12,shadowOffset:{width:0,height:6},elevation:3},
 cardPressed:{opacity:.88},
 cardInner:{minHeight:63,borderRadius:17,overflow:'hidden',flexDirection:'row',alignItems:'center',paddingHorizontal:15,paddingVertical:7,gap:13},
 cardIcon:{width:48,height:48,flexShrink:0},
 cardDivider:{width:1,height:28,backgroundColor:'#B6D5F3',flexShrink:0},
 cardTitle:{flex:1,minWidth:0,fontSize:18,fontWeight:'400',color:C.navy},
});

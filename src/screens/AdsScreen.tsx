import React from 'react';
import { Image, ImageSourcePropType, Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { LinearGradient } from 'expo-linear-gradient';
import { Ionicons } from '@expo/vector-icons';
import { Background } from '../components/UI';
import { BottomNav, useBottomNavMetrics } from '../components/BottomNav';
import { C } from '../theme';
import { LOGO, SPLASH_BACKGROUND, ADS_APP_MANAGEMENT, ADS_SYSTEM_RECOMMEND, ADS_APP_NOTIFICATIONS, ADS_MARKETING_NOTIFICATIONS, ADS_SHIELD } from '../assets';

type MainTab='home'|'apps'|'settings';
type AdsDestination='systemRecommend'|'appNotifications'|'marketingChannels';

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
  <LinearGradient colors={['rgba(255,255,255,0.94)','rgba(232,246,255,0.70)']} start={{x:0,y:0}} end={{x:1,y:1}} style={s.cardInner}>
   <Image source={icon} resizeMode="contain" style={s.cardIcon}/>
   <View pointerEvents="none" style={s.cardDivider}/>
   <Text style={s.cardTitle} numberOfLines={1}>{title}</Text>
   <Ionicons name="chevron-forward" size={22} color="#7E9AB8"/>
  </LinearGradient>
 </Pressable>;
}

export default function AdsScreen({onTab,onOpen,onOpenAppManagement}:{onTab:(tab:MainTab)=>void;onOpen:(page:AdsDestination)=>void;onOpenAppManagement:()=>void}){
 const {inset:navInset}=useBottomNavMetrics();
 const entries:{title:string;icon:ImageSourcePropType;onPress:()=>void}[]=[
  {title:'应用管理',icon:ADS_APP_MANAGEMENT,onPress:onOpenAppManagement},
  {title:'系统推荐开关',icon:ADS_SYSTEM_RECOMMEND,onPress:()=>onOpen('systemRecommend')},
  {title:'应用通知',icon:ADS_APP_NOTIFICATIONS,onPress:()=>onOpen('appNotifications')},
  {title:'营销通知',icon:ADS_MARKETING_NOTIFICATIONS,onPress:()=>onOpen('marketingChannels')},
 ];
 return <Background backgroundSource={ADS_WALLPAPER}><View style={s.root}>
  <ScrollView showsVerticalScrollIndicator={false} contentContainerStyle={[s.scroll,{paddingBottom:96+navInset}]}>
   <AdsBrandHeader/>
   {entries.map(entry=><AdEntryCard key={entry.title} title={entry.title} icon={entry.icon} onPress={entry.onPress}/>)}
  </ScrollView>
  <BottomNav active="apps" onChange={onTab}/>
 </View></Background>;
}

const s=StyleSheet.create({
 root:{flex:1},scroll:{flexGrow:1,paddingHorizontal:16},
 brandRow:{minHeight:138,marginBottom:16,flexDirection:'row',alignItems:'center',gap:8},
 brandLogo:{width:58,height:58,flexShrink:0},
 brandDivider:{width:1,height:44,backgroundColor:'#A5C9EA',marginHorizontal:3,flexShrink:0},
 brandText:{flex:1,minWidth:0,gap:2},
 brandTitle:{fontSize:25,lineHeight:32,fontWeight:'900',color:C.navy},
 brandEnglish:{fontSize:15,lineHeight:20,fontWeight:'900',color:C.navy},
 brandShield:{width:'34%',aspectRatio:645/585,flexShrink:0},
 card:{height:92,borderRadius:22,marginBottom:14,backgroundColor:'rgba(255,255,255,0.80)',borderWidth:1,borderColor:'rgba(255,255,255,0.95)',shadowColor:'#4A7EA2',shadowOpacity:.12,shadowRadius:14,shadowOffset:{width:0,height:6},elevation:3,overflow:'hidden'},
 cardPressed:{opacity:.88,transform:[{scale:.985}]},
 cardInner:{flex:1,flexDirection:'row',alignItems:'center',paddingHorizontal:16,gap:14},
 cardIcon:{width:56,height:56,flexShrink:0,borderRadius:16},
 cardDivider:{width:1,height:34,backgroundColor:'#C9DEF2',flexShrink:0},
 cardTitle:{flex:1,minWidth:0,fontSize:20,fontWeight:'900',color:C.navy,letterSpacing:-.2},
});

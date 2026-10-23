import React, { useContext } from 'react';
import { Image, ImageBackground, ImageSourcePropType, Pressable, StyleSheet, Text, View } from 'react-native';
import { LinearGradient } from 'expo-linear-gradient';
import { Ionicons } from '@expo/vector-icons';
import { C, COMPACT_UI, R } from '../theme';
import { LOGO, UI_BACKGROUND } from '../assets';
import { ScreenInsetsContext, SCREEN_TOP_GAP } from './ScreenInsets';

export function Background({children}:{children:React.ReactNode}){
  const {top}=useContext(ScreenInsetsContext);
  return <ImageBackground source={UI_BACKGROUND} resizeMode="stretch" style={[styles.root,{paddingTop:top+SCREEN_TOP_GAP}]} imageStyle={styles.backgroundImage}>
    {children}
  </ImageBackground>;
}

export function Card({children,style,compact=false}:{children:React.ReactNode;style?:any;compact?:boolean}){
  return <View style={[styles.card,compact&&styles.cardCompact,style]}>{children}</View>;
}

export function Toggle({value,onChange,compact=false}:{value:boolean;onChange?:()=>void;compact?:boolean}){
  return <Pressable accessibilityRole="switch" accessibilityState={{checked:value}} hitSlop={compact?{top:11,bottom:11,left:3,right:3}:undefined} onPress={onChange} style={[styles.toggle,compact&&styles.toggleCompact,value&&styles.toggleOn]}>
    <View style={[styles.knob,compact&&styles.knobCompact,value&&styles.knobOn]}/>
  </Pressable>;
}

export function Badge({children,green=true,compact=false}:{children:React.ReactNode;green?:boolean;compact?:boolean}){
  return <View style={[styles.badge,compact&&styles.badgeCompact,!green&&{backgroundColor:'#EDF1F6'}]}><Text style={[styles.badgeText,compact&&styles.badgeTextCompact,!green&&{color:'#7186A3'}]}>{children}</Text></View>;
}

export function AppIcon({source,size=56}:{source:ImageSourcePropType;size?:number}){
  return <Image source={source} style={{width:size,height:size,borderRadius:size*.23}} resizeMode="cover"/>;
}

export function SquareIcon({symbol,iconName,tone='blue',size=52}:{symbol:string;iconName?:keyof typeof Ionicons.glyphMap;tone?:'blue'|'green'|'purple'|'red';size?:number}){
  const bg={blue:'#DCEEFF',green:'#D8FAEA',purple:'#ECE8FF',red:'#FFE6EA'}[tone];
  const color={blue:C.blue,green:C.green,purple:C.purple,red:C.red}[tone];
  const iconBySymbol:Record<string,keyof typeof Ionicons.glyphMap>={
    '↗':'globe-outline',S:'globe-outline','≫':'scan-outline','▦':'grid-outline','□':'square-outline','✦':'sparkles-outline',
  };
  return <View style={[styles.square,{width:size,height:size,borderRadius:size*.28,backgroundColor:bg}]}><Ionicons name={iconName||iconBySymbol[symbol]||'apps-outline'} size={size*.43} color={color}/></View>;
}

export function BrandHeader({title='净屏',tagline=false,action,compact=false,smallText=false}:{title?:string;tagline?:boolean;action?:React.ReactNode;compact?:boolean;smallText?:boolean}){
  return <View style={[styles.brandRow,compact&&styles.brandRowCompact]}>
    <Image source={LOGO} style={[styles.brandLogo,compact&&styles.brandLogoCompact]}/>
    <View style={{flex:1}}><Text style={[styles.brandTitle,compact&&styles.brandTitleCompact,smallText&&styles.brandTitleSmall]}>{title}</Text><Text style={[styles.brandEnglish,compact&&styles.brandEnglishCompact,smallText&&styles.brandEnglishSmall]}>Clear<Text style={{color:C.teal}}>Screen</Text></Text></View>
    {action}
    {tagline?<Text style={styles.tagline}>净化屏幕{`\n`}专注生活</Text>:null}
  </View>;
}

export function TopBar({title,onBack,compact=false}:{title:string;onBack:()=>void;compact?:boolean}){
  return <View style={[styles.topbar,compact&&styles.topbarCompact]}>
    <Pressable accessibilityRole="button" onPress={onBack} hitSlop={16} style={styles.backButton}><Ionicons name="chevron-back" size={compact?22:27} color={C.navy}/></Pressable>
    <Text style={[styles.topTitle,compact&&styles.topTitleCompact]}>{title}</Text><View style={{width:36}}/>
  </View>;
}

export function PrimaryButton({title,onPress,compact=false}:{title:string;onPress:()=>void;compact?:boolean}){
  return <Pressable onPress={onPress}><LinearGradient colors={['#0D69F5','#15A4EA']} start={{x:0,y:0}} end={{x:1,y:0}} style={[styles.primary,compact&&styles.primaryCompact]}><Text style={[styles.primaryText,compact&&styles.primaryTextCompact]}>{title}</Text></LinearGradient></Pressable>;
}

export function SectionTitle({title}:{title:string}){return <Text style={styles.sectionTitle}>{title}</Text>}
export function Chevron({compact=false}:{compact?:boolean}){return <Ionicons name="chevron-forward" size={compact?18:22} color={C.text2}/>}

const styles=StyleSheet.create({
  root:{flex:1,backgroundColor:C.bg,overflow:'hidden',paddingBottom:0},backgroundImage:{width:'100%',height:'100%'},
  card:{backgroundColor:'rgba(255,255,255,0.96)',borderRadius:R.lg,borderWidth:1,borderColor:'rgba(255,255,255,0.98)',shadowColor:'#4A7EA2',shadowOpacity:.10,shadowRadius:18,shadowOffset:{width:0,height:8},elevation:3},
  cardCompact:{borderRadius:COMPACT_UI.cardRadius},
  toggle:{width:50,height:30,borderRadius:16,backgroundColor:C.graySwitch,padding:3,justifyContent:'center'},toggleCompact:{width:42,height:26,borderRadius:14},
  toggleOn:{backgroundColor:C.blue},
  knob:{width:24,height:24,borderRadius:12,backgroundColor:'#fff',shadowColor:'#243B55',shadowOpacity:.14,shadowRadius:4,shadowOffset:{width:0,height:1}},knobCompact:{width:20,height:20,borderRadius:10},
  knobOn:{alignSelf:'flex-end'},
  badge:{alignSelf:'flex-start',backgroundColor:C.greenBg,borderRadius:20,paddingHorizontal:13,paddingVertical:6},
  badgeText:{fontSize:14,fontWeight:'800',color:C.green},
  badgeCompact:{paddingHorizontal:8,paddingVertical:3,borderRadius:12},badgeTextCompact:{fontSize:COMPACT_UI.caption,lineHeight:16},
  square:{alignItems:'center',justifyContent:'center'},
  brandRow:{flexDirection:'row',alignItems:'center',gap:12,marginBottom:14,minHeight:72}, brandRowCompact:{gap:9,marginBottom:10,minHeight:72}, brandLogo:{width:68,height:68},brandLogoCompact:{width:68,height:68},
  brandTitle:{fontSize:32,fontWeight:'900',color:C.navy,letterSpacing:-.8,lineHeight:36},brandTitleCompact:{fontSize:25,letterSpacing:-.5,lineHeight:29},brandTitleSmall:{fontSize:28,letterSpacing:-.7,lineHeight:32},brandEnglish:{fontSize:16,fontWeight:'900',color:C.navy},brandEnglishCompact:{fontSize:13},brandEnglishSmall:{fontSize:14},
  tagline:{fontSize:14,lineHeight:19,color:C.blue,fontWeight:'700',textAlign:'center'},
  topbar:{height:66,flexDirection:'row',alignItems:'center',justifyContent:'space-between',marginBottom:12},backButton:{width:40,height:44,justifyContent:'center',alignItems:'flex-start'},topTitle:{fontSize:25,fontWeight:'900',color:C.navy},
  topbarCompact:{height:50,marginBottom:10},topTitleCompact:{fontSize:COMPACT_UI.title},
  primary:{height:60,borderRadius:20,alignItems:'center',justifyContent:'center'},primaryText:{color:'#fff',fontSize:20,fontWeight:'900'},
  primaryCompact:{height:'auto',minHeight:COMPACT_UI.buttonHeight,paddingVertical:10,paddingHorizontal:14,borderRadius:14},primaryTextCompact:{fontSize:COMPACT_UI.body,lineHeight:20,fontWeight:'800'},
  sectionTitle:{fontSize:16,fontWeight:'900',color:C.text2,paddingVertical:10},
});

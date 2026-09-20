import React from 'react';
import { Image, ImageSourcePropType, Pressable, StyleSheet, Text, View } from 'react-native';
import { LinearGradient } from 'expo-linear-gradient';
import { C, R } from '../theme';
import { LOGO } from '../assets';

export function Background({children}:{children:React.ReactNode}){
  return <View style={styles.root}>
    <LinearGradient colors={['#F3FCFF','#ECF9FE','#F8FDFF']} style={StyleSheet.absoluteFill}/>
    <View style={[styles.arc,{right:-190,top:-150}]}/>
    <View style={[styles.arc,{left:-230,bottom:-190,transform:[{rotate:'18deg'}]}]}/>
    {children}
  </View>;
}

export function Card({children,style}:{children:React.ReactNode;style?:any}){
  return <View style={[styles.card,style]}>{children}</View>;
}

export function Toggle({value,onChange}:{value:boolean;onChange?:()=>void}){
  return <Pressable onPress={onChange} style={[styles.toggle,value&&styles.toggleOn]}>
    <View style={[styles.knob,value&&styles.knobOn]}/>
  </Pressable>;
}

export function Badge({children,green=true}:{children:React.ReactNode;green?:boolean}){
  return <View style={[styles.badge,!green&&{backgroundColor:'#EDF1F6'}]}><Text style={[styles.badgeText,!green&&{color:'#7186A3'}]}>{children}</Text></View>;
}

export function AppIcon({source,size=56}:{source:ImageSourcePropType;size?:number}){
  return <Image source={source} style={{width:size,height:size,borderRadius:size*.23}} resizeMode="cover"/>;
}

export function SquareIcon({symbol,tone='blue',size=52}:{symbol:string;tone?:'blue'|'green'|'purple'|'red';size?:number}){
  const bg={blue:'#DCEEFF',green:'#D8FAEA',purple:'#ECE8FF',red:'#FFE6EA'}[tone];
  const color={blue:C.blue,green:C.green,purple:C.purple,red:C.red}[tone];
  return <View style={[styles.square,{width:size,height:size,borderRadius:size*.28,backgroundColor:bg}]}><Text style={[styles.squareText,{color,fontSize:size*.42}]}>{symbol}</Text></View>;
}

export function BrandHeader({title='净屏',tagline=true}:{title?:string;tagline?:boolean}){
  return <View style={styles.brandRow}>
    <Image source={LOGO} style={styles.brandLogo}/>
    <View style={{flex:1}}><Text style={styles.brandTitle}>{title}</Text><Text style={styles.brandEnglish}>Clear<Text style={{color:C.teal}}>Screen</Text></Text></View>
    {tagline?<Text style={styles.tagline}>净化屏幕{`\n`}专注生活</Text>:null}
  </View>;
}

export function TopBar({title,onBack}:{title:string;onBack:()=>void}){
  return <View style={styles.topbar}>
    <Pressable onPress={onBack} hitSlop={16}><Text style={styles.back}>‹</Text></Pressable>
    <Text style={styles.topTitle}>{title}</Text><View style={{width:36}}/>
  </View>;
}

export function PrimaryButton({title,onPress}:{title:string;onPress:()=>void}){
  return <Pressable onPress={onPress}><LinearGradient colors={['#0D69F5','#15A4EA']} start={{x:0,y:0}} end={{x:1,y:0}} style={styles.primary}><Text style={styles.primaryText}>{title}</Text></LinearGradient></Pressable>;
}

export function SectionTitle({title}:{title:string}){return <Text style={styles.sectionTitle}>{title}</Text>}
export function Chevron(){return <Text style={styles.chevron}>›</Text>}

const styles=StyleSheet.create({
  // Keep a small breathing area below the system status bar on every screen.
  // This is shared by the home, apps, settings, and secondary pages so their
  // headers stay aligned without changing the bottom navigation position.
  root:{flex:1,backgroundColor:C.bg,overflow:'hidden',paddingTop:30,paddingBottom:18},
  arc:{position:'absolute',width:500,height:500,borderRadius:250,borderWidth:42,borderColor:'rgba(78,199,233,.13)'},
  card:{backgroundColor:'rgba(255,255,255,.94)',borderRadius:R.lg,borderWidth:1,borderColor:'rgba(255,255,255,.95)',shadowColor:'#4A7EA2',shadowOpacity:.10,shadowRadius:20,shadowOffset:{width:0,height:10},elevation:3},
  toggle:{width:74,height:42,borderRadius:24,backgroundColor:C.graySwitch,padding:4,justifyContent:'center'},
  toggleOn:{backgroundColor:C.blue},
  knob:{width:34,height:34,borderRadius:17,backgroundColor:'#fff',shadowColor:'#456',shadowOpacity:.18,shadowRadius:6,shadowOffset:{width:0,height:2}},
  knobOn:{alignSelf:'flex-end'},
  badge:{alignSelf:'flex-start',backgroundColor:C.greenBg,borderRadius:20,paddingHorizontal:13,paddingVertical:6},
  badgeText:{fontSize:14,fontWeight:'800',color:C.green},
  square:{alignItems:'center',justifyContent:'center'}, squareText:{fontWeight:'900'},
  brandRow:{flexDirection:'row',alignItems:'center',gap:10,marginBottom:16}, brandLogo:{width:82,height:82},
  brandTitle:{fontSize:38,fontWeight:'900',color:C.navy,letterSpacing:-1.5,lineHeight:42},brandEnglish:{fontSize:20,fontWeight:'900',color:C.navy},
  tagline:{fontSize:18,lineHeight:25,color:'#0A70E9',fontStyle:'italic',fontWeight:'700',transform:[{rotate:'-8deg'}],textAlign:'center'},
  topbar:{height:72,flexDirection:'row',alignItems:'center',justifyContent:'space-between',marginBottom:12},back:{fontSize:54,lineHeight:54,color:'#071D62',fontWeight:'300'},topTitle:{fontSize:31,fontWeight:'900',color:'#071D62'},
  primary:{height:64,borderRadius:22,alignItems:'center',justifyContent:'center'},primaryText:{color:'#fff',fontSize:24,fontWeight:'900'},
  sectionTitle:{fontSize:15,fontWeight:'900',color:C.text2,paddingVertical:10},chevron:{fontSize:34,color:'#49688D',lineHeight:34},
});

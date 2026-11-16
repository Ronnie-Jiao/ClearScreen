import React from 'react';
import { Image, StyleSheet, Text, View } from 'react-native';
import { Background } from '../components/UI';
import { C } from '../theme';
import { LOGO, SPLASH_BACKGROUND, FONT_TAGLINE } from '../assets';

export default function SplashScreen(){
  return <Background backgroundSource={SPLASH_BACKGROUND} resizeMode="cover"><View style={s.wrap}>
    <Image source={LOGO} style={s.logo}/>
    <Text style={s.cn}>净屏</Text>
    <Text style={s.en}>Clear<Text style={{color:C.teal}}>Screen</Text></Text>
    <Text style={s.tagline}>净化屏幕 畅享生活</Text>
  </View></Background>
}
const s=StyleSheet.create({wrap:{flex:1,alignItems:'center',justifyContent:'center',paddingBottom:24},logo:{width:150,height:150},cn:{fontSize:48,fontWeight:'900',color:C.navy,marginTop:12,letterSpacing:-2},en:{fontSize:25,fontWeight:'900',color:C.navy,marginTop:-2},tagline:{marginTop:22,fontFamily:FONT_TAGLINE,fontSize:25,letterSpacing:2,color:C.text2}})

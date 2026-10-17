import React from 'react';
import { Image, StyleSheet, Text, View } from 'react-native';
import { LinearGradient } from 'expo-linear-gradient';
import { Background } from '../components/UI';
import { C } from '../theme';
import { LOGO } from '../assets';

export default function SplashScreen(){
  return <Background><View style={s.wrap}>
    <Image source={LOGO} style={s.logo}/>
    <Text style={s.cn}>净屏</Text>
    <Text style={s.en}>Clear<Text style={{color:C.teal}}>Screen</Text></Text>
    <View style={s.loader}><LinearGradient colors={['#CDEBF7','#0D72F2']} style={s.loaderRing}/><View style={s.loaderHole}/></View>
  </View></Background>
}
const s=StyleSheet.create({wrap:{flex:1,alignItems:'center',justifyContent:'center',paddingBottom:24},logo:{width:150,height:150},cn:{fontSize:48,fontWeight:'900',color:C.navy,marginTop:12,letterSpacing:-2},en:{fontSize:25,fontWeight:'900',color:C.navy,marginTop:-2},loader:{width:44,height:44,marginTop:64,alignItems:'center',justifyContent:'center'},loaderRing:{position:'absolute',width:44,height:44,borderRadius:22},loaderHole:{width:32,height:32,borderRadius:16,backgroundColor:'#F2FBFF'}})

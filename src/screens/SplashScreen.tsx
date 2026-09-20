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
    <Text style={s.tag}>净化屏幕  专注生活</Text>
    <View style={s.loader}><LinearGradient colors={['#CDEBF7','#0D72F2']} style={s.loaderRing}/><View style={s.loaderHole}/></View>
  </View></Background>
}
const s=StyleSheet.create({wrap:{flex:1,alignItems:'center',justifyContent:'center',paddingBottom:50},logo:{width:170,height:170},cn:{fontSize:58,fontWeight:'900',color:C.navy,marginTop:12,letterSpacing:-3},en:{fontSize:33,fontWeight:'900',color:C.navy,marginTop:-4},tag:{fontSize:27,fontWeight:'700',fontStyle:'italic',color:'#1474EA',marginTop:50,transform:[{rotate:'-7deg'}]},loader:{width:56,height:56,marginTop:90,alignItems:'center',justifyContent:'center'},loaderRing:{position:'absolute',width:56,height:56,borderRadius:28},loaderHole:{width:42,height:42,borderRadius:21,backgroundColor:'#F2FBFF'}})

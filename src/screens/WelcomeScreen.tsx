import React from 'react';
import { Image, Pressable, StyleSheet, Text, View } from 'react-native';
import { LinearGradient } from 'expo-linear-gradient';
import { Background, Card, PrimaryButton, SquareIcon } from '../components/UI';
import { C } from '../theme';
import { LOGO } from '../assets';

export default function WelcomeScreen({onStart,onLater}:{onStart:()=>void;onLater:()=>void}){
 return <Background><View style={s.wrap}>
  <View style={s.brand}><Image source={LOGO} style={s.logo}/><View><Text style={s.cn}>净屏</Text><Text style={s.en}>Clear<Text style={{color:C.teal}}>Screen</Text></Text></View></View>
  <Text style={s.tag}>净化屏幕  专注生活</Text>
  <View style={s.hero}>
   <LinearGradient colors={['#1672F3','#1AD6BE']} style={s.shield}><Text style={s.check}>✓</Text></LinearGradient>
   <View style={s.noAd}><Text style={s.noAdText}>AD</Text><View style={s.slash}/></View>
   <Text style={[s.float,{left:22,top:34}]}>AD −</Text><Text style={[s.float,{right:18,top:52}]}>▧ −</Text><Text style={[s.float,{left:22,bottom:28}]}>▶ −</Text>
  </View>
  <View style={s.features}>
   <Card style={s.feature}><SquareIcon symbol="≫" tone="blue" size={74}/><Text style={s.featureText}>自动跳过</Text></Card>
   <Card style={s.feature}><SquareIcon symbol="◎" tone="green" size={74}/><Text style={s.featureText}>网络过滤</Text></Card>
   <Card style={s.feature}><SquareIcon symbol="★" tone="purple" size={74}/><Text style={s.featureText}>白名单</Text></Card>
  </View>
  <View style={{width:'100%'}}><PrimaryButton title="开始使用  ›" onPress={onStart}/></View>
  <Pressable onPress={onLater}><Text style={s.later}>稍后</Text></Pressable>
 </View></Background>
}
const s=StyleSheet.create({wrap:{flex:1,paddingHorizontal:24,paddingTop:58,alignItems:'center'},brand:{flexDirection:'row',alignItems:'center',gap:12},logo:{width:100,height:100},cn:{fontSize:44,fontWeight:'900',color:C.navy},en:{fontSize:26,fontWeight:'900',color:C.navy,marginTop:-7},tag:{fontSize:27,fontWeight:'700',fontStyle:'italic',color:'#1474EA',marginTop:18,transform:[{rotate:'-6deg'}]},hero:{width:'100%',height:430,alignItems:'center',justifyContent:'center'},shield:{width:230,height:250,borderRadius:70,alignItems:'center',justifyContent:'center',transform:[{rotate:'0deg'}]},check:{fontSize:105,color:'#fff',fontWeight:'800'},noAd:{position:'absolute',right:58,bottom:54,width:126,height:126,borderRadius:63,borderWidth:13,borderColor:'#0B79F2',backgroundColor:'#fff',alignItems:'center',justifyContent:'center'},noAdText:{fontSize:48,fontWeight:'900',color:'#6682AA'},slash:{position:'absolute',width:115,height:13,borderRadius:7,backgroundColor:'#0B79F2',transform:[{rotate:'45deg'}]},float:{position:'absolute',fontSize:24,fontWeight:'900',color:'#82A0BF',opacity:.7},features:{flexDirection:'row',gap:12,width:'100%',marginBottom:26},feature:{flex:1,alignItems:'center',paddingVertical:20},featureText:{fontSize:18,fontWeight:'900',color:C.text,marginTop:12},later:{fontSize:18,fontWeight:'800',color:'#7890AF',padding:20}})

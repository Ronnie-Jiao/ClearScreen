import React from 'react';
import { Image, Pressable, StyleSheet, Text, View, useWindowDimensions } from 'react-native';
import { LinearGradient } from 'expo-linear-gradient';
import { Background, Card, PrimaryButton, SquareIcon } from '../components/UI';
import { C } from '../theme';
import { LOGO } from '../assets';

export default function WelcomeScreen({onStart,onLater}:{onStart:()=>void;onLater:()=>void}){
 const { width, height } = useWindowDimensions();
 const scale = Math.min(1, Math.max(.84, Math.min(width / 360, height / 800)));
 const px = (value:number) => Math.round(value * scale);
 return <Background><View style={[s.wrap,{paddingHorizontal:px(24),paddingTop:px(36),paddingBottom:px(2)}]}>
  <View style={[s.brand,{gap:px(11)}]}><Image source={LOGO} style={{width:px(92),height:px(92)}}/><View><Text style={[s.cn,{fontSize:px(42),lineHeight:px(46)}]}>净屏</Text><Text style={[s.en,{fontSize:px(25),lineHeight:px(29),marginTop:-px(6)}]}>Clear<Text style={{color:C.teal}}>Screen</Text></Text></View></View>
  <Text style={[s.tag,{fontSize:px(26),lineHeight:px(32),marginTop:px(12)}]}>净化屏幕  专注生活</Text>
  <View style={[s.hero,{height:px(338)}]}>
   <LinearGradient colors={['#1672F3','#1AD6BE']} style={[s.shield,{width:px(220),height:px(240),borderRadius:px(66)}]}><Text style={[s.check,{fontSize:px(98)}]}>✓</Text></LinearGradient>
   <View style={[s.noAd,{right:px(54),bottom:px(42),width:px(116),height:px(116),borderRadius:px(58),borderWidth:px(12)}]}><Text style={[s.noAdText,{fontSize:px(44)}]}>AD</Text><View style={[s.slash,{width:px(106),height:px(12),borderRadius:px(7)}]}/></View>
   <Text style={[s.float,{left:px(22),top:px(34),fontSize:px(22)}]}>AD −</Text><Text style={[s.float,{right:px(18),top:px(52),fontSize:px(22)}]}>▧ −</Text><Text style={[s.float,{left:px(22),bottom:px(28),fontSize:px(22)}]}>▶ −</Text>
  </View>
  <View style={[s.features,{gap:px(11),marginBottom:px(14)}]}>
   <Card style={[s.feature,{paddingVertical:px(12)}]}><SquareIcon symbol="≫" tone="blue" size={px(70)}/><Text style={[s.featureText,{fontSize:px(17),lineHeight:px(22),marginTop:px(8)}]}>自动跳过</Text></Card>
   <Card style={[s.feature,{paddingVertical:px(12)}]}><SquareIcon symbol="◎" tone="green" size={px(70)}/><Text style={[s.featureText,{fontSize:px(17),lineHeight:px(22),marginTop:px(8)}]}>网络过滤</Text></Card>
   <Card style={[s.feature,{paddingVertical:px(12)}]}><SquareIcon symbol="★" tone="purple" size={px(70)}/><Text style={[s.featureText,{fontSize:px(17),lineHeight:px(22),marginTop:px(8)}]}>白名单</Text></Card>
  </View>
  <View style={{width:'100%'}}><PrimaryButton title="开始使用  ›" onPress={onStart}/></View>
  <Pressable onPress={onLater}><Text style={[s.later,{fontSize:px(17),lineHeight:px(22),paddingVertical:px(6),paddingHorizontal:px(12)}]}>稍后</Text></Pressable>
 </View></Background>
}
const s=StyleSheet.create({wrap:{flex:1,alignItems:'center',overflow:'hidden'},brand:{flexDirection:'row',alignItems:'center'},cn:{fontWeight:'900',color:C.navy},en:{fontWeight:'900',color:C.navy},tag:{fontWeight:'700',fontStyle:'italic',color:'#1474EA',transform:[{rotate:'-6deg'}]},hero:{width:'100%',alignItems:'center',justifyContent:'center'},shield:{alignItems:'center',justifyContent:'center',transform:[{rotate:'0deg'}]},check:{color:'#fff',fontWeight:'800'},noAd:{position:'absolute',borderColor:'#0B79F2',backgroundColor:'#fff',alignItems:'center',justifyContent:'center'},noAdText:{fontWeight:'900',color:'#6682AA'},slash:{position:'absolute',backgroundColor:'#0B79F2',transform:[{rotate:'45deg'}]},float:{position:'absolute',fontWeight:'900',color:'#82A0BF',opacity:.7},features:{flexDirection:'row',width:'100%'},feature:{flex:1,alignItems:'center'},featureText:{fontWeight:'900',color:C.text},later:{fontWeight:'800',color:'#7890AF'}})

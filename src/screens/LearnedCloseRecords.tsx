import React from 'react';
import { Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { AppItem, LearnedCloseRecord } from '../data';
import { AppIcon, Background, Card, TopBar } from '../components/UI';
import { C, COMPACT_UI } from '../theme';

function recordTime(value: number) {
  if (!Number.isFinite(value) || value <= 0) return '时间未知';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '时间未知';
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth()+1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
}

export function LearnedCloseRow({record, apps, onDelete, compact=false}: {
  record: LearnedCloseRecord; apps: AppItem[]; onDelete: (record: LearnedCloseRecord) => void; compact?: boolean;
}) {
  const app = apps.find(item => item.id === record.appId || item.packageName === record.appId);
  const name = app?.name || record.appId || '未知应用';
  return <View style={[s.row, compact && s.compactRow]}>
    {app ? <AppIcon source={app.icon} size={compact ? 36 : 40}/> : <View style={s.fallback}><Ionicons name="apps-outline" size={21} color={C.blue}/></View>}
    <View style={s.nameBox}><Text style={s.name} numberOfLines={1}>{name}</Text><Text style={s.time}>{recordTime(record.timestamp)}{record.needsRelearn ? ' · 需重新学习' : ''}</Text></View>
    <Pressable accessibilityRole="button" accessibilityLabel={`删除${name}的自定义关闭记录`} onPress={() => onDelete(record)} style={s.deleteButton} hitSlop={5}>
      <Ionicons name="trash-outline" size={20} color={C.red}/>
    </Pressable>
  </View>;
}

export default function LearnedCloseRecords({records, apps, onBack, onDelete}: {
  records: LearnedCloseRecord[]; apps: AppItem[]; onBack: () => void; onDelete: (record: LearnedCloseRecord) => void;
}) {
  return <Background><ScrollView contentContainerStyle={s.page} showsVerticalScrollIndicator={false}>
    <TopBar compact title="自定义关闭窗口" onBack={onBack}/>
    <Card compact style={s.card}>{records.length ? records.map(record =>
      <LearnedCloseRow key={record.id} record={record} apps={apps} onDelete={onDelete} compact/>
    ) : <Text style={s.empty}>暂无自定义关闭记录</Text>}</Card>
  </ScrollView></Background>;
}

const s = StyleSheet.create({
  page:{flexGrow:1,paddingHorizontal:18,paddingBottom:30},card:{paddingHorizontal:10},
  row:{minHeight:60,flexDirection:'row',alignItems:'center',gap:9,borderBottomWidth:1,borderBottomColor:C.line},
  compactRow:{minHeight:COMPACT_UI.rowHeight},nameBox:{flex:1,minWidth:0},
  name:{fontSize:14,fontWeight:'800',color:C.text},time:{fontSize:12,color:C.text2,marginTop:2},
  fallback:{width:40,height:40,borderRadius:10,alignItems:'center',justifyContent:'center',backgroundColor:'#EAF4FF'},
  deleteButton:{width:44,height:44,alignItems:'center',justifyContent:'center'},
  empty:{paddingVertical:20,textAlign:'center',fontSize:COMPACT_UI.body,fontWeight:'700',color:C.text2},
});

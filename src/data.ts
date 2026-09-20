import { ImageSourcePropType } from 'react-native';
import { APP_ICONS } from './assets';

export type AppItem = {
  id: string;
  name: string;
  icon: ImageSourcePropType;
  skip: boolean;
  network: boolean;
  whitelist: boolean;
};

export const APPS: AppItem[] = [
  { id: 'xhs', name: '小红书', icon: APP_ICONS.xhs, skip: true, network: true, whitelist: false },
  { id: 'taobao', name: '淘宝', icon: APP_ICONS.taobao, skip: true, network: false, whitelist: false },
  { id: 'douyin', name: '抖音', icon: APP_ICONS.douyin, skip: true, network: true, whitelist: false },
  { id: 'weibo', name: '微博', icon: APP_ICONS.weibo, skip: false, network: true, whitelist: false },
  { id: 'bilibili', name: '哔哩哔哩', icon: APP_ICONS.bilibili, skip: false, network: false, whitelist: true },
  { id: 'zhihu', name: '知乎', icon: APP_ICONS.zhihu, skip: true, network: false, whitelist: false },
  { id: 'wechat', name: '微信', icon: APP_ICONS.wechat, skip: false, network: true, whitelist: true },
  { id: 'qq', name: 'QQ', icon: APP_ICONS.qq, skip: true, network: true, whitelist: false },
  { id: 'alipay', name: '支付宝', icon: APP_ICONS.alipay, skip: true, network: false, whitelist: false },
  { id: 'meituan', name: '美团', icon: APP_ICONS.meituan, skip: true, network: true, whitelist: true },
  { id: 'dingding', name: '钉钉', icon: APP_ICONS.dingding, skip: true, network: false, whitelist: false },
  { id: 'didi', name: '滴滴出行', icon: APP_ICONS.didi, skip: false, network: true, whitelist: false },
];

export type LogItem = { appId: string; time: string; type: 'skip'|'network'|'fail'; text: string };
export const LOGS: LogItem[] = [
  {appId:'xhs',time:'08:32',type:'skip',text:'已跳过开屏广告'},
  {appId:'taobao',time:'09:05',type:'network',text:'已拦截广告请求'},
  {appId:'douyin',time:'09:18',type:'fail',text:'处理失败'},
  {appId:'alipay',time:'10:24',type:'skip',text:'已跳过开屏广告'},
  {appId:'wechat',time:'11:03',type:'network',text:'已拦截广告请求'},
  {appId:'meituan',time:'12:17',type:'skip',text:'已跳过开屏广告'},
  {appId:'didi',time:'14:26',type:'fail',text:'处理失败'},
  {appId:'bilibili',time:'15:41',type:'network',text:'已拦截广告请求'},
  {appId:'douyin',time:'16:28',type:'skip',text:'已跳过开屏广告'},
  {appId:'xhs',time:'18:03',type:'network',text:'已拦截广告请求'},
];

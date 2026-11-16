import React, { createContext, useCallback, useContext, useRef, useState } from 'react';
import { Image, ImageSourcePropType, Modal, Pressable, ScrollView, StyleSheet, Text, useWindowDimensions, View } from 'react-native';
import { LinearGradient } from 'expo-linear-gradient';
import { Ionicons } from '@expo/vector-icons';
import { C } from '../theme';
import { DIALOG_TONE_ERROR, DIALOG_TONE_INFO, DIALOG_TONE_SUCCESS, DIALOG_TONE_WARNING } from '../assets';

type DialogTone = 'info' | 'success' | 'warning' | 'error';
export type DialogButton = {
  text?: string;
  style?: 'default' | 'cancel' | 'destructive';
  icon?: keyof typeof Ionicons.glyphMap;
  onPress?: () => void;
};
type DialogOptions = { cancelable?: boolean; onDismiss?: () => void; tone?: DialogTone };
type ShowAlert = (title: string, message?: string, buttons?: DialogButton[], options?: DialogOptions) => void;
type DialogRequest = { id: number; title: string; message?: string; buttons: DialogButton[]; options?: DialogOptions };

const AlertContext = createContext<ShowAlert | null>(null);
const TONE_ICONS: Record<DialogTone, ImageSourcePropType> = {
  info: DIALOG_TONE_INFO,
  success: DIALOG_TONE_SUCCESS,
  warning: DIALOG_TONE_WARNING,
  error: DIALOG_TONE_ERROR,
};

/** 按按钮文案推断图标；没有把握的文案不加图标。 */
function iconForButton(button: DialogButton): keyof typeof Ionicons.glyphMap | undefined {
  const text = button.text || '确定';
  if (/取消|关闭|返回/.test(text)) return 'close-outline';
  if (/稍后|以后|暂不/.test(text)) return 'time-outline';
  if (/重试|重新|刷新|恢复|重置/.test(text)) return 'refresh-outline';
  if (/删除|清除|清空|移除|卸载/.test(text)) return 'trash-outline';
  if (/连接|连上|配对/.test(text)) return 'link-outline';
  if (/授权|授予|允许/.test(text)) return 'shield-checkmark-outline';
  if (/去|前往|打开|开启|设置/.test(text)) return 'open-outline';
  if (/确定|完成|好的|知道了|继续|应用|保存/.test(text)) return 'checkmark-outline';
  return undefined;
}

export function AppDialog({ visible, title, message, children, buttons, tone = 'info', cancelable = false, onRequestClose }: {
  visible: boolean;
  title: string;
  message?: string;
  children?: React.ReactNode;
  buttons: DialogButton[];
  tone?: DialogTone;
  cancelable?: boolean;
  onRequestClose: () => void;
}) {
  const { height, width, fontScale } = useWindowDimensions();
  const stacked = buttons.length > 2 || fontScale > 1.25 || buttons.some(button => (button.text?.length || 0) > 6) || width < 340;
  const dismiss = () => { if (cancelable) onRequestClose(); };

  return <Modal visible={visible} transparent animationType="fade" statusBarTranslucent navigationBarTranslucent onRequestClose={dismiss}>
    <View style={s.root}>
      <Pressable accessible={cancelable} accessibilityRole="button" accessibilityLabel="关闭提示" onPress={dismiss} style={s.backdrop}/>
      <View accessibilityViewIsModal onAccessibilityEscape={dismiss} style={[s.card, { maxHeight: height - 64 }]}>
        <ScrollView style={s.scroll} contentContainerStyle={s.content} showsVerticalScrollIndicator={false} bounces={false}>
          <View style={s.heading}>
            <View importantForAccessibility="no-hide-descendants" style={s.icon}>
              <Image source={TONE_ICONS[tone]} style={s.iconImg} accessibilityIgnoresInvertColors/>
            </View>
            <Text accessibilityRole="header" style={s.title}>{title}</Text>
          </View>
          {message || children ? <View style={s.body}>
            {message ? <Text style={s.message}>{message}</Text> : null}
            {children}
          </View> : null}
        </ScrollView>
        <View style={[s.actions, stacked && s.actionsStacked]}>
          {buttons.map((button, index) => {
            const secondary = button.style === 'cancel' || (buttons.length > 1 && index < buttons.length - 1 && button.style !== 'destructive');
            const colors = ['#0A69DD', '#0877CC'] as const;
            const label = button.text || '确定';
            const icon = button.icon ?? iconForButton(button);
            return <Pressable key={`${index}-${label}`} accessibilityRole="button" onPress={button.onPress} style={({ pressed }) => [s.button, !stacked && s.buttonRow, pressed && s.pressed]}>
              {secondary ? <View style={[s.buttonFill, s.secondary]}>{icon ? <Ionicons name={icon} size={15} color={C.text2}/> : null}<Text style={[s.buttonText, s.secondaryText]}>{label}</Text></View> :
                <LinearGradient colors={colors} start={{ x: 0, y: 0 }} end={{ x: 1, y: 1 }} style={s.buttonFill}>{icon ? <Ionicons name={icon} size={15} color="#FFFFFF"/> : null}<Text style={s.buttonText}>{label}</Text></LinearGradient>}
            </Pressable>;
          })}
        </View>
      </View>
    </View>
  </Modal>;
}

export function AppDialogProvider({ children }: { children: React.ReactNode }) {
  const [requests, setRequests] = useState<DialogRequest[]>([]);
  const nextId = useRef(0);
  const handledId = useRef(0);
  const lastRequest = useRef<DialogRequest | undefined>(undefined);
  const showAlert = useCallback<ShowAlert>((title, message, buttons, options) => {
    const request = { id: ++nextId.current, title, message, buttons: buttons?.length ? buttons : [{ text: '确定' }], options };
    setRequests(current => [...current, request]);
  }, []);
  const finish = (request: DialogRequest, callback?: () => void) => {
    if (request.id <= handledId.current) return;
    handledId.current = request.id;
    setRequests(current => current[0]?.id === request.id ? current.slice(1) : current);
    callback?.();
  };
  const current = requests[0];
  if (current) lastRequest.current = current;
  // Retain the contents while the native modal fades out.
  const displayed = current || lastRequest.current;

  return <AlertContext.Provider value={showAlert}>
    {children}
    {displayed ? <AppDialog
      visible={!!current}
      title={displayed.title}
      message={displayed.message}
      tone={displayed.options?.tone || (displayed.buttons.some(button => button.style === 'destructive') ? 'warning' : 'info')}
      cancelable={displayed.options?.cancelable}
      onRequestClose={() => finish(displayed, displayed.options?.onDismiss)}
      buttons={displayed.buttons.map(button => ({ ...button, onPress: () => finish(displayed, button.onPress) }))}
    /> : null}
  </AlertContext.Provider>;
}

export function useAppAlert(): ShowAlert {
  const showAlert = useContext(AlertContext);
  if (!showAlert) throw new Error('useAppAlert requires AppDialogProvider');
  return showAlert;
}

const s = StyleSheet.create({
  root: { flex: 1, justifyContent: 'center', alignItems: 'center', paddingHorizontal: 24, paddingVertical: 32 },
  backdrop: { ...StyleSheet.absoluteFillObject, backgroundColor: 'rgba(8,26,52,0.42)' },
  card: { width: '100%', maxWidth: 400, backgroundColor: C.surface, borderRadius: 22, borderWidth: 1, borderColor: 'rgba(255,255,255,0.9)', shadowColor: '#092868', shadowOpacity: 0.18, shadowRadius: 28, shadowOffset: { width: 0, height: 14 }, elevation: 20 },
  scroll: { flexGrow: 0, flexShrink: 1 },
  content: { padding: 18 },
  heading: { flexDirection: 'row', alignItems: 'center', gap: 11 },
  icon: { width: 38, height: 38 },
  iconImg: { width: 38, height: 38 },
  title: { flex: 1, fontSize: 17, lineHeight: 24, fontWeight: '800', color: C.navy },
  body: { marginTop: 11 },
  message: { fontSize: 13, lineHeight: 20, fontWeight: '500', color: C.text2 },
  actions: { flexDirection: 'row', gap: 10, paddingHorizontal: 18, paddingBottom: 18 },
  actionsStacked: { flexDirection: 'column' },
  button: { borderRadius: 13, overflow: 'hidden' },
  buttonRow: { flex: 1 },
  buttonFill: { minHeight: 42, paddingVertical: 10, paddingHorizontal: 12, flexDirection: 'row', alignItems: 'center', justifyContent: 'center', gap: 5 },
  buttonText: { fontSize: 13, lineHeight: 19, fontWeight: '700', color: '#FFFFFF', textAlign: 'center' },
  secondary: { backgroundColor: '#EDF4FA', borderWidth: 1, borderColor: 'rgba(11,120,242,0.10)' },
  secondaryText: { color: C.text2 },
  pressed: { opacity: 0.78 },
});

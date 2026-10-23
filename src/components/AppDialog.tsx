import React, { createContext, useCallback, useContext, useRef, useState } from 'react';
import { Modal, Pressable, ScrollView, StyleSheet, Text, useWindowDimensions, View } from 'react-native';
import { LinearGradient } from 'expo-linear-gradient';
import { Ionicons } from '@expo/vector-icons';
import { C } from '../theme';

type DialogTone = 'info' | 'success' | 'warning' | 'error';
export type DialogButton = {
  text?: string;
  style?: 'default' | 'cancel' | 'destructive';
  onPress?: () => void;
};
type DialogOptions = { cancelable?: boolean; onDismiss?: () => void; tone?: DialogTone };
type ShowAlert = (title: string, message?: string, buttons?: DialogButton[], options?: DialogOptions) => void;
type DialogRequest = { id: number; title: string; message?: string; buttons: DialogButton[]; options?: DialogOptions };

const AlertContext = createContext<ShowAlert | null>(null);
const TONES: Record<DialogTone, { icon: keyof typeof Ionicons.glyphMap; color: string; background: string }> = {
  info: { icon: 'information-circle-outline', color: C.blue, background: '#EAF4FF' },
  success: { icon: 'checkmark-circle-outline', color: '#079E72', background: '#E5F8F1' },
  warning: { icon: 'alert-circle-outline', color: '#CF7520', background: '#FFF3E4' },
  error: { icon: 'close-circle-outline', color: '#DF1645', background: '#FFF0F3' },
};

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
  const palette = TONES[tone];
  const dismiss = () => { if (cancelable) onRequestClose(); };

  return <Modal visible={visible} transparent animationType="fade" statusBarTranslucent navigationBarTranslucent onRequestClose={dismiss}>
    <View style={s.root}>
      <Pressable accessible={cancelable} accessibilityRole="button" accessibilityLabel="关闭提示" onPress={dismiss} style={s.backdrop}/>
      <View accessibilityViewIsModal onAccessibilityEscape={dismiss} style={[s.card, { maxHeight: height - 64 }]}>
        <ScrollView style={s.scroll} contentContainerStyle={s.content} showsVerticalScrollIndicator={false} bounces={false}>
          <View style={s.heading}>
            <View importantForAccessibility="no-hide-descendants" style={[s.icon, { backgroundColor: palette.background }]}>
              <Ionicons name={palette.icon} size={27} color={palette.color}/>
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
            const colors = button.style === 'destructive' ? ['#E62050', '#CC1642'] as const : ['#0A69DD', '#0877CC'] as const;
            const label = button.text || '确定';
            return <Pressable key={`${index}-${label}`} accessibilityRole="button" onPress={button.onPress} style={({ pressed }) => [s.button, !stacked && s.buttonRow, pressed && s.pressed]}>
              {secondary ? <View style={[s.buttonFill, s.secondary]}><Text style={[s.buttonText, s.secondaryText]}>{label}</Text></View> :
                <LinearGradient colors={colors} start={{ x: 0, y: 0 }} end={{ x: 1, y: 1 }} style={s.buttonFill}><Text style={s.buttonText}>{label}</Text></LinearGradient>}
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
  card: { width: '100%', maxWidth: 400, backgroundColor: C.surface, borderRadius: 26, borderWidth: 1, borderColor: 'rgba(255,255,255,0.9)', shadowColor: '#092868', shadowOpacity: 0.18, shadowRadius: 28, shadowOffset: { width: 0, height: 14 }, elevation: 20 },
  scroll: { flexGrow: 0, flexShrink: 1 },
  content: { padding: 22 },
  heading: { flexDirection: 'row', alignItems: 'center', gap: 13 },
  icon: { width: 46, height: 46, borderRadius: 16, alignItems: 'center', justifyContent: 'center' },
  title: { flex: 1, fontSize: 20, lineHeight: 28, fontWeight: '900', color: C.navy },
  body: { marginTop: 16 },
  message: { fontSize: 15, lineHeight: 24, fontWeight: '500', color: C.text2 },
  actions: { flexDirection: 'row', gap: 10, paddingHorizontal: 22, paddingBottom: 22 },
  actionsStacked: { flexDirection: 'column' },
  button: { borderRadius: 15, overflow: 'hidden' },
  buttonRow: { flex: 1 },
  buttonFill: { minHeight: 48, paddingVertical: 13, paddingHorizontal: 12, alignItems: 'center', justifyContent: 'center' },
  buttonText: { fontSize: 15, lineHeight: 22, fontWeight: '800', color: '#FFFFFF', textAlign: 'center' },
  secondary: { backgroundColor: '#EDF4FA' },
  secondaryText: { color: C.text2 },
  pressed: { opacity: 0.78 },
});

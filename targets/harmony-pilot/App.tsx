import { StatusBar } from 'expo-status-bar';
import { Platform, StyleSheet, Text, View } from 'react-native';

export default function App() {
  return (
    <View style={styles.container}>
      <Text style={styles.title}>expo-harmony</Text>
      <Text style={styles.subtitle}>Expo SDK 55 + HarmonyOS（RNOH 0.84）</Text>
      <Text style={styles.platform}>Platform.OS = {Platform.OS}</Text>
      <StatusBar style="auto" />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#fff',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 8,
  },
  title: {
    fontSize: 28,
    fontWeight: '700',
    color: '#0A59F7',
  },
  subtitle: {
    fontSize: 14,
    color: '#333',
  },
  platform: {
    fontSize: 12,
    color: '#999',
    marginTop: 8,
  },
});

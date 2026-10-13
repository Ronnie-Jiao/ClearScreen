import React from 'react';
import { View, type StyleProp, type ViewStyle } from 'react-native';

type LinearGradientProps = React.PropsWithChildren<{
  colors: readonly string[];
  start?: { x: number; y: number };
  end?: { x: number; y: number };
  style?: StyleProp<ViewStyle>;
}>;

export function LinearGradient({ colors, style, children }: LinearGradientProps) {
  return (
    <View style={[{ backgroundColor: colors[0] ?? 'transparent' }, style]}>
      {children}
    </View>
  );
}

declare module 'expo-linear-gradient' {
  import type { ComponentType, PropsWithChildren } from 'react';
  import type { ViewProps } from 'react-native';

  type Point = { x: number; y: number };

  export const LinearGradient: ComponentType<
    PropsWithChildren<
      ViewProps & {
        colors: readonly string[];
        start?: Point;
        end?: Point;
      }
    >
  >;
}

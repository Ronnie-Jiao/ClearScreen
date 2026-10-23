import { createContext } from 'react';

// The Android/iOS entry point supplies native insets; shared UI stays platform independent.
export const ScreenInsetsContext = createContext({ top: 0 });

export const SCREEN_TOP_GAP = 16;

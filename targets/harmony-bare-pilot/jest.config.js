module.exports = {
  preset: 'react-native',
  moduleNameMapper: {
    '^react$': '<rootDir>/node_modules/react',
    '^react-native$': '<rootDir>/node_modules/react-native',
    '^react-native/(.*)$': '<rootDir>/node_modules/react-native/$1',
    '^expo-linear-gradient$': '<rootDir>/LinearGradientShim.tsx',
  },
};

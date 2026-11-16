import { ImageSourcePropType } from 'react-native';

export const LOGO: ImageSourcePropType = require('../assets/logo.png');
export const UI_BACKGROUND: ImageSourcePropType = require('../assets/ui-background.png');
export const SPLASH_BACKGROUND: ImageSourcePropType = require('../assets/splash-background.png');

// Handwritten signature-style brand tagline font (ZhiMangXing 志莽行书 subset, 2026-11-16;
// replaces the earlier MaShanZheng subset per user feedback — user wanted a "signature" look).
// NOTE: subset only contains the slogan glyphs 「净化屏幕 畅享生活」(+「净屏ClearScreen」、punctuation).
// If the tagline copy changes, re-subset the font to include the new characters.
export const FONT_TAGLINE = 'BrandTaglineHand';
export const FONT_TAGLINE_SOURCE = require('../assets/fonts/brand-tagline-zhimangxing.ttf');

// Transparent PNG art supplied with the 2026-11-15 home screen design.
export const HOME_PAUSE: ImageSourcePropType = require('../assets/home/pause-circle.png');
export const HOME_PLAY: ImageSourcePropType = require('../assets/home/play-circle.png');
export const HOME_HISTORY: ImageSourcePropType = require('../assets/home/icon-history.png');
export const HOME_LAYERS: ImageSourcePropType = require('../assets/home/icon-layers.png');
export const HOME_SCAN: ImageSourcePropType = require('../assets/home/icon-scan.png');
export const HOME_REFERENCE_SHIELD_WORKING: ImageSourcePropType = require('../assets/home/reference-shield-working.png');
export const HOME_REFERENCE_SHIELD_PAUSED: ImageSourcePropType = require('../assets/home/reference-shield-paused.png');

// Transparent PNG art supplied with the 2026-11-15 ad-management design.
export const ADS_APP_MANAGEMENT: ImageSourcePropType = require('../assets/ads/app-management.png');
export const ADS_SYSTEM_RECOMMEND: ImageSourcePropType = require('../assets/ads/system-recommendations.png');
export const ADS_APP_NOTIFICATIONS: ImageSourcePropType = require('../assets/ads/app-notifications.png');
export const ADS_MARKETING_NOTIFICATIONS: ImageSourcePropType = require('../assets/ads/marketing-notifications.png');
export const ADS_LEARNED_CLOSE: ImageSourcePropType = require('../assets/ads/learned-close.png');
export const ADS_SHIELD: ImageSourcePropType = require('../assets/ads/shield-hologram.png');

// Dialog tone icons in the same rounded-square style as the ads icons (2026-11-16).
export const DIALOG_TONE_INFO: ImageSourcePropType = require('../assets/dialog/tone-info.png');
export const DIALOG_TONE_SUCCESS: ImageSourcePropType = require('../assets/dialog/tone-success.png');
export const DIALOG_TONE_WARNING: ImageSourcePropType = require('../assets/dialog/tone-warning.png');
export const DIALOG_TONE_ERROR: ImageSourcePropType = require('../assets/dialog/tone-error.png');

// Rounded transparent row icons supplied with the 2026-11-15 service & permissions design.
export const PERM_AUTO_SKIP: ImageSourcePropType = require('../assets/permissions/auto-skip.png');
export const PERM_STARTUP: ImageSourcePropType = require('../assets/permissions/startup.png');
export const PERM_BACKGROUND: ImageSourcePropType = require('../assets/permissions/background.png');
export const PERM_BATTERY: ImageSourcePropType = require('../assets/permissions/battery.png');
export const PERM_SYSTEM_SETTINGS: ImageSourcePropType = require('../assets/permissions/system-settings.png');

// Light settings artwork supplied with the 2026-11-15 settings screen design.
export const SETTINGS_GEAR: ImageSourcePropType = require('../assets/settings/hero-gear.png');
export const SETTINGS_SERVICE: ImageSourcePropType = require('../assets/settings/service.png');
export const SETTINGS_PRIVACY: ImageSourcePropType = require('../assets/settings/privacy.png');
export const SETTINGS_RESTORE: ImageSourcePropType = require('../assets/settings/restore.png');
export const SETTINGS_GUIDE: ImageSourcePropType = require('../assets/settings/guide.png');
export const SETTINGS_ABOUT: ImageSourcePropType = require('../assets/settings/about.png');

export const APP_ICONS = {
  xhs: require('../assets/apps/xhs.png'),
  taobao: require('../assets/apps/taobao.png'),
  douyin: require('../assets/apps/douyin.png'),
  weibo: require('../assets/apps/weibo.png'),
  bilibili: require('../assets/apps/bilibili.png'),
  zhihu: require('../assets/apps/zhihu.png'),
  wechat: require('../assets/apps/wechat.png'),
  qq: require('../assets/apps/qq.png'),
  alipay: require('../assets/apps/alipay.png'),
  meituan: require('../assets/apps/meituan.png'),
  dingding: require('../assets/apps/dingding.png'),
  didi: require('../assets/apps/didi.png'),
} as const;

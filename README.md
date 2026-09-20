# 净屏 ClearScreen

高保真 React Native + Expo 前端原型，当前以 **Android** 为目标平台。仓库同时保留早期 Hatchable Web 原型在 `public/` 目录。

## 当前状态

- Android 前端高保真 UI 已实现
- iOS / HarmonyOS 暂不开发
- 不包含账号、会员、订阅或支付
- AccessibilityService、VpnService 等 Android 系统层能力尚未接入

## 已实现页面

1. 启动页
2. 首次使用 / 欢迎页
3. 权限开启页
4. 首页
5. 应用列表
6. 应用详情
7. 记录
8. 设置
9. 外观
10. 白名单

## 已实现交互

- 启动页自动进入欢迎页
- 首页 / 应用 / 设置三 Tab 切换
- 总开关
- 权限状态开关
- App 自动跳过开关
- App 详情自动跳过 / 网络过滤 / 白名单
- 应用搜索与筛选
- 记录筛选
- 设置开关
- 外观主题选择和动效开关
- 白名单筛选、启用与保存
- 返回历史栈

## 运行

需要 Node.js、npm 和 Expo 环境。

```bash
npm install
npm run start
```

Android：

```bash
npm run android
```

## 目录

```text
ClearScreen/
├─ App.tsx
├─ src/
│  ├─ components/
│  ├─ screens/
│  ├─ assets.ts       # 内嵌 Logo / App 图标，便于 GitHub 单仓库同步
│  ├─ data.ts
│  └─ theme.ts
├─ public/             # 早期 Hatchable Web 交互原型
├─ package.json
├─ app.json
└─ tsconfig.json
```

## 视觉

- 主色：`#0B78F2`
- 青色：`#14B8B0`
- 深蓝文字：`#092868`
- 浅蓝背景：`#F4FCFF`
- 大圆角白色卡片、蓝青渐变、柔和阴影

页面按已确认的高保真 UI 图进行布局还原，使用响应式尺寸适配常见 Android 手机纵向屏幕。

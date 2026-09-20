# ClearScreen / 净屏

净屏 ClearScreen 的高保真可交互原型代码。

## 当前范围
- Android 版本原型
- 首页 / 应用 / 应用详情 / 记录 / 设置 / 外观 / 白名单 / 规则管理
- 启动页、首次使用与权限引导
- 当前不包含账号、会员、订阅或支付
- iOS 与 HarmonyOS 暂不开发

## 本地预览
```bash
cd public
python -m http.server 8000
```

然后浏览器打开 `http://localhost:8000`。

## 文件
- `public/index.html`：入口
- `public/app.js`：页面与交互
- `public/theme.css`：视觉样式
- `public/logo.svg`：Logo

原型源自 Hatchable 项目，用于验证界面结构与交互流程，不代表 Android 系统层能力已经实现。

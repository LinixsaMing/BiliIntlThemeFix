# BiliIntlThemeFix

修复哔哩哔哩国际版（`com.bilibili.app.in`）冷启动后个性装扮主题被重置的问题。

## 问题

应用无法拉取正常配置，重启时会被默认白色主题覆盖。

## 做法

只拦住「网络回包覆盖本地配置」这一步，其余逻辑不动。

## 使用

1. 下载 [Releases](../../releases) 里的 APK
2. 在 LSPosed / LSPatch / NPatch 中启用，作用域选 `com.bilibili.app.in`
3. 重启 App，重新选一次主题

之后进入 **设置 → BiliIntlThemeFix** 可以开关和查看信息。

## 支持的版本

- 宿主：哔哩哔哩国际版（已验证 3.20.4）
- 框架：LSPosed / LSPatch / NPatch（传统 Xposed API）
- 系统：Android 8.0+

## 声明

仅阻止云端空数据覆盖本地已拥有的装扮。不含宿主任何代码或资源，不破解、不解锁、不绕过付费内容，与哔哩哔哩官方无关。

## 附言

AI神力了属于是

## 协议

GPL-3.0

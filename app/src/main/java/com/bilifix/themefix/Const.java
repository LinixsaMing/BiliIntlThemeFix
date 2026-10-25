package com.bilifix.themefix;

/** 模块常量。改包名支持国内版时从这里入手。 */
public final class Const {
    public static final String HOST_PKG = "com.bilibili.app.in";
    public static final String TAG = "BiliIntlThemeFix";
    public static final String GARB_PKG = "tv.danmaku.bili.ui.garb";
    public static final String GARB_DATA = "tv.danmaku.bili.ui.garb.model.GarbData";
    public static final String ACTIVITY = "android.app.Activity";
    public static final String VERSION = "0.3.4";

    /** 开发者 */
    public static final String AUTHOR_NAME = "LinixsaMing";

    /** 仓库地址 */
    public static final String GITHUB_URL = "https://github.com/LinixsaMing/BiliIntlThemeFix";

    public static final String ABOUT =
            "开发者：" + AUTHOR_NAME + "\n\n"
            + "作用：阻止哔哩哔哩国际版在冷启动时用云端空数据覆盖本地已购买的个性装扮主题。\n\n"
            + "原理：只拦截「网络回包 → 应用装扮」这一次调用，启动时读取本地装扮、"
            + "手动切换主题等逻辑全部保持原样。\n\n"
            + "声明：本模块不含宿主任何代码或资源，不破解、不解锁、不绕过付费内容，"
            + "与哔哩哔哩官方无关。\n\n"
            + "开源协议：GPL-3.0";

    private Const() {}
}

package com.bilifix.themefix;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

/**
 * 在宿主「设置」页注入一条原生 Preference。
 *
 * 注意：宿主的 androidx.preference 被混淆过，OnPreferenceClickListener 改名成
 * androidx.preference.Preference$d，直接反射那个接口会 ClassNotFound。
 * 所以这里不给 Preference 挂监听器，而是 hook Fragment 的
 * onPreferenceTreeClick(Preference)Z —— 点击统一从这里出来。
 */
public final class SettingsInjector {

    private static final String FRAG =
            "com.bilibili.app.preferences.BiliPreferencesActivity$BiliPreferencesFragment";
    private static final String PREF_CLS = "androidx.preference.Preference";
    private static final String KEY = "bilifix_theme_entry";
    private static final String TITLE = "BiliIntlThemeFix";

    private static WeakReference<Object> sEntry = new WeakReference<>(null);

    public static void install(final ClassLoader cl) {
        try {
            Class<?> frag = Class.forName(FRAG, false, cl);
            Class<?> bundle = Class.forName("android.os.Bundle", false, cl);
            Class<?> prefCls = Class.forName(PREF_CLS, false, cl);

            Method onCreate = frag.getMethod("onCreatePreferences", bundle, String.class);
            XposedBridge.hookMethod(onCreate, new XC_MethodHook() {
                @Override protected void afterHookedMethod(MethodHookParam p) {
                    try { inject(p.thisObject, cl); }
                    catch (Throwable t) { Logx.e("settings inject failed", t); }
                }
            });

            Method click = frag.getMethod("onPreferenceTreeClick", prefCls);
            XposedBridge.hookMethod(click, new XC_MethodHook() {
                @Override protected void beforeHookedMethod(MethodHookParam p) {
                    try {
                        Object pref = p.args != null && p.args.length > 0 ? p.args[0] : null;
                        if (pref == null) return;
                        Object key = pref.getClass().getMethod("getKey").invoke(pref);
                        if (!KEY.equals(key)) return;
                        Object ctxObj = call(p.thisObject, "getContext");
                        if (!(ctxObj instanceof Context)) ctxObj = call(p.thisObject, "getActivity");
                        if (ctxObj instanceof Context) showDialog((Context) ctxObj);
                        p.setResult(Boolean.TRUE);
                    } catch (Throwable t) {
                        Logx.e("click handle failed", t);
                    }
                }
            });

            Logx.i("settings hooks installed on " + FRAG);
        } catch (Throwable t) {
            Logx.e("settings hook install failed", t);
        }
    }

    private static void inject(Object fragment, ClassLoader cl) throws Exception {
        Object screen = call(fragment, "getPreferenceScreen");
        if (screen == null) { Logx.w("PreferenceScreen is null"); return; }

        Object ctxObj = call(fragment, "getContext");
        if (!(ctxObj instanceof Context)) ctxObj = call(fragment, "getActivity");
        if (!(ctxObj instanceof Context)) { Logx.w("no context"); return; }
        Context ctx = (Context) ctxObj;

        Class<?> prefCls = Class.forName(PREF_CLS, false, cl);
        Object existing = screen.getClass().getMethod("findPreference", CharSequence.class)
                .invoke(screen, KEY);
        if (existing != null) { sEntry = new WeakReference<>(existing); return; }

        Object pref = prefCls.getConstructor(Context.class).newInstance(ctx);
        prefCls.getMethod("setKey", String.class).invoke(pref, KEY);
        prefCls.getMethod("setTitle", CharSequence.class).invoke(pref, TITLE);
        prefCls.getMethod("setSummary", CharSequence.class).invoke(pref, summary());
        prefCls.getMethod("setPersistent", boolean.class).invoke(pref, false);

        forceFirstOrder(screen, prefCls, pref);
        screen.getClass().getMethod("addPreference", prefCls).invoke(screen, pref);
        sEntry = new WeakReference<>(pref);
        moveToTop(screen, prefCls, pref);

        Object first = screen.getClass().getMethod("getPreference", int.class).invoke(screen, 0);
        Object firstKey = first == null ? null : first.getClass().getMethod("getKey").invoke(first);
        Logx.i("settings entry added: " + TITLE + " | first item key=" + firstKey);
    }


    /**
     * 把入口移到最前面。PreferenceGroup 只有 addPreference(追加)，
     * 没有 insert，所以按「取出全部 → removeAll → 先加自己 → 再按原顺序加回」实现。
     */
    private static void moveToTop(Object screen, Class<?> prefCls, Object mine) throws Exception {
        int n = (Integer) screen.getClass().getMethod("getPreferenceCount").invoke(screen);
        java.util.List<Object> kept = new java.util.ArrayList<>();
        for (int i = 0; i < n; i++) {
            Object child = screen.getClass().getMethod("getPreference", int.class).invoke(screen, i);
            if (child != null && child != mine) kept.add(child);
        }
        screen.getClass().getMethod("removeAll").invoke(screen);

        Method add = screen.getClass().getMethod("addPreference", prefCls);
        add.invoke(screen, mine);
        for (Object child : kept) add.invoke(screen, child);
    }

    /** 宿主 XML 里每项都带 android:order，排序按 order 而不是插入位置，
     *  必须把自己设成比现有最小值更小，才能真正排到最前。 */
    private static void forceFirstOrder(Object screen, Class<?> prefCls, Object pref) throws Exception {
        int n = (Integer) screen.getClass().getMethod("getPreferenceCount").invoke(screen);
        int min = Integer.MAX_VALUE;
        for (int i = 0; i < n; i++) {
            Object child = screen.getClass().getMethod("getPreference", int.class).invoke(screen, i);
            if (child == null) continue;
            Object o = child.getClass().getMethod("getOrder").invoke(child);
            if (o instanceof Integer) min = Math.min(min, (Integer) o);
        }
        if (min == Integer.MAX_VALUE) min = 1;
        int target = (min == Integer.MIN_VALUE) ? min : min - 1;
        prefCls.getMethod("setOrder", int.class).invoke(pref, target);
        Logx.i("order: existing min=" + min + " -> mine=" + target + " (children=" + n + ")");
    }

    private static String summary() {
        return Config.enabled() ? "装扮主题守护 · 已启用" : "装扮主题守护 · 已停用";
    }

    private static void refreshSummary() {
        Object p = sEntry.get();
        if (p == null) return;
        try {
            p.getClass().getMethod("setSummary", CharSequence.class).invoke(p, summary());
        } catch (Throwable ignored) { }
    }

    private static void showDialog(Context ctx) {
        float d = ctx.getResources().getDisplayMetrics().density;
        int pad = (int) (20 * d);

        LinearLayout root = new LinearLayout(ctx);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, 0);

        TextView tip = new TextView(ctx);
        tip.setText("国际版每次冷启动都会用云端数据覆盖本地装扮，开启后不再覆盖。");
        root.addView(tip);

        final Switch swEnable = new Switch(ctx);
        swEnable.setText("启用守护");
        swEnable.setChecked(Config.enabled());
        root.addView(swEnable);

        final Switch swLog = new Switch(ctx);
        swLog.setText("输出调试日志");
        swLog.setChecked(Config.logEnabled());
        root.addView(swLog);

        new AlertDialog.Builder(ctx)
                .setTitle(TITLE + " v" + Const.VERSION)
                .setView(root)
                .setPositiveButton("保存", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface dialog, int which) {
                        Config.setEnabled(swEnable.isChecked());
                        Config.setLogEnabled(swLog.isChecked());
                        Logx.setEnabled(swLog.isChecked());
                        refreshSummary();
                        Logx.i("settings saved: enabled=" + swEnable.isChecked());
                    }
                })
                .setNeutralButton("关于", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface dialog, int which) { showAbout(ctx); }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private static void showAbout(final Context ctx) {
        new AlertDialog.Builder(ctx)
                .setTitle("关于 " + TITLE)
                .setMessage(Const.ABOUT)
                .setPositiveButton("打开 GitHub 仓库", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface dialog, int which) {
                        openUrl(ctx, Const.GITHUB_URL);
                    }
                })
                .setNegativeButton("关闭", null)
                .show();
    }

    private static void openUrl(Context ctx, String url) {
        try {
            ctx.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Throwable ignored) { }
    }

    private static Object call(Object target, String name, Object... args) {
        try {
            if (args == null || args.length == 0) {
                return target.getClass().getMethod(name).invoke(target);
            }
            for (Method m : target.getClass().getMethods()) {
                if (m.getName().equals(name) && m.getParameterTypes().length == args.length) {
                    return m.invoke(target, args);
                }
            }
        } catch (Throwable ignored) { }
        return null;
    }

    private SettingsInjector() {}
}

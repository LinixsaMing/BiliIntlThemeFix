package com.bilifix.themefix;

import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * 核心修复：阻断「网络回包 → 覆盖本地装扮」这一条路径。
 *
 * 定位顺序：已知类名 → 包内短名枚举 → 按 (Activity, GarbData)void 签名特征匹配。
 * 任一步失败都只记日志，不影响宿主运行。
 */
public final class GarbHook {

    private static final String[] KNOWN_OWNERS = {Const.GARB_PKG + ".t"};

    public static void install(ClassLoader cl) {
        Logx.i("install() start, loader=" + cl);
        try {
            Class<?> garbData = findClass(Const.GARB_DATA, cl);
            Logx.i("GarbData = " + garbData);
            if (garbData == null) { Logx.w("GarbData not found, abort"); return; }

            Class<?> activity = findClass(Const.ACTIVITY, cl);
            Logx.i("Activity = " + activity);
            if (activity == null) { Logx.w("Activity not found, abort"); return; }

            Method target = null;
            String owner = null;
            int tried = 0;
            for (String name : candidates()) {
                Class<?> c = loadQuietly(name, cl);
                if (c == null) continue;
                tried++;
                Method m = matchApply(c, activity, garbData);
                if (m != null) { target = m; owner = name; break; }
            }
            Logx.i("candidates loaded=" + tried);
            if (target == null) {
                Logx.w("no (Activity,GarbData)void method matched");
                return;
            }

            Logx.i("matched " + owner + "#" + target.getName() + " " + sig(target));
            XposedBridge.hookMethod(target, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (!Config.enabled()) {
                        Logx.i("guard disabled by user, pass through");
                        return;
                    }
                    param.setResult(null);
                    Logx.i("blocked cloud garb overwrite");
                }
            });
            Logx.i("hook installed OK");
        } catch (Throwable t) {
            Logx.e("install failed", t);
        }
    }

    private static Class<?> findClass(String name, ClassLoader cl) {
        try {
            Class<?> c = XposedHelpers.findClassIfExists(name, cl);
            if (c != null) return c;
        } catch (Throwable t) {
            Logx.w("findClassIfExists(" + name + ") failed: " + t);
        }
        try {
            return Class.forName(name, false, cl);
        } catch (Throwable t) {
            return null;
        }
    }

    private static String[] candidates() {
        java.util.ArrayList<String> out = new java.util.ArrayList<>(KNOWN_OWNERS.length + 286);
        for (String s : KNOWN_OWNERS) out.add(s);
        for (char c = 'a'; c <= 'z'; c++) {
            out.add(Const.GARB_PKG + "." + c);
            for (char d = '0'; d <= '9'; d++) out.add(Const.GARB_PKG + "." + c + d);
        }
        return out.toArray(new String[0]);
    }

    private static Class<?> loadQuietly(String name, ClassLoader cl) {
        try { return Class.forName(name, false, cl); } catch (Throwable ignored) { return null; }
    }

    private static Method matchApply(Class<?> c, Class<?> activity, Class<?> garbData) {
        Method[] ms;
        try { ms = c.getDeclaredMethods(); } catch (Throwable t) { return null; }
        for (Method m : ms) {
            Class<?>[] p = m.getParameterTypes();
            if (p.length != 2) continue;
            if (m.getReturnType() != void.class) continue;
            if (!p[0].isAssignableFrom(activity)) continue;
            if (!p[1].isAssignableFrom(garbData)) continue;
            return m;
        }
        return null;
    }

    private static String sig(Method m) {
        StringBuilder sb = new StringBuilder("(");
        for (Class<?> p : m.getParameterTypes()) sb.append(p.getName()).append(',');
        return sb.append(")").append(m.getReturnType().getName()).toString();
    }

    private GarbHook() {}
}

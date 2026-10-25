package com.bilifix.themefix;

import android.util.Log;

import java.io.File;
import java.io.FileWriter;

/**
 * 日志出口：同时写 logcat 和宿主的外部私有目录，方便在没有 root/adb 时排障。
 * 路径：/storage/emulated/0/Android/data/<宿主包名>/files/themefix.log
 */
public final class Logx {

    private static final String EXTERNAL_LOG =
            "/storage/emulated/0/Android/data/" + Const.HOST_PKG + "/files/themefix.log";

    private static volatile boolean sEnabled = true;

    public static void setEnabled(boolean e) { sEnabled = e; }

    public static void i(String msg) { out("I", msg, null); }
    public static void w(String msg) { out("W", msg, null); }
    public static void e(String msg, Throwable t) { out("E", msg, t); }

    private static void out(String level, String msg, Throwable t) {
        String line = level + "/" + msg + (t == null ? "" : " :: " + t.getClass().getName() + ": " + t.getMessage());
        if (sEnabled) {
            try {
                if (t == null) Log.println(Log.INFO, Const.TAG, line);
                else Log.println(Log.ERROR, Const.TAG, line + "\n" + Log.getStackTraceString(t));
            } catch (Throwable ignored) { }
        }
        file(line, t);
    }

    private static void file(String line, Throwable t) {
        FileWriter w = null;
        try {
            File f = new File(EXTERNAL_LOG);
            File parent = f.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            w = new FileWriter(f, true);
            w.write(System.currentTimeMillis() + " " + line + "\n");
            if (t != null) w.write(Log.getStackTraceString(t) + "\n");
        } catch (Throwable ignored) {
        } finally {
            if (w != null) try { w.close(); } catch (Throwable ignored) { }
        }
    }

    private Logx() {}
}

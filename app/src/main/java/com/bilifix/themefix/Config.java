package com.bilifix.themefix;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * 配置读写。模块运行在宿主进程内，直接读写宿主自己的数据目录，
 * 不依赖 Context，进程启动早期即可用。
 */
public final class Config {

    private static final File FILE =
            new File("/data/data/" + Const.HOST_PKG + "/files/themefix.cfg");

    private static final String K_ENABLED = "enabled";
    private static final String K_LOG = "log";

    private static volatile Map<String, String> sCache;

    public static boolean enabled() { return bool(K_ENABLED, true); }
    public static boolean logEnabled() { return bool(K_LOG, true); }

    public static void setEnabled(boolean v) { put(K_ENABLED, String.valueOf(v)); }
    public static void setLogEnabled(boolean v) { put(K_LOG, String.valueOf(v)); }

    private static boolean bool(String key, boolean def) {
        String v = load().get(key);
        return v == null ? def : Boolean.parseBoolean(v);
    }

    private static synchronized Map<String, String> load() {
        if (sCache != null) return sCache;
        Map<String, String> m = new HashMap<>();
        BufferedReader r = null;
        try {
            if (FILE.exists()) {
                r = new BufferedReader(new FileReader(FILE));
                String line;
                while ((line = r.readLine()) != null) {
                    int i = line.indexOf('=');
                    if (i > 0) m.put(line.substring(0, i).trim(), line.substring(i + 1).trim());
                }
            }
        } catch (Throwable ignored) {
        } finally {
            if (r != null) try { r.close(); } catch (Throwable ignored) { }
        }
        sCache = m;
        return m;
    }

    private static synchronized void put(String key, String value) {
        Map<String, String> m = new HashMap<>(load());
        m.put(key, value);
        sCache = m;
        FileWriter w = null;
        try {
            File dir = FILE.getParentFile();
            if (dir != null && !dir.exists()) dir.mkdirs();
            w = new FileWriter(FILE, false);
            for (Map.Entry<String, String> e : m.entrySet()) {
                w.write(e.getKey() + "=" + e.getValue() + "\n");
            }
        } catch (Throwable t) {
            Logx.e("save config failed", t);
        } finally {
            if (w != null) try { w.close(); } catch (Throwable ignored) { }
        }
    }

    public static void invalidate() { sCache = null; }

    private Config() {}
}

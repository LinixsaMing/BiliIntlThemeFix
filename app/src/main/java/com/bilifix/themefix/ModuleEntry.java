package com.bilifix.themefix;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public final class ModuleEntry implements IXposedHookLoadPackage {

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lp) {
        try {
            if (lp == null) { Logx.e("lpparam is null", null); return; }
            Logx.i("=== module v0.2.1 loaded: pkg=" + lp.packageName + " first=" + lp.isFirstApplication + " ===");
            if (!Const.HOST_PKG.equals(lp.packageName)) return;
            Logx.setEnabled(Config.logEnabled());
            GarbHook.install(lp.classLoader);
            SettingsInjector.install(lp.classLoader);
        } catch (Throwable t) {
            Logx.e("handleLoadPackage crashed", t);
        }
    }
}

package de.robv.android.xposed;
import java.lang.reflect.Member;
public final class XposedBridge {
    public static XC_MethodHook.Unhook hookMethod(Member method, XC_MethodHook callback) { return null; }
    public static void log(String text) {}
    public static void log(Throwable t) {}
}

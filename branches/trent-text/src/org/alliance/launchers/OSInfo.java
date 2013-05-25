package org.alliance.launchers;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-jun-07
 * Time: 10:35:53
 */
public class OSInfo {

    private static boolean supportsTrayIcon;

    public static boolean supportsTrayIcon() {
        return supportsTrayIcon;
    }

    public static boolean isLinux() {
        return System.getProperty("os.name").toUpperCase().indexOf("LINUX") != -1;
    }

    public static boolean isWindows() {
        return System.getProperty("os.name").toUpperCase().indexOf("WINDOWS") != -1;
    }

    public static boolean isMac() {
        return System.getProperty("os.name").toUpperCase().indexOf("MAC") != -1;
    }

    /* This is set when starting alliance */
    public static void setSupportsTrayIcon(boolean b) {
        supportsTrayIcon = b;
    }
}

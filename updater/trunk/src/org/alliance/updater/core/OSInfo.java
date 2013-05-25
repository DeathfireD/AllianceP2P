package org.alliance.updater.core;

/**
 *
 * @author Bastvera
 */
public class OSInfo {

    public static boolean isLinux() {
        return System.getProperty("os.name").toUpperCase().indexOf("LINUX") != -1;
    }

    public static boolean isWindows() {
        return System.getProperty("os.name").toUpperCase().indexOf("WINDOWS") != -1;
    }

    public static boolean isMac() {
        return System.getProperty("os.name").toUpperCase().indexOf("MAC") != -1;
    }
}

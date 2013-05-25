package org.alliance.updater.launcher;

import java.io.File;
import org.alliance.updater.core.LauncherJava;

/**
 *
 * @author Bastvera
 */
public class DirectoryCheck {

    private static final boolean DEBUG = false;

    public DirectoryCheck(String[] args) {
        try {
            String startedJarName = getJarName();
            String newCurrentDirPath = getFullJarPath();
            if (!new File(startedJarName).exists()) {
                try {
                    LauncherJava.execJar(newCurrentDirPath + startedJarName, new String[0], args, newCurrentDirPath);
                    System.exit(0);
                } catch (Exception ex) {
                    if (DEBUG) {
                        ex.printStackTrace();
                    }
                    System.exit(0);
                }
            }
        } catch (NullPointerException e) {
            if (DEBUG) {
                e.printStackTrace();
                System.out.println("There was an error restarting updater into the current directory, probably starting from debug build");
            }
            return;
        }
    }

    private String pathHelper() {
        String newCurrentDirPath = getClass().getResource("/res/gfx/updater.png").toExternalForm();
        newCurrentDirPath = newCurrentDirPath.replace("file:", "");
        newCurrentDirPath = newCurrentDirPath.replace("jar:", "");
        return newCurrentDirPath;
    }

    public String getJarName() {
        String path = pathHelper();
        String startedJar = path.substring(0, path.lastIndexOf(".jar"));
        startedJar = startedJar.substring(startedJar.lastIndexOf("/") + 1);
        startedJar = startedJar + ".jar";
        if (DEBUG) {
            System.out.println("started jar is " + startedJar);
        }
        return startedJar;
    }

    public String getFullJarPath() {
        String newCurrentDirPath = pathHelper();
        newCurrentDirPath = newCurrentDirPath.substring(0, newCurrentDirPath.indexOf(getJarName()));
        if (DEBUG) {
            System.out.println("newCurrentDirPath is " + newCurrentDirPath);
        }
        return newCurrentDirPath;
    }
}

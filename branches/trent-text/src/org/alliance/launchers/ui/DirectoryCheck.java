package org.alliance.launchers.ui;

import java.io.File;
import org.alliance.T;
import org.alliance.core.LauncherJava;
import org.alliance.launchers.OSInfo;

/**
 *
 * @author Bastvera
 */
public class DirectoryCheck {

    public static String STARTED_JAR_NAME = "debug.jar";

    public DirectoryCheck(String[] args) {
        if (!OSInfo.isWindows()) {
            try {
                STARTED_JAR_NAME = getJarName();
                String newCurrentDirPath = getFullJarPath();
                if (!new File(STARTED_JAR_NAME).exists()) {
                    try {
                        LauncherJava.execJar(newCurrentDirPath + STARTED_JAR_NAME, new String[0], args, newCurrentDirPath);
                        System.exit(0);
                    } catch (Exception ex) {
                        if (T.t) {
                            ex.printStackTrace();
                        }
                        System.exit(0);
                    }
                }
            } catch (NullPointerException e) {
                debugInfo(e);
                return;
            } catch (IndexOutOfBoundsException e) {
                debugInfo(e);
                return;
            }
        } else {
            STARTED_JAR_NAME = "alliance.dat";
        }
    }

    private void debugInfo(Exception e) {
        if (T.t) {
            e.printStackTrace();
            T.info("There was an error restarting alliance into the current directory, probably starting from debug build");
        }
    }

    private String pathHelper() {
        String newCurrentDirPath = getClass().getResource("/res/alliance.cer").toExternalForm();
        newCurrentDirPath = newCurrentDirPath.replace("file:", "");
        newCurrentDirPath = newCurrentDirPath.replace("jar:", "");
        return newCurrentDirPath;
    }

    public String getJarName() {
        String path = pathHelper();
        String startedJar = path.substring(0, path.lastIndexOf(".jar"));
        startedJar = startedJar.substring(startedJar.lastIndexOf("/") + 1);
        startedJar = startedJar + ".jar";
        if (T.t) {
            T.info("started jar is " + startedJar);
        }
        return startedJar;
    }

    public String getFullJarPath() {
        String newCurrentDirPath = pathHelper();
        newCurrentDirPath = newCurrentDirPath.substring(0, newCurrentDirPath.indexOf(getJarName()));
        if (T.t) {
            T.info("newCurrentDirPath is " + newCurrentDirPath);
        }
        return newCurrentDirPath;
    }
}

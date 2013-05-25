package org.alliance.updater.core;

import org.alliance.updater.ui.MainWindow;

/**
 *
 * @author Bastvera
 */
public class Core {

    private Update update;
    private MainWindow mainWindow;

    public Core() {
        update = new Update(this);
        mainWindow = new MainWindow(this);
        mainWindow.setVisible(true);
    }

    public Update getUpdate() {
        return update;
    }

    public MainWindow getMainWindow() {
        return mainWindow;
    }

    public void restart() {
        try {
            if (OSInfo.isWindows()) {
                String s = "cmd /c ." + System.getProperty("file.separator") + "alliance.exe";
                Runtime.getRuntime().exec(s);
                System.exit(0);
            } else {
                LauncherJava.execJar(update.getMainAllianceFile().getName(), new String[0], new String[0], "");
                System.exit(0);
            }
        } catch (Exception ex) {
            MainWindow.showDialog("Restart ... failed\nPlease start manually.", "Error", "error");
            System.exit(0);
        }
        System.exit(0);
    }
}

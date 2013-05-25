package org.alliance.ui.macos;

import org.alliance.launchers.OSInfo;
import org.alliance.ui.UISubsystem;

import javax.swing.SwingUtilities;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2008-apr-28
 * Time: 19:25:27
 * To change this template use File | Settings | File Templates.
 */
public class OSXAdaptation {

    private UISubsystem ui;

    public OSXAdaptation(UISubsystem ui) {
        try {
            this.ui = ui;
            if (OSInfo.isMac()) {
                OSXAdapter.setPreferencesHandler(this, getClass().getMethod("preferences"));
                OSXAdapter.setOpenApplicationHandler(this, getClass().getMethod("show"));
                OSXAdapter.setReOpenApplicationHandler(this, getClass().getMethod("show"));
                OSXAdapter.setQuitHandler(this, getClass().getMethod("quit"));
            }
        } catch (Exception e) {
            ui.handleErrorInEventLoop(e);
        }
    }

    public void preferences() {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                try {
                    ui.getMainWindow().EVENT_options(null);
                } catch (Exception e) {
                    ui.handleErrorInEventLoop(e);
                }
            }
        });
    }

    public void show() {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                ui.getMainWindow().setVisible(true);
            }
        });
    }

    public void quit() {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                ui.getCore().shutdown();
            }
        });
    }
}

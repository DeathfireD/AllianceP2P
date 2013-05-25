package org.alliance.launchers.ui;

import java.awt.AWTEvent;
import java.awt.EventQueue;
import java.awt.PopupMenu;
import java.awt.TrayIcon;

//This is a dirty hack to get around java sucking, found the code here http://weblogs.java.net/blog/ixmal/archive/2006/05/using_jpopupmen.html
//otherwise linux will give you wonderful class cast exceptions
public class PopupFixQueue extends EventQueue {

    private PopupMenu popup;

    public PopupFixQueue(PopupMenu m) {
        this.popup = m;
    }

    @Override
    protected void dispatchEvent(AWTEvent event) {
        try {
            super.dispatchEvent(event);
        } catch (RuntimeException ex) {
            if (event.getSource() instanceof TrayIcon) {
                //popup.setVisible(false);
            } else {
                throw ex;
            }
        }
    }
}

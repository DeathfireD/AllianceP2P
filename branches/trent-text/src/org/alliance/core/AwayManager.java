package org.alliance.core;

import java.awt.MouseInfo;
import java.awt.Point;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2007-feb-17
 * Time: 16:35:28
 * To change this template use File | Settings | File Templates.
 */
public class AwayManager extends Manager implements Runnable {

    public interface AwayStatusListener {

        abstract void awayStatusChanged(boolean away) throws IOException;
    }
    private Thread thread;
    private boolean away = false;
    private CoreSubsystem core;
    private ArrayList<AwayStatusListener> listeners = new ArrayList<AwayStatusListener>();

    public AwayManager(CoreSubsystem core) {
        this.core = core;
    }

    @Override
    public void init() throws IOException, Exception {
        if (T.t) {
            T.info("AwayManger - <init>");
        }
        thread = new Thread(this);
        thread.setDaemon(true);
        thread.start();
    }

    public void addListener(AwayStatusListener l) {
        listeners.add(l);
    }

    @Override
    public void run() {
        try {
            if (T.t) {
                T.info("Away manager thread starting.");
            }
            long lastTimeMouseMoved = System.currentTimeMillis();
            while (true) {
                Point p = MouseInfo.getPointerInfo().getLocation();
                Thread.sleep(1000);
                if ((p.y > 100000 || p.x > 100000 || p.x < -100000 || p.y < -100000) || //big coords are bougus, disregard them (this happens when user has a minimized remote desktop on windows)
                        p.equals(MouseInfo.getPointerInfo().getLocation())) {
                    //mouse has not moved
                    if (System.currentTimeMillis() - lastTimeMouseMoved > core.getSettings().getInternal().getSecondstoaway() * 1000) {
                        updateAway(true);
                    }
                } else {
                    //mouse has moved
                    updateAway(false);
                    lastTimeMouseMoved = System.currentTimeMillis();
                }
            }
        } catch (InterruptedException e) {
            if (T.t) {
                T.info("Away loop interrupted");
            }
        } catch (Throwable e) {
            if (T.t) {
                T.error("Error in away loop: " + e);
            }
        }
    }

    private void updateAway(boolean b) {
        if (b != away) {
            away = b;
            core.invokeLater(new Runnable() {

                @Override
                public void run() {
                    try {
                        if (T.t) {
                            T.info("Away status changed for me: " + away);
                        }
                        core.informFriendsOfAwayStatus(away);
                        for (AwayStatusListener listener : listeners) {
                            listener.awayStatusChanged(away);
                        }
                    } catch (IOException e) {
                        core.reportError(e, this);
                    }
                }
            });
        }
    }

    public void shutdown() {
        thread.interrupt();
    }

    public boolean isAway() {
        return away;
    }
}

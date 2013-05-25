package org.alliance.core.comm.throttling;

import org.alliance.core.CoreSubsystem;
import org.alliance.core.comm.Connection;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Timer;
import java.util.TimerTask;

/**
 * based on code from limewire
 */
public class BandwidthThrottle {

    private static final int TICKS_PER_SECOND = 10;
    private static final int MILLIS_PER_TICK = 1000 / TICKS_PER_SECOND;
    private volatile int _bytesPerTick;
    private int _availableBytes;
    private long _nextTickTime;
    private CoreSubsystem core;
    private HashSet<Connection> connectionsToWakeUp = new HashSet<Connection>();
    private Timer timer = new Timer(true);
    private boolean sleeping;

    public BandwidthThrottle(CoreSubsystem core, float bytesPerSecond) {
        this.core = core;
        setRate(bytesPerSecond);
    }

    public void setRate(float bytesPerSecond) {
        bytesPerSecond += bytesPerSecond / 10; //w000! what a hack. The limit seems to always be 10% slower then what we set. So. well. yeah.
        _bytesPerTick = (int) ((float) bytesPerSecond / TICKS_PER_SECOND);
        if (T.t) {
            T.info("Setting bytesPerTick: " + _bytesPerTick);
        }
    }

    synchronized public int request(Connection c, int desired) {
        if (_bytesPerTick == 0) {
            return desired;
        }
        if (T.t) {
            T.trace("con : " + c + " connectionsToWakeUp: " + connectionsToWakeUp.size() + ": " + connectionsToWakeUp.contains(c) + " rate: " + _bytesPerTick);
        }
        if (sleeping) {
            if (T.t) {
                T.trace("sleeping. Returning 0.");
            }
            connectionsToWakeUp.add(c);
            return 0;
        }
        if (waitForBandwidth(c)) {
            return 0;
        }
        return Math.min(desired, _availableBytes);
    }

    synchronized public void bytesProcessed(int bytes) {
        _availableBytes -= bytes;
    }

    /**
     * Waits until data is _availableBytes.
     */
    private boolean waitForBandwidth(Connection c) {
        for (;;) {
            long now = System.currentTimeMillis();
            updateWindow(now);
            if (_availableBytes > 0) {
                return false;
            }
            startDelay(c, _nextTickTime - now);
            return true;
        }
    }

    /**
     * Updates _availableBytes and _nextTickTime if possible.
     */
    private void updateWindow(long now) {
        if (now >= _nextTickTime) {
            if (T.t) {
                T.trace("Resetting window");
            }
            _availableBytes = _bytesPerTick;
            _nextTickTime = now + MILLIS_PER_TICK;
        }
    }

    private void startDelay(Connection c, long time) {
        if (time <= 0) {
            if (T.t) {
                T.warn("Delay illegal: " + time);
            }
            time = 10;
        }
        connectionsToWakeUp.add(c);
        if (T.t) {
            T.info("Waiting for " + time + "ms");
        }
        sleeping = true;
        timer.schedule(new Task(), time);
    }

    private class Task extends TimerTask {

        @Override
        public void run() {
            core.invokeLater(new Runnable() {

                @Override
                public void run() {
                    ArrayList<Connection> al = new ArrayList<Connection>(connectionsToWakeUp);
                    if (T.t) {
                        T.info("kickstarting - " + al.size());
                    }
                    connectionsToWakeUp.clear();
                    sleeping = false;
                    //randomize the order of wakeups
                    while (al.size() > 0) {
                        int i = (int) (Math.random() * al.size());
                        if (i >= al.size()) {
                            i = al.size() - 1;
                        }
                        Connection c = al.get(i);
                        try {
                            c.readyToSend();
                        } catch (IOException e) {
                            core.reportError(e, c);
                        }
                        al.remove(c);
                    }
                }
            });
        }
    }
}

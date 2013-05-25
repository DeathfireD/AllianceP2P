package org.alliance.core;

/**
 * Invokes the run method of the supplied runner every time invoke() is called BUT makes sure that the run
 * method is never called more often than paceIsMs ms.
 * Created by maciek at 2008-dec-04
 */
public class PacedRunner extends Thread {

    private final static boolean DEBUG = false;
    private Runnable runner;
    private int paceInMs;
    private boolean alive = true;
    private long lastRunTick;
    private boolean runHasBeenRequested = false;

    public PacedRunner(int paceInMs) {
        this.paceInMs = paceInMs;
        if (paceInMs < 250) {
            throw new IllegalArgumentException("Pace may not be less then 250ms. This could affect performance adversly.");
        }
        setDaemon(true);
        start();
    }

    public PacedRunner(Runnable runner, int paceInMs) {
        this(paceInMs);
        this.runner = runner;
    }

    @Override
    public void run() {
        while (alive) {
            int sleep = paceInMs;
            if (runHasBeenRequested && System.currentTimeMillis() - lastRunTick < paceInMs) {
                sleep = paceInMs - (int) (System.currentTimeMillis() - lastRunTick);
            }
            if (DEBUG) {
                if (sleep != paceInMs) {
                    System.err.println("Sleeping " + sleep + " ms...");
                }
            }
            try {
                Thread.sleep(sleep);
            } catch (InterruptedException e) {
            }
            if (runHasBeenRequested) {
                if (System.currentTimeMillis() - lastRunTick >= paceInMs - 100) {   //subtract 100ms because we can't trust that sleep will actually sleep EXACTLY paceInMs ms.
                    if (DEBUG) {
                        System.err.println("Running our runnable!");
                    }
                    runHasBeenRequested = false;
                    if (runner != null) {
                        runner.run();
                    } else {
                        if (T.t) {
                            T.warn("Runner was NULL in PacedRunner - this should not happen.");
                        }
                    }
                    lastRunTick = System.currentTimeMillis();
                } else {
                    if (DEBUG) {
                        System.err.println("Won't run our runnable because it was ran less then " + paceInMs + " ms ago");
                    }
                }
            }
        }
    }

    public void invoke() {
        if (DEBUG) {
            System.err.println("invoke()!");
        }
        runHasBeenRequested = true;
        interrupt();
    }

    public void kill() {
        alive = false;
    }

    public void setRunner(Runnable runner) {
        this.runner = runner;
    }
}

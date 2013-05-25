package org.alliance.core;

public class SimpleTimer {

    private long tick;

    public SimpleTimer() {
        this.tick = System.nanoTime();
    }

    public String getTime() {
        long diff = System.nanoTime() - this.tick;
        if (diff / 100000000L >= 15L) {
            return ((int) (diff / 10000000L) / 100.0F) + "s";
        }
        return ((int) (diff / 10000L) / 100.0F) + "ms";
    }

    public int getTimeInMs() {
        long diff = System.nanoTime() - this.tick;
        return (int) (diff / 1000000L);
    }
}

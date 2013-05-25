package org.alliance.core.comm;

import com.stendahls.util.TextUtils;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-jan-29
 * Time: 16:51:59
 * To change this template use File | Settings | File Templates.
 */
public class BandwidthAnalyzer {

    public static final int INNER_INVERVAL = 1000, OUTER_INTERVAL = 3000;
    private long tick = -1;
    private int nBytes;
    private double highestCps;
    private long totalBytes;
    private int updateInterval = 1000;
    private double cps, averageCps; // bytes per second or Chars Per Second
    private double[] rollingAverage = new double[30];
    private int index;

    public BandwidthAnalyzer(int updateInterval) {
        this.updateInterval = updateInterval;
    }

    public BandwidthAnalyzer() {
    }

    public BandwidthAnalyzer(int updateInterval, int highestCps) {
        this.updateInterval = updateInterval;
        this.highestCps = highestCps;
    }

    public BandwidthAnalyzer(int updateInterval, int highestCps, long totalBytesStartingAt) {
        this.updateInterval = updateInterval;
        this.highestCps = highestCps;
        if (totalBytesStartingAt < 0) {
            totalBytesStartingAt = 0;
        }
        this.totalBytes = totalBytesStartingAt;
    }

    public void update(int bytes) {
        if (tick == -1) {
            tick = System.currentTimeMillis();
        }
        nBytes += bytes;
        totalBytes += bytes;
        updateCps();
    }

    private void updateCps() {
        if (System.currentTimeMillis() - tick > updateInterval) {
            cps = ((double) nBytes) / ((System.currentTimeMillis() - tick) / 1000.);
            tick = System.currentTimeMillis();
            nBytes = 0;

            rollingAverage[(index++) % rollingAverage.length] = cps;

            double n = 0;
            averageCps = 0;
            for (int i = 0; i < index && i < rollingAverage.length; i++) {
                averageCps += rollingAverage[i];
                n++;
            }
            averageCps /= n;
        }

//        if (highestCps < averageCps) highestCps = averageCps;
        if (highestCps < cps) {
            highestCps = cps;
        }
    }

    public double getCPS() {
        updateCps();
        return cps;
    }

    public String getCPSHumanReadable() {
        updateCps();
        return getHumanReadable(cps);
    }

    public static String getHumanReadable(double cps) {
        return TextUtils.formatByteSize((int) cps) + "/s";
    }

    public double getHighestCPS() {
        return highestCps;
    }

    public String getHighestCPSHumanReadable() {
        return getHumanReadable(highestCps);
    }

    public long getTotalBytes() {
        return totalBytes;
    }

    public double getAverageCps() {
        return averageCps;
    }

    public boolean hasGoodAverage() {
        return index > 4;
    }

    public void resetHighestCPS() {
        highestCps = 0;
    }
}

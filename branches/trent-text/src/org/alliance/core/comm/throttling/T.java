package org.alliance.core.comm.throttling;

import org.alliance.core.trace.TraceChannel;

/**
 * User: maciek
 * Date: 2004-sep-17
 * Time: 09:38:09
 */
public class T {

    public static final boolean t = false && org.alliance.core.comm.T.t;
    private static TraceChannel tc = new TraceChannel("throt");

    private T() {
    }

    public static void trace(Object message) {
        tc.trace(message);
    }

    public static void debug(Object message) {
        tc.debug(message);
    }

    public static void info(Object message) {
        tc.info(message);
    }

    public static void warn(Object message) {
        tc.warn(message);
    }

    public static void warn(Object message, Exception stackTrace) {
        tc.warn(message, stackTrace);
    }

    public static void error(Object message) {
        tc.error(message);
    }

    public static void ass(boolean a, Object m) {
        tc.ass(a, m);
    }
}

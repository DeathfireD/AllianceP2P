package org.alliance.core.crypto;

import org.alliance.core.trace.TraceChannel;

/**
 * User: maciek
 * Date: 2004-sep-17
 * Time: 09:38:09
 */
public class T {

    public static final boolean t = true && org.alliance.T.t;
    private static TraceChannel tc = new TraceChannel("crypto");

    private T() {
    }

    public static void trace(Object message) {
        org.alliance.core.crypto.T.tc.trace(message);
    }

    public static void debug(Object message) {
        org.alliance.core.crypto.T.tc.debug(message);
    }

    public static void info(Object message) {
        org.alliance.core.crypto.T.tc.info(message);
    }

    public static void warn(Object message) {
        org.alliance.core.crypto.T.tc.warn(message);
    }

    public static void error(Object message) {
        org.alliance.core.crypto.T.tc.error(message);
    }

    public static void ass(boolean assertion, Object message) {
        org.alliance.core.crypto.T.tc.ass(assertion, message);
    }
}

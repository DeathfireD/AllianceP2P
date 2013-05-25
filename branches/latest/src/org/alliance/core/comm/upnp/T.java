package org.alliance.core.comm.upnp;

import com.stendahls.trace.TraceChannel;

/**
 * User: maciek
 * Date: 2004-sep-17
 * Time: 09:38:09
 */
public class T {

    public static final boolean t = true && org.alliance.core.comm.T.t;
    private static TraceChannel tc = new TraceChannel("upnp");

    public static void trace(Object message) {
        org.alliance.core.comm.upnp.T.tc.trace(message);
    }

    public static void debug(Object message) {
        org.alliance.core.comm.upnp.T.tc.debug(message);
    }

    public static void info(Object message) {
        org.alliance.core.comm.upnp.T.tc.info(message);
    }

    public static void warn(Object message) {
        org.alliance.core.comm.upnp.T.tc.warn(message);
    }

    public static void warn(Object message, Exception stackTrace) {
        org.alliance.core.comm.upnp.T.tc.warn(message, stackTrace);
    }

    public static void error(Object message) {
        org.alliance.core.comm.upnp.T.tc.error(message);
    }

    public static void ass(boolean a, Object m) {
        org.alliance.core.comm.upnp.T.tc.ass(a, m);
    }
}

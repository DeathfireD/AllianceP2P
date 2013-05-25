package org.alliance.core.trace;

import com.stendahls.util.TextUtils;

public class Trace {

    public static final int LEVEL_TRACE = 0;
    public static final int LEVEL_DEBUG = 1;
    public static final int LEVEL_INFO = 2;
    public static final int LEVEL_WARN = 3;
    public static final int LEVEL_ERROR = 4;
    public static final String[] LEVELS = {"", "", "", "", ""};
    public static TraceHandler handler;

    public void print(int level, Object message) {
        print(level, message, null);
    }

    public void print(int level, Object message, Exception error) {
        if (handler != null) {
            handler.print(level, message, error);
        } else {
            StackTraceElement ste = new Exception().getStackTrace()[3];
            String s = message.toString();
            int i = s.indexOf(32);
            String channel = s.substring(0, i);
            String m = s.substring(i + 1);

            s = TextUtils.complete(channel, 10) + LEVELS[level] + ' ' + m;

            if (System.getProperty("idea.launcher.library") != null) {
                s = TextUtils.complete(s, 100) + " at " + ste.getClassName() + '.' + ste.getMethodName() + '(' + ste.getFileName() + ':' + ste.getLineNumber() + ')';
            }
            if (level >= 3) {
                System.err.println(s);
            } else {
                System.out.println(s);
            }
        }
    }
}

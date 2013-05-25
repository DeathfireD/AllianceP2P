package org.alliance.core.trace;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.StringTokenizer;

public class TraceChannel extends Trace {

    private String channelName;

    public TraceChannel(String channelName) {
        this.channelName = channelName.toUpperCase();
    }

    public void trace(Object message, Exception error) {
        print(0, this.channelName + ' ' + message, error);
    }

    public void debug(Object message, Exception error) {
        print(1, this.channelName + ' ' + message, error);
    }

    public void info(Object message, Exception error) {
        print(2, this.channelName + ' ' + message, error);
    }

    public void warn(Object message, Exception error) {
        print(3, this.channelName + ' ' + message, error);
    }

    public void trace(Object message) {
        trace(message, null);
    }

    public void debug(Object message) {
        debug(message, null);
    }

    public void info(Object message) {
        info(message, null);
    }

    public void warn(Object message) {
        warn(message, null);
    }

    public void error(Object message) {
        error(message, null);
    }

    public void error(Object message, Exception error) {
        if (message instanceof Throwable) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Throwable t = (Throwable) message;
            PrintWriter pw = new PrintWriter(new OutputStreamWriter(out));
            t.printStackTrace(pw);
            pw.flush();
            StringTokenizer st = new StringTokenizer(new String(out.toByteArray()), "\n");
            int level = 4;
            while (st.hasMoreTokens()) {
                print(level, this.channelName + ' ' + st.nextToken());
                level = 0;
            }
        } else {
            print(4, this.channelName + ' ' + message, error);
        }
    }

    public void ass(boolean assertion, Object message) {
        if (!(assertion)) {
            String s = "Assertion error: " + message;
            error(s);
            throw new RuntimeException(s);
        }
    }
}

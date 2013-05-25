package org.alliance.core;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Log {

    private static final DateFormat FORMAT = new SimpleDateFormat("HH:mm yyyyMMdd");
    private PrintWriter out;

    public Log(String filename) throws FileNotFoundException {
        this(filename, false);
    }

    public Log(String filename, boolean append) throws FileNotFoundException {
        this.out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(filename, append)));
    }

    public void log(Object message) throws IOException {
        String s = FORMAT.format(new Date()) + " ";
        if (message instanceof Throwable) {
            this.out.print(s);
            ((Throwable) message).printStackTrace(this.out);
        } else {
            this.out.println(s + message);
        }
        this.out.flush();
    }
}

package org.alliance.launchers.testsuite;

import com.stendahls.XUI.XUIFrame;
import org.alliance.T;
import org.alliance.core.CoreSubsystem;
import org.alliance.core.ResourceSingelton;
import org.alliance.core.settings.Plugin;
import org.alliance.core.trace.Trace;
import org.alliance.core.trace.TraceHandler;
import org.alliance.misc.TestPlugIn;
import org.alliance.ui.UISubsystem;

import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

/**
 * Use GenerateTestSuite to generate test data for this class.
 *
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-jan-05
 * Time: 10:03:57
 */
public class Main {

    public static final boolean LOG_TO_FILES = false;
    public static final String TEST_SUITE_DIRNAME = "testsuite";
    private static HashMap<String, Main> users = new HashMap<String, Main>();
    private CoreSubsystem core;
    private UISubsystem ui;
    private String settings;
    private static JList list;

    public Main() {
    }

    public Main(String settings) throws Exception {
        System.out.println("  " + settings);
        this.settings = settings;
        launchCore();
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Welcome to Alliance testsuite. Launching:");

        System.setProperty("testsuite", "true");

        for (File f : new File(Main.TEST_SUITE_DIRNAME + File.separator + "logs").listFiles()) {
            f.delete();
        }

        if (T.t) {
            Trace.handler = new TraceHandler() {

                private HashMap<String, BufferedWriter> logs = new HashMap<String, BufferedWriter>();

                @Override
                public void print(int level, Object o, Exception error) {
                    try {
                        String message = String.valueOf(o);
                        String name = Thread.currentThread().getName();
                        // String module = message.substring(0, message.indexOf(' '));

                        if (level >= Trace.LEVEL_WARN) {
                            System.out.println(message + " (" + name + ")");
                        }

                        if (LOG_TO_FILES) {
                            BufferedWriter out;
                            if (logs.containsKey(name)) {
                                out = logs.get(name);
                            } else {
                                out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(Main.TEST_SUITE_DIRNAME + File.separator + "logs"+ File.separator + name + ".log")));
                                logs.put(name, out);
                            }
                            out.write(message + "\r\n");
                            out.flush();
                        }

                        int i = name.indexOf("--");
                        if (i != -1) {
                            name = name.substring(i + 3);
                            Main m = users.get(name.toLowerCase());
//                            System.out.println(name+" "+message);
                            if (m != null) {
                                if (error == null) {
                                    error = new Exception();
                                }
                                m.core.propagateTraceMessage(level, String.valueOf(message), error);
                            }
                        } else {
                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            };
        }

        XUIFrame frame = new XUIFrame(Main.TEST_SUITE_DIRNAME + File.separator + "testsuite.xui.xml");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getXUI().setEventHandler(new Main());
        list = new JList(new DefaultListModel());
        JScrollPane p = (JScrollPane) frame.getXUI().getComponent("sp");
        p.setViewportView(list);
        frame.display();
        list.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                if (list.getSelectedValue() != null && e.getClickCount() > 1) {
                    Thread t = new Thread(new Runnable() {

                        @Override
                        public void run() {
                            try {
                                Main m = (Main) list.getSelectedValue();
                                m.launchUI();
                            } catch (Exception e1) {
                                e1.printStackTrace();
                            }
                        }
                    });
                    t.start();
                }
            }
        });

        File settings[] = new File(Main.TEST_SUITE_DIRNAME + File.separator + "settings").listFiles();
        for (File f : settings) {
            if (f.toString().endsWith("xml")) {
                final Main m = new Main(f.toString());
                users.put(f.toString().substring(f.toString().lastIndexOf(System.getProperty("file.separator")) + 1, f.toString().lastIndexOf('.')).toLowerCase(), m);
                SwingUtilities.invokeLater(new Runnable() {

                    @Override
                    public void run() {
                        ((DefaultListModel) list.getModel()).addElement(m);
                    }
                });
            }
            Thread.sleep((long) (1000));
        }
    }

    private void launchUI() throws Exception {
        if (core == null) {
            launchCore();
        }
        ui = new UISubsystem();
        ui.init(ResourceSingelton.getRl(), core);
    }

    private void launchCore() throws Exception {
        if (core != null) {
            shutdown();
        }
        core = new CoreSubsystem();
        core.init(ResourceSingelton.getRl(), settings);
    }

    private void shutdown() {
        if (ui != null) {
            ui.shutdown();
        }
        if (core != null) {
            // check that the plugin system got initialized correctly
            for (Plugin plugin : core.getSettings().getPluginlist()) {
                if (plugin.retrievePluginClass().endsWith("TestPlugIn")) {
                    TestPlugIn.checkInitialized();
                }
            }
            core.shutdown();
        }
        ui = null;
        core = null;
    }

    @Override
    public String toString() {
        return settings;
    }

    public void EVENT_openui(ActionEvent e) throws Exception {
        Main m = (Main) list.getSelectedValue();
        if (m != null) {
            m.launchUI();
        }
    }

    public void EVENT_start(ActionEvent e) throws Exception {
        Main m = (Main) list.getSelectedValue();
        if (m != null) {
            m.launchCore();
        }
    }

    public void EVENT_stop(ActionEvent e) throws Exception {
        Main m = (Main) list.getSelectedValue();
        if (m != null) {
            m.shutdown();
        }
    }
}

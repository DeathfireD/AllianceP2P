package org.alliance.ui.dialogs;

import com.stendahls.XUI.XUIDialog;
import com.stendahls.XUI.XUIException;
import com.stendahls.util.resourceloader.GeneralResourceLoader;
import com.stendahls.util.resourceloader.ResourceLoader;
import com.stendahls.ui.JHtmlLabel;
import com.stendahls.util.TextUtils;
import org.alliance.core.Language;
import org.alliance.ui.themes.util.SubstanceThemeHelper;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Date;
import java.util.Enumeration;
import java.util.Properties;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

public class ErrorDialog extends XUIDialog {

    private static String errorReportUrl = "";
    private Throwable error;
    private Throwable innerError;
    private String errorReport;
    private boolean fatal;
    private static JFrame fakeParent = new JFrame();
    private static ResourceLoader rl = new GeneralResourceLoader(ErrorDialog.class);
    private static ExceptionTranslator translator = new ExceptionTranslator() {

        @Override
        public String translate(Throwable t) {
            return "Error: " + t.toString();
        }
    };

    public static void setErrorReportUrl(String errorReportUrl) {
        ErrorDialog.errorReportUrl = errorReportUrl;
    }

    public static void setExceptionTranslator(ExceptionTranslator et) {
        translator = et;
    }

    public ErrorDialog() throws XUIException {
        super(fakeParent);
    }

    public ErrorDialog(Throwable error, boolean fatal) throws XUIException {
        super(fakeParent);
        init(error, fatal);
    }

    public ErrorDialog(JFrame parent) throws XUIException {
        super(parent);
    }

    public ErrorDialog(JFrame parent, Throwable error, boolean fatal) throws XUIException {
        super(parent);
        init(error, fatal);
    }

    public ErrorDialog(JDialog parent) throws XUIException {
        super(parent);
    }

    public ErrorDialog(JDialog parent, Throwable error, boolean fatal) throws XUIException {
        super(parent);
        init(error, fatal);
    }

    public void init(final Throwable error, final boolean fatal) throws XUIException {
        if (SwingUtilities.isEventDispatchThread()) {
            initInCorrectThread(error, fatal);
        } else {
            SwingUtilities.invokeLater(new Runnable() {

                @Override
                public void run() {
                    try {
                        ErrorDialog.this.initInCorrectThread(error, fatal);
                    } catch (XUIException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    private void initInCorrectThread(Throwable error, boolean fatal) throws XUIException {
        try {
            this.error = error;
            this.fatal = fatal;

            this.innerError = error;

            String errorMessage = translator.translate(this.innerError);

            this.errorReport = generateErrorReport(errorMessage);

            if (fatal) {
                System.err.println("FATAL ERROR: ");
            } else {
                System.err.println("NON-FATAL ERROR: ");
            }
            error.printStackTrace();

            init(rl, rl.getResourceStream("res/xui/template/errordialog.xui.xml"));
            Language.translateXUIElements(getClass(), xui.getXUIComponents());
            SubstanceThemeHelper.setButtonsToGeneralArea(xui.getXUIComponents());

            setTitle(Language.getLocalizedString(getClass(), "titleerror"));
            if (!(fatal)) {
                ((JLabel) this.xui.getComponent("image")).setIcon(new ImageIcon(rl.getResource("res/gfx/icons/warning.png")));
                setTitle(Language.getLocalizedString(getClass(), "titlewarning"));
            }
            Component remove;
            Component keep;
            if (!(fatal)) {
                remove = this.xui.getComponent("fatal");
                keep = this.xui.getComponent("nonfatal");
            } else {
                remove = this.xui.getComponent("nonfatal");
                keep = this.xui.getComponent("fatal");
            }

            remove.getParent().remove(remove);

            JHtmlLabel label = (JHtmlLabel) keep;

            if (fatal) {
                JComponent close = (JComponent) this.xui.getComponent("close");
                close.getParent().remove(close);
                close = (JComponent) this.xui.getComponent("close2");
                close.getParent().remove(close);
            }

            if (errorMessage == null) {
                dispose();
            } else {
                if (!(fatal)) {
                    label.setText(Language.getLocalizedString(getClass(), "xui.nonfatal", TextUtils.cutOffWithDots(errorMessage, 130)));
                } else {
                    label.setText(Language.getLocalizedString(getClass(), "xui.fatal", TextUtils.cutOffWithDots(errorMessage, 130)));
                }
                display();
            }
        } catch (Exception e) {
            throw new XUIException("Could not open error dialog!", e);
        }
    }

    private String generateErrorReport(String errorMessage) {
        StringWriter sw = new StringWriter();

        sw.write("Fatal Error report generated at " + new Date() + "\n");
        sw.write("\n");
        sw.write("Human readable error: \n" + errorMessage);
        sw.write("\n\n");
        sw.write("System properties: \n\n");
        Properties p = System.getProperties();
        for (Enumeration e = p.keys(); e.hasMoreElements();) {
            String key = e.nextElement().toString();
            sw.write(key + ":\n");
            sw.write(p.getProperty(key) + "\n\n");
        }
        sw.write("Error stack trace: \n");
        this.error.printStackTrace(new PrintWriter(sw));

        sw.flush();

        return sw.toString();
    }

    public void EVENT_view(ActionEvent e) {
        try {
            XUIDialog xd = new XUIDialog(this.xui.getResourceLoader(), this.xui.getResourceLoader().getResourceStream("res/xui/template/viewerror.xui.xml"), this);
            Language.translateXUIElements(getClass(), xd.getXUI().getXUIComponents());
            SubstanceThemeHelper.setButtonsToGeneralArea(xd.getXUI().getXUIComponents());
            xui.setEventHandler(this);
            xd.setTitle(Language.getLocalizedString(getClass(), "titlereport"));
            JTextArea ta = (JTextArea) xd.getXUI().getComponent("reportarea");
            ta.setFont(new Font("Tahoma", 0, 11));
            ta.setLineWrap(true);
            ta.setText(this.errorReport);
            xd.display();
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }

    public void EVENT_send(ActionEvent e) {
        try {
            XUIDialog xd = new XUIDialog(this.xui.getResourceLoader(), this.xui.getResourceLoader().getResourceStream("res/xui/template/usercomment.xui.xml"), this);
            Language.translateXUIElements(getClass(), xd.getXUI().getXUIComponents());
            SubstanceThemeHelper.setButtonsToGeneralArea(xd.getXUI().getXUIComponents());
            xd.setTitle(Language.getLocalizedString(getClass(), "titlereport"));
            JTextArea ta = (JTextArea) xd.getXUI().getComponent("commentarea");
            xd.display();
            String userComment = ta.getText();
            ErrorDialog temporary = this;
            temporary.errorReport = temporary.errorReport + "\n\nUser Comment:\n" + userComment;

            getContentPane().setCursor(Cursor.getPredefinedCursor(3));

            String data = "error=" + URLEncoder.encode(this.errorReport, "UTF-8");
            URL url = new URL(errorReportUrl);
            URLConnection conn = url.openConnection();
            conn.setDoOutput(true);
            OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
            wr.write(data);
            wr.flush();
            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            wr.close();
            rd.close();

            String s = Language.getLocalizedString(getClass(), "sendok");
            if (this.fatal) {
                s = s + "\n" + Language.getLocalizedString(getClass(), "shutdown");
            }
            OptionDialog.showInformationDialog(this, s);
        } catch (Exception e1) {
            System.err.println("WARNING: Could not send error report.");
            e1.printStackTrace();
        } finally {
            getContentPane().setCursor(Cursor.getPredefinedCursor(0));
        }
        if (this.fatal) {
            System.exit(1);
        } else {
            dispose();
        }
    }

    public void EVENT_close(ActionEvent e) throws Exception {
        if (this.fatal) {
            System.exit(1);
        } else {
            dispose();
        }
    }

    public void EVENT_quit(ActionEvent e) throws Exception {
        System.exit(1);
    }

    static {
        try {
            fakeParent.setIconImage(Toolkit.getDefaultToolkit().getImage(rl.getResource("res/gfx/icons/icon.png")));
        } catch (Exception e) {
        }
    }

    public static abstract interface ExceptionTranslator {

        public abstract String translate(Throwable paramThrowable);
    }
}

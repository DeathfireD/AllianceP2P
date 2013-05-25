package org.alliance.ui.windows;

import com.stendahls.nif.ui.mdi.MDIWindow;
import com.stendahls.ui.JHtmlLabel;
import org.alliance.core.file.filedatabase.FileType;
import org.alliance.ui.UISubsystem;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-feb-09
 * Time: 14:12:18
 */
public class WelcomeMDIWindow extends AllianceMDIWindow {

    public static final String IDENTIFIER = "welcome";
    private JHtmlLabel label;
    private UISubsystem ui;

    public WelcomeMDIWindow() {
    }

    public WelcomeMDIWindow(UISubsystem ui) throws Exception {
        super(ui.getMainWindow().getMDIManager(), "welcome", ui);
        setWindowType(WINDOWTYPE_OBJECT);
        this.ui = ui;
        BufferedReader r = new BufferedReader(new InputStreamReader(ui.getRl().getResourceStream("welcome.html")));
        StringBuffer data = new StringBuffer();
        String line = null;
        while ((line = r.readLine()) != null) {
            data.append(line);
        }
        init(data.toString(), "Changelog");
    }

    private void init(String html, String title) throws Exception {
        label = (JHtmlLabel) xui.getComponent("label");
        label.setText(html);

        label.addHyperlinkListener(new HyperlinkListener() {

            @Override
            public void hyperlinkUpdate(HyperlinkEvent e) {
                String s = e.getURL().toString();
                s = s.substring(s.length() - 1);
                FileType ft = FileType.getFileTypeById(Integer.parseInt(s));

                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    try {
                        ui.getMainWindow().getMDIManager().selectWindow(ui.getMainWindow().getSearchWindow());
                        ui.getMainWindow().getSearchWindow().searchForNewFilesOfType(ft);
                    } catch (IOException e1) {
                        ui.handleErrorInEventLoop(e1);
                    }
                } else if (e.getEventType() == HyperlinkEvent.EventType.ENTERED) {
                    ui.getMainWindow().setStatusMessage("Click here to search for new files in type " + ft.description());
                }
            }
        });
        setTitle(title);
        postInit();
    }

    @Override
    public void save() throws Exception {
    }

    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }

    @Override
    public void revert() throws Exception {
    }

    @Override
    public void serialize(ObjectOutputStream out) throws IOException {
    }

    @Override
    public MDIWindow deserialize(ObjectInputStream in) throws IOException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}

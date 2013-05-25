package org.alliance.ui.windows.mdi;

import com.stendahls.nif.ui.mdi.MDIWindow;
import com.stendahls.ui.JHtmlLabel;
import org.alliance.ui.UISubsystem;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import org.alliance.core.Language;

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
        this.ui = ui;
        Language.translateXUIElements(getClass(), xui.getXUIComponents());

        BufferedReader r = new BufferedReader(new InputStreamReader(ui.getRl().getResourceStream("welcome.html")));
        StringBuffer data = new StringBuffer();
        String line = null;
        while ((line = r.readLine()) != null) {
            data.append(line);
        }
        setTitle(Language.getLocalizedString(getClass(), "title"));
        label = (JHtmlLabel) xui.getComponent("label");
        label.setText(data.toString());
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
        return null;
    }
}

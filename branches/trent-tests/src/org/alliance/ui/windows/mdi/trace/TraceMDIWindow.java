package org.alliance.ui.windows.mdi.trace;

import com.stendahls.nif.ui.mdi.MDIWindow;
import org.alliance.ui.windows.mdi.AllianceMDIWindow;
import org.alliance.ui.UISubsystem;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import javax.swing.JPanel;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-jan-05
 * Time: 13:21:55
 * To change this template use File | Settings | File Templates.
 */
public class TraceMDIWindow extends AllianceMDIWindow {

    private TraceWindow tw;

    public TraceMDIWindow(UISubsystem ui) throws Exception {
        super(ui.getMainWindow().getMDIManager(), "trace", ui);

        tw = new TraceWindow(false);
        JPanel p = (JPanel) xui.getComponent("panel");
        p.removeAll();
        p.add(tw.getContentPane());

        setTitle("Trace");
        postInit();
    }

    public void trace(int level, String text, Exception stackTrace) throws IOException {
        tw.print(level, text, stackTrace);
    }

    @Override
    public String getIdentifier() {
        return "trace";
    }

    @Override
    public void save() throws Exception {
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

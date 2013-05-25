package org.alliance.ui.windows;

import com.stendahls.nif.ui.framework.StendahlsThemeMDIWindow;
import com.stendahls.nif.ui.mdi.MDIManager;
import org.alliance.ui.UISubsystem;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-jan-05
 * Time: 13:49:55
 * To change this template use File | Settings | File Templates.
 */
public abstract class AllianceMDIWindow extends StendahlsThemeMDIWindow {

    protected UISubsystem ui;

    protected AllianceMDIWindow(MDIManager manager, String title) {
        super(manager, title);
//        setWindowType(WINDOWTYPE_NAVIGATION);
    }

    protected AllianceMDIWindow() {
//        setWindowType(WINDOWTYPE_NAVIGATION);
    }

    protected AllianceMDIWindow(MDIManager manager, String mdiWindowIdentifier, UISubsystem ui) throws Exception {
        super(manager, mdiWindowIdentifier, ui);
        this.ui = ui;
//        setWindowType(WINDOWTYPE_NAVIGATION);
    }

    protected AllianceMDIWindow(MDIManager manager, String mdiWindowIdentifier, UISubsystem ui, String xuiName) throws Exception {
        super(manager, mdiWindowIdentifier, ui, xuiName);
        this.ui = ui;
//        setWindowType(WINDOWTYPE_NAVIGATION);
    }

    protected AllianceMDIWindow(MDIManager manager, String mdiWindowIdentifier, UISubsystem ui, String xuiName, String toolbarName) throws Exception {
        super(manager, mdiWindowIdentifier, ui, xuiName, toolbarName);
        this.ui = ui;
//        setWindowType(WINDOWTYPE_NAVIGATION);
    }

    protected AllianceMDIWindow(MDIManager manager, String mdiWindowIdentifier, UISubsystem ui, String xuiName, String toolbarName, String xuiPath) throws Exception {
        super(manager, mdiWindowIdentifier, ui, xuiName, toolbarName, xuiPath);
        this.ui = ui;
//        setWindowType(WINDOWTYPE_NAVIGATION);
    }
}

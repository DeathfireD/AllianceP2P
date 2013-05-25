package org.alliance.ui.windows.mdi;

import com.stendahls.XUI.XUI;
import com.stendahls.nif.ui.mdi.MDIManager;
import com.stendahls.nif.ui.mdi.MDIWindow;
import com.stendahls.nif.ui.mdi.infonodemdi.InfoNodeMDIManager;
import org.alliance.core.Language;
import org.alliance.ui.UISubsystem;
import org.alliance.ui.themes.util.SubstanceThemeHelper;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.ObjectOutputStream;
import javax.swing.ImageIcon;
import javax.swing.JToolBar;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-jan-05
 * Time: 13:49:55
 * To change this template use File | Settings | File Templates.
 */
public abstract class AllianceMDIWindow extends MDIWindow {

    protected XUI xui;
    protected String mdiWindowIdentifier;
    protected UISubsystem ui;
    protected JToolBar toolbar;
    protected String toolbarName;

    protected AllianceMDIWindow(MDIManager manager, String title) {
        super(manager, title);
    }

    protected AllianceMDIWindow() {
        super(null, null);
    }

    public AllianceMDIWindow(MDIManager manager, String mdiWindowIdentifier, UISubsystem ui) throws Exception {
        this(manager, mdiWindowIdentifier, ui, mdiWindowIdentifier);
    }

    public AllianceMDIWindow(MDIManager manager, String mdiWindowIdentifier, UISubsystem ui, String xuiName) throws Exception {
        this(manager, mdiWindowIdentifier, ui, xuiName, mdiWindowIdentifier);
    }

    public AllianceMDIWindow(MDIManager manager, String mdiWindowIdentifier, UISubsystem ui, String xuiName, String toolbarName) throws Exception {
        this(manager, mdiWindowIdentifier, ui, xuiName, toolbarName, "xui");
    }

    public AllianceMDIWindow(MDIManager manager, String mdiWindowIdentifier, UISubsystem ui, String xuiName, String toolbarName, String xuiPath) throws Exception {
        super(manager, "");

        this.mdiWindowIdentifier = mdiWindowIdentifier;
        this.ui = ui;
        this.toolbarName = toolbarName;

        this.xui = new XUI();

        this.xui.init(ui.getRl(), xuiPath + "/mdi/" + xuiName + ".xui.xml");
        for (Object o : xui.getComponents()) {
            SubstanceThemeHelper.setColorization(o, new Double(0.0));
        }
        SubstanceThemeHelper.setButtonsToGeneralArea(xui.getXUIComponents());
        SubstanceThemeHelper.setPanelsToFooterArea(xui.getXUIComponents());

        this.xui.setEventHandler(this);
        this.xui.setOpaque(false);
        setOpaque(false);

        this.toolbar = ((JToolBar) this.xui.getComponent("toolbar"));
        ui.getToolbarActionManager().fillToolbar(this.toolbar, toolbarName, this);
        Language.getLocalizedXUIToolbar(getClass(), ui.getToolbarActionManager().getButtonsBy(this));

        setIcon(new ImageIcon(ui.getRl().getResource("gfx/icons/" + mdiWindowIdentifier + ".png")));
    }

    protected void postInit() {
        removeAll();
        setLayout(new BorderLayout());
        add(this.xui);
    }

    public String getToolbarName() {
        return this.toolbarName;
    }

    public void EVENT_close(ActionEvent e) {
        InfoNodeMDIManager m = (InfoNodeMDIManager) this.manager;
        m.removeWindow(this, true, true);
    }

    public void EVENT_revert(ActionEvent e) throws Exception {
        pleaseRevert();
    }

    @Override
    public abstract void serialize(ObjectOutputStream paramObjectOutputStream) throws IOException;
}

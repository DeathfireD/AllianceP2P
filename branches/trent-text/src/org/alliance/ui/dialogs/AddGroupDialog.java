package org.alliance.ui.dialogs;

import com.stendahls.XUI.XUIDialog;
import org.alliance.core.Language;
import org.alliance.ui.UISubsystem;
import org.alliance.ui.themes.util.SubstanceThemeHelper;

import java.awt.event.ActionEvent;
import javax.swing.JFrame;
import javax.swing.JTextField;

/**
 *
 * @author Bastvera
 */
public class AddGroupDialog extends XUIDialog {

    private String groupName;

    public AddGroupDialog(UISubsystem ui, JFrame f) throws Exception {
        super(ui.getRl(), ui.getRl().getResourceStream("xui/groupdialog.xui.xml"), ui.getMainWindow(), true);
        Language.translateXUIElements(getClass(), xui.getXUIComponents());
        SubstanceThemeHelper.setButtonsToGeneralArea(xui.getXUIComponents());
        ((JTextField) xui.getComponent("customtext")).selectAll();
        ((JTextField) xui.getComponent("customtext")).requestFocus();
        setTitle(Language.getLocalizedString(getClass(), "title"));
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        pack();
        display();
        this.requestFocus();
    }

    public String getGroupName() {
        return groupName;
    }

    public void EVENT_confirm(ActionEvent e) throws Exception {
        groupName = ((JTextField) xui.getComponent("customtext")).getText();
        dispose();
    }
}

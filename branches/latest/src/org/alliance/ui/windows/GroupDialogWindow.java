package org.alliance.ui.windows;

import com.stendahls.XUI.XUIDialog;
import java.awt.event.ActionEvent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import org.alliance.ui.UISubsystem;

/**
 *
 * @author Bastvera
 */
public class GroupDialogWindow extends XUIDialog {

    private String groupname;

    public GroupDialogWindow(UISubsystem ui, JFrame f) throws Exception {
        super(ui.getRl(), ui.getRl().getResourceStream("xui/groupdialogwindow.xui.xml"), ui.getMainWindow(), true);
        ((JRadioButton) xui.getComponent("selectedpublic")).setSelected(true);
        EVENT_selectedpublic(null);
        display();
        this.requestFocus();
    }

    public String getGroupname() {
        return groupname;
    }

    public void EVENT_selectedpublic(ActionEvent e) {
        ((JPanel) xui.getComponent("customp1")).setVisible(false);
        ((JPanel) xui.getComponent("customp2")).setVisible(false);
    }

    public void EVENT_selectedcustom(ActionEvent e) {
        ((JPanel) xui.getComponent("customp1")).setVisible(true);
        ((JPanel) xui.getComponent("customp2")).setVisible(true);
        ((JTextField) xui.getComponent("customtext")).setText("Enter new group name");
        ((JTextField) xui.getComponent("customtext")).requestFocus();
        ((JTextField) xui.getComponent("customtext")).selectAll();
    }

    public void EVENT_confirm(ActionEvent e) {
        if (((JRadioButton) xui.getComponent("selectedcustom")).isSelected()) {
            if (!((JTextField) xui.getComponent("customtext")).getText().equals("Enter new group name")) {
                groupname = ((JTextField) xui.getComponent("customtext")).getText();
                dispose();
            }
        } else {
            groupname = "Public";
            dispose();
        }
    }
}

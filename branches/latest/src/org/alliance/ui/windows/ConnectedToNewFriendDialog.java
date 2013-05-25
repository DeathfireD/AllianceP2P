package org.alliance.ui.windows;

import com.stendahls.XUI.XUIDialog;
import com.stendahls.nif.ui.OptionDialog;
import com.stendahls.ui.JHtmlLabel;
import org.alliance.ui.UISubsystem;

import javax.swing.JCheckBox;
import javax.swing.JFrame;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2008-maj-02
 * Time: 14:46:04
 * To change this template use File | Settings | File Templates.
 */
public class ConnectedToNewFriendDialog extends XUIDialog {

    public ConnectedToNewFriendDialog(UISubsystem ui, JFrame f, String name) throws Exception {
        super(ui.getRl(), ui.getRl().getResourceStream("xui/newfriendconnection.xui.xml"), f, true);
        ((JHtmlLabel) xui.getComponent("label")).replaceString("$$NAME$$", name);
        ui.getMainWindow().setConnectedToNewFriendDialogShowing(true);
        display();
        JCheckBox cb = (JCheckBox) xui.getComponent("dontshow");
        if (cb.isSelected()) {
            if (OptionDialog.showQuestionDialog(this, "Next time a new friend connects to you no message will be shown. Is this what you want?")) {
                ui.getCore().getSettings().getInternal().setDisablenewuserpopup(1);
            }
        }
        ui.getMainWindow().setConnectedToNewFriendDialogShowing(false);
    }
}

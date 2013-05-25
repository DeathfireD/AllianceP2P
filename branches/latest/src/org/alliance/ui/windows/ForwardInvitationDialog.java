package org.alliance.ui.windows;

import com.stendahls.XUI.XUIDialog;
import com.stendahls.ui.JHtmlLabel;
import org.alliance.core.interactions.PleaseForwardInvitationInteraction;
import org.alliance.core.node.FriendManager;
import org.alliance.ui.UISubsystem;

import java.awt.event.ActionEvent;
import javax.swing.JCheckBox;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-mar-20
 * Time: 22:33:46
 * To change this template use File | Settings | File Templates.
 */
public class ForwardInvitationDialog extends XUIDialog {

    private JCheckBox alwaysAllowInvite;
    private boolean pressedYes = false;

    public ForwardInvitationDialog(UISubsystem ui, PleaseForwardInvitationInteraction pmi) throws Exception {
        super(ui.getMainWindow());

        init(ui.getRl(), ui.getRl().getResourceStream("xui/forwardinvitation.xui.xml"));

        FriendManager fm = ui.getCore().getFriendManager();
        String from = fm.nicknameWithContactPath(pmi.getFromGuid());
        String to = fm.nicknameWithContactPath(pmi.getToGuid());

        ((JHtmlLabel) xui.getComponent("label")).setText(from + " wants to connect to " + to + ". These two users are not connected right now. You are the connection between them.<p>Do you want to allow " + from + " to connect to " + to + "?");

        alwaysAllowInvite = (JCheckBox) xui.getComponent("alwaysAllowInvite");

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        display();
    }

    public boolean hasPressedYes() {
        return pressedYes;
    }

    public boolean alwaysAllowInvite() {
        return alwaysAllowInvite.isSelected();
    }

    public void EVENT_yes(ActionEvent a) throws Exception {
        pressedYes = true;
        dispose();
    }

    public void EVENT_no(ActionEvent a) throws Exception {
        dispose();
    }
}

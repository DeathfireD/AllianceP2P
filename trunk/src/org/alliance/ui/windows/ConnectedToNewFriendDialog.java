package org.alliance.ui.windows;

import com.stendahls.XUI.XUIDialog;
import com.stendahls.ui.JHtmlLabel;
import org.alliance.core.Language;
import org.alliance.ui.UISubsystem;
import org.alliance.ui.dialogs.OptionDialog;
import org.alliance.ui.themes.util.SubstanceThemeHelper;

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
        Language.translateXUIElements(getClass(), xui.getXUIComponents());
        SubstanceThemeHelper.setButtonsToGeneralArea(xui.getXUIComponents());
        setTitle(Language.getLocalizedString(getClass(), "title"));
        JHtmlLabel label = (JHtmlLabel) xui.getComponent("label");
        label.setText(Language.getLocalizedString(getClass(), "xui.label", name));
        ui.getMainWindow().setConnectedToNewFriendDialogShowing(true);
        display();
        JCheckBox cb = (JCheckBox) xui.getComponent("dontshow");
        if (cb.isSelected()) {
            if (OptionDialog.showQuestionDialog(this, Language.getLocalizedString(getClass(), "question"))) {
                ui.getCore().getSettings().getInternal().setDisablenewuserpopup(1);
            }
        }
        ui.getMainWindow().setConnectedToNewFriendDialogShowing(false);
    }
}

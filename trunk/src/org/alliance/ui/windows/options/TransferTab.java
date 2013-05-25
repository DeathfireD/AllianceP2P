package org.alliance.ui.windows.options;

import com.stendahls.XUI.XUI;
import com.stendahls.XUI.XUIDialog;
import static org.alliance.core.CoreSubsystem.KB;
import java.awt.event.ActionEvent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import org.alliance.core.Language;
import org.alliance.ui.UISubsystem;
import org.alliance.ui.themes.util.SubstanceThemeHelper;

import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 *
 * @author Bastvera
 */
public class TransferTab extends XUIDialog implements TabHelper {

    private JPanel tab;
    private UISubsystem ui;
    private final static String[] OPTIONS = new String[]{
        "internal.downloadfolder", "internal.uploadthrottle"};

    public TransferTab(String loading) {
        tab = new JPanel();
        tab.add(new JLabel(loading));
        tab.setName(Language.getLocalizedString(getClass(), "title"));
        tab.setToolTipText(Language.getLocalizedString(getClass(), "tooltip"));
    }

    public TransferTab(final UISubsystem ui) throws Exception {
        init(ui.getRl(), ui.getRl().getResourceStream("xui/optionstabs/transfertab.xui.xml"));
        this.ui = ui;

        Language.translateXUIElements(getClass(), xui.getXUIComponents());
        SubstanceThemeHelper.setButtonsToGeneralArea(xui.getXUIComponents());
        tab = (JPanel) xui.getComponent("transfertab");
        tab.setName(Language.getLocalizedString(getClass(), "title"));
        tab.setToolTipText(Language.getLocalizedString(getClass(), "tooltip"));
    }

    public void EVENT_browse(ActionEvent e) {
        JTextField download = (JTextField) xui.getComponent("internal.downloadfolder");
        JFileChooser fc = new JFileChooser(download.getText());
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int returnVal = fc.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            String path = fc.getSelectedFile().getPath();
            download.setText(path);
        }
    }

    @Override
    public JPanel getTab() {
        return tab;
    }

    @Override
    public String[] getOptions() {
        return OPTIONS;
    }

    @Override
    public XUI getXUI() {
        return super.getXUI();
    }

    @Override
    public boolean isAllowedEmpty(String option) {
        return false;
    }

    @Override
    public String getOverridedSettingValue(String option, String value) {
        return value;
    }

    @Override
    public Object getOverridedComponentValue(String option, Object value) {
        if (value == null || value.toString().isEmpty()) {
            if (!isAllowedEmpty(option)) {
                return null;
            }
        }
        if (option.equals("internal.uploadthrottle")) {
            ui.getCore().getNetworkManager().getUploadThrottle().setRate((Integer.parseInt(value.toString())) * KB);
            if ((Integer.parseInt(value.toString())) != ui.getCore().getSettings().getInternal().getUploadthrottle()) {
                ui.getCore().getNetworkManager().getBandwidthOut().resetHighestCPS();
            }
        }
        return value;
    }

    @Override
    public void postOperation() {
    }
}

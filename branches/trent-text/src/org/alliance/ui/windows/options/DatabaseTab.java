package org.alliance.ui.windows.options;

import com.stendahls.XUI.XUI;
import com.stendahls.XUI.XUIDialog;
import org.alliance.ui.dialogs.OptionDialog;
import org.alliance.core.Language;
import org.alliance.ui.UISubsystem;
import org.alliance.ui.themes.util.SubstanceThemeHelper;

import java.awt.event.ActionEvent;
import java.io.File;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.filechooser.FileFilter;

/**
 *
 * @author Bastvera
 */
public class DatabaseTab extends XUIDialog implements TabHelper {

    private JPanel tab;
    private UISubsystem ui;
    private final static String[] OPTIONS = new String[]{"internal.hashspeedinmbpersecond",
        "internal.politehashingintervalingigabytes", "internal.politehashingwaittimeinminutes",
        "internal.rescansharewhenalliancestarts"};

    public DatabaseTab(String loading) {
        tab = new JPanel();
        tab.add(new JLabel(loading));
        tab.setName(Language.getLocalizedString(getClass(), "title"));
        tab.setToolTipText(Language.getLocalizedString(getClass(), "tooltip"));
    }

    public DatabaseTab(final UISubsystem ui) throws Exception {
        init(ui.getRl(), ui.getRl().getResourceStream("xui/optionstabs/databasetab.xui.xml"));
        this.ui = ui;

        Language.translateXUIElements(getClass(), xui.getXUIComponents());
        SubstanceThemeHelper.setButtonsToGeneralArea(xui.getXUIComponents());
        tab = (JPanel) xui.getComponent("databasetab");
        tab.setName(Language.getLocalizedString(getClass(), "title"));
        tab.setToolTipText(Language.getLocalizedString(getClass(), "tooltip"));
    }

    public void EVENT_browse(ActionEvent e) throws Exception {
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fc.setAcceptAllFileFilterUsed(false);

        fc.addChoosableFileFilter(new FileFilter() {

            @Override
            public boolean accept(File pathname) {
                if ((pathname.toString().contains("alliance-script-") && pathname.toString().endsWith(".zip")) || pathname.isDirectory()) {
                    return true;
                } else {
                    return false;
                }
            }

            @Override
            public String getDescription() {
                return ("DB Script");
            }
        });

        int returnVal = fc.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            String path = fc.getSelectedFile().getPath();
            ui.getCore().getFileManager().getDbCore().connect(ui.getCore().getFileManager().prepareToRestore(path));
            ui.getCore().getFileManager().getFileDatabase().updateCacheCounters();
            OptionDialog.showInformationDialog(this, Language.getLocalizedString(getClass(), "restoreok"));
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
        return value;
    }

    @Override
    public void postOperation() {
    }
}

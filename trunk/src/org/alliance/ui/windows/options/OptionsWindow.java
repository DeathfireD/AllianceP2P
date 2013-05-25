package org.alliance.ui.windows.options;

import com.stendahls.XUI.XUIDialog;
import org.alliance.core.Language;
import org.alliance.ui.UISubsystem;
import org.alliance.ui.themes.util.SubstanceThemeHelper;

import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import org.alliance.core.settings.SettingClass;
import org.alliance.ui.dialogs.OptionDialog;

/**
 *
 * @author Bastvera
 */
public class OptionsWindow extends XUIDialog {

    private UISubsystem ui;
    private ArrayList<TabHelper> tabs = new ArrayList<TabHelper>();

    public OptionsWindow(final UISubsystem ui) throws Exception {
        super(ui.getMainWindow());
        this.ui = ui;

        init(ui.getRl(), ui.getRl().getResourceStream("xui/optionswindow.xui.xml"));
        Language.translateXUIElements(getClass(), xui.getXUIComponents());
        SubstanceThemeHelper.setButtonsToGeneralArea(xui.getXUIComponents());
        setTitle(Language.getLocalizedString(getClass(), "title"));

        final JTabbedPane tabPane = (JTabbedPane) xui.getComponent("tab");

        for (int i = 1; i <= 7; i++) {
            createTab(i, tabPane, true);
            if (i == 1) {
                createTab(i, tabPane, false);
            }
        }

        tabPane.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseReleased(MouseEvent e) {
                try {
                    if (((JPanel) tabPane.getSelectedComponent()).getComponent(0) instanceof JLabel) {
                        setPreferredSize(getSize());
                        createTab(tabPane.getSelectedIndex() + 1, tabPane, false);
                    }
                } catch (Exception ex) {
                    // ex.printStackTrace();
                }
            }
        });

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        showWindow();
    }

    private void showWindow() {
        if (ui.getMainWindow().loadWindowState(getTitle(), this)) {
            setVisible(true);
            toFront();
        } else {
            display();
        }
    }

    private void createTab(int tabNumber, JTabbedPane tabPane, boolean dummy) throws Exception {
        String loading = Language.getLocalizedString(getClass(), "loading");
        switch (tabNumber) {
            case 1:
                addTab(tabPane, dummy ? new GeneralTab(loading) : new GeneralTab(ui), dummy, tabNumber);
                break;
            case 2:
                addTab(tabPane, dummy ? new SoundTab(loading) : new SoundTab(ui), dummy, tabNumber);
                break;
            case 3:
                addTab(tabPane, dummy ? new NetworkTab(loading) : new NetworkTab(ui), dummy, tabNumber);
                break;
            case 4:
                addTab(tabPane, dummy ? new FirewallTab(loading) : new FirewallTab(ui), dummy, tabNumber);
                break;
            case 5:
                addTab(tabPane, dummy ? new TransferTab(loading) : new TransferTab(ui), dummy, tabNumber);
                break;
            case 6:
                addTab(tabPane, dummy ? new DatabaseTab(loading) : new DatabaseTab(ui), dummy, tabNumber);
                break;
            case 7:
                addTab(tabPane, dummy ? new SecurityTab(loading) : new SecurityTab(ui), dummy, tabNumber);
                break;
            default:
        }
    }

    private void addTab(final JTabbedPane tabPane, TabHelper tab, boolean dummy, final int tabNumber) throws Exception {
        final JPanel panel = tab.getTab();
        if (dummy) {
            tabPane.add(panel);
            tabPane.setToolTipTextAt(tabPane.getTabCount() - 1, panel.getToolTipText());
            panel.setToolTipText(null);
            return;
        }
        tabPane.setToolTipTextAt(tabNumber - 1, panel.getToolTipText());
        panel.setToolTipText(null);
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                tabPane.setComponentAt(tabNumber - 1, panel);
            }
        });
        for (String option : tab.getOptions()) {
            JComponent c = (JComponent) tab.getXUI().getComponent(option);
            if (c != null) {
                setComponentValue(c, tab.getOverridedSettingValue(option, getSettingValue(option)));
            }
        }
        tab.postOperation();
        tabs.add(tab);
    }

    private String getSettingValue(String option) throws Exception {
        String className = option.substring(0, option.indexOf('.'));
        option = option.substring(option.indexOf('.') + 1);
        SettingClass setting = getSettingClass(className);
        return String.valueOf(setting.getValue(option));
    }

    private SettingClass getSettingClass(String className) throws Exception {
        if (className.equals("internal")) {
            return ui.getCore().getSettings().getInternal();
        } else if (className.equals("my")) {
            return ui.getCore().getSettings().getMy();
        } else if (className.equals("server")) {
            return ui.getCore().getSettings().getServer();
        } else {
            throw new Exception("Could not find class type: " + className);
        }
    }

    private void setComponentValue(JComponent c, String settingValue) {
        if (c instanceof JTextField) {
            ((JTextField) c).setText(settingValue);
        } else if (c instanceof JCheckBox) {
            if ("0".equals(settingValue)) {
                ((JCheckBox) c).setSelected(false);
            } else {
                ((JCheckBox) c).setSelected(true);
            }
        } else if (c instanceof JComboBox) {
            try {
                ((JComboBox) c).setSelectedIndex(Integer.parseInt(settingValue));
            } catch (NumberFormatException e) {
                ((JComboBox) c).setSelectedItem(settingValue);
            }
        }
    }

    private Object getComponentValue(JComponent c) {
        if (c instanceof JTextField) {
            return ((JTextField) c).getText().trim();
        }
        if (c instanceof JCheckBox) {
            return ((JCheckBox) c).isSelected() ? 1 : 0;
        }
        if (c instanceof JComboBox) {
            return ((JComboBox) c).getSelectedIndex();
        }
        return null;
    }

    private void setSettingValue(String option, Object val) throws Exception {
        String className = option.substring(0, option.indexOf('.'));
        option = option.substring(option.indexOf('.') + 1);
        SettingClass setting = getSettingClass(className);
        setting.setValue(option, val);
    }

    private boolean apply() throws Exception {
        for (TabHelper tab : tabs) {
            for (String option : tab.getOptions()) {
                JComponent c = (JComponent) tab.getXUI().getComponent(option);
                Object value = tab.getOverridedComponentValue(option, getComponentValue(c));
                if (value == null) {
                    OptionDialog.showErrorDialog(this, "One or more required fields is empty on tab: " + tab.getTab().getName());
                    return false;
                }
                setSettingValue(option, value);
            }
        }
        ui.getCore().saveSettings();
        return true;
    }

    public void EVENT_ok(ActionEvent a) throws Exception {
        if (apply()) {
            ui.getMainWindow().saveWindowState(getTitle(), getLocation(), getSize(), -1);
            ui.getCore().getShareManager().updateShareBases();
            dispose();
        }
    }

    public void EVENT_cancel(ActionEvent a) throws Exception {
        dispose();
    }

    public void EVENT_apply(ActionEvent a) throws Exception {
        apply();
    }
}

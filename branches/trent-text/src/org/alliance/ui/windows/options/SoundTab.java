package org.alliance.ui.windows.options;

import com.stendahls.XUI.XUI;
import com.stendahls.XUI.XUIDialog;
import java.awt.event.ActionEvent;
import java.io.File;
import javax.swing.filechooser.FileFilter;
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
public class SoundTab extends XUIDialog implements TabHelper {

    private JPanel tab;
    private UISubsystem ui;
    private final static String[] OPTIONS = new String[]{
        "internal.pmsound", "internal.downloadsound", "internal.publicsound"};

    public SoundTab(String loading) {
        tab = new JPanel();
        tab.add(new JLabel(loading));
        tab.setName(Language.getLocalizedString(getClass(), "title"));
        tab.setToolTipText(Language.getLocalizedString(getClass(), "tooltip"));
    }

    public SoundTab(UISubsystem ui) throws Exception {
        init(ui.getRl(), ui.getRl().getResourceStream("xui/optionstabs/soundtab.xui.xml"));
        this.ui = ui;

        Language.translateXUIElements(getClass(), xui.getXUIComponents());
        SubstanceThemeHelper.setButtonsToGeneralArea(xui.getXUIComponents());
        tab = (JPanel) xui.getComponent("soundtab");
        tab.setName(Language.getLocalizedString(getClass(), "title"));
        tab.setToolTipText(Language.getLocalizedString(getClass(), "tooltip"));
    }

    private void browseSound(String s) {
        JFileChooser fc = new JFileChooser(((JTextField) xui.getComponent(s)).getText());
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fc.setAcceptAllFileFilterUsed(false);

        fc.addChoosableFileFilter(new FileFilter() {

            @Override
            public boolean accept(File pathname) {
                if (pathname.toString().endsWith("wav") || pathname.isDirectory()) {
                    return true;
                } else {
                    return false;
                }
            }

            @Override
            public String getDescription() {
                return ("Wave files");
            }
        });

        int returnVal = fc.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            String path = fc.getSelectedFile().getPath();
            ((JTextField) xui.getComponent(s)).setText(path);
        }
    }

    public void EVENT_browsepm(ActionEvent e) {
        browseSound("internal.pmsound");
    }

    public void EVENT_browsedownload(ActionEvent e) {
        browseSound("internal.downloadsound");
    }

    public void EVENT_browsepublic(ActionEvent e) {
        browseSound("internal.publicsound");
    }

    public void EVENT_sounddefault(ActionEvent e) {
        ((JTextField) xui.getComponent("internal.pmsound")).setText("sounds/chatpm.wav");
        ((JTextField) xui.getComponent("internal.downloadsound")).setText("sounds/download.wav");
        ((JTextField) xui.getComponent("internal.publicsound")).setText("sounds/chatpublic.wav");
    }

    public void EVENT_soundmute(ActionEvent e) {
        ((JTextField) xui.getComponent("internal.pmsound")).setText("");
        ((JTextField) xui.getComponent("internal.downloadsound")).setText("");
        ((JTextField) xui.getComponent("internal.publicsound")).setText("");
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
        if (option.equals("internal.pmsound") || option.equals("internal.downloadsound") || option.equals("internal.publicsound")) {
            return true;
        }
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

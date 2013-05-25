package org.alliance.ui.windows.options;

import com.stendahls.XUI.XUI;
import com.stendahls.XUI.XUIDialog;
import com.stendahls.nif.util.EnumerationIteratorConverter;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.DefaultListModel;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import org.alliance.core.Language;
import org.alliance.ui.UISubsystem;
import org.alliance.ui.themes.util.SubstanceThemeHelper;

import javax.swing.JPanel;
import org.alliance.core.settings.Routerule;
import org.alliance.ui.windows.AddRuleWindow;

/**
 *
 * @author Bastvera
 */
public class FirewallTab extends XUIDialog implements TabHelper {

    private JList ruleList;
    private DefaultListModel ruleListModel;
    private JPanel tab;
    private UISubsystem ui;
    private final static String[] OPTIONS = new String[]{"internal.enableiprules"};

    public FirewallTab(String loading) {
        tab = new JPanel();
        tab.add(new JLabel(loading));
        tab.setName(Language.getLocalizedString(getClass(), "title"));
        tab.setToolTipText(Language.getLocalizedString(getClass(), "tooltip"));
    }

    public FirewallTab(final UISubsystem ui) throws Exception {
        init(ui.getRl(), ui.getRl().getResourceStream("xui/optionstabs/firewalltab.xui.xml"));
        this.ui = ui;

        Language.translateXUIElements(getClass(), xui.getXUIComponents());
        SubstanceThemeHelper.setButtonsToGeneralArea(xui.getXUIComponents());
        SubstanceThemeHelper.flatButton((JComponent) xui.getComponent("addrule"));
        SubstanceThemeHelper.flatButton((JComponent) xui.getComponent("removerule"));
        SubstanceThemeHelper.flatButton((JComponent) xui.getComponent("moveruleup"));
        SubstanceThemeHelper.flatButton((JComponent) xui.getComponent("moveruledown"));
        SubstanceThemeHelper.flatButton((JComponent) xui.getComponent("editrule"));
        tab = (JPanel) xui.getComponent("firewalltab");
        tab.setName(Language.getLocalizedString(getClass(), "title"));
        tab.setToolTipText(Language.getLocalizedString(getClass(), "tooltip"));

        ruleList = (JList) xui.getComponent("rulelist");
        ruleListModel = new DefaultListModel();
        final JCheckBox ipfilter = ((JCheckBox) xui.getComponent("internal.enableiprules"));
        ipfilter.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (ipfilter.isSelected()) {
                    ruleList.setEnabled(true);
                } else {
                    ruleList.setEnabled(false);
                }
            }
        });
    }

    public void EVENT_addrule(ActionEvent e) throws Exception {
        AddRuleWindow window = new AddRuleWindow(ui);
        if (window.getHuman() == null) {
            //This is here to take care of the case of a user adding a rule, then hitting cancel
            return;
        }
        ruleListModel.add(ruleListModel.size(), new Routerule(window.getHuman()));
        ruleList.revalidate();
        ruleList.setSelectedIndex(ruleListModel.size() - 1);
    }

    public void EVENT_editrule(ActionEvent e) throws Exception {
        if (ruleList.getSelectedIndex() != -1) {
            Routerule temp = (Routerule) ruleListModel.get(ruleList.getSelectedIndex());
            int ruleIndex = ruleList.getSelectedIndex();
            AddRuleWindow window = new AddRuleWindow(ui, ruleIndex, temp.getHumanreadable());
            if (window.getHuman() != null) {
                ruleListModel.remove(ruleList.getSelectedIndex());
                ruleListModel.add(ruleIndex, new Routerule(window.getHuman()));
                ruleList.revalidate();
                ruleList.setSelectedIndex(ruleIndex);
            }
        }
    }

    public void EVENT_moveruleup(ActionEvent e) {
        if (ruleList.getSelectedIndex() != -1) {
            int ruleIndex = ruleList.getSelectedIndex();
            if (ruleIndex <= 0 || ruleIndex > ruleListModel.size()) {
                return;
            }
            Object temp = ruleListModel.get(ruleIndex);
            ruleListModel.remove(ruleList.getSelectedIndex());
            ruleListModel.add(ruleIndex - 1, temp);
            ruleList.revalidate();
            ruleList.setSelectedIndex(ruleIndex - 1);
        }
    }

    public void EVENT_moveruledown(ActionEvent e) {
        if (ruleList.getSelectedIndex() != -1) {
            int ruleIndex = ruleList.getSelectedIndex();
            if (ruleIndex < 0 || ruleIndex >= ruleListModel.size() - 1) {
                return;
            }
            Object temp = ruleListModel.get(ruleIndex);
            ruleListModel.remove(ruleList.getSelectedIndex());
            ruleListModel.add(ruleIndex + 1, temp);
            ruleList.revalidate();
            ruleList.setSelectedIndex(ruleIndex + 1);
        }
    }

    public void EVENT_removerule(ActionEvent e) {
        if (ruleList.getSelectedIndex() != -1) {
            ruleListModel.remove(ruleList.getSelectedIndex());
            ruleList.revalidate();
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
        if (option.equals("internal.enableiprules") && value.equals("0")) {
            ruleList.setEnabled(false);
        }
        return value;
    }

    @Override
    public Object getOverridedComponentValue(String option, Object value) {
        if (value == null || value.toString().isEmpty()) {
            if (!isAllowedEmpty(option)) {
                return null;
            }
        }
        if (option.equals("internal.enableiprules")) {
            ui.getCore().getSettings().getRulelist().clear();
            for (Routerule rule : EnumerationIteratorConverter.iterable(ruleListModel.elements(), Routerule.class)) {
                ui.getCore().getSettings().getRulelist().add(rule);
            }
        }
        return value;
    }

    @Override
    public void postOperation() {
        for (Routerule rule : ui.getCore().getSettings().getRulelist()) {
            ruleListModel.addElement(rule);
        }
        ruleList.setModel(ruleListModel);
    }
}

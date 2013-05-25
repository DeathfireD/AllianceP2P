package org.alliance.ui.windows;

import com.stendahls.XUI.XUIDialog;
import com.stendahls.nif.util.EnumerationIteratorConverter;
import org.alliance.core.settings.Plugin;
import org.alliance.core.settings.Settings;
import org.alliance.core.Language;
import org.alliance.ui.UISubsystem;
import org.alliance.ui.themes.util.SubstanceThemeHelper;

import java.awt.event.ActionEvent;
import java.io.IOException;
import javax.swing.DefaultListModel;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

public class AddPluginWindow extends XUIDialog {

    private Settings settings;
    private DefaultListModel pluginListModel;
    private JList pluginList;
    private boolean addedOrRemovedSomething = false;
    private UISubsystem ui;

    public AddPluginWindow(UISubsystem ui) throws Exception {
        super(ui.getMainWindow());
        this.ui = ui;
        this.settings = ui.getCore().getSettings();
        init(ui.getRl(), ui.getRl().getResourceStream("xui/pluginwindow.xui.xml"));
        Language.translateXUIElements(getClass(), xui.getXUIComponents());
        SubstanceThemeHelper.setButtonsToGeneralArea(xui.getXUIComponents());
        setTitle(Language.getLocalizedString(getClass(), "title"));
        pluginList = (JList) xui.getComponent("pluginList");
        pluginListModel = new DefaultListModel();

        for (Plugin plugin : settings.getPluginlist()) {
            pluginListModel.addElement(plugin);
        }
        pluginList.setModel(pluginListModel);
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

    public void EVENT_cancel(ActionEvent a) throws Exception {
        dispose();
    }

    public void EVENT_ok(ActionEvent a) throws Exception {
        settings.getPluginlist().clear();
        for (Plugin plugin : EnumerationIteratorConverter.iterable(pluginListModel.elements(), Plugin.class)) {
            settings.getPluginlist().add(plugin);
        }
        ui.getCore().saveSettings();
        if (this.addedOrRemovedSomething) {
            //restart the plugin subsystem
            ui.getCore().getPluginManager().shutdown();
            ui.getCore().getPluginManager().init();
        }
        ui.getMainWindow().saveWindowState(getTitle(), getLocation(), getSize(), -1);
        dispose();
    }

    public void EVENT_addplugin(ActionEvent a) throws Exception {
        JFileChooser file = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter(Language.getLocalizedString(getClass(), "files"), "JAR", "jar", "Jar");
        file.setFileFilter(filter);
        int returnVal = file.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            try {
                Plugin newPlugin = new Plugin();
                newPlugin.init(file.getSelectedFile());
                pluginListModel.add(pluginListModel.size(), newPlugin);
                this.addedOrRemovedSomething = true;
            } catch (IOException e) {
                JOptionPane.showMessageDialog(null, Language.getLocalizedString(getClass(), "error"));
            }
        }
    }

    public void EVENT_removeplugin(ActionEvent a) throws Exception {
        if (pluginList.getSelectedIndex() != -1) {
            pluginListModel.remove(pluginList.getSelectedIndex());
            pluginList.revalidate();
            this.addedOrRemovedSomething = true;
        }
    }
}

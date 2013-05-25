package org.alliance.ui.windows;

import org.alliance.core.CoreSubsystem;
import org.alliance.core.settings.Plugin;
import org.alliance.core.settings.Settings;
import org.alliance.ui.UISubsystem;
import com.stendahls.XUI.XUIDialog;
import com.stendahls.nif.util.EnumerationIteratorConverter;

import java.awt.event.ActionEvent;
import java.io.IOException;
import javax.swing.DefaultListModel;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

public class AddPluginWindow extends XUIDialog {

    private Settings settings;
    private CoreSubsystem core;
    private DefaultListModel pluginListModel;
    private JList pluginList;
    private boolean addedOrRemovedSomething = false;

    public AddPluginWindow(UISubsystem ui) throws Exception {
        super(ui.getMainWindow());
        this.settings = ui.getCore().getSettings();
        this.core = ui.getCore();
        init(ui.getRl(), ui.getRl().getResourceStream(
                "xui/pluginwindow.xui.xml"));
        pluginList = (JList) xui.getComponent("pluginList");
        pluginListModel = new DefaultListModel();

        for (Plugin plugin : settings.getPluginlist()) {
            pluginListModel.addElement(plugin);
        }
        pluginList.setModel(pluginListModel);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        pack();
        display();
    }

    public void EVENT_cancel(ActionEvent a) throws Exception {
        dispose();
    }

    public void EVENT_ok(ActionEvent a) throws Exception {
        settings.getPluginlist().clear();
        for (Plugin plugin : EnumerationIteratorConverter.iterable(
                pluginListModel.elements(), Plugin.class)) {
            settings.getPluginlist().add(plugin);
        }
        core.saveSettings();
        if (this.addedOrRemovedSomething) {
            //restart the plugin subsystem
            core.getPluginManager().shutdown();
            core.getPluginManager().init();
        }
        dispose();
    }

    public void EVENT_addplugin(ActionEvent a) throws Exception {
        JFileChooser file = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter("JAR files", "JAR", "jar", "Jar");
        file.setFileFilter(filter);
        int returnVal = file.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            try {
                Plugin newPlugin = new Plugin();
                newPlugin.init(file.getSelectedFile());
                pluginListModel.add(pluginListModel.size(), newPlugin);
                this.addedOrRemovedSomething = true;
            } catch (IOException e) {
                JOptionPane.showMessageDialog(null,
                        "Could not parse given jar file to find entry point");
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

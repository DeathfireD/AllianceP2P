package org.alliance.core.plugins;

import org.alliance.core.Manager;
import org.alliance.core.CoreSubsystem;
import org.alliance.core.settings.Plugin;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URLClassLoader;
import java.net.URL;
import java.util.List;
import java.util.ArrayList;
import java.util.ListIterator;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2008-jun-05
 * Time: 19:04:56
 * To change this template use File | Settings | File Templates.
 */
public class PluginManager extends Manager {

    private CoreSubsystem core;
    private URLClassLoader classLoader;
    private List<PlugIn> plugIns = new ArrayList<PlugIn>();
    private List<ConsolePlugInExtension> plugInConsoleExtensions;

    public PluginManager(CoreSubsystem core) {
        this.core = core;
    }

    @Override
    public void init() throws Exception {
    	plugInConsoleExtensions = new ArrayList<ConsolePlugInExtension>();
        if (!core.getSettings().getPluginlist().isEmpty()) {
            setupClassLoader();
            for (org.alliance.core.settings.Plugin p : core.getSettings().getPluginlist()) {
                try {
                    PlugIn pi = (PlugIn) classLoader.loadClass(p.retrievePluginClass()).newInstance();
                    pi.init(core);
                    plugInConsoleExtensions.add(pi.getConsoleExtensions());
                    plugIns.add(pi);
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(null, 
                            "There was an error loading the main class of  " + p.getJar() + " so this plugin will be disabled");
                }
            }
        }
    }
    
    public List<ConsolePlugInExtension> getPluginConsoleExtensions() {
    	return plugInConsoleExtensions;
    }

    private void setupClassLoader() throws Exception {
        List<URL> l = new ArrayList<URL>();
        for(ListIterator<Plugin> itr = core.getSettings().getPluginlist().listIterator(); itr.hasNext(); ) {
            org.alliance.core.settings.Plugin p = (Plugin) itr.next();
            File f = new File(p.getJar());
            if (f.exists()) {
                l.add(f.toURI().toURL());
            } else {
                int response = JOptionPane.showConfirmDialog(null, "There was an error loading the plugin " + f + " because the file does not exist \n" +
                        "would you like to attempt to locate the jar?", "Error Loading jar", JOptionPane.YES_NO_OPTION);
                if (response == JOptionPane.YES_OPTION) {
                    JFileChooser file = new JFileChooser();
                    FileNameExtensionFilter filter = new FileNameExtensionFilter("JAR files", "JAR", "jar", "Jar");
                    file.setFileFilter(filter);
                    if (file.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                        try {
                            itr.remove(); //Remove old plugin that we are trying to replace
                            Plugin newPlugin = new Plugin();
                            newPlugin.init(file.getSelectedFile());
                            l.add(file.getSelectedFile().toURI().toURL());
                            itr.add(newPlugin);
                        } catch (FileNotFoundException e) {
                            JOptionPane.showMessageDialog(null, 
                                    "The Jar file " + file.getSelectedFile() + 
                                        " is missing the launcher file, and was unable to be loaded correctly");
                        }
                    }
                } else {
                    itr.remove();
                }
            }
        }
        URL[] u = new URL[l.size()];
        u = l.toArray(u);
        classLoader = new URLClassLoader(u);
    }

    public void shutdown() throws Exception {
        for (PlugIn pi : plugIns) {
            pi.shutdown();
        }
    }
}

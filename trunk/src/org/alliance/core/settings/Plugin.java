package org.alliance.core.settings;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.alliance.core.plugins.PlugIn;

/**
 * Created by IntelliJ IDEA. User: maciek Date: 2008-jun-05 Time: 19:03:00 To
 * change this template use File | Settings | File Templates.
 */
public class Plugin {

    private String jar, pluginclass;
    private final String JARCLASSFILE = "Alliance.config";

    public String getJar() {
        return jar;
    }

    public void setJar(String jar) throws IOException {
        File jarfile = new File(jar);
        if (!init(jarfile)) {
            this.jar = jar;
            pluginclass = "";
        }
    }

    // TODO: This is not a "get" to ensure that it is not in the settings.xml file
    // If there is a better way please fix
    public String retrievePluginClass() {
        return pluginclass;
    }

    @Override
    public String toString() {
        return this.jar;
    }

    /**
     * Code below gets a file in the root of the jar with a name of JARCLASSFILE
     * this class needs to contain the alliance class
     * @param file 
     * @return 
     * @throws IOException
     * @throws FileNotFoundException 
     */
    public boolean init(File file) throws IOException, FileNotFoundException {
        if (file.exists()) {
            JarFile jarfile = new JarFile(file);
            if (jarfile.getEntry(JARCLASSFILE) != null) {
                InputStream is = jarfile.getInputStream(jarfile.getEntry(JARCLASSFILE));
                BufferedReader dis = new BufferedReader(new InputStreamReader(is));
                String mainclass = dis.readLine();
                jarfile.close();
                dis.close();
                is.close();
                this.jar = file.getCanonicalPath();
                this.pluginclass = mainclass;
                return true;

            } else {

                // Grab the first implementation of PlugIn found inside the jar.
                ClassLoader jarLoader = new URLClassLoader(new URL[]{ file.toURI().toURL() });
                for (Enumeration<JarEntry> e = jarfile.entries(); e.hasMoreElements();) {
                    JarEntry entry = e.nextElement();
                    if (!entry.isDirectory()
                        && entry.getName().endsWith(".class")) {
                        String className = entry.getName().substring(0, entry.getName().length() - ".class".length());
                        className = className.replace("/", ".");
                        try {
                            Class fileClass = Class.forName(className, false, jarLoader);
                            if (PlugIn.class.isAssignableFrom(fileClass)) {
                                this.jar = file.getCanonicalPath();
                                this.pluginclass = fileClass.getCanonicalName();
                                return true;
                            }
                        } catch (ClassNotFoundException ee) {
                            // This shouldn't happen.  If it does, let's abort.
                            System.err.println("Something went wrong trying to load " + className + " from " + file.getAbsolutePath() + ".  Will abort trying to load PlugIn classes from that jar.");
                            ee.printStackTrace();
                            break;
                        }
                    }
                }
                throw new FileNotFoundException(); // Why not just return false?
            }
        }
        return false;
    }
}

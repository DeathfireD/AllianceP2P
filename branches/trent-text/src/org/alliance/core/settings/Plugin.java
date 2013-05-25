package org.alliance.core.settings;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.jar.JarFile;

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
                throw new FileNotFoundException();
            }
        }
        return false;
    }
}

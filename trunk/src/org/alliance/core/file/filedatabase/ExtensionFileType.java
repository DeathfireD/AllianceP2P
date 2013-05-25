package org.alliance.core.file.filedatabase;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-mar-02
 * Time: 19:06:53
 * To change this template use File | Settings | File Templates.
 */
public class ExtensionFileType extends FileTypeIdentifier {

    private String extensions[];

    public ExtensionFileType(String[] extensions) {
        this.extensions = extensions;
    }

    public String[] getExtensions() {
        return extensions;
    }

    @Override
    public boolean matches(String s) {
        for (String e : extensions) {
            if (s.toLowerCase().endsWith(e)) {
                return true;
            }
        }
        return false;
    }
}

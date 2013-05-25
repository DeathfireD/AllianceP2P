package org.alliance.core.file.filedatabase;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-mar-02
 * Time: 19:05:45
 * To change this template use File | Settings | File Templates.
 */
public abstract class FileTypeIdentifier {

    public boolean matches(FileDescriptor fd) {
        return matches(fd.getSubPath());
    }

    public abstract boolean matches(String path);
}

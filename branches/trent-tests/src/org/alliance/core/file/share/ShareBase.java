package org.alliance.core.file.share;

import com.stendahls.util.TextUtils;

import java.io.File;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-jan-06
 * Time: 15:41:15
 * To change this template use File | Settings | File Templates.
 */
public class ShareBase {

    private String path;
    private String groupname;
    private boolean external;

    public ShareBase(String path, String groupname, int external) {
        try {
            path = new File(path).getCanonicalFile().getPath();
        } catch (IOException e) {
            if (T.t) {
                T.error("Could not resolve canonical share path: " + e);
            }
        }
        this.path = TextUtils.makeSurePathIsMultiplatform(path);
        this.groupname = groupname;
        if (external == 0) {
            this.external = false;
        } else {
            this.external = true;
        }
    }

    public String getPath() {
        return path;
    }

    public String getGroupName() {
        return groupname;
    }

    public boolean isExternal() {
        return external;
    }

    @Override
    public String toString() {
        return "ShareBase " + path;
    }

    /**
     * @return A friendly, short version of this share. Sent over the groupname to other users. Don't want to send full
     * pathname because is discloses security information that remote peers don't need to know. Currently this is the
     * name of the last directory in the sharebase's path.
     */
    public String getName() {
        int i = path.lastIndexOf('/');
        if (i == -1) {
            return path;
        }
        return path.substring(i + 1);
    }
}

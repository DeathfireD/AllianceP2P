package org.alliance.core.settings;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-jan-06
 * Time: 15:43:26
 * To change this template use File | Settings | File Templates.
 */
public class Share {

    private String path;
    private String sgroupname = "Public";
    private Integer external = 0;

    public Share() {
    }

    public Share(String path) {
        this.path = path;
    }

    public String getPath() {
        if (path == null) {
            return null;
        }
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        if (path.endsWith("\\")) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setSgroupname(String sgroupname) {
        this.sgroupname = sgroupname;
    }

    public String getSgroupname() {
        return sgroupname;
    }

    public Integer getExternal() {
        return external;
    }

    public void setExternal(Integer external) {
        this.external = external;
    }
}

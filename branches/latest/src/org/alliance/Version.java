package org.alliance;

import java.util.Properties;
import org.alliance.core.ResourceSingelton;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-jan-05
 * Time: 10:05:08
 * To change this template use File | Settings | File Templates.
 */
public class Version {

    public static final String VERSION = "1.1.0d";
    public static final int BUILD_NUMBER;
    public static final int PROTOCOL_VERSION = 3;

    static {
        int n = 0;
        try {
            Properties p = new Properties();
            p.load(ResourceSingelton.getRl().getResourceStream("build.properties"));
            n = Integer.parseInt(p.get("build.number").toString().replaceAll("\\D", ""));
        } catch (Exception e) {
            System.err.println("Could not load buildnumber: " + e);
        } finally {
            BUILD_NUMBER = n;
        }
    }
}

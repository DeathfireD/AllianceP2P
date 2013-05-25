package org.alliance.core;

import com.stendahls.resourceloader.GeneralResourceLoader;
import com.stendahls.resourceloader.ResourceLoader;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-jan-05
 * Time: 09:59:07
 * To change this template use File | Settings | File Templates.
 */
public class ResourceSingelton {

    private static GeneralResourceLoader rl;

    public static ResourceLoader getRl() {
        if (rl == null) {
            rl = new GeneralResourceLoader(T.class, "res");
        }
        return rl;
    }
}

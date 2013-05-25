package org.alliance;

import com.stendahls.resourceloader.ResourceLoader;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2005-dec-30
 * Time: 16:47:11
 */
public interface Subsystem {

    void init(ResourceLoader rl, Object... params) throws Exception;

    void shutdown();
}

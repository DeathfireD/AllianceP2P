package org.alliance.core;

import java.io.Serializable;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-mar-21
 * Time: 20:14:33
 * To change this template use File | Settings | File Templates.
 */
public interface NeedsUserInteraction extends Serializable {

    boolean canRunInParallelWithOtherInteractions();
}

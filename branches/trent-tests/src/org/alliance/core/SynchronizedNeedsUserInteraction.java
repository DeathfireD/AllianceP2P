package org.alliance.core;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-jul-11
 * Time: 14:25:51
 */
public abstract class SynchronizedNeedsUserInteraction implements NeedsUserInteraction {

    @Override
    public boolean canRunInParallelWithOtherInteractions() {
        return false;
    }
}


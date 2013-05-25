package org.alliance.core.plugins;

import org.alliance.core.CoreSubsystem;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2008-jun-05
 * Time: 19:16:39
 * To change this template use File | Settings | File Templates.
 */
public interface PlugIn {

    abstract void init(CoreSubsystem core) throws Exception;
    
    /**
     * Returns the callback object for the debug console.
     * 
     * Return null if no debug console extensions are implemented
     * @return 
     */
    
    abstract ConsolePlugInExtension getConsoleExtensions();

    /**
     * Note: if any UICallbacks are added, this should disable them.
     * This overlaps with the CoreSubsystem.resetUICallback, but there
     * are some system callbacks, which is why UICallbacks aren't 100%
     * managed by the PlugIn system.  (But maybe we can refactor this situation.)
     *
     * @throws Exception
     */
    abstract void shutdown() throws Exception;
}

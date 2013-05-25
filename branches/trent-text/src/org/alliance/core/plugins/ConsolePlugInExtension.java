package org.alliance.core.plugins;

import org.alliance.launchers.console.Console.Printer;

/**
 * This is a interface for a plugin class to implement that provides a callback
 * into the main debug console.
 */

public interface ConsolePlugInExtension {

	/**
     * Interface provides a plugin hook into the debug console.  This method is called in order of
     * loaded plugins if no valid commands are found internally.
     * 
     * The things are up to the plugin developer
     * -It is up to each plugin to use unique commands.
     * -It is up to each plugin to return true if a valid command is found
     * -Output should be displayed to the user using the Printer object passed in
     * 
     * @param line The command to be parsed
         * @param print The printing method that is being used
         * @return 
     */
	abstract boolean handleLine(String line, Printer print);
	
}

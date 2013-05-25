package org.alliance.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.alliance.launchers.OSInfo;

/**
 * JavaLauncher Provides an easy way to launch java appliations. The <code>ClasspathBuilder</code> class
 * is handle to use in conjunction with this class.
 * @author Jason Ederle
 */
public class LauncherJava {

    private static Logger logger = Logger.getLogger("JavaLauncher");
    private static boolean debug = false;

    private LauncherJava() {
    }

    /**
     * Launch a java program.
     * @param mainClass - class with main method
     * @param classpath - the java classpath
     * @param jvmargs - arguments for the jvm
     * @param properties - any system properties
     * @param xDockName - Mac os x, application name
     * @return
     * @throws Exception
     */
    public static Process exec(String mainClass, String classpath, String[] jvmargs, String[] properties, String xDockName) throws Exception {

        //get a jvm to execute with
        String jvm = findJVM();

        StringBuffer strClasspath = new StringBuffer("." + File.pathSeparator + classpath);

        //combine all the arguments into 1 array.
        String[] allArguments = new String[properties.length + jvmargs.length];
        System.arraycopy(jvmargs, 0, allArguments, 0, jvmargs.length);
        System.arraycopy(properties, 0, allArguments, jvmargs.length, properties.length);

        //build the command with jvm, arguments, and mainclass
        String[] command = new String[5 + allArguments.length];

        //put java command in place
        command[0] = jvm;

        //add all the arguments
        System.arraycopy(allArguments, 0, command, 1, allArguments.length);

        //set application name
        command[allArguments.length + 1] = "-Xdock:name=" + xDockName;
        //command[allArguments.length + 2] = "-Xdock:icon=\"\"";
        command[allArguments.length + 2] = "-classpath";
        command[allArguments.length + 3] = "\"" + strClasspath + "\"";
        command[allArguments.length + 4] = mainClass;

        String[] env = {};

        //combine to printable string for debugging
        StringBuffer wholeCommand = new StringBuffer();
        for (int i = 0; i < command.length; i++) {
            wholeCommand.append(command[i] + " ");
        }

        logger.log(Level.INFO, "Executing Command: " + wholeCommand);
        try {
            Process proc = Runtime.getRuntime().exec(command);

            if (debug) {
                monitorProcess(proc);
            }
            return proc;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to launch java program: " + e.getMessage());
            throw new Exception("Failed to launch java program: " + e.getMessage());
        }
    }

    /**
     * Execute a java jar file that contains a manifest.
     * @param pathToJar - absolute path to your jar
     * @param jvmargs - arguments for the java virtual machine
     * @param args
     * @param currentDir
     * @return Process
     * @throws Exception
     */
    public static Process execJar(String pathToJar, String[] jvmargs, String[] args, String currentDir) throws Exception {
        String jvm = findJVM();

        String[] command = new String[jvmargs.length + args.length + 3];
        command[0] = jvm;

        //copy arguments into command
        System.arraycopy(jvmargs, 0, command, 1, jvmargs.length);

        command[jvmargs.length + 1] = "-jar";
        command[jvmargs.length + 2] = new File(pathToJar).getAbsolutePath();

        //add jar arguments
        System.arraycopy(args, 0, command, jvmargs.length + 3, args.length);

        //combine to printable string for debugging
        StringBuffer wholeCommand = new StringBuffer();
        for (int i = 0; i < command.length; i++) {
            wholeCommand.append(command[i] + " ");
        }

        logger.log(Level.INFO, "Executing Command: " + wholeCommand);

        try {
            Process proc = null;
            if (currentDir.isEmpty()) {
                proc = Runtime.getRuntime().exec(command);
            } else {
                proc = Runtime.getRuntime().exec(command, null, new File(currentDir));
            }

            if (debug) {
                monitorProcess(proc);
            }
            return proc;
        } catch (Exception e) {
            throw new Exception("Failed to launch java program: " + e.getMessage());
        }
    }

    /**
     * Monitor an execute java program for errors and exit status.
     * @param proc
     * @throws java.io.IOException
     */
    private static void monitorProcess(Process proc) throws IOException {
        proc.getInputStream().close();
        proc.getErrorStream().close();

        //Read updaters output
        InputStream inputstream =
                proc.getErrorStream();
        InputStreamReader inputstreamreader =
                new InputStreamReader(inputstream);
        BufferedReader bufferedreader =
                new BufferedReader(inputstreamreader);

        // read the output
        String line;
        while ((line = bufferedreader.readLine()) != null) {
            logger.log(Level.INFO, line);
        }

        // check for failure
        try {
            if (proc.waitFor() != 0) {
                logger.log(Level.INFO, "exit value = " + proc.exitValue());
            }
        } catch (InterruptedException e) {
            logger.log(Level.SEVERE, e.getMessage());
        }
    }

    /**
     * Find a suitable JVM on the user's system.
     * @return - path to java binary
     */
    public static String findJVM() {

        String jvm = null;
        jvm = System.getProperty("java.home");

        //handle property not set
        if (jvm == null) {
            logger.log(Level.WARNING, "Java home property not set, just guessing with a general java call, and will probably fail.");

            //just take a guess an hope it's in the classpath
            jvm = "java";
        }

        //add binary folder
        if (OSInfo.isWindows()) {
            jvm = jvm + File.separator + "bin" + File.separator + "javaw";
        } else {
            jvm = jvm + File.separator + "bin" + File.separator + "java";
        }
        return jvm;
    }
}

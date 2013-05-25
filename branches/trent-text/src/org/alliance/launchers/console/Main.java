package org.alliance.launchers.console;

import org.alliance.core.CoreSubsystem;
import org.alliance.core.ResourceSingelton;
import org.alliance.core.node.FriendManager;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Simple command line interface to alliance
 * User: maciek
 * Date: 2005-dec-21
 * Time: 16:14:53
 * To change this template use File | Settings | File Templates.
 */
public class Main {

    public Main(String args[]) throws Exception {
        trace();

        Thread.currentThread().setName("Main program thread");

        CoreSubsystem core = new CoreSubsystem();
        core.init(ResourceSingelton.getRl(), org.alliance.launchers.ui.Main.getSettingsFile());

        FriendManager manager = core.getFriendManager();

        System.out.println("Welcome to alliance " + manager.getSettings().getMy().getNickname() + "!\n");

        Console c = new Console(core, null);

        while (true) {
            System.out.print("> ");
            String line;
            line = new BufferedReader(new InputStreamReader(System.in)).readLine();
            c.handleLine(line);
        }
    }

    public static void main(String[] args) throws Exception {
        new Main(args);
    }

    private void trace() {
//        if (T.t) {
//            try {
//                Class.forName("com.stendahls.trace.TraceWindow").loadOrCreate();
//            } catch(Exception e) {
//                e.printStackTrace();
//            }
//        }
    }
}

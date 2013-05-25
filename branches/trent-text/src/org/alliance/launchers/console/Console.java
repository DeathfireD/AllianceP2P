package org.alliance.launchers.console;

import com.stendahls.util.TextUtils;
import org.alliance.Subsystem;
import org.alliance.core.CoreSubsystem;
import org.alliance.core.SimpleTimer;
import org.alliance.core.ResourceSingelton;
import org.alliance.core.comm.Connection;
import org.alliance.core.comm.FriendConnection;
import org.alliance.core.comm.rpc.GetDirectoryListing;
import org.alliance.core.comm.rpc.GetShareBaseList;
import org.alliance.core.comm.rpc.Ping;
import org.alliance.core.crypto.cryptolayers.SSLCryptoLayer;
import org.alliance.core.file.blockstorage.BlockFile;
import org.alliance.core.file.filedatabase.FileType;
import org.alliance.core.file.hash.Hash;
import org.alliance.core.file.share.ShareBase;
import org.alliance.core.node.Friend;
import org.alliance.core.node.FriendManager;
import org.alliance.core.node.Invitation;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;
import org.alliance.core.plugins.ConsolePlugInExtension;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-jan-07
 * Time: 16:32:01
 * To change this template use File | Settings | File Templates.
 */
public class Console {

    public interface Printer {

        void println(String line);

        void print(String line);
    }
    private final static Printer PLAIN_PRINTER = new Printer() {

        @Override
        public void println(String line) {
            System.out.println(line);
        }

        @Override
        public void print(String line) {
            System.out.print(line);
        }
    };
    private CoreSubsystem core;
    private FriendManager manager;
    private List<ConsolePlugInExtension> extensions;
    private Subsystem ui;
    private Printer printer = PLAIN_PRINTER;
    private boolean netLog;

    public Console(CoreSubsystem core, List<ConsolePlugInExtension> extensions) {
        this.core = core;
        manager = core.getFriendManager();
        if (extensions == null) {
            extensions = new ArrayList<ConsolePlugInExtension>();//Create empty list
        } else {
            this.extensions = extensions;
        }
    }

    public void setPrinter(Printer printer) {
        this.printer = printer;
    }

    public void handleLine(String line) throws Exception {
        String command = line;

        ArrayList<String> params = new ArrayList<String>();
        StringTokenizer st = new StringTokenizer(line);
        if (st.hasMoreTokens()) {
            command = st.nextToken();
            while (st.hasMoreTokens()) {
                params.add(st.nextToken());
            }
        }

        if ("list".equals(command)) {
            list();
        } else if ("connect".equals(command)) {
            connect(params.get(0));
        } else if ("Ping".equals(command)) {
            ping();
        } else if ("startnetlog".equals(command)) {
            startnetlog();
        } else if ("contains".equals(command)) {
            contains(params);
        } else if ("ui".equals(command)) {
            ui();
        } else if ("killui".equals(command)) {
            killUI();
        } else if ("share".equals(command)) {
            share(params);
        } else if ("sharebases".equals(command)) {
            sharebases();
        } else if ("showbuilds".equals(command)) {
            showbuilds();
        } else if ("pingbomb".equals(command)) {
            if (CoreSubsystem.isRunningAsTestSuite()) {
                printer.println("Sending a bunch of pings at random intervals");
                Thread t = new Thread(new Runnable() {

                    @Override
                    public void run() {
                        for (int i = 0; i < 10000; i++) {
                            Collection<Friend> c = core.getFriendManager().friends();
                            Friend fa[] = new Friend[c.size()];
                            c.toArray(fa);
                            int n = (int) (c.size() * Math.random());
                            final Friend f = fa[n];
                            core.invokeLater(new Runnable() {

                                @Override
                                public void run() {
                                    try {
                                        f.getFriendConnection().send(new Ping());
                                    } catch (IOException e) {
                                        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                                    }
                                }
                            });
                            try {
                                Thread.sleep((long) (Math.random() * 50));
                            } catch (InterruptedException e) {
                            }
                        }
                    }
                });
                t.start();
            } else {
                printer.println("I think not");
            }
        } else if ("gc".equals(command)) {
            gc();
        } else if ("dir".equals(command)) {
            String sharebase = params.get(0);
            params.remove(0);
            String p = "";
            for (String s : params) {
                p += s + " ";
            }
            p = p.trim();
            dir(sharebase, p);
        } else if ("remotedir".equals(command)) {
            int sharebaseIndex = Integer.parseInt(params.get(0));
            String user = params.get(1);
            params.remove(0);
            params.remove(0);

            String p = "";
            for (String s : params) {
                p += s + " ";
            }
            p = p.trim();
            remotedir(sharebaseIndex, user, p);
        } else if ("remotesharebases".equals(command)) {
            remotesharebases(params.get(0));
        } else if ("threads".equals(command)) {
            threads();
        } else if ("filedatabase".equals(command)) {
            filedatabase();
        } else if ("sl".equals(command) || "searchLocal".equals(command)) {
            searchLocal(params);
        } else if ("ci".equals(command)) {
            clearInvitaitons(params.get(0));
        } else if ("Search".equals(command)) {
            search(params);
        } else if ("scan".equals(command)) {
            scan();
        } else if ("bye".equals(command)) {
            bye();
        } else if ("forcesslhandshake".equals(command)) {
            forcesslhandshake();
        } else if ("invitations".equals(command)) {
            invitations();
        } else if ("scaninvi".equals(command)) {
            invitationsScan();
        } else if ("removeinvi".equals(command)) {
            invitationsRemove();
        } else if ("error".equals(command)) {
            throw new Exception("test error");
        } else {
            boolean success = false;
            for (ConsolePlugInExtension ext : extensions) {
                if (ext != null && ext.handleLine(command, printer)) {
                    success = true;
                    break;
                }
            }
            if (!success) {
                printer.println("Unknown command " + command);
            }
        }
    }

    private void invitations() {
        int count = 0;
        printer.println("Stored Invitation codes:");
        Collection<Invitation> invitations = core.getInvitationManager().allInvitations();
        for (Invitation i : invitations.toArray(new Invitation[invitations.size()])) {
            count++;
            printer.print(i.getCompleteInvitaitonString());
            if (core.getInvitationManager().getInvitation(i.getInvitationPassKey()).isForwardedInvitation()) {
                printer.println(" (Forwarded)  -  Valid next: " + Long.toString((i.getValidTime() - (System.currentTimeMillis() - i.getCreatedAt())) / (1000 * 60)) + " minutes.");
            } else {
                printer.println(" (Manual)  -  Valid next: " + Long.toString((i.getValidTime() - (System.currentTimeMillis() - i.getCreatedAt())) / (1000 * 60 * 60)) + " hours.");
            }
        }
        printer.println("Found " + count + " Invitations.");
    }

    private void invitationsScan() {
        int count = 0;
        Collection<Invitation> invitations = core.getInvitationManager().allInvitations();
        for (Invitation i : invitations.toArray(new Invitation[invitations.size()])) {
            if (!core.getInvitationManager().isValid(i.getInvitationPassKey())) {
                core.getInvitationManager().consume(i.getInvitationPassKey());
                count++;
            }
        }
        printer.println("Removed " + count + " outdated Invitations.");
    }

    private void invitationsRemove() {
        int count = 0;
        Collection<Invitation> invitations = core.getInvitationManager().allInvitations();
        for (Invitation i : invitations.toArray(new Invitation[invitations.size()])) {
            core.getInvitationManager().consume(i.getInvitationPassKey());
            count++;
        }
        printer.println("Removed " + count + " Invitations.");
    }

    private void showbuilds() {
        for (Friend f : core.getFriendManager().friends()) {
            printer.println(f.getAllianceBuildNumber() + ": " + core.getFriendManager().nickname(f.getGuid()));
        }
    }

    private void filedatabase() throws IOException {
        core.getFileManager().getFileDatabase().printStats(printer);
    }

    private void clearInvitaitons(String s) throws Exception {
        int i = Integer.parseInt(s);
        if (i >= core.getSettings().getMy().getInvitations()) {
            printer.println("Can't raise number of invitaitons");
            return;
        }
        core.getSettings().getMy().createChecksumAndSetInvitations(i);
        core.saveSettings();
    }

    private void forcesslhandshake() throws IOException {
        for (Friend f : manager.friends()) {
            if (f.getFriendConnection() != null) {
                printer.println("Starting SSL handshake for " + f);
                ((SSLCryptoLayer) core.getCryptoManager().getCryptoLayer()).resendHandshake(f.getFriendConnection());
            }
        }
    }

    private void remotesharebases(String s) throws IOException {
        Friend f = manager.getFriend(s);
        f.getFriendConnection().send(new GetShareBaseList());
    }

    private void remotedir(int sharebaseIndex, String user, String path) throws IOException {
        Friend f = manager.getFriend(user);
        printer.println("Found friend: " + f + " for " + user);
        f.getFriendConnection().send(new GetDirectoryListing(sharebaseIndex, path));
    }

    private void sharebases() {
        printer.println("Sharebases: ");
        for (ShareBase b : core.getFileManager().getShareManager().shareBases()) {
            printer.println("  " + b);
        }
    }

    private void dir(String sharebase, String path) {
        printer.println("Directory listing for " + path + ": ");
        sharebase = TextUtils.makeSurePathIsMultiplatform(sharebase);
        ShareBase b = core.getFileManager().getShareManager().getBaseByPath(sharebase);
        if (b == null) {
            printer.println("Could not find share base for " + sharebase);
            return;
        }
        printer.println("Found share base: " + b);
        /*  for (String s : core.getFileManager().getFileDatabase().getDirectoryListing(b, path)) {
        printer.println(s);
        }*/
    }

    private void startnetlog() {
        netLog = true;
        printer.println("Net log is now on.");
    }
    private final static DateFormat FORMAT = new SimpleDateFormat("HH:mm yyyyMMdd");

    public void logNetworkEvent(String event) {
        if (netLog) {
            printer.println(FORMAT.format(new Date()) + " " + event);
        }
    }

    private void search(ArrayList<String> params) throws IOException {
        String q = params.get(0);
        manager.getNetMan().sendSearch(q, FileType.EVERYTHING);
    }

    private Thread[] getAllThreads() {
        ThreadGroup g = Thread.currentThread().getThreadGroup();
        while (g.getParent() != null) {
            g = g.getParent();
        }
        Thread threads[] = new Thread[1000];
        g.enumerate(threads);
        return threads;
    }

    private void threads() {
        printer.println("All threads: ");
        for (Thread t : getAllThreads()) {
            if (t == null) {
                break;
            }
            if (t.getThreadGroup() != null) {
                printer.println("  " + t.getName() + " (" + t.getThreadGroup().getName() + ")");
            } else {
                printer.println("  " + t.getName());
            }


            StackTraceElement[] elems = t.getStackTrace();
            if (elems != null) {
                for (StackTraceElement e : elems) {
                    printer.println("    " + e.toString());
                }
            }
        }
    }

    private void scan() {
        core.getShareManager().getShareScanner().startScan(true);
        printer.println("Scanning for new files in share directories (and cache (and downloads))");
    }

    private void searchLocal(ArrayList<String> params) throws IOException {
        FileType ft = FileType.EVERYTHING;
        if (Character.isDigit(params.get(0).charAt(0)) && params.get(0).length() == 1) {
            ft = FileType.getFileTypeById(Integer.parseInt(params.get(0)));
            params.remove(0);
        }

        String query = "";
        for (String s : params) {
            query += s + " ";
        }

        printer.println("Searching in " + ft.description() + "...");
        SimpleTimer st = new SimpleTimer();
        //int indices[] = core.getShareManager().getFileDatabase().getKeywordIndex().search(query, 100, ft);
        printer.println("...completed in " + st.getTime() + ".");
        // for (int i : indices) {
        //TODO
        //printer.println("  " + core.getShareManager().getFileDatabase().getFd(i));
        // }
    }

    private void gc() {
        System.gc();
        System.gc();
        long used = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        printer.println("Garbage collected. Using " + TextUtils.formatNumber("" + used) + " bytes of memory.");
    }

    private void contains(ArrayList<String> params) {
        printer.println("Result: " + core.getShareManager().getFileDatabase().contains(Hash.createFrom(params.get(0))));
    }

    private void share(ArrayList<String> params) throws IOException {
        printer.println("All complete files: ");
        //ArrayList<Hash> al = new ArrayList<Hash>(core.getFileManager().getFileDatabase().getAllHashes());
        // for (Hash h : al) {
        //    FileDescriptor fd = core.getFileManager().getFileDatabase().getFd(h);
        // //    printer.println("  " + fd);
        //   }
        printer.println("");

        printer.println("Incomplete files in cache: ");
        for (Hash h : core.getFileManager().getCache().rootHashes()) {
            BlockFile bf = core.getFileManager().getCache().getBlockFile(h);
            printer.println("  " + bf);
        }
        printer.println("");

        printer.println("Incomplete files in downloads: ");
        for (Hash h : core.getFileManager().getDownloadStorage().rootHashes()) {
            BlockFile bf = core.getFileManager().getDownloadStorage().getBlockFile(h);
            printer.println("  " + bf);
        }
        printer.println("");

        printer.println("Sharing "
                + TextUtils.formatByteSize(core.getShareManager().getFileDatabase().getShareSize()) + " in " + core.getShareManager().getFileDatabase().getNumberOfShares() + " files.");
        printer.println("");
    }

    private void ui() throws Exception {
        Runnable r = (Runnable) Class.forName("org.alliance.launchers.SplashWindow").newInstance();
        ui = (Subsystem) Class.forName("org.alliance.ui.UISubsystem").newInstance();
        ui.init(ResourceSingelton.getRl(), core);
        r.run(); //closes splashwindow
    }

    private void killUI() {
        if (ui != null) {
            ui.shutdown();
        }
        printer = PLAIN_PRINTER;
        ui = null;
        printer.println("UI Shutdown.");
    }

    private void bye() {
        printer.println("goodbye!");
        core.shutdown();
        System.exit(0);
    }

    private void ping() throws IOException {
        manager.ping();
    }

    private void connect(String nickname) throws IOException {
        printer.println("Connecting to " + nickname + "...");
        Friend f = manager.getFriend(nickname);
        manager.getNetMan().connect(f.getLastKnownHost(), f.getLastKnownPort(), new FriendConnection(manager.getNetMan(), Connection.Direction.OUT, f.getGuid()));
    }

    private void list() {
        printer.println("Friends: ");
        for (Friend f : manager.friends()) {
            printer.println("  " + f.getNickname() + " "
                    + (f.getFriendConnection() == null ? "disconnected" : f.getFriendConnection().getSocketAddress()));
        }
    }
}

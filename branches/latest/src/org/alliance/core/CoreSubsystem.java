package org.alliance.core;

import com.stendahls.XUI.XUIException;
import com.stendahls.nif.ui.OptionDialog;
import com.stendahls.nif.util.Log;
import com.stendahls.nif.util.SXML;
import com.stendahls.nif.util.xmlserializer.XMLSerializer;
import com.stendahls.resourceloader.ResourceLoader;
import com.stendahls.trace.Trace;
import com.stendahls.trace.TraceHandler;
import com.stendahls.ui.ErrorDialog;
import org.alliance.Subsystem;
import org.alliance.core.comm.AutomaticUpgrade;
import org.alliance.core.comm.NetworkManager;
import org.alliance.core.comm.rpc.AwayStatus;
import org.alliance.core.comm.rpc.GetUserInfo;
import org.alliance.core.comm.rpc.GetUserInfoV2;
import org.alliance.core.comm.upnp.UPnPManager;
import org.alliance.core.crypto.CryptoManager;
import org.alliance.core.file.FileManager;
import org.alliance.core.file.share.ShareManager;
import org.alliance.core.interactions.ForwardedInvitationInteraction;
import org.alliance.core.interactions.NeedsToRestartBecauseOfUpgradeInteraction;
import org.alliance.core.interactions.PleaseForwardInvitationInteraction;
import org.alliance.core.node.Friend;
import org.alliance.core.node.FriendManager;
import org.alliance.core.node.InvitaitonManager;
import org.alliance.core.plugins.DoubleUICallback;
import org.alliance.core.plugins.PluginManager;
import org.alliance.core.settings.Settings;
import org.alliance.launchers.OSInfo;
import org.alliance.launchers.StartupProgressListener;
import org.alliance.launchers.ui.Main;
import org.w3c.dom.Document;
import org.xml.sax.SAXParseException;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.UnresolvedAddressException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import javax.swing.JFrame;

/**
 * This is the core of the entire Alliance system. There is not too much code here, it's more of a hub for the entire
 * Core subsystem. Has a instance of FriendManager, FileManager, NetworkManager, InvitatationManager and the UICallback.
 * <p>
 * This class contains the oh-so-important invokeLater method that HAS to be used when code in the Core subsystem need
 * to be run from another thread than the Core thread. Very much like SwingUtilities.invokeLater().
 * <p>
 * There's also a queue of NeedsUserInteractions. This is an interface that is used when something happens in the Core
 * subsystem that the user need to interact to. Examples are: chat message received, invitation code received etc..
 * These things are queued by the Core subsystem and fetched by the UI subsystem
 *
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2005-dec-30
 * Time: 16:38:25
 */
public class CoreSubsystem implements Subsystem {

    public final static boolean ALLOW_TO_SEND_UPGRADE_TO_FRIENDS = false; //don't forget to turn off trace, run with registered synthetica
    private static final int STATE_FILE_VERSION = 5;
    public final static int KB = 1024;
    public final static int MB = 1024 * KB;
    public final static long GB = 1024 * MB;
    public final static int BLOCK_SIZE = MB;
    public final static String ERROR_URL = "http://maciek.tv/alliance/errorreporter/";
    private ResourceLoader rl;
    private FriendManager friendManager;
    private FileManager fileManager;
    private NetworkManager networkManager;
    private InvitaitonManager invitaitonManager;
    private UPnPManager upnpManager;
    private CryptoManager cryptoManager;
    private AwayManager awayManager;
    private PluginManager pluginManager;
    private PublicChatHistory publicChatHistory;
    private UICallback uiCallback = new NonWindowUICallback();
    private Settings settings;
    private String settingsFile;
    private Log errorLog, traceLog;
    ArrayList<NeedsUserInteraction> userInternactionQue = new ArrayList<NeedsUserInteraction>();

    public CoreSubsystem() {
    }

    @Override
    public void init(ResourceLoader rl, Object... params) throws Exception {
        StartupProgressListener progress = new StartupProgressListener() {

            @Override
            public void updateProgress(String message) {
            }
        };
        if (params != null && params.length >= 2 && params[1] != null) {
            progress = (StartupProgressListener) params[1];
        }

        progress.updateProgress("Loading core");
        setupLog();
        if (T.t && !isRunningAsTestSuite()) {
            final TraceHandler old = Trace.handler;
            Trace.handler = new TraceHandler() {

                @Override
                public void print(int level, Object message, Exception error) {
                    logTrace(level, message);
                    if (old != null) {
                        old.print(level, message, error);
                    }
                    propagateTraceMessage(level, String.valueOf(message), error);
                }
            };
        }

        if (T.t) {
            T.info("CoreSubsystem starting...");
        }

        this.rl = rl;
        this.settingsFile = String.valueOf(params[0]);

        Thread.currentThread().setName("Booting Core");

        loadSettings();

        fileManager = new FileManager(this, settings);
        friendManager = new FriendManager(this, settings);
        cryptoManager = new CryptoManager(this);
        networkManager = new NetworkManager(this, settings);
        invitaitonManager = new InvitaitonManager(this, settings);
        upnpManager = new UPnPManager(this);
        awayManager = new AwayManager(this);
        pluginManager = new PluginManager(this);

        publicChatHistory = new PublicChatHistory();

        loadState();
        cryptoManager.init();
        progress.updateProgress("Loading core (database manager)");
        fileManager.init();
        friendManager.init();
        progress.updateProgress("Loading core (network manager)");
        networkManager.init();
        awayManager.init();
        progress.updateProgress("Loading core (plugin manager)");
        pluginManager.init();

        if (!isRunningAsTestSuite()) {
            Thread t = new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        upnpManager.init();
                    } catch (IOException e) {
                        reportError(e, "upnp");
                    }
                }
            });
            t.start();
        }

        Thread.currentThread().setName(friendManager.getMe() + " main");

        Runtime.getRuntime().addShutdownHook(new Thread() {

            @Override
            public void run() {
                shutdown();
            }
        });

        System.setProperty("alliance.share.nfiles", "" + fileManager.getFileDatabase().getNumberOfShares());
        System.setProperty("alliance.share.size", "" + fileManager.getFileDatabase().getShareSize());
        System.setProperty("alliance.network.nfriends", "" + friendManager.getNFriends());
        System.setProperty("alliance.invites", "" + getSettings().getMy().getInvitations());

        if (T.t) {
            T.info("CoreSubsystem stated.");
        }
    }

    private void setupLog() throws Exception {
        try {
            String logpath = Main.localizeHomeDir();
            new File(logpath + "logs").mkdirs();
            errorLog = new Log(logpath + "logs/error.log");
            traceLog = new Log(logpath + "logs/trace.log");
        } catch (FileNotFoundException e) {
            if (OSInfo.isMac()) {
                OptionDialog.showErrorDialog(new JFrame(), "It seems that you are trying to run Alliance from a mounted image on Mac.[p]You need to drag'n'drop the Alliance icon you clicked on to your Applications folder and then start Alliance from there.[p]Alliance will now shut down.");
                System.exit(1);
            } else {
                throw new Exception("Permission problem. Can't write to files in Alliance application folder.");
            }
        }
    }

    public static boolean isRunningAsTestSuite() {
        return System.getProperty("testsuite") != null;
    }

    public void logTrace(int level, Object message) {
        try {
            traceLog.log("^" + level + " " + message);
            System.out.println(message);
        } catch (IOException e) {
            reportError(e, null);
        }
    }

    public void logError(Object error) {
        try {
            errorLog.log(error);
        } catch (IOException e) {
            reportError(e, null);
        }
    }

    private void loadSettings() throws Exception {
        loadSettings(true);
    }

    private void loadSettings(boolean tryWithBackupIfFail) throws Exception {
        if (T.t) {
            T.info("Loading settings...");
        }
        XMLSerializer s = new XMLSerializer();
        try {
            File file = new File(settingsFile);
            FileInputStream fis = new FileInputStream(file);
            Document document;
            try {
                document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(fis));
            } finally {
                fis.close();
            }
            settings = s.deserialize(
                    document,
                    Settings.class);
        } catch (FileNotFoundException e) {
            if (T.t) {
                T.info("No settings file - creating default settings.");
            }
            File file = new File(settingsFile);
            if (file.getParentFile() != null) {
                file.getParentFile().mkdirs();
            }
            settings = new Settings();
            saveSettings();
        } catch (SAXParseException e) {
            if (tryWithBackupIfFail) {
                if (T.t) {
                    T.info("Settings file is corrupt - trying to use backup version of settings..");
                }
                File file = new File(settingsFile);
                File bak = new File(settingsFile + ".bak");
                if (bak.exists()) {
                    file.renameTo(new File(settingsFile + ".corrupt@" + System.currentTimeMillis()));
                    bak.renameTo(file);
                    //calls itself recursively but no infinite loop should occur since the .bak file has been moved
                    loadSettings(false);
                } else {
                    throw new Exception("Settings file is corrupt. Tried to used backed up version but failed. Sorry.", e);
                }
            } else {
                throw new Exception("Settings file is corrupt. Tried to used backed up version but failed. Sorry.", e);
            }
        }
    }

    public synchronized void saveState() throws IOException {
        if (T.t) {
            T.info("Saving core state");
        }
        File file = new File(settings.getInternal().getCorestatefile());
        if (file.getParentFile() != null) {
            file.getParentFile().mkdirs();
        }
        ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(file));
        out.writeInt(STATE_FILE_VERSION);
        invitaitonManager.save(out);
        networkManager.save(out);
        out.writeObject(userInternactionQue);
        out.writeObject(publicChatHistory);
        out.flush();
        out.close();
        if (T.t) {
            T.info("Core state saved.");
        }
    }

    @SuppressWarnings("unchecked")
    public void loadState() throws Exception {
        try {
            if (T.t) {
                T.info("Loading core state");
            }
            ObjectInputStream in = new ObjectInputStream(new FileInputStream(settings.getInternal().getCorestatefile()));
            int ver = in.readInt();
            if (ver != STATE_FILE_VERSION) {
                if (T.t) {
                    T.error("Incorrect state file version. Ignoring old state.");
                }
                in.close();
                return;
            }
            invitaitonManager.load(in);
            networkManager.load(in);
            userInternactionQue = (ArrayList<NeedsUserInteraction>) in.readObject();
            publicChatHistory = (PublicChatHistory) in.readObject();
            for (Iterator i = userInternactionQue.iterator(); i.hasNext();) {
                if (i.next() instanceof NeedsToRestartBecauseOfUpgradeInteraction) {
                    i.remove(); //we don't need to restart if it's a interaction from the last time we ran alliance
                }
            }
            in.close();
        } catch (FileNotFoundException e) {
            if (T.t) {
                T.info("No core state found.");
            }
        } catch (IOException e) {
            if (T.t) {
                T.error("Could not load state: " + e);
            }
        }
    }

    public synchronized void saveSettings() throws Exception {
        if (T.t) {
            T.info("Saving settings");
        }
        if (new File(settingsFile).exists()) {
            AutomaticUpgrade.copyFile(new File(settingsFile), new File(settingsFile + ".bak"));
        }

        XMLSerializer s = new XMLSerializer();
        Document doc = s.serialize(settings);

        OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(new File(settingsFile)), "UTF-8");
        out.write(SXML.toString(doc));
        out.flush();
        out.close();
    }

    public ResourceLoader getRl() {
        return rl;
    }

    public FriendManager getFriendManager() {
        return friendManager;
    }
    private boolean shutdownInProgress;

    @Override
    public synchronized void shutdown() {
        if (shutdownInProgress) {
            return;
        }
        shutdownInProgress = true;
        if (T.t) {
            T.info("Shutting down core..");
        }
        try {
            fileManager.shutdown();
        } catch (Exception e) {
            if (T.t) {
                T.error("Problem when shutting down filemanager: " + e);
            }
        }
        try {
            updateLastSeenOnlineForFriends();
        } catch (Exception e) {
            if (T.t) {
                T.error("Problem when saving friends: " + e);
            }
        }
        try {
            friendManager.shutdown();
        } catch (Exception e) {
            if (T.t) {
                T.error("Problem when shutting down friendmanager: " + e);
            }
        }
        try {
            networkManager.shutdown();
        } catch (Exception e) {
            if (T.t) {
                T.error("Problem when shutting down networkmanager: " + e);
            }
        }
        try {
            upnpManager.shutdown();
        } catch (Exception e) {
            if (T.t) {
                T.error("Problem when shutting down upnpmanager: " + e);
            }
        }
        try {
            awayManager.shutdown();
        } catch (Exception e) {
            if (T.t) {
                T.error("Problem when shutting down awaymanager: " + e);
            }
        }
        try {
            pluginManager.shutdown();
        } catch (Exception e) {
            if (T.t) {
                T.error("Problem when shutting down pluginmanager: " + e);
            }
        }
        try {
            saveSettings();
        } catch (Exception e) {
            if (T.t) {
                T.error("Problem when saving settings: " + e);
            }
        }
        try {
            saveState();
        } catch (Exception e) {
            if (T.t) {
                T.error("Problem when saving state: " + e);
            }
        }
        try {
            Thread.sleep(1500); //wait for GracefulClose RPCs to be sent
        } catch (InterruptedException e) {
        }
    }

    public void updateLastSeenOnlineForFriends() {
        for (Friend f : friendManager.friends()) {
            updateLastSeenOnlineFor(f);
        }
    }

    public void updateLastSeenOnlineFor(Friend f) {
        if (f.isConnected()) {
            if (settings.getFriend(f.getGuid()) != null) {
                settings.getFriend(f.getGuid()).setLastseenonlineat(System.currentTimeMillis());
            }
        }
    }

    public Settings getSettings() {
        return settings;
    }

    public ShareManager getShareManager() {
        return fileManager.getShareManager();
    }

    public NetworkManager getNetworkManager() {
        return networkManager;
    }

    public void propagateTraceMessage(int level, String message, Exception e) {
        uiCallback.trace(level, message, e);
    }

    public UICallback getUICallback() {
        return uiCallback;
    }

    public void setUICallback(UICallback uiCallback) {
        UICallback old = this.uiCallback;
        if (uiCallback == null) {
            this.uiCallback = new NonWindowUICallback();
        } else {
            this.uiCallback = uiCallback;
        }
        if (old != null) {
            old.callbackRemoved();
        }
    }

    /** Adds this callback using a DoubleUICallback class */
    public void addUICallback(UICallback u) {
        uiCallback = new DoubleUICallback(uiCallback, u);
    }

    public FileManager getFileManager() {
        return fileManager;
    }

    /**
     * The core package is not thread safe. It all runs in one thread. If another threads want to invoke something
     * in core it should use this method. Same design pattern as SwingUtilities.invokeLater
     * @param runnable
     */
    public void invokeLater(Runnable runnable) {
        networkManager.invokeLater(runnable);
    }

    public void reportError(Throwable e, Object source) {
        if (shutdownInProgress && e instanceof ClosedChannelException) {
            return;
        }
        logError("Error for " + source + ": ");
        logError(e);

        if (e instanceof IOException) {
            if (T.t) {
                T.warn(e);
            }
            return;
        }
        if (e instanceof UnresolvedAddressException) {
            if (T.t) {
                T.warn(e);
            }
            return;
        }

        if (shutdownInProgress) {
            System.err.println("Error while shutting down for " + source + ": " + e);
            e.printStackTrace();
            return;
        }

        uiCallback.handleError(e, source);
    }

    public void restartProgram(boolean openWithUI) throws IOException {
        restartProgram(openWithUI, 2);
    }

    /**
     * @param openWithUI true if the UI should open once the program is restarted
     * @param restartDelay Wait this many minutes before actually starting the program. This is for "Shutdown for 30 minuters" etc..
     * @throws java.io.IOException if the program cant be restarted
     */
    public void restartProgram(boolean openWithUI, int restartDelay) throws IOException {
        shutdown();

        Main.stopStartSignalThread(); //such a fucking hack. When we run using the normal UI we need to signal the launcher that he needs to stop this startsignalthread
        String s = "." + System.getProperty("file.separator") + "alliance" + //must have exe/script/batch in current directory to start program
                (openWithUI ? "" : " /min")
                + (restartDelay == 0 ? "" : " /w" + restartDelay);

        Runtime.getRuntime().exec(s);
        System.exit(0);
    }

    public void uiToFront() {
        uiCallback.toFront();
    }

    public InvitaitonManager getInvitaitonManager() {
        return invitaitonManager;
    }

    public void queNeedsUserInteraction(NeedsUserInteraction ui) {
        if (ui instanceof PleaseForwardInvitationInteraction
                && getSettings().getInternal().getAlwaysallowfriendstoconnect() > 0) {
            try {
                //automatically forward this user invitation - the settings are set to do this.
                if (T.t) {
                    T.info("Automatically forwarding invitation: " + ui);
                }
                getFriendManager().forwardInvitation((PleaseForwardInvitationInteraction) ui);
                return;
            } catch (IOException e) {
                reportError(e, ui);
            }
        } else if (ui instanceof ForwardedInvitationInteraction) {
            if (getSettings().getInternal().getAlwaysallowfriendsoffriendstoconnecttome() > 0) {
                try {
                    ForwardedInvitationInteraction fii = (ForwardedInvitationInteraction) ui;
                    getInvitaitonManager().attemptToBecomeFriendWith(fii.getInvitationCode(), fii.getMiddleman(this), fii.getFromGuid());
                    return;
                } catch (Exception e) {
                    reportError(e, ui);
                }
            } else if (getSettings().getInternal().getAlwaysallowfriendsoftrustedfriendstoconnecttome() > 0) {
                try {
                    ForwardedInvitationInteraction fii = (ForwardedInvitationInteraction) ui;
                    Collection<Friend> friends = friendManager.friends();
                    for (Friend f : friends.toArray(new Friend[friends.size()])) {
                        if (f.getFriendsFriend(fii.getFromGuid()) != null && f.getTrusted() == 1) {
                            getInvitaitonManager().attemptToBecomeFriendWith(fii.getInvitationCode(), fii.getMiddleman(this), fii.getFromGuid());
                            return;
                        }
                    }
                    if (getSettings().getInternal().getAlwaysdenyuntrustedinvitations() > 0) {
                        return;
                    }
                } catch (Exception e) {
                    reportError(e, ui);
                }
            }
        }
        userInternactionQue.add(ui);
        uiCallback.newUserInteractionQueued(ui);
    }

    public NeedsUserInteraction fetchUserInteraction() {
        if (userInternactionQue.size() > 0) {
            NeedsUserInteraction ui = userInternactionQue.get(0);
            userInternactionQue.remove(ui);
            try {
                saveState();
            } catch (IOException e) {
                reportError(e, this);
            }
            return ui;
        }
        return null;
    }

    public NeedsUserInteraction peekUserInteraction() {
        if (userInternactionQue.size() > 0) {
            return userInternactionQue.get(0);
        }
        return null;
    }

    public void removeUserInteraction(NeedsUserInteraction nui) {
        userInternactionQue.remove(nui);
    }

    public boolean doesInterationQueContain(Class<? extends NeedsUserInteraction> c) {
        for (NeedsUserInteraction u : userInternactionQue) {
            if (u.getClass().equals(c)) {
                return true;
            }
        }
        return false;
    }

    public void refreshFriendInfo() throws IOException {
        networkManager.sendToAllFriends(new GetUserInfo());
        networkManager.sendToAllFriends(new GetUserInfoV2());
    }

    public void softRestart() throws IOException {
        if (uiCallback.isUIVisible()) {
            if (!doesInterationQueContain(NeedsToRestartBecauseOfUpgradeInteraction.class)) {
                queNeedsUserInteraction(new NeedsToRestartBecauseOfUpgradeInteraction());
            }
        } else {
            restartProgram(false);
        }
    }
    private int GULCounter;
    private long GULTick = System.currentTimeMillis();

    /**
     * Temporary stuff needed to figure out a serious bug.
     */
    public void increaseGULCounter() {
        GULCounter++;
        if (System.currentTimeMillis() - GULTick > 1000 * 60) {
            if (T.t) {
                T.trace("GUL counter: " + GULCounter);
            }
            if (GULCounter > 300) {
                try {
                    new ErrorDialog(new Exception("UserList flood detected: " + GULCounter + "! <b>This is a fatal error. You need to restart Alliance.</b> Please send this error report by pressing 'send error'."), true);
                } catch (XUIException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
            }
            GULCounter = 0;
            GULTick = System.currentTimeMillis();
        }
    }

    /**
     * Logs network messages for debug purposes. An event can be for example "user x went online"
     */
    public void logNetworkEvent(String event) {
        uiCallback.logNetworkEvent(event);
    }

    public UPnPManager getUpnpManager() {
        return upnpManager;
    }

    @SuppressWarnings("unchecked")
    public List<NeedsUserInteraction> getAllUserInteractionsInQue() {
        return (List<NeedsUserInteraction>) userInternactionQue.clone();
    }

    public CryptoManager getCryptoManager() {
        return cryptoManager;
    }

    public ByteBuffer allocateBuffer(int size) {
        if (settings.getInternal().getUsedirectbuffers() > 0) {
            try {
                return ByteBuffer.allocateDirect(size);
            } catch (OutOfMemoryError e) {
                if (T.t) {
                    T.warn("Falling back to non-direct buffers - out of direct buffer space!");
                }
                return ByteBuffer.allocate(size);
            }
        } else {
            return ByteBuffer.allocate(size);
        }
    }

    public PublicChatHistory getPublicChatHistory() {
        return publicChatHistory;
    }

    /**
     * Every time the user using this alliance installation sucessfully invites a new friend to the network he gets
     * an invitation point
     */
    public void incInvitationPoints() {
        getSettings().getMy().createChecksumAndSetInvitations(getSettings().getMy().getInvitations() + 1);
    }

    public void informFriendsOfAwayStatus(boolean away) throws IOException {
        getNetworkManager().sendToAllFriends(new AwayStatus(away));
    }

    public AwayManager getAwayManager() {
        return awayManager;
    }

    public PluginManager getPluginManager() {
        return pluginManager;
    }
}

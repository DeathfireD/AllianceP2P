package org.alliance.ui;

import com.stendahls.XUI.MenuItemDescriptionListener;
import com.stendahls.XUI.XUIFrame;
import com.stendahls.nif.ui.mdi.MDIManager;
import com.stendahls.nif.ui.mdi.MDIManagerEventListener;
import com.stendahls.nif.ui.mdi.MDIWindow;
import com.stendahls.nif.ui.mdi.infonodemdi.InfoNodeMDIManager;
import com.stendahls.nif.ui.toolbaractions.ToolbarActionManager;
import com.stendahls.nif.util.xmlserializer.SXML;
import com.stendahls.ui.JHtmlLabel;
import com.stendahls.util.TextUtils;
import static org.alliance.core.CoreSubsystem.MB;
import org.alliance.core.NeedsUserInteraction;
import org.alliance.core.PublicChatHistory;
import org.alliance.core.CoreSubsystem;
import org.alliance.core.comm.BandwidthAnalyzer;
import org.alliance.core.node.Friend;
import org.alliance.core.node.Node;
import org.alliance.core.interactions.ForwardedInvitationInteraction;
import org.alliance.core.interactions.FriendAlreadyInListUserInteraction;
import org.alliance.core.interactions.NeedsToRestartBecauseOfUpgradeInteraction;
import org.alliance.core.interactions.NewFriendConnectedUserInteraction;
import org.alliance.core.interactions.PleaseForwardInvitationInteraction;
import org.alliance.core.interactions.PostMessageInteraction;
import org.alliance.core.interactions.PostMessageToAllInteraction;
import org.alliance.core.interactions.NewVersionAvailableInteraction;
import org.alliance.core.Language;
import org.alliance.launchers.OSInfo;
import org.alliance.launchers.StartupProgressListener;
import org.alliance.ui.addfriendwizard.AddFriendWizard;
import org.alliance.ui.addfriendwizard.ForwardInvitationNodesList;
import org.alliance.ui.dialogs.OptionDialog;
import org.alliance.ui.themes.util.SubstanceThemeHelper;
import org.alliance.ui.windows.AddPluginWindow;
import org.alliance.ui.windows.ConnectedToNewFriendDialog;
import org.alliance.ui.windows.mdi.ConnectionsMDIWindow;
import org.alliance.ui.windows.mdi.ConsoleMDIWindow;
import org.alliance.ui.windows.mdi.DownloadsMDIWindow;
import org.alliance.ui.windows.mdi.DuplicatesMDIWindow;
import org.alliance.ui.windows.ForwardInvitationDialog;
import org.alliance.ui.windows.mdi.friends.FriendListMDIWindow;
import org.alliance.ui.windows.mdi.FriendsTreeMDIWindow;
import org.alliance.ui.windows.options.OptionsWindow;
import org.alliance.ui.windows.mdi.chat.HistoryChatMessageMDIWindow;
import org.alliance.ui.windows.mdi.chat.PrivateChatMessageMDIWindow;
import org.alliance.ui.windows.mdi.chat.PublicChatMessageMDIWindow;
import org.alliance.ui.windows.mdi.trace.TraceMDIWindow;
import org.alliance.ui.windows.mdi.UploadsMDIWindow;
import org.alliance.ui.windows.mdi.WelcomeMDIWindow;
import org.alliance.ui.windows.mdi.search.SearchMDIWindow;
import org.alliance.ui.windows.mdi.viewshare.ViewShareMDIWindow;
import org.alliance.ui.windows.shares.SharesWindow;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.TreeMap;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import org.alliance.ui.windows.mdi.InvitationsMDIWindow;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2005-dec-30
 * Time: 16:25:12
 */
public class MainWindow extends XUIFrame implements MenuItemDescriptionListener, MDIManagerEventListener, Runnable {

    private UISubsystem ui;
    private JLabel statusMessage, shareMessage;
    private JProgressBar bandwidthIn, bandwidthOut;
    private ToolbarActionManager toolbarActionManager;
    protected MDIManager mdiManager;
    private AddFriendWizard lastAddFriendWizard;
    private int userInteractionsInProgress = 0;
    private PublicChatMessageMDIWindow publicChat;
    private JButton rescan;
    private JPanel statusPanel;
    private Icon[] refreshIconStates = new Icon[2];

    public MainWindow() throws Exception {
    }

    public void init(final UISubsystem ui, StartupProgressListener pml) throws Exception {
        this.ui = ui;

        int[] imageSizes = {16, 24, 32, 48, 64, 128};
        ArrayList<Image> images = new ArrayList<Image>();
        for (int size : imageSizes) {
            images.add((new ImageIcon(getClass().getResource("/res/gfx/icons/alliance" + size + ".png")).getImage()));
        }
        this.setIconImages(images);

        pml.updateProgress(Language.getLocalizedString(getClass(), "mainloading"));
        init(ui.getRl(), "xui/mainwindow.xui.xml");
        Language.translateXUIElements(getClass(), xui.getXUIComponents());
        SubstanceThemeHelper.setButtonsToGeneralArea(xui.getXUIComponents());
        SubstanceThemeHelper.setComponentToToolbarArea((JComponent) xui.getComponent("fakeShadow"));

        rescan = (JButton) xui.getComponent("rescan");
        rescan.setFocusPainted(false);
        refreshIconStates[0] = rescan.getIcon();
        refreshIconStates[1] = new ImageIcon(getClass().getResource("/res/gfx/icons/icon.png"));
        SubstanceThemeHelper.flatButton(rescan);

        bandwidthIn = (JProgressBar) xui.getComponent("bandwidthin");
        bandwidthOut = (JProgressBar) xui.getComponent("bandwidthout");

        xui.setEventHandler(this);
        xui.setMenuItemDescriptionListener(this);
        statusMessage = (JLabel) xui.getComponent("statusbar");
        statusPanel = (JPanel) xui.getComponent("statuspanel");
        statusMessage.setText(" ");
        shareMessage = (JLabel) xui.getComponent("sharing");

        setupToolbar();

        setupWindowEvents();

        setupSpeedDiagram();

        setupMDIManager();

        pml.updateProgress(Language.getLocalizedString(getClass(), "friendloading"));
        mdiManager.addWindow(new FriendListMDIWindow(mdiManager, ui));
        pml.updateProgress(Language.getLocalizedString(getClass(), "chatloading"));
        mdiManager.addWindow(publicChat = new PublicChatMessageMDIWindow(ui));
        pml.updateProgress(Language.getLocalizedString(getClass(), "searchloading"));
        mdiManager.addWindow(new SearchMDIWindow(ui));
        pml.updateProgress(Language.getLocalizedString(getClass(), "downloadsloading"));
        mdiManager.addWindow(new DownloadsMDIWindow(ui));
        pml.updateProgress(Language.getLocalizedString(getClass(), "uploadsloading"));
        EVENT_uploads(null);

        pml.updateProgress(Language.getLocalizedString(getClass(), "mainloading"));
        for (PublicChatHistory.Entry e : ui.getCore().getPublicChatHistory().allMessages()) {
            publicChat.addMessage(ui.getCore().getFriendManager().nickname(e.fromGuid), e.message, e.tick, true, false);
        }

        mdiManager.selectWindow(publicChat);

        getRootPane().setWindowDecorationStyle(JRootPane.NONE);
        getRootPane().setWindowDecorationStyle(JRootPane.FRAME);

        showWindow();

        Thread t = new Thread(this, "Regular Interval UI Update Thread");
        t.setDaemon(true);
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();

        if (ui.getCore().getSettings().getInternal().getFirststart() == 1) {
            SwingUtilities.invokeLater(new Runnable() {

                @Override
                public void run() {
                    OptionDialog.showInformationDialog(MainWindow.this, Language.getLocalizedString(getClass(), "welcomeinfo"));
                    try {
                        ui.getCore().getSettings().getInternal().setFirststart(0);
                        EVENT_options(null);
                    } catch (Exception e) {
                        ui.handleErrorInEventLoop(e);
                    }
                }
            });
        }


        if (T.t) {
            T.info("done");
        }
    }

    private void setupSpeedDiagram() {
        JSpeedDiagram diagram = new JSpeedDiagram(ui);
        Thread diagramThread = new Thread(diagram);
        diagramThread.setName("SpeedDiagram");
        diagramThread.start();
        ((JPanel) xui.getComponent("diagrampanel")).add(diagram);
    }

    private void setupMDIManager() {
        mdiManager = new InfoNodeMDIManager(ui);
        mdiManager.setEventListener(this);
        ((InfoNodeMDIManager) mdiManager).setMaximumNumberOfWindowsByClass(10);
        ((JPanel) xui.getComponent("applicationArea")).add(mdiManager);
    }

    private void setupToolbar() throws Exception {
        toolbarActionManager = new ToolbarActionManager(SXML.loadXML(xui.getResourceLoader().getResourceStream("toolbaractions.xml")));
        toolbarActionManager.bindToFrame(this);
    }
    private long lastUpdateTime = System.currentTimeMillis();

    public void setStatusMessage(final String s) {
        if (System.currentTimeMillis() - lastUpdateTime < 50) {
            return;
        }
        lastUpdateTime = System.currentTimeMillis();
        setStatusMessage(s, false);
    }
    private Thread messageFadeThread;

    public synchronized void setStatusMessage(final String s, final boolean longDelay) {
        if (statusMessage.getText().trim().equals(s.trim())) {
            return;
        }
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                if (s == null || s.isEmpty()) {
                    statusMessage.setText(" ");
                } else {
                    int width = statusPanel.getWidth();
                    statusMessage.setText(s);
                    if (statusMessage.getMaximumSize().getWidth() != width) {
                        Dimension d = new Dimension(width - 10, statusMessage.getHeight());
                        statusMessage.setPreferredSize(d);
                        statusMessage.setMaximumSize(d);
                    }
                }
            }
        });

        if (messageFadeThread != null) {
            messageFadeThread.interrupt();
        }
        messageFadeThread = new Thread(new Runnable() {

            @Override
            public void run() {
                Thread myThread = messageFadeThread;
                try {
                    if (myThread != messageFadeThread) {
                        return;
                    }
                    changeStatusMessageColor(0);
                    if (myThread != messageFadeThread) {
                        return;
                    }
                    Thread.sleep(5 * 1000);
                    if (longDelay) {
                        Thread.sleep(25 * 1000);
                    }
                    if (myThread != messageFadeThread) {
                        return;
                    }
                    int c = 0;
                    for (int i = 0; i < 23 && messageFadeThread == myThread; i++) {
                        changeStatusMessageColor(c | c << 8 | c << 16);
                        c += 10;
                        Thread.sleep(80);
                    }
                    if (myThread != messageFadeThread) {
                        return;
                    }
                    SwingUtilities.invokeLater(new Runnable() {

                        @Override
                        public void run() {
                            statusMessage.setText(" ");
                        }
                    });
                } catch (InterruptedException e) {
                } catch (Exception e) {
                    if (T.t) {
                        T.error("Problem in progressMessage fade loop: " + e);
                    }
                }
            }
        });
        messageFadeThread.setName("MessageFadeThread");
        messageFadeThread.start();
    }

    private synchronized void changeStatusMessageColor(final int color) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                statusMessage.setForeground(new Color(color));
                statusMessage.repaint();
            }
        });
    }

    public void EVENT_hide(ActionEvent e) throws Exception {
        if (OSInfo.supportsTrayIcon() || OSInfo.isMac()) {
            setVisible(false);
        } else {
            OptionDialog.showInformationDialog(this, Language.getLocalizedString(getClass(), "hideinfo"));
        }
    }

    private void setupWindowEvents() {
        addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {
                if (OSInfo.isMac()) {
                    if (T.t) {
                        T.info("Running on mac - never shutdown - just hide the window - that's how mac applications work");
                    }
                    setVisible(false);
                } else if (CoreSubsystem.isRunningAsTestSuite()) {
                    if (T.t) {
                        T.info("Running as testsuite - only hide window");
                    }
                    setVisible(false);
                } else if (OSInfo.supportsTrayIcon()) {
                    if (T.t) {
                        T.info("We have a tray icon so we just hide the window here");
                    }
                    setVisible(false);
                } else {
                    if (T.t) {
                        T.info("No tray icon and not Mac - just shut down alliance");
                    }
                    shutdown();
                }
            }
        });
    }

    @Override
    public void showMenuItemDescription(String description) {
        setStatusMessage(description);
    }
    boolean shuttingDown = false;

    public boolean shutdown() {
        if (shuttingDown) {
            return true;
        }
        shuttingDown = true;
        saveWindowState(getTitle(), getLocation(), getSize(), getExtendedState());
        setVisible(false);
        dispose();
        ui.shutdown();
        return true;
    }

    private void writeStateBlock(ObjectOutputStream outObj, String title, Point point, Dimension dimension, int extendedState) throws Exception {
        outObj.writeUTF(title);
        outObj.writeObject(point);
        outObj.writeObject(dimension);
        outObj.writeInt(extendedState);
    }

    public void saveWindowState(String title, Point point, Dimension dimension, int extendedState) {
        ObjectOutputStream outObj = null;
        ObjectInputStream inObj = null;
        try {
            if (T.t) {
                T.info("Serializing window state");
            }
            File states = new File(ui.getCore().getSettings().getInternal().getWindowstatefile());
            File oldStates = new File(ui.getCore().getSettings().getInternal().getWindowstatefile() + ".bak");
            boolean stateFileExist = states.exists();
            if (stateFileExist) {
                states.renameTo(oldStates);
            }

            FileOutputStream out = new FileOutputStream(states);
            outObj = new ObjectOutputStream(out);

            if (!stateFileExist) {
                //Create state file with source object's data
                writeStateBlock(outObj, title, point, dimension, extendedState);
            } else {
                FileInputStream in = new FileInputStream(oldStates);
                inObj = new ObjectInputStream(in);
                boolean blockWriten = false;
                try {
                    while (true) {
                        String inTitle = inObj.readUTF();
                        Point inPoint = (Point) inObj.readObject();
                        Dimension inDimension = (Dimension) inObj.readObject();
                        int inExtendedState = inObj.readInt();
                        if (!inTitle.equals(title)) {
                            //Rewrite non-source object's data
                            writeStateBlock(outObj, inTitle, inPoint, inDimension, inExtendedState);
                        } else {
                            //Replace source object's data
                            writeStateBlock(outObj, title, point, dimension, extendedState);
                            blockWriten = true;
                        }
                    }
                } catch (Exception e) {
                    //EOF reached
                }
                if (!blockWriten) {
                    //Source object's data not replaced, probably new data
                    writeStateBlock(outObj, title, point, dimension, extendedState);
                }
                inObj.close();
            }
            outObj.flush();
            outObj.close();
            oldStates.delete();
        } catch (Exception e) {
            if (T.t) {
                T.error("Could not save window state " + e);
            }
        } finally {
            try {
                inObj.close();
                outObj.close();
            } catch (Exception e) {
                //Ignore
            }
        }
    }

    public boolean loadWindowState(String title, Component component) {
        if (T.t) {
            T.info("Deserializing window state");
        }
        ObjectInputStream inObj = null;
        try {
            FileInputStream in = new FileInputStream(ui.getCore().getSettings().getInternal().getWindowstatefile());
            inObj = new ObjectInputStream(in);
            try {
                while (true) {
                    String inTitle = inObj.readUTF();
                    Point inPoint = (Point) inObj.readObject();
                    Dimension inDimension = (Dimension) inObj.readObject();
                    int inExtendedState = inObj.readInt();
                    if (inTitle.equals(title)) {
                        component.setLocation(inPoint);
                        component.setSize(inDimension);
                        component.setPreferredSize(inDimension);
                        if (component instanceof JFrame) {
                            ((JFrame) component).setExtendedState(inExtendedState);
                        }
                        return true;
                    }
                }
            } catch (Exception e) {
                //EOF reached
            }
            inObj.close();
            return false;
        } catch (Exception e) {
            return false;
        } finally {
            try {
                inObj.close();
            } catch (Exception e) {
                //Ignore
            }
        }
    }

    public void showWindow() {
        if (loadWindowState(getTitle(), this)) {
            setVisible(true);
            toFront();
            return;
        }
        display();
        Dimension ss = Toolkit.getDefaultToolkit().getScreenSize();
        if (ss.width <= 1024) {
            setExtendedState(MAXIMIZED_BOTH);
        }
        toFront();
    }

    @Override
    public void titleChanged(MDIWindow source, String newTitle) {
    }

    @Override
    public void windowSelected(MDIWindow source) {
    }

    public ToolbarActionManager getToolbarActionManager() {
        return toolbarActionManager;
    }

    public MDIManager getMDIManager() {
        return mdiManager;
    }

    public void chatMessage(int guid, String message, long tick, boolean messageHasBeenQueuedAwayForAWhile) throws Exception {
        PrivateChatMessageMDIWindow w = (PrivateChatMessageMDIWindow) mdiManager.getWindow("msg" + guid);
        if (w == null) {
            w = new PrivateChatMessageMDIWindow(ui, guid);
            mdiManager.addWindow(w);
        }
        if (message != null) {
            w.addMessage(ui.getCore().getFriendManager().nickname(guid), message, tick, messageHasBeenQueuedAwayForAWhile);
        }
    }

    public void publicChatMessage(int guid, String message, long tick, boolean messageHasBeenQueuedAwayForAWhile) throws Exception {
        if (message != null) {
            if (T.t) {
                T.info("Received public chat message: " + message);
            }
            if (ui.getCore().getSettings().getMy().getGuid() != guid) {
                UISound Sound = new UISound(new File(ui.getCore().getSettings().getInternal().getPublicsound()));
                Sound.start();
            }
            publicChat.addMessage(ui.getCore().getFriendManager().nickname(guid), message, tick, messageHasBeenQueuedAwayForAWhile);
            ui.getCore().getPublicChatHistory().addMessage(tick, guid, message);
        }
    }

    public void viewShare(Node f) throws Exception {
        ArrayList<MDIWindow> al = new ArrayList<MDIWindow>();
        for (MDIWindow w : mdiManager) {
            al.add(w);
        }
        for (MDIWindow w : al) {
            if (w instanceof ViewShareMDIWindow) {
                mdiManager.removeWindow(w);
            }
        }
        ViewShareMDIWindow w = (ViewShareMDIWindow) mdiManager.getWindow("viewshare" + f.getGuid());
        if (w == null) {
            w = new ViewShareMDIWindow(ui, f);
            mdiManager.addWindow(w);
        }
        mdiManager.selectWindow(w);
    }

    public void EVENT_myshare(ActionEvent e) throws Exception {
        viewShare(ui.getCore().getFriendManager().getMe());
    }

    public SearchMDIWindow getSearchWindow() {
        return (SearchMDIWindow) mdiManager.getWindow("Search");
    }

    public TraceMDIWindow getTraceWindow() {
        if (mdiManager != null) {
            return (TraceMDIWindow) mdiManager.getWindow("trace");
        } else {
            return null;
        }
    }

    public void createTraceWindow() throws Exception {
        TraceMDIWindow w = (TraceMDIWindow) mdiManager.getWindow("trace");
        if (w == null) {
            w = new TraceMDIWindow(ui);
            mdiManager.addWindow(w);
        }
    }

    public ConnectionsMDIWindow getConnectionsWindow() {
        return (ConnectionsMDIWindow) mdiManager.getWindow("connections");
    }

    public DownloadsMDIWindow getDownloadsWindow() {
        return (DownloadsMDIWindow) mdiManager.getWindow("downloads");
    }

    public UploadsMDIWindow getUploadsWindow() {
        return (UploadsMDIWindow) mdiManager.getWindow("uploads");
    }

    public MDIWindow getFriendMDIWindow() {
        return (FriendsTreeMDIWindow) mdiManager.getWindow("friends");
    }

    public FriendListMDIWindow getFriendListMDIWindow() {
        return (FriendListMDIWindow) mdiManager.getWindow("friendlist");
    }

    public ConsoleMDIWindow getConsoleMDIWindow() {
        return (ConsoleMDIWindow) mdiManager.getWindow("console");
    }

    @Override
    public void run() {
        while (!shuttingDown) {
            if (isVisible()) {
                SwingUtilities.invokeLater(new Runnable() {

                    @Override
                    public void run() {
                        if (mdiManager != null && getConnectionsWindow() != null) {
                            getConnectionsWindow().updateConnectionData();
                        }
                        if (mdiManager != null && getDownloadsWindow() != null) {
                            getDownloadsWindow().update();
                        }
                        if (mdiManager != null && getUploadsWindow() != null) {
                            getUploadsWindow().update();
                        }
                        if (mdiManager != null && getFriendListMDIWindow() != null) {
                            getFriendListMDIWindow().update();
                        }
                        shareMessage.setText(Language.getLocalizedString(getClass(), "sharesinfo",
                                TextUtils.formatByteSize(ui.getCore().getShareManager().getFileDatabase().getShareSize()),
                                Integer.toString(ui.getCore().getShareManager().getFileDatabase().getNumberOfShares())));
                        updateBandwidth("in", bandwidthIn, ui.getCore().getNetworkManager().getBandwidthIn());
                        updateBandwidth("out", bandwidthOut, ui.getCore().getNetworkManager().getBandwidthOut());
                        displayNagAboutInvitingFriendsIfNeeded();
                        if (ui.getCore().getShareManager().getShareScanner().isScanInProgress()) {
                            if (!rescan.getIcon().equals(refreshIconStates[1])) {
                                rescan.setToolTipText(Language.getLocalizedString(getClass(), "break"));
                                rescan.setIcon(refreshIconStates[1]);
                            }
                        } else {
                            if (!rescan.getIcon().equals(refreshIconStates[0])) {
                                rescan.setToolTipText(Language.getLocalizedString(getClass(), "rescan"));
                                rescan.setIcon(refreshIconStates[0]);
                            }
                        }
                    }

                    private void displayNagAboutInvitingFriendsIfNeeded() {
                        try {
                            if (ui.getCore().getFriendManager().getMe().getNumberOfInvitedFriends() > 0 || (ui.getCore().getSettings().getInternal().getHastriedtoinviteafriend() != null && ui.getCore().getSettings().getInternal().getHastriedtoinviteafriend() != 0)) {
                                return;
                            }
                            Long tick = ui.getCore().getSettings().getInternal().getLastnaggedaboutinvitingafriend();
                            if (tick == null) {
                                tick = System.currentTimeMillis() - 1000l * 60 * 60 * 24 * 10;
                                ui.getCore().getSettings().getInternal().setLastnaggedaboutinvitingafriend(tick);
                            } else {
                                if (System.currentTimeMillis() - tick > 1000l * 60 * 60 * 24 * 7) {
                                    //more then a week since we nagged last time
                                    if (ui.getCore().getFriendManager().getMe().getTotalBytesReceived() > 800 * MB) {
                                        //more then 800mb downloaded
                                        if (ui.getCore().getNetworkManager().getBandwidthIn().getCPS() > ui.getCore().getNetworkManager().getBandwidthIn().getHighestCPS() / 3) {
                                            //downloading at fairly high speed - let's show the infomration dialog
                                            ui.getCore().getSettings().getInternal().setLastnaggedaboutinvitingafriend(System.currentTimeMillis());
                                            if (OptionDialog.showQuestionDialog(MainWindow.this, Language.getLocalizedString(getClass(), "invitenag"))) {
                                                ui.getCore().getSettings().getInternal().setHastriedtoinviteafriend(1);
                                                openWizardAt(AddFriendWizard.STEP_PORT_OPEN_TEST);
                                            }
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) {
                            if (T.t) {
                                T.error(e);
                            }
                        }
                    }

                    private void updateBandwidth(String type, JProgressBar pb, BandwidthAnalyzer a) {
                        double curr = a.getCPS();
                        double max = a.getHighestCPS();
                        pb.setString(a.getCPSHumanReadable());
                        pb.setStringPainted(true);
                        if (max == 0) {
                            pb.setValue(0);
                        } else {
                            pb.setValue((int) (curr * 100 / max));
                        }
                        StringBuilder sb = new StringBuilder();
                        sb.append("<html>");
                        if (type.equals("in")) {
                            sb.append(Language.getLocalizedString(getClass(), "downloading", a.getCPSHumanReadable()));
                        } else {
                            sb.append(Language.getLocalizedString(getClass(), "uploading", a.getCPSHumanReadable()));
                        }
                        sb.append("<br>");
                        sb.append(Language.getLocalizedString(getClass(), "speedrecord", a.getHighestCPSHumanReadable()));
                        sb.append("<br>");
                        if (type.equals("in")) {
                            sb.append(Language.getLocalizedString(getClass(), "downtotalbytes", TextUtils.formatByteSize(a.getTotalBytes())));
                        } else {
                            sb.append(Language.getLocalizedString(getClass(), "uptotalbytes", TextUtils.formatByteSize(a.getTotalBytes())));
                        }
                        sb.append("</html>");
                        pb.setToolTipText(sb.toString());
                    }
                });
            }
            final boolean[] removedAUserInteraction = new boolean[]{false};
            for (NeedsUserInteraction nui : ui.getCore().getAllUserInteractionsInQue()) {
                if (userInteractionsInProgress == 0 || nui.canRunInParallelWithOtherInteractions()) {
                    if (nui instanceof NewFriendConnectedUserInteraction && isConnectedToNewFriendDialogShowing) {
                        continue; //wait til the dialog is no longer displayed
                    }
                    if (T.t) {
                        T.info("running user interaction: " + nui);
                    }
                    ui.getCore().removeUserInteraction(nui);
                    final NeedsUserInteraction nui1 = nui;
                    SwingUtilities.invokeLater(new Runnable() {

                        @Override
                        public void run() {
                            handleNeedsUserInteraction(nui1);
                        }
                    });
                    if (!(nui instanceof NewFriendConnectedUserInteraction)) {
                        removedAUserInteraction[0] = true;
                    }
                    break;
                }
            }

            if (!removedAUserInteraction[0]) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                }
            }
        }
    }

    // @todo: move this logic in separate class - it's getting too big
    private void handleNeedsUserInteraction(NeedsUserInteraction nui) {
        userInteractionsInProgress++;
        try {
            if (nui instanceof PostMessageInteraction) {
                PostMessageInteraction pmi = (PostMessageInteraction) nui;
                try {
                    if (pmi instanceof PostMessageToAllInteraction) {
                        publicChatMessage(pmi.getFromGuid(), pmi.getMessage(), pmi.getSentAtTick(), pmi.isMessageWasPersisted());
                    } else {
                        chatMessage(pmi.getFromGuid(), pmi.getMessage(), pmi.getSentAtTick(), pmi.isMessageWasPersisted());
                    }
                } catch (Exception e) {
                    ui.handleErrorInEventLoop(e);
                }
            } else if (nui instanceof PleaseForwardInvitationInteraction) {
                final PleaseForwardInvitationInteraction pmi = (PleaseForwardInvitationInteraction) nui;
                try {
                    if (ui.getCore().getSettings().getInternal().getAlwaysallowfriendstoconnect() > 0) {
                        forwardInvitation(pmi);
                    } else {
                        ForwardInvitationDialog d = new ForwardInvitationDialog(ui, pmi); //blocks
                        if (d.hasPressedYes()) {
                            forwardInvitation(pmi);
                        }
                        if (d.alwaysAllowInvite()) {
                            ui.getCore().getSettings().getInternal().setAlwaysallowfriendstoconnect(1);
                        }
                    }
                } catch (Exception e) {
                    ui.handleErrorInEventLoop(e);
                }
            } else if (nui instanceof NeedsToRestartBecauseOfUpgradeInteraction) {
                if (!ui.getCore().getAwayManager().isAway()) {
                    OptionDialog.showInformationDialog(this, Language.getLocalizedString(getClass(), "newupdateok"));
                    ui.getCore().getFileManager().getSiteUpdater().prepareUpdate();
                }
            } else if (nui instanceof NewVersionAvailableInteraction) {
                if (!ui.getCore().getAwayManager().isAway()) {
                    try {
                        OptionDialog updateDialog = new OptionDialog(this, Language.getLocalizedString(getClass(), "newupdateheader"),
                                Language.getLocalizedString(getClass(), "newupdate",
                                ui.getCore().getFileManager().getSiteUpdater().getSiteVersion(),
                                Integer.toString(ui.getCore().getFileManager().getSiteUpdater().getSiteBuild()))
                                + "[a href='.']" + Language.getLocalizedString(getClass(), "newupdateinfo") + "[/a]", 1, 1, true);
                        JLayeredPane lp = (JLayeredPane) updateDialog.getRootPane().getComponent(1);
                        JPanel p = (JPanel) ((JPanel) lp.getComponent(0)).getComponent(0);
                        for (Component c : p.getComponents()) {
                            if (c instanceof JHtmlLabel) {

                                ((JHtmlLabel) c).addHyperlinkListener(new HyperlinkListener() {

                                    @Override
                                    public void hyperlinkUpdate(HyperlinkEvent e) {
                                        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                                            ui.openURL("http://code.google.com/p/alliancep2pbeta/wiki/Info");
                                        }
                                    }
                                });
                            }
                        }
                        if (updateDialog.showAndGetResult()) {
                            ui.getCore().getFileManager().getSiteUpdater().beginDownload();
                        }
                    } catch (Exception ex) {
                        ui.getCore().getUICallback().statusMessage(Language.getLocalizedString(getClass(), "newupdateerror"), true);
                    }
                }
            } else if (nui instanceof ForwardedInvitationInteraction) {
                ForwardedInvitationInteraction fii = (ForwardedInvitationInteraction) nui;
                if (ui.getCore().getFriendManager().getFriend(fii.getFromGuid()) != null && ui.getCore().getFriendManager().getFriend(fii.getFromGuid()).isConnected()) {
                    if (T.t) {
                        T.error("Already was connected to this friend!!");
                    }
                } else {
                    if (ui.getCore().getSettings().getInternal().getAutomaticallydenyallinvitations() == 0
                            && OptionDialog.showQuestionDialog(this, Language.getLocalizedString(getClass(), "friendattempt", fii.getRemoteName(), fii.getRemoteName(), fii.getMiddleman(ui.getCore()).getNickname(), fii.getRemoteName()))) {
                        try {
                            ui.getCore().getInvitationManager().attemptToBecomeFriendWith(fii.getInvitationCode(), fii.getMiddleman(ui.getCore()));
                            openWizardAt(AddFriendWizard.STEP_ATTEMPT_CONNECT, fii.getFromGuid());
                        } catch (Exception e) {
                            ui.handleErrorInEventLoop(e);
                        }
                    } else {
                        //nothing
                    }
                }
            } else if (nui instanceof NewFriendConnectedUserInteraction) {
                NewFriendConnectedUserInteraction i = (NewFriendConnectedUserInteraction) nui;
                String name = ui.getCore().getFriendManager().nickname(i.getGuid());
                if (lastAddFriendWizard != null) {
                    lastAddFriendWizard.connectionWasSuccessful();
                }

                if (ui.getCore().doesInterationQueContain(ForwardedInvitationInteraction.class)
                        || new ForwardInvitationNodesList.ForwardInvitationListModel(ui.getCore()).getSize() == 0) {
                    if (lastAddFriendWizard != null) {
                        lastAddFriendWizard.getOuterDialog().dispose();
                    }
                    showSuccessfullyConnectedToNewFriendDialog(name);
                    //after this method completes the next pending interaction will be processed.
                } else {
                    if (ui.getCore().getSettings().getInternal().getAlwaysautomaticallyconnecttoallfriendsoffriend() == 1) {
                        ui.getCore().getFriendManager().connectToAllFriendsOfFriends();
                    } else {
                        showSuccessfullyConnectedToNewFriendDialog(name);
                        try {
                            if (ui.getCore().getSettings().getInternal().getDisablenewuserpopup() == 0) {
                                openWizardAt(AddFriendWizard.STEP_FORWARD_INVITATIONS);
                            }
                        } catch (Exception e) {
                            ui.handleErrorInEventLoop(e);
                        }
                    }
                }
                try {
                    getFriendListMDIWindow().updateMyLevelInformation();
                } catch (IOException e) {
                    ui.handleErrorInEventLoop(e);
                }
            } else if (nui instanceof FriendAlreadyInListUserInteraction) {
                if (T.t) {
                    T.trace("Last wizard: " + lastAddFriendWizard);
                }
                if (lastAddFriendWizard != null) {
                    lastAddFriendWizard.getOuterDialog().dispose();
                    if (T.t) {
                        T.trace("Wizard disposed");
                    }
                }
            } else {
                System.out.println("unknown: " + nui);
            }
        } finally {
            userInteractionsInProgress--;
        }
    }
    private boolean isConnectedToNewFriendDialogShowing = false;
    private boolean isAddRuleWindowDialogShowing = false;

    public boolean isConnectedToNewFriendDialogShowing() {
        return isConnectedToNewFriendDialogShowing;
    }

    public boolean isAddRuleWindowDialogShowing() {
        return isAddRuleWindowDialogShowing;
    }

    public void setConnectedToNewFriendDialogShowing(boolean connectedToNewFriendDialogShowing) {
        isConnectedToNewFriendDialogShowing = connectedToNewFriendDialogShowing;
    }

    public void setAddRuleWindowDialogShowing(boolean AddruleWindowDialogShowing) {
        isAddRuleWindowDialogShowing = AddruleWindowDialogShowing;
    }

    private void showSuccessfullyConnectedToNewFriendDialog(String name) {
        try {
            if (ui.getCore().getSettings().getInternal().getDisablenewuserpopup() == 0) {
                if (ui.getCore().getSettings().getInternal().getAlwaysautomaticallyconnecttoallfriendsoffriend() == 0) {
                    new ConnectedToNewFriendDialog(ui, this, name);
                }
            }
        } catch (Exception e) {
            ui.handleErrorInEventLoop(e);
        }
    }

    public void forwardInvitation(final PleaseForwardInvitationInteraction pmi) {
        ui.getCore().invokeLater(new Runnable() {

            @Override
            public void run() {
                try {
                    ui.getCore().getFriendManager().forwardInvitation(pmi);
                } catch (final IOException e) {
                    SwingUtilities.invokeLater(new Runnable() {

                        @Override
                        public void run() {
                            ui.handleErrorInEventLoop(e);
                        }
                    });
                }
            }
        });
    }

    public void EVENT_options(ActionEvent e) throws Exception {
        new OptionsWindow(ui);
    }

    public void EVENT_invitations(ActionEvent e) throws Exception {
        InvitationsMDIWindow w = (InvitationsMDIWindow) mdiManager.getWindow("historychat");
        if (w == null) {
            w = new InvitationsMDIWindow(ui);
            mdiManager.addWindow(w);
        }
        mdiManager.selectWindow(w);
    }

    public void EVENT_shares(ActionEvent e) throws Exception {
        new SharesWindow(ui);
    }

    public void EVENT_addshare(ActionEvent e) throws Exception {
        EVENT_shares(e);
    }

    public void EVENT_plugins(ActionEvent e) throws Exception {
        new AddPluginWindow(ui);
    }

    public void EVENT_chathistory(ActionEvent e) throws Exception {
        HistoryChatMessageMDIWindow w = (HistoryChatMessageMDIWindow) mdiManager.getWindow("historychat");
        if (w == null) {
            w = new HistoryChatMessageMDIWindow(ui);
            mdiManager.addWindow(w);
        }
        mdiManager.selectWindow(w);
    }

    public void EVENT_trace(ActionEvent e) throws Exception {
        if (!org.alliance.T.t) {
            ((JComponent) e.getSource()).setEnabled(false);
            OptionDialog.showInformationDialog(this, Language.getLocalizedString(getClass(), "menudisabled"));
        } else {
            createTraceWindow();
        }
    }

    public void EVENT_exitapp(ActionEvent e) throws Exception {
        if (OptionDialog.showQuestionDialog(this, Language.getLocalizedString(getClass(), "exit"))) {
            if (ui.getCore() != null) {
                ui.getCore().shutdown();
            }

            if (ui != null) {
                ui.shutdown();
                ui = null;
            }
            System.exit(0);
        }
    }

    public void EVENT_rescan(ActionEvent e) {
        ui.getCore().getShareManager().getShareScanner().startScan(true);
    }

    public void EVENT_console(ActionEvent e) throws Exception {
        ConsoleMDIWindow w = (ConsoleMDIWindow) mdiManager.getWindow("console");
        if (w == null) {
            w = new ConsoleMDIWindow(ui);
            mdiManager.addWindow(w);
        }
        mdiManager.selectWindow(w);
    }

    public void EVENT_connections(ActionEvent e) throws Exception {
        ConnectionsMDIWindow w = (ConnectionsMDIWindow) mdiManager.getWindow("connections");
        if (w == null) {
            w = new ConnectionsMDIWindow(ui);
            mdiManager.addWindow(w);
        }
        mdiManager.selectWindow(w);
    }

    public void EVENT_changelog(ActionEvent e) throws Exception {
        mdiManager.addWindow(new WelcomeMDIWindow(ui));
    }

    public void EVENT_uploads(ActionEvent e) throws Exception {
        UploadsMDIWindow w = (UploadsMDIWindow) mdiManager.getWindow("uploads");
        if (w == null) {
            w = new UploadsMDIWindow(ui);
            mdiManager.addWindow(w);
        }
        mdiManager.selectWindow(w);
    }

    public void EVENT_dups(ActionEvent e) throws Exception {
        DuplicatesMDIWindow w = (DuplicatesMDIWindow) mdiManager.getWindow("duplicates");
        if (w == null) {
            w = new DuplicatesMDIWindow(ui);
            mdiManager.addWindow(w);
        }
        mdiManager.selectWindow(w);
    }

    public void EVENT_friendtree(ActionEvent e) throws Exception {
        if (UISubsystem.NODE_TREE_MODEL_DISABLED) {
            ((JComponent) e.getSource()).setEnabled(false);
            OptionDialog.showInformationDialog(this, Language.getLocalizedString(getClass(), "menudisabled"));
        } else {
            mdiManager.addWindow(new FriendsTreeMDIWindow(mdiManager, ui));
        }
    }

    public void EVENT_addfriendwizard(ActionEvent e) throws Exception {
        openWizard();
    }

    public void openWizard() throws Exception {
        if (T.t) {
            T.ass(lastAddFriendWizard == null || !lastAddFriendWizard.getOuterDialog().isVisible(), "Wizard already open!");
        }
        lastAddFriendWizard = AddFriendWizard.open(ui, AddFriendWizard.STEP_INTRO);
        lastAddFriendWizard.getOuterDialog().display();
    }

    public void openWizardAt(int step, Integer invitationFromGuid) throws Exception {
        if (lastAddFriendWizard != null) {
            if (T.t) {
                T.trace("visible: " + lastAddFriendWizard.getOuterDialog().isVisible());
            }
        }
        if (lastAddFriendWizard != null && lastAddFriendWizard.getOuterDialog().isVisible()) {
            if (T.t) {
                T.ass(step == AddFriendWizard.STEP_FORWARD_INVITATIONS || step == AddFriendWizard.STEP_ATTEMPT_CONNECT || step == AddFriendWizard.STEP_PORT_OPEN_TEST, "No support for starting at step " + step + " like this");
            }
            lastAddFriendWizard.setInvitationFromGuid(invitationFromGuid);
            if (step == AddFriendWizard.STEP_FORWARD_INVITATIONS) {
                lastAddFriendWizard.goToForwardInvitations();
            } else if (step == AddFriendWizard.STEP_ATTEMPT_CONNECT) {
                lastAddFriendWizard.goToAttemptConnect();
            } else {
                lastAddFriendWizard.goToPortTest();
            }
        } else {
            lastAddFriendWizard = AddFriendWizard.open(ui, step);
            lastAddFriendWizard.setInvitationFromGuid(invitationFromGuid);
            lastAddFriendWizard.getOuterDialog().display();
        }
    }

    public void openWizardAt(int step) throws Exception {
        openWizardAt(step, null);
    }

    public void shareBaseListReceived(Friend friend, String[] shareBaseNames) {
        if (friend == null) {
            return;
        }
        ViewShareMDIWindow w = (ViewShareMDIWindow) mdiManager.getWindow("viewshare" + friend.getGuid());
        if (w == null) {
            if (T.t) {
                T.error("Could not find view share window for " + friend);
            }
        } else {
            w.shareBaseListReceived(shareBaseNames);
        }
    }

    public void directoryListingReceived(Friend friend, int shareBaseIndex, String path, TreeMap<String, Long> fileSize) {
        if (friend == null) {
            return;
        }
        ViewShareMDIWindow w = (ViewShareMDIWindow) mdiManager.getWindow("viewshare" + friend.getGuid());
        if (w == null) {
            if (T.t) {
                T.error("Could not find view share window for " + friend);
            }
        } else {
            w.directoryListingReceived(shareBaseIndex, path, fileSize);
        }
        fileSize.clear();
    }

    public PublicChatMessageMDIWindow getPublicChat() {
        return publicChat;
    }
}

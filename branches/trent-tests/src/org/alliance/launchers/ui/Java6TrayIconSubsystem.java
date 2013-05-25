package org.alliance.launchers.ui;

import com.stendahls.util.resourceloader.ResourceLoader;
import com.stendahls.util.TextUtils;
import org.alliance.Subsystem;
import org.alliance.Version;
import org.alliance.core.CoreSubsystem;
import org.alliance.core.NeedsUserInteraction;
import org.alliance.core.ResourceSingelton;
import org.alliance.core.T;
import org.alliance.core.UICallback;
import org.alliance.core.comm.SearchHit;
import org.alliance.core.interactions.PostMessageInteraction;
import org.alliance.core.interactions.PostMessageToAllInteraction;
import org.alliance.core.node.Friend;
import org.alliance.core.node.Node;
import org.alliance.core.SimpleTimer;
import org.alliance.core.Language;

import java.awt.Font;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Method;
import java.util.List;
import java.util.TreeMap;

/**
 * New tray icon using java.awt.SystemTray (new stuff in Java 6).
 */
public class Java6TrayIconSubsystem implements Subsystem, Runnable {

    private CoreSubsystem core;
    private Subsystem ui;
    private ResourceLoader rl;
    private SystemTray tray;
    private TrayIcon ti;
    private Runnable balloonClickHandler;

    @Override
    public void init(ResourceLoader rl, Object... params) throws Exception {
        this.rl = rl;
        core = (CoreSubsystem) params[0];
        initTray();
        core.addUICallback(new UICallback() {

            @Override
            public void firstDownloadEverFinished() {
            }

            @Override
            public void callbackRemoved() {
            }

            public void signalFileDatabaseFlushStarting() {
            }

            public void signalFileDatabaseFlushComplete() {
            }

            @Override
            public void nodeOrSubnodesUpdated(Node node) {
            }

            @Override
            public void noRouteToHost(Node node) {
            }

            @Override
            public void pluginCommunicationReceived(Friend source, String data) {
            }

            @Override
            public void searchHits(int srcGuid, int hops, List<SearchHit> hits) {
            }

            @Override
            public void trace(int level, String message, Exception stackTrace) {
            }

            @Override
            public void statusMessage(String s) {
            }

            @Override
            public void statusMessage(String s, boolean b) {
            }

            @Override
            public void toFront() {
            }

            @Override
            public void signalFriendAdded(Friend friend) {
            }

            @Override
            public boolean isUIVisible() {
                return false;
            }

            @Override
            public void logNetworkEvent(String event) {
            }

            @Override
            public void receivedShareBaseList(Friend friend, String[] shareBaseNames) {
            }

            @Override
            public void receivedDirectoryListing(Friend friend, int i, String s, TreeMap<String, Long> fileSize) {
            }

            @Override
            public void newUserInteractionQueued(NeedsUserInteraction ui) {
                if (ui instanceof PostMessageInteraction) {
                    PostMessageInteraction pmi = (PostMessageInteraction) ui;
                    String msg = pmi.getMessage().replaceAll("\\<.*?\\>", "");      // Strip html
                    if (pmi instanceof PostMessageToAllInteraction) {
                        if (core.getSettings().getInternal().getShowpublicchatmessagesintray() != 0) {
                            ti.displayMessage(Language.getLocalizedString(getClass(), "chatnew"), core.getFriendManager().nickname(pmi.getFromGuid()) + ": " + msg, TrayIcon.MessageType.INFO);
                        }
                    } else {
                        if (core.getSettings().getInternal().getShowprivatechatmessagesintray() != 0) {
                            ti.displayMessage(Language.getLocalizedString(getClass(), "privatenew"), core.getFriendManager().nickname(pmi.getFromGuid()) + ": " + msg, TrayIcon.MessageType.INFO);
                        }
                    }
                } else {
                    if (core.getSettings().getInternal().getShowsystemmessagesintray() != 0) {
                        ti.displayMessage(Language.getLocalizedString(getClass(), "attentionheader"), Language.getLocalizedString(getClass(), "attention"), TrayIcon.MessageType.INFO);
                    }
                }
                balloonClickHandler = new Runnable() {

                    @Override
                    public void run() {
                        openUI();
                    }
                };
            }

            @Override
            public void handleError(final Throwable e, final Object source) {
                ti.displayMessage(e.getClass().getName(), e + "\n" + source, TrayIcon.MessageType.ERROR);
                e.printStackTrace();
                balloonClickHandler = new Runnable() {

                    @Override
                    public void run() {
                        try {
                            e.printStackTrace();
                            //report error. Use reflection to init dialogs because we want NO references to UI stuff in this
                            //class - we want this class to load fast (ie load minimal amount of classes)
                            Object errorDialog = Class.forName("org.alliance.ui.dialogs.ErrorDialog").newInstance();
                            Method m = errorDialog.getClass().getMethod("init", Throwable.class, boolean.class);
                            m.invoke(errorDialog, e, false);
                        } catch (Throwable t) {
                            t.printStackTrace();
                        }
                    }
                };
            }
        });
    }

    private void initTray() throws Exception {
        tray = SystemTray.getSystemTray();

        PopupMenu m = new PopupMenu();
        Font f = new Font("Tahoma", 0, 11);
        m.setFont(f);

        MenuItem mi = new MenuItem(Language.getLocalizedString(getClass(), "menuopen"));
        mi.setFont(f);
        mi.setFont(new Font(mi.getFont().getName(), mi.getFont().getStyle() | Font.BOLD, mi.getFont().getSize()));
        mi.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                openUI();
            }
        });
        m.add(mi);

        m.addSeparator();

        mi = new MenuItem(Language.getLocalizedString(getClass(), "menuunload"));
        mi.setFont(f);
        mi.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                unloadUI();
            }
        });
        m.add(mi);

        m.addSeparator();

        mi = new MenuItem(Language.getLocalizedString(getClass(), "menuexit"));
        mi.setFont(f);
        mi.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                shutdown();
            }
        });
        m.add(mi);

        Toolkit.getDefaultToolkit().getSystemEventQueue().push(new PopupFixQueue(m));

        int[] imageSizes = {16, 24, 32, 48, 64, 128};
        for (int size : imageSizes) {
            if (size - tray.getTrayIconSize().getWidth() < 0) {
                continue;
            } else {
                ti = new TrayIcon(Toolkit.getDefaultToolkit().getImage(rl.getResource("gfx/icons/alliance" + size + ".png")), "Alliance", m);
                break;
            }
        }

        ti.setImageAutoSize(true);

        ti.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                openUI();
            }
        });

        tray.add(ti);

        ti.addActionListener(new ActionListener() {

            private long lastClickAt;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (System.currentTimeMillis() - lastClickAt < 1000) {
                    openUI();
                }
                lastClickAt = System.currentTimeMillis();
            }
        });

        // Update tooltip periodically with current transfer rates
        Thread t = new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    while (true) {
                        ti.setToolTip("Alliance v" + Version.VERSION + " build " + Version.BUILD_NUMBER + "\nDownload: " + core.getNetworkManager().getBandwidthIn().getCPSHumanReadable() + "\nUpload: " + core.getNetworkManager().getBandwidthOut().getCPSHumanReadable() + "\nOnline: " + core.getFriendManager().getNFriendsConnected() + "/" + core.getFriendManager().getNFriends() + " (" + TextUtils.formatByteSize(core.getFriendManager().getTotalBytesShared()) + ")");
                        Thread.sleep(5000);
                    }
                } catch (InterruptedException e) {
                } catch (NullPointerException e) {
                }
            }
        });
        t.setDaemon(true);
        t.start();
    }

    @Override
    public synchronized void shutdown() {
        if (tray != null && ti != null) {
            ti.displayMessage("", Language.getLocalizedString(getClass(), "shutting"), TrayIcon.MessageType.NONE);
            balloonClickHandler = null;
        }

        if (ui != null) {
            ui.shutdown();
            ui = null;
        }
        if (core != null) {
            core.shutdown();
            core = null;
        }
        if (tray != null) {
            tray.remove(ti);
        }
        System.exit(0);
    }

    private synchronized void openUI() {
        try {
            if (ui != null) {
                if (T.t) {
                    T.info("Subsystem already started.");
                }
                core.uiToFront();
                return;
            }
            Runnable r = (Runnable) Class.forName("org.alliance.launchers.SplashWindow").newInstance();
            SimpleTimer s = new SimpleTimer();
            ui = (Subsystem) Class.forName("org.alliance.ui.UISubsystem").newInstance();
            ui.init(ResourceSingelton.getRl(), core, r);
            if (T.t) {
                T.trace("Subsystem UI started in " + s.getTime());
            }
            r.run();
        } catch (Exception t) {
            core.reportError(t, this);
        }
    }

    @Override
    public void run() {
        openUI();
    }

    private void unloadUI() {
        try {
            if (ui == null) {
                if (T.t) {
                    T.info("Subsystem already unloaded.");
                }
                return;
            }
            if (tray != null && ti != null) {
                ti.displayMessage("", Language.getLocalizedString(getClass(), "unloading"), TrayIcon.MessageType.NONE);
                balloonClickHandler = null;
            }
            core.restartProgram(false);
        } catch (Exception t) {
            core.reportError(t, this);
        }
    }
}

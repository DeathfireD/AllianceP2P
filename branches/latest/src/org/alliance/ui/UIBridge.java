package org.alliance.ui;

import com.stendahls.nif.ui.OptionDialog;
import org.alliance.core.NeedsUserInteraction;
import org.alliance.core.UICallback;
import org.alliance.core.comm.SearchHit;
import org.alliance.core.node.Friend;
import org.alliance.core.node.Node;
import org.alliance.launchers.OSInfo;

import java.io.File;
import java.util.List;
import java.util.TreeMap;
import javax.swing.SwingUtilities;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-jan-02
 * Time: 22:05:42
 * To change this template use File | Settings | File Templates.
 */
public class UIBridge implements UICallback {

    private UISubsystem ui;
    private UICallback oldCallback;

    public UIBridge(UISubsystem ui, UICallback oldCallback) {
        this.ui = ui;
        this.oldCallback = oldCallback;
    }

    @Override
    public void nodeOrSubnodesUpdated(final Node node) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                if (ui.getNodeTreeModel(false) != null) {
                    ui.getNodeTreeModel(false).signalNodeChanged(node);
                }
                if (node instanceof Friend) {
                    ui.getFriendListModel().signalFriendChanged((Friend) node);
                }
            }
        });
    }

    @Override
    public void signalFriendAdded(final Friend friend) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                try {
                    if (ui.getMainWindow().getFriendMDIWindow() != null) {
                        ui.getMainWindow().getFriendMDIWindow().revert();
                    }
                    ui.getFriendListModel().signalFriendAdded(friend);
                } catch (Exception e) {
                    ui.handleErrorInEventLoop(e);
                }
            }
        });
    }

    @Override
    public boolean isUIVisible() {
        return ui.getMainWindow().isVisible();
        //return ui.getMainWindow().isVisible() && ui.getMainWindow().getState() != Frame.ICONIFIED;
    }

    @Override
    public void logNetworkEvent(String event) {
        if (ui.getMainWindow().getConsoleMDIWindow() != null) {
            ui.getMainWindow().getConsoleMDIWindow().getConsole().logNetworkEvent(event);
        }
    }

    @Override
    public void receivedShareBaseList(final Friend friend, final String[] shareBaseNames) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                ui.getMainWindow().shareBaseListReceived(friend, shareBaseNames);
            }
        });
    }

    @Override
    public void receivedDirectoryListing(final Friend friend, final int shareBaseIndex, final String path, final TreeMap<String, Long> fileSize) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                ui.getMainWindow().directoryListingReceived(friend, shareBaseIndex, path, fileSize);
            }
        });
    }

    @Override
    public void newUserInteractionQueued(NeedsUserInteraction ui) {
    }

    @Override
    public void firstDownloadEverFinished() {
        if (ui.getCore().getSettings().getInternal().getHasneverdownloadedafile() != null && ui.getCore().getSettings().getInternal().getHasneverdownloadedafile() == 1) {
            ui.getCore().getSettings().getInternal().setHasneverdownloadedafile(0);
            if (OSInfo.isWindows()) {
                SwingUtilities.invokeLater(new Runnable() {

                    @Override
                    public void run() {
                        OptionDialog.showInformationDialog(ui.getMainWindow(),
                                "Congratulations! You have downloaded your first file using Alliance.[p]To find your Alliance downloads use the shortcut on the Desktop called 'My Alliance Downloads'.");
                    }
                });
            }
        }
        UISound Sound = new UISound(new File(ui.getCore().getSettings().getInternal().getDownloadsound()));
        Sound.start();
    }

    @Override
    public void callbackRemoved() {
    }

    @Override
    public void noRouteToHost(final Node node) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                if (ui.getNodeTreeModel(false) != null) {
                    ui.getNodeTreeModel(false).signalNoRouteToHost(node);
                }
            }
        });
    }

    @Override
    public void pluginCommunicationReceived(Friend source, String data) {
        if (T.t) {
            T.trace("Plugin communication received from " + source + ": " + data);
        }
    }

    @Override
    public void searchHits(final int fromGuid, final int hops, final List<SearchHit> hits) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                try {
                    ui.getMainWindow().getSearchWindow().searchHits(fromGuid, hops, hits);
                } catch (Exception e) {
                    ui.handleErrorInEventLoop(e);
                }
            }
        });
    }

    @Override
    public void trace(final int level, final String message, Exception stackTrace) {
        ui.makeSureThreadNameIsCorrect();

        if (ui == null || ui.getMainWindow() == null || ui.getMainWindow().getTraceWindow() == null) {
            return;
        }

        final Exception st;
        if (stackTrace == null) {
            st = new Exception();
        } else {
            st = stackTrace;
        }

        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                try {
                    ui.getMainWindow().getTraceWindow().trace(level, message, st);
                } catch (Exception e) {
                    ui.handleErrorInEventLoop(e);
                }
            }
        });
    }

    @Override
    public void handleError(final Throwable e, final Object source) {
        if (oldCallback != null) {
            oldCallback.handleError(e, source);
        } else {
            SwingUtilities.invokeLater(new Runnable() {

                @Override
                public void run() {
                    ui.handleErrorInEventLoop(new Exception(source + ": " + e, e));
                }
            });
        }
    }

    @Override
    public void statusMessage(final String s) {
        if (T.t) {
            T.info("status message: " + s);
        }
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                ui.getMainWindow().setStatusMessage(s);
            }
        });
    }

    @Override
    public void statusMessage(final String s, final boolean b) {
        if (T.t) {
            T.info("status message: " + s);
        }
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                ui.getMainWindow().setStatusMessage(s, b);
            }
        });
    }

    @Override
    public void toFront() {
        ui.getMainWindow().setVisible(true);
        ui.getMainWindow().toFront();
    }
}

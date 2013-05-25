package org.alliance.ui;

import com.stendahls.XUI.SwingDeadlockWarningRepaintManager;
import com.stendahls.XUI.XUIErrorDialogHelper;
import com.stendahls.XUI.XUIException;
import com.stendahls.nif.ui.mdi.infonodemdi.UINexus;
import com.stendahls.nif.ui.toolbaractions.ToolbarActionManager;
import com.stendahls.util.resourceloader.ResourceLoader;
import org.alliance.ui.dialogs.ErrorDialog;
import org.alliance.Subsystem;
import org.alliance.core.CoreSubsystem;
import org.alliance.core.Language;
import static org.alliance.core.CoreSubsystem.ERROR_URL;
import org.alliance.launchers.StartupProgressListener;
import org.alliance.ui.macos.OSXAdaptation;
import org.alliance.ui.nodetreemodel.NodeTreeModel;
import org.alliance.ui.nodetreemodel.NodeTreeNode;
import org.alliance.ui.dialogs.OptionDialog;
import org.alliance.ui.themes.util.SubstanceThemeHelper;

import java.awt.Font;
import java.awt.Window;
import java.lang.reflect.Method;
import java.util.Enumeration;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;
import org.alliance.core.UICallback;
import org.alliance.core.plugins.DoubleUICallback;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2005-dec-30
 * Time: 16:13:14
 */
public class UISubsystem implements UINexus, Subsystem {

    public static final boolean NODE_TREE_MODEL_DISABLED = true; //it's disabled when it's a production release because there's a bug in it and it's not really needed anyway
    private MainWindow mainWindow;
    private ResourceLoader rl;
    private CoreSubsystem core;
    private NodeTreeModel nodeTreeModel;
    private FriendListModel friendListModel;
    private StartupProgressListener progress;

    public UISubsystem() {
    }

    /**
     * @param rl 
     * @param params - takes one parameter - a boolean indicating if Alliance should shutdown when window closes
     * @throws Exception 
     */
    @Override
    public void init(ResourceLoader rl, final Object... params) throws Exception {
        this.rl = rl;
        core = (CoreSubsystem) params[0];

        progress = new StartupProgressListener() {

            @Override
            public void updateProgress(String message) {
            }
        };
        if (params != null && params.length >= 2 && params[1] != null) {
            progress = (StartupProgressListener) params[1];
        }
        progress.updateProgress(Language.getLocalizedString(getClass(), "uiloading"));

        if (SwingUtilities.isEventDispatchThread()) {
            realInit(params);
        } else {
            SwingUtilities.invokeAndWait(new Runnable() {

                @Override
                public void run() {
                    realInit(params);
                }
            });
        }
    }

    private void realInit(Object... params) {
        XUIErrorDialogHelper.setErrorDialogClass(ErrorDialog.class.getName());
        ErrorDialog.setErrorReportUrl(ERROR_URL);
        ErrorDialog.setExceptionTranslator(new ErrorDialog.ExceptionTranslator() {

            @Override
            public String translate(Throwable t) {
                Throwable innerError = t;

                if (innerError.getStackTrace().length > 0 && innerError.getStackTrace()[0].toString().indexOf("sun.java2d.pisces.Renderer.crossingListFinished") != -1) {
                    return null; //some java2d bug?
                }

                return innerError.toString();
            }
        });

        try {
            if (core.getSettings().getInternal().getGuiskin().equals("OS Native")) {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } else {
                SubstanceThemeHelper.setSubstanceTheme(core.getSettings().getInternal().getGuiskin());
            }

            if (core.getSettings().getInternal().getGlobalfont() != null
                    && !core.getSettings().getInternal().getGlobalfont().isEmpty()) {
                setGlobalFont(core.getSettings().getInternal().getGlobalfont(), core.getSettings().getInternal().getGlobalsize());
            }

            if (core.getSettings().getInternal().getEnablesupportfornonenglishcharacters() != null
                    && core.getSettings().getInternal().getEnablesupportfornonenglishcharacters() == 1) {
                setGlobalFont("Dialog", 12);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        Thread.setDefaultUncaughtExceptionHandler(new GlobalExceptionHandler());

        if (T.t) {
            SwingDeadlockWarningRepaintManager.hookRepaints(true, new String[]{"NetworkIndicator", "SystemMonitor"});
        }

        try {
            new OSXAdaptation(this);
            mainWindow = new MainWindow();
            mainWindow.init(UISubsystem.this, progress);
        } catch (Exception e) {
            handleErrorInEventLoop(e);
        }

        // add a UIBridge if none exist already in the chain
        if (!hasUIBridge(core.getUICallback())) {
            core.addUICallback(new UIBridge(this, core.getUICallback()));
        }
    }

    private boolean hasUIBridge(UICallback callback) {
        if (callback == null) {
            return false;
        } else if (callback instanceof UIBridge) {
            return true;
        } else if (callback instanceof DoubleUICallback) {
            DoubleUICallback dback = (DoubleUICallback) callback;
            return hasUIBridge(dback.getFirst()) || hasUIBridge(dback.getSecond());
        } else {
            return false;
        }
    }

    private void setGlobalFont(String fontName, int size) {
        Enumeration<Object> keys = UIManager.getDefaults().keys();
        FontUIResource f = new FontUIResource(fontName, Font.PLAIN, size);
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            Object value = UIManager.get(key);
            if (value instanceof FontUIResource) {
                FontUIResource orig = (FontUIResource) value;
                Font font = new Font(f.getFontName(), orig.getStyle(), size);
                UIManager.put(key, new FontUIResource(font));
            }
        }
    }

    @Override
    public void handleErrorInEventLoop(Throwable t) {
        handleErrorInEventLoop(null, t, false);
    }

    @Override
    public void handleErrorInEventLoop(Throwable t, boolean fatal) {
        handleErrorInEventLoop(null, t, fatal);
    }

    @Override
    public void handleErrorInEventLoop(Window parent, Throwable t, boolean fatal) {
        core.reportError(t, null);
    }

    @Override
    public MainWindow getMainWindow() {
        return mainWindow;
    }

    @Override
    public ResourceLoader getRl() {
        return rl;
    }

    @Override
    public ToolbarActionManager getToolbarActionManager() {
        return mainWindow.getToolbarActionManager();
    }

    @Override
    public void shutdown() {
        mainWindow.shutdown();
        Thread.setDefaultUncaughtExceptionHandler(null);
    }

    public CoreSubsystem getCore() {
        makeSureThreadNameIsCorrect();
        return core;
    }

    void makeSureThreadNameIsCorrect() {
        if (T.t) {
            //make sure we have a labaled thread name - for testsuite
            if (Thread.currentThread().getName().indexOf(core.getFriendManager().getMe().getNickname()) == -1) {
                String n = Thread.currentThread().getName();
                if (n.indexOf(' ') != -1) {
                    n = n.substring(0, n.indexOf(' '));
                }
                Thread.currentThread().setName(n + " -- " + core.getFriendManager().getMe().getNickname());
            }
        }
    }

    public NodeTreeModel getNodeTreeModel(boolean loadIfNeeded) {
        if (NODE_TREE_MODEL_DISABLED) {
            return null;
        } else {
            if (nodeTreeModel == null && loadIfNeeded) {
                nodeTreeModel = new NodeTreeModel();
                nodeTreeModel.setRoot(new NodeTreeNode(core.getFriendManager().getMe(), null, this, nodeTreeModel));
            }
            return nodeTreeModel;
        }
    }

    public void purgeNodeTreeModel() {
        nodeTreeModel = null;
    }

    public FriendListModel getFriendListModel() {
        if (friendListModel == null) {
            friendListModel = new FriendListModel(core, this);
        }
        return friendListModel;
    }

    //borrowed from BareBonesBrowserLaunch
    public void openURL(String url) {
        String osName = System.getProperty("os.name");
        try {
            if (osName.startsWith("Mac OS")) {
                Class<?> fileMgr = Class.forName("com.apple.eio.FileManager");
                Method openURL = fileMgr.getDeclaredMethod("openURL",
                        new Class<?>[]{String.class});
                openURL.invoke(null, new Object[]{url});
            } else if (osName.startsWith("Windows")) {
                String s = "rundll32 url.dll,FileProtocolHandler " + url;
                Runtime.getRuntime().exec(s);
            } else { //assume Unix or Linux
                String[] browsers = {
                    "firefox", "opera", "konqueror", "epiphany", "mozilla", "netscape"};
                String browser = null;
                for (int count = 0; count < browsers.length && browser == null; count++) {
                    if (Runtime.getRuntime().exec(
                            new String[]{"which", browsers[count]}).waitFor() == 0) {
                        browser = browsers[count];
                    }
                }
                if (browser == null) {
                    throw new Exception("Could not find web browser");
                } else {
                    Runtime.getRuntime().exec(new String[]{browser, url});
                }
            }
        } catch (Exception e) {
            OptionDialog.showErrorDialog(getMainWindow(), Language.getLocalizedString(getClass(), "browsererror", e.toString()));
        }
    }

    private class GlobalExceptionHandler implements Thread.UncaughtExceptionHandler {

        @Override
        public void uncaughtException(Thread t, Throwable e) {
            try {
                new ErrorDialog(e, false);
            } catch (XUIException e1) {
                e1.printStackTrace();
            }
        }
    }
}

package org.alliance.ui;

import com.stendahls.nif.ui.OptionDialog;
import com.stendahls.nif.ui.framework.GlobalExceptionHandler;
import com.stendahls.nif.ui.framework.SwingDeadlockWarningRepaintManager;
import com.stendahls.nif.ui.framework.UINexus;
import com.stendahls.nif.ui.toolbaractions.ToolbarActionManager;
import com.stendahls.resourceloader.ResourceLoader;
import com.stendahls.ui.ErrorDialog;
import de.javasoft.plaf.synthetica.SyntheticaBlackStarLookAndFeel;
import de.javasoft.plaf.synthetica.SyntheticaLookAndFeel;
import java.awt.Font;
import javax.swing.plaf.FontUIResource;
import org.alliance.Subsystem;
import org.alliance.core.CoreSubsystem;
import static org.alliance.core.CoreSubsystem.ERROR_URL;
import org.alliance.launchers.StartupProgressListener;
import org.alliance.ui.macos.OSXAdaptation;
import org.alliance.ui.nodetreemodel.NodeTreeModel;
import org.alliance.ui.nodetreemodel.NodeTreeNode;

import java.awt.Window;
import java.lang.reflect.Method;
import java.util.Enumeration;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2005-dec-30
 * Time: 16:13:14
 */
public class UISubsystem implements UINexus, Subsystem {

    public static final boolean NODE_TREE_MODEL_DISABLED = false; //it's disabled when it's a production release because there's a bug in it and it's not really needed anyway
    private MainWindow mainWindow;
    private ResourceLoader rl;
    private CoreSubsystem core;
    private NodeTreeModel nodeTreeModel;
    private FriendListModel friendListModel;
    private StartupProgressListener progress;

    public UISubsystem() {
    }

    /**
     * @param params - takes one parameter - a boolean indicating if Alliance should shutdown when window closes
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
        progress.updateProgress("Loading user interface");

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
        ErrorDialog.setErrorReportUrl(ERROR_URL);
        ErrorDialog.setExceptionTranslator(new ErrorDialog.ExceptionTranslator() {

            @Override
            public String translate(Throwable t) {
                Throwable innerError = t;

                if (innerError.getStackTrace().length > 0 && innerError.getStackTrace()[0].toString().indexOf("de.javasoft.plaf.synthetica.StyleFactory$ComponentProperty.hashCode") != -1) {
                    return null; //some wicked blackstar bug probably
                }

                if (innerError.getStackTrace().length > 0 && innerError.getStackTrace()[0].toString().indexOf("sun.java2d.pisces.Renderer.crossingListFinished") != -1) {
                    return null; //some java2d bug?
                }

                return innerError.toString();
            }
        });

        try {
            if (core.getSettings().getInternal().getGuiskin().equals("OS Native")) {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                if (core.getSettings().getInternal().getEnablesupportfornonenglishcharacters() != null
                        && core.getSettings().getInternal().getEnablesupportfornonenglishcharacters() == 1) {
                    Enumeration keys = UIManager.getDefaults().keys();
                    FontUIResource f = new FontUIResource(new javax.swing.plaf.FontUIResource("Dialog", Font.TRUETYPE_FONT, 12));
                    while (keys.hasMoreElements()) {
                        Object key = keys.nextElement();
                        Object value = UIManager.get(key);
                        if (value instanceof FontUIResource) {
                            FontUIResource orig = (FontUIResource) value;
                            Font font = new Font(f.getFontName(), orig.getStyle(), f.getSize());
                            UIManager.put(key, new FontUIResource(font));
                        }
                    }
                }
            } else if (core.getSettings().getInternal().getGuiskin().equals("Alliance")) {
                JFrame.setDefaultLookAndFeelDecorated(true);
                JDialog.setDefaultLookAndFeelDecorated(true);
                UIManager.setLookAndFeel(new SyntheticaBlackStarLookAndFeel());
                if (core.getSettings().getInternal().getEnablesupportfornonenglishcharacters() != null
                        && core.getSettings().getInternal().getEnablesupportfornonenglishcharacters() == 1) {
                    SyntheticaLookAndFeel.setFont("Dialog", 12);
                }
            } else {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
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

        core.setUICallback(new UIBridge(this, core.getUICallback()));
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
//        try {
//            if (parent == null) parent = getMainWindow();
//            if (parent instanceof JDialog) {
//                new ErrorDialog((JDialog) parent, t, fatal);
//            } else if (parent instanceof JFrame) {
//                new ErrorDialog((JFrame) parent, t, fatal);
//            } else {
//                new ErrorDialog(t, fatal);
//            }
//        } catch (XUIException e) {
//            if (T.t) T.error("Oh no! Could not open error dialog!");
//            e.printStackTrace();
//        }
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
        core.setUICallback(null);
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
                Class fileMgr = Class.forName("com.apple.eio.FileManager");
                Method openURL = fileMgr.getDeclaredMethod("openURL",
                        new Class[]{String.class});
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
            OptionDialog.showErrorDialog(getMainWindow(), "Could not open url: " + e);
        }
    }
}

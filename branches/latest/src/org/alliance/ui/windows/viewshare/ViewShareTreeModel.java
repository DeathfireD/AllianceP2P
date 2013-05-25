package org.alliance.ui.windows.viewshare;

import com.stendahls.ui.T;
import org.alliance.core.CoreSubsystem;
import org.alliance.core.file.share.ShareBase;
import org.alliance.core.comm.rpc.GetShareBaseList;
import org.alliance.core.node.Friend;
import org.alliance.core.node.Node;
import org.alliance.ui.UISubsystem;

import javax.swing.tree.DefaultTreeModel;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-aug-10
 * Time: 18:14:33
 */
public class ViewShareTreeModel extends DefaultTreeModel {

    private Node node;
    private CoreSubsystem core;
    private UISubsystem ui;
    private ViewShareMDIWindow win;

    public ViewShareTreeModel(final Node node, final UISubsystem ui, ViewShareMDIWindow win) throws IOException {
        super(new ViewShareRootNode());
        this.node = node;
        this.win = win;
        this.ui = ui;
        core = ui.getCore();

        getRoot().setModel(this);

        if (node instanceof Friend) {
            //send get share base list to remote - answer will come asynchronously
            core.invokeLater(new Runnable() {

                @Override
                public void run() {
                    if (node.isConnected()) {
                        try {
                            ((Friend) node).getFriendConnection().send(new GetShareBaseList());
                        } catch (IOException e) {
                            core.reportError(e, this);
                        }
                    } else {
                        if (T.t) {
                            T.error("Friend is not connected! " + node);
                        }
                    }
                }
            });
        } else {
            ArrayList<String> al = new ArrayList<String>();
            for (ShareBase sb : core.getFileManager().getShareManager().shareBases()) {
                al.add(sb.getName());
            }
            getRoot().fill(al.toArray(new String[al.size()]));
        }
    }

    public ViewShareMDIWindow getWin() {
        return win;
    }

    public void shareBaseNamesReceived(String[] shareBaseNames) {
        getRoot().fill(shareBaseNames);
        nodeStructureChanged(getRoot());
    }

    @Override
    public ViewShareRootNode getRoot() {
        return (ViewShareRootNode) super.getRoot();
    }

    public CoreSubsystem getCore() {
        return core;
    }

    public Node getNode() {
        return node;
    }

    public UISubsystem getUi() {
        return ui;
    }
}

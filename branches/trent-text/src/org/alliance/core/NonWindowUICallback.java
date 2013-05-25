package org.alliance.core;

import org.alliance.core.comm.SearchHit;
import org.alliance.core.node.Friend;
import org.alliance.core.node.Node;

import java.util.List;
import java.util.TreeMap;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-jan-02
 * Time: 21:35:21
 * To change this template use File | Settings | File Templates.
 */
public class NonWindowUICallback implements UICallback {

    @Override
    public void nodeOrSubnodesUpdated(Node node) {
    }

    @Override
    public void noRouteToHost(Node node) {
    }

    @Override
    public void pluginCommunicationReceived(Friend source, String data) {
    }

    public void messageRecieved(int srcGuid, String message) {
        /*@todo: handle this in some good way. balloon should be shown. should add it to ui
        and balloon click should open ui with chat window*/
    }

    @Override
    public void searchHits(int srcGuid, int hops, List<SearchHit> hits) {
    }

    @Override
    public void trace(int level, String message, Exception stackTrace) {
    }

    @Override
    public void handleError(Throwable e, Object source) {
        System.err.println("Error for : " + source + ": ");
        e.printStackTrace();
    }

    @Override
    public void statusMessage(String s) {
        if (!CoreSubsystem.isRunningAsTestSuite()) {
            System.out.println(s);
        }
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
    }

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
    public void statusMessage(String s, boolean b) {
        if (!CoreSubsystem.isRunningAsTestSuite()) {
            System.out.println(s);
        }
    }
}

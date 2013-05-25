package org.alliance.core.plugins;

import org.alliance.core.NeedsUserInteraction;
import org.alliance.core.UICallback;
import org.alliance.core.comm.SearchHit;
import org.alliance.core.node.Friend;
import org.alliance.core.node.Node;

import java.util.List;
import java.util.TreeMap;

/**
 * Used by PlugIns to hook onto the UICallback of core.  
 *
 * User: maciek
 * Date: 2008-jun-06
 * Time: 11:16:30
 * To change this template use File | Settings | File Templates.
 */
public class DoubleUICallback implements UICallback {

    private UICallback first, second;

    public DoubleUICallback(UICallback first, UICallback second) {
        this.first = first;
        this.second = second;
    }

    public UICallback getFirst() {
        return first;
    }

    public UICallback getSecond() {
        return second;
    }

    @Override
    public void nodeOrSubnodesUpdated(Node node) {
        first.nodeOrSubnodesUpdated(node);
        second.nodeOrSubnodesUpdated(node);
    }

    @Override
    public void noRouteToHost(Node node) {
        first.noRouteToHost(node);
        second.noRouteToHost(node);
    }

    @Override
    public void pluginCommunicationReceived(Friend source, String data) {
        first.pluginCommunicationReceived(source, data);
        second.pluginCommunicationReceived(source, data);
    }

    @Override
    public void searchHits(int srcGuid, int hops, List<SearchHit> hits) {
        first.searchHits(srcGuid, hops, hits);
        second.searchHits(srcGuid, hops, hits);
    }

    @Override
    public void trace(int level, String message, Exception stackTrace) {
        first.trace(level, message, stackTrace);
        second.trace(level, message, stackTrace);
    }

    @Override
    public void handleError(Throwable e, Object Source) {
        first.handleError(e, Source);
        second.handleError(e, Source);
    }

    @Override
    public void statusMessage(String s) {
        first.statusMessage(s);
        second.statusMessage(s);
    }

    @Override
    public void statusMessage(String s, boolean b) {
        first.statusMessage(s, b);
        second.statusMessage(s, b);
    }

    @Override
    public void toFront() {
        first.toFront();
        second.toFront();
    }

    @Override
    public void signalFriendAdded(Friend friend) {
        first.signalFriendAdded(friend);
        second.signalFriendAdded(friend);
    }

    @Override
    public boolean isUIVisible() {
        return first.isUIVisible() || second.isUIVisible();
    }

    @Override
    public void logNetworkEvent(String event) {
        first.logNetworkEvent(event);
        second.logNetworkEvent(event);
    }

    @Override
    public void receivedShareBaseList(Friend friend, String[] shareBaseNames) {
        first.receivedShareBaseList(friend, shareBaseNames);
        second.receivedShareBaseList(friend, shareBaseNames);
    }

    @Override
    public void receivedDirectoryListing(Friend friend, int shareBaseIndex, String path, TreeMap<String, Long> fileSize) {
        first.receivedDirectoryListing(friend, shareBaseIndex, path, fileSize);
        second.receivedDirectoryListing(friend, shareBaseIndex, path, fileSize);
    }

    @Override
    public void newUserInteractionQueued(NeedsUserInteraction ui) {
        first.newUserInteractionQueued(ui);
        second.newUserInteractionQueued(ui);
    }

    @Override
    public void firstDownloadEverFinished() {
        first.firstDownloadEverFinished();
        second.firstDownloadEverFinished();
    }

    @Override
    public void callbackRemoved() {
        first.callbackRemoved();
        second.callbackRemoved();
    }
}

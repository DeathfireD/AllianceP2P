package org.alliance.core.comm;

import org.alliance.core.CoreSubsystem;

import java.io.IOException;
import java.nio.ByteBuffer;

import com.stendahls.util.TextUtils;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2005-dec-26
 * Time: 11:56:52
 * To change this template use File | Settings | File Templates.
 */
public abstract class Connection {

    public static enum Direction {

        IN, OUT
    }
    protected NetworkManager netMan;
    protected CoreSubsystem core;
    protected Object key;
    protected Direction direction;
    protected boolean hasWriteInterest;
    protected long bytesSent, bytesReceived;
    protected BandwidthAnalyzer bandwidthIn = new BandwidthAnalyzer(BandwidthAnalyzer.INNER_INVERVAL), bandwidthOut = new BandwidthAnalyzer(BandwidthAnalyzer.INNER_INVERVAL);
    protected String statusString;
    protected boolean connected;

    protected Connection(NetworkManager netMan, Direction direction) {
        this.netMan = netMan;
        this.direction = direction;
        core = netMan.getFriendManager().getCore();
    }

    protected Connection(NetworkManager netMan, Direction direction, Object key) {
        this.netMan = netMan;
        this.direction = direction;
        this.key = key;
        core = netMan.getFriendManager().getCore();
        connected = true;
    }

    public abstract void received(ByteBuffer buf) throws IOException;

    public abstract void readyToSend() throws IOException;

    protected abstract int getConnectionId();

    //can be overridden by connection to perform stuff when connection breaks - used by 
    public void signalConnectionAttemtError() {
    }

    public int getConnectionIdForRemote() {
        return getConnectionId();
    }

    public void bytesReceived(int n) {
        bytesReceived += n;
        bandwidthIn.update(n);
        netMan.bytesReceived(n);
    }

    public void bytesSent(int sent) {
        bytesSent += sent;
        bandwidthOut.update(sent);
        netMan.bytesSent(sent);
    }

    public void setKey(Object key) {
        this.key = key;
    }

    public Direction getDirection() {
        return direction;
    }

    public void init() throws IOException {
    }

    public NetworkManager getNetMan() {
        return netMan;
    }

    public Object getKey() {
        return key;
    }

    @Override
    public String toString() {
        return TextUtils.simplifyClassName(getClass()) + " [" + key + "]";
    }

    /** Close this connection */
    public void close() throws IOException {
        connected = false;
        netMan.closed(this);
        netMan.getSocketFor(this).close();
    }

    public long getBytesSent() {
        return bytesSent;
    }

    public long getBytesReceived() {
        return bytesReceived;
    }

    public boolean hasWriteInterest() {
        return hasWriteInterest;
    }

    public void setHasWriteInterest(boolean hasWriteInterest) {
        this.hasWriteInterest = hasWriteInterest;
    }

    public String getStatusString() {
        return statusString;
    }

    public void setStatusString(String statusString) {
        this.statusString = statusString;
    }

    public BandwidthAnalyzer getBandwidthIn() {
        return bandwidthIn;
    }

    public BandwidthAnalyzer getBandwidthOut() {
        return bandwidthOut;
    }

    public boolean isConnected() {
        return connected;
    }
}

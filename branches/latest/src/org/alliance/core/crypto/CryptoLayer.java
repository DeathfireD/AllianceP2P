package org.alliance.core.crypto;

import org.alliance.core.CoreSubsystem;
import org.alliance.core.comm.Connection;
import org.alliance.core.comm.networklayers.tcpnio.TCPNIONetworkLayer;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-aug-22
 * Time: 16:56:54
 */
public abstract class CryptoLayer {

    protected TCPNIONetworkLayer networkLayer;
    protected CoreSubsystem core;

    public CryptoLayer(CoreSubsystem core) {
        this.core = core;
    }

    public abstract int send(Connection c, ByteBuffer buf) throws IOException;

    public abstract void received(Connection connection, ByteBuffer buf) throws IOException;

    public abstract void readyToSend(Connection connection) throws IOException;

    public abstract void signalInterestToSend(final Connection c);

    public abstract void noInterestToSend(final Connection c);

    public abstract void closed(Connection c);

    //can be overridden by implementing classes
    public void init() throws Exception {
    }

    public void setNetworkLayer(TCPNIONetworkLayer networkLayer) {
        this.networkLayer = networkLayer;
    }

    protected void trace(String s) {
        if (T.t) {
//            System.out.println(core.getFriendManager().getMe().getNickname()+"  "+s);
            T.trace(s);
        }
    }

    protected void debug(String s) {
        if (T.t) {
//            System.out.println(core.getFriendManager().getMe().getNickname()+" "+s);
            T.debug(s);
        }
    }

    protected void info(String s) {
        if (T.t) {
//            System.out.println(core.getFriendManager().getMe().getNickname()+" "+s);
            T.info(s);
        }
    }
}

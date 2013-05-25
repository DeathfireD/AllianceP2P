package org.alliance.core.comm.filetransfers;

import org.alliance.core.comm.AuthenticatedConnection;
import org.alliance.core.comm.NetworkManager;
import org.alliance.core.comm.Packet;
import org.alliance.core.file.hash.Hash;

import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-jan-25
 * Time: 14:02:42
 */
public abstract class TransferConnection extends AuthenticatedConnection {

    public enum Command {

        GET_FD((byte) 1), HASH((byte) 2), GETBLOCK((byte) 3), GRACEFULCLOSE((byte) 4);
        private final byte value;

        Command(byte value) {
            this.value = value;
        }

        public final byte value() {
            return value;
        }
    }

    public enum TransferDirection {

        DOWNLOAD, UPLOAD
    }

    public enum Mode {

        RAW, PACKET
    }
    protected Mode mode;

    protected TransferConnection(NetworkManager netMan, Direction direction) {
        super(netMan, direction);
    }

    protected TransferConnection(NetworkManager netMan, Direction direction, Object key) {
        super(netMan, direction, key);
    }

    protected TransferConnection(NetworkManager netMan, Object key, Direction direction, int userGUID) {
        super(netMan, key, direction, userGUID);
    }

    protected TransferConnection(NetworkManager netMan, Direction direction, int userGUID) {
        super(netMan, direction, userGUID);
    }

    public abstract Hash getRoot();

    protected void sendCommand(Command cmd) throws IOException {
        if (T.t) {
            T.debug("Sending command: " + cmd);
        }
        Packet p = netMan.createPacketForSend();
        p.writeByte(cmd.value());
        send(p);
    }

    protected void switchMode(Mode m) {
        if (T.t) {
            T.trace("Setting mode: " + m);
        }
        mode = m;
    }
}

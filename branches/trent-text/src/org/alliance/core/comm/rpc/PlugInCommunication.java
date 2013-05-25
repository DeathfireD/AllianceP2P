package org.alliance.core.comm.rpc;

import org.alliance.core.comm.Packet;

import java.io.IOException;

/**
 * Can be used by plugins/bots to send data beetwen different installations of Alliance. When doing a plugin make sure
 * to prefix the data-string with something unique for your plugin. Otherwise you might receive communication ment for
 * some other plugin running on this installation of Alliance.
 *
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2005-dec-28
 * Time: 18:42:48
 */
public class PlugInCommunication extends PersistantRPC {

    private String data;

    public PlugInCommunication() {
    }

    public PlugInCommunication(String data) {
        this.data = data;
    }

    @Override
    public void execute(Packet in) throws IOException {
        data = in.readUTF();
        manager.getCore().getUICallback().pluginCommunicationReceived(con.getRemoteFriend(), data);
    }

    @Override
    public Packet serializeTo(Packet p) {
        p.writeUTF(data);
        return p;
    }
}

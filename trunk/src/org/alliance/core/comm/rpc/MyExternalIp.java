package org.alliance.core.comm.rpc;

import org.alliance.core.comm.IpDetection;
import org.alliance.core.comm.Packet;
import org.alliance.core.comm.RPC;

import java.io.IOException;

/**
 * @author Bastvera
 */
public class MyExternalIp extends RPC {

    public MyExternalIp() {
    }

    @Override
    public void execute(Packet in) throws IOException {
        String host = in.readUTF();
        if (!IpDetection.isLan(host, false)) {
            manager.getNetMan().getIpDetection().setLastExternalIp(host);
        }
    }

    @Override
    public Packet serializeTo(Packet p) {
        p.writeUTF(manager.getNetMan().getSocketFor(con).getInetAddress().getHostAddress());
        return p;
    }
}

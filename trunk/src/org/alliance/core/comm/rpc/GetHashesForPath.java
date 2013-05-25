package org.alliance.core.comm.rpc;

import org.alliance.core.comm.Packet;
import org.alliance.core.comm.RPC;

import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2005-dec-21
 * Time: 16:34:22
 * To change this template use File | Settings | File Templates.
 */
public class GetHashesForPath extends RPC {

    private String path;
    private int shareBaseIndex;

    public GetHashesForPath() {
    }

    public GetHashesForPath(int shareBaseIndex, String path) {
        this.shareBaseIndex = shareBaseIndex;
        this.path = path;
    }

    @Override
    public void execute(Packet data) throws IOException {
        shareBaseIndex = data.readInt();
        path = data.readUTF();

        String basePath = core.getShareManager().getBaseByIndex(shareBaseIndex).getPath();
        send(new HashesForPath(path, basePath, core));
    }

    @Override
    public Packet serializeTo(Packet p) {
        p.writeInt(shareBaseIndex);
        p.writeUTF(path);
        return p;
    }
}


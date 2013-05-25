package org.alliance.core.comm.rpc;

import org.alliance.core.comm.Packet;
import org.alliance.core.comm.RPC;

import java.io.IOException;

/**
 *
 * Sent when a friend needs information about the correct ip/port of a common friend of ours.
 *
 * Received when we need info about a friend that we haven't got the correct ip/port to.
 *
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2005-dec-28
 * Time: 18:42:48
 */
/* This is old class left for compatibility*/
public class SearchHits extends RPC {

    public SearchHits() {
    }

    @Override
    public void execute(Packet in) throws IOException {
    }

    @Override
    public Packet serializeTo(Packet p) {
        p.writeLong(-1);
        return p;
    }
}

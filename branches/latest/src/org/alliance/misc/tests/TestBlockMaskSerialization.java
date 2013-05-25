package org.alliance.misc.tests;

import junit.framework.TestCase;
import org.alliance.core.comm.networklayers.tcpnio.NIOPacket;
import org.alliance.core.file.blockstorage.BlockMask;

import java.nio.ByteBuffer;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-feb-02
 * Time: 19:07:31
 * To change this template use File | Settings | File Templates.
 */
public class TestBlockMaskSerialization extends TestCase {

    public void testSerialize() throws Exception {
        for (int i = 0; i < 100; i++) {
            tryOnce();
        }
    }

    private void tryOnce() throws Exception {
        BlockMask bm = new BlockMask();
        for (int i = 0; i < 500; i++) {
            bm.set((int) (Math.random() * 1000));
        }

        NIOPacket p = new NIOPacket(ByteBuffer.allocate(200), false);

        bm.serializeTo(p);
        p.flip();

        BlockMask bm2 = BlockMask.createFrom(p);

        if (bm.length() != bm2.length()) {
            throw new Exception("Length mismatch");
        }

        for (int i = 0; i < bm.length(); i++) {
            if (bm.get(i) != bm2.get(i)) {
                throw new Exception("Mismatch at bit " + i);
            }
        }
    }
}

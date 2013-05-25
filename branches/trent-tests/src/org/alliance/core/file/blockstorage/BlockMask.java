package org.alliance.core.file.blockstorage;

import org.alliance.core.comm.Packet;

import java.io.IOException;
import java.util.BitSet;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-jan-22
 * Time: 19:19:02
 * To change this template use File | Settings | File Templates.
 */
public class BlockMask extends BitSet {

    public BlockMask() {
    }

    public BlockMask(int nbits, boolean allSet) {
        super(nbits);
        if (allSet) {
            set(0, nbits);
        }
    }

    public void serializeTo(Packet p) {
        try {
            int nBits = length();
            int len = nBits / 8;
            if (nBits % 8 != 0) {
                len++;
            }
            p.writeShort(len);
            for (int i = 0; i < len; i++) {
                byte b = 0;
                for (int j = 0; j < 8; j++) {
                    if (get(i * 8 + j)) {
                        b |= 1 << j;
                    }
                }
                p.writeByte(b);
            }
        } catch (Exception e) {
            throw new RuntimeException("Could not serialize BlockMask: " + e);
        }
    }

    public static BlockMask createFrom(Packet data) throws IOException {
        BlockMask bm = new BlockMask();
        int nbytes = data.readUnsignedShort();
        for (int i = 0; i < nbytes; i++) {
            byte b = data.readByte();
            for (int j = 0; j < 8; j++) {
                if ((b & (1 << j)) != 0) {
                    bm.set(i * 8 + j);
                }
            }
        }
        return bm;
    }
}

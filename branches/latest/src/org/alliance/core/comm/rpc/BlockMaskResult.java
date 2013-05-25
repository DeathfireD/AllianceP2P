package org.alliance.core.comm.rpc;

import org.alliance.core.comm.Packet;
import org.alliance.core.comm.RPC;
import org.alliance.core.file.blockstorage.BlockMask;
import org.alliance.core.file.hash.Hash;

import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2005-dec-21
 * Time: 16:34:22
 * To change this template use File | Settings | File Templates.
 */
public class BlockMaskResult extends RPC {

    private BlockMask blockMask;
    private boolean hasAllBits;
    private Hash hash;
    private int nBits;

    public BlockMaskResult() {
        routable = true;
    }

    public BlockMaskResult(Hash hash, boolean hasAllBits, int nBits) {
        this();
        this.hash = hash;
        this.hasAllBits = hasAllBits;
        this.nBits = nBits;
    }

    public BlockMaskResult(Hash hash, BlockMask blockMask) {
        this();
        this.hash = hash;
        this.blockMask = blockMask;
        hasAllBits = false;
    }

    @Override
    public void execute(Packet data) throws IOException {
        hasAllBits = data.readBoolean();
        hash = new Hash();
        data.readArray(hash.array());
        if (!hasAllBits) {
            blockMask = BlockMask.createFrom(data);
        } else {
            blockMask = new BlockMask(data.readUnsignedShort(), true);
        }

        manager.getNetMan().getDownloadManager().blockMaskReceived(fromGuid, hops, hash, blockMask);
    }

    @Override
    public Packet serializeTo(Packet p) {
        p.writeBoolean(hasAllBits);
        p.writeArray(hash.array());
        if (!hasAllBits) {
            blockMask.serializeTo(p);
        } else {
            p.writeShort(nBits);
        }
        return p;
    }

    public BlockMask getBlockMask() {
        return blockMask;
    }

    public boolean isHasAllBits() {
        return hasAllBits;
    }

    public Hash getHash() {
        return hash;
    }
}

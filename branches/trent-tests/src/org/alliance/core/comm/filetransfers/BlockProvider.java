package org.alliance.core.comm.filetransfers;

import org.alliance.core.CoreSubsystem;
import org.alliance.core.file.blockstorage.BlockFile;
import org.alliance.core.file.filedatabase.FileDescriptor;
import org.alliance.core.file.hash.Hash;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-feb-02
 * Time: 16:59:13
 */
public abstract class BlockProvider implements DataProvider {

    protected int blockNumber;
    protected FileDescriptor fd;
    protected int read = 0;
    protected Hash root;
    protected CoreSubsystem core;

    public BlockProvider(int blockNumber, Hash root, CoreSubsystem core) throws IOException {
        this.blockNumber = blockNumber;
        this.root = root;
        this.core = core;

        if (T.t) {
            T.trace("Loading");
        }
        fd = core.getFileManager().getFd(root);
        if (T.t) {
            T.ass(fd != null, "Could not load FD for " + root);
        }

        if (T.t) {
            T.debug("BlockProvider <init> - block " + blockNumber + " from complete file: " + fd);
        }
    }

    protected int prepare(ByteBuffer buf) {
        int bs = BlockFile.getBlockSize(blockNumber, fd.getSize());
//        if(T.t)T.trace("read: "+read+", bs: "+bs+" rem: "+buf.remaining());
        if (buf.remaining() > bs - read) {
            int l = bs - read + buf.position();
//            if(T.t)T.trace("Setting limit to "+l);
            buf.limit(l);
        }
        return bs;
    }
}

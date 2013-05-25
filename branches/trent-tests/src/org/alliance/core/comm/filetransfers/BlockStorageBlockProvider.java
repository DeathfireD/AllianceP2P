package org.alliance.core.comm.filetransfers;

import org.alliance.core.CoreSubsystem;
import org.alliance.core.file.blockstorage.BlockFile;
import org.alliance.core.file.blockstorage.BlockStorage;
import org.alliance.core.file.hash.Hash;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-feb-02
 * Time: 17:04:21
 */
public class BlockStorageBlockProvider extends BlockProvider {

    private BlockFile bf;
    private boolean doneFilling;
    private CompleteFileBlockProvider wrapped;

    public BlockStorageBlockProvider(int blockNumber, Hash root, CoreSubsystem core) throws IOException {
        super(blockNumber, root, core);
        BlockStorage storage = core.getFileManager().getBlockStorageFor(root);
        bf = storage.getBlockFile(root);
        if (T.t) {
            T.ass(bf != null, "Could not find block file when about to send block from block storage " + storage + ", fd: " + fd);
        }
    }

    @Override
    public int fill(ByteBuffer buf) throws IOException {
        if (doneFilling) {
            return -1;
        }

        if (core.getFileManager().containsComplete(root)) {
            if (T.t) {
                T.info("Hmm! We have this file complete now! Should not read from the block storage - this could re-open the file.");
            }
            if (wrapped == null) {
                wrapped = new CompleteFileBlockProvider(blockNumber, root, core);
                wrapped.updateRead(read);
            }
            return wrapped.fill(buf);
        }

        int bs = prepare(buf);
        int n = bf.read(blockNumber, read, buf);
        read += n;

        if (n == -1 || read >= bs) {
            if (T.t) {
                T.ass(read == bs, "read more then needed!");
            }
            if (T.t) {
                T.info("Block successfulyl provided. Rest is in buffer. Used block storage so we're not closing file.");
            }
            //@todo: maybe should decide some way if we need to close the file channel - will probably leak open files..
            doneFilling = true;
        }
        return n;
    }
}

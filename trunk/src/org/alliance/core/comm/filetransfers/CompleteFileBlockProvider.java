package org.alliance.core.comm.filetransfers;

import org.alliance.core.CoreSubsystem;
import static org.alliance.core.CoreSubsystem.BLOCK_SIZE;
import org.alliance.core.file.hash.Hash;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-jan-26
 * Time: 21:15:42
 * To change this template use File | Settings | File Templates.
 */
public class CompleteFileBlockProvider extends BlockProvider {

    protected FileChannel fileChannel;

    public CompleteFileBlockProvider(int blockNumber, Hash root, CoreSubsystem core) throws IOException {
        super(blockNumber, root, core);

        File f = new File(fd.getFullPath());
        if (!f.exists()) {
            throw new FileNotFoundException("Could not find: " + fd + " in complete files!");
        }

        FileInputStream in = new FileInputStream(f);
        fileChannel = in.getChannel();
        fileChannel.position(((long) blockNumber) * BLOCK_SIZE);

        if (T.t) {
            T.ass(fileChannel.isOpen(), "FileChannel closed!");
        }
        if (T.t) {
            T.trace("Ready to start sending block");
        }
    }

    @Override
    public int fill(ByteBuffer buf) throws IOException {
        if (!fileChannel.isOpen()) {
            return -1;
        }

        int bs = prepare(buf);

        int n = fileChannel.read(buf);
        read += n;

        if (n == -1 || read >= bs) {
            if (T.t) {
                T.ass(read == bs, "read more then needed!");
            }
            if (T.t) {
                T.info("Block successfulyl provided. Rest is in buffer. Closing file channel.");
            }
            fileChannel.close();
        }
        return n;
    }

    public void updateRead(int read) throws IOException {
        this.read = read;
        fileChannel.position(((long) blockNumber) * BLOCK_SIZE + read);
    }
}

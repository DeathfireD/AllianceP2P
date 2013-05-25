package org.alliance.misc.tests;

import junit.framework.TestCase;
import org.alliance.core.CoreSubsystem;
import static org.alliance.core.CoreSubsystem.BLOCK_SIZE;
import org.alliance.core.ResourceSingelton;
import org.alliance.core.T;
import org.alliance.core.file.blockstorage.BlockFile;
import org.alliance.core.file.blockstorage.BlockStorage;
import org.alliance.core.file.blockstorage.CacheStorage;
import org.alliance.core.file.filedatabase.FileDescriptor;
import org.alliance.core.file.filedatabase.FileType;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-jan-22
 * Time: 16:53:15
 * To change this template use File | Settings | File Templates.
 */
public class TestBlockStorage extends TestCase {

   /* private CoreSubsystem core;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
//        new TraceWindow();
        core = new CoreSubsystem();
        core.init(ResourceSingelton.getRl(), "testsuite/settings/maciek.xml");
    }

    public void testMain() throws Exception {
        BlockStorage bs = new CacheStorage("test/cache/_incomplete_", "test/cache", core);

        FileDescriptor fda[] = core.getFileManager().search("mp3", 5, FileType.EVERYTHING);

        System.out.println(" ****** Moving 5 files through block storage ...");

        for (int f = 0; f < 5 & f < fda.length; f++) {
            FileDescriptor fd = fda[f];
            fake(bs, fd, false);
        }

        System.out.println(" ****** Moving 1 half file ...");
        fake(bs, fda[6], true);

        System.out.println(" ****** Trying to move other halv ...");
        bs.shutdown();
        bs = new CacheStorage("test/cache/_incomplete_", "test/cache", core);
        fake(bs, fda[6], false);

        System.out.println("press any key to continure");
    }

    private void fake(BlockStorage bs, FileDescriptor fd, boolean breakInMiddle) throws IOException {
        System.out.println("Faking a download of " + fd + "...");

        FileInputStream in = new FileInputStream(fd.getFullPath());
        System.out.println("size: " + fd.getSize());

        int nBlocks = (int) (fd.getSize() / BLOCK_SIZE);
        if (fd.getSize() % BLOCK_SIZE != 0) {
            nBlocks++;
        }

        for (int i = 0; i < nBlocks; i++) {
            byte buf[] = new byte[323333];
            int off = 0;
            System.out.println("block: " + i);
            for (;;) {
                int read = BLOCK_SIZE - off;
                if (read <= 0) {
                    break;
                }
                if (read > buf.length) {
                    read = buf.length;
                }
                read = in.read(buf, 0, read);
                if (read == -1) {
                    System.out.println("Got -1 in read. Should be done now.");
                    break;
                }
                if (read == 0) {
                    continue;
                }

                //convert array to bytebyffer - this is temporary - nio will be used all over later
                ByteBuffer bbuf = ByteBuffer.allocate(read);
                bbuf.put(buf, 0, read);
                bbuf.flip();

                if (!bs.containsBlock(fd.getRootHash(), i)) {
                    int r = bs.saveSlice(fd.getRootHash(), i, off, bbuf, fd);
                    if (T.t) {
                        T.ass(r == read, "Mismatch");
                    }
                    if (off + r >= BlockFile.getBlockSize(i, fd.getSize())) {
                        if (T.t) {
                            T.ass(off + r <= BlockFile.getBlockSize(i, fd.getSize()), "Wrote outside!");
                        }
                        break;
                    }
                } else {
                    System.out.println("Skipping because block already exists.");
                }
                off += read;
                if (breakInMiddle && (i > nBlocks / 2 || i > 2)) {
                    return;
                }
            }
        }
        System.out.println("all blocks should be done");
    }*/
}

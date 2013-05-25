package org.alliance.core.comm.filetransfers;

import org.alliance.core.file.blockstorage.BlockFile;
import org.alliance.core.file.blockstorage.BlockStorage;
import org.alliance.core.file.filedatabase.FileDescriptor;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-jan-26
 * Time: 21:56:49
 * To change this template use File | Settings | File Templates.
 */
public class BlockConsumer implements DataConsumer {

    private FileDescriptor fd;
    private int blockNumber;
    private BlockStorage storage;
    private DownloadConnection dc;
    private int sliceOffset = 0;
    private ByteBuffer saveBuffer;

    public BlockConsumer(DownloadConnection c, int blockNumber, BlockStorage storage) throws IOException {
        this.dc = c;
        this.fd = dc.getDownload().getFd();
        this.blockNumber = blockNumber;
        this.storage = storage;

        saveBuffer = ByteBuffer.allocate(storage.getCore().getSettings().getInternal().getDiscwritebuffer());

        //@todo: this is not optimal. We treat block downloads in consumerque the same way
        // as block downloads actually beeing downloaded. This is here to signal that this block is in
        //progress - although it's actually only in the consumerQue (probably)
        storage.saveSlice(fd.getRootHash(), blockNumber, sliceOffset, ByteBuffer.allocate(0), fd);
    }
    private boolean firstConsume = true;

    @Override
    public void consume(ByteBuffer buf) throws IOException {
        int saveBufferPos = saveBuffer == null ? 0 : saveBuffer.position();
        if (sliceOffset + buf.remaining() + saveBufferPos > BlockFile.getBlockSize(blockNumber, fd.getSize())) {
            if (T.t) {
                T.info("On the edge between two blocks. " + sliceOffset + ", " + buf.remaining() + ", " + saveBufferPos);
            }
            //there's more then we need. This happens when the the chunk of data sent in crossing two blocks
            ByteBuffer buf2 = ByteBuffer.allocate(
                    BlockFile.getBlockSize(blockNumber, fd.getSize()) - (sliceOffset + saveBufferPos));
            int oldLimit = buf.limit();
            buf.limit(buf.position() + buf2.remaining());
            buf2.put(buf);
            buf2.flip();
            buf.limit(oldLimit);
            buf.compact();
            buf.flip(); //make the buffer ready for the next consumer
            buf = buf2;
        }

        if (dc.getDownload().isInvalid() || dc.getDownload().isComplete() || dc.getDownload().isBlockComplete(blockNumber)) {
            if (saveBuffer != null) {
                //flush anything that might be in the saveBuffer
                sliceOffset += saveBuffer.position();
                saveBuffer = null;
            }
            sliceOffset += buf.remaining();
            buf.position(buf.limit()); //make it look like we read the bytes

            if (sliceOffset >= BlockFile.getBlockSize(blockNumber, fd.getSize())) {
                blockComplete();
            }
            return;
        }

        dc.getDownload().addBytesComplete(buf.remaining());

        if (buf.remaining() > saveBuffer.remaining()) {
            flush();
        }
        saveBuffer.put(buf);
        if (firstConsume ||
                saveBuffer.position() + sliceOffset >= BlockFile.getBlockSize(blockNumber, fd.getSize())) {
            flush();
            firstConsume = false;
        }
    }

    private void flush() throws IOException {
        if (dc.getDownload().isInvalid()) {
            if (T.t) {
                T.info("Invalidated download. Flushing data from download. Waiting to gracefully close connection.");
            }
            saveBuffer.clear();
        } else {
            saveBuffer.flip();

            sliceOffset += storage.saveSlice(fd.getRootHash(), blockNumber, sliceOffset, saveBuffer, fd);
            saveBuffer.compact();
            if (sliceOffset >= BlockFile.getBlockSize(blockNumber, fd.getSize())) {
                blockComplete();
            }
        }
    }
    private boolean performedBlockComplete;

    private void blockComplete() throws IOException {
        if (performedBlockComplete) {
            return;
        }
        performedBlockComplete = true;

        if (T.t) {
            T.ass(sliceOffset <= BlockFile.getBlockSize(blockNumber, fd.getSize()), "Wrote outside of block!");
        }

        if (dc.getDownload().checkIfDownloadIsComplete() || dc.getDownload().isInvalid()) {
            if (dc.getDownload().checkIfDownloadIsComplete()) {
                if (T.t) {
                    T.info("Yay. File downloaded! Closing download connection.");
                }
            } else if (dc.getDownload().isInvalid()) {
                if (T.t) {
                    T.info("Download invalidated. Closing download connection.");
                }
            }
            dc.blockDownloadComplete(blockNumber);
            dc.sendGracefulClose();
        } else {
            if (T.t) {
                T.info("Block " + blockNumber + " from " + dc + " downloaded succesfully!");
            }
            dc.blockDownloadComplete(blockNumber);
            dc.startDownloadingBlock();
        }
    }
}

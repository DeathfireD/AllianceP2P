package org.alliance.core.file.blockstorage;

import com.stendahls.nif.util.SimpleTimer;
import com.stendahls.util.TextUtils;
import org.alliance.core.CoreSubsystem;
import static org.alliance.core.CoreSubsystem.BLOCK_SIZE;
import static org.alliance.core.CoreSubsystem.GB;
import org.alliance.core.file.filedatabase.FileDescriptor;
import org.alliance.core.file.hash.Hash;
import org.alliance.core.file.hash.Tiger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.BitSet;
import java.util.HashMap;

/**
 *
 * Contains information about an incomplete file. What block is has (and in what order) and what the FileDescriptor
 * is for this file.
 *
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-jan-20
 * Time: 13:04:46
 */
public final class BlockFile {

    private static final int DIRECTORY_MAGIC = 0xbaceface;
    private final FileDescriptor fd;
    private final int blockOffsets[]; //-1 means no offset yet. Multiply offset value by BLOCK_SIZE to get actual offset
    private BlockMask blockMask = new BlockMask(); //what blocks are completed?
    private RandomAccessFile raf;
    private BlockStorage parent;
    private HashMap<Integer, Integer> bytesCompleteForBlock = new HashMap<Integer, Integer>();

    public BlockFile(FileDescriptor fd, BlockStorage parent) {
        this.fd = fd;
        this.parent = parent;
        blockOffsets = new int[getNumberOfBlocks()];
        for (int i = 0; i < blockOffsets.length; i++) {
            blockOffsets[i] = -1;
        }
        if (fd.getSize() > 30 * GB) {
            //a limit becasue blockOffsets are saves as signed shorts. No more then around 32000 blocks is supported
            throw new RuntimeException("File seems to be larger then 30GB. This system does not handle files over 30GB.");
        }
    }

    private int getNumberOfBlocks() {
        if (blockOffsets == null) {
            return getNumberOfBlockForSize(fd.getSize());
        } else {
            return blockOffsets.length;
        }
    }

    public static int getNumberOfBlockForSize(long size) {
        int n = (int) (size / BLOCK_SIZE);
        if (size % BLOCK_SIZE > 0) {
            n++;
        }
        return n;
    }

    public void blockCompleted(int blockNumber) {
        bytesCompleteForBlock.remove(blockNumber);
        blockMask.set(blockNumber);
    }

    private long getOffset(int blockNumber) {
        return ((long) blockOffsets[blockNumber]) * BLOCK_SIZE;
    }

    private void startBlock(int blockNumber) {
        if (parent.isSequential()) {
            blockOpened(blockNumber, blockNumber);
        } else {
            blockOpened(blockNumber, getNumberOfBlocksStartedOrComplete());
        }
    }

    public boolean isBlockComplete(int blockNumber) {
        return blockMask.get(blockNumber);
    }

    public BitSet getBlocksInProgress() {
        BitSet bs = new BitSet();
        for (int i = 0; i < blockOffsets.length; i++) {
            if (isBlockStartedOrComplete(i) && !isBlockComplete(i)) {
                bs.set(i);
            }
        }
        return bs;
    }

    public int getNumberOfBlocksStartedOrComplete() {
        int n = 0;
        for (int i = 0; i < blockOffsets.length; i++) {
            if (isBlockStartedOrComplete(i)) {
                n++;
            }
        }
        return n;
    }

    public int getNumberOfBlocksComplete() {
        int n = 0;
        for (int i = 0; i < blockOffsets.length; i++) {
            if (isBlockComplete(i)) {
                n++;
            }
        }
        return n;
    }

    public boolean isBlockStartedOrComplete(int blockNumber) {
        return blockOffsets[blockNumber] != -1;
    }

    public void blockCorrupted(int blockNumber) {
        if (T.t) {
            T.info("Block was downloaded but appears to be corrupt - marking as need to be downloaded.");
        }
        blockOffsets[blockNumber] = -1;
    }

    public boolean isComplete() {
        for (int i = 0; i < getNumberOfBlocks(); i++) {
            if (!blockMask.get(i)) {
                return false;
            }
        }
        return true;
    }

    /**
     *
     * @param blockNumber First block in file has blockNUmber 0. Last block has blockNUmber fileSize/BLOCKSIZE(+1 if fileSize%BLOCKSIZE != 0)
     * @param offset 0 means first block in storage, 1 means second BLOCK_SIZE block in storage
     */
    private void blockOpened(int blockNumber, int offset) {
        blockOffsets[blockNumber] = offset;
    }

    public void serializeTo(OutputStream o) throws IOException {
        ObjectOutputStream out = new ObjectOutputStream(o);
        out.writeInt(DIRECTORY_MAGIC);
        fd.serializeTo(out);
        for (int s : blockOffsets) {
            out.writeShort(s);
        }
        out.writeObject(blockMask);
    }

    public static BlockFile createFrom(BlockStorage parent, InputStream is, CoreSubsystem core) throws IOException {
        ObjectInputStream in = new ObjectInputStream(is);
        if (in.readInt() != DIRECTORY_MAGIC) {
            throw new IOException("Corrupt block file!");
        }
        FileDescriptor fd = FileDescriptor.createFrom(in, core);
        if (fd == null) {
            return null;
        }
        BlockFile bf = new BlockFile(fd, parent);
        for (int i = 0; i < bf.blockOffsets.length; i++) {
            bf.blockOffsets[i] = in.readShort();
        }
        try {
            bf.blockMask = (BlockMask) in.readObject();
        } catch (ClassNotFoundException e) {
            throw new IOException(e.toString());
        }
        if (T.t) {
            T.ass(is.available() <= 0, "File corrupt - data at end of file: " + in.available());
        }
        return bf;
    }

    public synchronized void save() throws IOException {
        String s = parent.getStoragePath() + "/" + fd.getRootHash().getRepresentation() + ".dir";
        if (T.t) {
            T.debug("Saving " + s);
        }
        SimpleTimer st = new SimpleTimer();
        FileOutputStream out = new FileOutputStream(s);
        serializeTo(out);
        out.flush();
        out.close();
        if (raf != null) {
            raf.getFD().sync();
        }
        if (T.t) {
            T.trace("Saved in " + st.getTime() + ".");
        }
    }

    public static BlockFile loadFrom(BlockStorage parent, Hash root, CoreSubsystem core) throws IOException {
        try {
            String s = TextUtils.makeSurePathIsMultiplatform(parent.getStoragePath() + "/" + root.getRepresentation() + ".dir");
            if (T.t) {
                T.debug("Loading " + s);
            }
            FileInputStream in = new FileInputStream(s);
            BlockFile bf = createFrom(parent, in, core);
            in.close();
            return bf;
        } catch (FileNotFoundException e) {
            if (T.t) {
                T.trace("Could not find BlockFile for root: " + root);
            }
            return null;
        }
    }

    public FileDescriptor getFd() {
        return fd;
    }

    public boolean isOpen() {
        return raf != null;
    }

    public void open() throws IOException {
        if (T.t) {
            T.ass(!isOpen(), "BlockFile already open!");
        }
        if (T.t) {
            T.info("Opening RAF for " + this + " instance hashcode: " + hashCode());
        }
        raf = new RandomAccessFile(createDatFile(), "rw");
    }

    private File createDatFile() {
        return new File(parent.getStoragePath() + "/" + fd.getRootHash().getRepresentation() + ".dat");
    }

    public void assureOpen() throws IOException {
        if (!isOpen()) {
            open();
        }
    }

    /**
     * @return Number of bytes written, or -1 if the block is complete
     * @throws IOException
     */
    public int write(int blockNumber, int sliceOffset, ByteBuffer slice) throws IOException {
        if (T.t) {
            T.ass(isOpen(), "File not open!");
        }
        if (!isBlockStartedOrComplete(blockNumber)) {
            startBlock(blockNumber);
        }

        raf.seek(getOffset(blockNumber) + sliceOffset);
        byte buf[] = new byte[slice.remaining()];
        slice.get(buf);
        if (T.t) {
            T.ass(sliceOffset + buf.length <= getBlockSize(blockNumber), "Writing outside of block!!! " + (sliceOffset + buf.length) + " - " + getBlockSize(blockNumber));
        }
        raf.write(buf);

        bytesCompleteForBlock.put(blockNumber, sliceOffset + buf.length);

        return buf.length;
    }

    public int read(int blockNumber, int sliceOffset, ByteBuffer buf) throws IOException {
        if (!isOpen()) {
            open();
        }
        if (T.t) {
            T.ass(isOpen(), "File not open!");
        }
        if (T.t) {
            T.ass(isBlockComplete(blockNumber), "Block is not complete and we're trying to send it to a upload!");
        }
        raf.seek(getOffset(blockNumber) + sliceOffset);
        return raf.getChannel().read(buf);
    }

    public Hash calculateHash(int blockNumber) throws IOException {
        byte buf[] = new byte[getBlockSize(blockNumber)];
        raf.seek(getOffset(blockNumber));
        int r = raf.read(buf);
        if (T.t) {
            T.ass(r == buf.length, "wtf");
        }
        Tiger t = new Tiger();
        t.update(buf);
        return new Hash(t.digest());
    }

    public int getBlockSize(int blockNumber) {
        if (blockNumber < getNumberOfBlocks() - 1) {
            return BLOCK_SIZE;
        }
        int s = (int) (fd.getSize() % BLOCK_SIZE);
        if (s == 0) {
            s = BLOCK_SIZE;
        }
        return s;
    }

    public static int getBlockSize(int blockNumber, long size) {
        if (blockNumber < getNumberOfBlockForSize(size) - 1) {
            return BLOCK_SIZE;
        }
        int s = (int) (size % BLOCK_SIZE);
        if (s == 0) {
            s = BLOCK_SIZE;
        }
        return s;
    }

    public void moveToComplete(String directory) throws Exception {
        if (T.t) {
            T.info("Defragmenting file and moving to " + directory);
        }
        if (T.t) {
            T.ass(isComplete(), "File not complete and we're going for a move!");
        }

        File file = new File(directory + "/" + fd.getSubpath());
        if (file.exists()) {
            file = createUniqueFilename(file);
            fd.setSubpath(fd.createSubpath(file.toString()));
        }
        file.getParentFile().mkdirs();

        if (parent.isSequential()) {
            close();
            if (!createDatFile().renameTo(file)) {
                throw new Exception("Could not rename file " + createDatFile() + " to " + file + "!");
            }
        } else {
            defragmentTo(file);
            close();
        }
        file.setLastModified(fd.getModifiedAt());
    }

    private void defragmentTo(File dest) throws IOException {
        SimpleTimer st = new SimpleTimer();
        File file = new File(dest + ".incomplete");
        FileOutputStream out = new FileOutputStream(file);
        byte buf[] = new byte[BLOCK_SIZE];
        Hash hl[] = new Hash[getNumberOfBlocks()];
        Tiger tiger = new Tiger();
        for (int i = 0; i < getNumberOfBlocks(); i++) {
            if (T.t) {
                T.ass(isOpen(), "File seems to be closed!");
            }
            raf.seek(getOffset(i));
            int read = raf.read(buf, 0, getBlockSize(i));
            if (T.t) {
                T.ass(read == getBlockSize(i), "could not read entire block");
            }
            tiger.reset();
            tiger.update(buf, 0, read);
            hl[i] = new Hash(tiger.digest());
            out.write(buf, 0, read);
        }

        //dest moved. Lets check to root hash
        tiger.reset();
        for (Hash h : hl) {
            tiger.update(h.array());
        }
        Hash root = new Hash(tiger.digest());

        if (!root.equals(fd.getRootHash())) {
            file.delete();
            throw new IOException("Integrity check failed when defragmeting file!");
        }

        out.flush();
        out.close();

        if (!file.renameTo(dest)) {
            throw new IOException("Could not rename from " + file + " to " + dest);
        }
        if (T.t) {
            T.info("Defragmented and verified integrity in " + st.getTime() + ".");
        }
    }

    private File createUniqueFilename(File dest) {
        return createUniqueFilename(dest, 1);
    }

    private File createUniqueFilename(File dest, int n) {
        File res;
        String s = dest.toString();
        int i = s.indexOf('.');
        if (i == -1) {
            res = new File(s + " (" + n + ")");
        } else {
            res = new File(s.substring(0, i) + " (" + n + ")" + s.substring(i));
        }

        if (res.exists()) {
            res = createUniqueFilename(dest, n + 1);
        }
        return res;
    }

    public boolean close() throws IOException {
        if (isOpen()) {
            if (T.t) {
                T.info("Closing RAF: " + this);
            }
            raf.getFD().sync();
            raf.close();
            raf = null;
            return true;
        } else {
            return false;
        }
    }

    public BlockMask getBlockMask() {
        return blockMask;
    }

    @Override
    public String toString() {
        return "BlockFile: " + fd + " - " + blockMask;
    }

    public int getHighestCompleteBlock() {
        int max = 0;
        for (int off : blockOffsets) {
            if (off > max) {
                max = off;
            }
        }
        return max;
    }

    public int getBytesCompleteForBlock(int blockNumber) {
        if (isBlockComplete(blockNumber)) {
            return BLOCK_SIZE;
        }
        Integer integer = bytesCompleteForBlock.get(blockNumber);
        if (integer == null) {
            return 0;
        }
        return integer;
    }
}


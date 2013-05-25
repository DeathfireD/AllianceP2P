package org.alliance.core.file.filedatabase;

import com.stendahls.util.TextUtils;
import org.alliance.core.CoreSubsystem;
import org.alliance.core.SimpleTimer;
import static org.alliance.core.CoreSubsystem.BLOCK_SIZE;
import org.alliance.core.UICallback;
import org.alliance.core.file.blockstorage.BlockFile;
import org.alliance.core.file.hash.Hash;
import org.alliance.core.file.hash.Tiger;
import org.alliance.core.file.share.T;
import org.alliance.core.Language;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-jan-06
 * Time: 16:13:57
 * To change this template use File | Settings | File Templates.
 */
public class FileDescriptor {

    public static class FileModifiedWhileHashingException extends IOException {

        public FileModifiedWhileHashingException() {
        }

        public FileModifiedWhileHashingException(String s) {
            super(s);
        }
    }
    private final static byte VERSION = 1;
    private String basePath;
    private String subPath;
    private long size;
    private Hash rootHash;
    private Hash[] hashList;
    private long modifiedAt;

    public FileDescriptor() {
    }

    public FileDescriptor(String basePath, String subPath, long size, Hash rootHash, Hash[] hashList, long modifiedAt) {
        this.basePath = basePath;
        this.subPath = subPath;
        this.size = size;
        this.rootHash = rootHash;
        this.hashList = hashList;
        this.modifiedAt = modifiedAt;
    }

    /**
     * Creates a new file descriptor from scratch. Including creating hashes.
     * @param basePath 
     * @param file
     * @param hashSpeedInMbPerSecond
     * @param callback
     * @throws IOException
     */
    public FileDescriptor(String basePath, File file, int hashSpeedInMbPerSecond, UICallback callback) throws IOException {
        basePath = TextUtils.makeSurePathIsMultiplatform(basePath);

        byte buf[] = new byte[BLOCK_SIZE];

        if (T.t) {
            T.trace("Hashing " + file + " ...");
        }
        long modifiedAtBeforeHashing = file.lastModified();

        Tiger tiger = new Tiger();
        FileInputStream in = new FileInputStream(file);
        ArrayList<Hash> hashes = new ArrayList<Hash>();

        SimpleTimer st = new SimpleTimer();

        long startSize = file.length();
        long totalRead = 0;
        int read;
        long startedLastReadAt = System.currentTimeMillis();
        if (callback != null) {
            callback.statusMessage(Language.getLocalizedString(getClass(), "hashing", file.getName()));
        }
        long updateHashMessageTick = System.currentTimeMillis() + 1500; //delay first update with 1500ms
        long startTick = System.currentTimeMillis();
        while ((read = in.read(buf)) == buf.length) {
            //loaded complete block

            tiger.update(buf);
            hashes.add(new Hash(tiger.digest()));
            tiger.reset();

            totalRead += read;

            if (hashSpeedInMbPerSecond > 0) {
                long t = System.currentTimeMillis();
                if (t - startedLastReadAt < 1000 / hashSpeedInMbPerSecond) {
                    try {
                        Thread.sleep(1000 / hashSpeedInMbPerSecond - (t - startedLastReadAt));
                    } catch (InterruptedException e) {
                    }
                }
            }

            startedLastReadAt = System.currentTimeMillis();
            if (callback != null && System.currentTimeMillis() - updateHashMessageTick > 500) {
                String hashLimit = "";
                if (hashSpeedInMbPerSecond > 0) {
                    hashLimit = " " + Language.getLocalizedString(getClass(), "hashlimit", Integer.toString(hashSpeedInMbPerSecond));
                }
                String s = Language.getLocalizedString(getClass(), "hashprogress", file.getName(),
                        Long.toString(totalRead * 100 / file.length()),
                        TextUtils.formatByteSize(totalRead / ((System.currentTimeMillis() - startTick) / 1000)), hashLimit);
                callback.statusMessage(s);
                updateHashMessageTick = System.currentTimeMillis();
            }
            if (startSize != file.length()) {
                throw new FileModifiedWhileHashingException("Inconsistent file size! File was probably written to.");
            }
            if (modifiedAtBeforeHashing != file.lastModified()) {
                throw new FileModifiedWhileHashingException("File modified while hashing!");
            }
        }

        if (read != -1) {
            totalRead += read;
            if (totalRead != file.length()) {
                throw new IOException("Inconsistent file size in read!");
            }
            tiger.update(buf, 0, read);
            hashes.add(new Hash(tiger.digest()));
        }

        //Get root hash from all hashes
        tiger.reset();
        for (Hash h : hashes) {
            tiger.update(h.array());
        }

        rootHash = new Hash(tiger.digest());
        hashList = hashes.toArray(new Hash[hashes.size()]);
        this.basePath = basePath;
        size = file.length();
        subPath = createSubPath(file.getPath());
        modifiedAt = file.lastModified();
        in.close();

        if (T.t) {
            T.debug("Hashed " + TextUtils.formatNumber("" + totalRead) + " bytes in " + st.getTime() + ". Hash list contains " + hashes.size() + " hashes.");
        }
    }

    public String createSubPath(String path) throws IOException {
        path = TextUtils.makeSurePathIsMultiplatform(path);
        if (!path.startsWith(basePath)) {
            if (T.t) {
                T.error("Path doesn't start with correct base! " + path + " " + basePath);
            }
            throw new IOException("Internal error while hashing file.");
        }
        return path.substring(basePath.length() + 1);
    }

    public String getSubPath() {
        return subPath;
    }

    public long getSize() {
        return size;
    }

    public Hash getRootHash() {
        return rootHash;
    }

    public void setRootHash(Hash rootHash) {
        this.rootHash = rootHash;
    }

    public Hash[] getHashList() {
        return hashList;
    }

    public String getBasePath() {
        return basePath;
    }

    public void serializeTo(OutputStream o) throws IOException {
        serializeTo(o, false);
    }

    public void serializeTo(OutputStream o, boolean noBasePath) throws IOException {
        DataOutputStream out;
        if (o instanceof DataOutputStream) {
            out = (DataOutputStream) o;
        } else {
            out = new DataOutputStream(o);
        }
        subPath = TextUtils.makeSurePathIsMultiplatform(subPath);

        out.writeByte(VERSION);
        out.writeUTF(subPath);
        out.writeLong(size);
        out.write(rootHash.array());
        out.writeShort(hashList.length);
        for (Hash h : hashList) {
            out.write(h.array());
        }
        if (noBasePath) {
            out.writeUTF("");
        } else {
            out.writeUTF(basePath);
        }
        out.writeLong(modifiedAt);
    }

    public static FileDescriptor createFrom(InputStream is, CoreSubsystem core) throws IOException {
        if (is == null) {
            return null;
        }

        FileDescriptor fd = new FileDescriptor();
        DataInputStream in = new DataInputStream(is);

        if (in.readByte() != VERSION) {
            if (T.t) {
                T.warn("Incorrect version for file descriptor. Ignoring.");
            }
            return null;
        }
        fd.subPath = TextUtils.makeSurePathIsMultiplatform(in.readUTF());
        fd.size = in.readLong();
        fd.rootHash = Hash.createFrom(in);

        int n = in.readUnsignedShort();
        fd.hashList = new Hash[n];
        for (int i = 0; i < n; i++) {
            fd.hashList[i] = Hash.createFrom(in);
        }

        fd.basePath = in.readUTF();

        fd.modifiedAt = in.readLong();

        if (fd.basePath.length() > 0 && !new File(fd.basePath).exists()) {
            if (T.t) {
                T.warn("Base path " + fd.basePath + " is not existant. For File " + fd.subPath + " - have to throw it way.");
            }
         
            return null;
        }
      
        return fd;
    }

    @Override
    public String toString() {
        return "FileDescriptor [" + basePath + ", " + subPath + ", " + size + ", " + rootHash + "]";
    }

    public String getFullPath() {
        return basePath + "/" + subPath;
    }

    public Object getSubHash(int blockNumber) {
        return hashList[blockNumber];
    }

    public int getNumberOfBlocks() {
        return BlockFile.getNumberOfBlockForSize(getSize());
    }

    public void setBasePath(String basePath) {
        this.basePath = TextUtils.makeSurePathIsMultiplatform(basePath);
    }

    public String getFilename() {
        String s = getSubPath();
        int i = s.lastIndexOf('/');
        if (i != -1) {
            return s.substring(i + 1);
        } else {
            return s;
        }
    }

    public void setSubPath(String subPath) {
        this.subPath = subPath;
    }

    public long getModifiedAt() {
        return modifiedAt;
    }

    public void setModifiedAt(long modifiedAt) {
        this.modifiedAt = modifiedAt;
    }

    public void updateModifiedAt() {
        setModifiedAt(new File(getFullPath()).lastModified());
    }

    /**
     * When a file is downloaded there's no point in having the entire subpath (for example apps/free/waste/waste.zip) it's better to simplify it to waste/waste.zip - one direcroty and then the file
     */
    public void simplifySubpath() {
        while (countSeparators(subPath) > 1) {
            subPath = subPath.substring(TextUtils.makeSurePathIsMultiplatform(subPath).indexOf('/') + 1);
        }
    }

    private int countSeparators(String subpath) {
        String s = TextUtils.makeSurePathIsMultiplatform(subpath);
        int n = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == '/') {
                n++;
            }
        }
        return n;
    }
}

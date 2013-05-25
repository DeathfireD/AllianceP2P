package org.alliance.core.comm.filetransfers;

import org.alliance.core.file.filedatabase.FileDescriptor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-jan-26
 * Time: 21:15:42
 * To change this template use File | Settings | File Templates.
 */
public class FileDescriptorProvider implements DataProvider {

    private byte[] fd;
    private int index = 0;
    private boolean firstFill = true;

    public FileDescriptorProvider(FileDescriptor fd) throws IOException {
        if (T.t) {
            T.debug("Getting ready to send a FD: " + fd);
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        fd.serializeTo(out, true);
        out.flush();
        this.fd = out.toByteArray();
    }

    @Override
    public int fill(ByteBuffer buf) {
        if (firstFill) {
            buf.putInt(fd.length);
            firstFill = false;
        }
        int len = fd.length - index;
        if (T.t) {
            T.trace("len: " + len);
        }
        if (len <= 0) {
            if (T.t) {
                T.trace("Done providing file descriptor");
            }
            return -1;
        }
        if (buf.remaining() < len) {
            len = buf.remaining();
        }
        buf.put(fd, index, len);
        index += len;
        if (T.t) {
            T.trace("Provided with " + len + " bytes of FD");
        }
        return len;
    }
}

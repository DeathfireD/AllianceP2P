package org.alliance.core.comm.filetransfers;

import org.alliance.core.comm.Packet;
import org.alliance.core.file.filedatabase.FileDescriptor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-jan-26
 * Time: 21:56:49
 * To change this template use File | Settings | File Templates.
 */
public class FileDescriptorConsumer implements DataConsumer {

    private Download download;
    private int length = -1;
    private ByteBuffer completeFD;
    private ByteBuffer lengthBuf = ByteBuffer.allocate(4);
    private DownloadConnection dc;

    public FileDescriptorConsumer(Download download, DownloadConnection dc) {
        this.download = download;
        this.dc = dc;
    }

    @Override
    public void consume(ByteBuffer buf) throws IOException {
        if (length == -1) {
            if (T.t) {
                T.trace("Waiting for length");
            }
            while (buf.remaining() > 0 && lengthBuf.remaining() > 0) {
                lengthBuf.put(buf.get());
            }
            if (lengthBuf.remaining() > 0) {
                return; //wait for four bytes
            }
            lengthBuf.flip();
            length = lengthBuf.getInt();
            if (T.t) {
                T.debug("FD Length: " + length + " -  Receiving FD.");
            }
            completeFD = ByteBuffer.allocate(length);
        }

        if (T.t) {
            T.trace("Received " + buf.remaining() + " bytes - putting into FD array");
        }
        completeFD.put(buf);

        if (!completeFD.hasRemaining()) {
            if (T.t) {
                T.info("Received FD succesfully! Starting download for real.");
            }

            if (T.t) {
                T.trace("Creating FD");
            }
            completeFD.flip();
            FileDescriptor fd = FileDescriptor.createFrom(new InputStream() {

                @Override
                public int read() throws IOException {
                    return Packet.readUnsignedByte(completeFD.get());
                }

                @Override
                public int read(byte b[], int off, int len) throws IOException {
                    completeFD.get(b, off, len);
                    return len;
                }
            }, download.getManager().getCore());
            //todo: not pretty but will have to do for now
            if (download.getAuxInfoFilename().trim().length() > 0 && !"Link from chat".equals(download.getAuxInfoFilename())) {
                fd.setSubpath(download.getAuxInfoFilename());
            } else {
                fd.simplifySubpath();
            }
            if (T.t) {
                T.trace("Signaling we've got " + fd);
            }
            dc.fileDescriptorReceived(); //has to call this first so that the next line understands it and starts a download for our DownloadCOnnection
            download.fileDescriptorReceived(dc, fd);
        }
    }
}

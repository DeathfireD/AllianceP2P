package org.alliance.core.crypto.cryptolayers;

import org.alliance.core.CoreSubsystem;
import org.alliance.core.comm.Connection;
import org.alliance.core.comm.upnp.ReverseConnection;
import org.alliance.core.comm.filetransfers.TransferConnection;
import org.alliance.core.crypto.CryptoLayer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;

/**
 *
 *
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-aug-22
 * Time: 14:22:13
 */
public abstract class BufferedCryptoLayer extends CryptoLayer {

    private ByteBuffer bufferIn;
    private HashMap<Object, ConnectionData> connectionData = new HashMap<Object, ConnectionData>();

    protected class ConnectionData {

        public ByteBuffer encryptionBuffer;
        public boolean connectionWantsToSendData;

        public ConnectionData(int approxBufferSize) {
            if (org.alliance.core.crypto.T.t) {
                debug("Creating new enryptionBuffer");
            }
            encryptionBuffer = ByteBuffer.allocate(approxBufferSize * 12 / 10);
        }
    }

    public BufferedCryptoLayer(CoreSubsystem core) {
        super(core);

        //a bit shady..  should have a general way of figuring out the largest buffer needed. Could be dynamic too.
        //there's only one of this for an Alliance instance so it's ok
        bufferIn = ByteBuffer.allocate(core.getSettings().getInternal().getSocketsendbuffer());
    }

    public abstract int encrypt(Connection c, ByteBuffer src, ByteBuffer dst) throws IOException;

    public abstract void decrypt(Connection c, ByteBuffer src, ByteBuffer dst) throws IOException;

    @Override
    public int send(Connection c, ByteBuffer buf) throws IOException {
        int bytesToSend = buf.remaining();
        if (T.t) {
            trace("Trying to encrypt " + bytesToSend + " bytes from source to encryption buffer");
        }

        ConnectionData d = getConnectionDataFor(c);

        if (bytesToSend > d.encryptionBuffer.remaining()) {
            if (T.t) {
                trace("Encryption buffer overflow: wants to send " + bytesToSend + " remaining in buffer: " +
                        d.encryptionBuffer.remaining() + " - pos: " + d.encryptionBuffer.position() + " - wait for it to clear");
            }
            if (bytesToSend > d.encryptionBuffer.capacity()) {
                if (T.t) {
                    T.error("Oh fuck. Wants to send more then our buffer allows. This will start an infinite loop.");
                }
            }
            addSendInterest(c);
            return 0;
        }

        //move data from buf to bufferOut, encrypting it at the same time
        int sent = encrypt(c, buf, d.encryptionBuffer);
        if (T.t) {
            T.trace("Actually encrypted " + sent + "bytes - pos: " + d.encryptionBuffer.position());
        }

        //signal we're interested in sending the data in the encryptionBuffer
        if (d.encryptionBuffer.position() > 0) {
            addSendInterest(c);
        }

        return sent;
    }

    protected ConnectionData getConnectionDataFor(Connection c) {
        ConnectionData d = connectionData.get(c.getKey());
        if (d == null) {
            if (T.t) {
                debug("Creating a ConnectionData for new connection " + c);
            }
            if (core.getSettings().getInternal().getEncryption() == 1) {
                d = new ConnectionData(core.getSettings().getInternal().getSocketsendbuffer());
                connectionData.put(c.getKey(), d);
            } else {
                d = new ConnectionData(c instanceof TransferConnection || c instanceof ReverseConnection ? //@todo: this is such bullshit. Encryption buffers should be dynamic then this parameter would not be needed at all
                        core.getSettings().getInternal().getSocketsendbuffer() : core.getSettings().getInternal().getMaximumAlliancePacketSize());
                connectionData.put(c.getKey(), d);
            }
        }
        return d;
    }

    private void removeDataConnectionFor(Connection c) {
        connectionData.remove(c.getKey());
    }

    public void tryToFlushEncryptionBuffer(Connection c) throws IOException {
        if (T.t) {
            trace("Flushing enryption buffer");
        }
        ConnectionData d = getConnectionDataFor(c);
        ByteBuffer buf = d.encryptionBuffer;
        buf.flip();
        if (T.t) {
            trace("Trying to send " + buf.remaining() + " bytes");
        }
        int sent = networkLayer.send(c.getKey(), buf);
        if (sent == -1) {
            throw new IOException("Connection ended");
        }
        if (T.t) {
            trace("Sent " + sent + " bytes");
        }
        buf.compact();
        if (buf.position() > 0) {
            if (T.t) {
                trace("Still have data in encryptionBuffer - interested to send");
            }
            addSendInterest(c);
        } else if (!d.connectionWantsToSendData) {
            if (T.t) {
                trace("No data in encryptionBuffer and connection does not have anything more to send - not interested in sending");
            }
            removeSendInterest(c);
        }
    }

    @Override
    public void received(Connection connection, ByteBuffer buf) throws IOException {
        if (T.t) {
            trace("Received " + buf.remaining() + " bytes of data. Decrypting and sending to connection.");
        }
        bufferIn.clear();
        decrypt(connection, buf, bufferIn);
        bufferIn.flip();
        if (T.t) {
            T.ass(buf.remaining() == 0, "decrypt method did not read the src buffer clean: " + buf.remaining());
        }
        while (bufferIn.remaining() > 0) {
            int n = bufferIn.remaining();
            connection.received(bufferIn);
            if (n == bufferIn.remaining()) {
                throw new IOException("Connection refuses to read more data from buffer!");
            }
        }
    }

    @Override
    public void readyToSend(Connection connection) throws IOException {
        if (T.t) {
            trace("Connection is ready to send");
        }
        ConnectionData d = getConnectionDataFor(connection);
        if (d.encryptionBuffer.position() > 0) {
            tryToFlushEncryptionBuffer(connection);
        }
        if (d.encryptionBuffer.position() == 0) {
            if (T.t) {
                trace("Nothing in encryption buffer - let connection know that is can send new data.");
            }
            connection.readyToSend();
        }
    }

    protected void addSendInterest(final Connection c) {
        if (T.t) {
            trace("CrytptoLayer interested in sending");
        }
        if (c.hasWriteInterest()) {
        } else {
            networkLayer.invokeLater(new Runnable() {

                @Override
                public void run() {
                    c.setHasWriteInterest(true);
                    networkLayer.addInterestForWrite(c.getKey());
                }
            });
        }
    }

    protected void removeSendInterest(final Connection c) {
        if (T.t) {
            trace("CrytptoLayer NOT interested in sending");
        }
        if (!c.hasWriteInterest()) {
        } else {
            networkLayer.invokeLater(new Runnable() {

                @Override
                public void run() {
                    c.setHasWriteInterest(false);
                    networkLayer.removeInterestForWrite(c.getKey());
                }
            });
        }
    }

    /**
     * Called from connection classes
     * @param c
     */
    @Override
    public void signalInterestToSend(final Connection c) {
        if (T.t) {
            trace("Connection interested in sending");
        }
        ConnectionData d = getConnectionDataFor(c);
        d.connectionWantsToSendData = true;
        addSendInterest(c);
    }

    /**
     * Called from connection classes
     * @param c
     */
    @Override
    public void noInterestToSend(final Connection c) {
        if (T.t) {
            trace("Connection NOT interested in sending");
        }
        ConnectionData d = getConnectionDataFor(c);
        d.connectionWantsToSendData = false;
        if (d.encryptionBuffer.position() == 0) {
            removeSendInterest(c);
        }
    }

    @Override
    public void closed(Connection c) {
        removeDataConnectionFor(c);
    }
}

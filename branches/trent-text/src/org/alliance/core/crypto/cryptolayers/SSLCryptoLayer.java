package org.alliance.core.crypto.cryptolayers;

import org.alliance.core.CoreSubsystem;
import static org.alliance.core.CoreSubsystem.KB;
import org.alliance.core.comm.Connection;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 *
 * Implementation of CryptoLayer for Alliance using the SSLEngine API.
 *
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-aug-22
 * Time: 14:22:13
 */
public class SSLCryptoLayer extends BufferedCryptoLayer {

    private SSLContext sslContext;
    private HashMap<Object, Context> contexts = new HashMap<Object, Context>();
//    private static final String INTERESTING_CHIPHER_SUITES[] = {"TLS_DH_anon_WITH_AES_256_CBC_SHA", "TLS_DH_anon_WITH_AES_128_CBC_SHA"};
    private static final String INTERESTING_CHIPHER_SUITES[] = {"TLS_DH_anon_WITH_AES_128_CBC_SHA"};
    private String ALLOWED_CHIPHER_SUITES[]; //generated at runtime
    private static final int START_SIZE_OF_INCOMING_ENCRYPTED_DATA_BUFFER = 17 * KB; //should calculate this from getPacketBufferSize in SSLEngine API

    private class Context {

        SSLEngine engine;
        ByteBuffer incomingEncryptedData;
    }

    public SSLCryptoLayer(CoreSubsystem core) throws Exception {
        super(core);
    }

    @Override
    public void closed(Connection c) {
        super.closed(c);
        contexts.remove(c.getKey());
    }

    @Override
    public void init() throws Exception {
        if (T.t) {
            T.info("SSLCryptoLayer initializing!");
        }
        //init sslengine
        sslContext = SSLContext.getInstance("TLSv1");
        sslContext.init(null, null, null);

        List<String> supportedChipherSuites = Arrays.asList(sslContext.createSSLEngine().getSupportedCipherSuites());
        ArrayList<String> allowedChipherSuites = new ArrayList<String>();
        if (core.getSettings().getInternal().getChiphersuite() != null && core.getSettings().getInternal().getChiphersuite().trim().length() > 0) {
            if (supportedChipherSuites.contains(core.getSettings().getInternal().getChiphersuite())) {
                if (T.t) {
                    T.debug("Supported user defined chipher suite: " + core.getSettings().getInternal().getChiphersuite());
                }
                allowedChipherSuites.add(core.getSettings().getInternal().getChiphersuite());
            } else {
                if (T.t) {
                    T.debug("User defined chipher suite not supported by java runtime: " + core.getSettings().getInternal().getChiphersuite());
                }
            }
        }

        for (String s : INTERESTING_CHIPHER_SUITES) {
            if (supportedChipherSuites.contains(s)) {
                if (T.t) {
                    T.debug("Supported chipher suite: " + s);
                }
                allowedChipherSuites.add(s);
            }
        }
        ALLOWED_CHIPHER_SUITES = new String[allowedChipherSuites.size()];
        allowedChipherSuites.toArray(ALLOWED_CHIPHER_SUITES);
    }

    @Override
    public int encrypt(Connection c, ByteBuffer src, ByteBuffer dst) throws IOException {
        if (T.t) {
            T.debug("Encrypt");
        }
        SSLEngine e = getContext(c).engine;

        SSLEngineResult r;
        int read = 0;
        do {
            if (T.t) {
                T.trace("Wrapping " + src.remaining() + " bytes");
            }
            r = e.wrap(src, dst);
            read += r.bytesConsumed();
            if (T.t) {
                T.trace("SSLResult: " + r.getStatus() + " " + r.getHandshakeStatus() + " produced: " +
                        r.bytesProduced() + " consumed: " + r.bytesConsumed());
            }
            if (r.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.NEED_TASK) {
                Runnable task;
                while ((task = e.getDelegatedTask()) != null) {
                    if (T.t) {
                        T.trace("Executing delegated task: " + task);
                    }
                    task.run();
                    if (T.t) {
                        T.trace("Task complete.");
                    }
                }
            }
        } while (r.getStatus() == SSLEngineResult.Status.OK &&
                ((r.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING && src.remaining() > 0) ||
                r.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.NEED_WRAP));

        if (r.getStatus() != SSLEngineResult.Status.OK) {
            throw new IOException("Status not ok: " + r.getStatus() + "!");
        }
        if (r.bytesProduced() > 0) {
            if (T.t) {
                T.trace("SSLEngine produced data - add interest to sent it.");
            }
            addSendInterest(c);
        }
        if (r.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.NEED_WRAP) {
            if (T.t) {
                T.debug("Hmm. SSLEngine needs wrap. This is recursive and a bit tricky.");
            }
            encrypt(c, ByteBuffer.allocate(0), dst);
        }

        checkSSLEngineResult(c, r);

        return read;
    }

    @Override
    public void decrypt(Connection c, ByteBuffer src, ByteBuffer dst) throws IOException {
        if (T.t) {
            T.debug("Decrypt");
        }

        Context cx = getContext(c);

        if (cx.incomingEncryptedData != null) {
            if (cx.incomingEncryptedData.remaining() < src.remaining()) {
                cx.incomingEncryptedData = expandBufferAndAppend(cx.incomingEncryptedData, src);
            } else {
                cx.incomingEncryptedData.put(src);
            }
            src = cx.incomingEncryptedData;
            src.flip();
        }

        SSLEngine e = cx.engine;
        SSLEngineResult r;
        boolean forceAnotherUnwrap = false;
        do {
            r = e.unwrap(src, dst);
            forceAnotherUnwrap = false;
            if (T.t) {
                T.trace("SSLResult: " + r.getStatus() + " " + r.getHandshakeStatus() +
                        " produced: " + r.bytesProduced() + " consumed: " + r.bytesConsumed());
            }
            if (T.t) {
                T.trace("Left in src buffer: " + src.remaining());
            }

            if (r.getStatus() == SSLEngineResult.Status.BUFFER_UNDERFLOW) {
                if (cx.incomingEncryptedData == null) {
                    if (T.t) {
                        T.warn("Creating new incoming data buffer");
                    }
                    cx.incomingEncryptedData = ByteBuffer.allocate(src.remaining() >
                            START_SIZE_OF_INCOMING_ENCRYPTED_DATA_BUFFER ? src.remaining() : START_SIZE_OF_INCOMING_ENCRYPTED_DATA_BUFFER);
                }
                if (cx.incomingEncryptedData != src) {
                    if (cx.incomingEncryptedData.remaining() >= src.remaining()) {
                        cx.incomingEncryptedData.put(src);
                    } else {
                        cx.incomingEncryptedData = expandBufferAndAppend(cx.incomingEncryptedData, src);
                    }
                } else {
                    src.compact();
                }
                //nothing more to do. Wait for more data and then try to call unwrap again.
                return;
            }

            if (r.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.NEED_TASK) {
                Runnable task;
                while ((task = e.getDelegatedTask()) != null) {
                    if (T.t) {
                        T.trace("Executing delegated task: " + task);
                    }
                    task.run();
                    if (T.t) {
                        T.trace("Task complete.");
                    }
                }
                forceAnotherUnwrap = true; //force another unwrap after task delegation
/*                if(T.t)T.trace("Doing another unwrap after task delegation");
                r = e.unwrap(src, dst);
                if(T.t)T.trace("SSLResult: "+r.getStatus()+" "+r.getHandshakeStatus()+" produced: "+r.bytesProduced()+" consumed: "+r.bytesConsumed());*/
            }
        } while (forceAnotherUnwrap ||
                (r.getStatus() == SSLEngineResult.Status.OK &&
                ((r.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.NEED_UNWRAP && r.bytesProduced() == 0) || //wants to unwrap more data
                (r.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING && src.remaining() > 0 && r.bytesConsumed() > 0)))); //has more data to decrypt

        if (src == cx.incomingEncryptedData) {
            if (T.t) {
                T.ass(src.remaining() == 0, "unwrap did not read all data!");
            }
            src.clear(); //our buffer has been read and is ready for filling again (if necessary)
        }

        checkSSLEngineResult(c, r);

        if (r.getStatus() != SSLEngineResult.Status.OK) {
            throw new IOException("Status not ok: " + r.getStatus() + "!");
        }
        if (r.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.NEED_WRAP) {
            if (T.t) {
                T.debug("Hmm. SSLEngine needs wrap in decrypt. This is a bit tricky.");
            }
            send(c, ByteBuffer.allocate(0)); //bit shady but should be a safe way to call wrap in a controlled manner
        }
    }

    private ByteBuffer expandBufferAndAppend(ByteBuffer bufferToExpand, ByteBuffer bufferToAppend) {
        if (T.t) {
            T.warn("Need to expand buffer. Capacity before: " + bufferToExpand.capacity() +
                    " remaining: " + bufferToExpand.remaining() + ", need to add: " + bufferToAppend.remaining());
        }
        ByteBuffer old = bufferToExpand;
        old.flip();
        bufferToExpand = ByteBuffer.allocate(old.remaining() + bufferToAppend.remaining() + START_SIZE_OF_INCOMING_ENCRYPTED_DATA_BUFFER);
        bufferToExpand.put(old);
        bufferToExpand.put(bufferToAppend);
        return bufferToExpand;
    }

    private void checkSSLEngineResult(final Connection c, SSLEngineResult r) throws IOException {
        if (r.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.NEED_TASK) {
            throw new IOException("Task should not be needed here.");
        } else if (r.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.NEED_UNWRAP) {
            if (T.t) {
                T.trace("SSLEngine needs upwrap.");
            }
            if (getConnectionDataFor(c).encryptionBuffer.position() == 0) {
                if (T.t) {
                    T.trace("There is nothing in the encyptionBuffer - We're no longer interested in sending data. Wait for data to arrive");
                }
                removeSendInterest(c);
            } else {
                if (T.t) {
                    T.trace("Still stuff in encryption buffer - even tho SSLEngine neews unwrap we're still interested in sending.");
                }
            }
        } else if (r.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.FINISHED) {
            if (T.t) {
                T.trace("Handshaking finished!");
            }
            addSendInterest(c);
        } else if (r.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING) {
            if (T.t) {
                T.trace("Not handshaking. We're all good.");
            }
        }
    }

    private Context getContext(Connection c) throws SSLException {
        Context cx = contexts.get(c.getKey());
        if (cx == null) {
            cx = new Context();
            cx.engine = setupSLLEngine(c);
            contexts.put(c.getKey(), cx);
        }
        return cx;
    }

    private SSLEngine setupSLLEngine(Connection c) throws SSLException {
        if (T.t) {
            T.info("Creating new SSLEngine");
        }
        SSLEngine e = sslContext.createSSLEngine();

        e.setEnabledCipherSuites(ALLOWED_CHIPHER_SUITES);
        e.setUseClientMode(c.getDirection() == Connection.Direction.OUT);
        e.beginHandshake();
        return e;
    }

    public void resendHandshake(Connection c) throws IOException {
        Context cx = getContext(c);
        cx.engine.getSession().invalidate();
        cx.engine.beginHandshake();
    }
}

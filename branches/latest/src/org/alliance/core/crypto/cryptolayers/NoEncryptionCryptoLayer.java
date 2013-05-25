package org.alliance.core.crypto.cryptolayers;

import org.alliance.core.CoreSubsystem;
import org.alliance.core.comm.Connection;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 *
 * Simple encryption layer used for testing
 *
 *
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-aug-22
 * Time: 14:22:13
 */
public class NoEncryptionCryptoLayer extends BufferedCryptoLayer {

    public NoEncryptionCryptoLayer(CoreSubsystem core) {
        super(core);
    }

    @Override
    public void closed(Connection c) {
    }

    @Override
    public int encrypt(Connection c, ByteBuffer src, ByteBuffer dst) throws IOException {
        int r = src.remaining();
        dst.put(src);
        return r;
    }

    @Override
    public void decrypt(Connection c, ByteBuffer src, ByteBuffer dst) throws IOException {
        dst.put(src);
    }
}

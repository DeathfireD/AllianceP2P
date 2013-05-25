package org.alliance.core.crypto.cryptolayers;

import org.alliance.core.CoreSubsystem;
import org.alliance.core.comm.Connection;

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
public class TranslationCryptoLayer extends BufferedCryptoLayer {

    public TranslationCryptoLayer(CoreSubsystem core) {
        super(core);
        if (T.t) {
            T.info("Starting TranslationCryptoLayer");
        }
    }

    @Override
    public int encrypt(Connection c, ByteBuffer src, ByteBuffer dst) {
        int start = dst.position();
        dst.put(src);
        int end = dst.position();
        if (T.t) {
            T.ass(dst.hasArray(), "No array!");
        }
        byte arr[] = dst.array();
        for (int i = start; i < end; i++) {
            arr[i] = arr[i] += 33;
        }
        return end - start;

    }

    @Override
    public void decrypt(Connection c, ByteBuffer src, ByteBuffer dst) {
        int start = dst.position();
        dst.put(src);
        int end = dst.position();
        if (T.t) {
            T.ass(dst.hasArray(), "No array!");
        }
        byte arr[] = dst.array();
        for (int i = start; i < end; i++) {
            arr[i] = arr[i] -= 33;
        }
    }
}

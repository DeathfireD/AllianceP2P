package org.alliance.core.file.hash;

import com.stendahls.util.Base64Encoder;
import org.alliance.core.file.share.T;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.Serializable;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-jan-06
 * Time: 16:15:12
 * To change this template use File | Settings | File Templates.
 */
public class Hash implements Serializable {

    private static final long serialVersionUID = 6722610126499235070L;
    protected transient static final int HASH_SIZE = 24;
    protected byte[] hash;

    public Hash() {
        hash = new byte[HASH_SIZE];
    }

    public Hash(String hash) {
        this(Base64Encoder.fromBase64String(hash));
    }

    public Hash(byte[] hash) {
        if (hash.length != HASH_SIZE) {
            if (T.t) {
                T.error("Incorrect hash size!!!");
            }
        }
        this.hash = hash;
    }

    public byte[] array() {
        return hash;
    }

    /*   public void setHash(byte[] hash) {
    this.hash = hash;
    }*/
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if ((obj == null) || (obj.getClass() != this.getClass())) {
            return false;
        }
        Hash h = (Hash) obj;
        if (h.array().length != hash.length) {
            return false;
        }
        for (int i = 0; i < hash.length; i++) {
            if (hash[i] != h.array()[i]) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        int h = hash[0];
        for (int i = 1; i < hash.length; i++) {
            h ^= hash[i];
        }
        return h;
    }

    @Override
    public String toString() {
        return getRepresentation();
    }

    public String getRepresentation() {
        return Base64Encoder.toBase64SessionKeyString(hash);
    }

    public static Hash createFrom(DataInputStream in) throws IOException {
        Hash h = new Hash();
        int off = 0;
        while (off < HASH_SIZE) {
            int r = in.read(h.hash, off, HASH_SIZE - off);
            if (r <= 0) {
                throw new IOException("Corrupt serialized hash?");
            }
            off += r;
        }
        return h;
    }

    public static Hash createFrom(String base64encoded) {
        return new Hash(Base64Encoder.fromBase64String(base64encoded));
    }
}

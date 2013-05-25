package org.alliance.core.comm;

import org.alliance.core.trace.TraceChannel;
import com.stendahls.util.TextUtils;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 *
 * User: maciek
 * Date: 2005-dec-26
 * Time: 12:33:30
 */
public abstract class Packet {

    public static final boolean tracePacketContents = true & T.t;
    public static TraceChannel tc = new TraceChannel("pack");

    public abstract int getAvailable();

    public abstract int getSize();

    public abstract void setSize(int i);

    public abstract int getPos();

    public abstract void setPos(int pos);

    public abstract void skip(int n);

    public abstract void mark();

    public abstract void reset();

    public abstract byte[] asArray();

    public abstract byte readByte();

    public abstract void writeByte(byte b);

    public abstract int readInt();

    public abstract void writeInt(int i);

    public abstract boolean readBoolean();

    public abstract void writeBoolean(boolean v);

    public abstract void readArray(byte[] arr);

    public abstract void readArray(byte[] arr, int off, int len);

    public abstract void writeArray(byte[] buf);

    public abstract void writeArray(byte[] buf, int off, int len);

    public abstract void writeLong(long l);

    public abstract long readLong();

    public abstract void writeBuffer(ByteBuffer buf);

    public abstract void prepareForSend() throws IOException;

    public abstract void compact();

    public abstract void flip();

    public int readUnsignedShort() {
        int ch1 = readUnsignedByte();
        int ch2 = readUnsignedByte();
        return (ch1 << 8) + ch2;
    }

    public int readUnsignedByte() {
        return readUnsignedByte(readByte());
    }

    public static int readUnsignedByte(byte b) {
        int i = b & 127;
        if (b < 0) {
            i += 128;
        }
        return i;
    }

    public void writeUTF(String str) {

        int strlen = str.length();
        int utflen = 0;
        int c = 0;

        /* use charAt instead of copying String to char array */
        for (int i = 0; i < strlen; i++) {
            c = str.charAt(i);
            if ((c >= 0x0001) && (c <= 0x007F)) {
                utflen++;
            } else if (c > 0x07FF) {
                utflen += 3;
            } else {
                utflen += 2;
            }
        }

        if (utflen > 65535) {
            throw new RuntimeException("encoded string too long: " + utflen + " bytes");
        }

        writeByte((byte) ((utflen >>> 8) & 0xFF));
        writeByte((byte) ((utflen >>> 0) & 0xFF));

        int i = 0;
        for (i = 0; i < strlen; i++) {
            c = str.charAt(i);
            if (!((c >= 0x0001) && (c <= 0x007F))) {
                break;
            }
            writeByte((byte) c);
        }

        for (; i < strlen; i++) {
            c = str.charAt(i);
            if ((c >= 0x0001) && (c <= 0x007F)) {
                writeByte((byte) c);

            } else if (c > 0x07FF) {
                writeByte((byte) (0xE0 | ((c >> 12) & 0x0F)));
                writeByte((byte) (0x80 | ((c >> 6) & 0x3F)));
                writeByte((byte) (0x80 | ((c >> 0) & 0x3F)));
            } else {
                writeByte((byte) (0xC0 | ((c >> 6) & 0x1F)));
                writeByte((byte) (0x80 | ((c >> 0) & 0x3F)));
            }
        }
    }

    public String readUTF() {
        int utflen = readUnsignedShort();
        byte[] bytearr = null;
        char[] chararr = null;
        bytearr = new byte[utflen];
        chararr = new char[utflen];

        int c, char2, char3;
        int count = 0;
        int chararr_count = 0;

        readArray(bytearr); //in.readFully(bytearr, 0, utflen);

        while (count < utflen) {
            c = (int) bytearr[count] & 0xff;
            if (c > 127) {
                break;
            }
            count++;
            chararr[chararr_count++] = (char) c;
        }

        while (count < utflen) {
            c = (int) bytearr[count] & 0xff;
            switch (c >> 4) {
                case 0:
                case 1:
                case 2:
                case 3:
                case 4:
                case 5:
                case 6:
                case 7:
                    /* 0xxxxxxx*/
                    count++;
                    chararr[chararr_count++] = (char) c;
                    break;
                case 12:
                case 13:
                    /* 110x xxxx   10xx xxxx*/
                    count += 2;
                    if (count > utflen) {
                        throw new RuntimeException("malformed input: partial character at end");
                    }
                    char2 = (int) bytearr[count - 1];
                    if ((char2 & 0xC0) != 0x80) {
                        throw new RuntimeException("malformed input around byte " + count);
                    }
                    chararr[chararr_count++] = (char) (((c & 0x1F) << 6)
                            | (char2 & 0x3F));
                    break;
                case 14:
                    /* 1110 xxxx  10xx xxxx  10xx xxxx */
                    count += 3;
                    if (count > utflen) {
                        throw new RuntimeException(
                                "malformed input: partial character at end");
                    }
                    char2 = (int) bytearr[count - 2];
                    char3 = (int) bytearr[count - 1];
                    if (((char2 & 0xC0) != 0x80) || ((char3 & 0xC0) != 0x80)) {
                        throw new RuntimeException(
                                "malformed input around byte " + (count - 1));
                    }
                    chararr[chararr_count++] = (char) (((c & 0x0F) << 12)
                            | ((char2 & 0x3F) << 6)
                            | ((char3 & 0x3F) << 0));
                    break;
                default:
                    /* 10xx xxxx,  1111 xxxx */
                    throw new RuntimeException(
                            "malformed input around byte " + count);
            }
        }
        // The number of chars produced may be less than utflen
        return new String(chararr, 0, chararr_count);
    }

    @Override
    public String toString() {
        return "Packet [size: " + getSize() + "]";
    }

    public void writeShort(int v) {
        writeByte((byte) ((v >>> 8) & 0xFF));
        writeByte((byte) ((v >>> 0) & 0xFF));
    }

    public void print(boolean out) {
        if (tracePacketContents) {
            if (out) {
                tc.trace("--> Outgoing");
            } else {
                tc.trace("<-- Incoming");
            }
            int len;
            if (out) {
                len = getPos();
                setPos(0);
            } else {
                len = getAvailable();
            }
            byte[] buf = new byte[len];
            readArray(buf);
            printBuf(buf);
            if (out) {
                setPos(len);
            } else {
                setPos(0);
            }
        }
    }

    public static void printBuf(byte buf[]) {
        printBuf(buf, buf.length);
    }

    public static void printBuf(byte buf[], int len) {
        if (tracePacketContents) {
            String hex = "";
            String ascii = "";
            for (int i = 0; i < len; i++) {
                int b = readUnsignedByte(buf[i]);
                hex += TextUtils.lcomplete(Integer.toHexString(b).toUpperCase(), 2, '0') + " ";
                ascii += (char) b;
                if ((i + 1) % 16 == 0) {
                    tc.trace(hex + " " + ascii);
                    hex = "";
                    ascii = "";
                }
            }
            if (hex.length() > 0) {
                hex = TextUtils.complete(hex, 47);
                tc.trace(hex + " " + ascii);
            }
            tc.trace("");
        }
    }
}

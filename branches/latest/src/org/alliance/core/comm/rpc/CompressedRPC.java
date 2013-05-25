package org.alliance.core.comm.rpc;

import org.alliance.core.comm.Packet;
import org.alliance.core.comm.RPC;
import org.alliance.core.comm.T;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2005-dec-21
 * Time: 16:34:22
 * To change this template use File | Settings | File Templates.
 */
public abstract class CompressedRPC extends RPC {

    public abstract void executeCompressed(DataInputStream in) throws IOException;

    public abstract void serializeCompressed(DataOutputStream out) throws IOException;

    @Override
    public void execute(Packet data) throws IOException {
        int len = data.readInt();
        byte arr[] = new byte[len];
        data.readArray(arr);
        ByteArrayInputStream bais = new ByteArrayInputStream(arr);
        DataInputStream in = new DataInputStream(new InflaterInputStream(bais));

        executeCompressed(in);
        in.close();
    }

    @Override
    public Packet serializeTo(Packet p) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream(1024); //in general compressed RPC are fairly big packets - that's why we crank up the starting byte array size
        DataOutputStream out = new DataOutputStream(new DeflaterOutputStream(buf, new Deflater(9)));

        serializeCompressed(out);

        out.flush();
        out.close();
        byte arr[] = buf.toByteArray();
        p.writeInt(arr.length);
        if (T.t) {
            System.out.println("Compressed RPC: from " + out.size() + " bytes to " + arr.length + " bytes.");
        }
        p.writeArray(arr);
        return p;
    }
}

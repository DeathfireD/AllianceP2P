package org.alliance.misc;

import com.stendahls.nif.util.SimpleTimer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2005-dec-30
 * Time: 14:04:23
 * To change this template use File | Settings | File Templates.
 */
public class RPCFactoryGenerator {

    public static void main(String[] args) throws IOException {
        System.out.println("USAGE: RPCFactoryGenerator <path to RPC source files> <rpcfactory filename>\n");

        System.out.println("Can't be used anymore. Can't guarantee that old id's will remain the same.");
        System.exit(1);

        SimpleTimer st = new SimpleTimer();
        File files[] = new File(args[0]).listFiles();
        ArrayList<String> rpcNames = new ArrayList<String>();
        for (File f : files) {
            String s = f.toString();
            if (s.indexOf('/') != -1) {
                s = s.substring(s.lastIndexOf('/') + 1);
            }
            if (s.indexOf('\\') != -1) {
                s = s.substring(s.lastIndexOf('\\') + 1);
            }
            if (s.indexOf('.') != -1) {
                s = s.substring(0, s.indexOf('.'));
            }
            if (!"CVS".equals(s)) {
                rpcNames.add(s);
            }
        }

        BufferedWriter out = new BufferedWriter(new FileWriter(new File(args[1])));

        out.write("package org.alliance.core.comm;\n");
        out.write("\n");
        out.write("import org.alliance.core.comm.rpc.*;\n");
        out.write("\n");
        out.write("/** Generated at " + new Date() + " by RPCFactoryGenerator */\n");
        out.write("public class RPCFactory {\n");
        out.write("    public static RPC newInstance(int packetId) {\n");
        out.write("        RPC rpc = null;\n");
        out.write("        switch(packetId) {\n");

        for (int i = 0; i < rpcNames.size(); i++) {
            String s = rpcNames.get(i);
            out.write("            case " + (i + 1) + ": rpc = new " + s + "(); break;\n");
        }

        out.write("        }\n");
        out.write("        if (rpc == null) if (T.t) T.error(\"UNRECOGNIZED rpc id: \"+packetId);\n");
        out.write("        return rpc;\n");
        out.write("    }\n");
        out.write("\n");
        out.write("    public static byte getPacketIdFor(RPC rpc) {\n");

        for (int i = 0; i < rpcNames.size(); i++) {
            String s = rpcNames.get(i);
            out.write("        if (rpc instanceof " + s + ") return " + (i + 1) + ";\n");
        }

        out.write("        if(T.t)T.error(\"Could not identify RPC: \"+rpc);\n");
        out.write("        return -1;\n");
        out.write("   }\n");
        out.write("}\n");
        out.flush();
        out.close();

        System.out.println("Generated source file in " + st.getTime() + ".");
    }
}

package org.alliance.core.comm;

import com.stendahls.nif.util.jarverifier.JarVerifier;
import org.alliance.core.CoreSubsystem;
import static org.alliance.core.CoreSubsystem.KB;
import org.alliance.core.file.blockstorage.CacheStorage;
import org.alliance.core.file.blockstorage.T;
import org.alliance.core.file.filedatabase.FileDescriptor;
import org.alliance.core.file.hash.Hash;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.jar.JarFile;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-feb-07
 * Time: 13:44:21
 */
public class AutomaticUpgrade {

    public static final String UPGRADE_FILENAME = "alliance.upgrade";
    public static final File SOURCE_JAR = new File("alliance.dat");
    private CacheStorage cache;
    private CoreSubsystem core;
    private boolean upgradeAttemtHasBeenMade = false;
    private Hash newVersionHash, myJarHash;

    public AutomaticUpgrade(CoreSubsystem core, CacheStorage cache) throws IOException {
        this.core = core;
        this.cache = cache;

        File latestVersionFile = new File(cache.getCompleteFilesFilePath() + "/" + UPGRADE_FILENAME);
        if (SOURCE_JAR.exists() && (!latestVersionFile.exists() || latestVersionFile.length() != SOURCE_JAR.length())) {
            if (org.alliance.core.file.blockstorage.T.t) {
                T.info("Copying alliance jar to cache...");
            }
            copyFile(SOURCE_JAR, latestVersionFile);
        }

        if (SOURCE_JAR.exists()) {
            myJarHash = new FileDescriptor("", SOURCE_JAR, core.getSettings().getInternal().getHashspeedinmbpersecond()).getRootHash();
        }
    }

    public static void copyFile(File src, File dst) throws IOException {
        if (T.t) {
            T.info("Copying " + src + " -> " + dst + "...");
        }
        FileInputStream in = new FileInputStream(src);
        FileOutputStream out = new FileOutputStream(dst);
        byte buf[] = new byte[40 * KB];
        int read;
        while ((read = in.read(buf)) != -1) {
            out.write(buf, 0, read);
        }
        out.flush();
        out.close();
        in.close();
        if (T.t) {
            T.info("File copied.");
        }
    }

    public Hash getMyJarHash() {
        return myJarHash;
    }

    public void performUpgrade() throws Exception {
        if (upgradeAttemtHasBeenMade) {
            if (T.t) {
                T.info("No need to try to upgrade to new version several times.");
            }
        }
        upgradeAttemtHasBeenMade = true;
        if (T.t) {
            T.info("Upgrade received! Automatically upgrading to new version with hash " + newVersionHash);
        }
        core.getUICallback().statusMessage("Verifying 2048 bit RSA signature of new update...");
        FileDescriptor fd = core.getFileManager().getFd(newVersionHash);

        if (fd == null) {
            if (T.t) {
                T.error("Could not find FD for upgrade!");
            }
            core.getUICallback().statusMessage("Automatic upgrade failed. Restarting might help...");
        } else {
            if (T.t) {
                T.info("Fd for update: " + fd);
            }

            if (T.t) {
                T.info("Loading certificate");
            }
            InputStream inStream = core.getRl().getResourceStream("alliance.cer");
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            X509Certificate cert = (X509Certificate) cf.generateCertificate(inStream);
            inStream.close();

            if (T.t) {
                T.info("Verifying jar " + fd.getFullPath() + "...");
            }
            //JarVerifier jv = new JarVerifier(new X509Certificate[]{cert});
            //jv.verify(new JarFile(fd.getFullPath(), true));

            core.getUICallback().statusMessage("Upgrade verified! Restarting.");

            if (T.t) {
                T.info("Jar verified! Upgrading...");
            }

            copyFile(new File(fd.getFullPath()), new File("alliance.tmp"));
            if (T.t) {
                T.info("Upgrade successful! Restarting.");
            }

            core.softRestart();
        }
    }

    public Hash getNewVersionHash() {
        return newVersionHash;
    }

    public void setNewVersionHash(Hash newVersionHash) {
        this.newVersionHash = newVersionHash;
    }
}

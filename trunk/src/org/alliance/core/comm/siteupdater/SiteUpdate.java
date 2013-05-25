package org.alliance.core.comm.siteupdater;

import org.alliance.Version;
import org.alliance.core.CoreSubsystem;
import org.alliance.core.file.FileManager;
import org.alliance.core.Language;
import org.alliance.core.comm.T;
import org.alliance.launchers.OSInfo;
import static org.alliance.core.CoreSubsystem.KB;
import static org.alliance.launchers.ui.DirectoryCheck.STARTED_JAR_NAME;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;

/**
 *
 * @author Bastvera
 */
public class SiteUpdate implements Runnable {

    private final CoreSubsystem core;
    private static final String JAR_URL = "http://alliancep2pbeta.googlecode.com/svn/updater/version/alliance.new";
    private static final String INFO_URL = "http://alliancep2pbeta.googlecode.com/svn/updater/version/build.info";
    private boolean alive = true;
    private String updateFilePath;
    private String orginalFilePath;
    private String siteVersion = Version.VERSION;
    private int siteBuild = Version.BUILD_NUMBER;
    private String md5;
    private boolean updateAttemptHasBeenMade = false;

    public SiteUpdate(CoreSubsystem core) throws IOException {
        this.core = core;
        updateFilePath = core.getFileManager().getCache().getCompleteFilesFilePath().getCanonicalPath() + System.getProperty("file.separator") + FileManager.UPDATE_FILE_NAME;
        orginalFilePath = new File(STARTED_JAR_NAME).getCanonicalPath();
    }

    public int getSiteBuild() {
        return siteBuild;
    }

    public String getSiteVersion() {
        return siteVersion;
    }

    @Override
    public void run() {
        //Delayed start
        try {
            Thread.sleep(15 * 1000);
        } catch (InterruptedException ex) {
        }
        while (alive) {
            try {
                if (isNewVersionAvailable()) {
                    core.siteUpdateAvailable();
                } else {
                    core.getUICallback().statusMessage(Language.getLocalizedString(getClass(), "noupdates"), true);
                }
            } catch (IOException ex) {
                core.getUICallback().statusMessage(Language.getLocalizedString(getClass(), "updatecheckfailed"), true);
            }
            try {
                Thread.sleep(4 * 60 * 60 * 1000);//4h
            } catch (InterruptedException ex) {
            }
        }
    }

    public boolean isNewVersionAvailable() throws IOException {
        core.getUICallback().statusMessage(Language.getLocalizedString(getClass(), "checking"), true);
        URLConnection http = new URL(INFO_URL).openConnection();
        http.setConnectTimeout(1000 * 15);
        http.setReadTimeout(1000 * 15);
        BufferedReader in = new BufferedReader(new InputStreamReader(http.getInputStream(), "UTF8"));
        String line;
        while ((line = in.readLine()) != null) {
            if (line.startsWith("version=")) {
                siteVersion = line.substring(8);
            } else if (line.startsWith("build=")) {
                siteBuild = Integer.parseInt(line.substring(6));
            } else if (line.startsWith("md5=")) {
                md5 = line.substring(4);
            }
        }
        in.close();
        if (siteBuild > Version.BUILD_NUMBER) {
            return true;
        } else {
            return false;
        }
    }

    public void beginDownload() {
        core.invokeLater(new Runnable() {

            @Override
            public void run() {
                try {
                    core.getUICallback().statusMessage(Language.getLocalizedString(getClass(), "downloadingstart"), true);
                    URLConnection http = new URL(JAR_URL).openConnection();
                    http.setConnectTimeout(1000 * 15);
                    http.setReadTimeout(1000 * 15);
                    InputStream in = http.getInputStream();
                    OutputStream out = new FileOutputStream(updateFilePath);
                    byte[] buf = new byte[256 * KB];
                    int read;
                    int readed = 0;
                    while ((read = in.read(buf)) > 0) {
                        readed += read;
                        core.getUICallback().statusMessage(Language.getLocalizedString(getClass(), "downloading",
                                Integer.toString(readed), Integer.toString(http.getContentLength())),
                                true);
                        out.write(buf, 0, read);
                    }
                    out.close();
                    in.close();
                    checkDownloadFile();
                    core.updateDownloaded();
                    core.getUICallback().statusMessage(Language.getLocalizedString(getClass(), "downloadok"), true);
                } catch (IOException ex) {
                    core.getUICallback().statusMessage(Language.getLocalizedString(getClass(), "downloadfailed"), true);
                } catch (SecurityException ex) {
                    core.getUICallback().statusMessage(Language.getLocalizedString(getClass(), "downloadcorrupt"), true);
                } catch (Exception ex) {
                    core.getUICallback().statusMessage(Language.getLocalizedString(getClass(), "downloadcorrupt"), true);
                }
            }
        });
    }

    public void checkDownloadFile() throws Exception {
        InputStream fis = new FileInputStream(updateFilePath);
        byte[] buffer = new byte[1024];
        MessageDigest complete = MessageDigest.getInstance("MD5");
        int numRead;
        do {
            numRead = fis.read(buffer);
            if (numRead > 0) {
                complete.update(buffer, 0, numRead);
            }
        } while (numRead != -1);
        fis.close();
        byte[] raw = complete.digest();
        String hexes = "0123456789ABCDEF";
        final StringBuilder hex = new StringBuilder(2 * raw.length);
        for (final byte b : raw) {
            hex.append(hexes.charAt((b & 0xF0) >> 4)).append(hexes.charAt((b & 0x0F)));
        }
        if (!hex.toString().equals(md5)) {
            throw new Exception();
        }
    }
    
    public void prepareUpdate() {
        try {
            if (updateAttemptHasBeenMade) {
                if (T.t) {
                    T.info("No need to try to upgrade to new version several times.");
                }
            }
            updateAttemptHasBeenMade = true;
            core.runUpdater(updateFilePath, orginalFilePath, siteVersion, siteBuild);
        } catch (Exception e) {
            core.getUICallback().statusMessage(Language.getLocalizedString(getClass(), "updatefailed"), true);
            if (!OSInfo.isWindows()) {
                core.getUICallback().statusMessage(Language.getLocalizedString(getClass(), "updatemanual"), true);
            }
        }
    }
}

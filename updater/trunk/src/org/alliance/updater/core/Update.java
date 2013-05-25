package org.alliance.updater.core;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;

import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.alliance.updater.ui.MainWindow;

/**
 *
 * @author Bastvera
 */
public class Update {

    public enum Step {

        UNZIP, VERIFY, BACKUP, UPDATE, CLEANING, FINALIZING;
    }
    private File updateFile;
    private String updateDirPath;
    private File mainAllianceFile;
    private String allianceDirPath;
    private String unzipDirPath;
    private String backupDirPath;
    private Core core;
    private final static String ALLIANCE_MAIN_WIN = "alliance.dat";
    private final static String ALLIANCE_MAIN_JAR = "alliance.jar";
    private final static String ALLIANCE_UPDATE = "alliance.update";

    public Update(Core core) {
        this.core = core;
        String userDirectory;
        if (OSInfo.isLinux()) {
            if (new File("portable").exists()) {
                userDirectory = "";
            } else {
                userDirectory = System.getProperty("user.home") + "/.alliance/";
            }
        } else if (OSInfo.isWindows()) {
            if (new File("portable").exists()) {
                userDirectory = "";
            } else {
                userDirectory = System.getenv("APPDATA") + "/Alliance/";
            }
        } else {
            userDirectory = "";
        }
        updateFile = new File(userDirectory + "cache/" + ALLIANCE_UPDATE);
        if (!updateFile.exists()) {
            updateFile = MainWindow.localizeFile(ALLIANCE_UPDATE, "update", "Alliance Update Files");
        }
        updateDirPath = updateFile.getAbsolutePath();
        updateDirPath = updateDirPath.substring(0, updateDirPath.length() - ALLIANCE_UPDATE.length());
        if (OSInfo.isWindows()) {
            mainAllianceFile = new File(ALLIANCE_MAIN_WIN);
        } else {
            mainAllianceFile = new File(ALLIANCE_MAIN_JAR);
        }
        if (!mainAllianceFile.exists()) {
            if (OSInfo.isWindows()) {
                mainAllianceFile = MainWindow.localizeFile(ALLIANCE_MAIN_WIN, "dat", "Alliance Main File");
            } else {
                mainAllianceFile = MainWindow.localizeFile(ALLIANCE_MAIN_JAR, "jar", "Alliance Main File");
            }
        }
        allianceDirPath = mainAllianceFile.getAbsolutePath();
        allianceDirPath = allianceDirPath.substring(0, allianceDirPath.length() - ALLIANCE_MAIN_WIN.length());
    }

    public void updateStep(final Step step) {
        Runnable runnable = new Runnable() {

            @Override
            public void run() {
                switch (step) {
                    case UNZIP:
                        core.getMainWindow().appendHeadText("Unzipping files:");
                        unzip();
                        break;
                    case VERIFY:
                        core.getMainWindow().appendHeadText("Verifying 2048 bit RSA signature of new update...");
                        verify();
                        break;
                    case BACKUP:
                        core.getMainWindow().appendHeadText("Preparing a backup:");
                        backup();
                        break;
                    case UPDATE:
                        core.getMainWindow().appendHeadText("Updating:");
                        updating();
                        break;
                    case CLEANING:
                        core.getMainWindow().appendHeadText("Cleaning...");
                        cleaning();
                        break;
                    case FINALIZING:
                        core.getMainWindow().finish();
                        break;
                    default:
                }
            }
        };
        if (runnable != null) {
            Thread runnableT = new Thread(runnable);
            runnableT.start();
        }
    }

    private void unzip() {
        try {
            new File(updateDirPath + "unzip").mkdir();
            unzipDirPath = updateDirPath + "unzip/";
            int buffer = 256 * 1024;
            BufferedOutputStream dest = null;
            FileInputStream fis = new FileInputStream(updateFile);
            ZipInputStream zis = new ZipInputStream(new BufferedInputStream(fis));
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    new File(unzipDirPath + entry.getName()).mkdir();
                    continue;
                }

                //Skip non-user OS files
                if (OSInfo.isWindows()) {
                    if (entry.getName().endsWith(".jar") && !entry.getName().contains("updater")) {
                        continue;
                    }
                } else {
                    if (entry.getName().endsWith(ALLIANCE_MAIN_WIN)) {
                        continue;
                    }
                    if (entry.getName().endsWith(".exe")) {
                        continue;
                    }
                }

                int count;
                byte data[] = new byte[buffer];
                FileOutputStream fos = new FileOutputStream(unzipDirPath + entry.getName());
                dest = new BufferedOutputStream(fos, buffer);
                while ((count = zis.read(data, 0, buffer)) != -1) {
                    dest.write(data, 0, count);
                }
                dest.flush();
                dest.close();
                core.getMainWindow().appendText(new File(unzipDirPath + entry.getName()).getAbsolutePath());
            }
            zis.close();
            updateStep(Step.VERIFY);
        } catch (Exception ex) {
            core.getMainWindow().showErrorDialog(ex);
        }
    }

    private void verify() {
        try {
            InputStream inStream = getClass().getResourceAsStream("/res/alliance.cer");
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            X509Certificate cert = (X509Certificate) cf.generateCertificate(inStream);
            inStream.close();
            JarVerifier jv = new JarVerifier(new X509Certificate[]{cert});
            if (OSInfo.isWindows()) {
                jv.verify(new JarFile(unzipDirPath + ALLIANCE_MAIN_WIN, true));
            } else {
                jv.verify(new JarFile(unzipDirPath + ALLIANCE_MAIN_JAR, true));
            }
            updateStep(Step.BACKUP);
        } catch (Exception ex) {
            core.getMainWindow().showErrorDialog(ex);
        }
    }

    private void backup() {
        try {
            new File(allianceDirPath + "backup").mkdir();
            backupDirPath = allianceDirPath + "backup/";
            copyFiles(new File(unzipDirPath), allianceDirPath, backupDirPath);

            long time = System.currentTimeMillis();

            core.getMainWindow().appendHeadText("Creating a backup file: alliance" + time + ".zip...");

            File zipped = new File(backupDirPath + "alliance" + time + ".zip");
            zipped.createNewFile();

            FileOutputStream dest = new FileOutputStream(zipped);
            ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(dest));
            int buffer = 256 * 1024;
            byte data[] = new byte[buffer];

            ArrayList<File> files = new ArrayList<File>();
            gatherFiles(files, new File(backupDirPath));

            BufferedInputStream origin = null;
            for (File file : files) {
                if (file.isDirectory()) {
                    continue;
                }
                FileInputStream fi = new FileInputStream(file);
                origin = new BufferedInputStream(fi, buffer);
                ZipEntry entry = new ZipEntry(file.getAbsolutePath().substring(backupDirPath.length()));
                out.putNextEntry(entry);
                int count;
                while ((count = origin.read(data, 0, buffer)) != -1) {
                    out.write(data, 0, count);
                }
                origin.close();
            }
            //Remove zipped files
            for (File file : files) {
                deleteFiles(file);
            }
            out.close();
            updateStep(Step.UPDATE);
        } catch (Exception ex) {
            core.getMainWindow().showErrorDialog(ex);
        }
    }

    private void gatherFiles(ArrayList<File> files, File file) {
        if (file.isDirectory()) {
            for (File f : file.listFiles()) {
                gatherFiles(files, f);
            }
        }
        if (file.getName().endsWith(".zip")) {
            return;
        }
        files.add(file);
    }

    private void updating() {
        try {
            copyFiles(new File(unzipDirPath), unzipDirPath, allianceDirPath);
            updateStep(Step.CLEANING);
        } catch (Exception ex) {
            core.getMainWindow().showErrorDialog(ex);
        }
    }

    private void copyFiles(File dir, String src, String dst) throws Exception {
        String path = dir.getAbsolutePath();
        if (dir.isDirectory()) {
            if (path.length() > unzipDirPath.length()) {
                path = path.substring(unzipDirPath.length());
                new File(dst + path).mkdirs();
            }
            for (File file : dir.listFiles()) {
                if (dir.isDirectory()) {
                    copyFiles(file, src, dst);
                }
            }
        } else {
            path = path.substring(unzipDirPath.length());
            core.getMainWindow().appendText(path + " -> " + new File(dst + path).getAbsolutePath());
            try {
                copyFile(new File(src + path), new File(dst + path));
            } catch (FileNotFoundException ex) {
                core.getMainWindow().appendText(path + " -> New File!!! Backup skipped...");
            }
        }
    }

    private void copyFile(final File src, final File dst) throws FileNotFoundException, Exception {
        FileInputStream in = null;
        in = new FileInputStream(src);
        FileOutputStream out = new FileOutputStream(dst);
        byte[] buf = new byte[256 * 1024];
        int read;
        while ((read = in.read(buf)) != -1) {
            out.write(buf, 0, read);
        }
        out.flush();
        out.close();
        in.close();
    }

    private void cleaning() {
        try {
            deleteFiles(new File(unzipDirPath));
            updateStep(Step.FINALIZING);
        } catch (Exception ex) {
            core.getMainWindow().showErrorDialog(ex);
        }
    }

    private void deleteFiles(File dir) throws Exception {
        if (dir.isDirectory()) {
            for (File file : dir.listFiles()) {
                if (dir.isDirectory()) {
                    deleteFiles(file);
                }
            }
        }
        if (!dir.getName().endsWith(".zip")) {
            dir.delete();
        }
    }

    public File getMainAllianceFile() {
        return mainAllianceFile;
    }
}

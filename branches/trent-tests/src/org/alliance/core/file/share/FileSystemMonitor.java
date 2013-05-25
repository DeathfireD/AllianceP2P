package org.alliance.core.file.share;

import com.stendahls.util.TextUtils;
import net.contentobjects.jnotify.JNotify;
import net.contentobjects.jnotify.JNotifyListener;
import static org.alliance.core.CoreSubsystem.KB;
import org.alliance.core.T;
import org.alliance.launchers.OSInfo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2008-apr-02
 * Time: 21:16:47
 * To change this template use File | Settings | File Templates.
 */
public class FileSystemMonitor {

    private ShareManager manager;
    private ArrayList<Integer> watchers = new ArrayList<Integer>();

    public FileSystemMonitor(ShareManager manager) throws IOException {
        this.manager = manager;
        if (OSInfo.isWindows()) {
            extractNativeLibs("jnotify.dll");
            extractNativeLibs("jnotify_64bit.dll");
        }
    }

    public void kill() throws IOException {
        if (!OSInfo.isWindows()) {
            return;
        }
        while (watchers.size() > 0) {
            if (T.t) {
                T.info("Should stop win32 watcher: " + watchers.get(0) + " but removing is buggy so just let it run in background.");
            }        
            watchers.remove(0);
        }
    }

    public void launch() throws IOException {
        if (!OSInfo.isWindows()) {
            return;
        }
        if (watchers.size() > 0) {
            kill();
        }

        int mask = JNotify.FILE_CREATED
                | JNotify.FILE_DELETED
                | JNotify.FILE_RENAMED;

        for (final ShareBase sb : manager.shareBases()) {
            if (!new File(sb.getPath()).exists()) {
                if (T.t) {
                    T.warn("Path does not exist: " + sb.getPath() + " - can't start win32 watcher.");
                }
            } else {
                if (T.t) {
                    T.info("Launching file system watcher for " + sb);
                }
                try {
                    int watchID = JNotify.addWatch(sb.getPath(), mask, true, new JNotifyListener() {

                        @Override
                        public void fileRenamed(int wd, String rootPath, String oldName, String newName) {
                            if (!watchers.contains(wd)) {
                                return;
                            }
                            if (T.t) {
                                T.trace("JNotifyTest.fileRenamed() : wd #" + wd + " root = " + rootPath + ", " + oldName + " -> " + newName);
                            }
                            rootPath = TextUtils.makeSurePathIsMultiplatform(rootPath);
                            oldName = TextUtils.makeSurePathIsMultiplatform(oldName);
                            newName = TextUtils.makeSurePathIsMultiplatform(newName);
                            if (!rootPath.endsWith("/")) {
                                rootPath += '/';
                            }
                            manager.getShareScanner().signalFileRenamed(sb.getPath(), rootPath + oldName, rootPath + newName);
                        }

                        @Override
                        public void fileModified(int wd, String rootPath, String name) {
                            if (!watchers.contains(wd)) {
                                return;
                            }
                            if (T.t) {
                                T.trace("JNotifyTest.fileModified() : wd #" + wd + " root = " + rootPath + ", " + name);
                            }
                        }

                        @Override
                        public void fileDeleted(int wd, String rootPath, String name) {
                            if (!watchers.contains(wd)) {
                                return;
                            }
                            if (T.t) {
                                T.trace("JNotifyTest.fileDeleted() : wd #" + wd + " root = " + rootPath + ", " + name);
                            }
                            rootPath = TextUtils.makeSurePathIsMultiplatform(rootPath);
                            name = TextUtils.makeSurePathIsMultiplatform(name);
                            if (!rootPath.endsWith("/")) {
                                rootPath += '/';
                            }
                            manager.getShareScanner().signalFileDeleted(sb.getPath(), rootPath + name);
                        }

                        @Override
                        public void fileCreated(int wd, String rootPath, String name) {
                            if (!watchers.contains(wd)) {
                                return;
                            }
                            if (T.t) {
                                T.trace("JNotifyTest.fileCreated() : wd #" + wd + " root = " + rootPath + ", " + name);
                            }
                            rootPath = TextUtils.makeSurePathIsMultiplatform(rootPath);
                            name = TextUtils.makeSurePathIsMultiplatform(name);
                            if (!rootPath.endsWith("/")) {
                                rootPath += '/';
                            }
                            manager.getShareScanner().signalFileCreated(rootPath + name);
                        }
                    });
                    if (T.t) {
                        T.info("Got watch id: " + watchID);
                    }
                    watchers.add(watchID);
                } catch (Exception e) {
                    if (T.t) {
                        T.warn("Could not add watch: " + e);
                    }
                }
            }
        }
    }

    private void extractNativeLibs(String name) throws IOException {
        File f = new File(name);
        if (!f.exists()) {
            if (org.alliance.core.T.t) {
                T.info("Extracting lib: " + name);
            }
            FileOutputStream out = new FileOutputStream(f);
            InputStream in = manager.getCore().getRl().getResourceStream(name);
            byte buf[] = new byte[10 * KB];
            int read;
            while ((read = in.read(buf)) != -1) {
                out.write(buf, 0, read);
            }
            out.flush();
            out.close();
            if (T.t) {
                T.info("Done.");
            }
        }
    }
}

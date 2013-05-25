package org.alliance.core.file.share;

import org.alliance.core.CoreSubsystem;
import org.alliance.core.file.filedatabase.FileDatabase;
import org.alliance.core.settings.Settings;
import org.alliance.core.settings.Share;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import com.stendahls.util.TextUtils;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2006-jan-06
 * Time: 15:40:55
 * To change this template use File | Settings | File Templates.
 */
public class ShareManager {

    private HashMap<String, ShareBase> shareBases = new HashMap<String, ShareBase>();
    private ArrayList<ShareBase> shareBaseOrder = new ArrayList<ShareBase>();
    private FileDatabase fileDatabase;
    private ShareScanner shareScanner;
    private FileSystemMonitor fileSystemMonitor;
    private Settings settings;
    private CoreSubsystem core;

    public ShareManager(CoreSubsystem core, Settings settings) throws IOException {
        this.core = core;
        this.settings = settings;
        fileDatabase = new FileDatabase(core);
        shareScanner = new ShareScanner(core, this);
        fileSystemMonitor = new FileSystemMonitor(this);

        updateShareBases();

        shareScanner.start();
    }

    public void updateShareBases() throws IOException {
        shareBases.clear();
        shareBaseOrder.clear();
        for (Share s : settings.getSharelist()) {
            add(new ShareBase(s.getPath(), s.getSgroupname(), s.getExternal()));
        }
        if (!isShared(settings.getInternal().getCachefolder())) {
            add(new ShareBase(settings.getInternal().getCachefolder(), "Public", 0));
        }
        if (!isShared(settings.getInternal().getDownloadfolder())) {
            add(new ShareBase(settings.getInternal().getDownloadfolder(), "Public", 0));
        }
        if (fileSystemMonitor != null) {
            fileSystemMonitor.launch();
        }
    }

    private boolean isShared(String folder) {
        folder = TextUtils.makeSurePathIsMultiplatform(folder);
        for (ShareBase sb : shareBases.values()) {
            if (folder.startsWith(TextUtils.makeSurePathIsMultiplatform(sb.getPath()))) {
                return true;
            }
        }
        return false;
    }

    public ShareBase getShareBaseByFile(String file) {
        file = TextUtils.makeSurePathIsMultiplatform(file);
        for (ShareBase sb : shareBases.values()) {
            if (file.startsWith(TextUtils.makeSurePathIsMultiplatform(sb.getPath()))) {
                return sb;
            }
        }
        return null;
    }

    private void add(ShareBase sb) {
        shareBases.put(sb.getPath(), sb);
        shareBaseOrder.add(sb);
    }

    public void shutdown() throws IOException {
        shareScanner.kill();
    }

    public ShareBase getBaseByPath(String path) {
        return shareBases.get(path);
    }

    public FileDatabase getFileDatabase() {
        return fileDatabase;
    }

    public Settings getSettings() {
        return settings;
    }

    public Collection<ShareBase> shareBases() {
        return shareBaseOrder;
    }

    public ShareBase getBaseByIndex(int index) {
        return shareBaseOrder.get(index);
    }

    public ShareScanner getShareScanner() {
        return shareScanner;
    }

    public CoreSubsystem getCore() {
        return core;
    }
}

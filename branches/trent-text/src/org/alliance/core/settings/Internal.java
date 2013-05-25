package org.alliance.core.settings;

import com.stendahls.util.TextUtils;
import static org.alliance.core.CoreSubsystem.KB;
import org.alliance.launchers.OSInfo;

import java.io.File;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileSystemView;

/**
 * Created by IntelliJ IDEA.
 * User: maciek
 * Date: 2005-dec-28
 * Time: 17:40:27
 */
public class Internal extends SettingClass {

    private static final String CURRENT_DIRECTORY;
    private static final String USER_DIRECTORY;

    static {
        if (OSInfo.isLinux()) {
            if (new File("portable").exists()) {
                USER_DIRECTORY = "";
            } else {
                USER_DIRECTORY = System.getProperty("user.home") + "/.alliance/";
            }
            CURRENT_DIRECTORY = USER_DIRECTORY;
        } else if (OSInfo.isWindows()) {
            if (new File("portable").exists()) {
                USER_DIRECTORY = "";
            } else {
                USER_DIRECTORY = System.getenv("APPDATA") + "/Alliance/";
            }
            JFileChooser fr = new JFileChooser();
            FileSystemView fw = fr.getFileSystemView();
            CURRENT_DIRECTORY = TextUtils.makeSurePathIsMultiplatform(fw.getDefaultDirectory() + "/Alliance/");
        } else {
            String s = new File(".").getAbsoluteFile().toString();
            if (s.endsWith(".")) {
                s = s.substring(0, s.length() - 1);
            }
            CURRENT_DIRECTORY = s;
            USER_DIRECTORY = "";
        }
    }
    private String databasefile = USER_DIRECTORY + "data/db/alliance";
    private String downloadquefile = USER_DIRECTORY + "data/downloads.dat";
    private String corestatefile = USER_DIRECTORY + "data/core.dat";
    private String historyfile = USER_DIRECTORY + "data/history.dat";
    private String windowstatefile = USER_DIRECTORY + "data/window.state";
    private String pmsound = "sounds/chatpm.wav";
    private String publicsound = "sounds/chatpublic.wav";
    private String downloadsound = "sounds/download.wav";
    private String downloadfolder = CURRENT_DIRECTORY + "downloads";
    private String cachefolder = USER_DIRECTORY + "cache";
    private String keystorefilename = CURRENT_DIRECTORY + "me.ks";
    private Integer reconnectinterval = 60 * 10;
    private Integer connectFriendInterval = 1;
    private Integer sharemanagercycle = 30;
    private Integer sharemanagercyclewithfilesystemeventsactive = 60 * 2;
    private Integer maxdownloadconnections = 10;
    private Integer recordoutspeed = 0, recordinspeed = 0;
    private Integer totalmegabytesdownloaded = 0, totalmegabytesuploaded = 0;
    private Integer connectionkeepaliveinterval = 60;
    private Integer numberofblockstopipeline = 2;
    private Integer usedirectbuffers = 1; // Should direct nio buffers be used? 0=no 1=yes   - does no performance gain but it looks better in Task Manager, when looking at memory usage, not sure if the actual memory usage is better
    private Long lastnaggedaboutinvitingafriend;
    private Integer hastriedtoinviteafriend;
    private Integer hasneverdownloadedafile = 1;
    private Integer enablesupportfornonenglishcharacters = 1;
    private Integer alwaysautomaticallyconnecttoallfriendsoffriend = 0;
    private Integer alwaysdenyuntrustedinvitations = 1;
    private Integer alwaysallowfriendsoftrustedfriendstoconnecttome = 1;
    private Integer disablenewuserpopup = 0;
    private Integer rdnsname = 0;
    private Integer secondstoaway = 60 * 5;
    private String chipersuite = ""; //user defined chipher suite, none by default
    private Integer encryption = 0; // 0: TranslationCryptoLayer 1: SSLCryptoLayer
    private Integer daysnotconnectedwhenold = 7 * 2; //after two weeks of disconnection from a friend hes concidered old
    private Integer alwaysallowfriendstoconnect = 1;
    private Integer alwaysallowfriendsoffriendstoconnecttome = 0;
    private Integer automaticallydenyallinvitations = 0;
    private Integer showpublicchatmessagesintray = 1;
    private Integer showprivatechatmessagesintray = 1;
    private Integer showsystemmessagesintray = 1;
    private Integer rescansharewhenalliancestarts = 1;
    private Integer minimumtimebetweeninvitations = 60 * 24 * 2; //in minutes
    /** Be polite when running on XP sp2 wich only allows 10 pending tcp/ip connections
     * before stopping the network stack. Set to 8 to be on the safe side. */
    private Integer maxpendingconnections = 8;
    private Integer hashspeedinmbpersecond = 20; //when in background mode //Bastvera DeathfireD request
    private Integer sosendbuf = -1, soreceivebuf = -1;
    private Integer discwritebuffer = 256 * KB, //one instance of this one per download
            socketsendbuffer = 256 * KB; //one instance per download
    private Integer socketreadbuffer = 256 * KB; //only one instance of this one - at the network layer
    private Integer maximumAlliancePacketSize = 32 * KB;
    private Integer politehashingwaittimeinminutes = 10;
    private Integer politehashingintervalingigabytes = 50;
    private Integer maxfileexpandinblocks = 50; //don't exceed file system size of file we're downloading to by more than this number
    //This is here because if we download the last block of a 4Gb file we seek to 4Gb into
    //an empry file. This makes XP grind to a halt. 100 means expand 100mb per block at most
    private Integer uploadthrottle = 0; //zero to disable
    private Integer enableiprules = 0;
    private String guiskin = "Alliance";
    private String language = "en";
    private String globalfont = "";
    private Integer globalsize = 12;
    private String chatfont = "";
    private Integer chatsize = 12;
    private Integer firststart = 1;
    private Integer skipportcheck = 0;
    private Integer autosortshares = 1;

    public Internal() {
    }

    public Internal(Integer reconnectinterval) {
        this.reconnectinterval = reconnectinterval;
    }

    public Integer getReconnectinterval() {
        return reconnectinterval;
    }

    public void setReconnectinterval(Integer reconnectinterval) {
        this.reconnectinterval = reconnectinterval;
    }

    public Integer getSharemanagercycle() {
        return sharemanagercycle;
    }

    public void setSharemanagercycle(Integer sharemanagercycle) {
        this.sharemanagercycle = sharemanagercycle;
    }

    public String getDatabasefile() {
        return databasefile;
    }

    public void setDatabasefile(String databasefile) {
        this.databasefile = databasefile;
    }

    public String getDownloadfolder() {
        if (!(new File(downloadfolder).exists())) {
            downloadfolder = CURRENT_DIRECTORY + "downloads";
            (new File(downloadfolder)).mkdirs();
        }
        return downloadfolder;
    }

    public void setDownloadfolder(String downloadfolder) {
        this.downloadfolder = downloadfolder;
    }

    public String getCachefolder() {
        return cachefolder;
    }

    public void setCachefolder(String cachefolder) {
        this.cachefolder = cachefolder;
    }

    public Integer getMaxdownloadconnections() {
        return maxdownloadconnections;
    }

    public void setMaxdownloadconnections(Integer maxdownloadconnections) {
        this.maxdownloadconnections = maxdownloadconnections;
    }

    public Integer getRecordoutspeed() {
        return recordoutspeed;
    }

    public void setRecordoutspeed(Integer recordoutspeed) {
        this.recordoutspeed = recordoutspeed;
    }

    public Integer getRecordinspeed() {
        return recordinspeed;
    }

    public void setRecordinspeed(Integer recordinspeed) {
        this.recordinspeed = recordinspeed;
    }

    public Integer getConnectionkeepaliveinterval() {
        return connectionkeepaliveinterval;
    }

    public void setConnectionkeepaliveinterval(Integer connectionkeepaliveinterval) {
        this.connectionkeepaliveinterval = connectionkeepaliveinterval;
    }

    public Integer getSosendbuf() {
        return sosendbuf;
    }

    public void setSosendbuf(Integer sosendbuf) {
        this.sosendbuf = sosendbuf;
    }

    public Integer getSoreceivebuf() {
        return soreceivebuf;
    }

    public void setSoreceivebuf(Integer soreceivebuf) {
        this.soreceivebuf = soreceivebuf;
    }

    public Integer getNumberofblockstopipeline() {
        return numberofblockstopipeline;
    }

    public void setNumberofblockstopipeline(Integer numberofblockstopipeline) {
        this.numberofblockstopipeline = numberofblockstopipeline;
    }

    public Integer getDiscwritebuffer() {
        return discwritebuffer;
    }

    public void setDiscwritebuffer(Integer discwritebuffer) {
        this.discwritebuffer = discwritebuffer;
    }

    public Integer getSocketsendbuffer() {
        return socketsendbuffer;
    }

    public void setSocketsendbuffer(Integer socketsendbuffer) {
        this.socketsendbuffer = socketsendbuffer;
    }

    public Integer getSocketreadbuffer() {
        return socketreadbuffer;
    }

    public void setSocketreadbuffer(Integer socketreadbuffer) {
        this.socketreadbuffer = socketreadbuffer;
    }

    public Integer getMaximumAlliancePacketSize() {
        return maximumAlliancePacketSize;
    }

    public void setMaximumAlliancePacketSize(Integer maximumAlliancePacketSize) {
        this.maximumAlliancePacketSize = maximumAlliancePacketSize;
    }

    public Integer getPolitehashingwaittimeinminutes() {
        return politehashingwaittimeinminutes;
    }

    public void setPolitehashingwaittimeinminutes(Integer politehashingwaittimeinminutes) {
        this.politehashingwaittimeinminutes = politehashingwaittimeinminutes;
    }

    public Integer getPolitehashingintervalingigabytes() {
        return politehashingintervalingigabytes;
    }

    public void setPolitehashingintervalingigabytes(Integer politehashingintervalingigabytes) {
        this.politehashingintervalingigabytes = politehashingintervalingigabytes;
    }

    //don't exceed file system size of file we're downloading to by more than this number
    //This is here because if we downloads the last block of a 4Gb file we seek to 4Gb into
    //an empry file. This makes XP grind to a halt. 100 means expan 100mb per block at most
    public Integer getMaxfileexpandinblocks() {
        return maxfileexpandinblocks;
    }

    public void setMaxfileexpandinblocks(Integer maxfileexpandinblocks) {
        this.maxfileexpandinblocks = maxfileexpandinblocks;
    }

    public Integer getUploadthrottle() {
        return uploadthrottle;
    }

    public void setUploadthrottle(Integer uploadthrottle) {
        this.uploadthrottle = uploadthrottle;
    }

    public Integer getHashspeedinmbpersecond() {
        return hashspeedinmbpersecond;
    }

    public void setHashspeedinmbpersecond(Integer hashspeedinmbpersecond) {
        this.hashspeedinmbpersecond = hashspeedinmbpersecond;
    }

    public Integer getConnectFriendInterval() {
        return connectFriendInterval;
    }

    public void setConnectFriendInterval(Integer connectFriendInterval) {
        this.connectFriendInterval = connectFriendInterval;
    }

    public String getCorestatefile() {
        return corestatefile;
    }

    public void setCorestatefile(String corestatefile) {
        this.corestatefile = corestatefile;
    }

    public String getHistoryfile() {
        return historyfile;
    }

    public void setHistoryfile(String historyfile) {
        this.historyfile = historyfile;
    }

    public String getWindowstatefile() {
        return windowstatefile;
    }

    public void setWindowstatefile(String windowstatefile) {
        this.windowstatefile = windowstatefile;
    }

    public String getPmsound() {
        return pmsound;
    }

    public void setPmsound(String pmsound) {
        this.pmsound = pmsound;
    }

    public String getDownloadsound() {
        return downloadsound;
    }

    public void setDownloadsound(String downloadsound) {
        this.downloadsound = downloadsound;
    }

    public String getPublicsound() {
        return publicsound;
    }

    public void setPublicsound(String publicsound) {
        this.publicsound = publicsound;
    }

    public Integer getMaxpendingconnections() {
        return maxpendingconnections;
    }

    public void setMaxpendingconnections(Integer maxpendingconnections) {
        this.maxpendingconnections = maxpendingconnections;
    }

    public Integer getMinimumtimebetweeninvitations() {
        return minimumtimebetweeninvitations;
    }

    public void setMinimumtimebetweeninvitations(Integer minimumtimebetweeninvitations) {
        this.minimumtimebetweeninvitations = minimumtimebetweeninvitations;
    }

    public Integer getAlwaysallowfriendstoconnect() {
        return alwaysallowfriendstoconnect;
    }

    public void setAlwaysallowfriendstoconnect(Integer alwaysallowfriendstoconnect) {
        this.alwaysallowfriendstoconnect = alwaysallowfriendstoconnect;
    }

    public Integer getDaysnotconnectedwhenold() {
        return daysnotconnectedwhenold;
    }

    public void setDaysnotconnectedwhenold(Integer daysnotconnectedwhenold) {
        this.daysnotconnectedwhenold = daysnotconnectedwhenold;
    }

    public String getKeystorefilename() {
        return keystorefilename;
    }

    public void setKeystorefilename(String keystorefilename) {
        this.keystorefilename = keystorefilename;
    }

    public Integer getUsedirectbuffers() {
        return usedirectbuffers;
    }

    public void setUsedirectbuffers(Integer usedirectbuffers) {
        this.usedirectbuffers = usedirectbuffers;
    }

    public Integer getEncryption() {
        return encryption;
    }

    public void setEncryption(Integer encryption) {
        this.encryption = encryption;
    }

    public String getChiphersuite() {
        return chipersuite;
    }

    public void setChipersuite(String chipersuite) {
        this.chipersuite = chipersuite;
    }

    public Integer getAlwaysallowfriendsoffriendstoconnecttome() {
        return alwaysallowfriendsoffriendstoconnecttome;
    }

    public void setAlwaysallowfriendsoffriendstoconnecttome(Integer alwaysallowfriendsoffriendstoconnecttome) {
        this.alwaysallowfriendsoffriendstoconnecttome = alwaysallowfriendsoffriendstoconnecttome;
    }

    public Integer getDisablenewuserpopup() {
        return disablenewuserpopup;
    }

    public void setDisablenewuserpopup(Integer disablenewuserpopup) {
        this.disablenewuserpopup = disablenewuserpopup;
    }

    public Integer getAlwaysallowfriendsoftrustedfriendstoconnecttome() {
        return alwaysallowfriendsoftrustedfriendstoconnecttome;
    }

    public void setAlwaysallowfriendsoftrustedfriendstoconnecttome(Integer alwaysallowfriendsoftrustedfriendstoconnecttome) {
        this.alwaysallowfriendsoftrustedfriendstoconnecttome = alwaysallowfriendsoftrustedfriendstoconnecttome;
    }

    public Integer getAlwaysdenyuntrustedinvitations() {
        return alwaysdenyuntrustedinvitations;
    }

    public void setAlwaysdenyuntrustedinvitations(Integer alwaysdenyuntrustedinvitations) {
        this.alwaysdenyuntrustedinvitations = alwaysdenyuntrustedinvitations;
    }

    public Integer getRdnsname() {
        return rdnsname;
    }

    public void setRdnsname(Integer rdnsname) {
        this.rdnsname = rdnsname;
    }

    public Integer getTotalmegabytesdownloaded() {
        return totalmegabytesdownloaded;
    }

    public void setTotalmegabytesdownloaded(Integer totalmegabytesdownloaded) {
        this.totalmegabytesdownloaded = totalmegabytesdownloaded;
    }

    public Integer getTotalmegabytesuploaded() {
        return totalmegabytesuploaded;
    }

    public void setTotalmegabytesuploaded(Integer totalmegabytesuploaded) {
        this.totalmegabytesuploaded = totalmegabytesuploaded;
    }

    public String getDownloadquefile() {
        return downloadquefile;
    }

    public void setDownloadquefile(String downloadquefile) {
        this.downloadquefile = downloadquefile;
    }

    public Integer getShowpublicchatmessagesintray() {
        return showpublicchatmessagesintray;
    }

    public void setShowpublicchatmessagesintray(Integer showpublicchatmessagesintray) {
        this.showpublicchatmessagesintray = showpublicchatmessagesintray;
    }

    public Integer getShowprivatechatmessagesintray() {
        return showprivatechatmessagesintray;
    }

    public void setShowprivatechatmessagesintray(Integer showprivatechatmessagesintray) {
        this.showprivatechatmessagesintray = showprivatechatmessagesintray;
    }

    public Integer getShowsystemmessagesintray() {
        return showsystemmessagesintray;
    }

    public void setShowsystemmessagesintray(Integer showsystemmessagesintray) {
        this.showsystemmessagesintray = showsystemmessagesintray;
    }

    public Integer getSecondstoaway() {
        return secondstoaway;
    }

    public void setSecondstoaway(Integer secondstoaway) {
        this.secondstoaway = secondstoaway;
    }

    public Integer getSharemanagercyclewithfilesystemeventsactive() {
        return sharemanagercyclewithfilesystemeventsactive;
    }

    public void setSharemanagercyclewithfilesystemeventsactive(Integer sharemanagercyclewithfilesystemeventsactive) {
        this.sharemanagercyclewithfilesystemeventsactive = sharemanagercyclewithfilesystemeventsactive;
    }

    public Long getLastnaggedaboutinvitingafriend() {
        return lastnaggedaboutinvitingafriend;
    }

    public void setLastnaggedaboutinvitingafriend(Long lastnaggedaboutinvitingafriend) {
        this.lastnaggedaboutinvitingafriend = lastnaggedaboutinvitingafriend;
    }

    public Integer getHastriedtoinviteafriend() {
        return hastriedtoinviteafriend;
    }

    public void setHastriedtoinviteafriend(Integer hastriedtoinviteafriend) {
        this.hastriedtoinviteafriend = hastriedtoinviteafriend;
    }

    public Integer getHasneverdownloadedafile() {
        return hasneverdownloadedafile;
    }

    public void setHasneverdownloadedafile(Integer hasneverdownloadedafile) {
        this.hasneverdownloadedafile = hasneverdownloadedafile;
    }

    public Integer getRescansharewhenalliancestarts() {
        return rescansharewhenalliancestarts;
    }

    public void setRescansharewhenalliancestarts(Integer rescansharewhenalliancestarts) {
        this.rescansharewhenalliancestarts = rescansharewhenalliancestarts;
    }

    public Integer getEnablesupportfornonenglishcharacters() {
        return enablesupportfornonenglishcharacters;
    }

    public void setEnablesupportfornonenglishcharacters(Integer enablesupportfornonenglishcharacters) {
        this.enablesupportfornonenglishcharacters = enablesupportfornonenglishcharacters;
    }

    public Integer getAlwaysautomaticallyconnecttoallfriendsoffriend() {
        return alwaysautomaticallyconnecttoallfriendsoffriend;
    }

    public void setAlwaysautomaticallyconnecttoallfriendsoffriend(Integer alwaysautomaticallyconnecttoallfriendsoffriend) {
        this.alwaysautomaticallyconnecttoallfriendsoffriend = alwaysautomaticallyconnecttoallfriendsoffriend;
    }

    public void setEnableiprules(Integer rule) {
        enableiprules = rule;
    }

    public Integer getEnableiprules() {
        return enableiprules;
    }

    public Integer getAutomaticallydenyallinvitations() {
        return automaticallydenyallinvitations;
    }

    public void setAutomaticallydenyallinvitations(Integer automaticallydenyallinvitations) {
        this.automaticallydenyallinvitations = automaticallydenyallinvitations;
    }

    public String getCurrentDirectory() {
        return CURRENT_DIRECTORY;
    }

    public String getUserDirectory() {
        return USER_DIRECTORY;
    }

    public String getGuiskin() {
        return guiskin;
    }

    public void setGuiskin(String guiskin) {
        this.guiskin = guiskin;
    }

    public Integer getSkipportcheck() {
        return skipportcheck;
    }

    public void setSkipportcheck(Integer skipportcheck) {
        this.skipportcheck = skipportcheck;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getChatfont() {
        return chatfont;
    }

    public void setChatfont(String chatfont) {
        this.chatfont = chatfont;
    }

    public Integer getChatsize() {
        return chatsize;
    }

    public void setChatsize(Integer chatsize) {
        this.chatsize = chatsize;
    }

    public String getGlobalfont() {
        return globalfont;
    }

    public void setGlobalfont(String globalfont) {
        this.globalfont = globalfont;
    }

    public Integer getGlobalsize() {
        return globalsize;
    }

    public void setGlobalsize(Integer globalsize) {
        this.globalsize = globalsize;
    }

    public Integer getFirststart() {
        return firststart;
    }

    public void setFirststart(Integer firststart) {
        this.firststart = firststart;
    }

    public Integer getAutosortshares() {
        return autosortshares;
    }

    public void setAutosortshares(Integer autosortshares) {
        this.autosortshares = autosortshares;
    }  
}

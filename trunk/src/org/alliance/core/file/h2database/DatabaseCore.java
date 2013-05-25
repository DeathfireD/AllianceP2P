package org.alliance.core.file.h2database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.alliance.core.CoreSubsystem;

/**
 *
 * @author Bastvera
 */
public class DatabaseCore {

    private final CoreSubsystem core;
    private Connection conn;
    private boolean connected;
    private DatabaseSharesBases dbSharesBases;
    private DatabaseShares dbShares;
    private DatabaseHashes dbHashes;
    private DatabaseDuplicates dbDuplicates;
    private static final String DRIVERURL = "jdbc:h2:";
    private static final String TYPE = "file:";
    //private static final String PATH = "./data/alliancedb";
    private static final String OPTIONS = ";DB_CLOSE_ON_EXIT=FALSE";
    private static final String USER = "sa";
    private static final String PASSWORD = "";
    private static final String DRIVER = "org.h2.Driver";

    public DatabaseCore(CoreSubsystem core) {
        this.core = core;
    }

    public void connect(String backup) throws Exception {
        try {
            Class.forName(DRIVER);
            String path = core.getSettings().getInternal().getDatabasefile();
            conn = DriverManager.getConnection(DRIVERURL + TYPE + path + OPTIONS, USER, PASSWORD);
            if (backup != null) {
                restoreFromBackup(backup);
            }
            changeCache(8 * 1024);
            dbSharesBases = new DatabaseSharesBases(conn);
            dbShares = new DatabaseShares(conn);
            dbHashes = new DatabaseHashes(conn);
            dbDuplicates = new DatabaseDuplicates(conn);
            connected = true;
        } catch (ClassNotFoundException ex) {
            throw new Exception("Failed to initalize database.", ex);
        } catch (SQLException ex) {
            if (backup == null) {
                connect(core.getFileManager().prepareToRestore(null));
            } else {
                throw new Exception("Failed to load database from backup.", ex);
            }
        }
    }

    public void shutdown() {
        if (!connected) {
            return;
        }
        connected = false;
        long time = System.currentTimeMillis();
        //Wait max 10 seconds
        while (core.getFileManager().getFileDatabase().isDbInUse() && System.currentTimeMillis() - time < 1000 * 10) {
            try {
                Thread.sleep(250);
            } catch (InterruptedException ex) {
            }
        }
        try {
            StringBuilder statement = new StringBuilder();
            statement.append("SHUTDOWN COMPACT;");
            PreparedStatement ps = conn.prepareStatement(statement.toString());
            ps.executeUpdate();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public void changeCache(int cache) {
        try {
            StringBuilder statement = new StringBuilder();
            statement.append("SET CACHE_SIZE ?;");
            PreparedStatement ps = conn.prepareStatement(statement.toString());
            ps.setInt(1, cache);
            ps.executeUpdate();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public void backup() {
        try {
            StringBuilder statement = new StringBuilder();
            statement.append("SCRIPT TO ? COMPRESSION ZIP;");
            PreparedStatement ps = conn.prepareStatement(statement.toString());
            ps.setString(1, core.getSettings().getInternal().getDatabasefile() + "-script-" + System.currentTimeMillis() + ".zip");
            ps.executeQuery();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public void restoreFromBackup(String backup) {
        try {
            StringBuilder statement = new StringBuilder();
            statement.append("RUNSCRIPT FROM ? COMPRESSION ZIP;");
            PreparedStatement ps = conn.prepareStatement(statement.toString());
            ps.setString(1, backup);
            ps.executeUpdate();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public DatabaseHashes getDbHashes() {
        return dbHashes;
    }

    public DatabaseShares getDbShares() {
        return dbShares;
    }

    public DatabaseDuplicates getDbDuplicates() {
        return dbDuplicates;
    }

    public DatabaseSharesBases getDbSharesBases() {
        return dbSharesBases;
    }

    public boolean isConnected() {
        return connected;
    }
}

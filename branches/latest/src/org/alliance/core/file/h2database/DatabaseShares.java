package org.alliance.core.file.h2database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 *
 * @author Bastvera
 */
public class DatabaseShares {

    private final Connection conn;

    public DatabaseShares(Connection conn) throws SQLException {
        this.conn = conn;
        createTable();
        createIndexes();
    }

    private void createTable() throws SQLException {
        Statement statement = conn.createStatement();
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TABLE IF NOT EXISTS shares(");
        sql.append("base_path character varying(32767) NOT NULL, ");
        sql.append("sub_path character varying(32767) NOT NULL, ");
        sql.append("filename character varying(32767) NOT NULL, ");
        sql.append("type tinyint NOT NULL, ");
        sql.append("size bigint NOT NULL, ");
        sql.append("root_hash binary NOT NULL, ");
        sql.append("modified bigint NOT NULL, ");
        sql.append("CONSTRAINT pk_shares PRIMARY KEY (root_hash));");
        statement.executeUpdate(sql.toString());
    }

    private void createIndexes() throws SQLException {
        Statement statement = conn.createStatement();
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE INDEX IF NOT EXISTS idx_shares_base_path ON shares(base_path);");
        sql.append("CREATE INDEX IF NOT EXISTS idx_shares_sub_path ON shares(sub_path);");
        sql.append("CREATE INDEX IF NOT EXISTS idx_shares_filename ON shares(filename);");
        sql.append("CREATE INDEX IF NOT EXISTS idx_shares_type ON shares(type);");
        statement.executeUpdate(sql.toString());
    }

    public boolean addEntry(String basePath, String subPath, String filename, byte type, long size, byte[] rootHash, long modifiedAt) {
        try {
            StringBuilder statement = new StringBuilder();
            statement.append("INSERT INTO shares(base_path, sub_path, filename, type, size, root_hash, modified) ");
            statement.append("VALUES(?, ?, ?, ?, ?, ?, ?);");
            PreparedStatement ps = conn.prepareStatement(statement.toString());
            ps.setString(1, basePath);
            ps.setString(2, subPath);
            ps.setString(3, filename);
            ps.setByte(4, type);
            ps.setLong(5, size);
            ps.setBytes(6, rootHash);
            ps.setLong(7, modifiedAt);
            ps.executeUpdate();
            return true;
        } catch (SQLException ex) {
            //TODO ONLY Detection of duplicate pk exception
            return false;
        }
    }

    public ResultSet getEntryByFullPath(String basePath, String subPath, String filename) {
        try {
            StringBuilder statement = new StringBuilder();
            statement.append("SELECT * FROM shares WHERE filename=? ");
            statement.append("GROUP BY root_hash HAVING (base_path=? AND sub_path =?);");
            PreparedStatement ps = conn.prepareStatement(statement.toString());
            ps.setString(1, filename);
            ps.setString(2, basePath);
            ps.setString(3, subPath);
            return ps.executeQuery();
        } catch (SQLException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public ResultSet getEntriesByBasePathAndSubPath(String basePath, String subPath, boolean withSubPaths, int limit) {
        try {
            StringBuilder statement = new StringBuilder();
            statement.append("SELECT * FROM shares WHERE sub_path LIKE ? ");
            statement.append("GROUP BY root_hash HAVING base_path=? ");
            statement.append("LIMIT ?;");
            PreparedStatement ps = conn.prepareStatement(statement.toString());
            if (withSubPaths) {
                ps.setString(1, subPath + "%");
            } else {
                ps.setString(1, subPath);
            }
            ps.setString(2, basePath);
            ps.setInt(3, limit);
            return ps.executeQuery();
        } catch (SQLException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public ResultSet getEntriesBySubPath(String subPath, int limit, int offset) {
        try {
            StringBuilder statement = new StringBuilder();
            statement.append("SELECT * FROM shares WHERE sub_path=? ");
            statement.append("LIMIT ? OFFSET ?;");
            PreparedStatement ps = conn.prepareStatement(statement.toString());
            ps.setString(1, subPath);
            ps.setInt(2, limit);
            ps.setInt(3, offset);
            return ps.executeQuery();
        } catch (SQLException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public ResultSet contains(String basePath, String subPath, String filename) {
        try {
            StringBuilder statement = new StringBuilder();
            statement.append("SELECT BOOL_OR(base_path=? AND sub_path =?) AS contains FROM shares WHERE (filename=?)");
            PreparedStatement ps = conn.prepareStatement(statement.toString());
            ps.setString(1, basePath);
            ps.setString(2, subPath);
            ps.setString(3, filename);
            return ps.executeQuery();
        } catch (SQLException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public ResultSet getEntryByRootHash(byte[] rootHash) {
        try {
            StringBuilder statement = new StringBuilder();
            statement.append("SELECT * FROM shares WHERE root_hash=?;");
            PreparedStatement ps = conn.prepareStatement(statement.toString());
            ps.setBytes(1, rootHash);
            return ps.executeQuery();
        } catch (SQLException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public ResultSet getEntriesByBasePath(String basePath, int limit, int offset) {
        try {
            StringBuilder statement = new StringBuilder();
            statement.append("SELECT * FROM shares WHERE base_path=? LIMIT ? OFFSET ?;");
            PreparedStatement ps = conn.prepareStatement(statement.toString());
            ps.setString(1, basePath);
            ps.setInt(2, limit);
            ps.setInt(3, offset);
            return ps.executeQuery();
        } catch (SQLException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public ResultSet getEntriesBySearchQuery(String query, byte type, int limit) {
        try {
            StringBuilder statement = new StringBuilder();
            statement.append("SELECT ent.* FROM shares AS ent ");
            statement.append("INNER JOIN (SELECT filename FROM shares GROUP BY filename HAVING filename LIKE ? LIMIT 1024) AS file ");
            statement.append("ON ent.filename=file.filename ");
            statement.append("GROUP BY root_hash HAVING type=? ");
            statement.append("LIMIT ?;");
            PreparedStatement ps = conn.prepareStatement(statement.toString());
            ps.setString(1, "%" + query + "%");
            ps.setByte(2, type);
            ps.setInt(3, limit);
            return ps.executeQuery();
        } catch (SQLException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public ResultSet getNumberOfShares() {
        try {
            StringBuilder statement = new StringBuilder();
            statement.append("SELECT Count(*) FROM shares;");
            PreparedStatement ps = conn.prepareStatement(statement.toString());
            return ps.executeQuery();
        } catch (SQLException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public ResultSet getTotalSizeOfFiles() {
        try {
            StringBuilder statement = new StringBuilder();
            statement.append("SELECT SUM(size) FROM shares;");
            PreparedStatement ps = conn.prepareStatement(statement.toString());
            return ps.executeQuery();
        } catch (SQLException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public ResultSet getBasePaths() {
        try {
            StringBuilder statement = new StringBuilder();
            statement.append("SELECT base_path FROM shares GROUP BY base_path;");
            PreparedStatement ps = conn.prepareStatement(statement.toString());
            return ps.executeQuery();
        } catch (SQLException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public ResultSet getSubPaths() {
        try {
            StringBuilder statement = new StringBuilder();
            statement.append("SELECT sub_path FROM shares GROUP BY sub_path;");
            PreparedStatement ps = conn.prepareStatement(statement.toString());
            return ps.executeQuery();
        } catch (SQLException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public void deleteEntryByRootHash(byte[] rootHash) {
        try {
            StringBuilder statement = new StringBuilder();
            statement.append("DELETE FROM shares WHERE root_hash=?;");
            PreparedStatement ps = conn.prepareStatement(statement.toString());
            ps.setBytes(1, rootHash);
            ps.executeUpdate();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }
}

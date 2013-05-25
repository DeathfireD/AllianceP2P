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
public class DatabaseDuplicates {

    private final Connection conn;

    public DatabaseDuplicates(Connection conn) throws SQLException {
        this.conn = conn;
        createTable();
    }

    private void createTable() throws SQLException {
        Statement statement = conn.createStatement();
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TABLE IF NOT EXISTS duplicates(");
        sql.append("path character varying(32767) NOT NULL, ");
        sql.append("root_hash binary NOT NULL, ");
        sql.append("modified bigint NOT NULL, ");
        sql.append("CONSTRAINT fk_duplicates_root_hash FOREIGN KEY (root_hash) REFERENCES shares(root_hash) ON DELETE CASCADE, ");
        sql.append("CONSTRAINT pk_duplicates PRIMARY KEY (path));");
        statement.executeUpdate(sql.toString());
    }

    public void addEntry(String path, byte[] rootHash, long modifiedAt) {
        try {
            StringBuilder statement = new StringBuilder();
            statement.append("INSERT INTO duplicates(path, root_hash, modified) ");
            statement.append("VALUES(?, ?, ?);");
            PreparedStatement ps = conn.prepareStatement(statement.toString());
            ps.setString(1, path);
            ps.setBytes(2, rootHash);
            ps.setLong(3, modifiedAt);
            ps.executeUpdate();
        } catch (SQLException ex) {
        }
    }

    public ResultSet getEntryByPath(String path) {
        try {
            StringBuilder statement = new StringBuilder();
            statement.append("SELECT * FROM duplicates WHERE path=?;");
            PreparedStatement ps = conn.prepareStatement(statement.toString());
            ps.setString(1, path);
            return ps.executeQuery();
        } catch (SQLException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public ResultSet getAllEntries(int limit) {
        try {
            StringBuilder statement = new StringBuilder();
            statement.append("SELECT dup.path, sh.base_path, sh.sub_path, sh.filename FROM DUPLICATES AS dup ");
            statement.append("INNER JOIN shares AS sh ON dup.root_hash=sh.root_hash LIMIT ?;");
            PreparedStatement ps = conn.prepareStatement(statement.toString());
            ps.setInt(1, limit);
            return ps.executeQuery();
        } catch (SQLException ex) {
            ex.printStackTrace();
            return null;
        }
    }
}

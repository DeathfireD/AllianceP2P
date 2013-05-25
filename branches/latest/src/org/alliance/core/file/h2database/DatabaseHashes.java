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
public class DatabaseHashes {

    private final Connection conn;

    public DatabaseHashes(Connection conn) throws SQLException {
        this.conn = conn;
        createTable();
    }

    private void createTable() throws SQLException {
        Statement statement = conn.createStatement();
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TABLE IF NOT EXISTS hashes(");
        sql.append("root_hash binary NOT NULL, ");
        sql.append("hash binary NOT NULL, ");
        sql.append("block_number SMALLINT NOT NULL, ");
        sql.append("CONSTRAINT fk_hashes_root_hash FOREIGN KEY (root_hash) REFERENCES shares(root_hash) ON DELETE CASCADE, ");
        sql.append("CONSTRAINT pk_hashes PRIMARY KEY (root_hash, hash, block_number));");
        statement.executeUpdate(sql.toString());
    }

    public void addEntry(byte[] rootHash, byte[] hash, int blockNumber) {
        try {
            StringBuilder statement = new StringBuilder();
            statement.append("INSERT INTO hashes(root_hash, hash, block_number) ");
            statement.append("VALUES(?, ?, ?);");
            PreparedStatement ps = conn.prepareStatement(statement.toString());
            ps.setBytes(1, rootHash);
            ps.setBytes(2, hash);
            ps.setInt(3, blockNumber);
            ps.executeUpdate();
        } catch (SQLException ex) {
        }
    }

    public ResultSet getEntriesRootHash(byte[] rootHash) {
        try {
            StringBuilder statement = new StringBuilder();
            statement.append("SELECT * FROM hashes WHERE root_hash=? ORDER BY block_number;");
            PreparedStatement ps = conn.prepareStatement(statement.toString());
            ps.setBytes(1, rootHash);
            return ps.executeQuery();
        } catch (SQLException ex) {
            ex.printStackTrace();
            return null;
        }
    }
}

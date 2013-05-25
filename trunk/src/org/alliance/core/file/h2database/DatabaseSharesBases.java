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
public class DatabaseSharesBases {

    private final Connection conn;

    public DatabaseSharesBases(Connection conn) throws SQLException {
        this.conn = conn;
        createTable();
    }

    private void createTable() throws SQLException {
        Statement statement = conn.createStatement();
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TABLE IF NOT EXISTS sharesbases(");
        sql.append("base_path character varying(4096) NOT NULL, ");
        sql.append("CONSTRAINT pk_sharesbases PRIMARY KEY (base_path));");
        statement.executeUpdate(sql.toString());
    }

    public void addEntry(String basePath) {
        try {
            StringBuilder statement = new StringBuilder();
            statement.append("INSERT INTO sharesbases(base_path) ");
            statement.append("VALUES(?);");
            PreparedStatement ps = conn.prepareStatement(statement.toString());
            ps.setString(1, basePath);
            ps.executeUpdate();
        } catch (SQLException ex) {
            //TODO ONLY Detection of duplicate pk exception     
        }
    }

    public ResultSet getBasePaths() {
        try {
            StringBuilder statement = new StringBuilder();
            statement.append("SELECT base_path FROM sharesbases;");
            PreparedStatement ps = conn.prepareStatement(statement.toString());
            return ps.executeQuery();
        } catch (SQLException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public void deleteEntryBy(String basePath) {
        try {
            StringBuilder statement = new StringBuilder();
            statement.append("DELETE FROM sharesbases WHERE base_path=?;");
            PreparedStatement ps = conn.prepareStatement(statement.toString());
            ps.setString(1, basePath);
            ps.executeUpdate();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }
}

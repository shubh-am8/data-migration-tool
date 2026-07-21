package com.migration.marketplace;

import com.migration.jobs.LabSchemas;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** Mutating operations on {@code migration_lab} — destination schema only. */
@Service
public class LabAdminService {
    private final String url;
    private final String user;
    private final String password;

    public LabAdminService(
        @Value("${app.lab-db.url:jdbc:postgresql://localhost:5433/migration_lab}") String url,
        @Value("${app.lab-db.user:migration}") String user,
        @Value("${app.lab-db.password:migration}") String password
    ) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    public void truncateTable(String schema, String table) throws SQLException {
        requireDestinationMutation(schema);
        validateIdent(table);
        try (Connection conn = connect(); Statement stmt = conn.createStatement()) {
            stmt.execute("TRUNCATE TABLE " + quoteIdent(schema) + "." + quoteIdent(table));
        }
    }

    public void dropTable(String schema, String table) throws SQLException {
        requireDestinationMutation(schema);
        validateIdent(table);
        try (Connection conn = connect(); Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS " + quoteIdent(schema) + "." + quoteIdent(table));
        }
    }

    public int truncateAllDestination() throws SQLException {
        return truncateOrDropAll(false);
    }

    public int dropAllDestination() throws SQLException {
        return truncateOrDropAll(true);
    }

    private int truncateOrDropAll(boolean drop) throws SQLException {
        List<String> tables = listDestinationTables();
        try (Connection conn = connect(); Statement stmt = conn.createStatement()) {
            for (String table : tables) {
                String sql = drop
                    ? "DROP TABLE IF EXISTS " + quoteIdent(LabSchemas.DESTINATION) + "." + quoteIdent(table)
                    : "TRUNCATE TABLE " + quoteIdent(LabSchemas.DESTINATION) + "." + quoteIdent(table);
                stmt.execute(sql);
            }
        }
        return tables.size();
    }

    private List<String> listDestinationTables() throws SQLException {
        try (Connection conn = connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("""
                 SELECT table_name FROM information_schema.tables
                 WHERE table_schema = '%s' AND table_type = 'BASE TABLE'
                 ORDER BY table_name
                 """.formatted(LabSchemas.DESTINATION))) {
            List<String> tables = new ArrayList<>();
            while (rs.next()) {
                tables.add(rs.getString(1));
            }
            return tables;
        }
    }

    private static void requireDestinationMutation(String schema) {
        if (!LabSchemas.DESTINATION.equals(schema)) {
            throw new IllegalArgumentException("Only " + LabSchemas.DESTINATION + " tables can be modified from the UI");
        }
    }

    private static void validateIdent(String ident) {
        if (!ident.matches("[a-z_][a-z0-9_]*")) {
            throw new IllegalArgumentException("Invalid table name: " + ident);
        }
    }

    private static String quoteIdent(String ident) {
        validateIdent(ident);
        return ident;
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }
}

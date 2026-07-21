package com.migration.marketplace;

import com.migration.jobs.LabSchemas;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** Introspects {@code migration_lab} (test_source / test_destination) for wizard + playground. */
@Service
public class LabIntrospectionService {
    private final String url;
    private final String user;
    private final String password;

    public LabIntrospectionService(
        @Value("${app.lab-db.url:jdbc:postgresql://localhost:5433/migration_lab}") String url,
        @Value("${app.lab-db.user:migration}") String user,
        @Value("${app.lab-db.password:migration}") String password
    ) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    public com.migration.connectors.SchemaInfo listSchemas() throws SQLException {
        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement("""
                 SELECT schema_name
                 FROM information_schema.schemata
                 WHERE schema_name IN (?, ?)
                 ORDER BY schema_name
                 """)) {
            ps.setString(1, LabSchemas.SOURCE);
            ps.setString(2, LabSchemas.DESTINATION);
            List<String> schemas = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    schemas.add(rs.getString(1));
                }
            }
            if (schemas.isEmpty()) {
                schemas.addAll(LabSchemas.ALL);
            }
            return new com.migration.connectors.SchemaInfo(List.copyOf(schemas));
        }
    }

    public com.migration.connectors.TableInfo listTables(String schema) throws SQLException {
        requireAllowedSchema(schema);
        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement("""
                 SELECT table_name, table_type
                 FROM information_schema.tables
                 WHERE table_schema = ? AND table_type IN ('BASE TABLE', 'VIEW', 'FOREIGN TABLE')
                 ORDER BY table_name
                 """)) {
            ps.setString(1, schema);
            List<com.migration.connectors.TableEntry> tables = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String type = rs.getString(2);
                    String kind = "FOREIGN TABLE".equals(type) ? "foreign" : type.toLowerCase().replace(' ', '_');
                    tables.add(new com.migration.connectors.TableEntry(rs.getString(1), kind, false, List.of()));
                }
            }
            return new com.migration.connectors.TableInfo(List.copyOf(tables));
        }
    }

    public com.migration.connectors.ColumnInfo listColumns(String schema, String table) throws SQLException {
        requireAllowedSchema(schema);
        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement("""
                 SELECT column_name, data_type, is_nullable
                 FROM information_schema.columns
                 WHERE table_schema = ? AND table_name = ?
                 ORDER BY ordinal_position
                 """)) {
            ps.setString(1, schema);
            ps.setString(2, table);
            List<com.migration.connectors.ColumnEntry> columns = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    columns.add(new com.migration.connectors.ColumnEntry(
                        rs.getString(1),
                        rs.getString(2),
                        "YES".equalsIgnoreCase(rs.getString(3)),
                        false
                    ));
                }
            }
            return new com.migration.connectors.ColumnInfo(List.copyOf(columns));
        }
    }

    public LabTableStats.SchemaStats tableStats(String schema) throws SQLException {
        requireAllowedSchema(schema);
        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement("""
                 SELECT c.relname AS table_name,
                        CASE c.relkind WHEN 'r' THEN 'table' WHEN 'v' THEN 'view' ELSE c.relkind::text END AS kind,
                        pg_total_relation_size(c.oid) AS size_bytes
                 FROM pg_class c
                 JOIN pg_namespace n ON n.oid = c.relnamespace
                 WHERE n.nspname = ? AND c.relkind IN ('r', 'v')
                 ORDER BY c.relname
                 """)) {
            ps.setString(1, schema);
            List<LabTableStats> tables = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String tableName = rs.getString(1);
                    long rowCount = countRows(conn, schema, tableName);
                    tables.add(new LabTableStats(
                        tableName,
                        rs.getString(2),
                        rowCount,
                        rs.getLong(3)
                    ));
                }
            }
            return new LabTableStats.SchemaStats(schema, List.copyOf(tables));
        }
    }

    private long countRows(Connection conn, String schema, String table) throws SQLException {
        String sql = "SELECT COUNT(*) FROM " + quoteIdent(schema) + "." + quoteIdent(table);
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            rs.next();
            return rs.getLong(1);
        }
    }

    private static String quoteIdent(String ident) {
        if (!ident.matches("[a-z_][a-z0-9_]*")) {
            throw new IllegalArgumentException("Invalid SQL identifier: " + ident);
        }
        return ident;
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    static void requireAllowedSchema(String schema) {
        if (!LabSchemas.ALL.contains(schema)) {
            throw new IllegalArgumentException(
                "Lab introspection only supports schemas: " + LabSchemas.SOURCE + ", " + LabSchemas.DESTINATION);
        }
    }
}

package com.migration.marketplace;

import com.migration.connectors.ColumnEntry;
import com.migration.connectors.ColumnInfo;
import com.migration.connectors.SchemaInfo;
import com.migration.connectors.TableEntry;
import com.migration.connectors.TableInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Introspects {@code migration_lab} (app/test schemas) for TEST-mode job wizard. */
@Service
public class LabIntrospectionService {
    private static final Set<String> ALLOWED_SCHEMAS = Set.of("app", "test");

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

    public SchemaInfo listSchemas() throws SQLException {
        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement("""
                 SELECT schema_name
                 FROM information_schema.schemata
                 WHERE schema_name IN ('app', 'test')
                 ORDER BY schema_name
                 """)) {
            List<String> schemas = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    schemas.add(rs.getString(1));
                }
            }
            return new SchemaInfo(List.copyOf(schemas));
        }
    }

    public TableInfo listTables(String schema) throws SQLException {
        requireAllowedSchema(schema);
        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement("""
                 SELECT table_name, table_type
                 FROM information_schema.tables
                 WHERE table_schema = ? AND table_type IN ('BASE TABLE', 'VIEW', 'FOREIGN TABLE')
                 ORDER BY table_name
                 """)) {
            ps.setString(1, schema);
            List<TableEntry> tables = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String type = rs.getString(2);
                    String kind = "FOREIGN TABLE".equals(type) ? "foreign" : type.toLowerCase().replace(' ', '_');
                    tables.add(new TableEntry(rs.getString(1), kind, false, List.of()));
                }
            }
            return new TableInfo(List.copyOf(tables));
        }
    }

    public ColumnInfo listColumns(String schema, String table) throws SQLException {
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
            List<ColumnEntry> columns = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    columns.add(new ColumnEntry(
                        rs.getString(1),
                        rs.getString(2),
                        "YES".equalsIgnoreCase(rs.getString(3)),
                        false
                    ));
                }
            }
            return new ColumnInfo(List.copyOf(columns));
        }
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    private static void requireAllowedSchema(String schema) {
        if (!ALLOWED_SCHEMAS.contains(schema)) {
            throw new IllegalArgumentException("Lab introspection only supports schemas: app, test");
        }
    }
}

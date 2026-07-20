package com.migration.connectors.postgresql;

import com.migration.connectors.*;

import java.sql.*;
import java.time.Duration;
import java.util.*;

public class PostgresqlConnectorPlugin implements ConnectorPlugin {

    static final List<ConfigField> FIELDS = List.of(
        new ConfigField("host", "Host", "text", true, "localhost"),
        new ConfigField("port", "Port", "number", true, "5432"),
        new ConfigField("database", "Database", "text", true, ""),
        new ConfigField("username", "Username", "text", true, ""),
        new ConfigField("password", "Password", "password", true, ""),
        new ConfigField("sslmode", "SSL Mode", "select", false, "prefer")
    );

    @Override
    public String id() {
        return "postgresql";
    }

    @Override
    public ConnectorMetadata metadata() {
        return new ConnectorMetadata(
            id(), "PostgreSQL",
            "Connect to PostgreSQL databases for schema introspection and data transfer",
            "1.0.0", "database", FIELDS
        );
    }

    @Override
    public ValidationResult validate(Map<String, String> config) {
        List<String> errors = new ArrayList<>();
        for (ConfigField f : FIELDS) {
            if (f.required() && (config.get(f.key()) == null || config.get(f.key()).isBlank())) {
                errors.add("Missing required field: " + f.key());
            }
        }
        return errors.isEmpty() ? ValidationResult.ok() : ValidationResult.fail(errors.toArray(String[]::new));
    }

    @Override
    public ConnectionTestResult testConnection(Map<String, String> config, Duration timeout) {
        long start = System.currentTimeMillis();
        try (Connection conn = openJdbc(config)) {
            conn.setReadOnly(true);
            try (Statement st = conn.createStatement()) {
                st.setQueryTimeout((int) timeout.getSeconds());
                try (ResultSet rs = st.executeQuery("SELECT 1")) {
                    rs.next();
                }
            }
            return new ConnectionTestResult(true, "Connection successful", System.currentTimeMillis() - start);
        } catch (Exception e) {
            return new ConnectionTestResult(false, e.getMessage(), System.currentTimeMillis() - start);
        }
    }

    @Override
    public ConnectionHandle connect(Map<String, String> config) {
        try {
            Connection conn = openJdbc(config);
            return new PgConnectionHandle(conn, config);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to connect: " + e.getMessage(), e);
        }
    }

    @Override
    public SchemaInfo listSchemas(ConnectionHandle conn) {
        List<String> schemas = new ArrayList<>();
        try (ResultSet rs = jdbc(conn).getMetaData().getSchemas()) {
            while (rs.next()) {
                String schema = rs.getString("TABLE_SCHEM");
                if (!"pg_catalog".equals(schema) && !"information_schema".equals(schema)) {
                    schemas.add(schema);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        Collections.sort(schemas);
        return new SchemaInfo(schemas);
    }

    @Override
    public TableInfo listTables(ConnectionHandle conn, String schema) {
        List<TableEntry> tables = new ArrayList<>();
        String sql = """
            SELECT c.relname AS name,
                   CASE c.relkind WHEN 'r' THEN 'table' WHEN 'p' THEN 'partitioned_table'
                        WHEN 'v' THEN 'view' WHEN 'm' THEN 'materialized_view' ELSE c.relkind::text END AS kind,
                   c.relkind IN ('p') AS partitioned
            FROM pg_class c
            JOIN pg_namespace n ON n.oid = c.relnamespace
            WHERE n.nspname = ? AND c.relkind IN ('r','p','v','m')
            ORDER BY c.relname
            """;
        try (PreparedStatement ps = jdbc(conn).prepareStatement(sql)) {
            ps.setString(1, schema);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    boolean partitioned = rs.getBoolean("partitioned");
                    List<String> partitions = partitioned
                        ? listPartitions(jdbc(conn), schema, rs.getString("name"))
                        : List.of();
                    tables.add(new TableEntry(rs.getString("name"), rs.getString("kind"), partitioned, partitions));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return new TableInfo(tables);
    }

    private List<String> listPartitions(Connection conn, String schema, String parent) throws SQLException {
        String sql = """
            SELECT c.relname FROM pg_class c
            JOIN pg_namespace n ON n.oid = c.relnamespace
            JOIN pg_inherits i ON i.inhrelid = c.oid
            JOIN pg_class p ON p.oid = i.inhparent
            WHERE n.nspname = ? AND p.relname = ?
            ORDER BY c.relname
            """;
        List<String> parts = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, schema);
            ps.setString(2, parent);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) parts.add(rs.getString(1));
            }
        }
        return parts;
    }

    @Override
    public ColumnInfo listColumns(ConnectionHandle conn, String schema, String table) {
        List<ColumnEntry> columns = new ArrayList<>();
        Set<String> pks;
        try {
            pks = primaryKeys(jdbc(conn), schema, table);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        String sql = """
            SELECT column_name, data_type, is_nullable
            FROM information_schema.columns
            WHERE table_schema = ? AND table_name = ?
            ORDER BY ordinal_position
            """;
        try (PreparedStatement ps = jdbc(conn).prepareStatement(sql)) {
            ps.setString(1, schema);
            ps.setString(2, table);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String name = rs.getString("column_name");
                    columns.add(new ColumnEntry(name, rs.getString("data_type"),
                        "YES".equals(rs.getString("is_nullable")), pks.contains(name)));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return new ColumnInfo(columns);
    }

    private Set<String> primaryKeys(Connection conn, String schema, String table) throws SQLException {
        Set<String> pks = new HashSet<>();
        try (ResultSet rs = conn.getMetaData().getPrimaryKeys(null, schema, table)) {
            while (rs.next()) pks.add(rs.getString("COLUMN_NAME"));
        }
        return pks;
    }

    @Override
    public ExplainResult explainScan(ConnectionHandle conn, QuerySpec query) {
        String sql = "EXPLAIN (FORMAT JSON) " + buildSelect(query);
        try (Statement st = jdbc(conn).createStatement()) {
            st.setQueryTimeout(30);
            try (ResultSet rs = st.executeQuery(sql)) {
                rs.next();
                String planJson = rs.getString(1);
                List<IndexRecommendation> recs = PgExplainParser.parseRecommendations(planJson, query.table());
                return new ExplainResult(planJson, recs);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public BatchReader openBatchReader(ConnectionHandle conn, CopySpec spec) {
        return new PgBatchReader(jdbc(conn), spec);
    }

    @Override
    public BatchWriter openBatchWriter(ConnectionHandle conn, CopySpec spec) {
        return new PgBatchWriter(jdbc(conn), spec);
    }

    @Override
    public long countRows(ConnectionHandle conn, QuerySpec query) {
        String sql = "SELECT COUNT(*) FROM " + quoteIdent(query.schema()) + "." + quoteIdent(query.table())
            + buildWhere(query);
        try (Statement st = jdbc(conn).createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getLong(1);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    static Connection openJdbc(Map<String, String> config) throws SQLException {
        String url = String.format("jdbc:postgresql://%s:%s/%s?sslmode=%s",
            config.get("host"), config.getOrDefault("port", "5432"),
            config.get("database"), config.getOrDefault("sslmode", "prefer"));
        return DriverManager.getConnection(url, config.get("username"), config.get("password"));
    }

    static Connection jdbc(ConnectionHandle conn) {
        return ((PgConnectionHandle) conn).connection();
    }

    static String buildSelect(QuerySpec query) {
        return "SELECT * FROM " + quoteIdent(query.schema()) + "." + quoteIdent(query.table()) + buildWhere(query);
    }

    static String buildWhere(QuerySpec query) {
        return PgSqlBuilder.buildWhere(query.filters(), query.hotColdFilter());
    }

    static String quoteIdent(String ident) {
        return "\"" + ident.replace("\"", "\"\"") + "\"";
    }
}

package com.migration.jobs;

import com.migration.connectors.ConnectionEntity;
import com.migration.connectors.ConnectionService;
import com.migration.connectors.ConnectorPluginRegistry;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Read-only check that a unique index exists for ON CONFLICT columns (PostgreSQL). */
@Service
public class ConflictIndexVerifier {
    private static final String INDEX_SQL = """
        SELECT i.relname AS index_name,
               array_agg(a.attname ORDER BY arr.ord) AS column_names
        FROM pg_class t
        JOIN pg_namespace n ON n.oid = t.relnamespace
        JOIN pg_index ix ON ix.indrelid = t.oid
        JOIN pg_class i ON i.oid = ix.indexrelid
        JOIN LATERAL unnest(ix.indkey) WITH ORDINALITY AS arr(attnum, ord) ON true
        JOIN pg_attribute a ON a.attrelid = t.oid AND a.attnum = arr.attnum
        WHERE n.nspname = ? AND t.relname = ? AND ix.indisunique
        GROUP BY i.relname
        """;

    private final ConnectionService connectionService;
    private final ConnectorPluginRegistry pluginRegistry;

    public ConflictIndexVerifier(ConnectionService connectionService, ConnectorPluginRegistry pluginRegistry) {
        this.connectionService = connectionService;
        this.pluginRegistry = pluginRegistry;
    }

    public Map<String, Object> verify(UUID connectionId, String schema, String table, List<String> columns)
        throws SQLException {
        List<String> logs = new ArrayList<>();
        if (columns == null || columns.isEmpty()) {
            return result(false, null, logs, null, "Select at least one ON CONFLICT column.");
        }
        for (String col : columns) validateIdent(col);
        validateIdent(schema);
        validateIdent(table);

        ConnectionEntity entity = connectionService.getEntity(connectionId);
        if (!"postgresql".equals(entity.getPluginId())) {
            return result(false, null, logs, null,
                "Conflict index verification is supported for PostgreSQL connections only.");
        }

        Map<String, String> config = connectionService.decryptConfig(entity);
        pluginRegistry.require(entity.getPluginId());

        Set<String> wanted = new HashSet<>(columns);
        logs.add("Checking unique indexes on " + schema + "." + table + " for columns: " + columns);

        try (Connection conn = DriverManager.getConnection(jdbcUrl(config), config.get("user"), config.get("password"));
             PreparedStatement ps = conn.prepareStatement(INDEX_SQL)) {
            ps.setString(1, schema);
            ps.setString(2, table);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String indexName = rs.getString("index_name");
                    Object arr = rs.getArray("column_names").getArray();
                    Set<String> indexCols = new HashSet<>();
                    if (arr instanceof String[] strings) {
                        indexCols.addAll(List.of(strings));
                    } else if (arr instanceof Object[] objects) {
                        for (Object o : objects) indexCols.add(String.valueOf(o));
                    }
                    logs.add("Found unique index " + indexName + " on (" + String.join(", ", indexCols) + ")");
                    if (indexCols.equals(wanted)) {
                        logs.add("Match: index columns match ON CONFLICT selection (order-independent).");
                        return result(true, indexName, logs, null, null);
                    }
                }
            }
        }

        String colsSql = columns.stream().map(this::quoteIdent).collect(Collectors.joining(", "));
        String suggested = "CREATE UNIQUE INDEX IF NOT EXISTS idx_"
            + table + "_conflict ON " + quoteIdent(schema) + "." + quoteIdent(table) + " (" + colsSql + ");";
        logs.add("No unique index matches the selected columns.");
        logs.add("Run this on the source database, then verify again:");
        logs.add(suggested);
        return result(false, null, logs, suggested, null);
    }

    private static Map<String, Object> result(boolean verified, String matchedIndex, List<String> logs,
                                                String suggestedSql, String error) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("verified", verified);
        map.put("matchedIndex", matchedIndex);
        map.put("logs", logs);
        map.put("suggestedSql", suggestedSql);
        if (error != null) map.put("error", error);
        return map;
    }

    private static String jdbcUrl(Map<String, String> config) {
        String host = config.getOrDefault("host", "localhost");
        String port = config.getOrDefault("port", "5432");
        String database = config.get("database");
        if (database == null || database.isBlank()) {
            throw new IllegalArgumentException("connection config missing database");
        }
        return "jdbc:postgresql://" + host + ":" + port + "/" + database;
    }

    private static void validateIdent(String ident) {
        if (!ident.matches("[a-z_][a-z0-9_]*")) {
            throw new IllegalArgumentException("Invalid identifier: " + ident);
        }
    }

    private String quoteIdent(String ident) {
        validateIdent(ident);
        return ident;
    }
}

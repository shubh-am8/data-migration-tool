package com.migration.connectors.postgresql;

import com.migration.connectors.BatchWriter;
import com.migration.connectors.CopySpec;
import com.migration.connectors.WriteResult;

import java.sql.*;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

final class PgBatchWriter implements BatchWriter {
    private final Connection conn;
    private final CopySpec spec;

    PgBatchWriter(Connection conn, CopySpec spec) {
        this.conn = conn;
        this.spec = spec;
    }

    @Override
    public WriteResult writeBatch(List<Map<String, Object>> rows) {
        if (rows.isEmpty()) return new WriteResult(0, 0, 0);
        List<String> cols = rows.get(0).keySet().stream().toList();
        String table = PostgresqlConnectorPlugin.quoteIdent(spec.schema()) + "."
            + PostgresqlConnectorPlugin.quoteIdent(spec.table());
        String colList = cols.stream().map(PostgresqlConnectorPlugin::quoteIdent).collect(Collectors.joining(", "));
        String placeholders = cols.stream().map(c -> "?").collect(Collectors.joining(", "));

        String conflictClause = buildConflictClause(cols);
        String sql = "INSERT INTO " + table + " (" + colList + ") VALUES (" + placeholders + ")" + conflictClause;

        long inserted = 0, updated = 0, skipped = 0;
        try {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (Map<String, Object> row : rows) {
                    for (int i = 0; i < cols.size(); i++) {
                        ps.setObject(i + 1, row.get(cols.get(i)));
                    }
                    int affected = ps.executeUpdate();
                    if ("DO_NOTHING".equals(spec.conflictMode())) {
                        skipped += affected == 0 ? 1 : 0;
                        inserted += affected > 0 ? 1 : 0;
                    } else {
                        // ponytail: PG doesn't distinguish insert vs update in affected rows for upsert
                        inserted += affected > 0 ? 1 : 0;
                    }
                }
            }
            conn.commit();
        } catch (SQLException e) {
            try { conn.rollback(); } catch (SQLException ignored) {}
            throw new RuntimeException(e);
        } finally {
            try { conn.setAutoCommit(true); } catch (SQLException ignored) {}
        }
        return new WriteResult(inserted, updated, skipped);
    }

    private String buildConflictClause(List<String> cols) {
        if (spec.conflictColumns() == null || spec.conflictColumns().isEmpty()) return "";
        String conflictCols = spec.conflictColumns().stream()
            .map(PostgresqlConnectorPlugin::quoteIdent).collect(Collectors.joining(", "));
        if ("DO_NOTHING".equals(spec.conflictMode())) {
            return " ON CONFLICT (" + conflictCols + ") DO NOTHING";
        }
        String updates = cols.stream()
            .filter(c -> !spec.conflictColumns().contains(c))
            .map(c -> PostgresqlConnectorPlugin.quoteIdent(c) + " = EXCLUDED." + PostgresqlConnectorPlugin.quoteIdent(c))
            .collect(Collectors.joining(", "));
        return " ON CONFLICT (" + conflictCols + ") DO UPDATE SET " + updates;
    }

    @Override
    public void close() {}
}

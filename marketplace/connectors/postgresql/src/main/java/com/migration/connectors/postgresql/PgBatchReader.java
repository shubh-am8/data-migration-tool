package com.migration.connectors.postgresql;

import com.migration.connectors.BatchReader;
import com.migration.connectors.CopySpec;
import com.migration.connectors.QuerySpec;

import java.sql.*;
import java.util.*;

final class PgBatchReader implements BatchReader {
    private final Connection conn;
    private final CopySpec spec;
    private final List<String> columns;
    private long offset;
    private long totalRead;
    private boolean done;

    PgBatchReader(Connection conn, CopySpec spec) {
        this.conn = conn;
        this.spec = spec;
        this.columns = spec.columns();
        this.offset = spec.cursorStart() != null ? Long.parseLong(spec.cursorStart()) : 0;
    }

    @Override
    public List<Map<String, Object>> readNextBatch() {
        if (done) return List.of();
        QuerySpec query = new QuerySpec(spec.schema(), spec.table(),
            spec.filters(), spec.hotColdFilter());
        String sql = PostgresqlConnectorPlugin.buildSelect(query)
            + " ORDER BY ctid LIMIT " + spec.batchSize() + " OFFSET " + offset;
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            List<Map<String, Object>> rows = new ArrayList<>();
            ResultSetMetaData meta = rs.getMetaData();
            int colCount = meta.getColumnCount();
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= colCount; i++) {
                    String name = meta.getColumnName(i);
                    if (columns.isEmpty() || columns.contains(name)) {
                        row.put(name, rs.getObject(i));
                    }
                }
                rows.add(row);
            }
            if (rows.size() < spec.batchSize()) done = true;
            offset += rows.size();
            totalRead += rows.size();
            return rows;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean hasMore() { return !done; }

    @Override
    public long rowsRead() { return totalRead; }

    @Override
    public void close() {}
}

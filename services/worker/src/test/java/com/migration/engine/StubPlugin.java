package com.migration.engine;

import com.migration.connectors.*;
import com.migration.jobs.PhaseType;

import java.time.Duration;
import java.util.List;
import java.util.Map;

class StubPlugin implements ConnectorPlugin {
    @Override public String id() { return "stub"; }
    @Override public ConnectorMetadata metadata() {
        return new ConnectorMetadata("stub", "Stub", "", "1", "", List.of());
    }
    @Override public ValidationResult validate(Map<String, String> config) { return ValidationResult.ok(); }
    @Override public ConnectionTestResult testConnection(Map<String, String> config, Duration timeout) {
        return new ConnectionTestResult(true, "ok", 0);
    }
    @Override public ConnectionHandle connect(Map<String, String> config) {
        return new ConnectionHandle() {
            @Override public String pluginId() { return "stub"; }
            @Override public Map<String, String> config() { return config; }
            @Override public void close() {}
        };
    }
    @Override public SchemaInfo listSchemas(ConnectionHandle conn) { return new SchemaInfo(List.of()); }
    @Override public TableInfo listTables(ConnectionHandle conn, String schema) { return new TableInfo(List.of()); }
    @Override public ColumnInfo listColumns(ConnectionHandle conn, String schema, String table) {
        return new ColumnInfo(List.of());
    }
    @Override public ExplainResult explainScan(ConnectionHandle conn, QuerySpec query) {
        return new ExplainResult("[]", List.of());
    }
    @Override public BatchReader openBatchReader(ConnectionHandle conn, CopySpec spec) {
        return new BatchReader() {
            boolean done;
            @Override public List<Map<String, Object>> readNextBatch() { done = true; return List.of(); }
            @Override public boolean hasMore() { return !done; }
            @Override public long rowsRead() { return 0; }
            @Override public void close() {}
        };
    }
    @Override public BatchWriter openBatchWriter(ConnectionHandle conn, CopySpec spec) {
        return new BatchWriter() {
            @Override public WriteResult writeBatch(List<Map<String, Object>> rows) {
                return new WriteResult(0, 0, 0);
            }
            @Override public void close() {}
        };
    }
    @Override public long countRows(ConnectionHandle conn, QuerySpec query) { return 0; }
}

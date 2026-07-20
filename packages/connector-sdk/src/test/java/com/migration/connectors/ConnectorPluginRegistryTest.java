package com.migration.connectors;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConnectorPluginRegistryTest {

    static class StubPlugin implements ConnectorPlugin {
        @Override public String id() { return "stub"; }
        @Override public ConnectorMetadata metadata() {
            return new ConnectorMetadata("stub", "Stub", "test", "1.0", "box", java.util.List.of());
        }
        @Override public ValidationResult validate(java.util.Map<String, String> config) { return ValidationResult.ok(); }
        @Override public ConnectionTestResult testConnection(java.util.Map<String, String> config, java.time.Duration timeout) {
            return new ConnectionTestResult(true, "ok", 1);
        }
        @Override public ConnectionHandle connect(java.util.Map<String, String> config) { return null; }
        @Override public SchemaInfo listSchemas(ConnectionHandle conn) { return null; }
        @Override public TableInfo listTables(ConnectionHandle conn, String schema) { return null; }
        @Override public ColumnInfo listColumns(ConnectionHandle conn, String schema, String table) { return null; }
        @Override public ExplainResult explainScan(ConnectionHandle conn, QuerySpec query) { return null; }
        @Override public BatchReader openBatchReader(ConnectionHandle conn, CopySpec spec) { return null; }
        @Override public BatchWriter openBatchWriter(ConnectionHandle conn, CopySpec spec) { return null; }
        @Override public long countRows(ConnectionHandle conn, QuerySpec query) { return 0; }
    }

    @Test
    void registersPluginExplicitly() {
        var registry = new ConnectorPluginRegistry(java.util.List.of(new StubPlugin()));
        assertTrue(registry.get("stub").isPresent());
        assertEquals("stub", registry.require("stub").id());
    }

    @Test
    void unknownPluginThrows() {
        var registry = new ConnectorPluginRegistry(java.util.List.of());
        assertThrows(IllegalArgumentException.class, () -> registry.require("missing"));
    }
}

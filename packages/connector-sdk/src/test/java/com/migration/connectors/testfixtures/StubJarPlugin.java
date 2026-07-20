package com.migration.connectors.testfixtures;

import com.migration.connectors.*;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/** Standalone (non-nested) fixture so PluginJarLoaderTest can package its compiled .class into a real JAR. */
public class StubJarPlugin implements ConnectorPlugin {
    @Override public String id() { return "stub-jar"; }

    @Override public ConnectorMetadata metadata() {
        return new ConnectorMetadata("stub-jar", "Stub Jar", "test fixture", "1.0", "box", List.of());
    }

    @Override public ValidationResult validate(Map<String, String> config) { return ValidationResult.ok(); }

    @Override public ConnectionTestResult testConnection(Map<String, String> config, Duration timeout) {
        return new ConnectionTestResult(true, "ok", 1);
    }

    @Override public ConnectionHandle connect(Map<String, String> config) { return null; }
    @Override public SchemaInfo listSchemas(ConnectionHandle conn) { return null; }
    @Override public TableInfo listTables(ConnectionHandle conn, String schema) { return null; }
    @Override public ColumnInfo listColumns(ConnectionHandle conn, String schema, String table) { return null; }
    @Override public ExplainResult explainScan(ConnectionHandle conn, QuerySpec query) { return null; }
    @Override public BatchReader openBatchReader(ConnectionHandle conn, CopySpec spec) { return null; }
    @Override public BatchWriter openBatchWriter(ConnectionHandle conn, CopySpec spec) { return null; }
    @Override public long countRows(ConnectionHandle conn, QuerySpec query) { return 0; }
}

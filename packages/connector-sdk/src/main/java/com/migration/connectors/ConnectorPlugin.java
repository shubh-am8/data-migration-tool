package com.migration.connectors;

import java.time.Duration;
import java.util.Map;

public interface ConnectorPlugin {
    String id();

    ConnectorMetadata metadata();

    ValidationResult validate(Map<String, String> config);

    ConnectionTestResult testConnection(Map<String, String> config, Duration timeout);

    ConnectionHandle connect(Map<String, String> config);

    SchemaInfo listSchemas(ConnectionHandle conn);

    TableInfo listTables(ConnectionHandle conn, String schema);

    ColumnInfo listColumns(ConnectionHandle conn, String schema, String table);

    ExplainResult explainScan(ConnectionHandle conn, QuerySpec query);

    BatchReader openBatchReader(ConnectionHandle conn, CopySpec spec);

    BatchWriter openBatchWriter(ConnectionHandle conn, CopySpec spec);

    long countRows(ConnectionHandle conn, QuerySpec query);
}

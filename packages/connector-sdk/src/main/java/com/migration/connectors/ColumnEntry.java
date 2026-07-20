package com.migration.connectors;

public record ColumnEntry(
    String name,
    String dataType,
    boolean nullable,
    boolean primaryKey
) {}

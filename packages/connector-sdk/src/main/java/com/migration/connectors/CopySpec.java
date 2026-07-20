package com.migration.connectors;

import java.util.List;

public record CopySpec(
    String schema,
    String table,
    List<String> columns,
    List<FilterSpec> filters,
    String hotColdFilter,
    String conflictMode,
    List<String> conflictColumns,
    int batchSize,
    String cursorStart,
    String cursorEnd
) {}

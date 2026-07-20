package com.migration.connectors;

import java.util.List;
import java.util.Map;

public record QuerySpec(
    String schema,
    String table,
    List<FilterSpec> filters,
    String hotColdFilter
) {}

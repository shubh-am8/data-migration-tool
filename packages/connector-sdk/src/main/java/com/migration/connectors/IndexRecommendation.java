package com.migration.connectors;

import java.util.List;

public record IndexRecommendation(
    String table,
    List<String> columns,
    String reason
) {}

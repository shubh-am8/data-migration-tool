package com.migration.connectors;

import java.util.List;

public record ExplainResult(
    String planJson,
    List<IndexRecommendation> recommendations
) {}

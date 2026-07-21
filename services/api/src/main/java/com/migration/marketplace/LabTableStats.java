package com.migration.marketplace;

import java.util.List;

public record LabTableStats(
    String name,
    String kind,
    long rowCount,
    long sizeBytes
) {
    public record SchemaStats(String schema, List<LabTableStats> tables) {}
}

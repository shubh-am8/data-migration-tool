package com.migration.connectors;

import java.util.List;

public record TableEntry(
    String name,
    String kind,
    boolean partitioned,
    List<String> partitions
) {}

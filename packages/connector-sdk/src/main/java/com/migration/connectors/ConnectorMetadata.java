package com.migration.connectors;

import java.util.List;
import java.util.Map;

public record ConnectorMetadata(
    String id,
    String name,
    String description,
    String version,
    String icon,
    List<ConfigField> configFields
) {}

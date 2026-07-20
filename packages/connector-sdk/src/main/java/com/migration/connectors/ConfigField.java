package com.migration.connectors;

public record ConfigField(
    String key,
    String label,
    String type,
    boolean required,
    String defaultValue
) {}

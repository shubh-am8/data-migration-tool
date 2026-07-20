package com.migration.connectors;

public record WriteResult(long inserted, long updated, long skipped) {}

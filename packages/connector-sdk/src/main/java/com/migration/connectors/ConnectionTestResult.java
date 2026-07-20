package com.migration.connectors;

public record ConnectionTestResult(boolean success, String message, long latencyMs) {}

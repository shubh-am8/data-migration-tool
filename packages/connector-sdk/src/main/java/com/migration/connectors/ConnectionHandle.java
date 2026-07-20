package com.migration.connectors;

import java.time.Duration;
import java.util.Map;

public interface ConnectionHandle extends AutoCloseable {
    String pluginId();
    Map<String, String> config();
}

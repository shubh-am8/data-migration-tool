package com.migration.connectors;

import java.util.List;
import java.util.Map;

public interface BatchWriter extends AutoCloseable {
    WriteResult writeBatch(List<Map<String, Object>> rows);
}

package com.migration.connectors;

import java.util.List;
import java.util.Map;

public interface BatchReader extends AutoCloseable {
    List<Map<String, Object>> readNextBatch();
    boolean hasMore();
    long rowsRead();
}

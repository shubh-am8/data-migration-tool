package com.migration.engine;

import com.migration.connectors.*;
import com.migration.jobs.ConflictMode;
import com.migration.jobs.JobEntity;

import java.util.List;
import java.util.Map;

public class BatchCopyEngine {
    private final ConnectorPlugin plugin;

    public BatchCopyEngine(ConnectorPlugin plugin) {
        this.plugin = plugin;
    }

    public BatchResult copyPhase(JobEntity job, Map<String, String> sourceConfig,
                                 Map<String, String> destConfig, ConflictMode conflictMode,
                                 List<FilterSpec> filters, String hotColdFilter,
                                 int batchSize) throws Exception {
        QuerySpec query = new QuerySpec(job.getSchemaName(), job.effectiveTable(), filters, hotColdFilter);

        long totalWritten = 0, totalSkipped = 0, totalUpdated = 0;

        try (ConnectionHandle srcConn = plugin.connect(sourceConfig);
             ConnectionHandle destConn = plugin.connect(destConfig)) {

            long sourceCount = plugin.countRows(srcConn, query);
            CopySpec spec = new CopySpec(
                job.getSchemaName(), job.effectiveTable(), List.of(), filters,
                hotColdFilter,
                conflictMode == ConflictMode.DO_NOTHING ? "DO_NOTHING" : "DO_UPDATE",
                job.getConflictColumns(), batchSize, null, null
            );

            try (BatchReader reader = plugin.openBatchReader(srcConn, spec);
                 BatchWriter writer = plugin.openBatchWriter(destConn, spec)) {
                while (reader.hasMore()) {
                    List<Map<String, Object>> batch = reader.readNextBatch();
                    if (batch.isEmpty()) break;
                    WriteResult result = writer.writeBatch(batch);
                    totalWritten += result.inserted();
                    totalSkipped += result.skipped();
                    totalUpdated += result.updated();
                }
            }
            return new BatchResult(sourceCount, totalWritten, totalSkipped, totalUpdated);
        }
    }

    public record BatchResult(long sourceCount, long written, long skipped, long updated) {}
}

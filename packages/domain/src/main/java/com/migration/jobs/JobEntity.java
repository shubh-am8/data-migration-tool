package com.migration.jobs;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "jobs")
public class JobEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(name = "source_connection_id", nullable = false)
    private UUID sourceConnectionId;

    @Column(name = "dest_connection_id", nullable = false)
    private UUID destConnectionId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "config_json", columnDefinition = "jsonb")
    private String configJson = "{}";

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "migration_mode", columnDefinition = "migration_mode")
    private MigrationMode migrationMode = MigrationMode.HOT_THEN_COLD;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(columnDefinition = "job_status")
    private JobStatus status = JobStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "run_mode", columnDefinition = "job_run_mode")
    private JobRunMode runMode = JobRunMode.TEST;

    @Column(name = "thread_count")
    private int threadCount = 1;

    @Column(name = "hot_days")
    private Integer hotDays;

    @Column(name = "range_start")
    private Instant rangeStart;

    @Enumerated(EnumType.STRING)
    @Column(name = "range_end_mode", nullable = false)
    private RangeEndMode rangeEndMode = RangeEndMode.NOW;

    @Column(name = "range_end")
    private Instant rangeEnd;

    @Column(name = "min_chunk_duration_hours", nullable = false)
    private Integer minChunkDurationHours = 24;

    @Column(name = "max_chunk_duration_hours", nullable = false)
    private Integer maxChunkDurationHours = 168;

    @Column(name = "ts_column")
    private String tsColumn;

    @Column(name = "schema_name")
    private String schemaName;

    @Column(name = "dest_schema_name")
    private String destSchemaName;

    @Column(name = "dest_table")
    private String destTable;

    @Column(name = "source_table")
    private String sourceTable;

    @Column(name = "is_partition")
    private boolean partition;

    @Column(name = "partition_name")
    private String partitionName;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "filters_json", columnDefinition = "jsonb")
    private String filtersJson = "[]";

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "conflict_columns", columnDefinition = "text[]")
    private List<String> conflictColumns = List.of();

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public UUID getSourceConnectionId() { return sourceConnectionId; }
    public void setSourceConnectionId(UUID sourceConnectionId) { this.sourceConnectionId = sourceConnectionId; }
    public UUID getDestConnectionId() { return destConnectionId; }
    public void setDestConnectionId(UUID destConnectionId) { this.destConnectionId = destConnectionId; }
    public String getConfigJson() { return configJson; }
    public void setConfigJson(String configJson) { this.configJson = configJson; }
    public MigrationMode getMigrationMode() { return migrationMode; }
    public void setMigrationMode(MigrationMode migrationMode) { this.migrationMode = migrationMode; }
    public JobStatus getStatus() { return status; }
    public void setStatus(JobStatus status) { this.status = status; }
    public JobRunMode getRunMode() { return runMode; }
    public void setRunMode(JobRunMode runMode) { this.runMode = runMode; }
    public int getThreadCount() { return threadCount; }
    public void setThreadCount(int threadCount) { this.threadCount = threadCount; }
    public Integer getHotDays() { return hotDays; }
    public void setHotDays(Integer hotDays) { this.hotDays = hotDays; }
    public Instant getRangeStart() { return rangeStart; }
    public void setRangeStart(Instant rangeStart) { this.rangeStart = rangeStart; }
    public RangeEndMode getRangeEndMode() { return rangeEndMode; }
    public void setRangeEndMode(RangeEndMode rangeEndMode) { this.rangeEndMode = rangeEndMode; }
    public Instant getRangeEnd() { return rangeEnd; }
    public void setRangeEnd(Instant rangeEnd) { this.rangeEnd = rangeEnd; }
    public Integer getMinChunkDurationHours() { return minChunkDurationHours; }
    public void setMinChunkDurationHours(Integer minChunkDurationHours) { this.minChunkDurationHours = minChunkDurationHours; }
    public Integer getMaxChunkDurationHours() { return maxChunkDurationHours; }
    public void setMaxChunkDurationHours(Integer maxChunkDurationHours) { this.maxChunkDurationHours = maxChunkDurationHours; }
    public String getTsColumn() { return tsColumn; }
    public void setTsColumn(String tsColumn) { this.tsColumn = tsColumn; }
    public String getSchemaName() { return schemaName; }
    public void setSchemaName(String schemaName) { this.schemaName = schemaName; }
    public String getDestSchemaName() { return destSchemaName; }
    public void setDestSchemaName(String destSchemaName) { this.destSchemaName = destSchemaName; }
    public String getDestTable() { return destTable; }
    public void setDestTable(String destTable) { this.destTable = destTable; }
    public String getSourceTable() { return sourceTable; }
    public void setSourceTable(String sourceTable) { this.sourceTable = sourceTable; }
    public boolean isPartition() { return partition; }
    public void setPartition(boolean partition) { this.partition = partition; }
    public String getPartitionName() { return partitionName; }
    public void setPartitionName(String partitionName) { this.partitionName = partitionName; }
    public String getFiltersJson() { return filtersJson; }
    public void setFiltersJson(String filtersJson) { this.filtersJson = filtersJson; }
    public List<String> getConflictColumns() { return conflictColumns; }
    public void setConflictColumns(List<String> conflictColumns) { this.conflictColumns = conflictColumns; }
    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public String effectiveTable() {
        return partition && partitionName != null ? partitionName : sourceTable;
    }
}

package com.migration.lab;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "lab_seed_sessions")
public class LabSeedSessionEntity {
    @Id
    @Column(name = "job_id")
    private UUID jobId;

    @Column(name = "schema_name", nullable = false)
    private String schemaName = "test_source";

    @Column(name = "table_name", nullable = false)
    private String tableName;

    @Column(nullable = false)
    private String scenario = "HOT_THEN_COLD";

    @Column(name = "inserts_per_minute", nullable = false)
    private int insertsPerMinute = 60;

    @Column(name = "updates_per_minute", nullable = false)
    private int updatesPerMinute = 12;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(nullable = false, columnDefinition = "lab_seed_status")
    private LabSeedStatus status = LabSeedStatus.PAUSED;

    @Column(name = "last_tick_at")
    private Instant lastTickAt;

    @Column(name = "rows_inserted", nullable = false)
    private long rowsInserted;

    @Column(name = "rows_updated", nullable = false)
    private long rowsUpdated;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public UUID getJobId() { return jobId; }
    public void setJobId(UUID jobId) { this.jobId = jobId; }
    public String getSchemaName() { return schemaName; }
    public void setSchemaName(String schemaName) { this.schemaName = schemaName; }
    public String getTableName() { return tableName; }
    public void setTableName(String tableName) { this.tableName = tableName; }
    public String getScenario() { return scenario; }
    public void setScenario(String scenario) { this.scenario = scenario; }
    public int getInsertsPerMinute() { return insertsPerMinute; }
    public void setInsertsPerMinute(int insertsPerMinute) { this.insertsPerMinute = insertsPerMinute; }
    public int getUpdatesPerMinute() { return updatesPerMinute; }
    public void setUpdatesPerMinute(int updatesPerMinute) { this.updatesPerMinute = updatesPerMinute; }
    public LabSeedStatus getStatus() { return status; }
    public void setStatus(LabSeedStatus status) { this.status = status; }
    public Instant getLastTickAt() { return lastTickAt; }
    public void setLastTickAt(Instant lastTickAt) { this.lastTickAt = lastTickAt; }
    public long getRowsInserted() { return rowsInserted; }
    public void setRowsInserted(long rowsInserted) { this.rowsInserted = rowsInserted; }
    public long getRowsUpdated() { return rowsUpdated; }
    public void setRowsUpdated(long rowsUpdated) { this.rowsUpdated = rowsUpdated; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}

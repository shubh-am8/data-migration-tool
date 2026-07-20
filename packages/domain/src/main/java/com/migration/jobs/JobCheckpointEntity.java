package com.migration.jobs;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "job_checkpoints")
public class JobCheckpointEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "job_id", nullable = false)
    private UUID jobId;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(columnDefinition = "phase_type")
    private PhaseType phase;

    @Column(name = "batch_key", nullable = false)
    private String batchKey;

    @Column(name = "last_cursor")
    private String lastCursor;

    @Column(name = "rows_processed", nullable = false)
    private long rowsProcessed;

    @Column(name = "committed_at", nullable = false)
    private Instant committedAt = Instant.now();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getJobId() { return jobId; }
    public void setJobId(UUID jobId) { this.jobId = jobId; }
    public PhaseType getPhase() { return phase; }
    public void setPhase(PhaseType phase) { this.phase = phase; }
    public String getBatchKey() { return batchKey; }
    public void setBatchKey(String batchKey) { this.batchKey = batchKey; }
    public String getLastCursor() { return lastCursor; }
    public void setLastCursor(String lastCursor) { this.lastCursor = lastCursor; }
    public long getRowsProcessed() { return rowsProcessed; }
    public void setRowsProcessed(long rowsProcessed) { this.rowsProcessed = rowsProcessed; }
    public Instant getCommittedAt() { return committedAt; }
    public void setCommittedAt(Instant committedAt) { this.committedAt = committedAt; }
}

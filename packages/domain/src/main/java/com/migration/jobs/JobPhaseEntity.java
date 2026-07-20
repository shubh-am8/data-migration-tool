package com.migration.jobs;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "job_phases")
public class JobPhaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "job_id", nullable = false)
    private UUID jobId;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(columnDefinition = "phase_type")
    private PhaseType phase;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(columnDefinition = "phase_status")
    private PhaseStatus status = PhaseStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "conflict_mode", columnDefinition = "conflict_mode")
    private ConflictMode conflictMode;

    @Column(name = "rows_processed")
    private long rowsProcessed;

    @Column(name = "rows_total")
    private Long rowsTotal;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getJobId() { return jobId; }
    public void setJobId(UUID jobId) { this.jobId = jobId; }
    public PhaseType getPhase() { return phase; }
    public void setPhase(PhaseType phase) { this.phase = phase; }
    public PhaseStatus getStatus() { return status; }
    public void setStatus(PhaseStatus status) { this.status = status; }
    public ConflictMode getConflictMode() { return conflictMode; }
    public void setConflictMode(ConflictMode conflictMode) { this.conflictMode = conflictMode; }
    public long getRowsProcessed() { return rowsProcessed; }
    public void setRowsProcessed(long rowsProcessed) { this.rowsProcessed = rowsProcessed; }
    public Long getRowsTotal() { return rowsTotal; }
    public void setRowsTotal(Long rowsTotal) { this.rowsTotal = rowsTotal; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
}

package com.migration.workers;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "worker_heartbeats")
public class WorkerHeartbeatEntity {
    @Id
    @Column(name = "worker_id")
    private String workerId;

    @Column(name = "active_threads")
    private int activeThreads;

    @Column(name = "current_job_id")
    private UUID currentJobId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "thread_details", columnDefinition = "jsonb")
    private String threadDetails = "[]";

    @Column(name = "last_seen")
    private Instant lastSeen = Instant.now();

    public String getWorkerId() { return workerId; }
    public void setWorkerId(String workerId) { this.workerId = workerId; }
    public int getActiveThreads() { return activeThreads; }
    public void setActiveThreads(int activeThreads) { this.activeThreads = activeThreads; }
    public UUID getCurrentJobId() { return currentJobId; }
    public void setCurrentJobId(UUID currentJobId) { this.currentJobId = currentJobId; }
    public String getThreadDetails() { return threadDetails; }
    public void setThreadDetails(String threadDetails) { this.threadDetails = threadDetails; }
    public Instant getLastSeen() { return lastSeen; }
    public void setLastSeen(Instant lastSeen) { this.lastSeen = lastSeen; }
}

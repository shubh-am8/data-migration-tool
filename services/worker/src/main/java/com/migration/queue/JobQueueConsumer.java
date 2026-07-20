package com.migration.queue;

import com.migration.connectors.postgresql.PostgresqlConnectorPlugin;
import com.migration.connectors.WorkerConnectionService;
import com.migration.engine.BatchCopyEngine;
import com.migration.engine.HotColdManager;
import com.migration.engine.ReconciliationService;
import com.migration.jobs.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class JobQueueConsumer {
    private static final Logger log = LoggerFactory.getLogger(JobQueueConsumer.class);

    private final StringRedisTemplate redis;
    private final WorkerJobRepository jobRepository;
    private final WorkerPhaseRepository phaseRepository;
    private final WorkerHeartbeatRepository heartbeatRepository;
    private final WorkerConnectionService connectionService;
    private final String workerId;
    private final String gspaceUrl;
    private final HotColdManager hotColdManager;
    private final ReconciliationService reconciliationService;
    private final RestClient restClient = RestClient.create();

    public JobQueueConsumer(StringRedisTemplate redis,
                            WorkerJobRepository jobRepository,
                            WorkerPhaseRepository phaseRepository,
                            WorkerHeartbeatRepository heartbeatRepository,
                            WorkerConnectionService connectionService,
                            @Value("${app.worker-id}") String workerId,
                            @Value("${app.gspace-webhook-url:}") String gspaceUrl) {
        this.redis = redis;
        this.jobRepository = jobRepository;
        this.phaseRepository = phaseRepository;
        this.heartbeatRepository = heartbeatRepository;
        this.connectionService = connectionService;
        this.workerId = workerId;
        this.gspaceUrl = gspaceUrl;
        var plugin = new PostgresqlConnectorPlugin();
        this.hotColdManager = new HotColdManager(new BatchCopyEngine(plugin));
        this.reconciliationService = new ReconciliationService();
    }

    @Scheduled(fixedDelayString = "${app.poll-interval-ms:2000}")
    public void poll() {
        heartbeat();
        String jobId = redis.opsForList().leftPop("job:queue");
        if (jobId == null) return;
        try {
            processJob(UUID.fromString(jobId));
        } catch (Exception e) {
            log.error("Job {} failed: {}", jobId, e.getMessage(), e);
            jobRepository.findById(UUID.fromString(jobId)).ifPresent(job -> {
                job.setStatus(JobStatus.FAILED);
                jobRepository.save(job);
                notifyGspace("Job *" + job.getName() + "* FAILED: " + e.getMessage());
            });
        }
    }

    // ponytail: no single long @Transactional here on purpose - each repository
    // save below auto-commits in its own transaction (Spring Data JPA default),
    // so status/progress becomes visible to readers as soon as each phase/step
    // finishes instead of only at the very end of the job.
    void processJob(UUID jobId) throws Exception {
        JobEntity job = jobRepository.findById(jobId).orElseThrow();
        job.setStatus(JobStatus.RUNNING);
        jobRepository.save(job);
        notifyGspace("Job *" + job.getName() + "* STARTED");

        List<JobPhaseEntity> phases = phaseRepository.findByJobId(jobId);
        var sourceConfig = connectionService.loadConfig(job.getSourceConnectionId());
        var destConfig = connectionService.loadConfig(job.getDestConnectionId());

        HotColdManager.CommandChecker checker = new HotColdManager.CommandChecker() {
            @Override public boolean isPaused() {
                return "PAUSE".equals(redis.opsForValue().get("job:" + jobId + ":command"));
            }
            @Override public boolean shouldStop() {
                return "CANCEL".equals(redis.opsForValue().get("job:" + jobId + ":command"));
            }
        };

        var results = hotColdManager.runJob(job, sourceConfig, destConfig, phases, checker);
        phaseRepository.saveAll(phases);

        for (var result : results) {
            var recon = reconciliationService.reconcile(
                result.batchResult().sourceCount(),
                result.batchResult().written(),
                result.batchResult().skipped(),
                result.batchResult().updated(),
                result.phase() == PhaseType.COLD);
            if (!recon.passed()) {
                job.setStatus(JobStatus.FAILED);
                jobRepository.save(job);
                notifyGspace("Job *" + job.getName() + "* RECONCILIATION_FAILED");
                return;
            }
        }

        job.setStatus(JobStatus.COMPLETED);
        jobRepository.save(job);
        notifyGspace("Job *" + job.getName() + "* COMPLETED");
    }

    private void heartbeat() {
        WorkerHeartbeatEntity hb = heartbeatRepository.findById(workerId).orElseGet(() -> {
            WorkerHeartbeatEntity w = new WorkerHeartbeatEntity();
            w.setWorkerId(workerId);
            return w;
        });
        hb.setLastSeen(Instant.now());
        heartbeatRepository.save(hb);
    }

    private void notifyGspace(String text) {
        if (gspaceUrl == null || gspaceUrl.isBlank()) {
            log.info("GSpace: {}", text);
            return;
        }
        try {
            restClient.post().uri(gspaceUrl)
                .body(Map.of("text", text))
                .retrieve().toBodilessEntity();
        } catch (Exception e) {
            log.warn("GSpace failed: {}", e.getMessage());
        }
    }
}

@Repository
interface WorkerJobRepository extends JpaRepository<JobEntity, UUID> {}

@Repository
interface WorkerPhaseRepository extends JpaRepository<JobPhaseEntity, UUID> {
    List<JobPhaseEntity> findByJobId(UUID jobId);
}

@Repository
interface WorkerHeartbeatRepository extends JpaRepository<WorkerHeartbeatEntity, String> {}

@jakarta.persistence.Entity
@jakarta.persistence.Table(name = "worker_heartbeats")
class WorkerHeartbeatEntity {
    @jakarta.persistence.Id
    @jakarta.persistence.Column(name = "worker_id")
    private String workerId;
    @jakarta.persistence.Column(name = "active_threads")
    private int activeThreads;
    @jakarta.persistence.Column(name = "current_job_id")
    private UUID currentJobId;
    @JdbcTypeCode(SqlTypes.JSON)
    @jakarta.persistence.Column(name = "thread_details", columnDefinition = "jsonb")
    private String threadDetails = "[]";
    @jakarta.persistence.Column(name = "last_seen")
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

package com.migration.queue;

import com.migration.connectors.ConnectorPlugin;
import com.migration.connectors.ConnectorPluginRegistry;
import com.migration.connectors.WorkerConnectionService;
import com.migration.engine.BatchCopyEngine;
import com.migration.engine.HotColdManager;
import com.migration.engine.ReconciliationService;
import com.migration.engine.SimulationEngine;
import com.migration.jobs.*;
import com.migration.lab.LabDestinationEnsurer;
import com.migration.simulation.SimulationConfig;
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

import java.sql.SQLException;
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
    private final WorkerCheckpointRepository checkpointRepository;
    private final WorkerJobEventRepository eventRepository;
    private final WorkerConnectionService connectionService;
    private final ConnectorPluginRegistry pluginRegistry;
    private final SimulationEngine simulationEngine;
    private final String workerId;
    private final String gspaceUrl;
    private final ReconciliationService reconciliationService;
    private final LabDestinationEnsurer labDestinationEnsurer;
    private final RestClient restClient = RestClient.create();

    public JobQueueConsumer(StringRedisTemplate redis,
                            WorkerJobRepository jobRepository,
                            WorkerPhaseRepository phaseRepository,
                            WorkerHeartbeatRepository heartbeatRepository,
                            WorkerCheckpointRepository checkpointRepository,
                            WorkerJobEventRepository eventRepository,
                            WorkerConnectionService connectionService,
                            ConnectorPluginRegistry pluginRegistry,
                            SimulationEngine simulationEngine,
                            @Value("${app.worker-id}") String workerId,
                            @Value("${app.gspace-webhook-url:}") String gspaceUrl,
                            @Value("${app.lab-db.url:jdbc:postgresql://localhost:5433/migration_lab}") String labDbUrl,
                            @Value("${app.lab-db.user:migration}") String labDbUser,
                            @Value("${app.lab-db.password:migration}") String labDbPassword) {
        this.redis = redis;
        this.jobRepository = jobRepository;
        this.phaseRepository = phaseRepository;
        this.heartbeatRepository = heartbeatRepository;
        this.checkpointRepository = checkpointRepository;
        this.eventRepository = eventRepository;
        this.connectionService = connectionService;
        this.pluginRegistry = pluginRegistry;
        this.simulationEngine = simulationEngine;
        this.workerId = workerId;
        this.gspaceUrl = gspaceUrl;
        this.reconciliationService = new ReconciliationService();
        this.labDestinationEnsurer = new LabDestinationEnsurer(labDbUrl, labDbUser, labDbPassword);
    }

    @Scheduled(fixedDelayString = "${app.poll-interval-ms:2000}")
    public void poll() {
        heartbeat();
        reclaimStaleRunningJobs();
        String jobId = redis.opsForList().leftPop("job:queue");
        if (jobId == null) return;
        try {
            processJob(UUID.fromString(jobId));
        } catch (Exception e) {
            log.error("Job {} failed: {}", jobId, e.getMessage(), e);
            jobRepository.findById(UUID.fromString(jobId)).ifPresent(job -> {
                job.setStatus(JobStatus.FAILED);
                jobRepository.save(job);
                writeEvent(job.getId(), "FAILED", e.getMessage());
                notifyFailureCard(job, null, e.getClass().getSimpleName(), e.getMessage());
            });
        }
    }

    private void reclaimStaleRunningJobs() {
        Instant cutoff = Instant.now().minus(2, java.time.temporal.ChronoUnit.MINUTES);
        for (JobEntity job : jobRepository.findByStatus(JobStatus.RUNNING)) {
            if (job.getUpdatedAt() != null && job.getUpdatedAt().isBefore(cutoff)) {
                job.setStatus(JobStatus.FAILED);
                jobRepository.save(job);
                writeEvent(job.getId(), "FAILED", "Reclaimed stale RUNNING job (worker lost)");
                notifyFailureCard(job, null, "StaleWorkerHeartbeat", "Reclaimed stale RUNNING job (worker lost)");
            }
        }
    }

    void processJob(UUID jobId) throws Exception {
        JobEntity job = jobRepository.findById(jobId).orElseThrow();
        job.setStatus(JobStatus.RUNNING);
        job.setUpdatedAt(Instant.now());
        jobRepository.save(job);
        writeEvent(jobId, "STARTED", null);
        notifyGspace("Job *" + job.getName() + "* STARTED");

        if (SimulationConfig.isSimulation(job.getConfigJson())) {
            runSimulation(job);
            return;
        }

        if (job.getRunMode() == JobRunMode.TEST) {
            labDestinationEnsurer.ensure(job);
            jobRepository.save(job);
        }

        List<JobPhaseEntity> phases = phaseRepository.findByJobId(jobId);
        String pluginId = connectionService.pluginId(job.getSourceConnectionId());
        ConnectorPlugin plugin = pluginRegistry.require(pluginId);
        HotColdManager hotColdManager = new HotColdManager(new BatchCopyEngine(plugin));

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

        HotColdManager.CheckpointSink sink = (phase, batchKey, cursor, rows) -> {
            JobCheckpointEntity cp = checkpointRepository
                .findByJobIdAndPhaseAndBatchKey(jobId, phase, batchKey)
                .orElseGet(JobCheckpointEntity::new);
            cp.setJobId(jobId);
            cp.setPhase(phase);
            cp.setBatchKey(batchKey);
            cp.setLastCursor(cursor);
            cp.setRowsProcessed(rows);
            cp.setCommittedAt(Instant.now());
            checkpointRepository.save(cp);
            writeEvent(jobId, "PROGRESS", phase + " " + batchKey + " rows=" + rows);
        };

        var results = hotColdManager.runJob(job, sourceConfig, destConfig, phases, checker, sink);
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
                job.setUpdatedAt(Instant.now());
                jobRepository.save(job);
                writeEvent(jobId, "FAILED", "RECONCILIATION_FAILED");
                notifyFailureCard(job, result.phase().name(), "ReconciliationFailure",
                    "source/dest counts did not reconcile");
                return;
            }
        }

        job.setStatus(JobStatus.COMPLETED);
        job.setUpdatedAt(Instant.now());
        jobRepository.save(job);
        writeEvent(jobId, "COMPLETED", null);
        notifyGspace("Job *" + job.getName() + "* COMPLETED");
    }

    /** SIMULATE jobs seed/touch lab DB sample rows directly — no plugin, connections, or phases. */
    private void runSimulation(JobEntity job) throws SQLException {
        SimulationConfig config = SimulationConfig.parse(job.getConfigJson());
        int hotDays = job.getHotDays() == null ? 0 : job.getHotDays();
        simulationEngine.run(config, hotDays);
        job.setStatus(JobStatus.COMPLETED);
        job.setUpdatedAt(Instant.now());
        jobRepository.save(job);
        writeEvent(job.getId(), "COMPLETED", null);
        notifyGspace("Job *" + job.getName() + "* COMPLETED");
    }

    private void writeEvent(UUID jobId, String type, String payload) {
        WorkerJobEventEntity ev = new WorkerJobEventEntity();
        ev.setJobId(jobId);
        ev.setEventType(type);
        ev.setPayload(payload == null ? null : "\"" + payload.replace("\"", "\\\"") + "\"");
        eventRepository.save(ev);
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

    private void notifyFailureCard(JobEntity job, String phase, String errorType, String message) {
        StringBuilder sb = new StringBuilder();
        sb.append("*Migration alert*\n");
        sb.append("• event: `FAILED`\n");
        sb.append("• jobId: `").append(job.getId()).append("`\n");
        sb.append("• job: *").append(job.getName()).append("*\n");
        if (phase != null) sb.append("• phase: `").append(phase).append("`\n");
        sb.append("• errorType: `").append(errorType).append("`\n");
        sb.append("• message: ").append(message).append("\n");
        sb.append("• worker: `").append(workerId).append("`\n");
        sb.append("• at: ").append(Instant.now());
        notifyGspace(sb.toString());
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
interface WorkerJobRepository extends JpaRepository<JobEntity, UUID> {
    List<JobEntity> findByStatus(JobStatus status);
}

@Repository
interface WorkerPhaseRepository extends JpaRepository<JobPhaseEntity, UUID> {
    List<JobPhaseEntity> findByJobId(UUID jobId);
}

@Repository
interface WorkerHeartbeatRepository extends JpaRepository<WorkerHeartbeatEntity, String> {}

@Repository
interface WorkerCheckpointRepository extends JpaRepository<JobCheckpointEntity, UUID> {
    java.util.Optional<JobCheckpointEntity> findByJobIdAndPhaseAndBatchKey(UUID jobId, PhaseType phase, String batchKey);
}

@Repository
interface WorkerJobEventRepository extends JpaRepository<WorkerJobEventEntity, UUID> {}

@jakarta.persistence.Entity
@jakarta.persistence.Table(name = "job_events")
class WorkerJobEventEntity {
    @jakarta.persistence.Id
    @jakarta.persistence.GeneratedValue(strategy = jakarta.persistence.GenerationType.UUID)
    private UUID id;
    @jakarta.persistence.Column(name = "job_id", nullable = false)
    private UUID jobId;
    @jakarta.persistence.Column(name = "event_type", nullable = false)
    private String eventType;
    @JdbcTypeCode(SqlTypes.JSON)
    @jakarta.persistence.Column(columnDefinition = "jsonb")
    private String payload;
    @jakarta.persistence.Column(name = "created_at")
    private Instant createdAt = Instant.now();

    public void setJobId(UUID jobId) { this.jobId = jobId; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public void setPayload(String payload) { this.payload = payload; }
}

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

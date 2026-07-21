package com.migration.jobs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.migration.common.PageResponse;
import com.migration.config.AppConfigService;
import com.migration.connectors.*;
import com.migration.connectors.ConnectionService;
import com.migration.notifications.GspaceNotifier;
import com.migration.simulation.SimulationConfig;
import com.migration.marketplace.LabDestinationProvisioner;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class JobService {
    private final JobRepository jobRepository;
    private final JobPhaseRepository phaseRepository;
    private final JobEventRepository eventRepository;
    private final AlertConfigRepository alertConfigRepository;
    private final ConnectionService connectionService;
    private final ConnectorPluginRegistry pluginRegistry;
    private final AppConfigService appConfigService;
    private final StringRedisTemplate redis;
    private final GspaceNotifier gspaceNotifier;
    private final LabDestinationProvisioner labDestinationProvisioner;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JobService(JobRepository jobRepository, JobPhaseRepository phaseRepository,
                      JobEventRepository eventRepository, AlertConfigRepository alertConfigRepository,
                      ConnectionService connectionService, ConnectorPluginRegistry pluginRegistry,
                      AppConfigService appConfigService, StringRedisTemplate redis,
                      GspaceNotifier gspaceNotifier,
                      LabDestinationProvisioner labDestinationProvisioner) {
        this.jobRepository = jobRepository;
        this.phaseRepository = phaseRepository;
        this.eventRepository = eventRepository;
        this.alertConfigRepository = alertConfigRepository;
        this.connectionService = connectionService;
        this.pluginRegistry = pluginRegistry;
        this.appConfigService = appConfigService;
        this.redis = redis;
        this.gspaceNotifier = gspaceNotifier;
        this.labDestinationProvisioner = labDestinationProvisioner;
    }

    public PageResponse<Map<String, Object>> list(Integer page, Integer size) {
        int p = PageResponse.clampPage(page);
        int s = PageResponse.clampSize(size);
        var result = jobRepository.findAll(PageRequest.of(p, s, Sort.by(Sort.Direction.DESC, "createdAt")));
        return PageResponse.of(result.getContent().stream().map(this::toDto).toList(), p, s, result.getTotalElements());
    }

    public Map<String, Object> get(UUID id) {
        return toDto(jobRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Job not found")));
    }

    @Transactional
    public Map<String, Object> create(Map<String, Object> body) {
        JobEntity job = new JobEntity();
        applyBody(job, body);
        validateJob(job, body);
        job = jobRepository.save(job);
        try {
            labDestinationProvisioner.provision(job);
            job = jobRepository.save(job);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to provision lab destination table: " + e.getMessage(), e);
        }
        initPhases(job);
        saveAlertConfig(job.getId(), body);
        logEvent(job.getId(), "CREATED", null);
        return toDto(job);
    }

    @Transactional
    public Map<String, Object> update(UUID id, Map<String, Object> body) {
        JobEntity job = jobRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Job not found"));
        if (job.getStatus() == JobStatus.RUNNING) {
            throw new IllegalStateException("Cannot edit running job");
        }
        applyBody(job, body);
        validateJob(job, body);
        job.setUpdatedAt(Instant.now());
        job = jobRepository.save(job);
        saveAlertConfig(job.getId(), body);
        return toDto(job);
    }

    @Transactional
    public void delete(UUID id) {
        jobRepository.deleteById(id);
    }

    public ExplainResult preflight(UUID id) throws Exception {
        JobEntity job = jobRepository.findById(id).orElseThrow();
        var plugin = pluginRegistry.require("postgresql");
        try (var conn = plugin.connect(connectionService.loadConfig(job.getSourceConnectionId()))) {
            QuerySpec query = buildQuerySpec(job, PhaseType.HOT);
            return plugin.explainScan(conn, query);
        }
    }

    @Transactional
    public Map<String, Object> start(UUID id) {
        JobEntity job = jobRepository.findById(id).orElseThrow();
        job.setStatus(JobStatus.PENDING);
        jobRepository.save(job);
        redis.opsForList().rightPush("job:queue", id.toString());
        redis.opsForValue().set("job:" + id + ":command", "START");
        logEvent(id, "STARTED", null);
        gspaceNotifier.sendLifecycle(id, "STARTED", job.getName());
        return toDto(job);
    }

    @Transactional
    public Map<String, Object> pause(UUID id) {
        JobEntity job = jobRepository.findById(id).orElseThrow();
        job.setStatus(JobStatus.PAUSED);
        jobRepository.save(job);
        redis.opsForValue().set("job:" + id + ":command", "PAUSE");
        logEvent(id, "PAUSED", null);
        gspaceNotifier.sendLifecycle(id, "PAUSED", job.getName());
        return toDto(job);
    }

    @Transactional
    public Map<String, Object> resume(UUID id) {
        JobEntity job = jobRepository.findById(id).orElseThrow();
        job.setStatus(JobStatus.RUNNING);
        jobRepository.save(job);
        redis.opsForValue().set("job:" + id + ":command", "RESUME");
        logEvent(id, "RESUMED", null);
        gspaceNotifier.sendLifecycle(id, "RESUMED", job.getName());
        return toDto(job);
    }

    @Transactional
    public Map<String, Object> cancel(UUID id) {
        JobEntity job = jobRepository.findById(id).orElseThrow();
        job.setStatus(JobStatus.CANCELLED);
        jobRepository.save(job);
        redis.opsForValue().set("job:" + id + ":command", "CANCEL");
        logEvent(id, "CANCELLED", null);
        return toDto(job);
    }

    public Map<String, Object> status(UUID id) {
        JobEntity job = jobRepository.findById(id).orElseThrow();
        List<JobPhaseEntity> phases = phaseRepository.findByJobId(id);
        List<JobEventEntity> events = eventRepository.findByJobIdOrderByCreatedAtDesc(id);
        Map<String, Object> result = new LinkedHashMap<>(toDto(job));
        result.put("phases", phases.stream().map(p -> Map.of(
            "phase", p.getPhase(), "status", p.getStatus(),
            "rowsProcessed", p.getRowsProcessed(), "rowsTotal", p.getRowsTotal() != null ? p.getRowsTotal() : 0
        )).toList());
        result.put("events", events.stream().limit(20).map(e -> Map.of(
            "type", e.getEventType(), "createdAt", e.getCreatedAt()
        )).toList());
        return result;
    }

    QuerySpec buildQuerySpec(JobEntity job, PhaseType phase) {
        String hotColdFilter = buildHotColdFilter(job, phase);
        return new QuerySpec(job.getSchemaName(), job.effectiveTable(), List.of(), hotColdFilter);
    }

    static String buildHotColdFilter(JobEntity job, PhaseType phase) {
        if (job.getTsColumn() == null || job.getHotDays() == null) return null;
        Instant effectiveEnd = TimeWindowMath.effectiveEnd(job, Instant.now());
        Instant hotBoundary = TimeWindowMath.hotBoundary(job, effectiveEnd);
        return switch (phase) {
            case HOT -> TimeWindowMath.timeRangeFilter(job.getTsColumn(), hotBoundary, effectiveEnd);
            case COLD -> TimeWindowMath.timeRangeFilter(job.getTsColumn(), job.getRangeStart(), hotBoundary);
        };
    }

    private void initPhases(JobEntity job) {
        switch (job.getMigrationMode()) {
            case HOT_ONLY -> createPhase(job.getId(), PhaseType.HOT, ConflictMode.DO_UPDATE);
            case COLD_ONLY -> createPhase(job.getId(), PhaseType.COLD, ConflictMode.DO_NOTHING);
            case HOT_THEN_COLD -> {
                createPhase(job.getId(), PhaseType.HOT, ConflictMode.DO_UPDATE);
                createPhase(job.getId(), PhaseType.COLD, ConflictMode.DO_NOTHING);
            }
        }
    }

    private void createPhase(UUID jobId, PhaseType phase, ConflictMode mode) {
        JobPhaseEntity entity = new JobPhaseEntity();
        entity.setJobId(jobId);
        entity.setPhase(phase);
        entity.setConflictMode(mode);
        phaseRepository.save(entity);
    }

    private void saveAlertConfig(UUID jobId, Map<String, Object> body) {
        AlertConfigEntity alert = alertConfigRepository.findById(jobId).orElseGet(() -> {
            AlertConfigEntity a = new AlertConfigEntity();
            a.setJobId(jobId);
            return a;
        });
        if (body.containsKey("lifecycleAlertsEnabled")) {
            alert.setLifecycleEnabled(Boolean.TRUE.equals(body.get("lifecycleAlertsEnabled")));
        }
        if (body.containsKey("progressIntervalMin")) {
            Object v = body.get("progressIntervalMin");
            alert.setProgressIntervalMin(v != null ? ((Number) v).intValue() : null);
        }
        if (body.containsKey("gspaceWebhookOverride")) {
            alert.setWebhookUrlOverride((String) body.get("gspaceWebhookOverride"));
        }
        alertConfigRepository.save(alert);
    }

    void validateJob(JobEntity job, Map<String, Object> body) {
        int min = Integer.parseInt(appConfigService.get("min_threads_per_job"));
        int max = Integer.parseInt(appConfigService.get("max_threads_per_job"));
        if (job.getThreadCount() < min || job.getThreadCount() > max) {
            throw new IllegalArgumentException("thread_count must be between " + min + " and " + max);
        }
        if (job.getTsColumn() == null || job.getTsColumn().isBlank()) {
            throw new IllegalArgumentException("ts_column is required for time-chunked migrations");
        }
        if ((job.getMigrationMode() == MigrationMode.HOT_ONLY || job.getMigrationMode() == MigrationMode.HOT_THEN_COLD)
            && job.getHotDays() == null) {
            throw new IllegalArgumentException("hot migration requires hot_days");
        }
        if (job.getConflictColumns() == null || job.getConflictColumns().isEmpty()) {
            throw new IllegalArgumentException("conflict_columns required");
        }
        validateRangeChunks(job);
        validateRunMode(job);
    }

    private void validateRunMode(JobEntity job) {
        if (job.getSourceConnectionId() == null || job.getDestConnectionId() == null) return;
        JobRunMode mode = job.getRunMode() == null ? JobRunMode.TEST : job.getRunMode();
        boolean sourceSandbox = connectionService.getEntity(job.getSourceConnectionId()).isSandbox();
        boolean destSandbox = connectionService.getEntity(job.getDestConnectionId()).isSandbox();
        JobRunModeGuard.validate(mode, sourceSandbox, destSandbox, job.getSchemaName(), isSimulation(job));
    }

    private boolean isSimulation(JobEntity job) {
        return SimulationConfig.isSimulation(job.getConfigJson());
    }

    static void validateRangeChunks(JobEntity job) {
        int min = job.getMinChunkDurationHours() == null ? 24 : job.getMinChunkDurationHours();
        int max = job.getMaxChunkDurationHours() == null ? 168 : job.getMaxChunkDurationHours();
        if (min < 1 || max < min) throw new IllegalArgumentException("invalid chunk duration hours");
        RangeEndMode mode = job.getRangeEndMode() == null ? RangeEndMode.NOW : job.getRangeEndMode();
        if (mode == RangeEndMode.FIXED && job.getRangeEnd() == null) {
            throw new IllegalArgumentException("rangeEnd required when rangeEndMode is FIXED");
        }
        if (job.getMigrationMode() != MigrationMode.HOT_ONLY) {
            if (job.getRangeStart() == null) throw new IllegalArgumentException("rangeStart required for cold migration");
        }
        if (job.getRangeStart() != null && job.getRangeEnd() != null && !job.getRangeEnd().isAfter(job.getRangeStart())) {
            throw new IllegalArgumentException("rangeEnd must be after rangeStart");
        }
    }

    @SuppressWarnings("unchecked")
    void applyBody(JobEntity job, Map<String, Object> body) {
        if (body.containsKey("name")) job.setName((String) body.get("name"));
        if (body.containsKey("sourceConnectionId")) job.setSourceConnectionId(UUID.fromString((String) body.get("sourceConnectionId")));
        if (body.containsKey("destConnectionId")) job.setDestConnectionId(UUID.fromString((String) body.get("destConnectionId")));
        if (body.containsKey("migrationMode")) job.setMigrationMode(MigrationMode.valueOf((String) body.get("migrationMode")));
        if (body.containsKey("runMode")) job.setRunMode(JobRunMode.valueOf((String) body.get("runMode")));
        if (body.containsKey("threadCount")) job.setThreadCount(((Number) body.get("threadCount")).intValue());
        if (body.containsKey("hotDays")) job.setHotDays(((Number) body.get("hotDays")).intValue());
        if (body.containsKey("rangeStart")) job.setRangeStart(body.get("rangeStart") == null ? null : Instant.parse((String) body.get("rangeStart")));
        if (body.containsKey("rangeEndMode")) job.setRangeEndMode(RangeEndMode.valueOf((String) body.get("rangeEndMode")));
        if (body.containsKey("rangeEnd")) job.setRangeEnd(body.get("rangeEnd") == null ? null : Instant.parse((String) body.get("rangeEnd")));
        if (body.containsKey("minChunkDurationHours")) job.setMinChunkDurationHours(((Number) body.get("minChunkDurationHours")).intValue());
        if (body.containsKey("maxChunkDurationHours")) job.setMaxChunkDurationHours(((Number) body.get("maxChunkDurationHours")).intValue());
        if (body.containsKey("tsColumn")) job.setTsColumn((String) body.get("tsColumn"));
        if (body.containsKey("schemaName")) job.setSchemaName((String) body.get("schemaName"));
        if (body.containsKey("sourceTable")) job.setSourceTable((String) body.get("sourceTable"));
        if (body.containsKey("isPartition")) job.setPartition(Boolean.TRUE.equals(body.get("isPartition")));
        if (body.containsKey("partitionName")) job.setPartitionName((String) body.get("partitionName"));
        if (body.containsKey("conflictColumns")) job.setConflictColumns((List<String>) body.get("conflictColumns"));
        if (body.containsKey("filters")) {
            try {
                job.setFiltersJson(objectMapper.writeValueAsString(body.get("filters")));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        if (body.containsKey("configJson")) {
            Object configJson = body.get("configJson");
            try {
                job.setConfigJson(configJson == null ? "{}" : objectMapper.writeValueAsString(configJson));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    void logEvent(UUID jobId, String type, String payload) {
        JobEventEntity event = new JobEventEntity();
        event.setJobId(jobId);
        event.setEventType(type);
        event.setPayload(payload);
        eventRepository.save(event);
    }

    private Map<String, Object> toDto(JobEntity job) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", job.getId());
        dto.put("name", job.getName());
        dto.put("sourceConnectionId", job.getSourceConnectionId());
        dto.put("destConnectionId", job.getDestConnectionId());
        dto.put("migrationMode", job.getMigrationMode());
        dto.put("runMode", job.getRunMode());
        dto.put("status", job.getStatus());
        dto.put("threadCount", job.getThreadCount());
        dto.put("hotDays", job.getHotDays());
        dto.put("rangeStart", job.getRangeStart());
        dto.put("rangeEndMode", job.getRangeEndMode());
        dto.put("rangeEnd", job.getRangeEnd());
        dto.put("minChunkDurationHours", job.getMinChunkDurationHours());
        dto.put("maxChunkDurationHours", job.getMaxChunkDurationHours());
        dto.put("tsColumn", job.getTsColumn());
        dto.put("schemaName", job.getSchemaName());
        dto.put("destSchemaName", job.getDestSchemaName());
        dto.put("destTable", job.getDestTable());
        dto.put("sourceTable", job.getSourceTable());
        dto.put("isPartition", job.isPartition());
        dto.put("partitionName", job.getPartitionName());
        dto.put("conflictColumns", job.getConflictColumns());
        dto.put("createdAt", job.getCreatedAt());
        dto.put("updatedAt", job.getUpdatedAt());
        return dto;
    }
}

package com.migration.marketplace;

import com.migration.jobs.JobEntity;
import com.migration.jobs.JobRepository;
import com.migration.jobs.JobRunMode;
import com.migration.jobs.LabSchemas;
import com.migration.lab.LabSeedSessionEntity;
import com.migration.lab.LabSeedStatus;
import com.migration.simulation.SimulationConfig;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class LabSeedSessionService {
    private final LabSeedSessionRepository repository;
    private final JobRepository jobRepository;

    public LabSeedSessionService(LabSeedSessionRepository repository, JobRepository jobRepository) {
        this.repository = repository;
        this.jobRepository = jobRepository;
    }

    public List<Map<String, Object>> list() {
        return repository.findAll().stream().map(this::toDto).toList();
    }

    @Transactional
    public void createForJob(JobEntity job) {
        if (job.getRunMode() != JobRunMode.TEST) return;
        if (SimulationConfig.isSimulation(job.getConfigJson())) return;
        if (job.getSchemaName() == null || job.getSourceTable() == null) return;
        if (!LabSchemas.SOURCE.equals(job.getSchemaName())) return;
        if (repository.existsById(job.getId())) return;

        LabSeedSessionEntity session = new LabSeedSessionEntity();
        session.setJobId(job.getId());
        session.setSchemaName(job.getSchemaName());
        session.setTableName(job.effectiveTable());
        session.setScenario(mapScenario(job.getMigrationMode().name()));
        repository.save(session);
    }

    @Transactional
    public Map<String, Object> start(UUID jobId) {
        return transition(jobId, LabSeedStatus.RUNNING);
    }

    @Transactional
    public Map<String, Object> pause(UUID jobId) {
        return transition(jobId, LabSeedStatus.PAUSED);
    }

    @Transactional
    public Map<String, Object> resume(UUID jobId) {
        LabSeedSessionEntity session = requireSession(jobId);
        if (session.getStatus() == LabSeedStatus.STOPPED) {
            throw new IllegalStateException("Cannot resume a stopped seed session");
        }
        session.setStatus(LabSeedStatus.RUNNING);
        return toDto(repository.save(session));
    }

    @Transactional
    public Map<String, Object> stop(UUID jobId) {
        return transition(jobId, LabSeedStatus.STOPPED);
    }

    @Transactional
    public Map<String, Object> updateRates(UUID jobId, Integer insertsPerMinute, Integer updatesPerMinute) {
        LabSeedSessionEntity session = requireSession(jobId);
        if (insertsPerMinute != null) {
            if (insertsPerMinute < 0 || insertsPerMinute > 10_000) {
                throw new IllegalArgumentException("inserts_per_minute must be 0–10000");
            }
            session.setInsertsPerMinute(insertsPerMinute);
        }
        if (updatesPerMinute != null) {
            if (updatesPerMinute < 0 || updatesPerMinute > 10_000) {
                throw new IllegalArgumentException("updates_per_minute must be 0–10000");
            }
            session.setUpdatesPerMinute(updatesPerMinute);
        }
        return toDto(repository.save(session));
    }

    @Transactional
    public void stopAllRunning() {
        for (LabSeedSessionEntity session : repository.findByStatus(LabSeedStatus.RUNNING)) {
            session.setStatus(LabSeedStatus.STOPPED);
            repository.save(session);
        }
    }

    private Map<String, Object> transition(UUID jobId, LabSeedStatus status) {
        LabSeedSessionEntity session = requireSession(jobId);
        session.setStatus(status);
        return toDto(repository.save(session));
    }

    private LabSeedSessionEntity requireSession(UUID jobId) {
        return repository.findById(jobId).orElseThrow(() -> new IllegalArgumentException("Seed session not found"));
    }

    private Map<String, Object> toDto(LabSeedSessionEntity session) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("jobId", session.getJobId());
        dto.put("jobName", jobRepository.findById(session.getJobId()).map(JobEntity::getName).orElse("—"));
        dto.put("schemaName", session.getSchemaName());
        dto.put("tableName", session.getTableName());
        dto.put("scenario", session.getScenario());
        dto.put("insertsPerMinute", session.getInsertsPerMinute());
        dto.put("updatesPerMinute", session.getUpdatesPerMinute());
        dto.put("status", session.getStatus());
        dto.put("lastTickAt", session.getLastTickAt());
        dto.put("rowsInserted", session.getRowsInserted());
        dto.put("rowsUpdated", session.getRowsUpdated());
        dto.put("createdAt", session.getCreatedAt());
        return dto;
    }

    private static String mapScenario(String migrationMode) {
        return switch (migrationMode) {
            case "COLD_ONLY" -> "COLD_ONLY";
            case "HOT_ONLY" -> "HOT_ONLY";
            case "COLD_THEN_HOT" -> "COLD_THEN_HOT";
            case "HOT_THEN_COLD" -> "HOT_THEN_COLD";
            default -> "HOT_THEN_COLD";
        };
    }
}

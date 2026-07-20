package com.migration.engine;

import com.migration.jobs.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HotColdManager {
    public static final int ROW_BATCH_SIZE = 5000;

    private final BatchCopyEngine batchCopyEngine;

    public HotColdManager(BatchCopyEngine batchCopyEngine) {
        this.batchCopyEngine = batchCopyEngine;
    }

    public List<PhaseResult> runJob(JobEntity job, Map<String, String> sourceConfig,
                                    Map<String, String> destConfig,
                                    List<JobPhaseEntity> phases,
                                    CommandChecker commandChecker) throws Exception {
        return runJob(job, sourceConfig, destConfig, phases, commandChecker, null);
    }

    public List<PhaseResult> runJob(JobEntity job, Map<String, String> sourceConfig,
                                    Map<String, String> destConfig,
                                    List<JobPhaseEntity> phases,
                                    CommandChecker commandChecker,
                                    CheckpointSink checkpointSink) throws Exception {
        Instant now = Instant.now();
        Instant effectiveEnd = JobEntityHotCold.effectiveEnd(job, now);
        Instant hotBoundary = JobEntityHotCold.hotBoundary(job, effectiveEnd);
        int minHours = job.getMinChunkDurationHours() == null ? 24 : job.getMinChunkDurationHours();
        int maxHours = job.getMaxChunkDurationHours() == null ? 168 : job.getMaxChunkDurationHours();

        List<PhaseResult> results = new ArrayList<>();
        for (JobPhaseEntity phase : orderedPhases(job.getMigrationMode(), phases)) {
            if (commandChecker.shouldStop()) break;
            if (commandChecker.isPaused()) {
                waitForResume(commandChecker);
            }
            phase.setStatus(PhaseStatus.RUNNING);
            PhaseChunkResult chunkResult = runPhaseChunks(
                job, sourceConfig, destConfig, phase, effectiveEnd, hotBoundary,
                minHours, maxHours, commandChecker, checkpointSink);
            BatchCopyEngine.BatchResult batchResult = chunkResult.batchResult();
            phase.setRowsProcessed(batchResult.written() + batchResult.skipped() + batchResult.updated());
            phase.setRowsTotal(batchResult.sourceCount());
            if (chunkResult.completed()) {
                phase.setStatus(PhaseStatus.COMPLETED);
            }
            results.add(new PhaseResult(phase.getPhase(), batchResult));
        }
        return results;
    }

    private PhaseChunkResult runPhaseChunks(JobEntity job, Map<String, String> sourceConfig,
            Map<String, String> destConfig, JobPhaseEntity phase, Instant effectiveEnd, Instant hotBoundary,
            int minHours, int maxHours, CommandChecker commandChecker,
            CheckpointSink checkpointSink) throws Exception {
        if (job.getTsColumn() == null) {
            BatchCopyEngine.BatchResult result = batchCopyEngine.copyPhase(job, sourceConfig, destConfig,
                phase.getConflictMode(), List.of(), null, ROW_BATCH_SIZE);
            if (checkpointSink != null) {
                checkpointSink.save(phase.getPhase(), "full", null, result.written());
            }
            return new PhaseChunkResult(result, true);
        }

        Instant windowStart = phase.getPhase() == PhaseType.HOT ? hotBoundary : job.getRangeStart();
        Instant windowEnd = phase.getPhase() == PhaseType.HOT ? effectiveEnd : hotBoundary;
        List<TimeChunkPlanner.TimeChunk> chunks = TimeChunkPlanner.plan(windowStart, windowEnd, minHours, maxHours);

        long sourceCount = 0, written = 0, skipped = 0, updated = 0;
        boolean completed = true;
        int idx = 0;
        for (TimeChunkPlanner.TimeChunk chunk : chunks) {
            if (commandChecker.shouldStop()) {
                completed = false;
                break;
            }
            String filter = JobEntityHotCold.timeRangeFilter(job.getTsColumn(), chunk.start(), chunk.end());
            BatchCopyEngine.BatchResult result = batchCopyEngine.copyPhase(job, sourceConfig, destConfig,
                phase.getConflictMode(), List.of(), filter, ROW_BATCH_SIZE);
            sourceCount += result.sourceCount();
            written += result.written();
            skipped += result.skipped();
            updated += result.updated();
            if (checkpointSink != null) {
                checkpointSink.save(phase.getPhase(), "chunk-" + idx, chunk.end().toString(), written);
            }
            idx++;
        }
        return new PhaseChunkResult(new BatchCopyEngine.BatchResult(sourceCount, written, skipped, updated), completed);
    }

    private List<JobPhaseEntity> orderedPhases(MigrationMode mode, List<JobPhaseEntity> phases) {
        List<JobPhaseEntity> ordered = new ArrayList<>();
        if (mode == MigrationMode.COLD_ONLY || mode == MigrationMode.HOT_THEN_COLD || mode == MigrationMode.HOT_ONLY) {
            phases.stream().filter(p -> p.getPhase() == PhaseType.HOT).findFirst().ifPresent(ordered::add);
        }
        if (mode == MigrationMode.COLD_ONLY || mode == MigrationMode.HOT_THEN_COLD) {
            phases.stream().filter(p -> p.getPhase() == PhaseType.COLD).findFirst().ifPresent(ordered::add);
        }
        if (mode == MigrationMode.COLD_ONLY) {
            ordered.clear();
            phases.stream().filter(p -> p.getPhase() == PhaseType.COLD).forEach(ordered::add);
        }
        return ordered;
    }

    private void waitForResume(CommandChecker checker) throws InterruptedException {
        while (checker.isPaused() && !checker.shouldStop()) {
            Thread.sleep(1000);
        }
    }

    public interface CommandChecker {
        boolean isPaused();
        boolean shouldStop();
    }

    @FunctionalInterface
    public interface CheckpointSink {
        void save(PhaseType phase, String batchKey, String cursor, long rowsProcessed);
    }

    public record PhaseResult(PhaseType phase, BatchCopyEngine.BatchResult batchResult) {}

    private record PhaseChunkResult(BatchCopyEngine.BatchResult batchResult, boolean completed) {}
}

package com.migration.engine;

import com.migration.jobs.*;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class HotColdManagerTest {

    @Test
    void coldThenHotRunsColdFirst() throws Exception {
        var engine = new BatchCopyEngine(new StubPlugin());
        var manager = new HotColdManager(engine);

        JobEntity job = new JobEntity();
        job.setMigrationMode(MigrationMode.COLD_THEN_HOT);
        job.setSchemaName("public");
        job.setSourceTable("t");
        job.setTsColumn("created_at");
        job.setHotDays(7);
        job.setRangeStart(Instant.now().minus(Duration.ofDays(30)));
        job.setMinChunkDurationHours(24);
        job.setMaxChunkDurationHours(168);
        job.setConflictColumns(java.util.List.of("id"));

        JobPhaseEntity hot = new JobPhaseEntity();
        hot.setPhase(PhaseType.HOT);
        hot.setConflictMode(ConflictMode.DO_UPDATE);
        JobPhaseEntity cold = new JobPhaseEntity();
        cold.setPhase(PhaseType.COLD);
        cold.setConflictMode(ConflictMode.DO_NOTHING);

        var checker = new HotColdManager.CommandChecker() {
            @Override public boolean isPaused() { return false; }
            @Override public boolean shouldStop() { return false; }
        };

        var results = manager.runJob(job, java.util.Map.of(), java.util.Map.of(),
            java.util.List.of(hot, cold), checker);
        assertEquals(2, results.size());
        assertEquals(PhaseType.COLD, results.get(0).phase());
        assertEquals(PhaseType.HOT, results.get(1).phase());
    }

    @Test
    void hotThenColdRunsHotFirst() throws Exception {
        var engine = new BatchCopyEngine(new StubPlugin());
        var manager = new HotColdManager(engine);

        JobEntity job = new JobEntity();
        job.setMigrationMode(MigrationMode.HOT_THEN_COLD);
        job.setSchemaName("public");
        job.setSourceTable("t");
        job.setTsColumn("created_at");
        job.setHotDays(7);
        job.setRangeStart(Instant.now().minus(Duration.ofDays(30)));
        job.setMinChunkDurationHours(24);
        job.setMaxChunkDurationHours(168);
        job.setConflictColumns(java.util.List.of("id"));

        JobPhaseEntity hot = new JobPhaseEntity();
        hot.setPhase(PhaseType.HOT);
        hot.setConflictMode(ConflictMode.DO_UPDATE);
        JobPhaseEntity cold = new JobPhaseEntity();
        cold.setPhase(PhaseType.COLD);
        cold.setConflictMode(ConflictMode.DO_NOTHING);

        var checker = new HotColdManager.CommandChecker() {
            @Override public boolean isPaused() { return false; }
            @Override public boolean shouldStop() { return false; }
        };

        var results = manager.runJob(job, java.util.Map.of(), java.util.Map.of(),
            java.util.List.of(hot, cold), checker);
        assertEquals(2, results.size());
        assertEquals(PhaseType.HOT, results.get(0).phase());
        assertEquals(PhaseType.COLD, results.get(1).phase());
    }

    @Test
    void runsSingleUnfilteredCopyWhenNoTsColumnConfigured() throws Exception {
        var engine = new BatchCopyEngine(new StubPlugin());
        var manager = new HotColdManager(engine);

        JobEntity job = new JobEntity();
        job.setMigrationMode(MigrationMode.COLD_ONLY);
        job.setSchemaName("public");
        job.setSourceTable("t");
        job.setRangeStart(Instant.now().minus(Duration.ofDays(30)));
        job.setConflictColumns(java.util.List.of("id"));

        JobPhaseEntity cold = new JobPhaseEntity();
        cold.setPhase(PhaseType.COLD);
        cold.setConflictMode(ConflictMode.DO_NOTHING);

        var checker = new HotColdManager.CommandChecker() {
            @Override public boolean isPaused() { return false; }
            @Override public boolean shouldStop() { return false; }
        };

        var results = manager.runJob(job, java.util.Map.of(), java.util.Map.of(),
            java.util.List.of(cold), checker);
        assertEquals(1, results.size());
        assertEquals(PhaseType.COLD, results.get(0).phase());
    }

    @Test
    void leavesPhaseRunningWhenStoppedMidChunkLoop() throws Exception {
        var engine = new BatchCopyEngine(new StubPlugin());
        var manager = new HotColdManager(engine);

        JobEntity job = new JobEntity();
        job.setMigrationMode(MigrationMode.HOT_ONLY);
        job.setSchemaName("public");
        job.setSourceTable("t");
        job.setTsColumn("created_at");
        job.setHotDays(7);
        job.setRangeStart(Instant.now().minus(Duration.ofDays(30)));
        job.setMinChunkDurationHours(24);
        job.setMaxChunkDurationHours(24);
        job.setConflictColumns(java.util.List.of("id"));

        JobPhaseEntity hot = new JobPhaseEntity();
        hot.setPhase(PhaseType.HOT);
        hot.setConflictMode(ConflictMode.DO_UPDATE);

        // Hot window is 7 days of 24h chunks (7 chunks). Let two chunks run, then stop.
        var callCount = new java.util.concurrent.atomic.AtomicInteger();
        var checker = new HotColdManager.CommandChecker() {
            @Override public boolean isPaused() { return false; }
            @Override public boolean shouldStop() { return callCount.getAndIncrement() >= 2; }
        };

        manager.runJob(job, java.util.Map.of(), java.util.Map.of(), java.util.List.of(hot), checker);

        assertEquals(PhaseStatus.RUNNING, hot.getStatus());
    }

    @Test
    void marksPhaseCompletedWhenAllChunksFinish() throws Exception {
        var engine = new BatchCopyEngine(new StubPlugin());
        var manager = new HotColdManager(engine);

        JobEntity job = new JobEntity();
        job.setMigrationMode(MigrationMode.HOT_ONLY);
        job.setSchemaName("public");
        job.setSourceTable("t");
        job.setTsColumn("created_at");
        job.setHotDays(7);
        job.setRangeStart(Instant.now().minus(Duration.ofDays(30)));
        job.setMinChunkDurationHours(24);
        job.setMaxChunkDurationHours(24);
        job.setConflictColumns(java.util.List.of("id"));

        JobPhaseEntity hot = new JobPhaseEntity();
        hot.setPhase(PhaseType.HOT);
        hot.setConflictMode(ConflictMode.DO_UPDATE);

        var checker = new HotColdManager.CommandChecker() {
            @Override public boolean isPaused() { return false; }
            @Override public boolean shouldStop() { return false; }
        };

        manager.runJob(job, java.util.Map.of(), java.util.Map.of(), java.util.List.of(hot), checker);

        assertEquals(PhaseStatus.COMPLETED, hot.getStatus());
    }
}

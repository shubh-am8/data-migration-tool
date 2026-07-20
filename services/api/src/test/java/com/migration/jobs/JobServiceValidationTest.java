package com.migration.jobs;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class JobServiceValidationTest {

    @Test
    void rejectsMaxChunkLessThanMin() {
        JobEntity job = new JobEntity();
        job.setMigrationMode(MigrationMode.HOT_THEN_COLD);
        job.setTsColumn("created_at");
        job.setHotDays(7);
        job.setRangeStart(Instant.parse("2024-01-01T00:00:00Z"));
        job.setRangeEndMode(RangeEndMode.NOW);
        job.setMinChunkDurationHours(48);
        job.setMaxChunkDurationHours(24);
        assertThrows(IllegalArgumentException.class, () -> JobService.validateRangeChunks(job));
    }

    @Test
    void rejectsMinChunkBelowOneHour() {
        JobEntity job = new JobEntity();
        job.setMigrationMode(MigrationMode.HOT_ONLY);
        job.setMinChunkDurationHours(0);
        job.setMaxChunkDurationHours(24);
        assertThrows(IllegalArgumentException.class, () -> JobService.validateRangeChunks(job));
    }

    @Test
    void rejectsFixedRangeEndModeWithoutRangeEnd() {
        JobEntity job = new JobEntity();
        job.setMigrationMode(MigrationMode.HOT_ONLY);
        job.setRangeEndMode(RangeEndMode.FIXED);
        job.setRangeEnd(null);
        assertThrows(IllegalArgumentException.class, () -> JobService.validateRangeChunks(job));
    }

    @Test
    void rejectsMissingRangeStartForColdMigration() {
        JobEntity job = new JobEntity();
        job.setMigrationMode(MigrationMode.COLD_ONLY);
        job.setRangeStart(null);
        assertThrows(IllegalArgumentException.class, () -> JobService.validateRangeChunks(job));
    }

    @Test
    void acceptsValidHotOnlyJobWithDefaults() {
        JobEntity job = new JobEntity();
        job.setMigrationMode(MigrationMode.HOT_ONLY);
        assertDoesNotThrow(() -> JobService.validateRangeChunks(job));
    }

    @Test
    void rejectsRangeEndNotAfterRangeStart() {
        JobEntity job = new JobEntity();
        job.setMigrationMode(MigrationMode.HOT_THEN_COLD);
        job.setRangeStart(Instant.parse("2024-02-01T00:00:00Z"));
        job.setRangeEndMode(RangeEndMode.FIXED);
        job.setRangeEnd(Instant.parse("2024-01-01T00:00:00Z"));
        assertThrows(IllegalArgumentException.class, () -> JobService.validateRangeChunks(job));
    }

    @Test
    void acceptsValidRangeAndChunkConfig() {
        JobEntity job = new JobEntity();
        job.setMigrationMode(MigrationMode.HOT_THEN_COLD);
        job.setTsColumn("created_at");
        job.setHotDays(7);
        job.setRangeStart(Instant.parse("2024-01-01T00:00:00Z"));
        job.setRangeEndMode(RangeEndMode.FIXED);
        job.setRangeEnd(Instant.parse("2024-02-01T00:00:00Z"));
        job.setMinChunkDurationHours(24);
        job.setMaxChunkDurationHours(168);
        assertDoesNotThrow(() -> JobService.validateRangeChunks(job));
    }
}

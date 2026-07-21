package com.migration.jobs;

import com.migration.config.AppConfigService;
import com.migration.simulation.SimulationConfig;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

    @Test
    void rejectsColdOnlyWithNullTsColumn() {
        JobService service = newServiceWithThreadBounds();
        JobEntity job = new JobEntity();
        job.setMigrationMode(MigrationMode.COLD_ONLY);
        job.setTsColumn(null);
        job.setRangeStart(Instant.parse("2024-01-01T00:00:00Z"));
        job.setConflictColumns(List.of("id"));
        assertThrows(IllegalArgumentException.class, () -> service.validateJob(job, Map.of()));
    }

    @Test
    void rejectsColdOnlyWithBlankTsColumn() {
        JobService service = newServiceWithThreadBounds();
        JobEntity job = new JobEntity();
        job.setMigrationMode(MigrationMode.COLD_ONLY);
        job.setTsColumn("   ");
        job.setRangeStart(Instant.parse("2024-01-01T00:00:00Z"));
        job.setConflictColumns(List.of("id"));
        assertThrows(IllegalArgumentException.class, () -> service.validateJob(job, Map.of()));
    }

    @Test
    void rejectsHotOnlyWithBlankTsColumn() {
        JobService service = newServiceWithThreadBounds();
        JobEntity job = new JobEntity();
        job.setMigrationMode(MigrationMode.HOT_ONLY);
        job.setTsColumn("");
        job.setHotDays(7);
        job.setConflictColumns(List.of("id"));
        assertThrows(IllegalArgumentException.class, () -> service.validateJob(job, Map.of()));
    }

    @Test
    void acceptsHotOnlyWithNonBlankTsColumn() {
        JobService service = newServiceWithThreadBounds();
        JobEntity job = new JobEntity();
        job.setMigrationMode(MigrationMode.HOT_ONLY);
        job.setTsColumn("created_at");
        job.setHotDays(7);
        job.setConflictColumns(List.of("id"));
        assertDoesNotThrow(() -> service.validateJob(job, Map.of()));
    }

    @Test
    void applyBodySerializesConfigJsonMapToJsonString() {
        JobService service = newServiceWithThreadBounds();
        JobEntity job = new JobEntity();
        service.applyBody(job, Map.of("configJson", Map.of("kind", "SIMULATE", "scenario", "COLD_ONLY")));
        assertTrue(SimulationConfig.isSimulation(job.getConfigJson()));
        assertEquals("COLD_ONLY", SimulationConfig.parse(job.getConfigJson()).scenario());
    }

    @Test
    void applyBodyLeavesConfigJsonUntouchedWhenKeyAbsent() {
        JobService service = newServiceWithThreadBounds();
        JobEntity job = new JobEntity();
        String before = job.getConfigJson();
        service.applyBody(job, Map.of("name", "job-1"));
        assertEquals(before, job.getConfigJson());
    }

    private static JobService newServiceWithThreadBounds() {
        AppConfigService appConfigService = mock(AppConfigService.class);
        when(appConfigService.get("min_threads_per_job")).thenReturn("1");
        when(appConfigService.get("max_threads_per_job")).thenReturn("16");
        return new JobService(null, null, null, null, null, null, appConfigService, null, null, null, null);
    }
}

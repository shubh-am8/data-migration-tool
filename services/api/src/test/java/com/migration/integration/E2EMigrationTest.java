package com.migration.integration;

import com.migration.jobs.JobStatus;
import com.migration.jobs.MigrationMode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * E2E smoke test scaffold — validates core domain contracts used in migration flow.
 * Full Testcontainers E2E runs when Docker is available (see FlywayMigrationTest).
 */
class E2EMigrationTest {

    @Test
    void jobLifecycleStatusesAreDefined() {
        assertNotNull(JobStatus.RUNNING);
        assertNotNull(JobStatus.COMPLETED);
        assertNotNull(MigrationMode.HOT_THEN_COLD);
    }

    @Test
    void hotColdModesCoverAllPhases() {
        assertEquals(4, MigrationMode.values().length);
        assertNotNull(MigrationMode.COLD_THEN_HOT);
    }
}

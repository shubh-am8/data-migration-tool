package com.migration.jobs;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class JobServiceHotColdFilterTest {

    @Test
    void hotFilterUsesHotBoundaryToEffectiveEndWithFixedRangeEnd() {
        JobEntity job = new JobEntity();
        job.setTsColumn("created_at");
        job.setHotDays(7);
        job.setRangeEndMode(RangeEndMode.FIXED);
        job.setRangeEnd(Instant.parse("2024-01-15T00:00:00Z"));

        String filter = JobService.buildHotColdFilter(job, PhaseType.HOT);

        assertNotNull(filter);
        assertTrue(filter.contains("2024-01-08T00:00:00Z"), filter);
        assertTrue(filter.contains("2024-01-15T00:00:00Z"), filter);
        assertTrue(filter.contains("\"created_at\""));
    }

    @Test
    void coldFilterUsesRangeStartToHotBoundaryWithFixedRangeEnd() {
        JobEntity job = new JobEntity();
        job.setTsColumn("created_at");
        job.setHotDays(7);
        job.setRangeStart(Instant.parse("2024-01-01T00:00:00Z"));
        job.setRangeEndMode(RangeEndMode.FIXED);
        job.setRangeEnd(Instant.parse("2024-01-15T00:00:00Z"));

        String filter = JobService.buildHotColdFilter(job, PhaseType.COLD);

        assertNotNull(filter);
        assertTrue(filter.contains("2024-01-01T00:00:00Z"), filter);
        assertTrue(filter.contains("2024-01-08T00:00:00Z"), filter);
    }

    @Test
    void returnsNullWhenTsColumnOrHotDaysMissing() {
        JobEntity job = new JobEntity();
        assertNull(JobService.buildHotColdFilter(job, PhaseType.HOT));
    }
}

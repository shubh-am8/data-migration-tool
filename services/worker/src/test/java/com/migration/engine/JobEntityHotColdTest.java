package com.migration.engine;

import com.migration.jobs.JobEntity;
import com.migration.jobs.RangeEndMode;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class JobEntityHotColdTest {

    @Test
    void effectiveEndUsesNowWhenModeIsNow() {
        JobEntity job = new JobEntity();
        job.setRangeEndMode(RangeEndMode.NOW);
        Instant now = Instant.parse("2024-06-01T00:00:00Z");

        assertEquals(now, JobEntityHotCold.effectiveEnd(job, now));
    }

    @Test
    void effectiveEndUsesRangeEndWhenModeIsFixed() {
        JobEntity job = new JobEntity();
        job.setRangeEndMode(RangeEndMode.FIXED);
        Instant fixedEnd = Instant.parse("2024-01-15T00:00:00Z");
        job.setRangeEnd(fixedEnd);
        Instant now = Instant.parse("2024-06-01T00:00:00Z");

        assertEquals(fixedEnd, JobEntityHotCold.effectiveEnd(job, now));
    }

    @Test
    void effectiveEndFallsBackToNowWhenFixedButRangeEndMissing() {
        JobEntity job = new JobEntity();
        job.setRangeEndMode(RangeEndMode.FIXED);
        job.setRangeEnd(null);
        Instant now = Instant.parse("2024-06-01T00:00:00Z");

        assertEquals(now, JobEntityHotCold.effectiveEnd(job, now));
    }

    @Test
    void hotBoundarySubtractsHotDaysFromEffectiveEnd() {
        JobEntity job = new JobEntity();
        job.setHotDays(7);
        Instant effectiveEnd = Instant.parse("2024-06-08T00:00:00Z");

        Instant expected = effectiveEnd.minus(Duration.ofDays(7));
        assertEquals(expected, JobEntityHotCold.hotBoundary(job, effectiveEnd));
    }

    @Test
    void hotBoundaryTreatsNullHotDaysAsZero() {
        JobEntity job = new JobEntity();
        job.setHotDays(null);
        Instant effectiveEnd = Instant.parse("2024-06-08T00:00:00Z");

        assertEquals(effectiveEnd, JobEntityHotCold.hotBoundary(job, effectiveEnd));
    }

    @Test
    void timeRangeFilterContainsColumnStartAndEnd() {
        Instant start = Instant.parse("2024-01-01T00:00:00Z");
        Instant end = Instant.parse("2024-01-02T00:00:00Z");

        String filter = JobEntityHotCold.timeRangeFilter("created_at", start, end);

        assertTrue(filter.contains("created_at"));
        assertTrue(filter.contains(start.toString()));
        assertTrue(filter.contains(end.toString()));
        assertTrue(filter.contains(">="));
        assertTrue(filter.contains("<"));
    }

    @Test
    void timeRangeFilterQuotesColumnName() {
        String filter = JobEntityHotCold.timeRangeFilter("created_at",
            Instant.parse("2024-01-01T00:00:00Z"), Instant.parse("2024-01-02T00:00:00Z"));

        assertTrue(filter.contains("\"created_at\""));
    }
}

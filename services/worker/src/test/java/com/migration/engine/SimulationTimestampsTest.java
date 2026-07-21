package com.migration.engine;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class SimulationTimestampsTest {

    private static final Instant NOW = Instant.parse("2024-06-08T00:00:00Z");

    @Test
    void coldOnlyStaysOlderThanHotBoundary() {
        Instant boundary = NOW.minus(Duration.ofDays(7));
        for (int i = 0; i < 10; i++) {
            Instant ts = SimulationTimestamps.coldOnly(NOW, 7, i, 10);
            assertTrue(ts.isBefore(boundary), "row " + i + " should be older than the hot boundary");
        }
    }

    @Test
    void coldOnlySpreadsRowsAcrossDistinctValues() {
        Instant first = SimulationTimestamps.coldOnly(NOW, 7, 0, 10);
        Instant last = SimulationTimestamps.coldOnly(NOW, 7, 9, 10);
        assertTrue(first.isBefore(last));
    }

    @Test
    void coldOnlyTreatsNegativeHotDaysAsZero() {
        Instant ts = SimulationTimestamps.coldOnly(NOW, -5, 0, 10);
        assertTrue(ts.isBefore(NOW));
    }

    @Test
    void coldOnlySingleRowDoesNotThrow() {
        assertDoesNotThrow(() -> SimulationTimestamps.coldOnly(NOW, 7, 0, 1));
    }

    @Test
    void hotThenColdFirstHalfIsBeforeBoundary() {
        Instant boundary = NOW.minus(Duration.ofDays(7));
        for (int i = 0; i < 5; i++) {
            Instant ts = SimulationTimestamps.hotThenCold(NOW, 7, i, 10);
            assertTrue(ts.isBefore(boundary), "row " + i + " should be cold");
        }
    }

    @Test
    void hotThenColdSecondHalfIsAtOrAfterBoundaryAndBeforeNow() {
        Instant boundary = NOW.minus(Duration.ofDays(7));
        for (int i = 5; i < 10; i++) {
            Instant ts = SimulationTimestamps.hotThenCold(NOW, 7, i, 10);
            assertFalse(ts.isBefore(boundary), "row " + i + " should be hot (>= boundary)");
            assertTrue(ts.isBefore(NOW), "row " + i + " should still be before now");
        }
    }

    @Test
    void hotThenColdSingleRowLandsBeforeBoundary() {
        Instant boundary = NOW.minus(Duration.ofDays(7));
        Instant ts = SimulationTimestamps.hotThenCold(NOW, 7, 0, 1);
        assertTrue(ts.isBefore(boundary));
    }

    @Test
    void hotThenColdTreatsNegativeHotDaysAsZero() {
        Instant ts = SimulationTimestamps.hotThenCold(NOW, -5, 9, 10);
        assertFalse(ts.isAfter(NOW));
    }
}

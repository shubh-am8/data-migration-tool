package com.migration.engine;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TimeChunkPlannerTest {

    @Test
    void plansMaxSizedChunksWithShortTail() {
        Instant start = Instant.parse("2024-01-01T00:00:00Z");
        Instant end = start.plus(Duration.ofHours(50));
        List<TimeChunkPlanner.TimeChunk> chunks = TimeChunkPlanner.plan(start, end, 1, 24);
        assertEquals(3, chunks.size());
        assertEquals(start, chunks.get(0).start());
        assertEquals(start.plus(Duration.ofHours(24)), chunks.get(0).end());
        assertEquals(Duration.ofHours(2), Duration.between(chunks.get(2).start(), chunks.get(2).end()));
    }

    @Test
    void returnsEmptyListWhenEndNotAfterStart() {
        Instant start = Instant.parse("2024-01-01T00:00:00Z");
        assertEquals(List.of(), TimeChunkPlanner.plan(start, start, 1, 24));
        assertEquals(List.of(), TimeChunkPlanner.plan(start, start.minusSeconds(1), 1, 24));
    }

    @Test
    void rejectsInvalidChunkHours() {
        Instant start = Instant.parse("2024-01-01T00:00:00Z");
        Instant end = start.plus(Duration.ofHours(10));
        assertThrows(IllegalArgumentException.class, () -> TimeChunkPlanner.plan(start, end, 0, 24));
        assertThrows(IllegalArgumentException.class, () -> TimeChunkPlanner.plan(start, end, 24, 1));
    }

    @Test
    void singleChunkWhenRangeFitsWithinMax() {
        Instant start = Instant.parse("2024-01-01T00:00:00Z");
        Instant end = start.plus(Duration.ofHours(10));
        List<TimeChunkPlanner.TimeChunk> chunks = TimeChunkPlanner.plan(start, end, 1, 24);
        assertEquals(1, chunks.size());
        assertEquals(start, chunks.get(0).start());
        assertEquals(end, chunks.get(0).end());
    }
}

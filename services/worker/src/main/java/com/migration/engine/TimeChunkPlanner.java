package com.migration.engine;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class TimeChunkPlanner {
    private TimeChunkPlanner() {}

    public record TimeChunk(Instant start, Instant end) {}

    public static List<TimeChunk> plan(Instant start, Instant end, int minHours, int maxHours) {
        if (start == null || end == null || !end.isAfter(start)) return List.of();
        if (minHours < 1 || maxHours < minHours) throw new IllegalArgumentException("invalid chunk hours");
        Duration step = Duration.ofHours(maxHours);
        List<TimeChunk> out = new ArrayList<>();
        Instant cursor = start;
        while (cursor.isBefore(end)) {
            Instant next = cursor.plus(step);
            if (next.isAfter(end)) next = end;
            out.add(new TimeChunk(cursor, next));
            cursor = next;
        }
        return out;
    }
}

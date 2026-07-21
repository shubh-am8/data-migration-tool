package com.migration.engine;

import java.time.Duration;
import java.time.Instant;

/**
 * Pure timestamp-window math for lab-devtools simulation rows. Mirrors {@code TimeWindowMath}'s
 * hot-boundary convention (boundary = now - hotDays) without depending on {@code JobEntity}, so
 * {@link SimulationEngine} can deterministically decide whether the {@code rowIndex}-th of
 * {@code totalRows} sample rows should look "hot" (recent) or "cold" (old) before ever opening a
 * lab DB connection.
 */
final class SimulationTimestamps {
    private SimulationTimestamps() {}

    private static final Duration COLD_SPAN = Duration.ofDays(60);
    private static final Duration COLD_BUFFER = Duration.ofDays(1);

    /** Every row lands in {@code [boundary - 61d, boundary - 1d)} — always older than the hot boundary. */
    static Instant coldOnly(Instant now, int hotDays, int rowIndex, int totalRows) {
        Instant boundary = now.minus(Duration.ofDays(Math.max(hotDays, 0)));
        Instant coldEnd = boundary.minus(COLD_BUFFER);
        return spread(coldEnd.minus(COLD_SPAN), coldEnd, rowIndex, totalRows);
    }

    /**
     * First half of rows land before {@code boundary} (cold); second half land in
     * {@code [boundary, now)} (hot) — a realistic hot/cold mix for {@code orders_hot_cold}.
     */
    static Instant hotThenCold(Instant now, int hotDays, int rowIndex, int totalRows) {
        Instant boundary = now.minus(Duration.ofDays(Math.max(hotDays, 0)));
        int hotCount = totalRows / 2;
        int coldCount = totalRows - hotCount;
        if (rowIndex < coldCount) {
            return spread(boundary.minus(COLD_SPAN), boundary, rowIndex, coldCount);
        }
        return spread(boundary, now, rowIndex - coldCount, hotCount);
    }

    /** All rows land in {@code [boundary, now)}. */
    static Instant hotOnly(Instant now, int hotDays, int rowIndex, int totalRows) {
        Instant boundary = now.minus(Duration.ofDays(Math.max(hotDays, 0)));
        return spread(boundary, now, rowIndex, totalRows);
    }

    /** First half hot, second half cold — inverse of {@link #hotThenCold}. */
    static Instant coldThenHot(Instant now, int hotDays, int rowIndex, int totalRows) {
        Instant boundary = now.minus(Duration.ofDays(Math.max(hotDays, 0)));
        int hotCount = totalRows / 2;
        int coldCount = totalRows - hotCount;
        if (rowIndex < hotCount) {
            return spread(boundary, now, rowIndex, hotCount);
        }
        return spread(boundary.minus(COLD_SPAN), boundary, rowIndex - hotCount, coldCount);
    }

    /** Evenly spaces {@code index} of {@code count} points across {@code [start, end)}. */
    private static Instant spread(Instant start, Instant end, int index, int count) {
        if (count <= 1) return start;
        long totalMillis = Duration.between(start, end).toMillis();
        return start.plusMillis(totalMillis * index / count);
    }
}

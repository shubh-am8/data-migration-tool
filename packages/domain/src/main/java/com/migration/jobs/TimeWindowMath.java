package com.migration.jobs;

import java.time.Duration;
import java.time.Instant;

/**
 * Shared hot/cold time-window math so the API's filter preview and the
 * worker's actual chunking agree on the same boundaries.
 */
public final class TimeWindowMath {
    private TimeWindowMath() {}

    public static Instant effectiveEnd(JobEntity job, Instant now) {
        if (job.getRangeEndMode() == RangeEndMode.FIXED && job.getRangeEnd() != null) return job.getRangeEnd();
        return now;
    }

    public static Instant hotBoundary(JobEntity job, Instant effectiveEnd) {
        int days = job.getHotDays() == null ? 0 : job.getHotDays();
        return effectiveEnd.minus(Duration.ofDays(days));
    }

    public static String timeRangeFilter(String tsColumn, Instant start, Instant end) {
        String col = "\"" + tsColumn.replace("\"", "") + "\"";
        return col + " >= '" + start + "' AND " + col + " < '" + end + "'";
    }
}

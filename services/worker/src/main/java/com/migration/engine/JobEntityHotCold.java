package com.migration.engine;

import com.migration.jobs.JobEntity;
import com.migration.jobs.RangeEndMode;

import java.time.Duration;
import java.time.Instant;

final class JobEntityHotCold {
    private JobEntityHotCold() {}

    static Instant effectiveEnd(JobEntity job, Instant now) {
        if (job.getRangeEndMode() == RangeEndMode.FIXED && job.getRangeEnd() != null) return job.getRangeEnd();
        return now;
    }

    static Instant hotBoundary(JobEntity job, Instant effectiveEnd) {
        int days = job.getHotDays() == null ? 0 : job.getHotDays();
        return effectiveEnd.minus(Duration.ofDays(days));
    }

    static String timeRangeFilter(String tsColumn, Instant start, Instant end) {
        String col = "\"" + tsColumn.replace("\"", "") + "\"";
        return col + " >= '" + start + "' AND " + col + " < '" + end + "'";
    }
}

package com.migration.engine;

import com.migration.jobs.JobEntity;
import com.migration.jobs.TimeWindowMath;

import java.time.Instant;

/** Thin wrapper so worker call sites stay unchanged; math lives in packages/domain. */
final class JobEntityHotCold {
    private JobEntityHotCold() {}

    static Instant effectiveEnd(JobEntity job, Instant now) {
        return TimeWindowMath.effectiveEnd(job, now);
    }

    static Instant hotBoundary(JobEntity job, Instant effectiveEnd) {
        return TimeWindowMath.hotBoundary(job, effectiveEnd);
    }

    static String timeRangeFilter(String tsColumn, Instant start, Instant end) {
        return TimeWindowMath.timeRangeFilter(tsColumn, start, end);
    }
}

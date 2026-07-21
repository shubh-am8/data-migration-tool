package com.migration.jobs;

import java.util.Set;

/** Canonical lab DB schema names on {@code migration_lab} (:5433). */
public final class LabSchemas {
    public static final String SOURCE = "test_source";
    public static final String DESTINATION = "test_destination";

    public static final Set<String> ALL = Set.of(SOURCE, DESTINATION);
    public static final Set<String> TEST_JOB_SOURCE_SCHEMAS = Set.of(SOURCE);

    private LabSchemas() {}
}

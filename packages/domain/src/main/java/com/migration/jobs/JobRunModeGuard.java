package com.migration.jobs;

/**
 * Enforces the TEST/PRODUCTION run-mode contract: TEST jobs must stay inside sandbox
 * connections and read from {@link LabSchemas#SOURCE}; PRODUCTION jobs must never target
 * lab schemas; simulation jobs only ever run in TEST mode.
 */
public final class JobRunModeGuard {
    private JobRunModeGuard() {}

    public static void validate(JobRunMode mode, boolean sourceSandbox, boolean destSandbox,
                                String schemaName, boolean isSimulation) {
        if (isSimulation && mode != JobRunMode.TEST) {
            throw new IllegalArgumentException("simulation requires runMode=TEST");
        }
        if (mode == JobRunMode.TEST) {
            if (!sourceSandbox || !destSandbox) {
                throw new IllegalArgumentException("TEST jobs require sandbox source and dest connections");
            }
            if (schemaName != null && !LabSchemas.TEST_JOB_SOURCE_SCHEMAS.contains(schemaName)) {
                throw new IllegalArgumentException(
                    "TEST jobs must use schema " + LabSchemas.SOURCE + " (lab source playground)");
            }
        }
        if (mode == JobRunMode.PRODUCTION && schemaName != null && LabSchemas.ALL.contains(schemaName)) {
            throw new IllegalArgumentException("PRODUCTION jobs cannot target lab schemas");
        }
    }
}

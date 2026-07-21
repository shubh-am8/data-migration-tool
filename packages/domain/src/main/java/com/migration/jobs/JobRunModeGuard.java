package com.migration.jobs;

/**
 * Enforces the TEST/PRODUCTION run-mode contract: TEST jobs must stay inside sandbox
 * connections and the lab schemas (app/test); PRODUCTION jobs must never target the
 * lab's test schema; simulation jobs only ever run in TEST mode.
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
            if (schemaName != null && !(schemaName.equals("app") || schemaName.equals("test"))) {
                throw new IllegalArgumentException("TEST jobs must use schema app or test");
            }
        }
        if (mode == JobRunMode.PRODUCTION && "test".equals(schemaName)) {
            throw new IllegalArgumentException("PRODUCTION jobs cannot target schema test");
        }
    }
}

package com.migration.jobs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JobRunModeGuardTest {

    @Test
    void rejectsSimulationWhenModeIsNotTest() {
        var ex = assertThrows(IllegalArgumentException.class,
            () -> JobRunModeGuard.validate(JobRunMode.PRODUCTION, true, true, "app", true));
        assertTrue(ex.getMessage().contains("simulation requires runMode=TEST"));
    }

    @Test
    void rejectsTestModeWithNonSandboxSource() {
        var ex = assertThrows(IllegalArgumentException.class,
            () -> JobRunModeGuard.validate(JobRunMode.TEST, false, true, "app", false));
        assertTrue(ex.getMessage().contains("require sandbox"));
    }

    @Test
    void rejectsTestModeWithNonSandboxDest() {
        var ex = assertThrows(IllegalArgumentException.class,
            () -> JobRunModeGuard.validate(JobRunMode.TEST, true, false, "app", false));
        assertTrue(ex.getMessage().contains("require sandbox"));
    }

    @Test
    void rejectsTestModeWithSchemaOutsideAppOrTest() {
        var ex = assertThrows(IllegalArgumentException.class,
            () -> JobRunModeGuard.validate(JobRunMode.TEST, true, true, "public", false));
        assertTrue(ex.getMessage().contains("schema app or test"));
    }

    @Test
    void acceptsTestModeWithSandboxConnectionsAndAppSchema() {
        assertDoesNotThrow(() -> JobRunModeGuard.validate(JobRunMode.TEST, true, true, "app", false));
    }

    @Test
    void acceptsTestModeWithSandboxConnectionsAndTestSchema() {
        assertDoesNotThrow(() -> JobRunModeGuard.validate(JobRunMode.TEST, true, true, "test", false));
    }

    @Test
    void acceptsTestModeWithNullSchema() {
        assertDoesNotThrow(() -> JobRunModeGuard.validate(JobRunMode.TEST, true, true, null, false));
    }

    @Test
    void rejectsProductionModeTargetingTestSchema() {
        var ex = assertThrows(IllegalArgumentException.class,
            () -> JobRunModeGuard.validate(JobRunMode.PRODUCTION, false, false, "test", false));
        assertTrue(ex.getMessage().contains("cannot target schema test"));
    }

    @Test
    void acceptsProductionModeWithNonSandboxConnectionsAndOtherSchema() {
        assertDoesNotThrow(() -> JobRunModeGuard.validate(JobRunMode.PRODUCTION, false, false, "public", false));
    }

    @Test
    void acceptsSimulationInTestMode() {
        assertDoesNotThrow(() -> JobRunModeGuard.validate(JobRunMode.TEST, true, true, "app", true));
    }
}

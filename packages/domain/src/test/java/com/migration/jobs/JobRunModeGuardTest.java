package com.migration.jobs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JobRunModeGuardTest {

    @Test
    void rejectsSimulationWhenModeIsNotTest() {
        var ex = assertThrows(IllegalArgumentException.class,
            () -> JobRunModeGuard.validate(JobRunMode.PRODUCTION, true, true, LabSchemas.SOURCE, true));
        assertTrue(ex.getMessage().contains("simulation requires runMode=TEST"));
    }

    @Test
    void rejectsTestModeWithNonSandboxSource() {
        var ex = assertThrows(IllegalArgumentException.class,
            () -> JobRunModeGuard.validate(JobRunMode.TEST, false, true, LabSchemas.SOURCE, false));
        assertTrue(ex.getMessage().contains("require sandbox"));
    }

    @Test
    void rejectsTestModeWithNonSandboxDest() {
        var ex = assertThrows(IllegalArgumentException.class,
            () -> JobRunModeGuard.validate(JobRunMode.TEST, true, false, LabSchemas.SOURCE, false));
        assertTrue(ex.getMessage().contains("require sandbox"));
    }

    @Test
    void rejectsTestModeWithSchemaOutsideTestSource() {
        var ex = assertThrows(IllegalArgumentException.class,
            () -> JobRunModeGuard.validate(JobRunMode.TEST, true, true, "public", false));
        assertTrue(ex.getMessage().contains(LabSchemas.SOURCE));
    }

    @Test
    void rejectsTestModeWithDestinationSchemaAsSource() {
        var ex = assertThrows(IllegalArgumentException.class,
            () -> JobRunModeGuard.validate(JobRunMode.TEST, true, true, LabSchemas.DESTINATION, false));
        assertTrue(ex.getMessage().contains(LabSchemas.SOURCE));
    }

    @Test
    void acceptsTestModeWithTestSourceSchema() {
        assertDoesNotThrow(() -> JobRunModeGuard.validate(JobRunMode.TEST, true, true, LabSchemas.SOURCE, false));
    }

    @Test
    void acceptsTestModeWithNullSchema() {
        assertDoesNotThrow(() -> JobRunModeGuard.validate(JobRunMode.TEST, true, true, null, false));
    }

    @Test
    void rejectsProductionModeTargetingLabSchemas() {
        var ex = assertThrows(IllegalArgumentException.class,
            () -> JobRunModeGuard.validate(JobRunMode.PRODUCTION, false, false, LabSchemas.DESTINATION, false));
        assertTrue(ex.getMessage().contains("cannot target lab schemas"));
    }

    @Test
    void acceptsProductionModeWithNonLabSchema() {
        assertDoesNotThrow(() -> JobRunModeGuard.validate(JobRunMode.PRODUCTION, false, false, "public", false));
    }

    @Test
    void acceptsSimulationInTestMode() {
        assertDoesNotThrow(() -> JobRunModeGuard.validate(JobRunMode.TEST, true, true, LabSchemas.SOURCE, true));
    }
}

package com.migration.engine;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ReconciliationServiceTest {

    @Test
    void failsOnRowMismatch() {
        var service = new ReconciliationService();
        var result = service.reconcile(100, 50, 40, 0, true);
        assertFalse(result.passed());
    }

    @Test
    void passesWhenCountsMatch() {
        var service = new ReconciliationService();
        var result = service.reconcile(100, 60, 40, 0, true);
        assertTrue(result.passed());
    }
}

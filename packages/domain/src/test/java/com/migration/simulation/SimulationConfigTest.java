package com.migration.simulation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SimulationConfigTest {

    @Test
    void isSimulationTrueWhenKindIsSimulate() {
        assertTrue(SimulationConfig.isSimulation("{\"kind\":\"SIMULATE\"}"));
    }

    @Test
    void isSimulationFalseForOtherKinds() {
        assertFalse(SimulationConfig.isSimulation("{\"kind\":\"OTHER\"}"));
    }

    @Test
    void isSimulationFalseForMalformedJson() {
        assertFalse(SimulationConfig.isSimulation("not json"));
    }

    @Test
    void isSimulationFalseForNull() {
        assertFalse(SimulationConfig.isSimulation(null));
    }

    @Test
    void parseReadsAllFields() {
        SimulationConfig config = SimulationConfig.parse(
            "{\"kind\":\"SIMULATE\",\"scenario\":\"HOT_THEN_COLD\",\"schema\":\"test_source\","
                + "\"table\":\"orders_hot_cold\",\"rows\":100,\"updateRatio\":0.2}");

        assertEquals("HOT_THEN_COLD", config.scenario());
        assertEquals("test_source", config.schema());
        assertEquals("orders_hot_cold", config.table());
        assertEquals(100, config.rows());
        assertEquals(0.2, config.updateRatio());
    }

    @Test
    void parseDefaultsMissingFields() {
        SimulationConfig config = SimulationConfig.parse("{\"kind\":\"SIMULATE\"}");

        assertEquals("COLD_ONLY", config.scenario());
        assertEquals("test_source", config.schema());
        assertEquals("orders_cold", config.table());
        assertEquals(100, config.rows());
        assertEquals(0.0, config.updateRatio());
    }

    @Test
    void parseThrowsOnMalformedJson() {
        assertThrows(IllegalArgumentException.class, () -> SimulationConfig.parse("not json"));
    }
}

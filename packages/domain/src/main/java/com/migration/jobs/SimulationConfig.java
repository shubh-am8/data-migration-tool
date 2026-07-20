package com.migration.jobs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Parsed shape of a job's {@code configJson} when {@code kind == "SIMULATE"} (the lab-devtools
 * TOOL): a TEST-only job that seeds/touches sample rows in the lab DB instead of migrating real
 * source/dest data. {@link #isSimulation} lets callers (job validation in the API, the worker's
 * queue consumer) branch without parsing the full shape.
 */
public record SimulationConfig(String scenario, String schema, String table, int rows, double updateRatio) {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static boolean isSimulation(String configJson) {
        return "SIMULATE".equals(kind(configJson));
    }

    private static String kind(String configJson) {
        if (configJson == null) return null;
        try {
            return MAPPER.readTree(configJson).path("kind").asText(null);
        } catch (Exception e) {
            return null;
        }
    }

    public static SimulationConfig parse(String configJson) {
        try {
            JsonNode n = MAPPER.readTree(configJson);
            return new SimulationConfig(
                n.path("scenario").asText("COLD_ONLY"),
                n.path("schema").asText("app"),
                n.path("table").asText("orders_cold"),
                n.path("rows").asInt(100),
                n.path("updateRatio").asDouble(0.0));
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid SIMULATE config_json: " + e.getMessage(), e);
        }
    }
}

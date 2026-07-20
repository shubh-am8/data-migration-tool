package com.migration.connectors.postgresql;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.migration.connectors.IndexRecommendation;

import java.util.ArrayList;
import java.util.List;

final class PgExplainParser {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private PgExplainParser() {}

    static List<IndexRecommendation> parseRecommendations(String planJson, String table) {
        List<IndexRecommendation> recs = new ArrayList<>();
        try {
            JsonNode root = MAPPER.readTree(planJson);
            JsonNode plan = root.isArray() ? root.get(0).get("Plan") : root.get("Plan");
            walkPlan(plan, table, recs);
        } catch (Exception ignored) {
            // ponytail: naive JSON walk; upgrade to full plan analyzer if needed
        }
        return recs;
    }

    private static void walkPlan(JsonNode node, String table, List<IndexRecommendation> recs) {
        if (node == null) return;
        String type = node.path("Node Type").asText("");
        if ("Seq Scan".equals(type)) {
            long rows = node.path("Plan Rows").asLong(0);
            if (rows > 10000) {
                recs.add(new IndexRecommendation(table, List.of("<filter columns>"),
                    "Sequential scan on large table (~" + rows + " rows estimated)"));
            }
        }
        JsonNode children = node.get("Plans");
        if (children != null && children.isArray()) {
            for (JsonNode child : children) walkPlan(child, table, recs);
        }
    }
}

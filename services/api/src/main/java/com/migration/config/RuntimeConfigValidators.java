package com.migration.config;

final class RuntimeConfigValidators {

    static boolean positiveInt(String value) {
        if (value == null || value.isBlank()) return false;
        try {
            return Integer.parseInt(value) > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /** Accepts [] , ["1.2.3.4"], or [{"label":"x","ip":"1.2.3.4"}]. */
    static boolean ipWhitelistJson(String value) {
        if (value == null || value.isBlank()) return true;
        try {
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            var root = mapper.readTree(value);
            if (!root.isArray()) return false;
            for (var node : root) {
                if (node.isTextual()) continue;
                if (node.isObject() && (node.has("ip") || node.has("cidr"))) continue;
                return false;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private RuntimeConfigValidators() {}
}

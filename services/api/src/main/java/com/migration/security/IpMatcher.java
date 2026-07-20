package com.migration.security;

import java.util.List;

/** Exact IP match; optional simple prefix CIDR like 10.0.0.0/8 (first octet only for /8). */
final class IpMatcher {
    private IpMatcher() {}

    static boolean matches(String clientIp, List<String> allowed) {
        if (clientIp == null || clientIp.isBlank() || allowed == null || allowed.isEmpty()) {
            return false;
        }
        String ip = clientIp.trim();
        for (String entry : allowed) {
            if (entry == null || entry.isBlank()) continue;
            String rule = entry.trim();
            if (rule.contains("/")) {
                if (cidrContains(ip, rule)) return true;
            } else if (rule.equals(ip)) {
                return true;
            }
        }
        return false;
    }

    private static boolean cidrContains(String ip, String cidr) {
        try {
            String[] parts = cidr.split("/");
            int prefix = Integer.parseInt(parts[1]);
            long ipLong = ipv4ToLong(ip);
            long netLong = ipv4ToLong(parts[0]);
            if (prefix < 0 || prefix > 32) return false;
            long mask = prefix == 0 ? 0 : -1L << (32 - prefix);
            return (ipLong & mask) == (netLong & mask);
        } catch (Exception e) {
            return false;
        }
    }

    private static long ipv4ToLong(String ip) {
        String[] o = ip.split("\\.");
        return (Long.parseLong(o[0]) << 24)
            | (Long.parseLong(o[1]) << 16)
            | (Long.parseLong(o[2]) << 8)
            | Long.parseLong(o[3]);
    }
}

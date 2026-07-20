package com.migration.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.migration.config.AppConfigService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class IpWhitelistFilter extends OncePerRequestFilter {
    private final AppConfigService appConfigService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public IpWhitelistFilter(AppConfigService appConfigService) {
        this.appConfigService = appConfigService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/oauth2/")
            || path.startsWith("/login/oauth2/")
            || path.equals("/api/health")
            || path.equals("/actuator/health")
            || path.startsWith("/actuator/health/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String mode = appConfigService.get("ip_whitelist_mode");
        if (mode == null || mode.isBlank() || "OPEN".equalsIgnoreCase(mode.trim())) {
            filterChain.doFilter(request, response);
            return;
        }
        if (!"RESTRICTED".equalsIgnoreCase(mode.trim())) {
            response.setStatus(403);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Forbidden\",\"message\":\"Invalid IP whitelist mode\"}");
            return;
        }
        String clientIp = resolveClientIp(request);
        List<String> allowed = parseAllowedIps(appConfigService.get("ip_whitelist"));
        if (IpMatcher.matches(clientIp, allowed)) {
            filterChain.doFilter(request, response);
            return;
        }
        response.setStatus(403);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"Forbidden\",\"message\":\"IP not allowed\"}");
    }

    static String resolveClientIp(HttpServletRequest request) {
        return request.getRemoteAddr();
    }

    /**
     * Accepts legacy {@code ["1.2.3.4"]} or labeled {@code [{"label":"VPN","ip":"1.2.3.4"}]}.
     */
    List<String> parseAllowedIps(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        try {
            JsonNode root = objectMapper.readTree(raw);
            if (!root.isArray()) {
                return List.of(raw.trim());
            }
            List<String> ips = new ArrayList<>();
            for (JsonNode node : root) {
                if (node.isTextual()) {
                    String ip = node.asText().trim();
                    if (!ip.isEmpty()) ips.add(ip);
                } else if (node.isObject()) {
                    JsonNode ipNode = node.has("ip") ? node.get("ip") : node.get("cidr");
                    if (ipNode != null && ipNode.isTextual()) {
                        String ip = ipNode.asText().trim();
                        if (!ip.isEmpty()) ips.add(ip);
                    }
                }
            }
            return ips;
        } catch (Exception e) {
            return List.of(raw.trim());
        }
    }
}

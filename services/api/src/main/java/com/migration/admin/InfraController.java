package com.migration.admin;

import com.migration.auth.UserService;
import com.migration.metrics.HttpRequestMetrics;
import com.migration.metrics.MetricSampleService;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.Status;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/infra")
public class InfraController {
    private final UserService userService;
    private final HealthEndpoint healthEndpoint;
    private final MeterRegistry meterRegistry;
    private final MetricSampleService metricSampleService;
    private final String workerMetricsUrl;
    private final RestClient restClient = RestClient.create();

    public InfraController(UserService userService,
                           HealthEndpoint healthEndpoint,
                           @Autowired(required = false) MeterRegistry meterRegistry,
                           MetricSampleService metricSampleService,
                           @Value("${app.worker.metrics-url:http://localhost:8081}") String workerMetricsUrl) {
        this.userService = userService;
        this.healthEndpoint = healthEndpoint;
        this.meterRegistry = meterRegistry;
        this.metricSampleService = metricSampleService;
        this.workerMetricsUrl = workerMetricsUrl;
    }

    @GetMapping
    public Map<String, Object> snapshot(Authentication auth) {
        requireAdmin(auth);
        Map<String, Object> out = new HashMap<>();
        out.put("api", apiSection());
        out.put("worker", workerSection());
        out.put("web", Map.of(
            "status", "UI",
            "buildId", System.getenv().getOrDefault("BUILD_ID", "dev"),
            "note", "Next.js process metrics are host-level only"
        ));
        if (meterRegistry != null) {
            out.put("http", HttpRequestMetrics.toMap(HttpRequestMetrics.from(meterRegistry)));
        }
        out.put("samples", metricSampleService.samples());
        return out;
    }

    private Map<String, Object> apiSection() {
        Map<String, Object> api = new HashMap<>();
        api.put("status", resolveHealthStatus());
        api.put("cpu", gauge("process.cpu.usage"));
        Double mem = gauge("jvm.memory.used");
        if (mem != null) api.put("memUsedBytes", mem);
        return api;
    }

    private String resolveHealthStatus() {
        try {
            Object health = healthEndpoint.health();
            if (health instanceof HealthComponent hc) {
                Status status = hc.getStatus();
                return status != null ? status.getCode() : "UNKNOWN";
            }
            String s = String.valueOf(health);
            if (s.contains("UP")) return "UP";
            if (s.contains("DOWN")) return "DOWN";
            return "UNKNOWN";
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }

    private Map<String, Object> workerSection() {
        Map<String, Object> worker = new HashMap<>();
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> health = restClient.get()
                .uri(workerMetricsUrl + "/actuator/health")
                .retrieve()
                .body(Map.class);
            Object status = health != null ? health.get("status") : null;
            worker.put("status", status != null ? String.valueOf(status) : "UNKNOWN");
        } catch (Exception e) {
            worker.put("status", "DOWN");
            worker.put("error", e.getMessage());
        }
        return worker;
    }

    private Double gauge(String name) {
        if (meterRegistry == null) return null;
        var g = meterRegistry.find(name).gauge();
        return g == null ? null : g.value();
    }

    private void requireAdmin(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || !userService.isAdmin(auth.getName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin required");
        }
    }
}

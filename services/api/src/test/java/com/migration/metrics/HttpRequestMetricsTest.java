package com.migration.metrics;

import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpRequestMetricsTest {

    @Test
    void aggregatesStatusAndRanksRoutes() {
        SimpleMeterRegistry reg = new SimpleMeterRegistry();

        Timer jobs = Timer.builder("http.server.requests")
            .tag("method", "GET").tag("uri", "/api/jobs").tag("status", "200")
            .register(reg);
        jobs.record(Duration.ofMillis(10));
        jobs.record(Duration.ofMillis(12));
        jobs.record(Duration.ofMillis(8));

        Timer fast = Timer.builder("http.server.requests")
            .tag("method", "GET").tag("uri", "/api/fast").tag("status", "200")
            .register(reg);
        for (int i = 0; i < 5; i++) {
            fast.record(Duration.ofMillis(2));
        }

        Timer.builder("http.server.requests")
            .tag("method", "GET").tag("uri", "/api/slow").tag("status", "200")
            .register(reg)
            .record(Duration.ofMillis(800));

        Timer.builder("http.server.requests")
            .tag("method", "GET").tag("uri", "/api/x").tag("status", "500")
            .register(reg)
            .record(Duration.ofMillis(50));

        Timer.builder("http.server.requests")
            .tag("method", "GET").tag("uri", "/actuator/health").tag("status", "200")
            .register(reg)
            .record(Duration.ofMillis(1));

        var snap = HttpRequestMetrics.from(reg);

        assertEquals(9, snap.status().s2xx());
        assertEquals(0, snap.status().s4xx());
        assertEquals(1, snap.status().s5xx());

        assertFalse(snap.choking().isEmpty());
        assertTrue(snap.choking().stream().anyMatch(r -> r.uri().contains("/api/slow")));

        assertEquals("/api/slow", snap.slowest().get(0).uri());
        assertTrue(snap.fastest().stream().anyMatch(r -> "/api/fast".equals(r.uri())));

        assertTrue(snap.routes().stream().noneMatch(r -> r.uri().startsWith("/actuator")));

        Map<String, Object> map = HttpRequestMetrics.toMap(snap);
        @SuppressWarnings("unchecked")
        Map<String, Object> status = (Map<String, Object>) map.get("status");
        assertEquals(9L, status.get("2xx"));
        assertEquals(1L, status.get("5xx"));
    }

    @Test
    void fastestExcludesRoutesBelowCountThreshold() {
        SimpleMeterRegistry reg = new SimpleMeterRegistry();
        Timer lowCount = Timer.builder("http.server.requests")
            .tag("method", "GET").tag("uri", "/api/once").tag("status", "200")
            .register(reg);
        lowCount.record(Duration.ofMillis(1));

        var snap = HttpRequestMetrics.from(reg);
        assertTrue(snap.fastest().isEmpty());
    }

    @Test
    void chokingIncludesHighMaxMs() {
        SimpleMeterRegistry reg = new SimpleMeterRegistry();
        Timer.builder("http.server.requests")
            .tag("method", "GET").tag("uri", "/api/spike").tag("status", "200")
            .register(reg)
            .record(Duration.ofMillis(2500));

        var snap = HttpRequestMetrics.from(reg);
        assertFalse(snap.choking().isEmpty());
        assertEquals("/api/spike", snap.choking().get(0).uri());
    }
}

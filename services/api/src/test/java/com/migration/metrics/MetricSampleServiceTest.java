package com.migration.metrics;

import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MetricSampleServiceTest {

    @Test
    void ringNeverExceedsCapacity() {
        MetricSampleService svc = new MetricSampleService(null, null, null);
        for (int i = 0; i < MetricSampleService.CAPACITY + 50; i++) {
            svc.offer(Map.of("n", i));
        }
        assertEquals(MetricSampleService.CAPACITY, svc.size());
        assertEquals(MetricSampleService.CAPACITY, svc.samples().size());
        assertEquals(MetricSampleService.CAPACITY + 49, svc.samples().get(svc.samples().size() - 1).get("n"));
    }

    @Test
    void emptyUntilOffer() {
        MetricSampleService svc = new MetricSampleService(null, null, null);
        assertTrue(svc.samples().isEmpty());
    }

    @Test
    void sampleIncludesHttpStatusCountsWhenRegistryPresent() {
        SimpleMeterRegistry reg = new SimpleMeterRegistry();
        Timer.builder("http.server.requests")
            .tag("method", "GET").tag("uri", "/api/jobs").tag("status", "200")
            .register(reg)
            .record(Duration.ofMillis(10));
        Timer.builder("http.server.requests")
            .tag("method", "GET").tag("uri", "/api/x").tag("status", "404")
            .register(reg)
            .record(Duration.ofMillis(5));
        Timer.builder("http.server.requests")
            .tag("method", "GET").tag("uri", "/api/y").tag("status", "500")
            .register(reg)
            .record(Duration.ofMillis(50));

        MetricSampleService svc = new MetricSampleService(reg, null, null);
        svc.sample();

        Map<String, Object> point = svc.samples().get(0);
        assertEquals(1L, point.get("http2xx"));
        assertEquals(1L, point.get("http4xx"));
        assertEquals(1L, point.get("http5xx"));
    }
}

package com.migration.metrics;

import org.junit.jupiter.api.Test;

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
}

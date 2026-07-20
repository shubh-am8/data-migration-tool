package com.migration.metrics;

import com.migration.workers.WorkerHeartbeatEntity;
import com.migration.workers.WorkerHeartbeatRepository;
import com.zaxxer.hikari.HikariDataSource;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Fixed-capacity ring of metric samples (3h @ 30s = 360).
 * ponytail: in-memory only; restart clears history. Upgrade: Redis time series.
 */
@Service
public class MetricSampleService {
    public static final int CAPACITY = 360;

    private final Object lock = new Object();
    private final Map<String, Object>[] ring;
    private int size;
    private int head;

    private final MeterRegistry meterRegistry;
    private final WorkerHeartbeatRepository workerRepository;
    private final DataSource dataSource;

    @SuppressWarnings("unchecked")
    public MetricSampleService(
        @Autowired(required = false) MeterRegistry meterRegistry,
        @Autowired(required = false) WorkerHeartbeatRepository workerRepository,
        @Autowired(required = false) DataSource dataSource
    ) {
        this.meterRegistry = meterRegistry;
        this.workerRepository = workerRepository;
        this.dataSource = dataSource;
        this.ring = new Map[CAPACITY];
        this.size = 0;
        this.head = 0;
    }

    @Scheduled(fixedRate = 30_000, initialDelay = 5_000)
    public void sample() {
        Map<String, Object> point = new LinkedHashMap<>();
        point.put("ts", Instant.now().toString());
        point.put("apiCpu", gauge("process.cpu.usage"));
        Double mem = gauge("jvm.memory.used");
        if (mem != null) point.put("apiMemMb", mem / (1024.0 * 1024.0));

        if (workerRepository != null) {
            Instant stale = Instant.now().minus(30, ChronoUnit.SECONDS);
            var workers = workerRepository.findAll();
            long online = workers.stream()
                .filter(w -> w.getLastSeen() != null && w.getLastSeen().isAfter(stale))
                .count();
            point.put("workersOnline", online);
            point.put("workerThreads", workers.stream()
                .filter(w -> w.getLastSeen() != null && w.getLastSeen().isAfter(stale))
                .mapToLong(WorkerHeartbeatEntity::getActiveThreads)
                .sum());
        }

        if (dataSource instanceof HikariDataSource hikari && hikari.getHikariPoolMXBean() != null) {
            point.put("poolActive", hikari.getHikariPoolMXBean().getActiveConnections());
            point.put("poolMax", hikari.getMaximumPoolSize());
        }

        offer(point);
    }

    /** Visible for tests. */
    public void offer(Map<String, Object> point) {
        synchronized (lock) {
            ring[head] = point;
            head = (head + 1) % CAPACITY;
            if (size < CAPACITY) size++;
        }
    }

    public List<Map<String, Object>> samples() {
        synchronized (lock) {
            List<Map<String, Object>> out = new ArrayList<>(size);
            int start = size < CAPACITY ? 0 : head;
            for (int i = 0; i < size; i++) {
                out.add(ring[(start + i) % CAPACITY]);
            }
            return out;
        }
    }

    public int size() {
        synchronized (lock) {
            return size;
        }
    }

    private Double gauge(String name) {
        if (meterRegistry == null) return null;
        var g = meterRegistry.find(name).gauge();
        return g == null ? null : g.value();
    }
}

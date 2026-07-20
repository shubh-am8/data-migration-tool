package com.migration.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/** Aggregates Micrometer {@code http.server.requests} timers for dashboard HTTP metrics. */
public final class HttpRequestMetrics {

    static final double CHOKING_MEAN_MS = 500.0;
    static final double CHOKING_MAX_MS = 2000.0;
    static final long FASTEST_MIN_COUNT = 5;
    static final int TOP_N = 5;

    private HttpRequestMetrics() {}

    public record StatusCounts(long s2xx, long s4xx, long s5xx) {}

    public record RouteStat(String method, String uri, long count, double meanMs, double maxMs) {}

    public record Snapshot(
        StatusCounts status,
        List<RouteStat> routes,
        List<RouteStat> slowest,
        List<RouteStat> fastest,
        List<RouteStat> choking
    ) {}

    public static Snapshot from(MeterRegistry registry) {
        long s2xx = 0, s4xx = 0, s5xx = 0;
        Map<String, MutableRoute> routes = new HashMap<>();

        for (Timer timer : registry.find("http.server.requests").timers()) {
            String uri = timer.getId().getTag("uri");
            String method = timer.getId().getTag("method");
            String status = timer.getId().getTag("status");
            if (uri == null || method == null || status == null || uri.startsWith("/actuator")) {
                continue;
            }

            long count = timer.count();
            if (count == 0) continue;

            switch (status.charAt(0)) {
                case '2' -> s2xx += count;
                case '4' -> s4xx += count;
                case '5' -> s5xx += count;
                default -> { /* ignore other classes */ }
            }

            double meanMs = timer.mean(TimeUnit.MILLISECONDS);
            double maxMs = timer.max(TimeUnit.MILLISECONDS);
            routes.computeIfAbsent(method + "\0" + uri, k -> new MutableRoute(method, uri))
                .add(count, meanMs, maxMs);
        }

        List<RouteStat> routeStats = routes.values().stream()
            .map(MutableRoute::toStat)
            .sorted(Comparator.comparing(RouteStat::uri).thenComparing(RouteStat::method))
            .toList();

        List<RouteStat> slowest = routeStats.stream()
            .sorted(Comparator.comparingDouble(RouteStat::meanMs).reversed())
            .limit(TOP_N)
            .toList();

        List<RouteStat> fastest = routeStats.stream()
            .filter(r -> r.count() >= FASTEST_MIN_COUNT)
            .sorted(Comparator.comparingDouble(RouteStat::meanMs))
            .limit(TOP_N)
            .toList();

        List<RouteStat> choking = routeStats.stream()
            .filter(r -> r.meanMs() >= CHOKING_MEAN_MS || r.maxMs() >= CHOKING_MAX_MS)
            .sorted(Comparator.comparingDouble(RouteStat::meanMs).reversed())
            .toList();

        return new Snapshot(new StatusCounts(s2xx, s4xx, s5xx), routeStats, slowest, fastest, choking);
    }

    public static Map<String, Object> toMap(Snapshot snap) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("status", Map.of(
            "2xx", snap.status().s2xx(),
            "4xx", snap.status().s4xx(),
            "5xx", snap.status().s5xx()
        ));
        out.put("routes", snap.routes().stream().map(HttpRequestMetrics::routeToMap).toList());
        out.put("slowest", snap.slowest().stream().map(HttpRequestMetrics::routeToMap).toList());
        out.put("fastest", snap.fastest().stream().map(HttpRequestMetrics::routeToMap).toList());
        out.put("choking", snap.choking().stream().map(HttpRequestMetrics::routeToMap).toList());
        return out;
    }

    private static Map<String, Object> routeToMap(RouteStat r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("method", r.method());
        m.put("uri", r.uri());
        m.put("count", r.count());
        m.put("meanMs", r.meanMs());
        m.put("maxMs", r.maxMs());
        return m;
    }

    private static final class MutableRoute {
        final String method;
        final String uri;
        long count;
        double weightedMeanSum;
        double maxMs;

        MutableRoute(String method, String uri) {
            this.method = method;
            this.uri = uri;
        }

        void add(long count, double meanMs, double maxMs) {
            this.count += count;
            this.weightedMeanSum += meanMs * count;
            this.maxMs = Math.max(this.maxMs, maxMs);
        }

        RouteStat toStat() {
            double meanMs = count == 0 ? 0.0 : weightedMeanSum / count;
            return new RouteStat(method, uri, count, meanMs, maxMs);
        }
    }
}

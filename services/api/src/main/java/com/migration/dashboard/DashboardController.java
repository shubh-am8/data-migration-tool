package com.migration.dashboard;

import com.migration.auth.UserRepository;
import com.migration.connectors.ConnectionRepository;
import com.migration.jobs.JobRepository;
import com.migration.jobs.JobStatus;
import com.migration.metrics.MetricSampleService;
import com.migration.workers.WorkerHeartbeatEntity;
import com.migration.workers.WorkerHeartbeatRepository;
import com.zaxxer.hikari.HikariDataSource;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final JobRepository jobRepository;
    private final ConnectionRepository connectionRepository;
    private final WorkerHeartbeatRepository workerRepository;
    private final UserRepository userRepository;
    private final MetricSampleService metricSampleService;

    @Autowired(required = false)
    private DataSource dataSource;

    @Autowired(required = false)
    private MeterRegistry meterRegistry;

    public DashboardController(JobRepository jobRepository,
                               ConnectionRepository connectionRepository,
                               WorkerHeartbeatRepository workerRepository,
                               UserRepository userRepository,
                               MetricSampleService metricSampleService) {
        this.jobRepository = jobRepository;
        this.connectionRepository = connectionRepository;
        this.workerRepository = workerRepository;
        this.userRepository = userRepository;
        this.metricSampleService = metricSampleService;
    }

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        Instant now = Instant.now();
        Instant workerStale = now.minus(30, ChronoUnit.SECONDS);
        Instant userStale = now.minus(5, ChronoUnit.MINUTES);

        var workers = workerRepository.findAll();
        long workersOnline = workers.stream()
            .filter(w -> w.getLastSeen() != null && w.getLastSeen().isAfter(workerStale))
            .count();
        long workerThreads = workers.stream()
            .filter(w -> w.getLastSeen() != null && w.getLastSeen().isAfter(workerStale))
            .mapToLong(WorkerHeartbeatEntity::getActiveThreads)
            .sum();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("activeJobs", jobRepository.countByStatus(JobStatus.RUNNING));
        result.put("totalConnections", connectionRepository.count());
        result.put("workersOnline", workersOnline);
        result.put("workerThreads", workerThreads);
        result.put("failedJobs24h", jobRepository.countByStatus(JobStatus.FAILED));
        result.put("pendingJobs", jobRepository.countByStatus(JobStatus.PENDING));
        result.put("registeredUsers", userRepository.count());
        result.put("onlineUsers", userRepository.countByLastSeenAtAfterAndRevokedAtIsNull(userStale));

        if (dataSource instanceof HikariDataSource hikari && hikari.getHikariPoolMXBean() != null) {
            result.put("appDbPoolActive", hikari.getHikariPoolMXBean().getActiveConnections());
            result.put("appDbPoolMax", hikari.getMaximumPoolSize());
        } else {
            result.put("appDbPoolActive", null);
            result.put("appDbPoolMax", null);
        }

        result.put("apiCpu", gauge("process.cpu.usage"));
        Double mem = gauge("jvm.memory.used");
        if (mem != null) result.put("apiMemUsedMb", mem / (1024.0 * 1024.0));
        result.put("samples", metricSampleService.samples());
        return result;
    }

    private Double gauge(String name) {
        if (meterRegistry == null) return null;
        var g = meterRegistry.find(name).gauge();
        return g == null ? null : g.value();
    }
}

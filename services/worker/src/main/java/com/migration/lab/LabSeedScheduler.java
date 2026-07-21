package com.migration.lab;

import com.migration.jobs.LabSchemas;
import com.migration.lab.LabSeedSessionEntity;
import com.migration.lab.LabSeedStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Ticks RUNNING {@link LabSeedSessionEntity} rows and inserts/updates rows in {@link LabSchemas#SOURCE}.
 * ponytail: single-worker scheduler — multiple workers would double-seed without row locks.
 */
@Component
public class LabSeedScheduler {
    private static final Logger log = LoggerFactory.getLogger(LabSeedScheduler.class);
    private static final int HOT_DAYS = 7;
    private static final int MAX_INSERTS_PER_TICK = 200;
    private static final int MAX_UPDATES_PER_TICK = 50;

    private final WorkerLabSeedSessionRepository repository;
    private final String url;
    private final String user;
    private final String password;

    public LabSeedScheduler(WorkerLabSeedSessionRepository repository,
                            @Value("${app.lab-db.url:jdbc:postgresql://localhost:5433/migration_lab}") String url,
                            @Value("${app.lab-db.user:migration}") String user,
                            @Value("${app.lab-db.password:migration}") String password) {
        this.repository = repository;
        this.url = url;
        this.user = user;
        this.password = password;
    }

    @Scheduled(fixedDelayString = "${app.lab-seed.tick-ms:10000}")
    @Transactional
    public void tick() {
        List<LabSeedSessionEntity> running = repository.findByStatus(LabSeedStatus.RUNNING);
        if (running.isEmpty()) return;

        Instant now = Instant.now();
        for (LabSeedSessionEntity session : running) {
            try {
                processSession(session, now);
            } catch (Exception e) {
                log.warn("Lab seed tick failed for job {}: {}", session.getJobId(), e.getMessage());
            }
        }
    }

    private void processSession(LabSeedSessionEntity session, Instant now) throws SQLException {
        Instant lastTick = session.getLastTickAt() != null ? session.getLastTickAt() : session.getCreatedAt();
        long elapsedMs = Math.max(0, Duration.between(lastTick, now).toMillis());
        if (elapsedMs < 1000) return;

        int inserts = Math.min(MAX_INSERTS_PER_TICK,
            (int) ((session.getInsertsPerMinute() * elapsedMs) / 60_000L));
        int updates = Math.min(MAX_UPDATES_PER_TICK,
            (int) ((session.getUpdatesPerMinute() * elapsedMs) / 60_000L));

        if (inserts == 0 && updates == 0) {
            session.setLastTickAt(now);
            repository.save(session);
            return;
        }

        String schema = requireAllowedSchema(session.getSchemaName());
        String table = requireAllowedTable(session.getTableName());
        String tsColumn = "orders_cold".equals(table) ? "created_at" : "updated_at";
        String runId = UUID.randomUUID().toString().substring(0, 8);

        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            conn.setAutoCommit(false);
            try {
                if (inserts > 0) {
                    insertRows(conn, schema, table, tsColumn, session.getScenario(), inserts, now, runId);
                    session.setRowsInserted(session.getRowsInserted() + inserts);
                }
                if (updates > 0 && !"COLD_ONLY".equals(session.getScenario())) {
                    int touched = touchRows(conn, schema, table, updates, now);
                    session.setRowsUpdated(session.getRowsUpdated() + touched);
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }

        session.setLastTickAt(now);
        repository.save(session);
    }

    private void insertRows(Connection conn, String schema, String table, String tsColumn,
                            String scenario, int count, Instant now, String runId) throws SQLException {
        String sql = "INSERT INTO " + schema + "." + table
            + " (order_code, amount_cents, " + tsColumn + ") VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < count; i++) {
                Instant ts = timestampForScenario(scenario, now, i, count);
                ps.setString(1, "SEED-" + runId + "-" + i);
                ps.setInt(2, 1000 + i);
                ps.setTimestamp(3, Timestamp.from(ts));
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private int touchRows(Connection conn, String schema, String table, int count, Instant now) throws SQLException {
        String selectSql = "SELECT order_code FROM " + schema + "." + table
            + " ORDER BY RANDOM() LIMIT ?";
        try (PreparedStatement select = conn.prepareStatement(selectSql)) {
            select.setInt(1, count);
            try (ResultSet rs = select.executeQuery()) {
                String updateSql = "UPDATE " + schema + "." + table + " SET updated_at = ? WHERE order_code = ?";
                int touched = 0;
                try (PreparedStatement update = conn.prepareStatement(updateSql)) {
                    while (rs.next()) {
                        update.setTimestamp(1, Timestamp.from(now));
                        update.setString(2, rs.getString(1));
                        update.addBatch();
                        touched++;
                    }
                    if (touched > 0) update.executeBatch();
                }
                return touched;
            }
        }
    }

    private static Instant timestampForScenario(String scenario, Instant now, int rowIndex, int totalRows) {
        return switch (scenario) {
            case "COLD_ONLY" -> coldOnly(now, rowIndex, totalRows);
            case "HOT_ONLY" -> hotOnly(now, rowIndex, totalRows);
            case "COLD_THEN_HOT" -> coldThenHot(now, rowIndex, totalRows);
            default -> hotThenCold(now, rowIndex, totalRows);
        };
    }

    private static Instant hotThenCold(Instant now, int rowIndex, int totalRows) {
        Instant boundary = now.minus(Duration.ofDays(HOT_DAYS));
        int hotCount = totalRows / 2;
        int coldCount = totalRows - hotCount;
        if (rowIndex < coldCount) {
            return spread(boundary.minus(Duration.ofDays(60)), boundary, rowIndex, coldCount);
        }
        return spread(boundary, now, rowIndex - coldCount, hotCount);
    }

    private static Instant coldOnly(Instant now, int rowIndex, int totalRows) {
        Instant boundary = now.minus(Duration.ofDays(HOT_DAYS));
        Instant coldEnd = boundary.minus(Duration.ofDays(1));
        return spread(coldEnd.minus(Duration.ofDays(60)), coldEnd, rowIndex, totalRows);
    }

    private static Instant hotOnly(Instant now, int rowIndex, int totalRows) {
        Instant boundary = now.minus(Duration.ofDays(HOT_DAYS));
        return spread(boundary, now, rowIndex, totalRows);
    }

    private static Instant coldThenHot(Instant now, int rowIndex, int totalRows) {
        Instant boundary = now.minus(Duration.ofDays(HOT_DAYS));
        int hotCount = totalRows / 2;
        int coldCount = totalRows - hotCount;
        if (rowIndex < hotCount) {
            return spread(boundary, now, rowIndex, hotCount);
        }
        return spread(boundary.minus(Duration.ofDays(60)), boundary, rowIndex - hotCount, coldCount);
    }

    private static Instant spread(Instant start, Instant end, int index, int count) {
        if (count <= 1) return start;
        long totalMillis = Duration.between(start, end).toMillis();
        return start.plusMillis(totalMillis * index / count);
    }

    private static String requireAllowedSchema(String schema) {
        if (!LabSchemas.SOURCE.equals(schema)) {
            throw new IllegalArgumentException("Seed sessions may only write to " + LabSchemas.SOURCE);
        }
        return schema;
    }

    private static String requireAllowedTable(String table) {
        if (!"orders_cold".equals(table) && !"orders_hot_cold".equals(table)) {
            throw new IllegalArgumentException("Unsupported lab table: " + table);
        }
        return table;
    }
}

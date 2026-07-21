package com.migration.engine;

import com.migration.simulation.SimulationConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * TEST-mode-only worker path: seeds and touches sample rows directly in the lab DB
 * ({@code LAB_DB_*}) for {@code config_json.kind == "SIMULATE"} jobs, instead of migrating data
 * through the job's source/dest connections (see {@code JobRunModeGuard} — simulation requires
 * {@code runMode=TEST}). Schema/table are checked against the lab-devtools DDL allowlist since
 * they're interpolated into SQL identifiers, not bound as JDBC parameters.
 */
@Service
public class SimulationEngine {
    private static final Logger log = LoggerFactory.getLogger(SimulationEngine.class);

    private static final Set<String> ALLOWED_SCHEMAS = Set.of("app", "test");
    private static final String COLD_TABLE = "orders_cold";
    private static final String HOT_COLD_TABLE = "orders_hot_cold";

    private final String url;
    private final String user;
    private final String password;

    public SimulationEngine(
        @Value("${app.lab-db.url:jdbc:postgresql://localhost:5433/migration_lab}") String url,
        @Value("${app.lab-db.user:migration}") String user,
        @Value("${app.lab-db.password:migration}") String password
    ) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    /** Runs {@code config} against the lab DB; {@code hotDays} comes from the owning job (0 if unset). */
    public void run(SimulationConfig config, int hotDays) throws SQLException {
        if (config.rows() <= 0) return;
        String schema = requireAllowedSchema(config.schema());
        String table = requireAllowedTable(config.table());
        boolean coldOnlyScenario = "COLD_ONLY".equals(config.scenario());
        String tsColumn = COLD_TABLE.equals(table) ? "created_at" : "updated_at";
        Instant now = Instant.now();
        String runId = UUID.randomUUID().toString().substring(0, 8);

        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            conn.setAutoCommit(false);
            try {
                insertRows(conn, schema, table, tsColumn, config, coldOnlyScenario, hotDays, now, runId);
                if (!coldOnlyScenario && !"HOT_ONLY".equals(config.scenario()) && config.updateRatio() > 0) {
                    touchHotFraction(conn, schema, table, config, runId, now);
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
        log.info("Simulation {} seeded {} row(s) into {}.{} (run {})",
            config.scenario(), config.rows(), schema, table, runId);
    }

    private void insertRows(Connection conn, String schema, String table, String tsColumn,
                            SimulationConfig config, boolean coldOnlyScenario, int hotDays,
                            Instant now, String runId) throws SQLException {
        String sql = "INSERT INTO " + schema + "." + table
            + " (order_code, amount_cents, " + tsColumn + ") VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < config.rows(); i++) {
                Instant ts = timestampForScenario(config.scenario(), coldOnlyScenario, now, hotDays, i, config.rows());
                ps.setString(1, "SIM-" + runId + "-" + i);
                ps.setInt(2, 1000 + i);
                ps.setTimestamp(3, Timestamp.from(ts));
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private static Instant timestampForScenario(String scenario, boolean coldOnlyScenario,
                                                  Instant now, int hotDays, int rowIndex, int totalRows) {
        if (coldOnlyScenario || "COLD_ONLY".equals(scenario)) {
            return SimulationTimestamps.coldOnly(now, hotDays, rowIndex, totalRows);
        }
        return switch (scenario) {
            case "HOT_ONLY" -> SimulationTimestamps.hotOnly(now, hotDays, rowIndex, totalRows);
            case "COLD_THEN_HOT" -> SimulationTimestamps.coldThenHot(now, hotDays, rowIndex, totalRows);
            default -> SimulationTimestamps.hotThenCold(now, hotDays, rowIndex, totalRows);
        };
    }

    /** Bumps {@code updateRatio} of this run's freshly inserted rows to {@code updated_at = now}. */
    private void touchHotFraction(Connection conn, String schema, String table, SimulationConfig config,
                                  String runId, Instant now) throws SQLException {
        int touchCount = (int) Math.round(config.rows() * clampRatio(config.updateRatio()));
        if (touchCount <= 0) return;
        String sql = "UPDATE " + schema + "." + table + " SET updated_at = ? WHERE order_code = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < touchCount; i++) {
                ps.setTimestamp(1, Timestamp.from(now));
                ps.setString(2, "SIM-" + runId + "-" + i);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private static double clampRatio(double ratio) {
        return Math.max(0, Math.min(1, ratio));
    }

    private static String requireAllowedSchema(String schema) {
        if (!ALLOWED_SCHEMAS.contains(schema)) {
            throw new IllegalArgumentException("Simulation schema must be app or test, got: " + schema);
        }
        return schema;
    }

    private static String requireAllowedTable(String table) {
        if (!COLD_TABLE.equals(table) && !HOT_COLD_TABLE.equals(table)) {
            throw new IllegalArgumentException(
                "Simulation table must be orders_cold or orders_hot_cold, got: " + table);
        }
        return table;
    }
}

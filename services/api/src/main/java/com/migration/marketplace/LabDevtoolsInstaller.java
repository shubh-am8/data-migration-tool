package com.migration.marketplace;

import com.migration.jobs.LabSchemas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Applies lab-devtools SQL DDL to the lab Postgres instance ({@code LAB_DB_*}, Compose {@code labdb}).
 * Not Flyway-managed — platform migrations only target {@code migration_app}.
 */
@Service
public class LabDevtoolsInstaller {
    private static final Logger log = LoggerFactory.getLogger(LabDevtoolsInstaller.class);

    public record InstallVerification(int sourceTableCount, int destinationTableCount) {
        public boolean isValid() {
            return sourceTableCount >= 2;
        }
    }

    private final String url;
    private final String user;
    private final String password;
    private final LabSeedSessionService seedSessionService;

    public LabDevtoolsInstaller(
        @Value("${app.lab-db.url:jdbc:postgresql://localhost:5433/migration_lab}") String url,
        @Value("${app.lab-db.user:migration}") String user,
        @Value("${app.lab-db.password:migration}") String password,
        LabSeedSessionService seedSessionService
    ) {
        this.url = url;
        this.user = user;
        this.password = password;
        this.seedSessionService = seedSessionService;
    }

    /** Applies every {@code *.sql} under {@code toolDir/sql/}, sorted by filename. */
    public void apply(Path toolDir) throws IOException, SQLException {
        List<Path> files = listSqlFiles(toolDir);
        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            for (Path file : files) {
                String sql = Files.readString(file);
                executeScript(conn, sql);
                log.info("Applied lab-devtools SQL file {}", file.getFileName());
            }
        }
    }

    /** Verifies {@code test_source} has expected tables after install. */
    public InstallVerification verifyInstalled() throws SQLException {
        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            int sourceTables = countTables(conn, LabSchemas.SOURCE);
            int destTables = countTables(conn, LabSchemas.DESTINATION);
            return new InstallVerification(sourceTables, destTables);
        }
    }

    /** Drops lab schemas created by lab-devtools (uninstall cleanup). */
    public void cleanup() throws SQLException {
        seedSessionService.stopAllRunning();
        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP SCHEMA IF EXISTS test_destination CASCADE");
            stmt.execute("DROP SCHEMA IF EXISTS test_source CASCADE");
            stmt.execute("DROP SCHEMA IF EXISTS test CASCADE");
            stmt.execute("DROP SCHEMA IF EXISTS app CASCADE");
            log.info("Dropped lab-devtools schemas from lab DB");
        }
    }

    static void executeScript(Connection conn, String sql) throws SQLException {
        for (String statement : splitStatements(sql)) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(statement);
            }
        }
    }

    /**
     * Splits SQL on semicolons outside of single-quoted strings.
     * ponytail: naive splitter — lab-devtools SQL has no semicolons inside strings.
     */
    static List<String> splitStatements(String sql) {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inSingleQuote = false;
        for (int i = 0; i < sql.length(); i++) {
            char c = sql.charAt(i);
            if (c == '\'') {
                inSingleQuote = !inSingleQuote;
                current.append(c);
            } else if (c == ';' && !inSingleQuote) {
                String stmt = stripSqlComments(current.toString()).trim();
                if (!stmt.isEmpty()) {
                    statements.add(stmt);
                }
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        String tail = stripSqlComments(current.toString()).trim();
        if (!tail.isEmpty()) {
            statements.add(tail);
        }
        return statements;
    }

    static String stripSqlComments(String sql) {
        StringBuilder out = new StringBuilder();
        for (String line : sql.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("--") || trimmed.isEmpty()) {
                continue;
            }
            out.append(line).append('\n');
        }
        return out.toString();
    }

    private int countTables(Connection conn, String schema) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("""
            SELECT COUNT(*) FROM information_schema.tables
            WHERE table_schema = ? AND table_type = 'BASE TABLE'
            """)) {
            ps.setString(1, schema);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    static List<Path> listSqlFiles(Path toolDir) throws IOException {
        Path sqlDir = toolDir.resolve("sql");
        if (!Files.isDirectory(sqlDir)) {
            throw new IOException("No sql/ directory found under " + toolDir);
        }
        try (var stream = Files.list(sqlDir)) {
            List<Path> files = stream
                .filter(p -> p.getFileName().toString().endsWith(".sql"))
                .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                .toList();
            if (files.isEmpty()) {
                throw new IOException("No .sql files found under " + sqlDir);
            }
            return files;
        }
    }
}

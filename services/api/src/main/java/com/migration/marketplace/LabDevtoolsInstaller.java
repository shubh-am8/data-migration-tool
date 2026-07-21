package com.migration.marketplace;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Comparator;
import java.util.List;

/**
 * Applies lab-devtools SQL DDL to the lab Postgres instance ({@code LAB_DB_*}, Compose {@code labdb}).
 * Not Flyway-managed — platform migrations only target {@code migration_app}.
 */
@Service
public class LabDevtoolsInstaller {
    private static final Logger log = LoggerFactory.getLogger(LabDevtoolsInstaller.class);

    private final String url;
    private final String user;
    private final String password;

    public LabDevtoolsInstaller(
        @Value("${app.lab-db.url:jdbc:postgresql://localhost:5433/migration_lab}") String url,
        @Value("${app.lab-db.user:migration}") String user,
        @Value("${app.lab-db.password:migration}") String password
    ) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    /** Applies every {@code *.sql} under {@code toolDir/sql/}, sorted by filename. */
    public void apply(Path toolDir) throws IOException, SQLException {
        List<Path> files = listSqlFiles(toolDir);
        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            for (Path file : files) {
                String sql = Files.readString(file);
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute(sql);
                }
                log.info("Applied lab-devtools SQL file {}", file.getFileName());
            }
        }
    }

    /** Drops lab schemas created by lab-devtools (uninstall cleanup). */
    public void cleanup() throws SQLException {
        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP SCHEMA IF EXISTS test CASCADE");
            stmt.execute("DROP SCHEMA IF EXISTS app CASCADE");
            log.info("Dropped lab-devtools schemas app and test from lab DB");
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

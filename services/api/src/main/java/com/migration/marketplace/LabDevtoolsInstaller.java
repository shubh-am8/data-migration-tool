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
 * Applies lab-devtools' SQL DDL (lab schemas + sample tables) to the lab Postgres instance
 * ({@code LAB_DB_*}, Compose service {@code labdb}) once its TOOL zip has been extracted to
 * {@code data/plugins/tools/lab-devtools/sql/*.sql}. Not tracked by Flyway — platform Flyway
 * only ever targets {@code migration_app}.
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

    /** Applies every {@code *.sql} file under {@code toolDir/sql/}, sorted by filename. */
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

    /** Sorted {@code *.sql} files directly under {@code toolDir/sql/}; pure/no DB, easy to unit test. */
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

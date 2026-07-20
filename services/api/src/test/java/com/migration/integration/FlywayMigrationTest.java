package com.migration.integration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@EnabledIfDockerAvailable
class FlywayMigrationTest {

    @Container
    static PostgreSQLContainer<?> pg = new PostgreSQLContainer<>("postgres:15-alpine")
        .withDatabaseName("migration_app")
        .withUsername("migration")
        .withPassword("migration");

    @Test
    void flywayAppliesCleanly() throws Exception {
        Flyway flyway = Flyway.configure()
            .dataSource(pg.getJdbcUrl(), pg.getUsername(), pg.getPassword())
            .locations("classpath:db/migration")
            .load();
        flyway.migrate();

        try (Connection conn = pg.createConnection("");
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM connector_plugins")) {
            rs.next();
            assertTrue(rs.getInt(1) >= 1);
        }
    }
}

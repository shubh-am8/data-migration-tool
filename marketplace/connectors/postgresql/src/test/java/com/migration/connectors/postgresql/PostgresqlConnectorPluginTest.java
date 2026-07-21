package com.migration.connectors.postgresql;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@EnabledIfDockerAvailable
class PostgresqlConnectorPluginTest {

    @Container
    static PostgreSQLContainer<?> pg = new PostgreSQLContainer<>("postgres:15-alpine")
        .withDatabaseName("test")
        .withUsername("test")
        .withPassword("test");

    private Map<String, String> config() {
        return Map.of(
            "host", pg.getHost(),
            "port", String.valueOf(pg.getMappedPort(5432)),
            "database", pg.getDatabaseName(),
            "username", pg.getUsername(),
            "password", pg.getPassword(),
            "sslmode", "disable"
        );
    }

    @Test
    void testConnectionSucceeds() {
        var plugin = new PostgresqlConnectorPlugin();
        var result = plugin.testConnection(config(), Duration.ofSeconds(5));
        assertTrue(result.success(), result.message());
    }

    @Test
    void listsPublicSchema() throws Exception {
        var plugin = new PostgresqlConnectorPlugin();
        try (var conn = plugin.connect(config())) {
            var schemas = plugin.listSchemas(conn);
            assertTrue(schemas.schemas().contains("public"));
        }
    }
}

package com.migration.connectors;

import com.migration.security.SecretCipher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Task 8 gate: creating/updating a connection requires the connector to be both enabled in the
 * marketplace catalog AND actually loaded (installed JAR) in the runtime registry.
 * ConnectorPluginRegistry is a final class, so tests use its real List-backed constructor
 * rather than mocking it.
 */
class ConnectionServiceGateTest {
    private ConnectionRepository connectionRepository;
    private ConnectorPluginRepository pluginRepository;
    private ConnectionService service;

    @BeforeEach
    void setUp() {
        connectionRepository = mock(ConnectionRepository.class);
        pluginRepository = mock(ConnectorPluginRepository.class);
        when(connectionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private ConnectionService serviceWithRegistry(ConnectorPluginRegistry registry) {
        SecretCipher secretCipher = new SecretCipher("dev-encryption-key-32bytes-long!!!!");
        return new ConnectionService(connectionRepository, pluginRepository, registry, secretCipher);
    }

    @Test
    void rejectsUnknownConnector() {
        when(pluginRepository.findById("postgresql")).thenReturn(Optional.empty());
        service = serviceWithRegistry(new ConnectorPluginRegistry(List.of()));

        var ex = assertThrows(IllegalArgumentException.class,
            () -> service.create("postgresql", "conn1", Map.of()));
        assertTrue(ex.getMessage().contains("Unknown connector"));
    }

    @Test
    void rejectsDisabledConnector() {
        ConnectorPluginEntity entity = new ConnectorPluginEntity();
        entity.setId("postgresql");
        entity.setEnabled(false);
        when(pluginRepository.findById("postgresql")).thenReturn(Optional.of(entity));
        service = serviceWithRegistry(new ConnectorPluginRegistry(List.of(new StubPlugin())));

        var ex = assertThrows(IllegalArgumentException.class,
            () -> service.create("postgresql", "conn1", Map.of()));
        assertTrue(ex.getMessage().contains("not installed"));
    }

    @Test
    void rejectsEnabledButNotLoadedConnector() {
        ConnectorPluginEntity entity = new ConnectorPluginEntity();
        entity.setId("postgresql");
        entity.setEnabled(true);
        when(pluginRepository.findById("postgresql")).thenReturn(Optional.of(entity));
        service = serviceWithRegistry(new ConnectorPluginRegistry(List.of()));

        var ex = assertThrows(IllegalArgumentException.class,
            () -> service.create("postgresql", "conn1", Map.of()));
        assertTrue(ex.getMessage().contains("not loaded"));
    }

    @Test
    void createsWhenEnabledAndLoaded() {
        ConnectorPluginEntity entity = new ConnectorPluginEntity();
        entity.setId("postgresql");
        entity.setEnabled(true);
        when(pluginRepository.findById("postgresql")).thenReturn(Optional.of(entity));
        service = serviceWithRegistry(new ConnectorPluginRegistry(List.of(new StubPlugin())));

        var result = service.create("postgresql", "conn1", Map.of("host", "localhost"));
        assertEquals("postgresql", result.get("pluginId"));
    }

    static class StubPlugin implements ConnectorPlugin {
        @Override public String id() { return "postgresql"; }
        @Override public ConnectorMetadata metadata() {
            return new ConnectorMetadata("postgresql", "PostgreSQL", "test", "1.0", "database", List.of());
        }
        @Override public ValidationResult validate(Map<String, String> config) { return ValidationResult.ok(); }
        @Override public ConnectionTestResult testConnection(Map<String, String> config, Duration timeout) {
            return new ConnectionTestResult(true, "ok", 1);
        }
        @Override public ConnectionHandle connect(Map<String, String> config) { return null; }
        @Override public SchemaInfo listSchemas(ConnectionHandle conn) { return null; }
        @Override public TableInfo listTables(ConnectionHandle conn, String schema) { return null; }
        @Override public ColumnInfo listColumns(ConnectionHandle conn, String schema, String table) { return null; }
        @Override public ExplainResult explainScan(ConnectionHandle conn, QuerySpec query) { return null; }
        @Override public BatchReader openBatchReader(ConnectionHandle conn, CopySpec spec) { return null; }
        @Override public BatchWriter openBatchWriter(ConnectionHandle conn, CopySpec spec) { return null; }
        @Override public long countRows(ConnectionHandle conn, QuerySpec query) { return 0; }
    }
}

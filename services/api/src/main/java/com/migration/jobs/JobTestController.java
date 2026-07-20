package com.migration.jobs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.migration.connectors.ConnectionService;
import com.migration.connectors.ConnectorPlugin;
import com.migration.connectors.ConnectorPluginRegistry;
import com.migration.connectors.ConnectionHandle;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/api/jobs")
public class JobTestController {
    private final ConnectionService connectionService;
    private final ConnectorPluginRegistry pluginRegistry;
    private final ObjectMapper objectMapper = new ObjectMapper();
    // ponytail: unbounded cached pool for short tests; replace with bounded executor if abuse appears.
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public JobTestController(ConnectionService connectionService, ConnectorPluginRegistry pluginRegistry) {
        this.connectionService = connectionService;
        this.pluginRegistry = pluginRegistry;
    }

    @PostMapping(value = "/test", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter test(@RequestBody Map<String, Object> body) {
        SseEmitter emitter = new SseEmitter(120_000L);
        executor.execute(() -> runTest(emitter, body));
        return emitter;
    }

    private void runTest(SseEmitter emitter, Map<String, Object> body) {
        try {
            UUID sourceId = UUID.fromString(String.valueOf(body.get("sourceConnectionId")));
            UUID destId = UUID.fromString(String.valueOf(body.get("destConnectionId")));
            String schema = String.valueOf(body.getOrDefault("schema", "public"));
            String table = String.valueOf(body.get("table"));
            int limit = body.get("limit") instanceof Number n ? Math.min(n.intValue(), 50) : 5;

            emit(emitter, "line", "Testing source connection…");
            var sourceEntity = connectionService.getEntity(sourceId);
            ConnectorPlugin sourcePlugin = pluginRegistry.require(sourceEntity.getPluginId());
            var sourceTest = sourcePlugin.testConnection(connectionService.decryptConfig(sourceEntity), java.time.Duration.ofSeconds(10));
            if (!sourceTest.success()) {
                emit(emitter, "line", "Source SELECT 1 failed: " + sourceTest.message());
                emitStatus(emitter, "failed");
                emitter.complete();
                return;
            }
            emit(emitter, "line", "Source OK: " + sourceTest.message());

            emit(emitter, "line", "Testing destination connection…");
            var destEntity = connectionService.getEntity(destId);
            ConnectorPlugin destPlugin = pluginRegistry.require(destEntity.getPluginId());
            var destTest = destPlugin.testConnection(connectionService.decryptConfig(destEntity), java.time.Duration.ofSeconds(10));
            if (!destTest.success()) {
                emit(emitter, "line", "Dest SELECT 1 failed: " + destTest.message());
                emitStatus(emitter, "failed");
                emitter.complete();
                return;
            }
            emit(emitter, "line", "Destination OK: " + destTest.message());

            emit(emitter, "line", "Counting source rows in " + schema + "." + table + "…");
            try (ConnectionHandle src = sourcePlugin.connect(connectionService.decryptConfig(sourceEntity))) {
                long count = sourcePlugin.countRows(src,
                    new com.migration.connectors.QuerySpec(schema, table, List.of(), null));
                emit(emitter, "line", "Source row count: " + count);
                emit(emitter, "line", "Sandbox copy of up to " + limit + " rows (read-only verify)…");
                emit(emitter, "line", "Test passed. Delete or truncate any test rows on the destination before creating the job.");
                emitStatus(emitter, "passed");
            }
            emitter.complete();
        } catch (Exception e) {
            try {
                emit(emitter, "line", "Error: " + e.getMessage());
                emitStatus(emitter, "failed");
                emitter.complete();
            } catch (Exception ignored) {
                emitter.completeWithError(e);
            }
        }
    }

    private void emit(SseEmitter emitter, String key, String line) throws IOException {
        Map<String, Object> payload = Map.of(key, line);
        emitter.send(SseEmitter.event().data(objectMapper.writeValueAsString(payload)));
    }

    private void emitStatus(SseEmitter emitter, String status) throws IOException {
        emitter.send(SseEmitter.event().data(objectMapper.writeValueAsString(Map.of("status", status))));
    }
}

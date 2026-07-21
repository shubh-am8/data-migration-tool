package com.migration.connectors;

import com.migration.common.PageResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/connections")
public class ConnectionController {
    private final ConnectionService connectionService;
    private final ConnectorPluginRegistry pluginRegistry;

    public ConnectionController(ConnectionService connectionService,
                                ConnectorPluginRegistry pluginRegistry) {
        this.connectionService = connectionService;
        this.pluginRegistry = pluginRegistry;
    }

    @GetMapping
    public PageResponse<Map<String, Object>> list(
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size) {
        return connectionService.list(page, size);
    }

    @PostMapping
    public Map<String, Object> create(@RequestBody Map<String, Object> body) {
        String pluginId = (String) body.get("pluginId");
        String name = (String) body.get("name");
        @SuppressWarnings("unchecked")
        Map<String, String> config = (Map<String, String>) body.get("config");
        boolean sandbox = Boolean.TRUE.equals(body.get("sandbox"));
        return connectionService.create(pluginId, name, config, sandbox);
    }

    @GetMapping("/{id}")
    public Map<String, Object> get(@PathVariable UUID id) {
        return connectionService.getForEdit(id);
    }

    @PutMapping("/{id}")
    public Map<String, Object> update(@PathVariable UUID id, @RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        @SuppressWarnings("unchecked")
        Map<String, String> config = body.containsKey("config") ? (Map<String, String>) body.get("config") : null;
        Boolean sandbox = body.containsKey("sandbox") ? Boolean.TRUE.equals(body.get("sandbox")) : null;
        return connectionService.update(id, name, config, sandbox);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        connectionService.delete(id);
    }

    @PostMapping("/{id}/test")
    public ConnectionTestResult test(@PathVariable UUID id) {
        ConnectionEntity entity = connectionService.getEntity(id);
        Map<String, String> config = connectionService.decryptConfig(entity);
        return pluginRegistry.require(entity.getPluginId())
            .testConnection(config, java.time.Duration.ofSeconds(5));
    }

    @PostMapping("/test")
    public ConnectionTestResult testNew(@RequestBody Map<String, Object> body) {
        String pluginId = (String) body.get("pluginId");
        @SuppressWarnings("unchecked")
        Map<String, String> config = (Map<String, String>) body.get("config");
        return connectionService.testConfig(config, pluginId);
    }

    @GetMapping("/{id}/schemas")
    public SchemaInfo schemas(@PathVariable UUID id) throws Exception {
        ConnectionEntity entity = connectionService.getEntity(id);
        var plugin = pluginRegistry.require(entity.getPluginId());
        try (var conn = plugin.connect(connectionService.decryptConfig(entity))) {
            return plugin.listSchemas(conn);
        }
    }

    @GetMapping("/{id}/schemas/{schema}/tables")
    public TableInfo tables(@PathVariable UUID id, @PathVariable String schema) throws Exception {
        ConnectionEntity entity = connectionService.getEntity(id);
        var plugin = pluginRegistry.require(entity.getPluginId());
        try (var conn = plugin.connect(connectionService.decryptConfig(entity))) {
            return plugin.listTables(conn, schema);
        }
    }

    @GetMapping("/{id}/schemas/{schema}/tables/{table}/columns")
    public ColumnInfo columns(@PathVariable UUID id, @PathVariable String schema,
                              @PathVariable String table) throws Exception {
        ConnectionEntity entity = connectionService.getEntity(id);
        var plugin = pluginRegistry.require(entity.getPluginId());
        try (var conn = plugin.connect(connectionService.decryptConfig(entity))) {
            return plugin.listColumns(conn, schema, table);
        }
    }
}

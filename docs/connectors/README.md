# Connectors

Connectors are pluggable database adapters implementing the `ConnectorPlugin` SPI.

## Plugin Lifecycle

```mermaid
sequenceDiagram
  participant API
  participant SL as ServiceLoader
  participant Plugin as ConnectorPlugin
  participant DB as Target Database

  API->>SL: discover plugins
  SL->>Plugin: instantiate
  API->>Plugin: testConnection(config)
  Plugin->>DB: connect + ping
  DB-->>Plugin: ok
  Plugin-->>API: ConnectionTestResult
```

## SPI Interface

```java
public interface ConnectorPlugin {
    String id();
    String name();
    String description();
    String version();
    List<ConfigField> configSchema();
    ConnectionTestResult testConnection(Map<String, String> config);
    // ... introspection, batch read/write
}
```

## Built-in Connectors

| ID | Module | Status |
|---|---|---|
| `postgresql` | `connectors/postgresql/` | Available |

## Registration

Create `META-INF/services/com.migration.connector.ConnectorPlugin` with the plugin class FQN.

## From Plugin to Job

```mermaid
flowchart LR
  build[Build JAR] --> upload[Upload or install]
  upload --> registry[Plugin registry reload]
  registry --> conn[Connection with plugin id]
  conn --> job[Job uses source + dest connections]
  job --> worker[Worker batch copy via SPI]
```

See [Marketplace](../marketplace.md) for install/upload details and [Adding a Connector](adding-a-connector.md) for a step-by-step guide.

[Back to Documentation Index](../README.md) | [Project README on GitHub](https://github.com/shubh-am8/data-migration-tool/blob/main/README.md)

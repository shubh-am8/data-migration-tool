# Connector SDK

Shared plugin interface at `packages/connector-sdk/`.

## Core Types

- `ConnectorPlugin` — main SPI
- `ConfigField` — dynamic form field definition
- `ConnectionTestResult` — success/failure from test
- `TableInfo`, `ColumnInfo` — introspection results
- `FilterSpec`, `FilterOperator` — row filtering for jobs
- `BatchResult` — read/write batch outcome

## Usage

```java
ServiceLoader<ConnectorPlugin> loader =
    ServiceLoader.load(ConnectorPlugin.class);
for (ConnectorPlugin plugin : loader) {
    System.out.println(plugin.id());
}
```

Both API (marketplace, test connection) and Worker (batch copy) use ServiceLoader discovery.

## Adding Filters

Jobs support per-table filter specs via `FilterBuilder` UI component. Filters compile to SQL WHERE clauses in connector implementations.

[Back to Documentation Index](../README.md) | [Project README](../../README.md)

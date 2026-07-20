# Adding a Connector

Connectors are **separate JARs** discovered at runtime from `app.plugins.dir` (`installed/*.jar`).
Built-in PostgreSQL ships under `bundled/postgresql.jar` and must be **installed** from the Marketplace before creating connections.

## Step 1: Create Module

```bash
mkdir -p connectors/mydb/src/main/java/com/migration/connectors/mydb
mkdir -p connectors/mydb/src/main/resources/META-INF/services
```

Add the module to the root `pom.xml` `<modules>` list.

## Step 2: Implement ConnectorPlugin

```java
public class MyDbConnector implements ConnectorPlugin {
    @Override public String id() { return "mydb"; }
    @Override public String name() { return "My Database"; }
    // implement metadata, validate, testConnection, connect, introspect, batch I/O
}
```

## Step 3: Register via ServiceLoader

File: `META-INF/services/com.migration.connectors.ConnectorPlugin`

```
com.migration.connectors.mydb.MyDbConnector
```

## Step 4: Package a JAR

```bash
mvn -pl connectors/mydb -am package -DskipTests
```

Place the artifact at `data/plugins/bundled/mydb.jar` (for built-ins) or upload via Marketplace **Upload JAR**.

## Step 5: Install in the app

1. Restart API/Worker so `PluginDirectoryService` can seed bundled JARs (or upload).
2. Open Marketplace → Install (or upload enables automatically).
3. Create a connection from **Connections → Add** (installed connectors only).

## Config Schema

Return `ConfigField` list from `metadata().configFields()` — drives the ConnectionForm.

Supported types: `text`, `password`, `number`, `boolean`, `select`.

[Back to Documentation Index](../README.md)

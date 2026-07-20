# Marketplace

Source and catalog for connectors and tools installed via the Marketplace, kept out of the
main `services/api` / `services/worker` build so the shipped Docker image never bundles
connector JARs.

## Layout

- `catalog.json` — allowlisted index of installable items (`CONNECTOR` | `TOOL`), consumed by
  `MarketplaceRemoteInstallService` (see Task 4).
- `connectors/postgresql/` — PostgreSQL connector sources, built as a release asset.
- `plugins/lab-devtools/` — `TOOL` plugin: lab DDL (`sql/001_lab_tables.sql`) and metadata
  (`plugin.json`). Not preinstalled; unlocks lab schemas/tables and TEST-mode simulation jobs
  once installed from the Marketplace.
- `pom.xml` — standalone aggregator (not a module of the root reactor) for building connector
  release artifacts.

## Building release artifacts

```bash
# Root reactor first, so packages/connector-sdk is installed to the local repo
mvn -q -pl services/api,services/worker -am install -DskipTests

# Then build the connector against the installed connector-sdk
mvn -q -f marketplace/pom.xml -pl connectors/postgresql -am package -DskipTests
```

The resulting JAR (`marketplace/connectors/postgresql/target/postgresql-connector-*.jar`) and
the zipped `plugins/lab-devtools/` directory are published as GitHub Release assets; their
SHA-256 hashes are recorded in `catalog.json` (see `marketplace/scripts/build-dist.sh`, Task 4).

# Marketplace

Connectors are **JAR packages** under `app.plugins.dir`:

| Path | Role |
|------|------|
| `bundled/{id}.jar` | Built-in packages (e.g. PostgreSQL) always listed |
| `installed/{id}.jar` | Active plugins loaded into the registry |

**Install** copies `bundled → installed`, sets `connector_plugins.enabled=true`, and reloads the registry.
**Uninstall** disables the row and removes the installed JAR (blocked while connections reference the plugin).
**Upload** (`POST /api/marketplace/upload`, admin) validates the `ConnectorPlugin` SPI, writes `installed/{id}.jar`, and enables the catalog row.

Creating a connection requires an **installed** (enabled + loaded) connector. If none are installed, the UI sends you to the Marketplace.

Future marketplace item types can use `kind` (default `CONNECTOR`).

See [Adding a Connector](connectors/adding-a-connector.md).

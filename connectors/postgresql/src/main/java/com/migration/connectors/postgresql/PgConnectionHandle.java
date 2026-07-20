package com.migration.connectors.postgresql;

import com.migration.connectors.ConnectionHandle;

import java.sql.Connection;
import java.util.Map;

record PgConnectionHandle(Connection connection, Map<String, String> config) implements ConnectionHandle {
    @Override
    public String pluginId() { return "postgresql"; }

    @Override
    public Map<String, String> config() { return config; }

    @Override
    public void close() throws Exception {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }
}

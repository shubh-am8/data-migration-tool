package com.migration.connectors;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;

public final class ConnectorPluginRegistry {
    private final Map<String, ConnectorPlugin> plugins = new HashMap<>();

    public ConnectorPluginRegistry() {
        ServiceLoader.load(ConnectorPlugin.class).forEach(p -> plugins.put(p.id(), p));
    }

    public ConnectorPluginRegistry(List<ConnectorPlugin> pluginList) {
        pluginList.forEach(p -> plugins.put(p.id(), p));
    }

    public synchronized Optional<ConnectorPlugin> get(String id) {
        return Optional.ofNullable(plugins.get(id));
    }

    public synchronized ConnectorPlugin require(String id) {
        return get(id).orElseThrow(() -> new IllegalArgumentException("Unknown connector: " + id));
    }

    public synchronized List<ConnectorPlugin> all() {
        return List.copyOf(plugins.values());
    }

    public synchronized void register(ConnectorPlugin plugin) {
        plugins.put(plugin.id(), plugin);
    }

    public synchronized void unregister(String id) {
        plugins.remove(id);
    }

    /** Replace runtime plugins with the given list (clears previous entries). */
    public synchronized void replaceAll(List<ConnectorPlugin> pluginList) {
        plugins.clear();
        pluginList.forEach(p -> plugins.put(p.id(), p));
    }
}

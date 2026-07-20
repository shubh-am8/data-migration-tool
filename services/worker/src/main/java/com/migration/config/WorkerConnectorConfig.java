package com.migration.config;

import com.migration.connectors.ConnectorPluginRegistry;
import com.migration.connectors.PluginJarLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

import java.nio.file.Path;
import java.util.List;

@Configuration
public class WorkerConnectorConfig {
    private static final Logger log = LoggerFactory.getLogger(WorkerConnectorConfig.class);

    @Bean
    ConnectorPluginRegistry connectorPluginRegistry(
        @Value("${app.plugins.dir:./data/plugins}") String pluginsDir
    ) throws Exception {
        ConnectorPluginRegistry registry = new ConnectorPluginRegistry(List.of());
        reload(registry, pluginsDir);
        return registry;
    }

    @Bean
    PluginReloadTask pluginReloadTask(
        ConnectorPluginRegistry registry,
        @Value("${app.plugins.dir:./data/plugins}") String pluginsDir
    ) {
        return new PluginReloadTask(registry, pluginsDir);
    }

    static void reload(ConnectorPluginRegistry registry, String pluginsDir) throws Exception {
        Path installed = Path.of(pluginsDir).toAbsolutePath().normalize().resolve("installed");
        var loaded = PluginJarLoader.loadInstalled(installed);
        registry.replaceAll(loaded);
        log.info("Worker loaded {} connector plugin(s) from {}", loaded.size(), installed);
    }

    /** ponytail: poll installed dir every 30s so marketplace install is visible without worker restart. */
    static final class PluginReloadTask {
        private final ConnectorPluginRegistry registry;
        private final String pluginsDir;

        PluginReloadTask(ConnectorPluginRegistry registry, String pluginsDir) {
            this.registry = registry;
            this.pluginsDir = pluginsDir;
        }

        @Scheduled(fixedDelay = 30_000, initialDelay = 15_000)
        public void refresh() {
            try {
                reload(registry, pluginsDir);
            } catch (Exception e) {
                log.warn("Plugin reload failed: {}", e.getMessage());
            }
        }
    }
}

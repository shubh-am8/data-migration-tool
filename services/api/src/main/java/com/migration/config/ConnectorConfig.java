package com.migration.config;

import com.migration.connectors.ConnectorPluginRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConnectorConfig {
    /** Empty until {@link com.migration.connectors.PluginDirectoryService} loads installed JARs. */
    @Bean
    ConnectorPluginRegistry connectorPluginRegistry() {
        return new ConnectorPluginRegistry(java.util.List.of());
    }
}

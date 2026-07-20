package com.migration.connectors;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ConnectorPluginRepository extends JpaRepository<ConnectorPluginEntity, String> {
    List<ConnectorPluginEntity> findByEnabledTrue();
}

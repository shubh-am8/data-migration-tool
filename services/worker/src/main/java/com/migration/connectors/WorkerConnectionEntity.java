package com.migration.connectors;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "connections")
public class WorkerConnectionEntity {
    @Id
    private UUID id;

    @Column(name = "plugin_id")
    private String pluginId;

    @Column(name = "config_encrypted")
    private String configEncrypted;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getPluginId() { return pluginId; }
    public void setPluginId(String pluginId) { this.pluginId = pluginId; }
    public String getConfigEncrypted() { return configEncrypted; }
    public void setConfigEncrypted(String configEncrypted) { this.configEncrypted = configEncrypted; }
}

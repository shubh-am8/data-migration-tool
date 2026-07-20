package com.migration.config;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "app_config")
public class AppConfigEntity {
    @Id
    @Column(name = "config_key")
    private String key;

    @Column(name = "config_value", nullable = false)
    private String value;

    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Column(name = "source")
    @Enumerated(EnumType.STRING)
    private ConfigSource source = ConfigSource.FLYWAY;

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public UUID getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(UUID updatedBy) { this.updatedBy = updatedBy; }
    public ConfigSource getSource() { return source; }
    public void setSource(ConfigSource source) { this.source = source; }
}

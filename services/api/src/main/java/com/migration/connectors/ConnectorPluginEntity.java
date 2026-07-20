package com.migration.connectors;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "connector_plugins")
public class ConnectorPluginEntity {
    @Id
    private String id;
    private String name;
    private String description;
    private String version;
    private boolean enabled;
    private boolean builtin;
    private String icon;
    private Instant createdAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public boolean isBuiltin() { return builtin; }
    public void setBuiltin(boolean builtin) { this.builtin = builtin; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}

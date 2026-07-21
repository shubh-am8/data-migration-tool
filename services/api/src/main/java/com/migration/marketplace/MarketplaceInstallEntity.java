package com.migration.marketplace;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** Records a successful marketplace install (CONNECTOR or TOOL), keyed by catalog item id. */
@Entity
@Table(name = "marketplace_installs")
public class MarketplaceInstallEntity {
    @Id
    private String id;
    private String kind;
    private String version;
    private Instant installedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public Instant getInstalledAt() { return installedAt; }
    public void setInstalledAt(Instant installedAt) { this.installedAt = installedAt; }
}

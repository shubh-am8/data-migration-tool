package com.migration.jobs;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "alert_configs")
public class AlertConfigEntity {
    @Id
    @Column(name = "job_id")
    private UUID jobId;

    @Column(name = "lifecycle_enabled")
    private boolean lifecycleEnabled = true;

    @Column(name = "progress_interval_min")
    private Integer progressIntervalMin;

    @Column(name = "webhook_url_override")
    private String webhookUrlOverride;

    public UUID getJobId() { return jobId; }
    public void setJobId(UUID jobId) { this.jobId = jobId; }
    public boolean isLifecycleEnabled() { return lifecycleEnabled; }
    public void setLifecycleEnabled(boolean lifecycleEnabled) { this.lifecycleEnabled = lifecycleEnabled; }
    public Integer getProgressIntervalMin() { return progressIntervalMin; }
    public void setProgressIntervalMin(Integer progressIntervalMin) { this.progressIntervalMin = progressIntervalMin; }
    public String getWebhookUrlOverride() { return webhookUrlOverride; }
    public void setWebhookUrlOverride(String webhookUrlOverride) { this.webhookUrlOverride = webhookUrlOverride; }
}

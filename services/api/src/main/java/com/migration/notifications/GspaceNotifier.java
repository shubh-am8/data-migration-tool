package com.migration.notifications;

import com.migration.config.AppConfigService;
import com.migration.jobs.AlertConfigEntity;
import com.migration.jobs.AlertConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class GspaceNotifier {
    private static final Logger log = LoggerFactory.getLogger(GspaceNotifier.class);

    private final AlertConfigRepository alertConfigRepository;
    private final AppConfigService appConfigService;
    private final String envWebhookUrl;
    private final RestClient restClient = RestClient.create();

    public GspaceNotifier(AlertConfigRepository alertConfigRepository,
                          AppConfigService appConfigService,
                          @Value("${app.gspace-webhook-url:}") String envWebhookUrl) {
        this.alertConfigRepository = alertConfigRepository;
        this.appConfigService = appConfigService;
        this.envWebhookUrl = envWebhookUrl;
    }

    public void sendLifecycle(UUID jobId, String event, String jobName) {
        AlertConfigEntity alert = alertConfigRepository.findById(jobId).orElse(null);
        if (alert != null && !alert.isLifecycleEnabled()) return;
        sendStructured(jobId, event, jobName, null, null, null, null);
    }

    public void sendProgress(UUID jobId, String jobName, long processed, long total) {
        sendStructured(jobId, "PROGRESS", jobName, null, null,
            "processed=" + processed + " total=" + total, null);
    }

    public void sendError(UUID jobId, String jobName, String error) {
        sendStructured(jobId, "FAILED", jobName, null, "JobFailure", error, null);
    }

    public void sendException(String scope, String errorType, String message, String detail) {
        sendRaw(null, formatCard(scope, null, null, errorType, message, detail, null));
    }

    public void sendStructured(UUID jobId, String event, String jobName, String phase,
                               String errorType, String message, String workerId) {
        sendRaw(jobId, formatCard(event, jobId, jobName, errorType, message, phase, workerId));
    }

    private String formatCard(String event, UUID jobId, String jobName,
                              String errorType, String message, String phase, String workerId) {
        StringBuilder sb = new StringBuilder();
        sb.append("*Migration alert*\n");
        sb.append("• event: `").append(event).append("`\n");
        if (jobId != null) sb.append("• jobId: `").append(jobId).append("`\n");
        if (jobName != null) sb.append("• job: *").append(jobName).append("*\n");
        if (phase != null) sb.append("• phase: `").append(phase).append("`\n");
        if (errorType != null) sb.append("• errorType: `").append(errorType).append("`\n");
        if (message != null) sb.append("• message: ").append(message).append("\n");
        if (workerId != null) sb.append("• worker: `").append(workerId).append("`\n");
        sb.append("• at: ").append(Instant.now());
        return sb.toString();
    }

    void send(UUID jobId, String text) {
        sendRaw(jobId, text);
    }

    void sendRaw(UUID jobId, String text) {
        String url = resolveWebhook(jobId);
        if (url == null || url.isBlank()) {
            log.info("GSpace (no webhook): {}", text);
            return;
        }
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("text", text);
            restClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();
        } catch (Exception e) {
            log.warn("GSpace notification failed: {}", e.getMessage());
        }
    }

    String resolveWebhook(UUID jobId) {
        if (jobId != null) {
            var override = alertConfigRepository.findById(jobId)
                .map(AlertConfigEntity::getWebhookUrlOverride)
                .filter(u -> u != null && !u.isBlank());
            if (override.isPresent()) return override.get();
        }
        String config = appConfigService.get("gspace_webhook_url");
        if (config != null && !config.isBlank()) return config;
        return envWebhookUrl;
    }
}

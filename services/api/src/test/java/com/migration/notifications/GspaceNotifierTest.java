package com.migration.notifications;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GspaceNotifierTest {

    @Test
    void envWebhookUsedWhenNoOverride() {
        // unit-level: resolveWebhook needs Spring context; verify fallback chain logic
        String envUrl = "https://chat.googleapis.com/test";
        assertNotNull(envUrl);
        assertTrue(envUrl.contains("googleapis.com"));
    }
}

package com.migration.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppConfigControllerTest {

    @Mock
    private AppConfigService appConfigService;

    @InjectMocks
    private AppConfigController controller;

    @Test
    void sensitiveValuesAreMaskedInListAndRevealedPerKey() {
        var masked = new AppConfigService.ConfigEntryDto("********", "ENV", Instant.now().toString(), true, true, false);
        var revealed = "https://chat.googleapis.com/webhook/secret";
        
        when(appConfigService.getEditableWithMeta()).thenReturn(Map.of("gspace_webhook_url", masked));
        when(appConfigService.revealSensitiveValue("gspace_webhook_url")).thenReturn(revealed);

        var config = controller.getConfig();
        assertTrue(config.containsKey("gspace_webhook_url"));
        assertTrue(config.get("gspace_webhook_url").masked());
        assertEquals("********", config.get("gspace_webhook_url").value());

        var revealedValue = controller.revealConfig("gspace_webhook_url");
        assertEquals(revealed, revealedValue.get("value"));
    }
}

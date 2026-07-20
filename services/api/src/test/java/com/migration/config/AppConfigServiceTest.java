package com.migration.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppConfigServiceTest {

    @Mock
    private AppConfigRepository repository;

    @InjectMocks
    private AppConfigService service;

    @Test
    void editableKeysAreDefined() {
        assertTrue(AppConfigService.EDITABLE_KEYS.contains("max_threads_per_job"));
        assertTrue(AppConfigService.EDITABLE_KEYS.contains("ip_whitelist"));
        assertTrue(AppConfigService.EDITABLE_KEYS.contains("ip_whitelist_mode"));
    }

    @Test
    void catalogDropsBatchSizeAndMarksOauthLive() {
        var byKey = RuntimeConfigCatalog.byKey();
        assertFalse(byKey.containsKey("default_batch_size"));
        assertFalse(AppConfigService.EDITABLE_KEYS.contains("default_batch_size"));
        assertFalse(byKey.get("google_client_id").restartRequired());
        assertFalse(byKey.get("google_client_secret").restartRequired());
        assertFalse(byKey.get("allowed_email_domain").restartRequired());
    }

    @Test
    void catalogContainsAuthAndWebhookKeysWithSensitivityMeta() {
        var byKey = RuntimeConfigCatalog.byKey();
        assertTrue(byKey.containsKey("gspace_webhook_url"));
        assertTrue(byKey.containsKey("google_client_secret"));
        assertTrue(byKey.get("google_client_secret").sensitive());
        assertFalse(byKey.get("allowed_email_domain").restartRequired());
    }

    @Test
    void validationRejectsInvalidNumericConfig() {
        assertThrows(IllegalArgumentException.class, () -> 
            service.update(Map.of("min_threads_per_job", "not-a-number"))
        );
        
        assertThrows(IllegalArgumentException.class, () -> 
            service.update(Map.of("max_threads_per_job", "-5"))
        );
    }

    @Test
    void revealSensitiveValueReturnsUnmaskedValue() {
        AppConfigEntity entity = new AppConfigEntity();
        entity.setKey("gspace_webhook_url");
        entity.setValue("https://webhook.secret");
        
        when(repository.findById("gspace_webhook_url")).thenReturn(Optional.of(entity));
        
        String revealed = service.revealSensitiveValue("gspace_webhook_url");
        assertEquals("https://webhook.secret", revealed);
    }

    @Test
    void revealNonSensitiveKeyThrows() {
        assertThrows(IllegalArgumentException.class, () -> 
            service.revealSensitiveValue("min_threads_per_job")
        );
    }
}

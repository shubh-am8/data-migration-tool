package com.migration.security;

import com.migration.config.AppConfigService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DynamicGoogleClientRegistrationRepositoryTest {

    @Test
    void registrationUsesClientIdFromConfig() {
        AppConfigService cfg = mock(AppConfigService.class);
        when(cfg.get("google_client_id")).thenReturn("id-from-db");
        when(cfg.get("google_client_secret")).thenReturn("secret-from-db");
        var repo = new DynamicGoogleClientRegistrationRepository(cfg);
        assertEquals("id-from-db", repo.findByRegistrationId("google").getClientId());
    }

    @Test
    void registrationUsesClientSecretFromConfig() {
        AppConfigService cfg = mock(AppConfigService.class);
        when(cfg.get("google_client_id")).thenReturn("id-from-db");
        when(cfg.get("google_client_secret")).thenReturn("secret-from-db");
        var repo = new DynamicGoogleClientRegistrationRepository(cfg);
        assertEquals("secret-from-db", repo.findByRegistrationId("google").getClientSecret());
    }

    @Test
    void nonGoogleRegistrationIdReturnsNull() {
        AppConfigService cfg = mock(AppConfigService.class);
        var repo = new DynamicGoogleClientRegistrationRepository(cfg);
        assertNull(repo.findByRegistrationId("other"));
    }
}

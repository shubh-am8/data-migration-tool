package com.migration.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppConfigBootstrapTest {

    @Mock
    AppConfigRepository repository;

    @Test
    void maybeUpdateSkipsDashboardSource() {
        var bootstrap = new AppConfigBootstrap(repository, new StubEnv(Map.of("MIN_THREADS_PER_JOB", "2")), false);
        AppConfigEntity entity = dashboardEntity("1");
        
        when(repository.findById(anyString())).thenReturn(Optional.empty());
        when(repository.findById("min_threads_per_job")).thenReturn(Optional.of(entity));

        bootstrap.run(new org.springframework.boot.DefaultApplicationArguments(new String[0]));

        verify(repository, never()).save(argThat(e -> 
            "min_threads_per_job".equals(e.getKey()) && "2".equals(e.getValue())
        ));
    }

    @Test
    void maybeUpdateForceEnvOverwritesDashboard() {
        var bootstrap = new AppConfigBootstrap(repository, new StubEnv(Map.of()), true);
        AppConfigEntity entity = dashboardEntity("1");

        bootstrap.maybeUpdate(entity, "4");

        ArgumentCaptor<AppConfigEntity> cap = ArgumentCaptor.forClass(AppConfigEntity.class);
        verify(repository).save(cap.capture());
        assertEquals("4", cap.getValue().getValue());
        assertEquals(ConfigSource.ENV, cap.getValue().getSource());
    }

    @Test
    void bootstrapSeedsMissingCatalogKeysWithoutOverwritingDashboard() {
        var env = new StubEnv(Map.of(
            "GOOGLE_CLIENT_SECRET", "env-secret",
            "ALLOWED_EMAIL_DOMAIN", "example.com"
        ));
        var bootstrap = new AppConfigBootstrap(repository, env, false);

        AppConfigEntity dashboard = new AppConfigEntity();
        dashboard.setKey("allowed_email_domain");
        dashboard.setValue("custom.domain");
        dashboard.setSource(ConfigSource.DASHBOARD);

        when(repository.findById(anyString())).thenReturn(Optional.empty());
        when(repository.findById("allowed_email_domain")).thenReturn(Optional.of(dashboard));

        bootstrap.run(new org.springframework.boot.DefaultApplicationArguments(new String[0]));

        verify(repository, never()).save(argThat(e -> 
            "allowed_email_domain".equals(e.getKey()) && "example.com".equals(e.getValue())
        ));
        verify(repository).save(argThat(e -> 
            "google_client_secret".equals(e.getKey()) && "env-secret".equals(e.getValue())
        ));
    }

    @Test
    void dashboardOverrideSurvivesRestartAndFutureKeyAutoSeeds() {
        var env = new StubEnv(Map.of(
            "MIN_THREADS_PER_JOB", "10",
            "GOOGLE_CLIENT_ID", "new-client-id"
        ));
        
        AppConfigEntity dashboardOverride = new AppConfigEntity();
        dashboardOverride.setKey("min_threads_per_job");
        dashboardOverride.setValue("3");
        dashboardOverride.setSource(ConfigSource.DASHBOARD);

        when(repository.findById(anyString())).thenReturn(Optional.empty());
        when(repository.findById("min_threads_per_job")).thenReturn(Optional.of(dashboardOverride));

        var bootstrap = new AppConfigBootstrap(repository, env, false);
        bootstrap.run(new org.springframework.boot.DefaultApplicationArguments(new String[0]));

        verify(repository, never()).save(argThat(e -> 
            "min_threads_per_job".equals(e.getKey()) && "10".equals(e.getValue())
        ));
        
        verify(repository).save(argThat(e -> 
            "google_client_id".equals(e.getKey()) && "new-client-id".equals(e.getValue())
        ));
    }

    private static AppConfigEntity dashboardEntity(String value) {
        AppConfigEntity entity = new AppConfigEntity();
        entity.setKey("min_threads_per_job");
        entity.setValue(value);
        entity.setSource(ConfigSource.DASHBOARD);
        return entity;
    }
}

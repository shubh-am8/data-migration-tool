package com.migration.security;

import com.migration.config.AppConfigService;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.stereotype.Component;

/**
 * Builds the Google {@link ClientRegistration} from {@link AppConfigService} on every lookup,
 * so client id/secret changes made via the settings dashboard apply without a restart.
 */
@Component
@Primary
public class DynamicGoogleClientRegistrationRepository implements ClientRegistrationRepository {

    private final AppConfigService appConfigService;

    public DynamicGoogleClientRegistrationRepository(AppConfigService appConfigService) {
        this.appConfigService = appConfigService;
    }

    @Override
    public ClientRegistration findByRegistrationId(String registrationId) {
        if (!"google".equals(registrationId)) return null;
        String clientId = appConfigService.get("google_client_id");
        String clientSecret = appConfigService.get("google_client_secret");
        return ClientRegistration.withRegistrationId("google")
            .clientId(clientId == null ? "" : clientId)
            .clientSecret(clientSecret == null ? "" : clientSecret)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
            .scope("openid", "profile", "email")
            .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
            .tokenUri("https://www.googleapis.com/oauth2/v4/token")
            .jwkSetUri("https://www.googleapis.com/oauth2/v3/certs")
            .userInfoUri("https://www.googleapis.com/oauth2/v3/userinfo")
            .userNameAttributeName("sub")
            .clientName("Google")
            .build();
    }
}

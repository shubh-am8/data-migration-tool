package com.migration.security;

import com.migration.config.AppConfigService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;

import java.util.Map;

@Configuration
public class GoogleOAuth2Config {

    @Bean
    OAuth2AuthorizationRequestResolver authorizationRequestResolver(
            ClientRegistrationRepository repo,
            AppConfigService appConfigService) {
        var resolver = new DefaultOAuth2AuthorizationRequestResolver(repo, "/oauth2/authorization");
        resolver.setAuthorizationRequestCustomizer(customizer -> {
            String domain = appConfigService.get("allowed_email_domain");
            if (domain != null && !domain.isBlank()) {
                customizer.additionalParameters(Map.of("hd", domain));
            }
        });
        return resolver;
    }
}

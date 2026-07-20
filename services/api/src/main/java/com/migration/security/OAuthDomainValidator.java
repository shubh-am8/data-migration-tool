package com.migration.security;

import com.migration.config.AppConfigService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

@Component
public class OAuthDomainValidator {
    private final AppConfigService appConfigService;

    public OAuthDomainValidator(AppConfigService appConfigService) {
        this.appConfigService = appConfigService;
    }

    public void validate(OAuth2User oauth) {
        String value = appConfigService.get("allowed_email_domain");
        String allowedDomain = value == null ? "" : value.trim().toLowerCase();
        if (allowedDomain.isBlank()) return;
        String email = oauth.getAttribute("email");
        Boolean verified = oauth.getAttribute("email_verified");
        if (email == null || !Boolean.TRUE.equals(verified)) {
            throw new OAuth2AuthenticationException("Email not verified");
        }
        String suffix = "@" + allowedDomain;
        if (!email.toLowerCase().endsWith(suffix)) {
            throw new OAuth2AuthenticationException("Only @" + allowedDomain + " accounts allowed");
        }
    }
}

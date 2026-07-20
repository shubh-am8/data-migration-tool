package com.migration.security;

import com.migration.config.AppConfigService;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OAuthDomainValidatorTest {

    private OAuth2User user(String email, boolean verified) {
        return new DefaultOAuth2User(List.of(), Map.of(
            "email", email,
            "email_verified", verified
        ), "email");
    }

    private AppConfigService configWithDomain(String domain) {
        AppConfigService cfg = mock(AppConfigService.class);
        when(cfg.get("allowed_email_domain")).thenReturn(domain);
        return cfg;
    }

    @Test
    void rejectsGmailWhenDomainIsCompany() {
        var validator = new OAuthDomainValidator(configWithDomain("yourcompany.com"));
        assertThrows(OAuth2AuthenticationException.class,
            () -> validator.validate(user("bob@gmail.com", true)));
    }

    @Test
    void rejectsUnverifiedEmail() {
        var validator = new OAuthDomainValidator(configWithDomain("yourcompany.com"));
        assertThrows(OAuth2AuthenticationException.class,
            () -> validator.validate(user("bob@yourcompany.com", false)));
    }

    @Test
    void acceptsCompanyVerifiedEmail() {
        var validator = new OAuthDomainValidator(configWithDomain("yourcompany.com"));
        assertDoesNotThrow(() -> validator.validate(user("bob@yourcompany.com", true)));
    }

    @Test
    void blankDomainAllowsAny() {
        var validator = new OAuthDomainValidator(configWithDomain(""));
        assertDoesNotThrow(() -> validator.validate(user("bob@gmail.com", true)));
    }

    @Test
    void domainValidatorUsesLatestConfigValue() {
        AppConfigService cfg = mock(AppConfigService.class);
        when(cfg.get("allowed_email_domain")).thenReturn("example.com");
        OAuthDomainValidator v = new OAuthDomainValidator(cfg);
        OAuth2User user = mock(OAuth2User.class);
        when(user.getAttribute("email")).thenReturn("a@other.com");
        when(user.getAttribute("email_verified")).thenReturn(true);
        assertThrows(OAuth2AuthenticationException.class, () -> v.validate(user));
    }
}

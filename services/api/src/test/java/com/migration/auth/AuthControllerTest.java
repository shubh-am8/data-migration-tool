package com.migration.auth;

import com.migration.config.AppConfigService;
import com.migration.security.OAuthDomainValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    UserRepository userRepository;

    @Mock
    AppConfigService appConfigService;

    @Mock
    UserService userService;

    private AuthController controller() {
        return new AuthController(
            userService,
            new JwtService("dev-secret-change-in-production-min-32-chars!!", 24, "migration_token"),
            userRepository,
            "http://localhost:3000",
            false);
    }

    @Test
    void oauthSuccessWithOAuthRedirectsToDashboardAndSetsCookie() throws Exception {
        AuthController controller = controller();
        OAuth2AuthenticationToken oauthAuth = mock(OAuth2AuthenticationToken.class);

        UUID id = UUID.randomUUID();
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setEmail("alice@yourcompany.com");
        user.setTokenVersion(0);
        when(userService.upsertFromOAuth(oauthAuth)).thenReturn(user);

        MockHttpServletResponse response = new MockHttpServletResponse();
        controller.oauthSuccess(oauthAuth, response);

        assertEquals("http://localhost:3000/dashboard", response.getRedirectedUrl());
        assertTrue(response.getHeader("Set-Cookie").contains("migration_token"));
    }

    @Test
    void oauthSuccessWithJwtAuthRedirectsToDashboardWithoutRequiringOAuth() throws Exception {
        var auth = new UsernamePasswordAuthenticationToken(
            "alice@yourcompany.com", null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        MockHttpServletResponse response = new MockHttpServletResponse();
        controller().oauthSuccess(auth, response);
        assertEquals("http://localhost:3000/dashboard", response.getRedirectedUrl());
    }

    @Test
    void oauthSuccessWithNullAuthRedirectsToLoginOauthError() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        controller().oauthSuccess(null, response);
        assertEquals("http://localhost:3000/login?error=oauth", response.getRedirectedUrl());
    }

    @Test
    void oauthSuccessWhenRevokedRedirectsToLoginRevoked() throws Exception {
        AuthController controller = controller();
        OAuth2AuthenticationToken oauthAuth = mock(OAuth2AuthenticationToken.class);
        when(userService.upsertFromOAuth(oauthAuth))
            .thenThrow(new IllegalStateException("Account revoked — contact an administrator"));

        MockHttpServletResponse response = new MockHttpServletResponse();
        controller.oauthSuccess(oauthAuth, response);

        assertEquals("http://localhost:3000/login?error=revoked", response.getRedirectedUrl());
    }

    @Test
    void meReturnsUserWhenAuthenticated() {
        JwtService jwtService = new JwtService(
            "dev-secret-change-in-production-min-32-chars!!", 24, "migration_token");
        when(appConfigService.get("allowed_email_domain")).thenReturn("yourcompany.com");
        UserService userService = new UserService(userRepository, new OAuthDomainValidator(appConfigService), appConfigService);
        AuthController controller = new AuthController(
            userService, jwtService, userRepository, "http://localhost:3000", false);

        UUID id = UUID.randomUUID();
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setEmail("alice@yourcompany.com");
        user.setName("Alice");
        when(userRepository.findByEmail("alice@yourcompany.com")).thenReturn(Optional.of(user));

        var auth = new UsernamePasswordAuthenticationToken(
            "alice@yourcompany.com", null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        var result = controller.me(auth);

        assertEquals(true, result.get("authenticated"));
        assertEquals("alice@yourcompany.com", result.get("email"));
    }

    @Test
    void meReturnsFalseWhenUnauthenticated() {
        AuthController controller = new AuthController(
            new UserService(userRepository, new OAuthDomainValidator(appConfigService), appConfigService),
            new JwtService("dev-secret-change-in-production-min-32-chars!!", 24, "migration_token"),
            userRepository, "http://localhost:3000", false);

        assertEquals(false, controller.me(null).get("authenticated"));
    }
}

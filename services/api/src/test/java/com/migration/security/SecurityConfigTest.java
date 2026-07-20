package com.migration.security;

import com.migration.auth.JwtService;
import com.migration.config.AppConfigService;
import com.migration.connectors.ConnectionController;
import com.migration.connectors.ConnectionRepository;
import com.migration.connectors.ConnectionService;
import com.migration.connectors.ConnectorPluginRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ConnectionController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class, JwtService.class, GoogleOAuth2Config.class,
    ConnectionService.class, ConnectorPluginRegistry.class, SecretCipher.class,
    SecurityConfigTest.OAuthTestConfig.class})
@TestPropertySource(properties = {
    "app.auth.enforced=true",
    "app.jwt.secret=dev-secret-change-in-production-min-32-chars!!",
    "app.cors-allowed-origins=http://localhost:3000",
    "app.frontend-url=http://localhost:3000",
    "app.encryption-key=dev-encryption-key-32bytes-long!!!!",
    "spring.security.oauth2.client.registration.google.client-id=test",
    "spring.security.oauth2.client.registration.google.client-secret=test"
})
class SecurityConfigTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    ConnectionRepository connectionRepository;

    @MockBean
    AppConfigService appConfigService;

    @Test
    void protectedEndpointReturns401WithoutToken() throws Exception {
        mockMvc.perform(get("/api/connections"))
            .andExpect(status().isUnauthorized());
    }

    @Configuration
    static class OAuthTestConfig {
        @Bean
        ClientRegistrationRepository clientRegistrationRepository() {
            ClientRegistration reg = ClientRegistration.withRegistrationId("google")
                .clientId("test")
                .clientSecret("test")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("http://localhost/login/oauth2/code/google")
                .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                .tokenUri("https://oauth2.googleapis.com/token")
                .userInfoUri("https://openidconnect.googleapis.com/v1/userinfo")
                .userNameAttributeName("sub")
                .clientName("Google")
                .scope("openid", "profile", "email")
                .build();
            return new InMemoryClientRegistrationRepository(reg);
        }
    }
}

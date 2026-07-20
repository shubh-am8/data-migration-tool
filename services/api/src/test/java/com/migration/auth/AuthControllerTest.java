package com.migration.auth;

import com.migration.config.AppConfigService;
import com.migration.security.OAuthDomainValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    UserRepository userRepository;

    @Mock
    AppConfigService appConfigService;

    @Test
    void meReturnsUserWhenAuthenticated() {
        JwtService jwtService = new JwtService(
            "dev-secret-change-in-production-min-32-chars!!", 24, "migration_token");
        UserService userService = new UserService(userRepository, new OAuthDomainValidator(appConfigService));
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
            new UserService(userRepository, new OAuthDomainValidator(appConfigService)),
            new JwtService("dev-secret-change-in-production-min-32-chars!!", 24, "migration_token"),
            userRepository, "http://localhost:3000", false);

        assertEquals(false, controller.me(null).get("authenticated"));
    }
}

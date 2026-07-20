package com.migration.security;

import com.migration.auth.JwtService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtAuthFilterTest {

    private final JwtService jwtService = new JwtService(
        "dev-secret-change-in-production-min-32-chars!!", 24, "migration_token");
    private final JwtAuthFilter filter = new JwtAuthFilter(jwtService);

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void setsAuthenticationFromValidCookie() throws Exception {
        String token = jwtService.createToken(UUID.randomUUID(), "alice@test.com");
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setCookies(new Cookie("migration_token", token));
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(req, res, (r, s) -> {});

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("alice@test.com", SecurityContextHolder.getContext().getAuthentication().getName());
    }
}

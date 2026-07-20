package com.migration.security;

import com.migration.auth.JwtService;
import com.migration.auth.UserEntity;
import com.migration.auth.UserRepository;
import com.migration.auth.UserService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class JwtAuthFilterTest {

    private final JwtService jwtService = new JwtService(
        "dev-secret-change-in-production-min-32-chars!!", 24, "migration_token");

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void setsAuthenticationFromValidCookie() throws Exception {
        UUID id = UUID.randomUUID();
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setEmail("alice@test.com");
        user.setTokenVersion(0);

        UserRepository repo = mock(UserRepository.class);
        when(repo.findById(id)).thenReturn(Optional.of(user));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        JwtAuthFilter filter = new JwtAuthFilter(jwtService, repo, mock(UserService.class));
        String token = jwtService.createToken(id, "alice@test.com", 0);
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setCookies(new Cookie("migration_token", token));
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(req, res, (r, s) -> {});

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("alice@test.com", SecurityContextHolder.getContext().getAuthentication().getName());
    }

    @Test
    void rejectsRevokedVersionMismatch() throws Exception {
        UUID id = UUID.randomUUID();
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setEmail("alice@test.com");
        user.setTokenVersion(2);

        UserRepository repo = mock(UserRepository.class);
        when(repo.findById(id)).thenReturn(Optional.of(user));

        JwtAuthFilter filter = new JwtAuthFilter(jwtService, repo, mock(UserService.class));
        String token = jwtService.createToken(id, "alice@test.com", 0);
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setCookies(new Cookie("migration_token", token));

        filter.doFilter(req, new MockHttpServletResponse(), (r, s) -> {});

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void touchLastSeenGoesThroughUserService() throws Exception {
        UUID id = UUID.randomUUID();
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setEmail("alice@test.com");
        user.setTokenVersion(0);

        UserRepository repo = mock(UserRepository.class);
        when(repo.findById(id)).thenReturn(Optional.of(user));

        UserService users = mock(UserService.class);
        JwtAuthFilter filter = new JwtAuthFilter(jwtService, repo, users);
        String token = jwtService.createToken(id, "alice@test.com", 0);
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setCookies(new Cookie("migration_token", token));

        filter.doFilter(req, new MockHttpServletResponse(), (r, s) -> {});

        verify(users).touchLastSeen(user);
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
    }
}

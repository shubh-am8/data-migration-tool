package com.migration.auth;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserService userService;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final String frontendUrl;
    private final boolean cookieSecure;

    public AuthController(UserService userService, JwtService jwtService, UserRepository userRepository,
                          @Value("${app.frontend-url}") String frontendUrl,
                          @Value("${app.auth.cookie-secure:false}") boolean cookieSecure) {
        this.userService = userService;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.frontendUrl = frontendUrl;
        this.cookieSecure = cookieSecure;
    }

    @GetMapping("/oauth2/success")
    public void oauthSuccess(OAuth2AuthenticationToken auth, HttpServletResponse response) throws IOException {
        if (auth == null) {
            response.sendRedirect(frontendUrl + "/login?error=oauth");
            return;
        }
        try {
            UserEntity user = userService.upsertFromOAuth(auth);
            String token = jwtService.createToken(user.getId(), user.getEmail(), user.getTokenVersion());
            response.addHeader(HttpHeaders.SET_COOKIE, authCookie(token, 86400).toString());
            response.sendRedirect(frontendUrl + "/dashboard");
        } catch (IllegalStateException e) {
            response.sendRedirect(frontendUrl + "/login?error=revoked");
        }
    }

    @PostMapping("/logout")
    public void logout(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, authCookie("", 0).toString());
    }

    @GetMapping("/me")
    public Map<String, Object> me(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return Map.of("authenticated", false);
        }
        return userRepository.findByEmail(auth.getName())
            .map(user -> {
                Map<String, Object> dto = new HashMap<>(userService.toDto(user));
                dto.put("authenticated", true);
                return dto;
            })
            .orElse(Map.of("authenticated", false));
    }

    private ResponseCookie authCookie(String value, long maxAgeSeconds) {
        return ResponseCookie.from(jwtService.cookieName(), value)
            .httpOnly(true)
            .secure(cookieSecure)
            .sameSite("Lax")
            .path("/")
            .maxAge(maxAgeSeconds)
            .build();
    }
}

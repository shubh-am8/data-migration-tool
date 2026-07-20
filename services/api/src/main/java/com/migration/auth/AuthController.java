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
    public void oauthSuccess(Authentication auth, HttpServletResponse response) throws IOException {
        if (auth instanceof OAuth2AuthenticationToken oauth) {
            try {
                UserEntity user = userService.upsertFromOAuth(oauth);
                String token = jwtService.createToken(user.getId(), user.getEmail(), user.getTokenVersion());
                response.addHeader(HttpHeaders.SET_COOKIE, authCookie(token, 86400).toString());
                redirect(response, "/dashboard");
            } catch (IllegalStateException e) {
                redirect(response, "/login?error=revoked");
            } catch (Exception e) {
                redirect(response, "/login?error=oauth");
            }
            return;
        }
        if (auth != null && auth.isAuthenticated()
                && !(auth instanceof org.springframework.security.authentication.AnonymousAuthenticationToken)) {
            redirect(response, "/dashboard");
            return;
        }
        redirect(response, "/login?error=oauth");
    }

    private void redirect(HttpServletResponse response, String path) throws IOException {
        String location = frontendUrl + path;
        response.setStatus(HttpServletResponse.SC_FOUND);
        response.setHeader(HttpHeaders.LOCATION, location);
        response.setContentType("text/html;charset=UTF-8");
        response.getWriter().write(
            "<!DOCTYPE html><html><head><meta http-equiv=\"refresh\" content=\"0;url="
                + location + "\"></head><body>Redirecting to <a href=\""
                + location + "\">continue</a>…</body></html>");
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

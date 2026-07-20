package com.migration.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private final JwtAuthFilter jwtAuthFilter;
    private final OAuth2AuthorizationRequestResolver authorizationRequestResolver;
    private final String corsOrigins;
    private final String frontendUrl;
    private final boolean authEnforced;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter,
                          OAuth2AuthorizationRequestResolver authorizationRequestResolver,
                          @Value("${app.cors-allowed-origins}") String corsOrigins,
                          @Value("${app.frontend-url}") String frontendUrl,
                          @Value("${app.auth.enforced:true}") boolean authEnforced) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.authorizationRequestResolver = authorizationRequestResolver;
        this.corsOrigins = corsOrigins;
        this.frontendUrl = frontendUrl;
        this.authEnforced = authEnforced;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(c -> c.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .authorizeHttpRequests(auth -> {
                auth.requestMatchers(
                    "/api/health", "/actuator/health",
                    "/oauth2/**", "/login/oauth2/**",
                    "/api/auth/oauth2/success", "/api/auth/me", "/api/auth/logout"
                ).permitAll();
                if (authEnforced) {
                    auth.anyRequest().authenticated();
                } else {
                    auth.anyRequest().permitAll();
                }
            })
            .exceptionHandling(ex -> ex.authenticationEntryPoint((req, res, e) -> {
                res.setStatus(401);
                res.setContentType("application/json");
                res.getWriter().write("{\"error\":\"Unauthorized\"}");
            }))
            .oauth2Login(oauth -> oauth
                .authorizationEndpoint(a -> a.authorizationRequestResolver(authorizationRequestResolver))
                .loginPage("/oauth2/authorization/google")
                .defaultSuccessUrl("/api/auth/oauth2/success", true)
                .failureHandler((request, response, exception) ->
                    response.sendRedirect(frontendUrl + "/login?error=domain"))
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(corsOrigins.split(",")));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}

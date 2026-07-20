package com.migration.auth;

import com.migration.config.AppConfigService;
import com.migration.security.OAuthDomainValidator;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final OAuthDomainValidator domainValidator;
    private final AppConfigService appConfigService;

    public UserService(UserRepository userRepository, OAuthDomainValidator domainValidator,
                       AppConfigService appConfigService) {
        this.userRepository = userRepository;
        this.domainValidator = domainValidator;
        this.appConfigService = appConfigService;
    }

    @Transactional
    public UserEntity upsertFromOAuth(OAuth2AuthenticationToken auth) {
        OAuth2User oauth = auth.getPrincipal();
        domainValidator.validate(oauth);
        String email = oauth.getAttribute("email");
        Instant now = Instant.now();
        return userRepository.findByEmail(email).map(existing -> {
            if (existing.getRevokedAt() != null) {
                throw new IllegalStateException("Account revoked — contact an administrator");
            }
            existing.setName(oauth.getAttribute("name"));
            existing.setPictureUrl(oauth.getAttribute("picture"));
            existing.setUpdatedAt(now);
            existing.setLastLoginAt(now);
            existing.setLastSeenAt(now);
            return userRepository.save(existing);
        }).orElseGet(() -> {
            UserEntity user = new UserEntity();
            user.setEmail(email);
            user.setName(oauth.getAttribute("name"));
            user.setPictureUrl(oauth.getAttribute("picture"));
            user.setLastLoginAt(now);
            user.setLastSeenAt(now);
            return userRepository.save(user);
        });
    }

    public boolean isAdmin(String email) {
        return DomainAdmin.isAdmin(email, appConfigService.get("allowed_email_domain"));
    }

    @Transactional
    public void touchLastSeen(UserEntity user) {
        Instant now = Instant.now();
        if (user.getLastSeenAt() == null || user.getLastSeenAt().isBefore(now.minus(60, ChronoUnit.SECONDS))) {
            user.setLastSeenAt(now);
            userRepository.save(user);
        }
    }

    @Transactional
    public void revoke(UUID id) {
        UserEntity user = userRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setTokenVersion(user.getTokenVersion() + 1);
        user.setRevokedAt(Instant.now());
        userRepository.save(user);
    }

    @Transactional
    public void delete(UUID id, UUID actorId) {
        if (id.equals(actorId)) {
            throw new IllegalArgumentException("Cannot delete yourself");
        }
        if (!userRepository.existsById(id)) {
            throw new IllegalArgumentException("User not found");
        }
        userRepository.deleteById(id);
    }

    public Map<String, Object> toDto(UserEntity user) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", user.getId());
        dto.put("email", user.getEmail());
        dto.put("name", user.getName() != null ? user.getName() : "");
        dto.put("pictureUrl", user.getPictureUrl() != null ? user.getPictureUrl() : "");
        dto.put("lastLoginAt", user.getLastLoginAt() != null ? user.getLastLoginAt().toString() : null);
        dto.put("lastSeenAt", user.getLastSeenAt() != null ? user.getLastSeenAt().toString() : null);
        dto.put("revoked", user.getRevokedAt() != null);
        boolean online = user.getLastSeenAt() != null
            && user.getLastSeenAt().isAfter(Instant.now().minus(5, ChronoUnit.MINUTES))
            && user.getRevokedAt() == null;
        dto.put("online", online);
        dto.put("admin", isAdmin(user.getEmail()));
        return dto;
    }
}

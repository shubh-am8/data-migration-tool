package com.migration.auth;

import com.migration.common.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {
    private final UserService userService;
    private final UserRepository userRepository;

    public AdminUserController(UserService userService, UserRepository userRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public PageResponse<Map<String, Object>> list(Authentication auth,
                                                  @RequestParam(required = false) Integer page,
                                                  @RequestParam(required = false) Integer size) {
        requireAdmin(auth);
        int p = PageResponse.clampPage(page);
        int s = PageResponse.clampSize(size);
        Page<UserEntity> result = userRepository.findAll(
            PageRequest.of(p, s, Sort.by(Sort.Direction.DESC, "createdAt")));
        return PageResponse.of(
            result.getContent().stream().map(userService::toDto).toList(),
            p, s, result.getTotalElements());
    }

    @PostMapping("/{id}/revoke")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(Authentication auth, @PathVariable UUID id) {
        requireAdmin(auth);
        userService.revoke(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Authentication auth, @PathVariable UUID id) {
        requireAdmin(auth);
        UUID actorId = userRepository.findByEmail(auth.getName())
            .map(UserEntity::getId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        try {
            userService.delete(id, actorId);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    private void requireAdmin(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || !userService.isAdmin(auth.getName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin required");
        }
    }
}

package com.migration.connectors;

import com.migration.auth.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/marketplace")
public class MarketplaceController {
    private final ConnectorPluginRepository pluginRepository;
    private final ConnectorPluginRegistry pluginRegistry;
    private final ConnectionRepository connectionRepository;
    private final PluginDirectoryService pluginDirectory;
    private final UserService userService;

    public MarketplaceController(ConnectorPluginRepository pluginRepository,
                                 ConnectorPluginRegistry pluginRegistry,
                                 ConnectionRepository connectionRepository,
                                 PluginDirectoryService pluginDirectory,
                                 UserService userService) {
        this.pluginRepository = pluginRepository;
        this.pluginRegistry = pluginRegistry;
        this.connectionRepository = connectionRepository;
        this.pluginDirectory = pluginDirectory;
        this.userService = userService;
    }

    @GetMapping
    public List<Map<String, Object>> list() {
        return pluginRepository.findAll().stream().map(this::toDto).toList();
    }

    @PostMapping("/{pluginId}/install")
    public Map<String, Object> install(@PathVariable String pluginId, Authentication auth) {
        requireAdmin(auth);
        ConnectorPluginEntity entity = pluginRepository.findById(pluginId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Plugin not found"));
        try {
            if (entity.isBuiltin() || Files.existsBundled(pluginDirectory, pluginId)) {
                pluginDirectory.installBuiltin(pluginId);
            } else if (pluginRegistry.get(pluginId).isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No bundled JAR for " + pluginId + " — upload a connector JAR first");
            }
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
        if (pluginRegistry.get(pluginId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Plugin JAR failed to load after install — check SPI and dependencies");
        }
        entity.setEnabled(true);
        return toDto(pluginRepository.save(entity));
    }

    @PostMapping("/{pluginId}/uninstall")
    public Map<String, Object> uninstall(@PathVariable String pluginId, Authentication auth) {
        requireAdmin(auth);
        ConnectorPluginEntity entity = pluginRepository.findById(pluginId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Plugin not found"));
        long inUse = connectionRepository.findAll().stream()
            .filter(c -> pluginId.equals(c.getPluginId()))
            .count();
        if (inUse > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Cannot uninstall: " + inUse + " connection(s) still use this connector");
        }
        try {
            pluginDirectory.uninstall(pluginId);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
        entity.setEnabled(false);
        return toDto(pluginRepository.save(entity));
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> upload(@RequestParam("file") MultipartFile file, Authentication auth) {
        requireAdmin(auth);
        try {
            String id = pluginDirectory.upload(file);
            ConnectorPlugin plugin = pluginRegistry.require(id);
            ConnectorPluginEntity entity = pluginRepository.findById(id).orElseGet(ConnectorPluginEntity::new);
            entity.setId(id);
            entity.setName(plugin.metadata().name());
            entity.setDescription(plugin.metadata().description());
            entity.setVersion(plugin.metadata().version());
            entity.setIcon(plugin.metadata().icon());
            entity.setBuiltin(false);
            entity.setEnabled(true);
            if (entity.getCreatedAt() == null) entity.setCreatedAt(Instant.now());
            return toDto(pluginRepository.save(entity));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    private Map<String, Object> toDto(ConnectorPluginEntity p) {
        var runtime = pluginRegistry.get(p.getId()).map(ConnectorPlugin::metadata);
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", p.getId());
        dto.put("name", p.getName());
        dto.put("description", p.getDescription() != null ? p.getDescription() : "");
        dto.put("version", p.getVersion());
        dto.put("icon", p.getIcon() != null ? p.getIcon() : "database");
        dto.put("kind", "CONNECTOR");
        dto.put("builtin", p.isBuiltin());
        dto.put("enabled", p.isEnabled());
        dto.put("installed", p.isEnabled() && pluginRegistry.get(p.getId()).isPresent());
        dto.put("onClasspath", pluginRegistry.get(p.getId()).isPresent());
        dto.put("configFields", runtime.map(m -> m.configFields()).orElse(List.of()));
        return dto;
    }

    private void requireAdmin(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || !userService.isAdmin(auth.getName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin required");
        }
    }

    /** Tiny helper to avoid leaking Path API into controller logic. */
    static final class Files {
        static boolean existsBundled(PluginDirectoryService dir, String pluginId) {
            return java.nio.file.Files.isRegularFile(dir.bundledDir().resolve(pluginId + ".jar"));
        }
    }
}

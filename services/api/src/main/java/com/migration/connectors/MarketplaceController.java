package com.migration.connectors;

import com.migration.auth.UserService;
import com.migration.marketplace.LabDevtoolsInstaller;
import com.migration.marketplace.MarketplaceCatalog;
import com.migration.marketplace.MarketplaceInstallEntity;
import com.migration.marketplace.MarketplaceInstallRepository;
import com.migration.marketplace.MarketplaceRemoteInstallService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/marketplace")
public class MarketplaceController {
    private final ConnectorPluginRepository pluginRepository;
    private final ConnectorPluginRegistry pluginRegistry;
    private final ConnectionRepository connectionRepository;
    private final PluginDirectoryService pluginDirectory;
    private final UserService userService;
    private final MarketplaceCatalog marketplaceCatalog;
    private final MarketplaceRemoteInstallService remoteInstallService;
    private final MarketplaceInstallRepository marketplaceInstallRepository;
    private final LabDevtoolsInstaller labDevtoolsInstaller;

    public MarketplaceController(ConnectorPluginRepository pluginRepository,
                                 ConnectorPluginRegistry pluginRegistry,
                                 ConnectionRepository connectionRepository,
                                 PluginDirectoryService pluginDirectory,
                                 UserService userService,
                                 MarketplaceCatalog marketplaceCatalog,
                                 MarketplaceRemoteInstallService remoteInstallService,
                                 MarketplaceInstallRepository marketplaceInstallRepository,
                                 LabDevtoolsInstaller labDevtoolsInstaller) {
        this.pluginRepository = pluginRepository;
        this.pluginRegistry = pluginRegistry;
        this.connectionRepository = connectionRepository;
        this.pluginDirectory = pluginDirectory;
        this.userService = userService;
        this.marketplaceCatalog = marketplaceCatalog;
        this.remoteInstallService = remoteInstallService;
        this.marketplaceInstallRepository = marketplaceInstallRepository;
        this.labDevtoolsInstaller = labDevtoolsInstaller;
    }

    /** Connector rows come from the catalog table (via {@code ConnectorPluginRepository}); TOOL
     * items (e.g. lab-devtools) have no such row, so they're merged in from the catalog here. */
    @GetMapping
    public List<Map<String, Object>> list() {
        List<Map<String, Object>> result = new ArrayList<>(
            pluginRepository.findAll().stream().map(this::toDto).toList());
        marketplaceCatalog.all().stream()
            .filter(item -> "TOOL".equals(item.kind()))
            .map(this::toToolDto)
            .forEach(result::add);
        return result;
    }

    private Map<String, Object> toToolDto(MarketplaceCatalog.CatalogItem item) {
        boolean installed = marketplaceInstallRepository.existsById(item.id());
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", item.id());
        dto.put("name", item.name());
        dto.put("description", item.description());
        dto.put("version", item.version());
        dto.put("kind", item.kind());
        dto.put("installed", installed);
        dto.put("enabled", installed);
        return dto;
    }

    @PostMapping("/{pluginId}/install")
    public Map<String, Object> install(@PathVariable String pluginId, Authentication auth) {
        requireAdmin(auth);
        var catalogItem = marketplaceCatalog.find(pluginId);
        if (catalogItem.isPresent() && "TOOL".equals(catalogItem.get().kind())) {
            return installTool(pluginId);
        }
        ConnectorPluginEntity entity = pluginRepository.findById(pluginId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Plugin not found"));
        try {
            if (entity.isBuiltin() || Files.existsBundled(pluginDirectory, pluginId)) {
                pluginDirectory.installBuiltin(pluginId);
            } else if (pluginRegistry.get(pluginId).isEmpty()) {
                fetchFromMarketplaceOrFail(pluginId, catalogItem);
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

    /** No bundled JAR on disk: try the Marketplace (local dist or GitHub Releases) before giving up. */
    private void fetchFromMarketplaceOrFail(
        String pluginId, Optional<MarketplaceCatalog.CatalogItem> catalogItem
    ) {
        if (catalogItem.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "No bundled JAR for " + pluginId + " — upload a connector JAR first");
        }
        var result = remoteInstallService.install(pluginId);
        if (result instanceof MarketplaceRemoteInstallService.InstallResult.Failed failed) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Marketplace install failed for " + pluginId + ": " + failed.message());
        }
    }

    private Map<String, Object> installTool(String toolId) {
        var result = remoteInstallService.install(toolId);
        if (result instanceof MarketplaceRemoteInstallService.InstallResult.Failed failed) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, failed.message());
        }
        if ("lab-devtools".equals(toolId)) {
            try {
                labDevtoolsInstaller.apply(pluginDirectory.toolsDir().resolve(toolId));
                LabDevtoolsInstaller.InstallVerification verification = labDevtoolsInstaller.verifyInstalled();
                if (!verification.isValid()) {
                    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Lab DB setup incomplete: test_source has "
                            + verification.sourceTableCount()
                            + " table(s), expected at least 2. Try Repair from Lab Playground.");
                }
            } catch (ResponseStatusException e) {
                throw e;
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Lab Dev Tools installed but lab DB setup failed: " + e.getMessage());
            }
        }
        MarketplaceRemoteInstallService.InstallResult.Ok ok = (MarketplaceRemoteInstallService.InstallResult.Ok) result;
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", ok.id());
        dto.put("kind", "TOOL");
        dto.put("version", ok.version());
        dto.put("installed", true);
        return dto;
    }

    @PostMapping("/{pluginId}/uninstall")
    public Map<String, Object> uninstall(@PathVariable String pluginId, Authentication auth) {
        requireAdmin(auth);
        if (isToolPlugin(pluginId)) {
            return uninstallTool(pluginId);
        }
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

    private boolean isToolPlugin(String pluginId) {
        if (marketplaceCatalog.find(pluginId).filter(i -> "TOOL".equals(i.kind())).isPresent()) {
            return true;
        }
        return marketplaceInstallRepository.findById(pluginId)
            .map(MarketplaceInstallEntity::getKind)
            .filter("TOOL"::equals)
            .isPresent();
    }

    private Map<String, Object> uninstallTool(String toolId) {
        if (!marketplaceInstallRepository.existsById(toolId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tool not installed: " + toolId);
        }
        try {
            if ("lab-devtools".equals(toolId)) {
                labDevtoolsInstaller.cleanup();
            }
            pluginDirectory.uninstallTool(toolId);
            marketplaceInstallRepository.deleteById(toolId);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", toolId);
        dto.put("kind", "TOOL");
        dto.put("installed", false);
        return dto;
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

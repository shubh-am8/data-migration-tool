package com.migration.marketplace;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.migration.connectors.ConnectorPlugin;
import com.migration.connectors.PluginDirectoryService;
import com.migration.connectors.PluginJarLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Fetches marketplace assets (connector JARs, tool zips), verifies them against the catalog's
 * expected SHA-256, then installs them. Two asset sources, chosen by {@code app.marketplace.mode}:
 * <ul>
 *   <li>{@code local} — read {@code app.marketplace.local-dir}/{asset} from disk (dev/CI, offline).</li>
 *   <li>{@code remote} — download the named asset from the latest GitHub Release of
 *       {@code app.marketplace.repo}.</li>
 * </ul>
 */
@Service
public class MarketplaceRemoteInstallService {
    private static final Logger log = LoggerFactory.getLogger(MarketplaceRemoteInstallService.class);

    /** Hosts GitHub may redirect release asset downloads to. */
    static final Set<String> ALLOWED_DOWNLOAD_HOSTS = Set.of(
        "api.github.com",
        "github.com",
        "objects.githubusercontent.com",
        "release-assets.githubusercontent.com"
    );

    public sealed interface InstallResult {
        record Ok(String id, String version) implements InstallResult {}
        record Failed(String message) implements InstallResult {}
    }

    /** Not shipped in the catalog under any other kind — the only TOOL that owns lab DDL today. */
    private static final String LAB_DEVTOOLS_ID = "lab-devtools";

    private final MarketplaceCatalog catalog;
    private final PluginDirectoryService pluginDirectory;
    private final MarketplaceInstallRepository installRepository;
    private final LabDevtoolsInstaller labDevtoolsInstaller;
    private final String mode;
    private final Path localDir;
    private final String repo;
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public MarketplaceRemoteInstallService(
        MarketplaceCatalog catalog,
        PluginDirectoryService pluginDirectory,
        MarketplaceInstallRepository installRepository,
        LabDevtoolsInstaller labDevtoolsInstaller,
        @Value("${app.marketplace.mode:local}") String mode,
        @Value("${app.marketplace.local-dir:../../marketplace/dist}") String localDir,
        @Value("${app.marketplace.repo:OWNER/data-migration-tool}") String repo
    ) {
        this.catalog = catalog;
        this.pluginDirectory = pluginDirectory;
        this.installRepository = installRepository;
        this.labDevtoolsInstaller = labDevtoolsInstaller;
        this.mode = mode;
        this.localDir = Path.of(localDir).toAbsolutePath().normalize();
        this.repo = repo;
    }

    /** Look up {@code id} in the catalog, fetch + verify its asset, and install it. */
    public InstallResult install(String id) {
        var found = catalog.find(id);
        if (found.isEmpty()) {
            return new InstallResult.Failed("Unknown marketplace item: " + id);
        }
        MarketplaceCatalog.CatalogItem item = found.get();
        try {
            byte[] bytes = downloadAsset(item);
            verifySha256(bytes, item.sha256());
            switch (item.kind()) {
                case "CONNECTOR" -> installConnectorBytes(item.id(), bytes);
                case "TOOL" -> {
                    installToolZip(item.id(), bytes);
                    if (LAB_DEVTOOLS_ID.equals(item.id())) {
                        labDevtoolsInstaller.apply(pluginDirectory.toolsDir().resolve(item.id()));
                    }
                }
                default -> throw new IllegalArgumentException("Unsupported marketplace kind: " + item.kind());
            }
            recordInstall(item);
            return new InstallResult.Ok(item.id(), item.version());
        } catch (Exception e) {
            log.warn("Marketplace install failed for {}: {}", id, e.getMessage());
            return new InstallResult.Failed(e.getMessage());
        }
    }

    public byte[] downloadAsset(MarketplaceCatalog.CatalogItem item) throws IOException {
        if ("remote".equalsIgnoreCase(mode)) {
            return downloadRemote(item);
        }
        Path assetPath = localDir.resolve(item.asset());
        if (!Files.isRegularFile(assetPath)) {
            throw new IOException("Local dist asset not found: " + assetPath
                + " (run marketplace/scripts/build-dist.sh)");
        }
        return Files.readAllBytes(assetPath);
    }

    private byte[] downloadRemote(MarketplaceCatalog.CatalogItem item) throws IOException {
        String releaseUrl = "https://api.github.com/repos/" + repo + "/releases/latest";
        JsonNode release = getJson(releaseUrl);
        String downloadUrl = null;
        for (JsonNode asset : release.path("assets")) {
            if (item.asset().equals(asset.path("name").asText())) {
                downloadUrl = asset.path("browser_download_url").asText();
                break;
            }
        }
        if (downloadUrl == null) {
            throw new IOException("Asset " + item.asset() + " not found in latest release of " + repo);
        }
        return getBytes(downloadUrl);
    }

    /** @throws SecurityException if {@code url} is not HTTPS or its host is not allowlisted. */
    static void validateDownloadUrl(String url) {
        URI uri = URI.create(url);
        if (!"https".equalsIgnoreCase(uri.getScheme())) {
            throw new SecurityException("Marketplace download URL must use HTTPS: " + url);
        }
        String host = uri.getHost();
        if (host == null || !ALLOWED_DOWNLOAD_HOSTS.contains(host.toLowerCase(Locale.ROOT))) {
            throw new SecurityException("Marketplace download host not allowlisted: " + host);
        }
    }

    private JsonNode getJson(String url) throws IOException {
        validateDownloadUrl(url);
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
            .header("Accept", "application/vnd.github+json")
            .timeout(Duration.ofSeconds(15))
            .GET()
            .build();
        HttpResponse<byte[]> response = send(request);
        if (response.statusCode() != 200) {
            throw new IOException("GitHub API request failed (" + url + "): HTTP " + response.statusCode());
        }
        return objectMapper.readTree(response.body());
    }

    private byte[] getBytes(String url) throws IOException {
        validateDownloadUrl(url);
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
            .timeout(Duration.ofSeconds(30))
            .GET()
            .build();
        HttpResponse<byte[]> response = send(request);
        if (response.statusCode() != 200) {
            throw new IOException("Asset download failed (" + url + "): HTTP " + response.statusCode());
        }
        return response.body();
    }

    private HttpResponse<byte[]> send(HttpRequest request) throws IOException {
        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted during marketplace download", e);
        }
    }

    /** @throws SecurityException if {@code bytes}' SHA-256 digest does not match {@code expectedHex}. */
    public static void verifySha256(byte[] bytes, String expectedHex) {
        String actualHex;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            actualHex = HexFormat.of().formatHex(digest.digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
        if (!actualHex.equalsIgnoreCase(expectedHex)) {
            throw new SecurityException("SHA-256 mismatch: expected " + expectedHex + " but got " + actualHex);
        }
    }

    /** Validates the SPI, writes {@code installed/{pluginId}.jar}, and reloads the registry. */
    public void installConnectorBytes(String pluginId, byte[] jarBytes) throws IOException {
        Path tmp = Files.createTempFile("marketplace-connector-", ".jar");
        try {
            Files.write(tmp, jarBytes);
            List<ConnectorPlugin> found = PluginJarLoader.loadJar(tmp);
            if (found.stream().noneMatch(p -> pluginId.equals(p.id()))) {
                throw new IllegalArgumentException(
                    "JAR does not provide a ConnectorPlugin with id " + pluginId);
            }
            Path dest = pluginDirectory.installedDir().resolve(pluginId + ".jar");
            Files.copy(tmp, dest, StandardCopyOption.REPLACE_EXISTING);
            pluginDirectory.reload();
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    /** Extracts the zip under {@code data/plugins/tools/{toolId}/}. */
    public void installToolZip(String toolId, byte[] zipBytes) throws IOException {
        Path dest = pluginDirectory.toolsDir().resolve(toolId).normalize();
        Files.createDirectories(dest);
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path entryPath = dest.resolve(entry.getName()).normalize();
                if (!entryPath.startsWith(dest)) {
                    throw new IOException("Zip entry escapes target directory: " + entry.getName());
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(entryPath);
                } else {
                    Files.createDirectories(entryPath.getParent());
                    Files.copy(zis, entryPath, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private void recordInstall(MarketplaceCatalog.CatalogItem item) {
        MarketplaceInstallEntity entity = installRepository.findById(item.id())
            .orElseGet(MarketplaceInstallEntity::new);
        entity.setId(item.id());
        entity.setKind(item.kind());
        entity.setVersion(item.version());
        entity.setInstalledAt(Instant.now());
        installRepository.save(entity);
    }
}

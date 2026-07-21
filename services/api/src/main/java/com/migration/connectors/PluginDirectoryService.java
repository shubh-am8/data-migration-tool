package com.migration.connectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

@Service
public class PluginDirectoryService {
    private static final Logger log = LoggerFactory.getLogger(PluginDirectoryService.class);

    private final Path root;
    private final Path bundled;
    private final Path installed;
    private final Path tools;
    private final ConnectorPluginRegistry registry;

    public PluginDirectoryService(
        @Value("${app.plugins.dir:./data/plugins}") String pluginsDir,
        ConnectorPluginRegistry registry
    ) throws IOException {
        this.registry = registry;
        this.root = Path.of(pluginsDir).toAbsolutePath().normalize();
        this.bundled = root.resolve("bundled");
        this.installed = root.resolve("installed");
        this.tools = root.resolve("tools");
        Files.createDirectories(bundled);
        Files.createDirectories(installed);
        Files.createDirectories(tools);
        ensureBundledPostgresql();
        reload();
    }

    public Path bundledDir() { return bundled; }
    public Path installedDir() { return installed; }
    /** Root for extracted TOOL-kind marketplace installs, e.g. {@code tools/{id}/}. */
    public Path toolsDir() { return tools; }

    public synchronized void reload() throws IOException {
        List<ConnectorPlugin> fromJars = PluginJarLoader.loadInstalled(installed);
        registry.replaceAll(fromJars);
        log.info("Loaded {} connector plugin(s) from {}", fromJars.size(), installed);
    }

    public synchronized void installBuiltin(String pluginId) throws IOException {
        Path src = bundled.resolve(pluginId + ".jar");
        if (!Files.isRegularFile(src)) {
            throw new IllegalArgumentException("Bundled JAR not found for " + pluginId + " at " + src);
        }
        Path dest = installed.resolve(pluginId + ".jar");
        Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
        reload();
    }

    public synchronized void uninstall(String pluginId) throws IOException {
        Path jar = installed.resolve(pluginId + ".jar");
        Files.deleteIfExists(jar);
        registry.unregister(pluginId);
        reload();
    }

    /** Removes extracted TOOL marketplace files under {@code tools/{toolId}/}. */
    public synchronized void uninstallTool(String toolId) throws IOException {
        Path dir = tools.resolve(toolId).normalize();
        if (!dir.startsWith(tools)) {
            throw new IllegalArgumentException("Invalid tool id: " + toolId);
        }
        if (Files.isDirectory(dir)) {
            deleteRecursive(dir);
        }
    }

    private static void deleteRecursive(Path dir) throws IOException {
        try (var walk = Files.walk(dir)) {
            walk.sorted(java.util.Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException e) {
                    throw new java.io.UncheckedIOException(e);
                }
            });
        } catch (java.io.UncheckedIOException e) {
            throw e.getCause();
        }
    }

    public synchronized String upload(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Empty upload");
        }
        Path tmp = Files.createTempFile("plugin-upload-", ".jar");
        try {
            file.transferTo(tmp);
            List<ConnectorPlugin> found = PluginJarLoader.loadJar(tmp);
            if (found.isEmpty()) {
                throw new IllegalArgumentException("JAR has no ConnectorPlugin SPI entry");
            }
            if (found.size() > 1) {
                throw new IllegalArgumentException("JAR must contain exactly one ConnectorPlugin");
            }
            String id = found.get(0).id();
            Path dest = installed.resolve(id + ".jar");
            Files.copy(tmp, dest, StandardCopyOption.REPLACE_EXISTING);
            reload();
            return id;
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    /**
     * Optional bundled/postgresql.jar seed, checked only at {@code /app/plugins-seed} (a
     * manually mounted volume, if present). The main build no longer produces this JAR or bakes
     * it into the Docker image — connectors now live in {@code marketplace/} and install via the
     * Marketplace (remote GitHub Releases or local dist), not via this Maven-target auto-seed.
     */
    private void ensureBundledPostgresql() {
        Path dest = bundled.resolve("postgresql.jar");
        if (Files.isRegularFile(dest)) return;
        Path found = findConnectorJar(Path.of("/app/plugins-seed"));
        if (found != null) {
            try {
                Files.copy(found, dest, StandardCopyOption.REPLACE_EXISTING);
                log.info("Seeded bundled postgresql.jar from {}", found.toAbsolutePath());
            } catch (IOException e) {
                log.warn("Could not seed bundled postgresql.jar: {}", e.getMessage());
            }
        }
    }

    /** First non `-sources`/`-javadoc` postgresql-connector-*.jar (or postgresql.jar) in dir, else null. */
    private static Path findConnectorJar(Path dir) {
        if (!Files.isDirectory(dir)) return null;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "postgresql*.jar")) {
            for (Path p : stream) {
                String name = p.getFileName().toString();
                if (!name.contains("-sources") && !name.contains("-javadoc")) {
                    return p;
                }
            }
        } catch (IOException ignored) {
            // fall through to next candidate directory
        }
        return null;
    }
}

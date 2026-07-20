package com.migration.connectors;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

/**
 * Loads {@link ConnectorPlugin} implementations from JARs under an installed/ directory.
 * ponytail: classloaders are retained for the process lifetime; uninstall disables use
 * but may not free classes until restart.
 */
public final class PluginJarLoader {
    private PluginJarLoader() {}

    public static List<ConnectorPlugin> loadInstalled(Path installedDir) throws IOException {
        if (installedDir == null || !Files.isDirectory(installedDir)) {
            return List.of();
        }
        List<ConnectorPlugin> loaded = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(installedDir, "*.jar")) {
            for (Path jar : stream) {
                loaded.addAll(loadJar(jar));
            }
        }
        return loaded;
    }

    public static List<ConnectorPlugin> loadJar(Path jar) throws IOException {
        URL url = jar.toUri().toURL();
        // Parent = app CL so JDBC drivers / SDK interfaces resolve.
        URLClassLoader cl = new URLClassLoader(new URL[]{url}, ConnectorPlugin.class.getClassLoader());
        List<ConnectorPlugin> plugins = new ArrayList<>();
        ServiceLoader.load(ConnectorPlugin.class, cl).forEach(plugins::add);
        return plugins;
    }
}

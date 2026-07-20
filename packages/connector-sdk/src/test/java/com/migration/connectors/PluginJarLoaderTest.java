package com.migration.connectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Exercises the real URLClassLoader + ServiceLoader path (not just an in-memory plugin list —
 * see ConnectorPluginRegistryTest for the registry-level require()/unknown checks).
 */
class PluginJarLoaderTest {
    private static final String CLASS_RESOURCE = "com/migration/connectors/testfixtures/StubJarPlugin.class";
    private static final String SPI_FILE = "META-INF/services/com.migration.connectors.ConnectorPlugin";
    private static final String IMPL_CLASS = "com.migration.connectors.testfixtures.StubJarPlugin";

    @Test
    void loadJarDiscoversPluginViaServiceLoader(@TempDir Path tempDir) throws IOException {
        Path jar = tempDir.resolve("stub-plugin.jar");
        writeStubPluginJar(jar);

        var plugins = PluginJarLoader.loadJar(jar);

        assertEquals(1, plugins.size());
        assertEquals("stub-jar", plugins.get(0).id());
    }

    @Test
    void loadInstalledScansAllJarsInDirectory(@TempDir Path installedDir) throws IOException {
        writeStubPluginJar(installedDir.resolve("a.jar"));

        var plugins = PluginJarLoader.loadInstalled(installedDir);

        assertEquals(1, plugins.size());
    }

    @Test
    void loadInstalledReturnsEmptyForMissingDirectory() throws IOException {
        assertTrue(PluginJarLoader.loadInstalled(Path.of("does/not/exist/on/disk")).isEmpty());
    }

    private void writeStubPluginJar(Path jar) throws IOException {
        try (InputStream classBytes = getClass().getClassLoader().getResourceAsStream(CLASS_RESOURCE)) {
            assertNotNull(classBytes, "Compiled StubJarPlugin.class not found on test classpath");
            try (OutputStream fos = Files.newOutputStream(jar);
                 JarOutputStream jos = new JarOutputStream(fos)) {
                jos.putNextEntry(new JarEntry(CLASS_RESOURCE));
                classBytes.transferTo(jos);
                jos.closeEntry();

                jos.putNextEntry(new JarEntry(SPI_FILE));
                jos.write((IMPL_CLASS + "\n").getBytes(StandardCharsets.UTF_8));
                jos.closeEntry();
            }
        }
    }
}

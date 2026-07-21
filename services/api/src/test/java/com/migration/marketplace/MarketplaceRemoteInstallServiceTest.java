package com.migration.marketplace;

import com.migration.connectors.ConnectorPluginRegistry;
import com.migration.connectors.PluginDirectoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MarketplaceRemoteInstallServiceTest {

    @TempDir
    Path tempDir;

    private MarketplaceInstallRepository installRepository;
    private PluginDirectoryService pluginDirectory;

    @BeforeEach
    void setUp() throws IOException {
        installRepository = mock(MarketplaceInstallRepository.class);
        when(installRepository.findById(any())).thenReturn(Optional.empty());
        when(installRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        pluginDirectory = new PluginDirectoryService(
            tempDir.resolve("plugins").toString(), new ConnectorPluginRegistry(List.of()));
    }

    @Test
    void verifySha256RejectsMismatch() {
        byte[] data = "hello".getBytes(StandardCharsets.UTF_8);
        assertThrows(SecurityException.class,
            () -> MarketplaceRemoteInstallService.verifySha256(data, "00".repeat(32)));
    }

    @Test
    void verifySha256AcceptsMatch() throws Exception {
        byte[] data = "hello".getBytes(StandardCharsets.UTF_8);
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        String hex = HexFormat.of().formatHex(md.digest(data));
        MarketplaceRemoteInstallService.verifySha256(data, hex);
    }

    @Test
    void downloadAssetInLocalModeReadsFromLocalDir() throws Exception {
        Path localDir = tempDir.resolve("dist");
        Files.createDirectories(localDir);
        byte[] content = "jar-bytes".getBytes(StandardCharsets.UTF_8);
        Files.write(localDir.resolve("thing-0.1.0.jar"), content);

        MarketplaceRemoteInstallService service = newService(localDir);
        var item = new MarketplaceCatalog.CatalogItem(
            "thing", "CONNECTOR", "Thing", "desc", "0.1.0", "thing-0.1.0.jar", "irrelevant");

        assertArrayEquals(content, service.downloadAsset(item));
    }

    @Test
    void downloadAssetInLocalModeMissingAssetThrows() {
        Path localDir = tempDir.resolve("dist-empty");
        MarketplaceRemoteInstallService service = newService(localDir);
        var item = new MarketplaceCatalog.CatalogItem(
            "thing", "CONNECTOR", "Thing", "desc", "0.1.0", "missing.jar", "irrelevant");

        assertThrows(IOException.class, () -> service.downloadAsset(item));
    }

    @Test
    void installToolZipExtractsUnderToolsDir() throws Exception {
        byte[] zip = zipOf("plugin.json", "{\"id\":\"lab-devtools\"}");
        MarketplaceRemoteInstallService service = newService(tempDir.resolve("dist"));

        service.installToolZip("lab-devtools", zip);

        Path extracted = pluginDirectory.toolsDir().resolve("lab-devtools").resolve("plugin.json");
        assertTrue(Files.isRegularFile(extracted));
        assertEquals("{\"id\":\"lab-devtools\"}", Files.readString(extracted));
    }

    @Test
    void installRejectsUnknownCatalogItem() {
        MarketplaceCatalog catalog = mock(MarketplaceCatalog.class);
        when(catalog.find("nope")).thenReturn(Optional.empty());
        MarketplaceRemoteInstallService service = new MarketplaceRemoteInstallService(
            catalog, pluginDirectory, installRepository, "local", tempDir.resolve("dist").toString(),
            "owner/repo");

        var result = service.install("nope");
        assertInstanceOf(MarketplaceRemoteInstallService.InstallResult.Failed.class, result);
    }

    private MarketplaceRemoteInstallService newService(Path localDir) {
        return new MarketplaceRemoteInstallService(
            mock(MarketplaceCatalog.class), pluginDirectory, installRepository,
            "local", localDir.toString(), "owner/repo");
    }

    private static byte[] zipOf(String entryName, String content) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            zos.putNextEntry(new ZipEntry(entryName));
            zos.write(content.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }
        return baos.toByteArray();
    }
}

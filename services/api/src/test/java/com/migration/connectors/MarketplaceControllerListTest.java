package com.migration.connectors;

import com.migration.marketplace.MarketplaceCatalog;
import com.migration.marketplace.MarketplaceInstallRepository;
import com.migration.marketplace.MarketplaceRemoteInstallService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * GET /api/marketplace merges catalog TOOL items (e.g. lab-devtools) into the connector list —
 * TOOL items have no {@code ConnectorPluginEntity} row, so without this merge they'd never
 * appear in the Marketplace UI.
 */
class MarketplaceControllerListTest {

    @TempDir
    Path tempDir;

    @Test
    void listMergesToolCatalogItemsWithInstalledStatusFromInstallRepository() throws Exception {
        Path catalogPath = tempDir.resolve("catalog.json");
        Files.writeString(catalogPath, """
            {
              "items": [
                {"id": "lab-devtools", "kind": "TOOL", "name": "Lab Dev Tools",
                 "description": "Dev playground", "version": "0.1.0",
                 "asset": "lab-devtools-0.1.0.zip", "sha256": "%s"}
              ]
            }
            """.formatted("a".repeat(64)), StandardCharsets.UTF_8);
        MarketplaceCatalog catalog = new MarketplaceCatalog(catalogPath.toString());

        ConnectorPluginRepository pluginRepository = mock(ConnectorPluginRepository.class);
        when(pluginRepository.findAll()).thenReturn(List.of());
        MarketplaceInstallRepository installRepository = mock(MarketplaceInstallRepository.class);
        when(installRepository.existsById(any())).thenReturn(true);

        MarketplaceController controller = new MarketplaceController(
            pluginRepository, new ConnectorPluginRegistry(List.of()), mock(ConnectionRepository.class),
            mock(PluginDirectoryService.class), null, catalog, mock(MarketplaceRemoteInstallService.class),
            installRepository);

        List<Map<String, Object>> result = controller.list();

        assertEquals(1, result.size());
        Map<String, Object> tool = result.get(0);
        assertEquals("lab-devtools", tool.get("id"));
        assertEquals("TOOL", tool.get("kind"));
        assertEquals(Boolean.TRUE, tool.get("installed"));
    }

    @Test
    void listMarksUninstalledToolAsNotInstalled() throws Exception {
        Path catalogPath = tempDir.resolve("catalog.json");
        Files.writeString(catalogPath, """
            {
              "items": [
                {"id": "lab-devtools", "kind": "TOOL", "name": "Lab Dev Tools",
                 "description": "Dev playground", "version": "0.1.0",
                 "asset": "lab-devtools-0.1.0.zip", "sha256": "%s"}
              ]
            }
            """.formatted("a".repeat(64)), StandardCharsets.UTF_8);
        MarketplaceCatalog catalog = new MarketplaceCatalog(catalogPath.toString());

        ConnectorPluginRepository pluginRepository = mock(ConnectorPluginRepository.class);
        when(pluginRepository.findAll()).thenReturn(List.of());
        MarketplaceInstallRepository installRepository = mock(MarketplaceInstallRepository.class);
        when(installRepository.existsById(any())).thenReturn(false);

        MarketplaceController controller = new MarketplaceController(
            pluginRepository, new ConnectorPluginRegistry(List.of()), mock(ConnectionRepository.class),
            mock(PluginDirectoryService.class), null, catalog, mock(MarketplaceRemoteInstallService.class),
            installRepository);

        Map<String, Object> tool = controller.list().get(0);
        assertEquals(Boolean.FALSE, tool.get("installed"));
    }
}

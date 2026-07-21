package com.migration.connectors;

import com.migration.auth.UserService;
import com.migration.marketplace.LabDevtoolsInstaller;
import com.migration.marketplace.MarketplaceCatalog;
import com.migration.marketplace.MarketplaceInstallEntity;
import com.migration.marketplace.MarketplaceInstallRepository;
import com.migration.marketplace.MarketplaceRemoteInstallService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarketplaceControllerTest {

    @Mock
    private ConnectorPluginRepository pluginRepository;
    @Mock
    private ConnectionRepository connectionRepository;
    @Mock
    private PluginDirectoryService pluginDirectory;
    @Mock
    private UserService userService;
    @Mock
    private MarketplaceCatalog marketplaceCatalog;
    @Mock
    private MarketplaceRemoteInstallService remoteInstallService;
    @Mock
    private MarketplaceInstallRepository marketplaceInstallRepository;
    @Mock
    private LabDevtoolsInstaller labDevtoolsInstaller;

    private MarketplaceController controller;

    @BeforeEach
    void setUp() {
        controller = new MarketplaceController(
            pluginRepository,
            new ConnectorPluginRegistry(),
            connectionRepository,
            pluginDirectory,
            userService,
            marketplaceCatalog,
            remoteInstallService,
            marketplaceInstallRepository,
            labDevtoolsInstaller
        );
    }

    @Test
    void uninstallLabDevtools_usesToolPath_notConnectorLookup() throws Exception {
        when(userService.isAdmin("admin")).thenReturn(true);
        when(marketplaceCatalog.find("lab-devtools")).thenReturn(Optional.of(
            new MarketplaceCatalog.CatalogItem(
                "lab-devtools", "TOOL", "Lab Dev Tools", "d", "0.1.0", "a.zip", "sha")));
        when(marketplaceInstallRepository.existsById("lab-devtools")).thenReturn(true);

        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getName()).thenReturn("admin");

        Map<String, Object> result = controller.uninstall("lab-devtools", auth);

        assertEquals(false, result.get("installed"));
        verify(labDevtoolsInstaller).cleanup();
        verify(pluginDirectory).uninstallTool("lab-devtools");
        verify(marketplaceInstallRepository).deleteById("lab-devtools");
        verify(pluginRepository, never()).findById("lab-devtools");
    }

    @Test
    void uninstallTool_whenCatalogMisses_butInstallRecordIsTool() throws Exception {
        when(userService.isAdmin("admin")).thenReturn(true);
        when(marketplaceCatalog.find("lab-devtools")).thenReturn(Optional.empty());
        MarketplaceInstallEntity install = new MarketplaceInstallEntity();
        install.setId("lab-devtools");
        install.setKind("TOOL");
        when(marketplaceInstallRepository.findById("lab-devtools")).thenReturn(Optional.of(install));
        when(marketplaceInstallRepository.existsById("lab-devtools")).thenReturn(true);

        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getName()).thenReturn("admin");

        controller.uninstall("lab-devtools", auth);

        verify(pluginDirectory).uninstallTool("lab-devtools");
        verify(pluginRepository, never()).findById("lab-devtools");
    }
}

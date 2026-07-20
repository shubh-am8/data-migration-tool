package com.migration.marketplace;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class MarketplaceCatalogTest {

    @TempDir
    Path tempDir;

    @Test
    void loadsItemsFromFile() throws IOException {
        Path catalogPath = tempDir.resolve("catalog.json");
        Files.writeString(catalogPath, """
            {
              "items": [
                {"id": "postgresql", "kind": "CONNECTOR", "name": "PostgreSQL",
                 "description": "d", "version": "0.1.0",
                 "asset": "postgresql-connector-0.1.0.jar", "sha256": "%s"}
              ]
            }
            """.formatted("a".repeat(64)), StandardCharsets.UTF_8);

        MarketplaceCatalog catalog = new MarketplaceCatalog(catalogPath.toString());

        assertEquals(1, catalog.all().size());
        var item = catalog.find("postgresql").orElseThrow();
        assertEquals("CONNECTOR", item.kind());
        assertEquals("postgresql-connector-0.1.0.jar", item.asset());
        assertTrue(catalog.find("nope").isEmpty());
    }

    @Test
    void loadsRealRepoCatalog() throws IOException {
        // Mirrors app.marketplace.catalog-path's default: Surefire/spring-boot:run both use
        // services/api as the working directory, so ../../ reaches the repo root.
        MarketplaceCatalog catalog = new MarketplaceCatalog("../../marketplace/catalog.json");
        assertTrue(catalog.find("postgresql").isPresent());
        assertTrue(catalog.find("lab-devtools").isPresent());
        assertEquals("TOOL", catalog.find("lab-devtools").orElseThrow().kind());
    }
}

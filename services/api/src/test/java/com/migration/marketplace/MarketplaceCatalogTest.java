package com.migration.marketplace;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

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

    @Test
    void loadsPackagedClasspathCatalogWhenFileMissing() throws IOException {
        MarketplaceCatalog catalog = new MarketplaceCatalog("/nonexistent/marketplace/catalog.json");
        assertTrue(catalog.find("postgresql").isPresent());
        assertTrue(catalog.find("lab-devtools").isPresent());
    }

    @Test
    void repoCatalogMatchesPackagedClasspathCopy() throws IOException, NoSuchAlgorithmException {
        // Both are read at runtime depending on cwd/packaging (see the two tests above); if they
        // drift, "remote" mode (repo file) and packaged jars (classpath copy) disagree on what's installable.
        Path repoCatalog = Path.of("../../marketplace/catalog.json");
        Path packagedCatalog = Path.of("src/main/resources/marketplace/catalog.json");
        assertTrue(Files.isRegularFile(repoCatalog), "missing " + repoCatalog.toAbsolutePath());
        assertTrue(Files.isRegularFile(packagedCatalog), "missing " + packagedCatalog.toAbsolutePath());

        assertEquals(sha256Of(repoCatalog), sha256Of(packagedCatalog),
            "marketplace/catalog.json and services/api/src/main/resources/marketplace/catalog.json "
                + "must be byte-identical; copy one over the other to resync.");
    }

    private static String sha256Of(Path path) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(digest.digest(Files.readAllBytes(path)));
    }
}

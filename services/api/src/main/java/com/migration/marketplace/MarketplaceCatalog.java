package com.migration.marketplace;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Allowlisted marketplace items (connectors + tools), loaded once at startup from
 * {@code catalog.json} — a file on disk in dev (repo checkout) or a packaged classpath
 * resource in prod. Never fetched over the network: the catalog only names assets and
 * their expected SHA-256; {@link MarketplaceRemoteInstallService} does the fetching.
 */
@Component
public class MarketplaceCatalog {

    public record CatalogItem(String id, String kind, String name, String description,
                               String version, String asset, String sha256) {}

    private final List<CatalogItem> items;

    public MarketplaceCatalog(
        @Value("${app.marketplace.catalog-path:../../marketplace/catalog.json}") String catalogPath
    ) throws IOException {
        this.items = load(catalogPath);
    }

    static List<CatalogItem> load(String catalogPath) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream in = openCatalogStream(catalogPath)) {
            JsonNode root = mapper.readTree(in);
            List<CatalogItem> parsed = new ArrayList<>();
            for (JsonNode n : root.path("items")) {
                parsed.add(new CatalogItem(
                    n.path("id").asText(),
                    n.path("kind").asText(),
                    n.path("name").asText(),
                    n.path("description").asText(),
                    n.path("version").asText(),
                    n.path("asset").asText(),
                    n.path("sha256").asText()));
            }
            return List.copyOf(parsed);
        }
    }

    private static InputStream openCatalogStream(String catalogPath) throws IOException {
        Path path = Path.of(catalogPath);
        if (Files.isRegularFile(path)) {
            return Files.newInputStream(path);
        }
        InputStream classpath = MarketplaceCatalog.class.getResourceAsStream("/marketplace/catalog.json");
        if (classpath != null) {
            return classpath;
        }
        throw new IOException(
            "Marketplace catalog not found at " + path.toAbsolutePath() + " or classpath /marketplace/catalog.json");
    }

    public List<CatalogItem> all() {
        return items;
    }

    public Optional<CatalogItem> find(String id) {
        return items.stream().filter(i -> i.id().equals(id)).findFirst();
    }
}

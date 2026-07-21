package com.migration.marketplace;

import com.migration.auth.UserService;
import com.migration.connectors.ColumnInfo;
import com.migration.connectors.PluginDirectoryService;
import com.migration.connectors.SchemaInfo;
import com.migration.connectors.TableInfo;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@RestController
@RequestMapping("/api/lab")
public class LabController {
    private final LabIntrospectionService labIntrospection;
    private final LabAdminService labAdmin;
    private final LabDevtoolsInstaller labDevtoolsInstaller;
    private final PluginDirectoryService pluginDirectory;
    private final UserService userService;

    public LabController(LabIntrospectionService labIntrospection,
                         LabAdminService labAdmin,
                         LabDevtoolsInstaller labDevtoolsInstaller,
                         PluginDirectoryService pluginDirectory,
                         UserService userService) {
        this.labIntrospection = labIntrospection;
        this.labAdmin = labAdmin;
        this.labDevtoolsInstaller = labDevtoolsInstaller;
        this.pluginDirectory = pluginDirectory;
        this.userService = userService;
    }

    @GetMapping("/schemas")
    public SchemaInfo schemas() {
        return wrap(() -> labIntrospection.listSchemas());
    }

    @GetMapping("/schemas/{schema}/tables")
    public TableInfo tables(@PathVariable String schema) {
        return wrap(() -> labIntrospection.listTables(schema));
    }

    @GetMapping("/schemas/{schema}/tables/{table}/columns")
    public ColumnInfo columns(@PathVariable String schema, @PathVariable String table) {
        return wrap(() -> labIntrospection.listColumns(schema, table));
    }

    @GetMapping("/stats/{schema}")
    public LabTableStats.SchemaStats stats(@PathVariable String schema) {
        return wrap(() -> labIntrospection.tableStats(schema));
    }

    @PostMapping("/schemas/{schema}/tables/{table}/truncate")
    public Map<String, Object> truncate(@PathVariable String schema, @PathVariable String table) {
        return wrap(() -> {
            labAdmin.truncateTable(schema, table);
            return Map.of("schema", schema, "table", table, "action", "truncate");
        });
    }

    @PostMapping("/schemas/{schema}/tables/{table}/drop")
    public Map<String, Object> drop(@PathVariable String schema, @PathVariable String table) {
        return wrap(() -> {
            labAdmin.dropTable(schema, table);
            return Map.of("schema", schema, "table", table, "action", "drop");
        });
    }

    @PostMapping("/destination/truncate-all")
    public Map<String, Object> truncateAllDestination() {
        return wrap(() -> Map.of("tablesAffected", labAdmin.truncateAllDestination()));
    }

    @PostMapping("/destination/drop-all")
    public Map<String, Object> dropAllDestination() {
        return wrap(() -> Map.of("tablesAffected", labAdmin.dropAllDestination()));
    }

    /** Re-applies lab-devtools SQL without marketplace uninstall (admin only). */
    @PostMapping("/repair")
    public Map<String, Object> repair(Authentication auth) {
        requireAdmin(auth);
        Path toolDir = pluginDirectory.toolsDir().resolve("lab-devtools");
        if (!Files.isDirectory(toolDir)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Lab Dev Tools not installed — install from Marketplace first.");
        }
        try {
            labDevtoolsInstaller.apply(toolDir);
            LabDevtoolsInstaller.InstallVerification verification = labDevtoolsInstaller.verifyInstalled();
            if (!verification.isValid()) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Repair incomplete: test_source has "
                        + verification.sourceTableCount()
                        + " table(s), expected at least 2.");
            }
            return Map.of(
                "repaired", true,
                "sourceTableCount", verification.sourceTableCount(),
                "destinationTableCount", verification.destinationTableCount()
            );
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Lab repair failed — is labdb running on :5433? " + e.getMessage());
        }
    }

    private void requireAdmin(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || !userService.isAdmin(auth.getName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin required");
        }
    }

    private <T> T wrap(SqlSupplier<T> supplier) {
        try {
            return supplier.get();
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Lab database unavailable — is labdb running on :5433? Install Lab Dev Tools if schemas are missing.");
        }
    }

    @FunctionalInterface
    private interface SqlSupplier<T> {
        T get() throws Exception;
    }
}

package com.migration.marketplace;

import com.migration.connectors.ColumnInfo;
import com.migration.connectors.SchemaInfo;
import com.migration.connectors.TableInfo;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/lab")
public class LabController {
    private final LabIntrospectionService labIntrospection;

    public LabController(LabIntrospectionService labIntrospection) {
        this.labIntrospection = labIntrospection;
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

package com.migration.marketplace;

import com.migration.jobs.JobEntity;
import com.migration.jobs.JobRunMode;
import com.migration.jobs.LabSchemas;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/** Creates per-job tables in {@link LabSchemas#DESTINATION} for TEST migrations. */
@Service
public class LabDestinationProvisioner {
    private final String url;
    private final String user;
    private final String password;

    public LabDestinationProvisioner(
        @Value("${app.lab-db.url:jdbc:postgresql://localhost:5433/migration_lab}") String url,
        @Value("${app.lab-db.user:migration}") String user,
        @Value("${app.lab-db.password:migration}") String password
    ) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    public void provision(JobEntity job) throws SQLException {
        if (job.getRunMode() != JobRunMode.TEST) return;
        if (job.getSchemaName() == null || job.getSourceTable() == null) return;

        String destTable = destinationTableName(job.getId());
        String sourceSchema = job.getSchemaName();
        String sourceTable = job.effectiveTable();
        LabIntrospectionService.requireAllowedSchema(sourceSchema);
        if (!LabSchemas.SOURCE.equals(sourceSchema)) {
            throw new IllegalArgumentException("TEST job source schema must be " + LabSchemas.SOURCE);
        }
        validateIdent(sourceTable);
        validateIdent(destTable);

        String ddl = "CREATE TABLE IF NOT EXISTS "
            + quoteIdent(LabSchemas.DESTINATION) + "." + quoteIdent(destTable)
            + " (LIKE " + quoteIdent(sourceSchema) + "." + quoteIdent(sourceTable) + " INCLUDING ALL)";

        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement stmt = conn.createStatement()) {
            stmt.execute(ddl);
        }

        job.setDestSchemaName(LabSchemas.DESTINATION);
        job.setDestTable(destTable);
    }

    public static String destinationTableName(java.util.UUID jobId) {
        String hex = jobId.toString().replace("-", "");
        return "job_" + hex.substring(0, 12);
    }

    private static void validateIdent(String ident) {
        if (!ident.matches("[a-z_][a-z0-9_]*")) {
            throw new IllegalArgumentException("Invalid identifier: " + ident);
        }
    }

    private static String quoteIdent(String ident) {
        validateIdent(ident);
        return ident;
    }
}

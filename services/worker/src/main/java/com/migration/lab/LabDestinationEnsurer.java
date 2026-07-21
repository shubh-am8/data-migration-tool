package com.migration.lab;

import com.migration.jobs.JobEntity;
import com.migration.jobs.JobRunMode;
import com.migration.jobs.LabSchemas;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

/** Ensures per-job destination table exists in {@link LabSchemas#DESTINATION} before migration copy. */
public class LabDestinationEnsurer {
    private final String url;
    private final String user;
    private final String password;

    public LabDestinationEnsurer(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    public void ensure(JobEntity job) throws SQLException {
        if (job.getRunMode() != JobRunMode.TEST) return;
        if (job.getSchemaName() == null || job.getSourceTable() == null) return;
        if (!LabSchemas.SOURCE.equals(job.getSchemaName())) {
            throw new IllegalArgumentException("TEST job source schema must be " + LabSchemas.SOURCE);
        }

        String destTable = job.getDestTable() != null
            ? job.getDestTable()
            : destinationTableName(job.getId());
        String sourceTable = job.effectiveTable();
        validateIdent(sourceTable);
        validateIdent(destTable);

        String ddl = "CREATE TABLE IF NOT EXISTS "
            + quoteIdent(LabSchemas.DESTINATION) + "." + quoteIdent(destTable)
            + " (LIKE " + quoteIdent(LabSchemas.SOURCE) + "." + quoteIdent(sourceTable) + " INCLUDING ALL)";

        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement stmt = conn.createStatement()) {
            stmt.execute(ddl);
        }

        if (job.getDestSchemaName() == null) {
            job.setDestSchemaName(LabSchemas.DESTINATION);
        }
        if (job.getDestTable() == null) {
            job.setDestTable(destTable);
        }
    }

    public static String destinationTableName(UUID jobId) {
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

package com.migration.lab;

import com.migration.jobs.JobEntity;
import com.migration.jobs.JobRunMode;
import com.migration.jobs.LabSchemas;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class LabDestinationEnsurerTest {

    @Test
    void destinationTableNameUsesJobIdPrefix() {
        UUID id = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");
        assertEquals("job_a1b2c3d4e5f6", LabDestinationEnsurer.destinationTableName(id));
    }

    @Test
    void ensureSkipsNonTestJobs() throws Exception {
        JobEntity job = new JobEntity();
        job.setRunMode(JobRunMode.PRODUCTION);
        job.setSchemaName("public");
        job.setSourceTable("orders");
        new LabDestinationEnsurer("jdbc:invalid", "u", "p").ensure(job);
        assertNull(job.getDestTable());
    }

    @Test
    void ensureRejectsNonSourceSchema() {
        JobEntity job = new JobEntity();
        job.setId(UUID.randomUUID());
        job.setRunMode(JobRunMode.TEST);
        job.setSchemaName(LabSchemas.DESTINATION);
        job.setSourceTable("orders_cold");
        assertThrows(IllegalArgumentException.class,
            () -> new LabDestinationEnsurer("jdbc:invalid", "u", "p").ensure(job));
    }
}

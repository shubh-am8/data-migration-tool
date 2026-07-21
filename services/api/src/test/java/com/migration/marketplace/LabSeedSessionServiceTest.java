package com.migration.marketplace;

import com.migration.jobs.JobEntity;
import com.migration.jobs.JobRunMode;
import com.migration.jobs.LabSchemas;
import com.migration.jobs.MigrationMode;
import com.migration.lab.LabSeedSessionEntity;
import com.migration.lab.LabSeedStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LabSeedSessionServiceTest {

    @Mock
    private LabSeedSessionRepository repository;

    @Mock
    private com.migration.jobs.JobRepository jobRepository;

    @InjectMocks
    private LabSeedSessionService service;

    @Test
    void createForJobSkipsSimulationJobs() {
        JobEntity job = testJob();
        job.setConfigJson("{\"kind\":\"SIMULATE\"}");
        service.createForJob(job);
        verify(repository, never()).save(any());
    }

    @Test
    void createForJobCreatesPausedSession() {
        JobEntity job = testJob();
        when(repository.existsById(job.getId())).thenReturn(false);

        service.createForJob(job);

        ArgumentCaptor<LabSeedSessionEntity> captor = ArgumentCaptor.forClass(LabSeedSessionEntity.class);
        verify(repository).save(captor.capture());
        LabSeedSessionEntity saved = captor.getValue();
        assertEquals(job.getId(), saved.getJobId());
        assertEquals(LabSchemas.SOURCE, saved.getSchemaName());
        assertEquals("orders_hot_cold", saved.getTableName());
        assertEquals(LabSeedStatus.PAUSED, saved.getStatus());
    }

    private static JobEntity testJob() {
        JobEntity job = new JobEntity();
        job.setId(UUID.randomUUID());
        job.setRunMode(JobRunMode.TEST);
        job.setSchemaName(LabSchemas.SOURCE);
        job.setSourceTable("orders_hot_cold");
        job.setMigrationMode(MigrationMode.HOT_THEN_COLD);
        job.setConfigJson("{}");
        return job;
    }
}

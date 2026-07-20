package com.migration.dashboard;

import com.migration.jobs.JobRepository;
import com.migration.jobs.JobStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:postgresql://localhost:5432/migration_app",
    "spring.datasource.username=migration",
    "spring.datasource.password=migration",
    "spring.jpa.hibernate.ddl-auto=validate",
    "spring.flyway.enabled=false"
})
class DashboardStatsProbeTest {

    @Autowired
    JobRepository jobRepository;

    @Test
    void countByStatusWorksAgainstPostgresEnumColumn() {
        assertDoesNotThrow(() -> {
            jobRepository.countByStatus(JobStatus.RUNNING);
            jobRepository.countByStatus(JobStatus.FAILED);
            jobRepository.countByStatus(JobStatus.PENDING);
        });
    }
}

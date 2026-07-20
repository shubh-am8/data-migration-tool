package com.migration.jobs;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface JobRepository extends JpaRepository<JobEntity, UUID> {
    List<JobEntity> findByStatus(JobStatus status);
    long countByStatus(JobStatus status);
}

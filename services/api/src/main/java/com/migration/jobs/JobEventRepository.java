package com.migration.jobs;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface JobEventRepository extends JpaRepository<JobEventEntity, UUID> {
    List<JobEventEntity> findByJobIdOrderByCreatedAtDesc(UUID jobId);
}

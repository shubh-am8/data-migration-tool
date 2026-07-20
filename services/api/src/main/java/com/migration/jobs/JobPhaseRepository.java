package com.migration.jobs;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JobPhaseRepository extends JpaRepository<JobPhaseEntity, UUID> {
    List<JobPhaseEntity> findByJobId(UUID jobId);
    Optional<JobPhaseEntity> findByJobIdAndPhase(UUID jobId, PhaseType phase);
}

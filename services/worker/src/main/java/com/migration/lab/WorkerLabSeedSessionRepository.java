package com.migration.lab;

import com.migration.lab.LabSeedSessionEntity;
import com.migration.lab.LabSeedStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WorkerLabSeedSessionRepository extends JpaRepository<LabSeedSessionEntity, UUID> {
    List<LabSeedSessionEntity> findByStatus(LabSeedStatus status);
}

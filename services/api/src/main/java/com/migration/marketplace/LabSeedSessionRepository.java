package com.migration.marketplace;

import com.migration.lab.LabSeedSessionEntity;
import com.migration.lab.LabSeedStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LabSeedSessionRepository extends JpaRepository<LabSeedSessionEntity, UUID> {
    List<LabSeedSessionEntity> findByStatus(LabSeedStatus status);
}

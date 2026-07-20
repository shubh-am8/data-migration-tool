package com.migration.jobs;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface AlertConfigRepository extends JpaRepository<AlertConfigEntity, UUID> {}

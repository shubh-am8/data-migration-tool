package com.migration.connectors;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface WorkerConnectionRepository extends JpaRepository<WorkerConnectionEntity, UUID> {}

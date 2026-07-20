package com.migration.workers;

import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkerHeartbeatRepository extends JpaRepository<WorkerHeartbeatEntity, String> {}

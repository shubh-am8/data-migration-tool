package com.migration.connectors;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface ConnectionRepository extends JpaRepository<ConnectionEntity, UUID> {}

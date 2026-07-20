package com.migration.config;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AppConfigRepository extends JpaRepository<AppConfigEntity, String> {}

package com.migration.marketplace;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MarketplaceInstallRepository extends JpaRepository<MarketplaceInstallEntity, String> {
}

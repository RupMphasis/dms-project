package com.dms.inventory_service.repository;

import com.dms.inventory_service.entity.DistributorInventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DistributorInventoryRepository extends JpaRepository<DistributorInventoryItem, Long> {
    List<DistributorInventoryItem> findByDistributorId(Long distributorId);
    Optional<DistributorInventoryItem> findByProductIdAndDistributorId(Long productId, Long distributorId);
    long countByProductId(Long productId);
    long countByDistributorId(Long distributorId);
}


package com.dms.order_service.repository;

import com.dms.order_service.entity.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, Long> {
    List<OrderEntity> findByDistributorId(Long distributorId);
    List<OrderEntity> findByProductId(Long productId);
    List<OrderEntity> findByProductIdAndStatusInOrderByCreatedAtAsc(Long productId, List<String> statuses);
    long countByProductIdAndStatusNotIn(Long productId, List<String> terminalStatuses);
    long countByDistributorIdAndStatusNotIn(Long distributorId, List<String> terminalStatuses);
}
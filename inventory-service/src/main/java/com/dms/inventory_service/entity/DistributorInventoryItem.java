package com.dms.inventory_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "distributor_inventory")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DistributorInventoryItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "distributor_id", nullable = false)
    private Long distributorId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "total_purchased", nullable = false, columnDefinition = "integer default 0")
    private Integer totalPurchased;

    @Column(name = "total_sold", nullable = false, columnDefinition = "integer default 0")
    private Integer totalSold;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;
}

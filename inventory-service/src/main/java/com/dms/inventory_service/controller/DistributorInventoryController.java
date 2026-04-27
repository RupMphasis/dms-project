package com.dms.inventory_service.controller;

import com.dms.inventory_service.entity.DistributorInventoryItem;
import com.dms.inventory_service.repository.DistributorInventoryRepository;
import com.dms.inventory_service.service.DistributorInventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/distributor-inventory")
@RequiredArgsConstructor
public class DistributorInventoryController {

    private final DistributorInventoryService service;
    private final DistributorInventoryRepository distributorInventoryRepository;

    @GetMapping
    public ResponseEntity<List<DistributorInventoryItem>> getAll(@RequestParam(required = false) Long distributorId) {
        if (distributorId != null) {
            return ResponseEntity.ok(service.findByDistributorId(distributorId));
        }
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DistributorInventoryItem> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<DistributorInventoryItem> getByProductId(@PathVariable Long productId, @RequestParam Long distributorId) {
        return ResponseEntity.ok(service.getByProductIdAndDistributorId(productId, distributorId));
    }

    @PostMapping
    public ResponseEntity<DistributorInventoryItem> create(@RequestBody DistributorInventoryItem item) {
        return ResponseEntity.ok(service.create(item));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DistributorInventoryItem> update(@PathVariable Long id, @RequestBody DistributorInventoryItem item) {
        return ResponseEntity.ok(service.update(id, item));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{productId}/adjust")
    public ResponseEntity<Void> adjustStock(
            @PathVariable Long productId,
            @RequestParam int delta,
            @RequestParam Long distributorId,
            @RequestParam(required = false) String transactionType,
            @RequestParam(required = false) String note) {
        
        service.adjustStock(productId, distributorId, delta, transactionType);
        return ResponseEntity.ok().build();
    }

    /**
     * Returns count of distributor inventory rows referencing a product.
     * Used by product-service before deleting a product to enforce ACID integrity.
     */
    @GetMapping("/count/product/{productId}")
    public ResponseEntity<Map<String, Long>> countByProduct(@PathVariable Long productId) {
        long count = distributorInventoryRepository.countByProductId(productId);
        return ResponseEntity.ok(Map.of("count", count));
    }

    /**
     * Returns count of distributor inventory rows referencing a distributor.
     * Used by user-service / distributor-service before deleting a distributor.
     */
    @GetMapping("/count/distributor/{distributorId}")
    public ResponseEntity<Map<String, Long>> countByDistributor(@PathVariable Long distributorId) {
        long count = distributorInventoryRepository.countByDistributorId(distributorId);
        return ResponseEntity.ok(Map.of("count", count));
    }
}


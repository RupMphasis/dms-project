package com.dms.inventory_service.controller;

import com.dms.inventory_service.entity.DistributorInventoryItem;
import com.dms.inventory_service.service.DistributorInventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/distributor-inventory")
@RequiredArgsConstructor
public class DistributorInventoryController {

    private final DistributorInventoryService service;

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
}

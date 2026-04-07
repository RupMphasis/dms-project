package com.dms.inventory_service.controller;

import com.dms.inventory_service.entity.InventoryItem;
import com.dms.inventory_service.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping
    public ResponseEntity<List<InventoryItem>> getAll() {
        return ResponseEntity.ok(inventoryService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<InventoryItem> getById(@PathVariable Long id) {
        return ResponseEntity.ok(inventoryService.getById(id));
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<InventoryItem> getByProductId(@PathVariable Long productId) {
        return ResponseEntity.ok(inventoryService.getByProductId(productId));
    }

    @PostMapping
    public ResponseEntity<InventoryItem> create(@RequestBody InventoryItem item) {
        return ResponseEntity.ok(inventoryService.create(item));
    }

    @PutMapping("/{id}")
    public ResponseEntity<InventoryItem> update(@PathVariable Long id, @RequestBody InventoryItem item) {
        return ResponseEntity.ok(inventoryService.update(id, item));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        inventoryService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{productId}/adjust")
    public ResponseEntity<Void> adjustStock(@PathVariable Long productId, @RequestParam int delta) {
        inventoryService.adjustStock(productId, delta);
        return ResponseEntity.ok().build();
    }
}
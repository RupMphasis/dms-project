package com.dms.inventory_service.controller;

import com.dms.inventory_service.entity.InventoryItem;
import com.dms.inventory_service.repository.InventoryRepository;
import com.dms.inventory_service.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;
    private final InventoryRepository inventoryRepository;

    @Value("${order.service.url:http://localhost:8083}")
    private String orderServiceUrl;

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
        InventoryItem created = inventoryService.create(item);
        try {
            recalculateProductOrders(created.getProductId());
        } catch (Exception ignored) {
        }
        return ResponseEntity.ok(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<InventoryItem> update(@PathVariable Long id, @RequestBody InventoryItem item) {
        InventoryItem updated = inventoryService.update(id, item);
        try {
            recalculateProductOrders(updated.getProductId());
        } catch (Exception ignored) {
        }
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        inventoryService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{productId}/adjust")
    public ResponseEntity<Void> adjustStock(@PathVariable Long productId, @RequestParam int delta) {
        inventoryService.adjustStock(productId, delta);
        try {
            recalculateProductOrders(productId);
        } catch (Exception ignored) {
        }
        return ResponseEntity.ok().build();
    }

    /**
     * Checks whether a central inventory row exists for the given product.
     * Used by product-service before deleting a product to enforce ACID integrity.
     */
    @GetMapping("/exists/product/{productId}")
    public ResponseEntity<Map<String, Boolean>> existsByProduct(@PathVariable Long productId) {
        boolean exists = inventoryRepository.existsByProductId(productId);
        return ResponseEntity.ok(Map.of("exists", exists));
    }

    private void recalculateProductOrders(Long productId) {
        RestTemplate restTemplate = new RestTemplate();
        String url = orderServiceUrl + "/api/orders/product/" + productId + "/recalculate";
        restTemplate.postForEntity(url, null, Void.class);
    }
}
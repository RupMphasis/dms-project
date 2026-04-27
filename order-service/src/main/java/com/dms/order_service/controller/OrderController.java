package com.dms.order_service.controller;

import com.dms.order_service.dto.OrderDecisionDto;
import com.dms.order_service.entity.OrderEntity;
import com.dms.order_service.repository.OrderRepository;
import com.dms.order_service.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final OrderRepository orderRepository;

    private static final List<String> TERMINAL_STATUSES = List.of("CANCELLED", "REJECTED");

    @GetMapping
    public ResponseEntity<List<OrderEntity>> getAll() {
        return ResponseEntity.ok(orderService.findAll());
    }

    @GetMapping("/distributor/{distributorId}")
    public ResponseEntity<List<OrderEntity>> getByDistributor(@PathVariable Long distributorId) {
        return ResponseEntity.ok(orderService.findByDistributor(distributorId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderEntity> getById(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getById(id));
    }

    @PostMapping
    public ResponseEntity<OrderEntity> create(@RequestBody OrderEntity order) {
        return ResponseEntity.ok(orderService.create(order));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<OrderEntity> updateStatus(@PathVariable Long id, @RequestParam String status) {
        return ResponseEntity.ok(orderService.updateStatus(id, status, null));
    }

    @PostMapping("/{id}/decision")
    public ResponseEntity<OrderEntity> decision(@PathVariable Long id, @RequestBody OrderDecisionDto decision) {
        return ResponseEntity.ok(orderService.updateStatus(id, decision.getStatus(), decision.getAdminMessage()));
    }

    @PostMapping("/product/{productId}/recalculate")
    public ResponseEntity<Void> recalculateProductOrders(@PathVariable Long productId) {
        orderService.recalculateFulfillmentForProduct(productId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        orderService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Returns the count of non-terminal (active) orders referencing a product.
     * Used by product-service before deleting a product to enforce ACID integrity.
     */
    @GetMapping("/count/product/{productId}")
    public ResponseEntity<Map<String, Long>> countActiveByProduct(@PathVariable Long productId) {
        long count = orderRepository.countByProductIdAndStatusNotIn(productId, TERMINAL_STATUSES);
        return ResponseEntity.ok(Map.of("count", count));
    }

    /**
     * Returns the count of non-terminal (active) orders referencing a distributor.
     * Used by user-service / distributor-service before deleting a distributor.
     */
    @GetMapping("/count/distributor/{distributorId}")
    public ResponseEntity<Map<String, Long>> countActiveByDistributor(@PathVariable Long distributorId) {
        long count = orderRepository.countByDistributorIdAndStatusNotIn(distributorId, TERMINAL_STATUSES);
        return ResponseEntity.ok(Map.of("count", count));
    }
}


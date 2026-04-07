package com.dms.order_service.controller;

import com.dms.order_service.entity.OrderEntity;
import com.dms.order_service.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

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
        return ResponseEntity.ok(orderService.updateStatus(id, status));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        orderService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

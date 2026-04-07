package com.dms.order_service.service;

import com.dms.order_service.client.AuditClient;
import com.dms.order_service.client.InventoryClient;
import com.dms.order_service.dto.AuditEventDto;
import com.dms.order_service.entity.OrderEntity;
import com.dms.order_service.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final InventoryClient inventoryClient;
    private final AuditClient auditClient;

    public List<OrderEntity> findAll() {
        return orderRepository.findAll();
    }

    public List<OrderEntity> findByDistributor(Long distributorId) {
        return orderRepository.findByDistributorId(distributorId);
    }

    public OrderEntity getById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found with id " + id));
    }

    @Transactional
    public OrderEntity create(OrderEntity order) {
        // Deduct stock from inventory service for product
        inventoryClient.adjustStock(order.getProductId(), -order.getQuantity());
        order.setStatus(order.getStatus() == null ? "PENDING" : order.getStatus());
        OrderEntity saved = orderRepository.save(order);
        auditClient.logEvent(new AuditEventDto(
                null,
                "ORDER_CREATED",
                String.valueOf(order.getDistributorId()),
                "ORDER",
                String.valueOf(saved.getId()),
                "Order created for product " + order.getProductId() + " and quantity " + order.getQuantity(),
                LocalDateTime.now()
        ));
        return saved;
    }

    @Transactional
    public OrderEntity updateStatus(Long id, String status) {
        OrderEntity order = getById(id);
        if (!"CANCELLED".equalsIgnoreCase(order.getStatus()) && "CANCELLED".equalsIgnoreCase(status)) {
            // return stock when canceled
            inventoryClient.adjustStock(order.getProductId(), order.getQuantity());
        }
        order.setStatus(status);
        OrderEntity saved = orderRepository.save(order);
        auditClient.logEvent(new AuditEventDto(
                null,
                "ORDER_STATUS_UPDATED",
                String.valueOf(order.getDistributorId()),
                "ORDER",
                String.valueOf(saved.getId()),
                "Order status updated to " + status,
                LocalDateTime.now()
        ));
        return saved;
    }

    @Transactional
    public void delete(Long id) {
        OrderEntity order = getById(id);
        auditClient.logEvent(new AuditEventDto(
                null,
                "ORDER_DELETED",
                String.valueOf(order.getDistributorId()),
                "ORDER",
                String.valueOf(order.getId()),
                "Order deleted",
                LocalDateTime.now()
        ));
        orderRepository.delete(order);
    }
}

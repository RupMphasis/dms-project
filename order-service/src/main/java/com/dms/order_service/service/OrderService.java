package com.dms.order_service.service;

import com.dms.order_service.client.AuditClient;
import com.dms.order_service.client.InventoryClient;
import com.dms.order_service.client.ProductClient;
import com.dms.order_service.client.ProductClient.ProductResponse;
import com.dms.order_service.dto.AuditEventDto;
import com.dms.order_service.entity.OrderEntity;
import com.dms.order_service.exception.OrderBadRequestException;
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
    private final ProductClient productClient;
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
        if ("APPROVED".equalsIgnoreCase(order.getStatus())) {
            boolean retry = false;
            try {
                inventoryClient.adjustStock(order.getProductId(), -order.getQuantity());
            } catch (RuntimeException ex) {
                if (ex.getMessage() != null && ex.getMessage().contains("Inventory item not found")) {
                    ProductResponse product = productClient.getProduct(order.getProductId());
                    inventoryClient.createInventory(order.getProductId(), product.getStock() != null ? product.getStock() : 0, "DEFAULT");
                    retry = true;
                } else {
                    throw new OrderBadRequestException("Unable to place order: " + ex.getMessage(), ex);
                }
            }

            if (retry) {
                try {
                    inventoryClient.adjustStock(order.getProductId(), -order.getQuantity());
                } catch (RuntimeException ex) {
                    throw new OrderBadRequestException("Unable to place order: " + ex.getMessage(), ex);
                }
            }
        }

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
    public OrderEntity updateStatus(Long id, String status, String adminMessage) {
        OrderEntity order = getById(id);
        boolean wasApproved = "APPROVED".equalsIgnoreCase(order.getStatus());
        boolean willApprove = "APPROVED".equalsIgnoreCase(status);
        boolean willCancel = "CANCELLED".equalsIgnoreCase(status);

        if (!wasApproved && willApprove) {
            allocateFulfillment(order);
        } else {
            order.setStatus(status);
        }

        if (wasApproved && willCancel) {
            int restockQuantity = order.getFulfilledQuantity() != null ? order.getFulfilledQuantity() : order.getQuantity();
            inventoryClient.adjustStock(order.getProductId(), restockQuantity);
        }

        if (adminMessage != null) {
            order.setAdminMessage(adminMessage);
        }
        OrderEntity saved = orderRepository.save(order);
        auditClient.logEvent(new AuditEventDto(
                null,
                "ORDER_STATUS_UPDATED",
                String.valueOf(order.getDistributorId()),
                "ORDER",
                String.valueOf(saved.getId()),
                "Order status updated to " + status + (adminMessage != null && !adminMessage.isBlank() ? " by admin: " + adminMessage : ""),
                LocalDateTime.now()
        ));
        return saved;
    }

    @Transactional
    public void delete(Long id) {
        OrderEntity order = getById(id);
        if (!"CANCELLED".equalsIgnoreCase(order.getStatus())) {
            inventoryClient.adjustStock(order.getProductId(), order.getQuantity());
        }
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

    private void allocateFulfillment(OrderEntity order) {
        int availableStock;
        try {
            Integer stock = inventoryClient.getStock(order.getProductId());
            availableStock = stock != null ? stock : 0;
        } catch (RuntimeException ex) {
            availableStock = 0;
        }

        int alreadyFulfilled = order.getFulfilledQuantity() != null ? order.getFulfilledQuantity() : 0;
        int pendingBefore = order.getPendingQuantity() != null ? order.getPendingQuantity() : order.getQuantity() - alreadyFulfilled;
        int allocate = Math.min(availableStock, pendingBefore);
        if (allocate > 0) {
            inventoryClient.adjustStock(order.getProductId(), -allocate);
            availableStock -= allocate;
        }

        int remainingPending = pendingBefore - allocate;
        if (remainingPending > 0) {
            ProductResponse product = productClient.getProduct(order.getProductId());
            int capacity = product.getProductionCapacityPerDay() != null ? product.getProductionCapacityPerDay() : 0;
            int estimatedDays = capacity > 0 ? (int) Math.ceil((double) remainingPending / capacity) : 0;
            order.setFulfilledQuantity(alreadyFulfilled + allocate);
            order.setPendingQuantity(remainingPending);
            order.setEstimatedFulfillmentDays(estimatedDays);
            order.setStatus("PARTIALLY_FULFILLED");
        } else {
            order.setFulfilledQuantity(order.getQuantity());
            order.setPendingQuantity(0);
            order.setEstimatedFulfillmentDays(0);
            order.setStatus("APPROVED");
        }
    }

    @Transactional
    public void recalculateFulfillmentForProduct(Long productId) {
        int availableStock;
        try {
            Integer stock = inventoryClient.getStock(productId);
            availableStock = stock != null ? stock : 0;
        } catch (RuntimeException ex) {
            availableStock = 0;
        }

        ProductResponse product = productClient.getProduct(productId);
        int capacity = product.getProductionCapacityPerDay() != null ? product.getProductionCapacityPerDay() : 0;

        List<String> statuses = List.of("PARTIALLY_FULFILLED", "APPROVED");
        List<OrderEntity> orders = orderRepository.findByProductIdAndStatusInOrderByCreatedAtAsc(productId, statuses);
        for (OrderEntity order : orders) {
            int alreadyFulfilled = order.getFulfilledQuantity() != null ? order.getFulfilledQuantity() : 0;
            int pendingBefore = order.getPendingQuantity() != null ? order.getPendingQuantity() : order.getQuantity() - alreadyFulfilled;
            if (pendingBefore <= 0) {
                continue;
            }

            int allocate = Math.min(availableStock, pendingBefore);
            if (allocate > 0) {
                try {
                    inventoryClient.adjustStock(productId, -allocate);
                    availableStock -= allocate;
                } catch (RuntimeException ex) {
                    allocate = 0;
                }
            }
            int remainingPending = pendingBefore - allocate;
            order.setFulfilledQuantity(alreadyFulfilled + allocate);
            order.setPendingQuantity(remainingPending);
            order.setEstimatedFulfillmentDays(remainingPending > 0 && capacity > 0 ? (int) Math.ceil((double) remainingPending / capacity) : 0);
            order.setStatus(remainingPending > 0 ? "PARTIALLY_FULFILLED" : "APPROVED");
            orderRepository.save(order);
        }
    }
}

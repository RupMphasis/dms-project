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
            if (!"PENDING_APPROVAL".equalsIgnoreCase(order.getStatus())) {
                boolean retry = false;
                try {
                    inventoryClient.adjustStock(order.getProductId(), -order.getQuantity());
                } catch (RuntimeException ex) {
                    if (ex.getMessage() != null && ex.getMessage().contains("Inventory item not found")) {
                        ProductResponse product = productClient.getProduct(order.getProductId());
                        inventoryClient.createInventory(order.getProductId(), product.getStock() != null ? product.getStock() : 0, "DEFAULT");
                        retry = true;
                    } else {
                        throw new OrderBadRequestException("Unable to approve order: " + ex.getMessage(), ex);
                    }
                }

                if (retry) {
                    try {
                        inventoryClient.adjustStock(order.getProductId(), -order.getQuantity());
                    } catch (RuntimeException ex) {
                        throw new OrderBadRequestException("Unable to approve order: " + ex.getMessage(), ex);
                    }
                }
            }
        }

        if (wasApproved && willCancel) {
            inventoryClient.adjustStock(order.getProductId(), order.getQuantity());
        }

        order.setStatus(status);
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
}

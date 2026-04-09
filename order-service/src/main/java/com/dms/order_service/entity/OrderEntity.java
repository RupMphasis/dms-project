package com.dms.order_service.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false)
    private Long distributorId;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false, length = 150)
    private String customerName;

    @Column(nullable = false, length = 20)
    private String customerPhone;

    @Column(nullable = false, length = 500)
    private String shippingAddress;

    @Column(nullable = false, length = 100)
    private String shippingCity;

    @Column(nullable = false, length = 20)
    private String shippingPostalCode;

    @Column(nullable = false, length = 50)
    private String status;

    @JsonIgnore
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "order_details_id")
    private OrderDetails orderDetails;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Transient
    public String getCustomMessage() {
        return orderDetails != null ? orderDetails.getCustomMessage() : null;
    }

    public void setCustomMessage(String customMessage) {
        if (customMessage == null) {
            return;
        }
        if (orderDetails == null) {
            orderDetails = new OrderDetails();
        }
        orderDetails.setCustomMessage(customMessage);
    }

    @Transient
    public LocalDateTime getFulfillmentTime() {
        return orderDetails != null ? orderDetails.getFulfillmentTime() : null;
    }

    public void setFulfillmentTime(LocalDateTime fulfillmentTime) {
        if (fulfillmentTime == null) {
            return;
        }
        if (orderDetails == null) {
            orderDetails = new OrderDetails();
        }
        orderDetails.setFulfillmentTime(fulfillmentTime);
    }

    @Transient
    public String getAdminMessage() {
        return orderDetails != null ? orderDetails.getAdminMessage() : null;
    }

    public void setAdminMessage(String adminMessage) {
        if (adminMessage == null) {
            return;
        }
        if (orderDetails == null) {
            orderDetails = new OrderDetails();
        }
        orderDetails.setAdminMessage(adminMessage);
    }

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = "PENDING";
        }
    }
}

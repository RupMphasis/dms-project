package com.dms.user_service.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OrderDto {
    private Long id;
    private Long productId;
    private Long distributorId;
    private Integer quantity;
    private String customerName;
    private String customerPhone;
    private String shippingAddress;
    private String shippingCity;
    private String shippingPostalCode;
    private String status;
    private String customMessage;
    private LocalDateTime fulfillmentTime;
    private String adminMessage;
    private LocalDateTime createdAt;
}

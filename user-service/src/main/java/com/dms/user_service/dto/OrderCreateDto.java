package com.dms.user_service.dto;

import lombok.Data;

@Data
public class OrderCreateDto {
    private Long productId;
    private Long distributorId;
    private Integer quantity;
    private String customerName;
    private String customerPhone;
    private String shippingAddress;
    private String shippingCity;
    private String shippingPostalCode;
    private String status;
}

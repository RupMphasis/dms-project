package com.dms.user_service.dto;

import lombok.Data;

@Data
public class CartItemDto {
    private Long productId;
    private String name;
    private String vehicleType;
    private String size;
    private Double price;
    private Integer quantity;
    private Integer stock;
}

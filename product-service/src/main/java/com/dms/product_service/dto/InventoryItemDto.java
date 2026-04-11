package com.dms.product_service.dto;

import lombok.Data;

@Data
public class InventoryItemDto {
    private Long id;
    private Long productId;
    private Integer quantity;
    private String location;
}

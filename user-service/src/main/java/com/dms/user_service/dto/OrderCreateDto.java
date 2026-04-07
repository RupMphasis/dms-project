package com.dms.user_service.dto;

import lombok.Data;

@Data
public class OrderCreateDto {
    private Long productId;
    private Long distributorId;
    private Integer quantity;
}

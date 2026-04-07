package com.dms.user_service.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OrderDto {
    private Long id;
    private Long productId;
    private Long distributorId;
    private Integer quantity;
    private String status;
    private LocalDateTime createdAt;
}

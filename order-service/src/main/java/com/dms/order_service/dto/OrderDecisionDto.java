package com.dms.order_service.dto;

import lombok.Data;

@Data
public class OrderDecisionDto {
    private String status;
    private String adminMessage;
}

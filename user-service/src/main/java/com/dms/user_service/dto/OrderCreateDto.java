package com.dms.user_service.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

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
    private String customMessage;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime fulfillmentTime;
}

package com.dms.user_service.dto;

import lombok.Data;

@Data
public class ProductDto {
    private Long id;
    private String name;
    private String vehicleType;
    private String size;
    private String description;
    private Double price;
    private Integer stock;
    private Integer productionCapacityPerDay;
    private Boolean active;
} 

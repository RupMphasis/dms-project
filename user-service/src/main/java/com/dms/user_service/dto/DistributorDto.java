package com.dms.user_service.dto;

import lombok.Data;

@Data
public class DistributorDto {
    private Long id;
    private String name;
    private String email;
    private String city;
    private String contact;
}

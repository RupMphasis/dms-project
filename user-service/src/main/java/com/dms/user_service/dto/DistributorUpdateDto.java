package com.dms.user_service.dto;

import lombok.Data;

@Data
public class DistributorUpdateDto {
    private Long distributorId;
    private String name;
    private String email;
    private String city;
    private String contact;
}

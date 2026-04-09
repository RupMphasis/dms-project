package com.dms.user_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DistributorViewDto {
    private Long userId;
    private String username;
    private Long distributorId;
    private String name;
    private String email;
    private String city;
    private String contact;
}

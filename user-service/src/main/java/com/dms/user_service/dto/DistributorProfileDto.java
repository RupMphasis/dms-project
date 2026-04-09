package com.dms.user_service.dto;

import lombok.Data;

@Data
public class DistributorProfileDto {
    private Long distributorId;
    private String username;
    private String name;
    private String email;
    private String city;
    private String contact;
    private String oldPassword;
    private String newPassword;
    private String confirmNewPassword;
}

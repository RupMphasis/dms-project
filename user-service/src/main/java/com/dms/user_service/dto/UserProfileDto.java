package com.dms.user_service.dto;

import lombok.Data;

@Data
public class UserProfileDto {
    private String username;
    private String fullName;
    private String email;
    private String oldPassword;
    private String newPassword;
    private String confirmNewPassword;
}

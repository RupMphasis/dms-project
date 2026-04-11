package com.dms.user_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponse {
    private Long userId;
    private String username;
    private String role;
    private String fullName;
    private String email;
    private String token;
    private String message;
}
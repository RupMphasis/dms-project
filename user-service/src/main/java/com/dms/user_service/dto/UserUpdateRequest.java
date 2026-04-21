package com.dms.user_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserUpdateRequest {

    @Size(min = 3, max = 100, message = "Username must be 3 to 100 characters")
    private String username;

    private String fullName;

    @Email(message = "Please provide a valid email")
    private String email;

    private String oldPassword;

    @Size(min = 6, message = "Password must be at least 6 characters")
    private String newPassword;

    private String confirmPassword;
}

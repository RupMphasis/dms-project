package com.dms.user_service.dto;

import com.dms.user_service.entity.User;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 100, message = "Username must be 3 to 100 characters")
    private String username;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    @NotBlank(message = "Confirm password is required")
    private String confirmPassword;

    @NotNull(message = "Role is required — must be ADMIN or DISTRIBUTOR")
    private User.Role role;

    @NotBlank(message = "Full name is required")
    private String fullName;

    @Email(message = "Please provide a valid email address")
    private String email;
}
package com.dms.user_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DistributorRegisterRequest {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 100, message = "Username must be 3 to 100 characters")
    private String username;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    @NotBlank(message = "Confirm password is required")
    private String confirmPassword;

    @NotBlank(message = "Distributor name is required")
    private String distributorName;

    @NotBlank(message = "Distributor email is required")
    @Email(message = "Please provide a valid distributor email address")
    private String distributorEmail;

    @NotBlank(message = "Distributor city is required")
    private String distributorCity;

    @NotBlank(message = "Distributor contact is required")
    private String distributorContact;
}

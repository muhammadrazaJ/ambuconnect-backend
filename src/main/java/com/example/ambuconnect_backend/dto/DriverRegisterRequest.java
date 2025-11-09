package com.example.ambuconnect_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DriverRegisterRequest {

    @NotNull(message = "User ID is required")
    private Long userId;

    @NotBlank(message = "License number is required")
    @Size(min = 1, message = "License number cannot be empty")
    private String licenseNumber;

    @NotBlank(message = "CNIC is required")
    @Pattern(regexp = "^\\d{13}$", message = "CNIC must be exactly 13 digits")
    private String cnic;
}

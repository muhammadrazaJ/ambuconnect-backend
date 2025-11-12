package com.example.ambuconnect_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UpdateDriverStatusRequest {

    @NotBlank(message = "Status is required")
    @Pattern(regexp = "^(Online|Offline)$", message = "Status must be either 'Online' or 'Offline'")
    private String status;
}
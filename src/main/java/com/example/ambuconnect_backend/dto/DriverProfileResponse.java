package com.example.ambuconnect_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverProfileResponse {
    private boolean success;
    private String message;
    private String email;
    private String name;
    private String phone;
    private String role;
    private String cnic;
    private String licenseNumber;
}
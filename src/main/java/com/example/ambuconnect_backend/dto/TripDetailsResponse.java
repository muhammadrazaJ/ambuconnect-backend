package com.example.ambuconnect_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TripDetailsResponse {
    
    // Trip Info
    private Long tripId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal fare;
    private String status;
    
    // Booking Info
    private LocationInfo pickupLocation;
    private LocationInfo dropLocation;
    
    // User Info (Patient)
    private UserInfo user;
    
    // Driver Info
    private DriverInfo driver;
    
    // Payment Info
    private PaymentInfo payment;
    
    // Nested DTOs
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LocationInfo {
        private String address;
        private Double lat;
        private Double lng;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserInfo {
        private String name;
        private String phone;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DriverInfo {
        private String driverName;
        private String driverPhone;
        private String ambulanceNumber;
        private String ambulanceType;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PaymentInfo {
        private BigDecimal amount;
        private String method;
        private String status;
    }
}

package com.example.ambuconnect_backend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BookingRequestDTO {
    
    @NotNull(message = "Pickup location ID is required")
    private Long pickup_location_id;
    
    @NotNull(message = "Drop location ID is required")
    private Long drop_location_id;

    public Long getPickup_location_id() {
        return pickup_location_id;
    }

    public Long getDrop_location_id() {
        return drop_location_id;
    }
}

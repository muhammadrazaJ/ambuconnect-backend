package com.example.ambuconnect_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SaveLocationResponse {
    private Long location_id;
    private String message;
}

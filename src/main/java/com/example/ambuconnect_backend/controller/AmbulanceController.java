package com.example.ambuconnect_backend.controller;

import com.example.ambuconnect_backend.dto.AmbulanceRequest;
import com.example.ambuconnect_backend.dto.AmbulanceResponse;
import com.example.ambuconnect_backend.dto.AmbulanceTypeResponse;
import com.example.ambuconnect_backend.dto.DriverAmbulanceResponse;
import com.example.ambuconnect_backend.service.AmbulanceService;
import com.example.ambuconnect_backend.service.AmbulanceTypeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ambulance")
@RequiredArgsConstructor
@CrossOrigin
public class AmbulanceController {

    private final AmbulanceTypeService ambulanceTypeService;
    private final AmbulanceService ambulanceService;

    @GetMapping("/types")
    public ResponseEntity<List<AmbulanceTypeResponse>> getAmbulanceTypes() {
        List<AmbulanceTypeResponse> types = ambulanceTypeService.getAllAmbulanceTypes();
        return ResponseEntity.ok(types);
    }

    @PostMapping
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<AmbulanceResponse> addAmbulance(@Valid @RequestBody AmbulanceRequest request) {
        AmbulanceResponse response = ambulanceService.addAmbulance(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<List<DriverAmbulanceResponse>> getDriverAmbulances() {
        return ResponseEntity.ok(ambulanceService.getDriverAmbulances());
    }
}

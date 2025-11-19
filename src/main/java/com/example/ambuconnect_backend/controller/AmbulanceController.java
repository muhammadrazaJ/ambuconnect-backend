package com.example.ambuconnect_backend.controller;

import com.example.ambuconnect_backend.dto.*;
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

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<AmbulanceResponse> updateAmbulance(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAmbulanceRequest request
    ) {
        AmbulanceResponse response = ambulanceService.updateAmbulance(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<ApiResponse> deleteAmbulance(@PathVariable Long id) {
        ApiResponse response = ambulanceService.deleteAmbulance(id);
        return ResponseEntity.ok(response);
    }

}

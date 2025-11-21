package com.example.ambuconnect_backend.controller;

import com.example.ambuconnect_backend.dto.SaveLocationRequest;
import com.example.ambuconnect_backend.dto.SaveLocationResponse;
import com.example.ambuconnect_backend.service.PatientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/location")
@RequiredArgsConstructor
@CrossOrigin
public class LocationController {

    private final PatientService patientService;

    @PostMapping
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<SaveLocationResponse> saveLocation(@Valid @RequestBody SaveLocationRequest request) {
        SaveLocationResponse response = patientService.saveLocation(request);
        return ResponseEntity.ok(response);
    }
}

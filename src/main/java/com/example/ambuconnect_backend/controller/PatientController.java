package com.example.ambuconnect_backend.controller;

import com.example.ambuconnect_backend.dto.PatientProfileResponse;
import com.example.ambuconnect_backend.service.PatientService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/patient")
@RequiredArgsConstructor
@CrossOrigin
public class PatientController {

    private final PatientService patientService;

    @GetMapping("/profile")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<PatientProfileResponse> getPatientProfile() {
        PatientProfileResponse profile = patientService.getPatientProfile();
        return ResponseEntity.ok(profile);
    }
}

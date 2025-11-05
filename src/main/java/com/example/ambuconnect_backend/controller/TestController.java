package com.example.ambuconnect_backend.controller;

import com.example.ambuconnect_backend.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping("/api/test")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse> secureTest() {
        return ResponseEntity.ok(new ApiResponse(true, "Secure endpoint success ✅"));
    }

    @GetMapping("/api/test/patient")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<ApiResponse> userOnly() {
        return ResponseEntity.ok(new ApiResponse(true, "PATIENT endpoint accessed ✅"));
    }

    @GetMapping("/api/test/driver")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<ApiResponse> driverOnly() {
        return ResponseEntity.ok(new ApiResponse(true, "DRIVER endpoint accessed ✅"));
    }

    @GetMapping("/api/test/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> adminOnly() {
        return ResponseEntity.ok(new ApiResponse(true, "ADMIN endpoint accessed ✅"));
    }
}
package com.example.ambuconnect_backend.controller;

import com.example.ambuconnect_backend.dto.DriverRegisterRequest;
import com.example.ambuconnect_backend.dto.DriverRegisterResponse;
import com.example.ambuconnect_backend.service.DriverService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/driver")
@RequiredArgsConstructor
@CrossOrigin
public class DriverController {

    private final DriverService driverService;

    @PostMapping("/register")
    public ResponseEntity<DriverRegisterResponse> registerDriver(
            @Valid @RequestBody DriverRegisterRequest request) {
        DriverRegisterResponse response = driverService.registerDriver(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}

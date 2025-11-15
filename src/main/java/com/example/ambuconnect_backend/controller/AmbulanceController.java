package com.example.ambuconnect_backend.controller;

import com.example.ambuconnect_backend.dto.AmbulanceTypeResponse;
import com.example.ambuconnect_backend.service.AmbulanceTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ambulance")
@RequiredArgsConstructor
@CrossOrigin
public class AmbulanceController {

    private final AmbulanceTypeService ambulanceTypeService;

    @GetMapping("/types")
    public ResponseEntity<List<AmbulanceTypeResponse>> getAmbulanceTypes() {
        List<AmbulanceTypeResponse> types = ambulanceTypeService.getAllAmbulanceTypes();
        return ResponseEntity.ok(types);
    }
}

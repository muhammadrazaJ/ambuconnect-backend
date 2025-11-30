package com.example.ambuconnect_backend.controller;

import com.example.ambuconnect_backend.service.AmbulanceTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/cache")
@RequiredArgsConstructor
public class CacheController {

    private final AmbulanceTypeService ambulanceTypeService;

    @DeleteMapping("/ambulance-types")
    public ResponseEntity<Map<String, String>> clearAmbulanceTypesCache() {
        ambulanceTypeService.clearAmbulanceTypesCache();

        Map<String, String> response = new HashMap<>();
        response.put("message", "Ambulance types cache cleared successfully");
        response.put("status", "success");

        return ResponseEntity.ok(response);
    }
}
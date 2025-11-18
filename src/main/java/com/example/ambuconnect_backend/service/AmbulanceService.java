package com.example.ambuconnect_backend.service;

import com.example.ambuconnect_backend.dto.AmbulanceRequest;
import com.example.ambuconnect_backend.dto.AmbulanceResponse;
import com.example.ambuconnect_backend.model.*;
import com.example.ambuconnect_backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AmbulanceService {

    private final AmbulanceRepository ambulanceRepository;
    private final AmbulanceAvailabilityRepository ambulanceAvailabilityRepository;
    private final AmbulanceTypeRepository ambulanceTypeRepository;
    private final DriverProfileRepository driverProfileRepository;
    private final UserRepository userRepository;

    @Transactional
    public AmbulanceResponse addAmbulance(AmbulanceRequest request) {
        // 1. Get driver_profile_id from authenticated user
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"
                ));

        // Verify user has DRIVER role
        if (user.getRole() == null || !user.getRole().getName().equals("DRIVER")) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Access denied. Driver role required."
            );
        }

        DriverProfile driverProfile = driverProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Driver profile not found"
                ));

        // 2. Validate ambulance_type_id exists
        AmbulanceType ambulanceType = ambulanceTypeRepository.findById(request.getAmbulanceTypeId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Ambulance type with ID " + request.getAmbulanceTypeId() + " not found"
                ));

        // Check if vehicle_number already exists
        if (ambulanceRepository.existsByVehicleNumber(request.getVehicleNumber())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Vehicle number already exists"
            );
        }

        // 3. Insert new ambulance into AMBULANCES table
        Ambulance ambulance = Ambulance.builder()
                .driverProfileId(driverProfile.getId())
                .ambulanceTypeId(request.getAmbulanceTypeId())
                .vehicleNumber(request.getVehicleNumber().trim())
                .model(request.getModel().trim())
                .build();

        Ambulance savedAmbulance = ambulanceRepository.save(ambulance);

        // 4. Create default AMBULANCE_AVAILABILITY record with is_available = false
        AmbulanceAvailability availability = AmbulanceAvailability.builder()
                .ambulanceId(savedAmbulance.getId())
                .isAvailable(false)
                .build();

        ambulanceAvailabilityRepository.save(availability);

        // 5. Return the inserted ambulance
        return AmbulanceResponse.builder()
                .success(true)
                .message("Ambulance successfully added")
                .id(savedAmbulance.getId())
                .vehicleNumber(savedAmbulance.getVehicleNumber())
                .model(savedAmbulance.getModel())
                .ambulanceType(ambulanceType.getType())
                .availability(false)
                .build();
    }
}

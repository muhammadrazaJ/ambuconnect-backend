package com.example.ambuconnect_backend.service;

import com.example.ambuconnect_backend.dto.*;
import com.example.ambuconnect_backend.model.*;
import com.example.ambuconnect_backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    @Transactional(readOnly = true)
    public List<DriverAmbulanceResponse> getDriverAmbulances() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"
                ));

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

        List<Ambulance> ambulances = ambulanceRepository.findAllByDriverProfileId(driverProfile.getId());
        if (ambulances.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> ambulanceIds = ambulances.stream()
                .map(Ambulance::getId)
                .toList();

        Map<Long, Boolean> availabilityMap = ambulanceAvailabilityRepository.findByAmbulanceIdIn(ambulanceIds)
                .stream()
                .collect(Collectors.toMap(AmbulanceAvailability::getAmbulanceId, AmbulanceAvailability::getIsAvailable));

        return ambulances.stream()
                .map(ambulance -> DriverAmbulanceResponse.builder()
                        .id(ambulance.getId())
                        .vehicleNumber(ambulance.getVehicleNumber())
                        .model(ambulance.getModel())
                        .ambulanceType(
                                ambulance.getAmbulanceType() != null ? ambulance.getAmbulanceType().getType() : null
                        )
                        .isAvailable(availabilityMap.getOrDefault(ambulance.getId(), Boolean.FALSE))
                        .build())
                .toList();
    }

    @Transactional
    public AmbulanceResponse updateAmbulance(Long ambulanceId, UpdateAmbulanceRequest request) {

        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // Only DRIVER can update his own ambulance
        if (user.getRole() == null || !user.getRole().getName().equalsIgnoreCase("DRIVER")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied. Driver role required");
        }

        DriverProfile driverProfile = driverProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Driver profile not found"));

        Ambulance ambulance = ambulanceRepository.findById(ambulanceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ambulance not found"));

        // Ensure the ambulance belongs to this driver
        if (!ambulance.getDriverProfileId().equals(driverProfile.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You are not allowed to update this ambulance.");
        }

        // Normalize vehicle number
        String normalizedVehicleNumber = request.getVehicleNumber().trim();

        // Ensure vehicle number is unique if changed
        if (!ambulance.getVehicleNumber().equals(normalizedVehicleNumber)
                && ambulanceRepository.existsByVehicleNumber(normalizedVehicleNumber)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Vehicle number already exists");
        }

        AmbulanceType ambulanceType = ambulanceTypeRepository.findById(request.getAmbulanceTypeId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ambulance type not found"));

        // Update fields
        ambulance.setVehicleNumber(normalizedVehicleNumber);
        ambulance.setModel(request.getModel());
        ambulance.setAmbulanceTypeId(request.getAmbulanceTypeId());

        Ambulance updated = ambulanceRepository.save(ambulance);

        return AmbulanceResponse.builder()
                .success(true)
                .message("Ambulance updated successfully")
                .id(updated.getId())
                .vehicleNumber(updated.getVehicleNumber())
                .model(updated.getModel())
                .ambulanceType(ambulanceType.getType())
                .build();
    }

    @Transactional
    public ApiResponse deleteAmbulance(Long ambulanceId) {
        // Fetch authenticated user's email
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        // Validate user exists
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // Ensure user has DRIVER role
        if (user.getRole() == null || !user.getRole().getName().equalsIgnoreCase("DRIVER")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied. Driver role required.");
        }

        // Resolve driver profile
        DriverProfile driverProfile = driverProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Driver profile not found"));

        // Load ambulance
        Ambulance ambulance = ambulanceRepository.findById(ambulanceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ambulance not found"));

        // Verify ownership
        if (!ambulance.getDriverProfileId().equals(driverProfile.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to delete this ambulance.");
        }

        // Delete related availability if exists
        ambulanceAvailabilityRepository.findByAmbulanceId(ambulance.getId())
                .ifPresent(ambulanceAvailabilityRepository::delete);

        // Delete ambulance
        ambulanceRepository.delete(ambulance);

        return new ApiResponse(true, "Ambulance removed successfully.");
    }

    @Transactional
    public AmbulanceAvailabilityResponse updateAmbulanceAvailability(Long ambulanceId, UpdateAmbulanceAvailabilityRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Ambulance ambulance = ambulanceRepository.findById(ambulanceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ambulance not found"));


        if (user.getRole() == null || !user.getRole().getName().equalsIgnoreCase("DRIVER")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied. Driver role required.");
        }

        DriverProfile driverProfile = driverProfileRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Driver profile not found"));

        if (!ambulance.getDriverProfileId().equals(driverProfile.getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to update this ambulance availability.");
        }
//        String roleName = user.getRole() != null ? user.getRole().getName() : null;
//        boolean isAdmin = roleName != null && roleName.equalsIgnoreCase("ADMIN");
//        boolean isDriver = roleName != null && roleName.equalsIgnoreCase("DRIVER");
//
//        if (!isAdmin) {
//            if (!isDriver) {
//                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
//            }
//            DriverProfile driverProfile = driverProfileRepository.findByUserId(user.getId())
//                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Driver profile not found"));
//
//        }

        AmbulanceAvailability availability = ambulanceAvailabilityRepository.findByAmbulanceId(ambulance.getId())
                .orElseGet(() -> AmbulanceAvailability.builder()
                        .ambulanceId(ambulance.getId())
                        .build());

        availability.setIsAvailable(request.getIsAvailable());
        AmbulanceAvailability saved = ambulanceAvailabilityRepository.save(availability);

        return AmbulanceAvailabilityResponse.builder()
                .success(true)
                .message("Ambulance availability updated")
                .id(saved.getId())
                .ambulanceId(saved.getAmbulanceId())
                .isAvailable(saved.getIsAvailable())
                .updatedAt(saved.getUpdatedAt())
                .build();

    }
}

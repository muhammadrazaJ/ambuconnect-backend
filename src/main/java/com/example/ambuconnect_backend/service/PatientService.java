package com.example.ambuconnect_backend.service;

import com.example.ambuconnect_backend.dto.PatientProfileResponse;
import com.example.ambuconnect_backend.dto.UpdatePatientProfileRequest;
import com.example.ambuconnect_backend.model.User;
import com.example.ambuconnect_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class PatientService {

    private final UserRepository userRepository;

    public PatientProfileResponse getPatientProfile() {
        // Get email from SecurityContext (set by JwtAuthFilter)
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        
        // Find user by email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, 
                        "Patient profile not found"
                ));

        // Verify user has PATIENT role
        if (user.getRole() == null || !user.getRole().getName().equalsIgnoreCase("PATIENT")) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, 
                    "Patient profile not found"
            );
        }

        // Return patient profile
        return new PatientProfileResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole().getName()
        );
    }

    public PatientProfileResponse updatePatientProfile(UpdatePatientProfileRequest request) {
        // Get email from SecurityContext (set by JwtAuthFilter)
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        // Find user by email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Patient profile not found"
                ));

        // Verify user has PATIENT role
        if (user.getRole() == null || !user.getRole().getName().equalsIgnoreCase("PATIENT")) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Access denied"
            );
        }

        // Validate name
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Name cannot be empty"
            );
        }

        // Validate phone format
        String phone = request.getPhone().trim();
        if (phone.length() < 10 || phone.length() > 15) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Phone number must be between 10 and 15 characters"
            );
        }

        // Optional: More strict phone format validation
        // Remove common formatting characters for validation
        String phoneDigits = phone.replaceAll("[^0-9+]", "");
        if (phoneDigits.length() < 10) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Phone number format is invalid"
            );
        }

        // Check phone uniqueness (but allow if it's the current user's phone)
        userRepository.findByPhone(phone).ifPresent(existingUser -> {
            if (!existingUser.getId().equals(user.getId())) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Phone number already registered"
                );
            }
        });

        // Update only allowed fields
        user.setName(request.getName().trim());
        user.setPhone(phone);

        // Save updated user
        User updatedUser = userRepository.save(user);

        // Return updated patient profile
        return new PatientProfileResponse(
                updatedUser.getId(),
                updatedUser.getName(),
                updatedUser.getEmail(),
                updatedUser.getPhone(),
                updatedUser.getRole().getName()
        );
    }
}

package com.example.ambuconnect_backend.service;

import com.example.ambuconnect_backend.dto.PatientProfileResponse;
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
}

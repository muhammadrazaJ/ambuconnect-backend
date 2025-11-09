package com.example.ambuconnect_backend.repository;

import com.example.ambuconnect_backend.model.DriverProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DriverProfileRepository extends JpaRepository<DriverProfile, Long> {
    Optional<DriverProfile> findByUserId(Long userId);
    Optional<DriverProfile> findByCnic(String cnic);
    Optional<DriverProfile> findByLicenseNumber(String licenseNumber);
    boolean existsByUserId(Long userId);
    boolean existsByCnic(String cnic);
    boolean existsByLicenseNumber(String licenseNumber);
}

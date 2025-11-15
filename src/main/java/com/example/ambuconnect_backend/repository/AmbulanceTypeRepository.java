package com.example.ambuconnect_backend.repository;

import com.example.ambuconnect_backend.model.AmbulanceType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AmbulanceTypeRepository extends JpaRepository<AmbulanceType, Integer> {
    Optional<AmbulanceType> findByType(String type);
}

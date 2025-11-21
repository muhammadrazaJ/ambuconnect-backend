package com.example.ambuconnect_backend.repository;

import com.example.ambuconnect_backend.model.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LocationRepository extends JpaRepository<Location, Long> {
    Optional<Location> findByLatitudeAndLongitude(Double latitude, Double longitude);
}

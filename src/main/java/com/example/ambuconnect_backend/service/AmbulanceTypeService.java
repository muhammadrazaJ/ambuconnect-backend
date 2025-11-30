package com.example.ambuconnect_backend.service;

import com.example.ambuconnect_backend.dto.AmbulanceTypeResponse;
import com.example.ambuconnect_backend.model.AmbulanceType;
import com.example.ambuconnect_backend.repository.AmbulanceTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AmbulanceTypeService {

    private final AmbulanceTypeRepository ambulanceTypeRepository;

    @Cacheable(value = "ambulanceTypes", key = "'all'")
    public List<AmbulanceTypeResponse> getAllAmbulanceTypes() {
        log.info("Fetching ambulance types from database - Cache MISS");

        try {
            List<AmbulanceType> types = ambulanceTypeRepository.findAll();

            if (types.isEmpty()) {
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No ambulance types found in the database"
                );
            }

            return types.stream()
                    .map(type -> AmbulanceTypeResponse.builder()
                            .id(type.getId())
                            .type(type.getType())
                            .build())
                    .collect(Collectors.toList());
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "An error occurred while retrieving ambulance types: " + e.getMessage()
            );
        }
    }

    /**
     * Clear the ambulance types cache
     * Call this method when ambulance types are added, updated, or deleted
     */
    @CacheEvict(value = "ambulanceTypes", allEntries = true)
    public void clearAmbulanceTypesCache() {
        log.info("Ambulance types cache cleared successfully");
    }
}
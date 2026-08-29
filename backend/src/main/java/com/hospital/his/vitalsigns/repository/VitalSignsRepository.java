package com.hospital.his.vitalsigns.repository;

import com.hospital.his.vitalsigns.entity.VitalSigns;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VitalSignsRepository
        extends JpaRepository<VitalSigns, Long> {

    boolean existsByAppointment_Id(
            Long appointmentId
    );

    Optional<VitalSigns> findByAppointment_Id(
            Long appointmentId
    );

    boolean existsByEmergencyReception_Id(
            Long emergencyReceptionId
    );

    Optional<VitalSigns> findByEmergencyReception_Id(
            Long emergencyReceptionId
    );
}
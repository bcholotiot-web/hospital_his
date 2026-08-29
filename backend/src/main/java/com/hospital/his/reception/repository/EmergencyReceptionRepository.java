package com.hospital.his.reception.repository;

import com.hospital.his.reception.entity.EmergencyReception;
import com.hospital.his.reception.entity.EmergencyReceptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface EmergencyReceptionRepository
        extends JpaRepository<EmergencyReception, Long> {

    boolean existsByPatientDpiAndStatusAndActiveTrue(
            String patientDpi,
            EmergencyReceptionStatus status
    );

    List<EmergencyReception>
    findByStatusAndActiveTrueOrderByArrivalTimeAsc(
            EmergencyReceptionStatus status
    );

    List<EmergencyReception>
    findByStatusInAndActiveTrueOrderByArrivalTimeAsc(
            Collection<EmergencyReceptionStatus> statuses
    );
}
package com.hospital.his.medicalconsultation.repository;

import com.hospital.his.medicalconsultation.entity.MedicalConsultation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MedicalConsultationRepository
        extends JpaRepository<MedicalConsultation, Long> {

    Optional<MedicalConsultation>
    findByAppointment_Id(
            Long appointmentId
    );

    Optional<MedicalConsultation>
    findByAppointment_IdAndDoctor_Username(
            Long appointmentId,
            String doctorUsername
    );

    boolean existsByAppointment_Id(
            Long appointmentId
    );
}
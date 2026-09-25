package com.hospital.his.medicalconsultation.repository;

import com.hospital.his.medicalconsultation.entity.MedicalConsultation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MedicalConsultationRepository
        extends JpaRepository<MedicalConsultation, Long> {

    /**
     * Busca la consulta asociada con una cita.
     */
    Optional<MedicalConsultation>
    findByAppointment_Id(
            Long appointmentId
    );

    /**
     * Busca la consulta asociada con una cita,
     * validando además al médico.
     */
    Optional<MedicalConsultation>
    findByAppointment_IdAndDoctor_Username(
            Long appointmentId,
            String doctorUsername
    );

    /**
     * CU-12:
     * Busca directamente una consulta por ID
     * y por el médico autenticado.
     */
    Optional<MedicalConsultation>
    findByIdAndDoctor_Username(
            Long consultationId,
            String doctorUsername
    );

    /**
     * Comprueba si una cita ya tiene una consulta.
     */
    boolean existsByAppointment_Id(
            Long appointmentId
    );
}
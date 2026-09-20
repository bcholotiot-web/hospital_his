package com.hospital.his.prescriptions.repository;

import com.hospital.his.prescriptions.entity.Prescription;
import com.hospital.his.prescriptions.entity.PrescriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PrescriptionRepository
        extends JpaRepository<Prescription, Long> {

    /**
     * Recupera una receta activa aplicando un bloqueo
     * de escritura durante la confirmación del despacho.
     *
     * Esto evita que dos usuarios de Farmacia procesen
     * simultáneamente la misma receta.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT prescription
        FROM Prescription prescription
        WHERE prescription.id = :prescriptionId
          AND prescription.active = true
        """)
    Optional<Prescription> findForDispensation(
            @Param("prescriptionId")
            Long prescriptionId
    );

    /**
     * Evita crear más de una receta activa
     * para la misma consulta médica.
     */
    boolean existsByMedicalConsultation_IdAndActiveTrue(
            Long medicalConsultationId
    );

    /**
     * Busca una receta activa por consulta.
     */
    Optional<Prescription>
    findByMedicalConsultation_IdAndActiveTrue(
            Long medicalConsultationId
    );

    /**
     * Busca una receta activa por ID.
     */
    Optional<Prescription>
    findByIdAndActiveTrue(
            Long prescriptionId
    );

    /**
     * Busca por número visible.
     */
    Optional<Prescription>
    findByPrescriptionNumberIgnoreCaseAndActiveTrue(
            String prescriptionNumber
    );

    /**
     * Lista las recetas activas emitidas por
     * el médico autenticado.
     */
    List<Prescription>
    findByDoctor_UsernameAndActiveTrueOrderByIssuedAtDesc(
            String doctorUsername
    );

    /**
     * Lista recetas activas de un paciente.
     */
    List<Prescription>
    findByPatient_DpiAndActiveTrueOrderByIssuedAtDesc(
            String patientDpi
    );

    /**
     * Permite consultar recetas por estado.
     */
    List<Prescription>
    findByStatusAndActiveTrueOrderByIssuedAtDesc(
            PrescriptionStatus status
    );




}
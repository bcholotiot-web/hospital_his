package com.hospital.his.prescriptions.repository;

import com.hospital.his.prescriptions.entity.PrescriptionItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PrescriptionItemRepository
        extends JpaRepository<PrescriptionItem, Long> {

    /**
     * Obtiene los medicamentos de una receta.
     */
    List<PrescriptionItem>
    findByPrescription_IdAndActiveTrueOrderByIdAsc(
            Long prescriptionId
    );

    /**
     * Obtiene un medicamento que pertenece
     * a una receta concreta.
     */
    Optional<PrescriptionItem>
    findByIdAndPrescription_IdAndActiveTrue(
            Long prescriptionItemId,
            Long prescriptionId
    );

    /**
     * Comprueba si un medicamento ya se encuentra
     * agregado a la receta.
     */
    boolean existsByPrescription_IdAndMedication_IdAndActiveTrue(
            Long prescriptionId,
            Long medicationId
    );
}
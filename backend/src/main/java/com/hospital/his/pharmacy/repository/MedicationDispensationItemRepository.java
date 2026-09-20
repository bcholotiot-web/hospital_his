package com.hospital.his.pharmacy.repository;

import com.hospital.his.pharmacy.entity.MedicationDispensationItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MedicationDispensationItemRepository
        extends JpaRepository<MedicationDispensationItem, Long> {

    /**
     * Obtiene todos los medicamentos entregados
     * en un despacho.
     */
    List<MedicationDispensationItem>
    findByDispensation_IdOrderByIdAsc(
            Long dispensationId
    );

    /**
     * Obtiene el historial de entregas de un ítem
     * específico de una receta.
     */
    List<MedicationDispensationItem>
    findByPrescriptionItem_IdOrderByIdAsc(
            Long prescriptionItemId
    );
}
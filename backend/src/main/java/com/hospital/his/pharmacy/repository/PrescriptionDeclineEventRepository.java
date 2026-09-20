package com.hospital.his.pharmacy.repository;

import com.hospital.his.pharmacy.entity.PrescriptionDeclineEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PrescriptionDeclineEventRepository
        extends JpaRepository<
        PrescriptionDeclineEvent,
        Long
        > {

    /**
     * Obtiene el historial de decisiones registradas
     * para una receta.
     */
    List<PrescriptionDeclineEvent>
    findByPrescription_IdOrderByCreatedAtDesc(
            Long prescriptionId
    );

    /**
     * Obtiene los eventos registrados por un usuario
     * de Farmacia.
     */
    List<PrescriptionDeclineEvent>
    findByPharmacyUsernameOrderByCreatedAtDesc(
            String pharmacyUsername
    );
}
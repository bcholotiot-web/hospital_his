package com.hospital.his.pharmacy.repository;

import com.hospital.his.pharmacy.entity.MedicationDispensation;
import com.hospital.his.pharmacy.entity.DispensationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MedicationDispensationRepository
        extends JpaRepository<MedicationDispensation, Long> {

    /**
     * Busca una solicitud procesada anteriormente.
     */
    Optional<MedicationDispensation>
    findByIdempotencyKey(
            UUID idempotencyKey
    );

    /**
     * Recupera un comprobante por número
     * de transacción.
     */
    Optional<MedicationDispensation>
    findByTransactionNumber(
            String transactionNumber
    );

    /**
     * Lista los despachos asociados con una receta.
     */
    List<MedicationDispensation>
    findByPrescription_IdOrderByCreatedAtDesc(
            Long prescriptionId
    );

    /**
     * Lista los despachos realizados por el usuario
     * de Farmacia autenticado.
     */
    List<MedicationDispensation>
    findByPharmacyUsernameOrderByCreatedAtDesc(
            String pharmacyUsername
    );

    /**
     * Verifica si ya existe un despacho confirmado
     * para la receta.
     *
     * Una receta parcialmente despachada puede tener
     * más de un despacho confirmado.
     */
    boolean existsByPrescription_IdAndStatus(
            Long prescriptionId,
            DispensationStatus status
    );
}
package com.hospital.his.pharmacy.repository;

import com.hospital.his.pharmacy.entity.PharmacyInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface PharmacyInventoryRepository
        extends JpaRepository<PharmacyInventory, Long> {

    /**
     * Recupera y bloquea temporalmente el inventario
     * mientras se realiza el despacho.
     *
     * PESSIMISTIC_WRITE evita que otra transacción
     * modifique el mismo registro antes de finalizar.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT inventory
        FROM PharmacyInventory inventory
        JOIN FETCH inventory.medication medication
        JOIN FETCH inventory.branch branch
        WHERE branch.id = :branchId
          AND medication.id = :medicationId
          AND inventory.active = true
          AND medication.active = true
        """)
    Optional<PharmacyInventory>
    findForDispensation(
            @Param("branchId")
            Long branchId,

            @Param("medicationId")
            Long medicationId
    );

    /**
     * Lista el inventario activo de una sucursal.
     */
    List<PharmacyInventory>
    findByBranch_IdAndActiveTrueOrderByMedication_NameAsc(
            Long branchId
    );

    /**
     * Busca el inventario activo de un medicamento
     * en una sucursal determinada.
     */
    Optional<PharmacyInventory>
    findByBranch_IdAndMedication_IdAndActiveTrue(
            Long branchId,
            Long medicationId
    );

    /**
     * Busca un registro, activo o inactivo, para evitar
     * duplicar la combinación sucursal y medicamento.
     */
    Optional<PharmacyInventory>
    findByBranch_IdAndMedication_Id(
            Long branchId,
            Long medicationId
    );

    /**
     * Busca un registro de inventario activo por ID
     * y sucursal.
     *
     * Impide que una persona consulte inventarios
     * pertenecientes a otra sucursal.
     */
    Optional<PharmacyInventory>
    findByIdAndBranch_IdAndActiveTrue(
            Long inventoryId,
            Long branchId
    );

    /**
     * Busca un registro por ID y sucursal sin importar
     * su estado.
     *
     * Se utiliza para activar o desactivar.
     */
    Optional<PharmacyInventory>
    findByIdAndBranch_Id(
            Long inventoryId,
            Long branchId
    );
}
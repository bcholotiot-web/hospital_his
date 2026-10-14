package com.hospital.his.pharmacy.repository;

import com.hospital.his.pharmacy.entity.InventoryMovement;
import com.hospital.his.pharmacy.entity.InventoryMovementType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.time.LocalDateTime;
import java.util.List;

public interface InventoryMovementRepository
        extends JpaRepository<
        InventoryMovement,
        Long
        >,
        JpaSpecificationExecutor<
                InventoryMovement
                > {

    /**
     * Historial paginado de un registro
     * de inventario.
     */
    Page<InventoryMovement>
    findByInventory_IdOrderByCreatedAtDesc(
            Long inventoryId,
            Pageable pageable
    );

    /**
     * Movimientos de una sucursal dentro
     * de un rango de fechas.
     */
    List<InventoryMovement>
    findByBranch_IdAndCreatedAtBetweenOrderByCreatedAtAsc(
            Long branchId,
            LocalDateTime startDate,
            LocalDateTime endDate
    );

    /**
     * Movimientos por tipo y sucursal.
     */
    List<InventoryMovement>
    findByBranch_IdAndMovementTypeAndCreatedAtBetweenOrderByCreatedAtAsc(
            Long branchId,
            InventoryMovementType movementType,
            LocalDateTime startDate,
            LocalDateTime endDate
    );

    /**
     * Obtiene un movimiento con sus relaciones
     * necesarias para generar el detalle.
     */
    @Query("""
    SELECT movement
    FROM InventoryMovement movement
    JOIN FETCH movement.inventory inventory
    JOIN FETCH movement.branch branch
    JOIN FETCH movement.medication medication
    WHERE movement.id = :movementId
    """)
    Optional<InventoryMovement>
    findDetailById(
            @Param("movementId")
            Long movementId
    );

}
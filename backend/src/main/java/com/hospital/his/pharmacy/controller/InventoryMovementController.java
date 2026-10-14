package com.hospital.his.pharmacy.controller;

import com.hospital.his.pharmacy.dto.InventoryMonthlySummaryResponse;
import com.hospital.his.pharmacy.dto.InventoryMovementPageResponse;
import com.hospital.his.pharmacy.service.InventoryMovementService;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import com.hospital.his.pharmacy.dto.CreateInventoryMovementRequest;
import com.hospital.his.pharmacy.dto.InventoryMovementDetailResponse;
import com.hospital.his.pharmacy.dto.InventoryMovementPreviewResponse;

import jakarta.validation.Valid;

import java.math.BigDecimal;

import java.time.LocalDate;

/**
 * CU-14:
 * Consulta de bitácora y resumen mensual
 * de inventario.
 */
@RestController
@RequestMapping("/api/pharmacy/inventory-movements")
@PreAuthorize("hasAuthority('INVENTORY_MOVEMENT_READ')")
public class InventoryMovementController {

    private final InventoryMovementService
            inventoryMovementService;

    public InventoryMovementController(
            InventoryMovementService
                    inventoryMovementService
    ) {
        this.inventoryMovementService =
                inventoryMovementService;
    }

    /**
     * Lista movimientos de inventario aplicando
     * filtros y alcance de sucursal.
     *
     * FARMACIA utiliza automáticamente la sucursal
     * asignada a su cuenta.
     *
     * ADMIN debe enviar branchId.
     */
    @GetMapping
    public ResponseEntity<
            InventoryMovementPageResponse
            >
    getMovements(
            @RequestParam(
                    required = false
            )
            Long branchId,

            @RequestParam(
                    required = false
            )
            String search,

            @RequestParam(
                    required = false
            )
            String movementType,

            @RequestParam(
                    required = false
            )
            String referenceNumber,

            @RequestParam(
                    required = false
            )
            String createdBy,

            @RequestParam(
                    required = false
            )
            Boolean active,

            @RequestParam(
                    required = false
            )
            @DateTimeFormat(
                    iso =
                            DateTimeFormat.ISO.DATE
            )
            LocalDate startDate,

            @RequestParam(
                    required = false
            )
            @DateTimeFormat(
                    iso =
                            DateTimeFormat.ISO.DATE
            )
            LocalDate endDate,

            @RequestParam(
                    defaultValue = "0"
            )
            Integer page,

            @RequestParam(
                    defaultValue = "20"
            )
            Integer size,

            Authentication authentication
    ) {
        return ResponseEntity.ok(
                inventoryMovementService
                        .getMyBranchMovements(
                                branchId,
                                search,
                                movementType,
                                referenceNumber,
                                createdBy,
                                active,
                                startDate,
                                endDate,
                                page,
                                size,
                                authentication.getName()
                        )
        );
    }

    /**
     * Genera el resumen mensual de inventario.
     *
     * FARMACIA:
     * utiliza automáticamente su sucursal.
     *
     * ADMIN:
     * debe seleccionar una sucursal mediante branchId.
     *
     * Ejemplo:
     *
     * GET /api/pharmacy/inventory-movements/monthly-summary
     *     ?branchId=1
     *     &year=2026
     *     &month=10
     */
    @GetMapping("/monthly-summary")
    public ResponseEntity<
            InventoryMonthlySummaryResponse
            >
    getMonthlySummary(
            @RequestParam(
                    required = false
            )
            Long branchId,

            @RequestParam
            Integer year,

            @RequestParam
            Integer month,

            Authentication authentication
    ) {
        return ResponseEntity.ok(
                inventoryMovementService
                        .getMonthlySummary(
                                branchId,
                                year,
                                month,
                                authentication.getName()
                        )
        );
    }

    /**
     * Calcula el stock proyectado antes de registrar
     * un movimiento.
     */
    @PreAuthorize(
            "hasAuthority('INVENTORY_MOVEMENT_CREATE')"
    )
    @GetMapping("/preview")
    public ResponseEntity<
            InventoryMovementPreviewResponse
            >
    previewMovement(
            @RequestParam(
                    required = false
            )
            Long branchId,

            @RequestParam
            Long medicationId,

            @RequestParam
            String movementType,

            @RequestParam
            Integer quantity,

            @RequestParam(
                    required = false
            )
            BigDecimal unitCost,

            Authentication authentication
    ) {
        return ResponseEntity.ok(
                inventoryMovementService
                        .previewManualMovement(
                                branchId,
                                medicationId,
                                movementType,
                                quantity,
                                unitCost,
                                authentication.getName()
                        )
        );
    }

    /**
     * Obtiene el detalle de un movimiento.
     */
    @GetMapping("/{movementId}")
    public ResponseEntity<
            InventoryMovementDetailResponse
            >
    getMovementDetail(
            @PathVariable
            Long movementId,

            Authentication authentication
    ) {
        return ResponseEntity.ok(
                inventoryMovementService
                        .getMovementDetail(
                                movementId,
                                authentication.getName()
                        )
        );
    }

    /**
     * Activa o desactiva administrativamente
     * un movimiento histórico.
     *
     * No modifica el stock.
     */
    @PreAuthorize(
            "hasAuthority('INVENTORY_MOVEMENT_STATUS_UPDATE')"
    )
    @PatchMapping("/{movementId}/status")
    public ResponseEntity<
            InventoryMovementDetailResponse
            >
    changeMovementStatus(
            @PathVariable
            Long movementId,

            @RequestParam
            Boolean active,

            @RequestParam
            Long version,

            Authentication authentication
    ) {
        return ResponseEntity.ok(
                inventoryMovementService
                        .changeMovementStatus(
                                movementId,
                                active,
                                version,
                                authentication.getName()
                        )
        );
    }


    /**
     * Registra un movimiento manual y actualiza
     * el stock en la misma transacción.
     */
    @PreAuthorize(
            "hasAuthority('INVENTORY_MOVEMENT_CREATE')"
    )
    @PostMapping
    public ResponseEntity<
            InventoryMovementDetailResponse
            >
    createMovement(
            @Valid
            @RequestBody
            CreateInventoryMovementRequest request,

            Authentication authentication
    ) {
        return ResponseEntity.ok(
                inventoryMovementService
                        .createManualMovement(
                                request,
                                authentication.getName()
                        )
        );
    }
}
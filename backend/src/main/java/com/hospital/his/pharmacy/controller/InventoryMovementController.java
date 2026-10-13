package com.hospital.his.pharmacy.controller;

import com.hospital.his.pharmacy.dto.InventoryMonthlySummaryResponse;
import com.hospital.his.pharmacy.dto.InventoryMovementPageResponse;
import com.hospital.his.pharmacy.service.InventoryMovementService;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

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
     * Lista movimientos de la sucursal autenticada.
     *
     * Ejemplo:
     *
     * GET /api/pharmacy/inventory-movements
     *     ?search=paracetamol
     *     &movementType=ENTRADA
     *     &startDate=2026-10-01
     *     &endDate=2026-10-31
     *     &page=0
     *     &size=20
     */
    @GetMapping
    public ResponseEntity<
            InventoryMovementPageResponse
            >
    getMovements(
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
                                search,
                                movementType,
                                startDate,
                                endDate,
                                page,
                                size,
                                authentication
                                        .getName()
                        )
        );
    }

    /**
     * Genera un resumen para un mes específico.
     *
     * Ejemplo:
     *
     * GET /api/pharmacy/inventory-movements
     *     /monthly-summary?year=2026&month=10
     */
    @GetMapping("/monthly-summary")
    public ResponseEntity<
            InventoryMonthlySummaryResponse
            >
    getMonthlySummary(
            @RequestParam
            Integer year,

            @RequestParam
            Integer month,

            Authentication authentication
    ) {
        return ResponseEntity.ok(
                inventoryMovementService
                        .getMonthlySummary(
                                year,
                                month,
                                authentication
                                        .getName()
                        )
        );
    }
}
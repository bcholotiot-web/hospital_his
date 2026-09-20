package com.hospital.his.pharmacy.controller;

import com.hospital.his.pharmacy.dto.CreatePharmacyInventoryRequest;
import com.hospital.his.pharmacy.dto.PharmacyInventoryResponse;
import com.hospital.his.pharmacy.dto.UpdatePharmacyInventoryRequest;
import com.hospital.his.pharmacy.service.PharmacyInventoryService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pharmacy/inventory")
public class PharmacyInventoryController {

    private final PharmacyInventoryService
            pharmacyInventoryService;

    public PharmacyInventoryController(
            PharmacyInventoryService
                    pharmacyInventoryService
    ) {
        this.pharmacyInventoryService =
                pharmacyInventoryService;
    }

    /**
     * Lista el inventario activo de la sucursal
     * del usuario autenticado.
     *
     * GET /api/pharmacy/inventory
     */
    @GetMapping
    public ResponseEntity<
            List<PharmacyInventoryResponse>
            >
    getMyBranchInventory(
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                pharmacyInventoryService
                        .getMyBranchInventory(
                                authentication.getName()
                        )
        );
    }

    /**
     * Obtiene un registro específico.
     *
     * GET /api/pharmacy/inventory/{inventoryId}
     */
    @GetMapping("/{inventoryId}")
    public ResponseEntity<
            PharmacyInventoryResponse
            >
    getInventoryById(
            @PathVariable
            Long inventoryId,

            Authentication authentication
    ) {
        return ResponseEntity.ok(
                pharmacyInventoryService
                        .getInventoryById(
                                inventoryId,
                                authentication.getName()
                        )
        );
    }

    /**
     * Lista las alertas de stock mínimo.
     *
     * GET /api/pharmacy/inventory/alerts
     *
     * Coloca esta ruta antes de /{inventoryId}
     * para que el código sea más sencillo de leer.
     */
    @GetMapping("/alerts")
    public ResponseEntity<
            List<PharmacyInventoryResponse>
            >
    getLowStockAlerts(
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                pharmacyInventoryService
                        .getLowStockAlerts(
                                authentication.getName()
                        )
        );
    }

    /**
     * Agrega un medicamento al inventario de
     * la sucursal autenticada.
     *
     * POST /api/pharmacy/inventory
     */
    @PostMapping
    public ResponseEntity<
            PharmacyInventoryResponse
            >
    createInventory(
            @Valid
            @RequestBody
            CreatePharmacyInventoryRequest request,

            Authentication authentication
    ) {
        return ResponseEntity.ok(
                pharmacyInventoryService
                        .createInventory(
                                request,
                                authentication.getName()
                        )
        );
    }

    /**
     * Actualiza el stock y el mínimo configurado.
     *
     * PUT /api/pharmacy/inventory/{inventoryId}
     */
    @PutMapping("/{inventoryId}")
    public ResponseEntity<
            PharmacyInventoryResponse
            >
    updateInventory(
            @PathVariable
            Long inventoryId,

            @Valid
            @RequestBody
            UpdatePharmacyInventoryRequest request,

            Authentication authentication
    ) {
        return ResponseEntity.ok(
                pharmacyInventoryService
                        .updateInventory(
                                inventoryId,
                                request,
                                authentication.getName()
                        )
        );
    }

    /**
     * Activa o desactiva lógicamente un registro.
     *
     * PATCH /api/pharmacy/inventory/{inventoryId}/status
     */
    @PatchMapping("/{inventoryId}/status")
    public ResponseEntity<
            PharmacyInventoryResponse
            >
    changeInventoryStatus(
            @PathVariable
            Long inventoryId,

            @RequestParam
            Boolean active,

            Authentication authentication
    ) {
        return ResponseEntity.ok(
                pharmacyInventoryService
                        .changeInventoryStatus(
                                inventoryId,
                                active,
                                authentication.getName()
                        )
        );
    }
}
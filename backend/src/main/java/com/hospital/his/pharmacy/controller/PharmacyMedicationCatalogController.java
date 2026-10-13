package com.hospital.his.pharmacy.controller;

import com.hospital.his.medications.dto.MedicationResponse;
import com.hospital.his.medications.service.MedicationCatalogService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Catálogo operativo de medicamentos para Farmacia.
 *
 * Este controlador no administra medicamentos.
 * Solamente devuelve medicamentos activos para:
 *
 * - Crear inventario.
 * - Reactivar inventario.
 * - Buscar medicamentos disponibles.
 *
 * La ruta /api/pharmacy/** ya requiere ROLE_FARMACIA
 * desde SecurityConfig.
 */
@RestController
@RequestMapping(
        "/api/pharmacy/medications"
)
public class PharmacyMedicationCatalogController {

    private final MedicationCatalogService
            medicationCatalogService;

    public PharmacyMedicationCatalogController(
            MedicationCatalogService
                    medicationCatalogService
    ) {
        this.medicationCatalogService =
                medicationCatalogService;
    }

    /**
     * Obtiene todos los medicamentos activos.
     *
     * GET /api/pharmacy/medications
     */
    @GetMapping
    public ResponseEntity<
            List<MedicationResponse>
            >
    getActiveMedications() {
        return ResponseEntity.ok(
                medicationCatalogService
                        .getActiveMedications()
        );
    }

    /**
     * Busca medicamentos activos.
     *
     * GET /api/pharmacy/medications/search
     *     ?value=paracetamol
     */
    @GetMapping("/search")
    public ResponseEntity<
            List<MedicationResponse>
            >
    searchActiveMedications(
            @RequestParam(
                    required = false
            )
            String value
    ) {
        return ResponseEntity.ok(
                medicationCatalogService
                        .searchActiveMedications(
                                value
                        )
        );
    }
}
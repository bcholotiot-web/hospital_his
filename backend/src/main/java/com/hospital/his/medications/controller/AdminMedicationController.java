package com.hospital.his.medications.controller;

import com.hospital.his.medications.dto.CreateMedicationRequest;
import com.hospital.his.medications.dto.MedicationPageResponse;
import com.hospital.his.medications.dto.MedicationResponse;
import com.hospital.his.medications.dto.UpdateMedicationRequest;
import com.hospital.his.medications.service.MedicationAdministrationService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

/**
 * CU-14:
 * Mantenimiento administrativo del catálogo
 * de medicamentos.
 */
@RestController
@RequestMapping(
        "/api/admin/medications"
)
public class AdminMedicationController {

    private final MedicationAdministrationService
            medicationAdministrationService;

    public AdminMedicationController(
            MedicationAdministrationService
                    medicationAdministrationService
    ) {
        this.medicationAdministrationService =
                medicationAdministrationService;
    }

    /**
     * Listado paginado con filtros.
     */
    @PreAuthorize("hasAuthority('MEDICATION_READ')")
    @GetMapping
    public ResponseEntity<MedicationPageResponse>
    getMedications(
            @RequestParam(
                    required = false
            )
            String search,

            @RequestParam(
                    required = false
            )
            Boolean active,

            @RequestParam(
                    required = false
            )
            Boolean controlled,

            @RequestParam(
                    defaultValue = "0"
            )
            Integer page,

            @RequestParam(
                    defaultValue = "20"
            )
            Integer size
    ) {
        return ResponseEntity.ok(
                medicationAdministrationService
                        .getMedications(
                                search,
                                active,
                                controlled,
                                page,
                                size
                        )
        );
    }

    /**
     * Lista medicamentos activos.
     */
    @PreAuthorize("hasAuthority('MEDICATION_READ')")
    @GetMapping("/active")

    public ResponseEntity<
            List<MedicationResponse>
            >
    getActiveMedications() {
        return ResponseEntity.ok(
                medicationAdministrationService
                        .getActiveMedications()
        );
    }

    /**
     * Busca un medicamento por ID.
     */
    @PreAuthorize("hasAuthority('MEDICATION_READ')")
    @GetMapping("/{medicationId}")
    public ResponseEntity<MedicationResponse>
    getMedicationById(
            @PathVariable
            Long medicationId
    ) {
        return ResponseEntity.ok(
                medicationAdministrationService
                        .getMedicationById(
                                medicationId
                        )
        );
    }

    /**
     * Crea o reactiva un medicamento.
     */
    @PreAuthorize("hasAuthority('MEDICATION_CREATE')")
    @PostMapping
    public ResponseEntity<MedicationResponse>
    createMedication(
            @Valid
            @RequestBody
            CreateMedicationRequest request,

            Authentication authentication
    ) {
        return ResponseEntity.ok(
                medicationAdministrationService
                        .createMedication(
                                request,
                                authentication.getName()
                        )
        );
    }

    /**
     * Actualiza el medicamento.
     */
    @PreAuthorize("hasAuthority('MEDICATION_UPDATE')")
    @PutMapping("/{medicationId}")
    public ResponseEntity<MedicationResponse>
    updateMedication(
            @PathVariable
            Long medicationId,

            @Valid
            @RequestBody
            UpdateMedicationRequest request,

            Authentication authentication
    ) {
        return ResponseEntity.ok(
                medicationAdministrationService
                        .updateMedication(
                                medicationId,
                                request,
                                authentication.getName()
                        )
        );
    }

    /**
     * Activa o desactiva el medicamento.
     */
    @PreAuthorize("hasAuthority('MEDICATION_DELETE')")
    @PatchMapping("/{medicationId}/status")
    public ResponseEntity<MedicationResponse>
    changeStatus(
            @PathVariable
            Long medicationId,

            @RequestParam
            Boolean active,

            @RequestParam
            Long version,

            Authentication authentication
    ) {
        return ResponseEntity.ok(
                medicationAdministrationService
                        .changeStatus(
                                medicationId,
                                active,
                                version,
                                authentication.getName()
                        )
        );
    }
}
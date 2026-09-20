package com.hospital.his.prescriptions.controller;

import com.hospital.his.prescriptions.dto.CreatePrescriptionRequest;
import com.hospital.his.prescriptions.dto.PrescriptionResponse;
import com.hospital.his.prescriptions.dto.PrescriptionSummaryResponse;
import com.hospital.his.prescriptions.service.PrescriptionService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/doctor/prescriptions")
public class PrescriptionController {

    private final PrescriptionService
            prescriptionService;

    public PrescriptionController(
            PrescriptionService prescriptionService
    ) {
        this.prescriptionService =
                prescriptionService;
    }

    /**
     * Crea una receta médica para una consulta finalizada.
     *
     * POST /api/doctor/prescriptions
     */
    @PostMapping
    public ResponseEntity<PrescriptionResponse>
    createPrescription(
            @Valid
            @RequestBody
            CreatePrescriptionRequest request,

            Authentication authentication
    ) {
        return ResponseEntity.ok(
                prescriptionService
                        .createPrescription(
                                request,
                                authentication.getName()
                        )
        );
    }

    /**
     * Lista las recetas emitidas por el médico.
     *
     * GET /api/doctor/prescriptions
     */
    @GetMapping
    public ResponseEntity<
            List<PrescriptionSummaryResponse>
            >
    getMyPrescriptions(
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                prescriptionService
                        .getMyPrescriptions(
                                authentication.getName()
                        )
        );
    }

    /**
     * Obtiene una receta concreta emitida
     * por el médico autenticado.
     *
     * GET /api/doctor/prescriptions/{prescriptionId}
     */
    @GetMapping("/{prescriptionId}")
    public ResponseEntity<PrescriptionResponse>
    getMyPrescription(
            @PathVariable
            Long prescriptionId,

            Authentication authentication
    ) {
        return ResponseEntity.ok(
                prescriptionService
                        .getMyPrescription(
                                prescriptionId,
                                authentication.getName()
                        )
        );
    }
}
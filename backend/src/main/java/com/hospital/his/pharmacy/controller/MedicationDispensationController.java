package com.hospital.his.pharmacy.controller;

import com.hospital.his.pharmacy.dto.PrescriptionDispensationDetailResponse;
import com.hospital.his.pharmacy.dto.PrescriptionDispensationSearchResponse;
import com.hospital.his.pharmacy.service.MedicationDispensationService;
import com.hospital.his.pharmacy.dto.ConfirmDispensationRequest;
import com.hospital.his.pharmacy.dto.MedicationDispensationResponse;
import com.hospital.his.pharmacy.dto.DeclinePrescriptionRequest;
import com.hospital.his.pharmacy.dto.PrescriptionDeclineResponse;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping(
        "/api/pharmacy/dispensations"
)
public class MedicationDispensationController {

    private final MedicationDispensationService
            medicationDispensationService;

    public MedicationDispensationController(
            MedicationDispensationService
                    medicationDispensationService
    ) {
        this.medicationDispensationService =
                medicationDispensationService;
    }

    /**
     * Busca una receta activa por número visible
     * o identificador de consulta médica.
     *
     * Ejemplos:
     *
     * GET /prescriptions/search
     *     ?criteria=PRESCRIPTION_NUMBER
     *     &value=REC-A1B2C3D4E5F6
     *
     * GET /prescriptions/search
     *     ?criteria=CONSULTATION_ID
     *     &value=25
     */
    @GetMapping(
            "/prescriptions/search"
    )
    public ResponseEntity<
            List<PrescriptionDispensationSearchResponse>
            >
    searchPrescriptions(
            @RequestParam
            String criteria,

            @RequestParam
            String value,

            Authentication authentication
    ) {
        return ResponseEntity.ok(
                medicationDispensationService
                        .searchPrescriptions(
                                criteria,
                                value,
                                authentication
                                        .getName()
                        )
        );
    }

    /**
     * Obtiene el detalle de la receta seleccionada.
     *
     * La respuesta incluye:
     *
     * - Vigencia.
     * - Cantidades pendientes.
     * - Inventario de la sucursal.
     * - Stock mínimo.
     * - Disponibilidad.
     * - Alternativas del mismo principio activo.
     *
     * Este endpoint todavía no descuenta inventario.
     */
    @GetMapping(
            "/prescriptions/{prescriptionId}"
    )
    public ResponseEntity<
            PrescriptionDispensationDetailResponse
            >
    getPrescriptionDetail(
            @PathVariable
            Long prescriptionId,

            Authentication authentication
    ) {
        return ResponseEntity.ok(
                medicationDispensationService
                        .getPrescriptionDetail(
                                prescriptionId,
                                authentication
                                        .getName()
                        )
        );
    }

    /**
     * Confirma el despacho y procesa el cobro.
     *
     * POST /api/pharmacy/dispensations
     */
    @PostMapping
    public ResponseEntity<MedicationDispensationResponse>
    confirmDispensation(
            @Valid
            @RequestBody
            ConfirmDispensationRequest request,

            Authentication authentication
    ) {
        return ResponseEntity.ok(
                medicationDispensationService
                        .confirmDispensation(
                                request,
                                authentication.getName()
                        )
        );
    }

    /**
     * Recupera el comprobante de un despacho.
     *
     * GET /api/pharmacy/dispensations/{dispensationId}/receipt
     */
    @GetMapping("/{dispensationId}/receipt")
    public ResponseEntity<MedicationDispensationResponse>
    getReceipt(
            @PathVariable
            Long dispensationId,

            Authentication authentication
    ) {
        return ResponseEntity.ok(
                medicationDispensationService
                        .getReceipt(
                                dispensationId,
                                authentication.getName()
                        )
        );
    }

    /**
     * Lista los despachos del usuario autenticado.
     *
     * GET /api/pharmacy/dispensations/my
     */
    @GetMapping("/my")
    public ResponseEntity<List<MedicationDispensationResponse>> getMyDispensations(Authentication authentication) {
        return ResponseEntity.ok(
                medicationDispensationService
                        .getMyDispensations(
                                authentication.getName()
                        )
        );
    }

    /**
     * Registra que el paciente decidió no adquirir
     * los medicamentos.
     *
     * POST
     * /api/pharmacy/dispensations/prescriptions/{prescriptionId}/decline
     *
     * No descuenta inventario ni cancela automáticamente
     * la receta.
     */
    @PostMapping("/prescriptions/{prescriptionId}/decline")
    public ResponseEntity<PrescriptionDeclineResponse> declinePrescription(@PathVariable Long prescriptionId, @Valid @RequestBody DeclinePrescriptionRequest request, Authentication authentication) {
        return ResponseEntity.ok(
                medicationDispensationService
                        .declinePrescription(
                                prescriptionId,
                                request,
                                authentication.getName()
                        )
        );
    }
}
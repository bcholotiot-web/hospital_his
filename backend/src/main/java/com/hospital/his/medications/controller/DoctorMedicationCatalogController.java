package com.hospital.his.medications.controller;

import com.hospital.his.medications.dto.MedicationResponse;
import com.hospital.his.medications.service.MedicationCatalogService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(
        "/api/doctor/medications"
)
public class DoctorMedicationCatalogController {

    private final MedicationCatalogService
            medicationCatalogService;

    public DoctorMedicationCatalogController(
            MedicationCatalogService
                    medicationCatalogService
    ) {
        this.medicationCatalogService =
                medicationCatalogService;
    }

    /**
     * Obtiene el catálogo activo de medicamentos.
     *
     * GET /api/doctor/medications
     *
     * El acceso ya está protegido para MEDICO porque
     * la ruta comienza con /api/doctor.
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
     * Busca medicamentos activos por nombre.
     *
     * GET /api/doctor/medications/search?value=paracetamol
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
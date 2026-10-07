package com.hospital.his.catalogs.controller;

import com.hospital.his.catalogs.dto.BranchSpecialtyOptionResponse;
import com.hospital.his.catalogs.service.BranchSpecialtyService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoint consumido por CU-03.
 *
 * La ruta inicia con /api/appointments para conservar
 * la protección existente del rol PACIENTE.
 */
@RestController
@RequestMapping(
        "/api/appointments/branches"
)
public class AppointmentBranchSpecialtyController {

    private final BranchSpecialtyService
            branchSpecialtyService;

    public AppointmentBranchSpecialtyController(
            BranchSpecialtyService
                    branchSpecialtyService
    ) {
        this.branchSpecialtyService =
                branchSpecialtyService;
    }

    /**
     * Devuelve solamente especialidades activas
     * configuradas para la sede seleccionada.
     *
     * GET:
     * /api/appointments/branches/{branchId}/specialties
     */
    @GetMapping(
            "/{branchId}/specialties"
    )
    public ResponseEntity<
            List<BranchSpecialtyOptionResponse>
            >
    getSpecialtiesByBranch(
            @PathVariable
            Long branchId
    ) {
        return ResponseEntity.ok(
                branchSpecialtyService
                        .getActiveSpecialtiesByBranch(
                                branchId
                        )
        );
    }
}
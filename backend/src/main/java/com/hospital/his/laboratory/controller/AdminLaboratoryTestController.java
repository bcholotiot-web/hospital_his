package com.hospital.his.laboratory.controller;

import com.hospital.his.laboratory.dto.CreateLaboratoryTestRequest;
import com.hospital.his.laboratory.dto.LaboratoryTestPageResponse;
import com.hospital.his.laboratory.dto.LaboratoryTestResponse;
import com.hospital.his.laboratory.dto.UpdateLaboratoryTestRequest;
import com.hospital.his.laboratory.service.LaboratoryTestCatalogService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

/**
 * CU-14:
 * Mantenimiento administrativo del catálogo
 * de exámenes de laboratorio.
 */
@RestController
@RequestMapping(
        "/api/admin/laboratory-tests"
)
public class AdminLaboratoryTestController {

    private final LaboratoryTestCatalogService
            laboratoryTestCatalogService;

    public AdminLaboratoryTestController(
            LaboratoryTestCatalogService
                    laboratoryTestCatalogService
    ) {
        this.laboratoryTestCatalogService =
                laboratoryTestCatalogService;
    }

    /**
     * Listado paginado.
     */
    @PreAuthorize("hasAuthority('LABORATORY_TEST_READ')")
    @GetMapping
    public ResponseEntity<
            LaboratoryTestPageResponse
            >
    getTests(
            @RequestParam(
                    required = false
            )
            String search,

            @RequestParam(
                    required = false
            )
            Boolean active,

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
                laboratoryTestCatalogService
                        .getTestsPage(
                                search,
                                active,
                                page,
                                size
                        )
        );
    }

    /**
     * Catálogo activo para selectores.
     */
    @PreAuthorize("hasAuthority('LABORATORY_TEST_READ')")
    @GetMapping("/active")
    public ResponseEntity<
            List<LaboratoryTestResponse>
            >
    getActiveTests() {
        return ResponseEntity.ok(
                laboratoryTestCatalogService
                        .getActiveTests()
        );
    }

    /**
     * Busca un examen por ID.
     */
    @PreAuthorize("hasAuthority('LABORATORY_TEST_READ')")
    @GetMapping("/{testId}")
    public ResponseEntity<LaboratoryTestResponse>
    getTestById(
            @PathVariable
            Long testId
    ) {
        return ResponseEntity.ok(
                laboratoryTestCatalogService
                        .getTestById(testId)
        );
    }

    /**
     * Crea o reactiva un examen.
     */
    @PreAuthorize("hasAuthority('LABORATORY_TEST_CREATE')")
    @PostMapping
    public ResponseEntity<LaboratoryTestResponse>
    createTest(
            @Valid
            @RequestBody
            CreateLaboratoryTestRequest request,

            Authentication authentication
    ) {
        return ResponseEntity.ok(
                laboratoryTestCatalogService
                        .createTest(
                                request,
                                authentication.getName()
                        )
        );
    }

    /**
     * Actualiza el examen.
     */
    @PreAuthorize("hasAuthority('LABORATORY_TEST_UPDATE')")
    @PutMapping("/{testId}")
    public ResponseEntity<LaboratoryTestResponse>
    updateTest(
            @PathVariable
            Long testId,

            @Valid
            @RequestBody
            UpdateLaboratoryTestRequest request,

            Authentication authentication
    ) {
        return ResponseEntity.ok(
                laboratoryTestCatalogService
                        .updateTest(
                                testId,
                                request,
                                authentication.getName()
                        )
        );
    }

    /**
     * Activa o desactiva lógicamente.
     */
    @PreAuthorize("hasAuthority('LABORATORY_TEST_DELETE')")
    @PatchMapping("/{testId}/status")

    public ResponseEntity<LaboratoryTestResponse>
    changeStatus(
            @PathVariable
            Long testId,

            @RequestParam
            Boolean active,

            @RequestParam
            Long version,

            Authentication authentication
    ) {
        return ResponseEntity.ok(
                laboratoryTestCatalogService
                        .changeStatus(
                                testId,
                                active,
                                version,
                                authentication.getName()
                        )
        );
    }
}
package com.hospital.his.catalogs.controller;

import com.hospital.his.catalogs.dto.BranchSpecialtyOptionResponse;
import com.hospital.his.catalogs.dto.BranchSpecialtyPageResponse;
import com.hospital.his.catalogs.dto.BranchSpecialtyResponse;
import com.hospital.his.catalogs.dto.CreateBranchSpecialtyRequest;
import com.hospital.his.catalogs.service.BranchSpecialtyService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@RestController
@RequestMapping(
        "/api/admin/branch-specialties"
)
public class BranchSpecialtyController {

    private final BranchSpecialtyService
            branchSpecialtyService;

    public BranchSpecialtyController(
            BranchSpecialtyService
                    branchSpecialtyService
    ) {
        this.branchSpecialtyService =
                branchSpecialtyService;
    }

    /**
     * Lista asignaciones con paginación.
     *
     * Filtro opcional:
     * id
     */
    @PreAuthorize("hasAuthority('BRANCH_SPECIALTY_READ')")
    @GetMapping
    public ResponseEntity<
            BranchSpecialtyPageResponse
            >
    getAssignments(
            @RequestParam(
                    required = false
            )
            Long id,

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
                branchSpecialtyService
                        .getAssignments(
                                id,
                                page,
                                size,
                                authentication
                                        .getName()
                        )
        );
    }

    /**
     * Crea o reactiva una asignación.
     */
    @PreAuthorize("hasAuthority('BRANCH_SPECIALTY_CREATE')")
    @PostMapping
    public ResponseEntity<
            BranchSpecialtyResponse
            >
    createAssignment(
            @Valid
            @RequestBody
            CreateBranchSpecialtyRequest request,

            Authentication authentication
    ) {
        return ResponseEntity.ok(
                branchSpecialtyService
                        .createAssignment(
                                request,
                                authentication
                                        .getName()
                        )
        );
    }

    /**
     * Elimina lógicamente una asignación.
     */
    @PreAuthorize("hasAuthority('BRANCH_SPECIALTY_DELETE')")
    @DeleteMapping("/{assignmentId}")
    public ResponseEntity<
            BranchSpecialtyResponse
            >
    deactivateAssignment(
            @PathVariable
            Long assignmentId,

            Authentication authentication
    ) {
        return ResponseEntity.ok(
                branchSpecialtyService
                        .deactivateAssignment(
                                assignmentId,
                                authentication
                                        .getName()
                        )
        );
    }

    /**
     * Devuelve sedes activas.
     */
    @PreAuthorize("hasAuthority('BRANCH_SPECIALTY_READ')")
    @GetMapping("/options/branches")
    public ResponseEntity<
            List<BranchSpecialtyOptionResponse>
            >
    getActiveBranches(
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                branchSpecialtyService
                        .getActiveBranchOptions(
                                authentication
                                        .getName()
                        )
        );
    }

    /**
     * Devuelve especialidades activas.
     */
    @PreAuthorize("hasAuthority('BRANCH_SPECIALTY_READ')")
    @GetMapping("/options/specialties")
    public ResponseEntity<
            List<BranchSpecialtyOptionResponse>
            >
    getActiveSpecialties(
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                branchSpecialtyService
                        .getActiveSpecialtyOptions(
                                authentication
                                        .getName()
                        )
        );
    }
}
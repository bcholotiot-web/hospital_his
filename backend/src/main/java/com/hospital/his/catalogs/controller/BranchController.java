package com.hospital.his.catalogs.controller;

import com.hospital.his.catalogs.dto.BranchPageResponse;
import com.hospital.his.catalogs.dto.BranchResponse;
import com.hospital.his.catalogs.dto.CreateBranchRequest;
import com.hospital.his.catalogs.dto.UpdateBranchRequest;
import com.hospital.his.catalogs.service.BranchService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;


import java.util.List;

@RestController
@RequestMapping("/api/branches")
public class BranchController {

    private final BranchService
            branchService;

    public BranchController(
            BranchService branchService
    ) {
        this.branchService =
                branchService;
    }

    /**
     * Crea o reactiva una sucursal.
     */
    @PreAuthorize("hasAuthority('BRANCH_CREATE')")
    @PostMapping
    public ResponseEntity<BranchResponse>
    createBranch(
            @Valid
            @RequestBody
            CreateBranchRequest request,

            Authentication authentication
    ) {
        return ResponseEntity.ok(
                branchService.createBranch(
                        request,
                        authentication.getName()
                )
        );
    }

    /**
     * Conserva compatibilidad con el frontend
     * administrativo actual.
     */
    @PreAuthorize("hasAuthority('BRANCH_READ')")
    @GetMapping
    public ResponseEntity<
            List<BranchResponse>
            >
    getAllBranches() {
        return ResponseEntity.ok(
                branchService.getAllBranches()
        );
    }

    /**
     * Listado administrativo paginado.
     */
    @PreAuthorize("hasAuthority('BRANCH_READ')")
    @GetMapping("/page")
    public ResponseEntity<
            BranchPageResponse
            >
    getBranchesPage(
            @RequestParam(
                    required = false
            )
            String name,

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
                branchService.getBranchesPage(
                        name,
                        active,
                        page,
                        size
                )
        );
    }

    /**
     * Devuelve únicamente sucursales activas.
     */
    @PreAuthorize("hasAuthority('BRANCH_READ')")
    @GetMapping("/active")
    public ResponseEntity<
            List<BranchResponse>
            >
    getActiveBranches() {
        return ResponseEntity.ok(
                branchService
                        .getActiveBranches()
        );
    }

    /**
     * Busca una sucursal por ID.
     */
    @PreAuthorize("hasAuthority('BRANCH_READ')")
    @GetMapping("/{id}")
    public ResponseEntity<BranchResponse>
    getBranchById(
            @PathVariable
            Long id
    ) {
        return ResponseEntity.ok(
                branchService.getBranchById(id)
        );
    }

    /**
     * Actualiza datos generales.
     */
    @PreAuthorize("hasAnyAuthority('BRANCH_UPDATE')")
    @PutMapping("/{id}")
    public ResponseEntity<BranchResponse>
    updateBranch(
            @PathVariable
            Long id,

            @Valid
            @RequestBody
            UpdateBranchRequest request,

            Authentication authentication
    ) {
        return ResponseEntity.ok(
                branchService.updateBranch(
                        id,
                        request,
                        authentication.getName()
                )
        );
    }

    /**
     * Activa o desactiva una sucursal.
     */
    @PreAuthorize("hasAuthority('BRANCH_DELETE')")
    @PatchMapping("/{id}/status")
    public ResponseEntity<BranchResponse>
    changeStatus(
            @PathVariable
            Long id,

            @RequestParam
            Boolean active,

            @RequestParam
            Long version,

            Authentication authentication
    ) {
        return ResponseEntity.ok(
                branchService.changeStatus(
                        id,
                        active,
                        version,
                        authentication.getName()
                )
        );
    }
}
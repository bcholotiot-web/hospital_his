package com.hospital.his.catalogs.controller;

import com.hospital.his.catalogs.dto.CreateSpecialtyRequest;
import com.hospital.his.catalogs.dto.SpecialtyPageResponse;
import com.hospital.his.catalogs.dto.SpecialtyResponse;
import com.hospital.his.catalogs.dto.UpdateSpecialtyRequest;
import com.hospital.his.catalogs.service.SpecialtyService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@RestController
@RequestMapping("/api/specialties")
public class SpecialtyController {

    private final SpecialtyService
            specialtyService;

    public SpecialtyController(
            SpecialtyService specialtyService
    ) {
        this.specialtyService =
                specialtyService;
    }

    /**
     * Crea o reactiva una especialidad.
     */
    @PreAuthorize("hasAuthority('SPECIALTY_CREATE')")
    @PostMapping
    public ResponseEntity<SpecialtyResponse>
    createSpecialty(
            @Valid
            @RequestBody
            CreateSpecialtyRequest request,

            Authentication authentication
    ) {
        return ResponseEntity.ok(
                specialtyService
                        .createSpecialty(
                                request,
                                authentication.getName()
                        )
        );
    }

    /**
     * Conserva el listado anterior para no romper
     * el frontend actual.
     */
    @PreAuthorize("hasAuthority('SPECIALTY_READ')")
    @GetMapping
    public ResponseEntity<
            List<SpecialtyResponse>
            >
    getAllSpecialties() {
        return ResponseEntity.ok(
                specialtyService
                        .getAllSpecialties()
        );
    }

    /**
     * Listado administrativo paginado.
     *
     * Ejemplos:
     *
     * /api/specialties/page?page=0&size=20
     *
     * /api/specialties/page?name=cardio
     *
     * /api/specialties/page?active=true
     */
    @GetMapping("/page")
    public ResponseEntity<
            SpecialtyPageResponse
            >
    getSpecialtiesPage(
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
                specialtyService
                        .getSpecialtiesPage(
                                name,
                                active,
                                page,
                                size
                        )
        );
    }

    /**
     * Opciones activas para formularios.
     */
    @GetMapping("/active")
    public ResponseEntity<
            List<SpecialtyResponse>
            >
    getActiveSpecialties() {
        return ResponseEntity.ok(
                specialtyService
                        .getActiveSpecialties()
        );
    }

    /**
     * Busca una especialidad por ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<SpecialtyResponse>
    getSpecialtyById(
            @PathVariable
            Long id
    ) {
        return ResponseEntity.ok(
                specialtyService
                        .getSpecialtyById(id)
        );
    }

    /**
     * Actualiza el nombre y la descripción.
     */
    @PutMapping("/{id}")
    public ResponseEntity<SpecialtyResponse>
    updateSpecialty(
            @PathVariable
            Long id,

            @Valid
            @RequestBody
            UpdateSpecialtyRequest request,

            Authentication authentication
    ) {
        return ResponseEntity.ok(
                specialtyService
                        .updateSpecialty(
                                id,
                                request,
                                authentication.getName()
                        )
        );
    }

    /**
     * Activa o desactiva una especialidad.
     *
     * La versión es obligatoria para detectar
     * actualizaciones simultáneas.
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<SpecialtyResponse>
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
                specialtyService
                        .changeStatus(
                                id,
                                active,
                                version,
                                authentication.getName()
                        )
        );
    }
}
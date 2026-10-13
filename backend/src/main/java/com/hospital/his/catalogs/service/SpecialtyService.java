package com.hospital.his.catalogs.service;

import com.hospital.his.audit.service.AuditService;
import com.hospital.his.catalogs.dto.CreateSpecialtyRequest;
import com.hospital.his.catalogs.dto.SpecialtyPageResponse;
import com.hospital.his.catalogs.dto.SpecialtyResponse;
import com.hospital.his.catalogs.dto.UpdateSpecialtyRequest;
import com.hospital.his.catalogs.entity.Specialty;
import com.hospital.his.catalogs.repository.SpecialtyRepository;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class SpecialtyService {

    private static final int MAXIMUM_PAGE_SIZE =
            100;

    private final SpecialtyRepository
            specialtyRepository;

    private final AuditService
            auditService;

    public SpecialtyService(
            SpecialtyRepository specialtyRepository,
            AuditService auditService
    ) {
        this.specialtyRepository =
                specialtyRepository;

        this.auditService =
                auditService;
    }

    /**
     * CU-14:
     * Crea una especialidad activa.
     *
     * Si existe una especialidad inactiva con el
     * mismo nombre, reactiva el registro histórico.
     */
    @Transactional
    public SpecialtyResponse createSpecialty(
            CreateSpecialtyRequest request,
            String adminUsername
    ) {
        validateCreateRequest(request);

        String name =
                normalizeName(
                        request.getName()
                );

        String description =
                normalizeRequiredText(
                        request.getDescription(),
                        500,
                        "La descripción de la especialidad "
                                + "es obligatoria.",
                        "La descripción no puede exceder "
                                + "los 500 caracteres."
                );

        Specialty existing =
                specialtyRepository
                        .findByNameIgnoreCase(name)
                        .orElse(null);

        LocalDateTime now =
                LocalDateTime.now();

        if (existing != null) {
            if (
                    Boolean.TRUE.equals(
                            existing.getActive()
                    )
            ) {
                throw new RuntimeException(
                        "La especialidad ya se encuentra registrada."
                );
            }

            /*
             * Reactivar la misma fila para conservar
             * referencias históricas.
             */
            existing.setDescription(
                    description
            );

            existing.setActive(true);
            existing.setUpdatedAt(now);
            existing.setUpdatedBy(
                    adminUsername
            );

            try {
                existing =
                        specialtyRepository
                                .saveAndFlush(
                                        existing
                                );

            } catch (
                    OptimisticLockingFailureException exception
            ) {
                throw new RuntimeException(
                        "La especialidad fue actualizada "
                                + "por otro usuario. Recargue "
                                + "la información."
                );
            }

            auditService.log(
                    adminUsername,
                    "REACTIVATE_SPECIALTY",
                    "CATALOGS",
                    "Especialidad "
                            + existing.getName()
                            + " reactivada."
            );

            return toResponse(
                    existing,
                    "Especialidad reactivada correctamente."
            );
        }

        Specialty specialty =
                Specialty.builder()
                        .name(name)
                        .description(description)
                        .active(true)
                        .createdAt(now)
                        .createdBy(
                                adminUsername
                        )
                        .updatedAt(now)
                        .updatedBy(
                                adminUsername
                        )
                        .build();

        try {
            specialty =
                    specialtyRepository
                            .saveAndFlush(
                                    specialty
                            );

        } catch (
                DataIntegrityViolationException exception
        ) {
            throw new RuntimeException(
                    "La especialidad ya se encuentra registrada."
            );
        }

        auditService.log(
                adminUsername,
                "CREATE_SPECIALTY",
                "CATALOGS",
                "Especialidad "
                        + specialty.getName()
                        + " creada correctamente."
        );

        return toResponse(
                specialty,
                "Especialidad creada correctamente."
        );
    }

    /**
     * Conserva compatibilidad con pantallas
     * existentes que esperan un arreglo completo.
     */
    @Transactional(readOnly = true)
    public List<SpecialtyResponse>
    getAllSpecialties() {
        return specialtyRepository
                .findAllByOrderByNameAsc(
                        Pageable.unpaged()
                )
                .getContent()
                .stream()
                .map(specialty ->
                        toResponse(
                                specialty,
                                null
                        )
                )
                .toList();
    }

    /**
     * Devuelve únicamente las especialidades activas
     * para selectores de otros módulos.
     */
    @Transactional(readOnly = true)
    public List<SpecialtyResponse>
    getActiveSpecialties() {
        return specialtyRepository
                .findByActiveTrueOrderByNameAsc()
                .stream()
                .map(specialty ->
                        toResponse(
                                specialty,
                                null
                        )
                )
                .toList();
    }

    /**
     * Listado administrativo paginado.
     */
    @Transactional(readOnly = true)
    public SpecialtyPageResponse
    getSpecialtiesPage(
            String name,
            Boolean active,
            Integer page,
            Integer size
    ) {
        int safePage =
                page != null
                        ? page
                        : 0;

        int safeSize =
                size != null
                        ? size
                        : 20;

        if (safePage < 0) {
            throw new RuntimeException(
                    "El número de página no puede ser negativo."
            );
        }

        if (
                safeSize <= 0 ||
                        safeSize > MAXIMUM_PAGE_SIZE
        ) {
            throw new RuntimeException(
                    "El tamaño de página debe estar entre "
                            + "1 y 100 registros."
            );
        }

        String cleanName =
                name != null
                        ? name.trim()
                        : "";

        if (cleanName.length() > 100) {
            throw new RuntimeException(
                    "El filtro de nombre no puede exceder "
                            + "los 100 caracteres."
            );
        }

        Pageable pageable =
                PageRequest.of(
                        safePage,
                        safeSize
                );

        Page<Specialty> result;

        if (
                !cleanName.isBlank() &&
                        active != null
        ) {
            result =
                    specialtyRepository
                            .findByNameContainingIgnoreCaseAndActiveOrderByNameAsc(
                                    cleanName,
                                    active,
                                    pageable
                            );

        } else if (
                !cleanName.isBlank()
        ) {
            result =
                    specialtyRepository
                            .findByNameContainingIgnoreCaseOrderByNameAsc(
                                    cleanName,
                                    pageable
                            );

        } else if (
                active != null
        ) {
            result =
                    specialtyRepository
                            .findByActiveOrderByNameAsc(
                                    active,
                                    pageable
                            );

        } else {
            result =
                    specialtyRepository
                            .findAllByOrderByNameAsc(
                                    pageable
                            );
        }

        return SpecialtyPageResponse
                .builder()
                .content(
                        result.getContent()
                                .stream()
                                .map(specialty ->
                                        toResponse(
                                                specialty,
                                                null
                                        )
                                )
                                .toList()
                )
                .page(
                        result.getNumber()
                )
                .size(
                        result.getSize()
                )
                .totalElements(
                        result.getTotalElements()
                )
                .totalPages(
                        result.getTotalPages()
                )
                .first(
                        result.isFirst()
                )
                .last(
                        result.isLast()
                )
                .empty(
                        result.isEmpty()
                )
                .build();
    }

    /**
     * Busca una especialidad por ID.
     */
    @Transactional(readOnly = true)
    public SpecialtyResponse getSpecialtyById(
            Long id
    ) {
        Specialty specialty =
                findSpecialty(id);

        return toResponse(
                specialty,
                "Especialidad encontrada."
        );
    }

    /**
     * Actualiza nombre y descripción.
     *
     * El estado no se modifica desde este método.
     */
    @Transactional
    public SpecialtyResponse updateSpecialty(
            Long id,
            UpdateSpecialtyRequest request,
            String adminUsername
    ) {
        if (request == null) {
            throw new RuntimeException(
                    "Los datos de actualización "
                            + "son obligatorios."
            );
        }

        Specialty specialty =
                findSpecialty(id);

        validateVersion(
                specialty,
                request.getVersion()
        );

        String name =
                normalizeName(
                        request.getName()
                );

        String description =
                normalizeRequiredText(
                        request.getDescription(),
                        500,
                        "La descripción es obligatoria.",
                        "La descripción no puede exceder "
                                + "los 500 caracteres."
                );

        if (
                specialtyRepository
                        .existsByNameIgnoreCaseAndIdNot(
                                name,
                                specialty.getId()
                        )
        ) {
            throw new RuntimeException(
                    "La especialidad ya se encuentra registrada."
            );
        }

        specialty.setName(name);
        specialty.setDescription(
                description
        );
        specialty.setUpdatedAt(
                LocalDateTime.now()
        );
        specialty.setUpdatedBy(
                adminUsername
        );

        try {
            specialty =
                    specialtyRepository
                            .saveAndFlush(
                                    specialty
                            );

        } catch (
                OptimisticLockingFailureException exception
        ) {
            throw new RuntimeException(
                    "La especialidad fue actualizada por "
                            + "otro usuario. Recargue la información."
            );
        }

        auditService.log(
                adminUsername,
                "UPDATE_SPECIALTY",
                "CATALOGS",
                "Especialidad ID "
                        + specialty.getId()
                        + " actualizada. Nombre: "
                        + specialty.getName()
                        + "."
        );

        return toResponse(
                specialty,
                "Especialidad actualizada correctamente."
        );
    }

    /**
     * Activa o desactiva lógicamente una especialidad.
     */
    @Transactional
    public SpecialtyResponse changeStatus(
            Long id,
            Boolean active,
            Long version,
            String adminUsername
    ) {
        if (active == null) {
            throw new RuntimeException(
                    "Debe indicar el nuevo estado."
            );
        }

        Specialty specialty =
                findSpecialty(id);

        validateVersion(
                specialty,
                version
        );

        if (
                Boolean.TRUE.equals(
                        specialty.getActive()
                ) == active
        ) {
            throw new RuntimeException(
                    active
                            ? "La especialidad ya se encuentra activa."
                            : "La especialidad ya se encuentra inactiva."
            );
        }

        specialty.setActive(active);
        specialty.setUpdatedAt(
                LocalDateTime.now()
        );
        specialty.setUpdatedBy(
                adminUsername
        );

        try {
            specialty =
                    specialtyRepository
                            .saveAndFlush(
                                    specialty
                            );

        } catch (
                OptimisticLockingFailureException exception
        ) {
            throw new RuntimeException(
                    "La especialidad fue actualizada por "
                            + "otro usuario. Recargue la información."
            );
        }

        String action =
                active
                        ? "REACTIVATE_SPECIALTY"
                        : "DEACTIVATE_SPECIALTY";

        auditService.log(
                adminUsername,
                action,
                "CATALOGS",
                "Especialidad ID "
                        + specialty.getId()
                        + " cambió su estado a "
                        + (
                        active
                                ? "ACTIVA"
                                : "INACTIVA"
                )
                        + "."
        );

        return toResponse(
                specialty,
                active
                        ? "Especialidad reactivada correctamente."
                        : "Especialidad desactivada correctamente."
        );
    }

    private Specialty findSpecialty(
            Long id
    ) {
        if (id == null) {
            throw new RuntimeException(
                    "Debe indicar la especialidad."
            );
        }

        return specialtyRepository
                .findById(id)
                .orElseThrow(
                        () -> new RuntimeException(
                                "Especialidad no encontrada."
                        )
                );
    }

    private void validateCreateRequest(
            CreateSpecialtyRequest request
    ) {
        if (request == null) {
            throw new RuntimeException(
                    "Los datos de la especialidad "
                            + "son obligatorios."
            );
        }
    }

    private void validateVersion(
            Specialty specialty,
            Long requestedVersion
    ) {
        if (requestedVersion == null) {
            throw new RuntimeException(
                    "Debe indicar la versión "
                            + "de la especialidad."
            );
        }

        if (
                !specialty.getVersion()
                        .equals(
                                requestedVersion
                        )
        ) {
            throw new RuntimeException(
                    "La especialidad fue actualizada por "
                            + "otro usuario. Recargue la información."
            );
        }
    }

    /**
     * Normaliza el nombre:
     *
     * - Elimina espacios extremos.
     * - Agrupa espacios internos.
     * - Convierte a mayúsculas.
     */
    private String normalizeName(
            String value
    ) {
        String cleanValue =
                normalizeRequiredText(
                        value,
                        100,
                        "El nombre de la especialidad "
                                + "es obligatorio.",
                        "El nombre no puede exceder "
                                + "los 100 caracteres."
                );

        return cleanValue
                .replaceAll(
                        "\\s+",
                        " "
                )
                .toUpperCase(
                        Locale.ROOT
                );
    }

    private String normalizeRequiredText(
            String value,
            int maximumLength,
            String requiredMessage,
            String maximumMessage
    ) {
        if (
                value == null ||
                        value.isBlank()
        ) {
            throw new RuntimeException(
                    requiredMessage
            );
        }

        String cleanValue =
                value.trim();

        if (
                cleanValue.length()
                        > maximumLength
        ) {
            throw new RuntimeException(
                    maximumMessage
            );
        }

        return cleanValue;
    }

    private SpecialtyResponse toResponse(
            Specialty specialty,
            String message
    ) {
        return SpecialtyResponse
                .builder()
                .id(
                        specialty.getId()
                )
                .name(
                        specialty.getName()
                )
                .description(
                        specialty.getDescription()
                )
                .active(
                        specialty.getActive()
                )
                .createdAt(
                        specialty.getCreatedAt()
                                .toString()
                )
                .createdBy(
                        specialty.getCreatedBy()
                )
                .updatedAt(
                        specialty.getUpdatedAt()
                                .toString()
                )
                .updatedBy(
                        specialty.getUpdatedBy()
                )
                .version(
                        specialty.getVersion()
                )
                .message(message)
                .build();
    }
}
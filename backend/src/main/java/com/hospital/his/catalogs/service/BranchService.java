package com.hospital.his.catalogs.service;

import com.hospital.his.audit.service.AuditService;
import com.hospital.his.catalogs.dto.BranchPageResponse;
import com.hospital.his.catalogs.dto.BranchResponse;
import com.hospital.his.catalogs.dto.CreateBranchRequest;
import com.hospital.his.catalogs.dto.UpdateBranchRequest;
import com.hospital.his.catalogs.entity.Branch;
import com.hospital.his.catalogs.repository.BranchRepository;

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
public class BranchService {

    private static final int MAXIMUM_PAGE_SIZE =
            100;

    private final BranchRepository
            branchRepository;

    private final AuditService
            auditService;

    public BranchService(
            BranchRepository branchRepository,
            AuditService auditService
    ) {
        this.branchRepository =
                branchRepository;

        this.auditService =
                auditService;
    }

    /**
     * Crea una sucursal activa.
     *
     * Si existe una sucursal inactiva con el mismo
     * nombre, reactiva la fila existente.
     */
    @Transactional
    public BranchResponse createBranch(
            CreateBranchRequest request,
            String adminUsername
    ) {
        if (request == null) {
            throw new RuntimeException(
                    "Los datos de la sucursal son obligatorios."
            );
        }

        String name =
                normalizeName(
                        request.getName()
                );

        String phone =
                normalizePhone(
                        request.getPhone()
                );

        String address =
                normalizeOptionalText(
                        request.getAddress(),
                        500,
                        "La dirección no puede exceder "
                                + "los 500 caracteres."
                );

        String description =
                normalizeOptionalText(
                        request.getDescription(),
                        250,
                        "La descripción no puede exceder "
                                + "los 250 caracteres."
                );

        Branch existing =
                branchRepository
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
                        "La sucursal ya se encuentra registrada."
                );
            }

            /*
             * Reactivar el registro histórico.
             */
            existing.setPhone(phone);
            existing.setAddress(address);
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
                        branchRepository
                                .saveAndFlush(
                                        existing
                                );

            } catch (
                    OptimisticLockingFailureException exception
            ) {
                throw new RuntimeException(
                        "La sucursal fue actualizada por otro "
                                + "usuario. Recargue la información."
                );
            }

            auditService.log(
                    adminUsername,
                    "REACTIVATE_BRANCH",
                    "CATALOGS",
                    "Sucursal "
                            + existing.getName()
                            + " reactivada."
            );

            return toResponse(
                    existing,
                    "Sucursal reactivada correctamente."
            );
        }

        Branch branch =
                Branch.builder()
                        .name(name)
                        .phone(phone)
                        .address(address)
                        .description(
                                description
                        )
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
            branch =
                    branchRepository
                            .saveAndFlush(
                                    branch
                            );

        } catch (
                DataIntegrityViolationException exception
        ) {
            throw new RuntimeException(
                    "La sucursal ya se encuentra registrada."
            );
        }

        auditService.log(
                adminUsername,
                "CREATE_BRANCH",
                "CATALOGS",
                "Sucursal "
                        + branch.getName()
                        + " creada correctamente."
        );

        return toResponse(
                branch,
                "Sucursal creada correctamente."
        );
    }

    /**
     * Conserva el listado utilizado por la
     * pantalla administrativa anterior.
     */
    @Transactional(readOnly = true)
    public List<BranchResponse>
    getAllBranches() {
        return branchRepository
                .findAllByOrderByNameAsc(
                        Pageable.unpaged()
                )
                .getContent()
                .stream()
                .map(branch ->
                        toResponse(
                                branch,
                                null
                        )
                )
                .toList();
    }

    /**
     * Devuelve sucursales activas para selectores.
     */
    @Transactional(readOnly = true)
    public List<BranchResponse>
    getActiveBranches() {
        return branchRepository
                .findByActiveTrueOrderByNameAsc()
                .stream()
                .map(branch ->
                        toResponse(
                                branch,
                                null
                        )
                )
                .toList();
    }

    /**
     * Listado administrativo con filtros.
     */
    @Transactional(readOnly = true)
    public BranchPageResponse getBranchesPage(
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

        Page<Branch> result;

        if (
                !cleanName.isBlank() &&
                        active != null
        ) {
            result =
                    branchRepository
                            .findByNameContainingIgnoreCaseAndActiveOrderByNameAsc(
                                    cleanName,
                                    active,
                                    pageable
                            );

        } else if (!cleanName.isBlank()) {
            result =
                    branchRepository
                            .findByNameContainingIgnoreCaseOrderByNameAsc(
                                    cleanName,
                                    pageable
                            );

        } else if (active != null) {
            result =
                    branchRepository
                            .findByActiveOrderByNameAsc(
                                    active,
                                    pageable
                            );

        } else {
            result =
                    branchRepository
                            .findAllByOrderByNameAsc(
                                    pageable
                            );
        }

        return BranchPageResponse.builder()
                .content(
                        result.getContent()
                                .stream()
                                .map(branch ->
                                        toResponse(
                                                branch,
                                                null
                                        )
                                )
                                .toList()
                )
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(
                        result.getTotalElements()
                )
                .totalPages(
                        result.getTotalPages()
                )
                .first(result.isFirst())
                .last(result.isLast())
                .empty(result.isEmpty())
                .build();
    }

    /**
     * Busca una sucursal por ID.
     */
    @Transactional(readOnly = true)
    public BranchResponse getBranchById(
            Long id
    ) {
        return toResponse(
                findBranch(id),
                "Sucursal encontrada."
        );
    }

    /**
     * Actualiza los datos generales.
     *
     * El estado se modifica mediante un endpoint
     * separado.
     */
    @Transactional
    public BranchResponse updateBranch(
            Long id,
            UpdateBranchRequest request,
            String adminUsername
    ) {
        if (request == null) {
            throw new RuntimeException(
                    "Los datos de actualización "
                            + "son obligatorios."
            );
        }

        Branch branch =
                findBranch(id);

        validateVersion(
                branch,
                request.getVersion()
        );

        String name =
                normalizeName(
                        request.getName()
                );

        if (
                branchRepository
                        .existsByNameIgnoreCaseAndIdNot(
                                name,
                                branch.getId()
                        )
        ) {
            throw new RuntimeException(
                    "La sucursal ya se encuentra registrada."
            );
        }

        branch.setName(name);

        branch.setPhone(
                normalizePhone(
                        request.getPhone()
                )
        );

        branch.setAddress(
                normalizeOptionalText(
                        request.getAddress(),
                        500,
                        "La dirección no puede exceder "
                                + "los 500 caracteres."
                )
        );

        branch.setDescription(
                normalizeOptionalText(
                        request.getDescription(),
                        250,
                        "La descripción no puede exceder "
                                + "los 250 caracteres."
                )
        );

        branch.setUpdatedAt(
                LocalDateTime.now()
        );

        branch.setUpdatedBy(
                adminUsername
        );

        try {
            branch =
                    branchRepository
                            .saveAndFlush(
                                    branch
                            );

        } catch (
                OptimisticLockingFailureException exception
        ) {
            throw new RuntimeException(
                    "La sucursal fue actualizada por otro "
                            + "usuario. Recargue la información."
            );
        }

        auditService.log(
                adminUsername,
                "UPDATE_BRANCH",
                "CATALOGS",
                "Sucursal ID "
                        + branch.getId()
                        + " actualizada. Nombre: "
                        + branch.getName()
                        + "."
        );

        return toResponse(
                branch,
                "Sucursal actualizada correctamente."
        );
    }

    /**
     * Activa o desactiva lógicamente una sucursal.
     */
    @Transactional
    public BranchResponse changeStatus(
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

        Branch branch =
                findBranch(id);

        validateVersion(
                branch,
                version
        );

        if (
                Boolean.TRUE.equals(
                        branch.getActive()
                ) == active
        ) {
            throw new RuntimeException(
                    active
                            ? "La sucursal ya se encuentra activa."
                            : "La sucursal ya se encuentra inactiva."
            );
        }

        branch.setActive(active);
        branch.setUpdatedAt(
                LocalDateTime.now()
        );
        branch.setUpdatedBy(
                adminUsername
        );

        try {
            branch =
                    branchRepository
                            .saveAndFlush(
                                    branch
                            );

        } catch (
                OptimisticLockingFailureException exception
        ) {
            throw new RuntimeException(
                    "La sucursal fue actualizada por otro "
                            + "usuario. Recargue la información."
            );
        }

        auditService.log(
                adminUsername,
                active
                        ? "REACTIVATE_BRANCH"
                        : "DEACTIVATE_BRANCH",
                "CATALOGS",
                "Sucursal ID "
                        + branch.getId()
                        + " cambió su estado a "
                        + (
                        active
                                ? "ACTIVA"
                                : "INACTIVA"
                )
                        + "."
        );

        return toResponse(
                branch,
                active
                        ? "Sucursal reactivada correctamente."
                        : "Sucursal desactivada correctamente."
        );
    }

    private Branch findBranch(
            Long id
    ) {
        if (id == null) {
            throw new RuntimeException(
                    "Debe indicar la sucursal."
            );
        }

        return branchRepository
                .findById(id)
                .orElseThrow(
                        () -> new RuntimeException(
                                "Sucursal no encontrada."
                        )
                );
    }

    private void validateVersion(
            Branch branch,
            Long requestedVersion
    ) {
        if (requestedVersion == null) {
            throw new RuntimeException(
                    "Debe indicar la versión de la sucursal."
            );
        }

        if (
                !branch.getVersion()
                        .equals(
                                requestedVersion
                        )
        ) {
            throw new RuntimeException(
                    "La sucursal fue actualizada por otro "
                            + "usuario. Recargue la información."
            );
        }
    }

    /**
     * Normaliza el nombre de la sede.
     */
    private String normalizeName(
            String value
    ) {
        if (
                value == null ||
                        value.isBlank()
        ) {
            throw new RuntimeException(
                    "El nombre de la sucursal es obligatorio."
            );
        }

        String cleanValue =
                value.trim()
                        .replaceAll(
                                "\\s+",
                                " "
                        );

        if (cleanValue.length() > 100) {
            throw new RuntimeException(
                    "El nombre no puede exceder "
                            + "los 100 caracteres."
            );
        }

        return cleanValue.toUpperCase(
                Locale.ROOT
        );
    }

    /**
     * Valida el teléfono opcional.
     */
    private String normalizePhone(
            String value
    ) {
        if (
                value == null ||
                        value.isBlank()
        ) {
            return null;
        }

        String cleanValue =
                value.trim();

        if (
                !cleanValue.matches(
                        "^[0-9]{8}$"
                )
        ) {
            throw new RuntimeException(
                    "El teléfono debe contener "
                            + "exactamente 8 dígitos."
            );
        }

        return cleanValue;
    }

    private String normalizeOptionalText(
            String value,
            int maximumLength,
            String maximumMessage
    ) {
        if (
                value == null ||
                        value.isBlank()
        ) {
            return null;
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

    private BranchResponse toResponse(
            Branch branch,
            String message
    ) {
        return BranchResponse.builder()
                .id(branch.getId())
                .name(branch.getName())
                .phone(branch.getPhone())
                .address(branch.getAddress())
                .description(
                        branch.getDescription()
                )
                .active(branch.getActive())
                .createdAt(
                        branch.getCreatedAt()
                                .toString()
                )
                .createdBy(
                        branch.getCreatedBy()
                )
                .updatedAt(
                        branch.getUpdatedAt()
                                .toString()
                )
                .updatedBy(
                        branch.getUpdatedBy()
                )
                .version(branch.getVersion())
                .message(message)
                .build();
    }
}
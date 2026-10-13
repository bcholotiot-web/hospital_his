package com.hospital.his.laboratory.service;

import com.hospital.his.audit.service.AuditService;
import com.hospital.his.laboratory.dto.CreateLaboratoryTestRequest;
import com.hospital.his.laboratory.dto.LaboratoryTestPageResponse;
import com.hospital.his.laboratory.dto.LaboratoryTestResponse;
import com.hospital.his.laboratory.dto.UpdateLaboratoryTestRequest;
import com.hospital.his.laboratory.entity.LaboratoryTest;
import com.hospital.his.laboratory.repository.LaboratoryTestRepository;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class LaboratoryTestCatalogService {

    private static final int MAXIMUM_PAGE_SIZE =
            100;

    private final LaboratoryTestRepository
            laboratoryTestRepository;

    private final AuditService
            auditService;

    public LaboratoryTestCatalogService(
            LaboratoryTestRepository
                    laboratoryTestRepository,
            AuditService auditService
    ) {
        this.laboratoryTestRepository =
                laboratoryTestRepository;

        this.auditService =
                auditService;
    }

    /**
     * Crea un examen activo.
     *
     * Si existe un registro inactivo con el mismo
     * código, reactiva la fila anterior.
     */
    @Transactional
    public LaboratoryTestResponse createTest(
            CreateLaboratoryTestRequest request,
            String adminUsername
    ) {
        if (request == null) {
            throw new RuntimeException(
                    "Los datos del examen son obligatorios."
            );
        }

        String code =
                normalizeCode(
                        request.getCode()
                );

        String name =
                normalizeRequiredText(
                        request.getName(),
                        200,
                        "El nombre del examen es obligatorio.",
                        "El nombre no puede exceder los 200 caracteres."
                );

        String description =
                normalizeOptionalText(
                        request.getDescription(),
                        500,
                        "La descripción no puede exceder los 500 caracteres."
                );

        String referenceRange =
                normalizeOptionalText(
                        request.getReferenceRange(),
                        200,
                        "El rango de referencia no puede exceder los 200 caracteres."
                );

        String defaultUnit =
                normalizeOptionalText(
                        request.getDefaultUnit(),
                        50,
                        "La unidad no puede exceder los 50 caracteres."
                );

        BigDecimal price =
                validatePrice(
                        request.getPrice()
                );

        LaboratoryTest existing =
                laboratoryTestRepository
                        .findByCodeIgnoreCase(code)
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
                        "El código del examen ya se encuentra registrado."
                );
            }

            /*
             * Reactivación del registro histórico.
             */
            existing.setName(name);
            existing.setDescription(
                    description
            );
            existing.setReferenceRange(
                    referenceRange
            );
            existing.setDefaultUnit(
                    defaultUnit
            );
            existing.setPrice(price);
            existing.setActive(true);
            existing.setUpdatedAt(now);
            existing.setUpdatedBy(
                    adminUsername
            );

            try {
                existing =
                        laboratoryTestRepository
                                .saveAndFlush(
                                        existing
                                );

            } catch (
                    OptimisticLockingFailureException exception
            ) {
                throw new RuntimeException(
                        "El examen fue actualizado por otro usuario. "
                                + "Recargue la información."
                );
            }

            auditService.log(
                    adminUsername,
                    "REACTIVATE_LABORATORY_TEST",
                    "LABORATORY_CATALOG",
                    "Examen "
                            + existing.getCode()
                            + " reactivado."
            );

            return toResponse(
                    existing,
                    "Examen de laboratorio reactivado correctamente."
            );
        }

        LaboratoryTest test =
                LaboratoryTest.builder()
                        .code(code)
                        .name(name)
                        .description(
                                description
                        )
                        .referenceRange(
                                referenceRange
                        )
                        .defaultUnit(
                                defaultUnit
                        )
                        .price(price)
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
            test =
                    laboratoryTestRepository
                            .saveAndFlush(test);

        } catch (
                DataIntegrityViolationException exception
        ) {
            throw new RuntimeException(
                    "El código del examen ya se encuentra registrado."
            );
        }

        auditService.log(
                adminUsername,
                "CREATE_LABORATORY_TEST",
                "LABORATORY_CATALOG",
                "Examen "
                        + test.getCode()
                        + " creado correctamente."
        );

        return toResponse(
                test,
                "Examen de laboratorio creado correctamente."
        );
    }

    /**
     * Listado paginado con filtros.
     */
    @Transactional(readOnly = true)
    public LaboratoryTestPageResponse getTestsPage(
            String search,
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
                    "El tamaño de página debe estar entre 1 y 100 registros."
            );
        }

        String cleanSearch =
                search != null
                        ? search.trim()
                        : "";

        if (cleanSearch.length() > 200) {
            throw new RuntimeException(
                    "El criterio de búsqueda no puede exceder los 200 caracteres."
            );
        }

        Pageable pageable =
                PageRequest.of(
                        safePage,
                        safeSize
                );

        Page<LaboratoryTest> result;

        if (
                !cleanSearch.isBlank() &&
                        active != null
        ) {
            result =
                    laboratoryTestRepository
                            .findByActiveAndCodeContainingIgnoreCaseOrActiveAndNameContainingIgnoreCaseOrderByNameAsc(
                                    active,
                                    cleanSearch,
                                    active,
                                    cleanSearch,
                                    pageable
                            );

        } else if (!cleanSearch.isBlank()) {
            result =
                    laboratoryTestRepository
                            .findByCodeContainingIgnoreCaseOrNameContainingIgnoreCaseOrderByNameAsc(
                                    cleanSearch,
                                    cleanSearch,
                                    pageable
                            );

        } else if (active != null) {
            result =
                    laboratoryTestRepository
                            .findByActiveOrderByNameAsc(
                                    active,
                                    pageable
                            );

        } else {
            result =
                    laboratoryTestRepository
                            .findAllByOrderByNameAsc(
                                    pageable
                            );
        }

        return LaboratoryTestPageResponse
                .builder()
                .content(
                        result.getContent()
                                .stream()
                                .map(test ->
                                        toResponse(
                                                test,
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
     * Devuelve exámenes activos para formularios.
     */
    @Transactional(readOnly = true)
    public List<LaboratoryTestResponse>
    getActiveTests() {
        return laboratoryTestRepository
                .findByActiveTrueOrderByNameAsc()
                .stream()
                .map(test ->
                        toResponse(
                                test,
                                null
                        )
                )
                .toList();
    }

    /**
     * Busca un examen por ID.
     */
    @Transactional(readOnly = true)
    public LaboratoryTestResponse getTestById(
            Long testId
    ) {
        return toResponse(
                findTest(testId),
                "Examen de laboratorio encontrado."
        );
    }

    /**
     * Actualiza los datos administrativos.
     */
    @Transactional
    public LaboratoryTestResponse updateTest(
            Long testId,
            UpdateLaboratoryTestRequest request,
            String adminUsername
    ) {
        if (request == null) {
            throw new RuntimeException(
                    "Los datos de actualización son obligatorios."
            );
        }

        LaboratoryTest test =
                findTest(testId);

        validateVersion(
                test,
                request.getVersion()
        );

        String code =
                normalizeCode(
                        request.getCode()
                );

        if (
                laboratoryTestRepository
                        .existsByCodeIgnoreCaseAndIdNot(
                                code,
                                test.getId()
                        )
        ) {
            throw new RuntimeException(
                    "El código del examen ya se encuentra registrado."
            );
        }

        test.setCode(code);

        test.setName(
                normalizeRequiredText(
                        request.getName(),
                        200,
                        "El nombre del examen es obligatorio.",
                        "El nombre no puede exceder los 200 caracteres."
                )
        );

        test.setDescription(
                normalizeOptionalText(
                        request.getDescription(),
                        500,
                        "La descripción no puede exceder los 500 caracteres."
                )
        );

        test.setReferenceRange(
                normalizeOptionalText(
                        request.getReferenceRange(),
                        200,
                        "El rango de referencia no puede exceder los 200 caracteres."
                )
        );

        test.setDefaultUnit(
                normalizeOptionalText(
                        request.getDefaultUnit(),
                        50,
                        "La unidad no puede exceder los 50 caracteres."
                )
        );

        test.setPrice(
                validatePrice(
                        request.getPrice()
                )
        );

        test.setUpdatedAt(
                LocalDateTime.now()
        );

        test.setUpdatedBy(
                adminUsername
        );

        try {
            test =
                    laboratoryTestRepository
                            .saveAndFlush(test);

        } catch (
                OptimisticLockingFailureException exception
        ) {
            throw new RuntimeException(
                    "El examen fue actualizado por otro usuario. "
                            + "Recargue la información."
            );
        }

        auditService.log(
                adminUsername,
                "UPDATE_LABORATORY_TEST",
                "LABORATORY_CATALOG",
                "Examen ID "
                        + test.getId()
                        + " actualizado. Código: "
                        + test.getCode()
                        + "."
        );

        return toResponse(
                test,
                "Examen de laboratorio actualizado correctamente."
        );
    }

    /**
     * Activa o desactiva el examen.
     */
    @Transactional
    public LaboratoryTestResponse changeStatus(
            Long testId,
            Boolean active,
            Long version,
            String adminUsername
    ) {
        if (active == null) {
            throw new RuntimeException(
                    "Debe indicar el nuevo estado."
            );
        }

        LaboratoryTest test =
                findTest(testId);

        validateVersion(
                test,
                version
        );

        if (
                Boolean.TRUE.equals(
                        test.getActive()
                ) == active
        ) {
            throw new RuntimeException(
                    active
                            ? "El examen ya se encuentra activo."
                            : "El examen ya se encuentra inactivo."
            );
        }

        test.setActive(active);
        test.setUpdatedAt(
                LocalDateTime.now()
        );
        test.setUpdatedBy(
                adminUsername
        );

        try {
            test =
                    laboratoryTestRepository
                            .saveAndFlush(test);

        } catch (
                OptimisticLockingFailureException exception
        ) {
            throw new RuntimeException(
                    "El examen fue actualizado por otro usuario. "
                            + "Recargue la información."
            );
        }

        auditService.log(
                adminUsername,
                active
                        ? "REACTIVATE_LABORATORY_TEST"
                        : "DEACTIVATE_LABORATORY_TEST",
                "LABORATORY_CATALOG",
                "Examen ID "
                        + test.getId()
                        + " cambió su estado a "
                        + (
                        active
                                ? "ACTIVO"
                                : "INACTIVO"
                )
                        + "."
        );

        return toResponse(
                test,
                active
                        ? "Examen de laboratorio reactivado correctamente."
                        : "Examen de laboratorio desactivado correctamente."
        );
    }

    private LaboratoryTest findTest(
            Long testId
    ) {
        if (testId == null) {
            throw new RuntimeException(
                    "Debe indicar el examen de laboratorio."
            );
        }

        return laboratoryTestRepository
                .findById(testId)
                .orElseThrow(
                        () -> new RuntimeException(
                                "Examen de laboratorio no encontrado."
                        )
                );
    }

    private void validateVersion(
            LaboratoryTest test,
            Long version
    ) {
        if (version == null) {
            throw new RuntimeException(
                    "Debe indicar la versión del examen."
            );
        }

        if (
                !test.getVersion()
                        .equals(version)
        ) {
            throw new RuntimeException(
                    "El examen fue actualizado por otro usuario. "
                            + "Recargue la información."
            );
        }
    }

    private String normalizeCode(
            String value
    ) {
        String cleanValue =
                normalizeRequiredText(
                        value,
                        30,
                        "El código del examen es obligatorio.",
                        "El código no puede exceder los 30 caracteres."
                )
                        .replaceAll(
                                "\\s+",
                                "-"
                        )
                        .toUpperCase(
                                Locale.ROOT
                        );

        if (
                !cleanValue.matches(
                        "^[A-Z0-9_-]+$"
                )
        ) {
            throw new RuntimeException(
                    "El código solamente puede contener letras, "
                            + "números, guion y guion bajo."
            );
        }

        return cleanValue;
    }

    private BigDecimal validatePrice(
            BigDecimal value
    ) {
        if (value == null) {
            throw new RuntimeException(
                    "El precio del examen es obligatorio."
            );
        }

        if (
                value.compareTo(
                        BigDecimal.ZERO
                ) <= 0
        ) {
            throw new RuntimeException(
                    "El precio debe ser mayor que cero."
            );
        }

        if (value.scale() > 2) {
            throw new RuntimeException(
                    "El precio admite como máximo 2 decimales."
            );
        }

        if (value.precision() - value.scale() > 8) {
            throw new RuntimeException(
                    "El precio admite como máximo 8 dígitos enteros."
            );
        }

        return value;
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
                value.trim()
                        .replaceAll(
                                "\\s+",
                                " "
                        );

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

    private LaboratoryTestResponse toResponse(
            LaboratoryTest test,
            String message
    ) {
        return LaboratoryTestResponse.builder()
                .id(test.getId())
                .code(test.getCode())
                .name(test.getName())
                .description(
                        test.getDescription()
                )
                .referenceRange(
                        test.getReferenceRange()
                )
                .defaultUnit(
                        test.getDefaultUnit()
                )
                .price(test.getPrice())
                .active(test.getActive())
                .createdAt(
                        test.getCreatedAt()
                                .toString()
                )
                .createdBy(
                        test.getCreatedBy()
                )
                .updatedAt(
                        test.getUpdatedAt()
                                .toString()
                )
                .updatedBy(
                        test.getUpdatedBy()
                )
                .version(test.getVersion())
                .message(message)
                .build();
    }
}
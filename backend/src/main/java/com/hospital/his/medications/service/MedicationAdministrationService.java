package com.hospital.his.medications.service;

import com.hospital.his.audit.service.AuditService;
import com.hospital.his.medications.dto.CreateMedicationRequest;
import com.hospital.his.medications.dto.MedicationPageResponse;
import com.hospital.his.medications.dto.MedicationResponse;
import com.hospital.his.medications.dto.UpdateMedicationRequest;
import com.hospital.his.medications.entity.Medication;
import com.hospital.his.medications.repository.MedicationRepository;

import jakarta.persistence.criteria.Predicate;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class MedicationAdministrationService {

    private static final int MAXIMUM_PAGE_SIZE =
            100;

    private final MedicationRepository
            medicationRepository;

    private final AuditService
            auditService;

    public MedicationAdministrationService(
            MedicationRepository medicationRepository,
            AuditService auditService
    ) {
        this.medicationRepository =
                medicationRepository;

        this.auditService =
                auditService;
    }

    /**
     * Crea un medicamento activo.
     *
     * Si existe una fila inactiva con el mismo
     * código, reactiva el medicamento histórico.
     */
    @Transactional
    public MedicationResponse createMedication(
            CreateMedicationRequest request,
            String adminUsername
    ) {
        if (request == null) {
            throw new RuntimeException(
                    "Los datos del medicamento son obligatorios."
            );
        }

        String code =
                normalizeCode(
                        request.getCode()
                );

        Medication existing =
                medicationRepository
                        .findByCodeIgnoreCase(code)
                        .orElse(null);

        MedicationData data =
                validateAndNormalize(
                        request.getName(),
                        request.getActiveIngredient(),
                        request.getConcentration(),
                        request.getPresentation(),
                        request.getUnit(),
                        request.getDescription(),
                        request.getUnitPrice(),
                        request.getControlled()
                );

        LocalDateTime now =
                LocalDateTime.now();

        if (existing != null) {
            if (
                    Boolean.TRUE.equals(
                            existing.getActive()
                    )
            ) {
                throw new RuntimeException(
                        "El código del medicamento ya se encuentra registrado."
                );
            }

            applyMedicationData(
                    existing,
                    data
            );

            existing.setActive(true);
            existing.setUpdatedAt(now);
            existing.setUpdatedBy(
                    adminUsername
            );

            try {
                existing =
                        medicationRepository
                                .saveAndFlush(
                                        existing
                                );

            } catch (
                    OptimisticLockingFailureException exception
            ) {
                throw new RuntimeException(
                        "El medicamento fue actualizado "
                                + "por otro usuario. Recargue "
                                + "la información."
                );
            }

            auditService.log(
                    adminUsername,
                    "REACTIVATE_MEDICATION",
                    "MEDICATION_CATALOG",
                    "Medicamento "
                            + existing.getCode()
                            + " reactivado."
            );

            return toResponse(
                    existing,
                    "Medicamento reactivado correctamente."
            );
        }

        Medication medication =
                Medication.builder()
                        .code(code)
                        .name(data.name())
                        .activeIngredient(
                                data.activeIngredient()
                        )
                        .concentration(
                                data.concentration()
                        )
                        .presentation(
                                data.presentation()
                        )
                        .unit(data.unit())
                        .description(
                                data.description()
                        )
                        .unitPrice(
                                data.unitPrice()
                        )
                        .controlled(
                                data.controlled()
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
            medication =
                    medicationRepository
                            .saveAndFlush(
                                    medication
                            );

        } catch (
                DataIntegrityViolationException exception
        ) {
            throw new RuntimeException(
                    "El código del medicamento ya se encuentra registrado."
            );
        }

        auditService.log(
                adminUsername,
                "CREATE_MEDICATION",
                "MEDICATION_CATALOG",
                "Medicamento "
                        + medication.getCode()
                        + " creado correctamente. Controlado: "
                        + medication.getControlled()
                        + "."
        );

        return toResponse(
                medication,
                "Medicamento creado correctamente."
        );
    }

    /**
     * Obtiene el catálogo administrativo paginado.
     *
     * Permite filtrar por:
     *
     * - Código.
     * - Nombre.
     * - Principio activo.
     * - Estado.
     * - Medicamento controlado.
     */
    @Transactional(readOnly = true)
    public MedicationPageResponse getMedications(
            String search,
            Boolean active,
            Boolean controlled,
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

        String cleanSearch =
                search != null
                        ? search.trim()
                        : "";

        if (cleanSearch.length() > 200) {
            throw new RuntimeException(
                    "El criterio de búsqueda no puede exceder "
                            + "los 200 caracteres."
            );
        }

        Specification<Medication> specification =
                buildSpecification(
                        cleanSearch,
                        active,
                        controlled
                );

        Pageable pageable =
                PageRequest.of(
                        safePage,
                        safeSize,
                        org.springframework.data.domain.Sort
                                .by(
                                        org.springframework.data.domain.Sort.Direction.ASC,
                                        "name"
                                )
                );

        Page<Medication> result =
                medicationRepository.findAll(
                        specification,
                        pageable
                );

        return MedicationPageResponse.builder()
                .content(
                        result.getContent()
                                .stream()
                                .map(medication ->
                                        toResponse(
                                                medication,
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
     * Devuelve medicamentos activos para
     * formularios administrativos.
     */
    @Transactional(readOnly = true)
    public List<MedicationResponse>
    getActiveMedications() {
        return medicationRepository
                .findByActiveTrueOrderByNameAsc()
                .stream()
                .map(medication ->
                        toResponse(
                                medication,
                                null
                        )
                )
                .toList();
    }

    /**
     * Busca un medicamento por ID.
     */
    @Transactional(readOnly = true)
    public MedicationResponse getMedicationById(
            Long medicationId
    ) {
        return toResponse(
                findMedication(
                        medicationId
                ),
                "Medicamento encontrado."
        );
    }

    /**
     * Actualiza el catálogo del medicamento.
     */
    @Transactional
    public MedicationResponse updateMedication(
            Long medicationId,
            UpdateMedicationRequest request,
            String adminUsername
    ) {
        if (request == null) {
            throw new RuntimeException(
                    "Los datos de actualización son obligatorios."
            );
        }

        Medication medication =
                findMedication(
                        medicationId
                );

        validateVersion(
                medication,
                request.getVersion()
        );

        String code =
                normalizeCode(
                        request.getCode()
                );

        if (
                medicationRepository
                        .existsByCodeIgnoreCaseAndIdNot(
                                code,
                                medication.getId()
                        )
        ) {
            throw new RuntimeException(
                    "El código del medicamento ya se encuentra registrado."
            );
        }

        MedicationData data =
                validateAndNormalize(
                        request.getName(),
                        request.getActiveIngredient(),
                        request.getConcentration(),
                        request.getPresentation(),
                        request.getUnit(),
                        request.getDescription(),
                        request.getUnitPrice(),
                        request.getControlled()
                );

        medication.setCode(code);

        applyMedicationData(
                medication,
                data
        );

        medication.setUpdatedAt(
                LocalDateTime.now()
        );

        medication.setUpdatedBy(
                adminUsername
        );

        try {
            medication =
                    medicationRepository
                            .saveAndFlush(
                                    medication
                            );

        } catch (
                OptimisticLockingFailureException exception
        ) {
            throw new RuntimeException(
                    "El medicamento fue actualizado "
                            + "por otro usuario. Recargue "
                            + "la información."
            );
        }

        auditService.log(
                adminUsername,
                "UPDATE_MEDICATION",
                "MEDICATION_CATALOG",
                "Medicamento ID "
                        + medication.getId()
                        + " actualizado. Código: "
                        + medication.getCode()
                        + ". Controlado: "
                        + medication.getControlled()
                        + "."
        );

        return toResponse(
                medication,
                "Medicamento actualizado correctamente."
        );
    }

    /**
     * Activa o desactiva lógicamente
     * un medicamento.
     *
     * Las recetas y despachos históricos conservan
     * la relación con el medicamento inactivo.
     */
    @Transactional
    public MedicationResponse changeStatus(
            Long medicationId,
            Boolean active,
            Long version,
            String adminUsername
    ) {
        if (active == null) {
            throw new RuntimeException(
                    "Debe indicar el nuevo estado."
            );
        }

        Medication medication =
                findMedication(
                        medicationId
                );

        validateVersion(
                medication,
                version
        );

        if (
                Boolean.TRUE.equals(
                        medication.getActive()
                ) == active
        ) {
            throw new RuntimeException(
                    active
                            ? "El medicamento ya se encuentra activo."
                            : "El medicamento ya se encuentra inactivo."
            );
        }

        medication.setActive(active);
        medication.setUpdatedAt(
                LocalDateTime.now()
        );
        medication.setUpdatedBy(
                adminUsername
        );

        try {
            medication =
                    medicationRepository
                            .saveAndFlush(
                                    medication
                            );

        } catch (
                OptimisticLockingFailureException exception
        ) {
            throw new RuntimeException(
                    "El medicamento fue actualizado "
                            + "por otro usuario. Recargue "
                            + "la información."
            );
        }

        auditService.log(
                adminUsername,
                active
                        ? "REACTIVATE_MEDICATION"
                        : "DEACTIVATE_MEDICATION",
                "MEDICATION_CATALOG",
                "Medicamento ID "
                        + medication.getId()
                        + " cambió su estado a "
                        + (
                        active
                                ? "ACTIVO"
                                : "INACTIVO"
                )
                        + "."
        );

        return toResponse(
                medication,
                active
                        ? "Medicamento reactivado correctamente."
                        : "Medicamento desactivado correctamente."
        );
    }

    /**
     * Construye filtros dinámicos.
     */
    private Specification<Medication>
    buildSpecification(
            String search,
            Boolean active,
            Boolean controlled
    ) {
        return (
                root,
                query,
                criteriaBuilder
        ) -> {
            List<Predicate> predicates =
                    new ArrayList<>();

            if (
                    search != null &&
                            !search.isBlank()
            ) {
                String pattern =
                        "%"
                                + search
                                .toLowerCase(
                                        Locale.ROOT
                                )
                                + "%";

                predicates.add(
                        criteriaBuilder.or(
                                criteriaBuilder.like(
                                        criteriaBuilder.lower(
                                                root.get("code")
                                        ),
                                        pattern
                                ),
                                criteriaBuilder.like(
                                        criteriaBuilder.lower(
                                                root.get("name")
                                        ),
                                        pattern
                                ),
                                criteriaBuilder.like(
                                        criteriaBuilder.lower(
                                                root.get(
                                                        "activeIngredient"
                                                )
                                        ),
                                        pattern
                                )
                        )
                );
            }

            if (active != null) {
                predicates.add(
                        criteriaBuilder.equal(
                                root.get("active"),
                                active
                        )
                );
            }

            if (controlled != null) {
                predicates.add(
                        criteriaBuilder.equal(
                                root.get("controlled"),
                                controlled
                        )
                );
            }

            return criteriaBuilder.and(
                    predicates.toArray(
                            new Predicate[0]
                    )
            );
        };
    }

    private Medication findMedication(
            Long medicationId
    ) {
        if (medicationId == null) {
            throw new RuntimeException(
                    "Debe indicar el medicamento."
            );
        }

        return medicationRepository
                .findById(medicationId)
                .orElseThrow(
                        () -> new RuntimeException(
                                "Medicamento no encontrado."
                        )
                );
    }

    private void validateVersion(
            Medication medication,
            Long requestedVersion
    ) {
        if (requestedVersion == null) {
            throw new RuntimeException(
                    "Debe indicar la versión del medicamento."
            );
        }

        if (
                !medication.getVersion()
                        .equals(
                                requestedVersion
                        )
        ) {
            throw new RuntimeException(
                    "El medicamento fue actualizado "
                            + "por otro usuario. Recargue "
                            + "la información."
            );
        }
    }

    private MedicationData validateAndNormalize(
            String name,
            String activeIngredient,
            String concentration,
            String presentation,
            String unit,
            String description,
            BigDecimal unitPrice,
            Boolean controlled
    ) {
        String normalizedName =
                normalizeRequiredText(
                        name,
                        200,
                        "El nombre del medicamento es obligatorio.",
                        "El nombre no puede exceder los 200 caracteres."
                );

        String normalizedIngredient =
                normalizeRequiredText(
                        activeIngredient,
                        200,
                        "El principio activo es obligatorio.",
                        "El principio activo no puede exceder los 200 caracteres."
                );

        String normalizedConcentration =
                normalizeRequiredText(
                        concentration,
                        100,
                        "La concentración es obligatoria.",
                        "La concentración no puede exceder los 100 caracteres."
                );

        String normalizedPresentation =
                normalizeRequiredText(
                        presentation,
                        100,
                        "La presentación es obligatoria.",
                        "La presentación no puede exceder los 100 caracteres."
                );

        String normalizedUnit =
                normalizeRequiredText(
                        unit,
                        50,
                        "La unidad del medicamento es obligatoria.",
                        "La unidad no puede exceder los 50 caracteres."
                )
                        .toUpperCase(
                                Locale.ROOT
                        );

        String normalizedDescription =
                normalizeRequiredText(
                        description,
                        1000,
                        "La descripción del medicamento es obligatoria.",
                        "La descripción no puede exceder los 1000 caracteres."
                );

        BigDecimal normalizedPrice =
                validatePrice(
                        unitPrice
                );

        if (controlled == null) {
            throw new RuntimeException(
                    "Debe indicar si el medicamento es controlado."
            );
        }

        return new MedicationData(
                normalizedName,
                normalizedIngredient,
                normalizedConcentration,
                normalizedPresentation,
                normalizedUnit,
                normalizedDescription,
                normalizedPrice,
                controlled
        );
    }

    private void applyMedicationData(
            Medication medication,
            MedicationData data
    ) {
        medication.setName(
                data.name()
        );

        medication.setActiveIngredient(
                data.activeIngredient()
        );

        medication.setConcentration(
                data.concentration()
        );

        medication.setPresentation(
                data.presentation()
        );

        medication.setUnit(
                data.unit()
        );

        medication.setDescription(
                data.description()
        );

        medication.setUnitPrice(
                data.unitPrice()
        );

        medication.setControlled(
                data.controlled()
        );
    }

    private String normalizeCode(
            String value
    ) {
        String cleanValue =
                normalizeRequiredText(
                        value,
                        40,
                        "El código del medicamento es obligatorio.",
                        "El código no puede exceder los 40 caracteres."
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
                    "El código solamente puede contener "
                            + "letras, números, guion y guion bajo."
            );
        }

        return cleanValue;
    }

    private BigDecimal validatePrice(
            BigDecimal value
    ) {
        if (value == null) {
            throw new RuntimeException(
                    "El precio unitario es obligatorio."
            );
        }

        if (
                value.compareTo(
                        BigDecimal.ZERO
                ) <= 0
        ) {
            throw new RuntimeException(
                    "El precio unitario debe ser mayor que cero."
            );
        }

        if (value.scale() > 2) {
            throw new RuntimeException(
                    "El precio admite como máximo 2 decimales."
            );
        }

        if (
                value.precision()
                        - value.scale()
                        > 8
        ) {
            throw new RuntimeException(
                    "El precio admite como máximo "
                            + "8 dígitos enteros."
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

    private MedicationResponse toResponse(
            Medication medication,
            String message
    ) {
        return MedicationResponse.builder()
                .id(medication.getId())
                .code(medication.getCode())
                .name(medication.getName())
                .activeIngredient(
                        medication
                                .getActiveIngredient()
                )
                .concentration(
                        medication
                                .getConcentration()
                )
                .presentation(
                        medication
                                .getPresentation()
                )
                .unit(medication.getUnit())
                .description(
                        medication
                                .getDescription()
                )
                .unitPrice(
                        medication.getUnitPrice()
                )
                .controlled(
                        medication.getControlled()
                )
                .active(
                        medication.getActive()
                )
                .createdAt(
                        medication.getCreatedAt()
                                .toString()
                )
                .createdBy(
                        medication.getCreatedBy()
                )
                .updatedAt(
                        medication.getUpdatedAt()
                                .toString()
                )
                .updatedBy(
                        medication.getUpdatedBy()
                )
                .version(
                        medication.getVersion()
                )
                .message(message)
                .build();
    }

    /**
     * Objeto interno para transportar datos
     * ya validados y normalizados.
     */
    private record MedicationData(
            String name,
            String activeIngredient,
            String concentration,
            String presentation,
            String unit,
            String description,
            BigDecimal unitPrice,
            Boolean controlled
    ) {
    }
}
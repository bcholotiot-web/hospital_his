package com.hospital.his.appointments.service;

import com.hospital.his.appointments.dto.AppointmentStatusCodeResponse;
import com.hospital.his.appointments.dto.AppointmentStatusConfigurationPageResponse;
import com.hospital.his.appointments.dto.AppointmentStatusConfigurationResponse;
import com.hospital.his.appointments.dto.CreateAppointmentStatusConfigurationRequest;
import com.hospital.his.appointments.dto.UpdateAppointmentStatusConfigurationRequest;
import com.hospital.his.appointments.entity.AppointmentStatus;
import com.hospital.his.appointments.entity.AppointmentStatusConfiguration;
import com.hospital.his.appointments.repository.AppointmentStatusConfigurationRepository;
import com.hospital.his.audit.service.AuditService;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Service
public class AppointmentStatusConfigurationService {

    private static final int MAXIMUM_PAGE_SIZE =
            100;

    private final AppointmentStatusConfigurationRepository
            configurationRepository;

    private final AuditService auditService;

    public AppointmentStatusConfigurationService(
            AppointmentStatusConfigurationRepository
                    configurationRepository,
            AuditService auditService
    ) {
        this.configurationRepository =
                configurationRepository;

        this.auditService =
                auditService;
    }

    /**
     * Lista configuraciones con paginación.
     *
     * El filtro search acepta:
     *
     * - Código exacto del enum.
     * - Texto parcial en el nombre visible.
     */
    @Transactional(readOnly = true)
    public AppointmentStatusConfigurationPageResponse
    getConfigurations(
            String search,
            Boolean active,
            Integer page,
            Integer size
    ) {
        int safePage =
                page != null ? page : 0;

        int safeSize =
                size != null ? size : 20;

        validatePagination(
                safePage,
                safeSize
        );

        String cleanSearch =
                search != null
                        ? search.trim()
                        : "";

        if (cleanSearch.length() > 100) {
            throw new RuntimeException(
                    "El criterio de búsqueda no puede exceder "
                            + "los 100 caracteres."
            );
        }

        Pageable pageable =
                PageRequest.of(
                        safePage,
                        safeSize
                );

        Page<AppointmentStatusConfiguration> result;

        if (active != null) {
            result =
                    configurationRepository
                            .findByActiveOrderByDisplayOrderAscCodeAsc(
                                    active,
                                    pageable
                            );

            /*
             * El primer contrato paginado mantiene
             * el filtro de texto separado del estado.
             * Posteriormente puede ampliarse mediante
             * JpaSpecificationExecutor si se requiere
             * combinar ambos filtros.
             */

        } else if (!cleanSearch.isBlank()) {
            AppointmentStatus parsedCode =
                    parseOptionalStatusCode(
                            cleanSearch
                    );

            result =
                    configurationRepository
                            .findByDisplayNameContainingIgnoreCaseOrCodeOrderByDisplayOrderAscCodeAsc(
                                    cleanSearch,
                                    parsedCode,
                                    pageable
                            );

        } else {
            result =
                    configurationRepository
                            .findAllByOrderByDisplayOrderAscCodeAsc(
                                    pageable
                            );
        }

        return toPageResponse(result);
    }

    /**
     * Devuelve todos los códigos técnicos del enum
     * e indica cuáles ya están configurados.
     */
    @Transactional(readOnly = true)
    public List<AppointmentStatusCodeResponse>
    getAvailableCodes() {
        return Arrays.stream(
                        AppointmentStatus.values()
                )
                .map(code -> {
                    AppointmentStatusConfiguration
                            configuration =
                            configurationRepository
                                    .findByCode(code)
                                    .orElse(null);

                    return AppointmentStatusCodeResponse
                            .builder()
                            .code(code.name())
                            .suggestedDisplayName(
                                    formatCode(
                                            code.name()
                                    )
                            )
                            .configured(
                                    configuration != null
                            )
                            .configurationId(
                                    configuration != null
                                            ? configuration
                                            .getId()
                                            : null
                            )
                            .active(
                                    configuration != null
                                            ? configuration
                                            .getActive()
                                            : null
                            )
                            .build();
                })
                .toList();
    }

    /**
     * Busca una configuración por ID.
     */
    @Transactional(readOnly = true)
    public AppointmentStatusConfigurationResponse
    getConfigurationById(
            Long configurationId
    ) {
        return toResponse(
                findConfiguration(
                        configurationId
                ),
                "Configuración encontrada."
        );
    }

    /**
     * Crea o reactiva la configuración de un
     * código técnico.
     */
    @Transactional
    public AppointmentStatusConfigurationResponse
    createConfiguration(
            CreateAppointmentStatusConfigurationRequest request,
            String adminUsername
    ) {
        if (request == null) {
            throw new RuntimeException(
                    "Los datos de configuración son obligatorios."
            );
        }

        AppointmentStatus code =
                parseRequiredStatusCode(
                        request.getCode()
                );

        String displayName =
                normalizeRequiredText(
                        request.getDisplayName(),
                        100,
                        "El nombre visible es obligatorio.",
                        "El nombre visible no puede exceder "
                                + "los 100 caracteres."
                );

        String description =
                normalizeRequiredText(
                        request.getDescription(),
                        500,
                        "La descripción del estado es obligatoria.",
                        "La descripción no puede exceder "
                                + "los 500 caracteres."
                );

        validateDisplayOrder(
                request.getDisplayOrder()
        );

        String color =
                normalizeColor(
                        request.getColor()
                );

        validateVisibility(
                request.getPatientVisible(),
                request.getStaffVisible()
        );

        AppointmentStatusConfiguration existing =
                configurationRepository
                        .findByCode(code)
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
                        "El estado técnico "
                                + code.name()
                                + " ya tiene una configuración activa."
                );
            }

            applyConfigurationData(
                    existing,
                    displayName,
                    description,
                    request.getDisplayOrder(),
                    color,
                    request.getPatientVisible(),
                    request.getStaffVisible()
            );

            existing.setActive(true);
            existing.setUpdatedAt(now);
            existing.setUpdatedBy(
                    adminUsername
            );

            try {
                existing =
                        configurationRepository
                                .saveAndFlush(
                                        existing
                                );

            } catch (
                    OptimisticLockingFailureException exception
            ) {
                throw new RuntimeException(
                        "La configuración fue actualizada "
                                + "por otro usuario. Recargue "
                                + "la información."
                );
            }

            auditService.log(
                    adminUsername,
                    "REACTIVATE_APPOINTMENT_STATUS_CONFIG",
                    "APPOINTMENT_CATALOG",
                    "Configuración del estado "
                            + code.name()
                            + " reactivada."
            );

            return toResponse(
                    existing,
                    "Configuración de estado reactivada correctamente."
            );
        }

        AppointmentStatusConfiguration configuration =
                AppointmentStatusConfiguration
                        .builder()
                        .code(code)
                        .displayName(
                                displayName
                        )
                        .description(
                                description
                        )
                        .displayOrder(
                                request.getDisplayOrder()
                        )
                        .color(color)
                        .patientVisible(
                                request.getPatientVisible()
                        )
                        .staffVisible(
                                request.getStaffVisible()
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
            configuration =
                    configurationRepository
                            .saveAndFlush(
                                    configuration
                            );

        } catch (
                DataIntegrityViolationException exception
        ) {
            throw new RuntimeException(
                    "El estado técnico ya tiene "
                            + "una configuración registrada."
            );
        }

        auditService.log(
                adminUsername,
                "CREATE_APPOINTMENT_STATUS_CONFIG",
                "APPOINTMENT_CATALOG",
                "Configuración del estado "
                        + code.name()
                        + " creada."
        );

        return toResponse(
                configuration,
                "Configuración de estado creada correctamente."
        );
    }

    /**
     * Actualiza únicamente la presentación.
     *
     * No permite cambiar el código técnico.
     */
    @Transactional
    public AppointmentStatusConfigurationResponse
    updateConfiguration(
            Long configurationId,
            UpdateAppointmentStatusConfigurationRequest request,
            String adminUsername
    ) {
        if (request == null) {
            throw new RuntimeException(
                    "Los datos de actualización son obligatorios."
            );
        }

        AppointmentStatusConfiguration configuration =
                findConfiguration(
                        configurationId
                );

        validateVersion(
                configuration,
                request.getVersion()
        );

        String displayName =
                normalizeRequiredText(
                        request.getDisplayName(),
                        100,
                        "El nombre visible es obligatorio.",
                        "El nombre visible no puede exceder "
                                + "los 100 caracteres."
                );

        String description =
                normalizeRequiredText(
                        request.getDescription(),
                        500,
                        "La descripción del estado es obligatoria.",
                        "La descripción no puede exceder "
                                + "los 500 caracteres."
                );

        validateDisplayOrder(
                request.getDisplayOrder()
        );

        String color =
                normalizeColor(
                        request.getColor()
                );

        validateVisibility(
                request.getPatientVisible(),
                request.getStaffVisible()
        );

        applyConfigurationData(
                configuration,
                displayName,
                description,
                request.getDisplayOrder(),
                color,
                request.getPatientVisible(),
                request.getStaffVisible()
        );

        configuration.setUpdatedAt(
                LocalDateTime.now()
        );

        configuration.setUpdatedBy(
                adminUsername
        );

        try {
            configuration =
                    configurationRepository
                            .saveAndFlush(
                                    configuration
                            );

        } catch (
                OptimisticLockingFailureException exception
        ) {
            throw new RuntimeException(
                    "La configuración fue actualizada "
                            + "por otro usuario. Recargue "
                            + "la información."
            );
        }

        auditService.log(
                adminUsername,
                "UPDATE_APPOINTMENT_STATUS_CONFIG",
                "APPOINTMENT_CATALOG",
                "Configuración del estado "
                        + configuration
                        .getCode()
                        .name()
                        + " actualizada."
        );

        return toResponse(
                configuration,
                "Configuración de estado actualizada correctamente."
        );
    }

    /**
     * Activa o desactiva solamente la configuración.
     *
     * No modifica citas ni AppointmentStatus.
     */
    @Transactional
    public AppointmentStatusConfigurationResponse
    changeStatus(
            Long configurationId,
            Boolean active,
            Long version,
            String adminUsername
    ) {
        if (active == null) {
            throw new RuntimeException(
                    "Debe indicar el nuevo estado."
            );
        }

        AppointmentStatusConfiguration configuration =
                findConfiguration(
                        configurationId
                );

        validateVersion(
                configuration,
                version
        );

        if (
                Boolean.TRUE.equals(
                        configuration.getActive()
                ) == active
        ) {
            throw new RuntimeException(
                    active
                            ? "La configuración ya se encuentra activa."
                            : "La configuración ya se encuentra inactiva."
            );
        }

        configuration.setActive(active);
        configuration.setUpdatedAt(
                LocalDateTime.now()
        );
        configuration.setUpdatedBy(
                adminUsername
        );

        try {
            configuration =
                    configurationRepository
                            .saveAndFlush(
                                    configuration
                            );

        } catch (
                OptimisticLockingFailureException exception
        ) {
            throw new RuntimeException(
                    "La configuración fue actualizada "
                            + "por otro usuario. Recargue "
                            + "la información."
            );
        }

        auditService.log(
                adminUsername,
                active
                        ? "REACTIVATE_APPOINTMENT_STATUS_CONFIG"
                        : "DEACTIVATE_APPOINTMENT_STATUS_CONFIG",
                "APPOINTMENT_CATALOG",
                "Configuración del estado "
                        + configuration
                        .getCode()
                        .name()
                        + " cambió a "
                        + (
                        active
                                ? "ACTIVA"
                                : "INACTIVA"
                )
                        + "."
        );

        return toResponse(
                configuration,
                active
                        ? "Configuración reactivada correctamente."
                        : "Configuración desactivada correctamente."
        );
    }

    private AppointmentStatusConfiguration
    findConfiguration(
            Long configurationId
    ) {
        if (configurationId == null) {
            throw new RuntimeException(
                    "Debe indicar la configuración."
            );
        }

        return configurationRepository
                .findById(configurationId)
                .orElseThrow(
                        () -> new RuntimeException(
                                "Configuración de estado no encontrada."
                        )
                );
    }

    private AppointmentStatus
    parseRequiredStatusCode(
            String value
    ) {
        if (
                value == null ||
                        value.isBlank()
        ) {
            throw new RuntimeException(
                    "El código del estado es obligatorio."
            );
        }

        try {
            return AppointmentStatus.valueOf(
                    value.trim()
                            .toUpperCase(
                                    Locale.ROOT
                            )
            );

        } catch (
                IllegalArgumentException exception
        ) {
            throw new RuntimeException(
                    "El código del estado no existe "
                            + "en el flujo técnico de citas."
            );
        }
    }

    /**
     * Para búsquedas parciales, si el texto no
     * coincide exactamente con un enum, utilizamos
     * un código imposible de encontrar.
     */
    private AppointmentStatus
    parseOptionalStatusCode(
            String value
    ) {
        if (
                value == null ||
                        value.isBlank()
        ) {
            return null;
        }

        try {
            return AppointmentStatus.valueOf(
                    value.trim()
                            .toUpperCase(
                                    Locale.ROOT
                            )
            );

        } catch (
                IllegalArgumentException exception
        ) {
            return null;
        }
    }

    private void applyConfigurationData(
            AppointmentStatusConfiguration configuration,
            String displayName,
            String description,
            Integer displayOrder,
            String color,
            Boolean patientVisible,
            Boolean staffVisible
    ) {
        configuration.setDisplayName(
                displayName
        );
        configuration.setDescription(
                description
        );
        configuration.setDisplayOrder(
                displayOrder
        );
        configuration.setColor(color);
        configuration.setPatientVisible(
                patientVisible
        );
        configuration.setStaffVisible(
                staffVisible
        );
    }

    private void validatePagination(
            int page,
            int size
    ) {
        if (page < 0) {
            throw new RuntimeException(
                    "El número de página no puede ser negativo."
            );
        }

        if (
                size <= 0 ||
                        size > MAXIMUM_PAGE_SIZE
        ) {
            throw new RuntimeException(
                    "El tamaño de página debe estar "
                            + "entre 1 y 100 registros."
            );
        }
    }

    private void validateDisplayOrder(
            Integer displayOrder
    ) {
        if (displayOrder == null) {
            throw new RuntimeException(
                    "El orden visual es obligatorio."
            );
        }

        if (
                displayOrder < 0 ||
                        displayOrder > 1000
        ) {
            throw new RuntimeException(
                    "El orden visual debe estar "
                            + "entre 0 y 1000."
            );
        }
    }

    private void validateVisibility(
            Boolean patientVisible,
            Boolean staffVisible
    ) {
        if (patientVisible == null) {
            throw new RuntimeException(
                    "Debe indicar si el estado es "
                            + "visible para el paciente."
            );
        }

        if (staffVisible == null) {
            throw new RuntimeException(
                    "Debe indicar si el estado es "
                            + "visible para el personal."
            );
        }
    }

    private String normalizeColor(
            String value
    ) {
        if (
                value == null ||
                        value.isBlank()
        ) {
            throw new RuntimeException(
                    "El color del estado es obligatorio."
            );
        }

        String cleanValue =
                value.trim()
                        .toUpperCase(
                                Locale.ROOT
                        );

        if (
                !cleanValue.matches(
                        "^#[0-9A-F]{6}$"
                )
        ) {
            throw new RuntimeException(
                    "El color debe utilizar el formato "
                            + "hexadecimal #RRGGBB."
            );
        }

        return cleanValue;
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

    private void validateVersion(
            AppointmentStatusConfiguration configuration,
            Long requestedVersion
    ) {
        if (requestedVersion == null) {
            throw new RuntimeException(
                    "Debe indicar la versión "
                            + "de la configuración."
            );
        }

        if (
                !configuration.getVersion()
                        .equals(
                                requestedVersion
                        )
        ) {
            throw new RuntimeException(
                    "La configuración fue actualizada "
                            + "por otro usuario. Recargue "
                            + "la información."
            );
        }
    }

    private String formatCode(
            String code
    ) {
        String cleanText =
                code.toLowerCase(
                                Locale.ROOT
                        )
                        .replace(
                                "_",
                                " "
                        );

        return Character
                .toUpperCase(
                        cleanText.charAt(0)
                )
                + cleanText.substring(1);
    }

    private AppointmentStatusConfigurationPageResponse
    toPageResponse(
            Page<AppointmentStatusConfiguration> page
    ) {
        return AppointmentStatusConfigurationPageResponse
                .builder()
                .content(
                        page.getContent()
                                .stream()
                                .map(configuration ->
                                        toResponse(
                                                configuration,
                                                null
                                        )
                                )
                                .toList()
                )
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(
                        page.getTotalElements()
                )
                .totalPages(
                        page.getTotalPages()
                )
                .first(page.isFirst())
                .last(page.isLast())
                .empty(page.isEmpty())
                .build();
    }

    private AppointmentStatusConfigurationResponse
    toResponse(
            AppointmentStatusConfiguration configuration,
            String message
    ) {
        return AppointmentStatusConfigurationResponse
                .builder()
                .id(configuration.getId())
                .code(
                        configuration
                                .getCode()
                                .name()
                )
                .displayName(
                        configuration
                                .getDisplayName()
                )
                .description(
                        configuration
                                .getDescription()
                )
                .displayOrder(
                        configuration
                                .getDisplayOrder()
                )
                .color(
                        configuration.getColor()
                )
                .patientVisible(
                        configuration
                                .getPatientVisible()
                )
                .staffVisible(
                        configuration
                                .getStaffVisible()
                )
                .active(
                        configuration.getActive()
                )
                .createdAt(
                        configuration
                                .getCreatedAt()
                                .toString()
                )
                .createdBy(
                        configuration.getCreatedBy()
                )
                .updatedAt(
                        configuration
                                .getUpdatedAt()
                                .toString()
                )
                .updatedBy(
                        configuration.getUpdatedBy()
                )
                .version(
                        configuration.getVersion()
                )
                .message(message)
                .build();
    }
}
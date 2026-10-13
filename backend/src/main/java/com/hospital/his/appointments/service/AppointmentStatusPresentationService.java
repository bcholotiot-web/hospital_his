package com.hospital.his.appointments.service;

import com.hospital.his.appointments.dto.AppointmentStatusPresentationResponse;
import com.hospital.his.appointments.entity.AppointmentStatus;
import com.hospital.his.appointments.entity.AppointmentStatusConfiguration;
import com.hospital.his.appointments.repository.AppointmentStatusConfigurationRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Optional;

/**
 * Resuelve la presentación visual de los estados
 * técnicos de cita.
 *
 * AppointmentStatus sigue siendo la fuente de verdad
 * de la máquina de estados.
 *
 * Este servicio solamente resuelve:
 *
 * - Nombre visible.
 * - Descripción.
 * - Color.
 * - Orden visual.
 * - Contexto de visibilidad.
 */
@Service
public class AppointmentStatusPresentationService {

    /**
     * Color utilizado cuando no existe una
     * configuración activa y visible.
     */
    private static final String DEFAULT_COLOR =
            "#64748B";

    /**
     * Orden utilizado para una presentación
     * no configurada.
     */
    private static final Integer DEFAULT_ORDER =
            1000;

    private final AppointmentStatusConfigurationRepository
            configurationRepository;

    public AppointmentStatusPresentationService(
            AppointmentStatusConfigurationRepository
                    configurationRepository
    ) {
        this.configurationRepository =
                configurationRepository;
    }

    /**
     * Resuelve la presentación para un paciente.
     *
     * Solamente utiliza la configuración cuando:
     *
     * - Está activa.
     * - patientVisible es true.
     *
     * En cualquier otro caso devuelve una presentación
     * derivada del enum.
     */
    @Transactional(readOnly = true)
    public AppointmentStatusPresentationResponse
    resolveForPatient(
            AppointmentStatus status
    ) {
        if (status == null) {
            return buildUnknownPresentation();
        }

        Optional<AppointmentStatusConfiguration>
                configuration =
                configurationRepository
                        .findByCodeAndActiveTrueAndPatientVisibleTrue(
                                status
                        );

        return configuration
                .map(this::toConfiguredPresentation)
                .orElseGet(
                        () -> buildDefaultPresentation(
                                status
                        )
                );
    }

    /**
     * Resuelve la presentación para personal interno.
     *
     * Solamente utiliza la configuración cuando:
     *
     * - Está activa.
     * - staffVisible es true.
     */
    @Transactional(readOnly = true)
    public AppointmentStatusPresentationResponse
    resolveForStaff(
            AppointmentStatus status
    ) {
        if (status == null) {
            return buildUnknownPresentation();
        }

        Optional<AppointmentStatusConfiguration>
                configuration =
                configurationRepository
                        .findByCodeAndActiveTrueAndStaffVisibleTrue(
                                status
                        );

        return configuration
                .map(this::toConfiguredPresentation)
                .orElseGet(
                        () -> buildDefaultPresentation(
                                status
                        )
                );
    }

    /**
     * Convierte una configuración persistida
     * a la respuesta visual.
     */
    private AppointmentStatusPresentationResponse
    toConfiguredPresentation(
            AppointmentStatusConfiguration configuration
    ) {
        return AppointmentStatusPresentationResponse
                .builder()
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
                .color(
                        normalizeConfiguredColor(
                                configuration
                                        .getColor()
                        )
                )
                .displayOrder(
                        configuration
                                .getDisplayOrder()
                )
                .configured(true)
                .build();
    }

    /**
     * Genera una presentación segura cuando:
     *
     * - No existe configuración.
     * - Está inactiva.
     * - No es visible en el contexto actual.
     */
    private AppointmentStatusPresentationResponse
    buildDefaultPresentation(
            AppointmentStatus status
    ) {
        return AppointmentStatusPresentationResponse
                .builder()
                .code(
                        status.name()
                )
                .displayName(
                        formatTechnicalCode(
                                status.name()
                        )
                )
                .description(null)
                .color(
                        DEFAULT_COLOR
                )
                .displayOrder(
                        DEFAULT_ORDER
                )
                .configured(false)
                .build();
    }

    /**
     * Respaldo para registros anómalos sin estado.
     */
    private AppointmentStatusPresentationResponse
    buildUnknownPresentation() {
        return AppointmentStatusPresentationResponse
                .builder()
                .code(null)
                .displayName(
                        "Sin estado"
                )
                .description(null)
                .color(
                        DEFAULT_COLOR
                )
                .displayOrder(
                        DEFAULT_ORDER
                )
                .configured(false)
                .build();
    }

    /**
     * Convierte:
     *
     * PENDIENTE_DE_PAGO
     *
     * en:
     *
     * Pendiente de pago
     */
    private String formatTechnicalCode(
            String technicalCode
    ) {
        if (
                technicalCode == null ||
                        technicalCode.isBlank()
        ) {
            return "Sin estado";
        }

        String formattedText =
                technicalCode
                        .trim()
                        .toLowerCase(
                                Locale.ROOT
                        )
                        .replace(
                                "_",
                                " "
                        );

        return Character
                .toUpperCase(
                        formattedText.charAt(0)
                )
                + formattedText.substring(1);
    }

    /**
     * Validación defensiva.
     *
     * La entidad y PostgreSQL ya validan el color,
     * pero esta comprobación evita propagar valores
     * inválidos si existen datos históricos.
     */
    private String normalizeConfiguredColor(
            String color
    ) {
        if (
                color == null ||
                        !color.matches(
                                "^#[0-9A-Fa-f]{6}$"
                        )
        ) {
            return DEFAULT_COLOR;
        }

        return color.toUpperCase(
                Locale.ROOT
        );
    }
}
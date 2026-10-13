package com.hospital.his.appointments.controller;

import com.hospital.his.appointments.dto.AppointmentStatusCodeResponse;
import com.hospital.his.appointments.dto.AppointmentStatusConfigurationPageResponse;
import com.hospital.his.appointments.dto.AppointmentStatusConfigurationResponse;
import com.hospital.his.appointments.dto.CreateAppointmentStatusConfigurationRequest;
import com.hospital.his.appointments.dto.UpdateAppointmentStatusConfigurationRequest;
import com.hospital.his.appointments.service.AppointmentStatusConfigurationService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

/**
 * CU-14:
 * Mantenimiento administrativo de la presentación
 * de los estados de cita.
 */
@RestController
@RequestMapping(
        "/api/admin/appointment-statuses"
)
public class AdminAppointmentStatusController {

    private final AppointmentStatusConfigurationService
            configurationService;

    public AdminAppointmentStatusController(
            AppointmentStatusConfigurationService
                    configurationService
    ) {
        this.configurationService =
                configurationService;
    }

    /**
     * Listado paginado.
     */
    @PreAuthorize("hasAuthority('APPOINTMENT_STATUS_READ')")
    @GetMapping
    public ResponseEntity<
            AppointmentStatusConfigurationPageResponse
            >
    getConfigurations(
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
                configurationService
                        .getConfigurations(
                                search,
                                active,
                                page,
                                size
                        )
        );
    }

    /**
     * Códigos técnicos disponibles.
     */
    @PreAuthorize("hasAuthority('APPOINTMENT_STATUS_READ')")
    @GetMapping("/available-codes")
    public ResponseEntity<
            List<AppointmentStatusCodeResponse>
            >
    getAvailableCodes() {
        return ResponseEntity.ok(
                configurationService
                        .getAvailableCodes()
        );
    }

    /**
     * Busca una configuración por ID.
     */
    @PreAuthorize("hasAuthority('APPOINTMENT_STATUS_READ')")
    @GetMapping("/{configurationId}")
    public ResponseEntity<
            AppointmentStatusConfigurationResponse
            >
    getConfigurationById(
            @PathVariable
            Long configurationId
    ) {
        return ResponseEntity.ok(
                configurationService
                        .getConfigurationById(
                                configurationId
                        )
        );
    }

    /**
     * Crea o reactiva una configuración.
     */
    @PreAuthorize("hasAuthority('APPOINTMENT_STATUS_CREATE')")
    @PostMapping
    public ResponseEntity<
            AppointmentStatusConfigurationResponse
            >
    createConfiguration(
            @Valid
            @RequestBody
            CreateAppointmentStatusConfigurationRequest request,

            Authentication authentication
    ) {
        return ResponseEntity.ok(
                configurationService
                        .createConfiguration(
                                request,
                                authentication.getName()
                        )
        );
    }

    /**
     * Actualiza la presentación.
     */
    @PreAuthorize("hasAuthority('APPOINTMENT_STATUS_UPDATE')")
    @PutMapping("/{configurationId}")
    public ResponseEntity<
            AppointmentStatusConfigurationResponse
            >
    updateConfiguration(
            @PathVariable
            Long configurationId,

            @Valid
            @RequestBody
            UpdateAppointmentStatusConfigurationRequest request,

            Authentication authentication
    ) {
        return ResponseEntity.ok(
                configurationService
                        .updateConfiguration(
                                configurationId,
                                request,
                                authentication.getName()
                        )
        );
    }

    /**
     * Activa o desactiva la configuración.
     */
    @PreAuthorize("hasAuthority('APPOINTMENT_STATUS_DELETE')")
    @PatchMapping("/{configurationId}/status")
    public ResponseEntity<
            AppointmentStatusConfigurationResponse
            >
    changeStatus(
            @PathVariable
            Long configurationId,

            @RequestParam
            Boolean active,

            @RequestParam
            Long version,

            Authentication authentication
    ) {
        return ResponseEntity.ok(
                configurationService
                        .changeStatus(
                                configurationId,
                                active,
                                version,
                                authentication.getName()
                        )
        );
    }
}